package top.rookiestwo.wheatmarket.service;

import net.minecraft.nbt.CompoundTag;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.caches.MarketItemCache;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;
import top.rookiestwo.wheatmarket.database.repository.MarketItemRepository;
import top.rookiestwo.wheatmarket.database.repository.PlayerInfoRepository;
import top.rookiestwo.wheatmarket.database.repository.PurchaseRecordRepository;
import top.rookiestwo.wheatmarket.database.transaction.TransactionManager;
import top.rookiestwo.wheatmarket.service.result.ServiceResult;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MarketService {
    private static final int DEFAULT_ITEMS_PER_PAGE = 10;
    private static final int MIN_ITEMS_PER_PAGE = 1;
    private static final int MAX_ITEMS_PER_PAGE = 64;

    private final TransactionManager transactionManager;
    private final PlayerInfoRepository playerInfoRepository;
    private final MarketItemRepository marketItemRepository;
    private final PurchaseRecordRepository purchaseRecordRepository;
    private final MarketItemCache marketItemCache;

    public MarketService(TransactionManager transactionManager,
                         PlayerInfoRepository playerInfoRepository,
                         MarketItemRepository marketItemRepository,
                         PurchaseRecordRepository purchaseRecordRepository,
                         MarketItemCache marketItemCache) {
        this.transactionManager = transactionManager;
        this.playerInfoRepository = playerInfoRepository;
        this.marketItemRepository = marketItemRepository;
        this.purchaseRecordRepository = purchaseRecordRepository;
        this.marketItemCache = marketItemCache;
    }

    public ServiceResult<MarketListResult> requestMarketList(int tradeType, int itemType, int sortType, String searchQuery, int page) {
        return requestMarketList(null, tradeType, itemType, sortType, searchQuery, page, DEFAULT_ITEMS_PER_PAGE);
    }

    public ServiceResult<MarketListResult> requestMarketList(int tradeType, int itemType, int sortType, String searchQuery, int page, int pageSize) {
        return requestMarketList(null, tradeType, itemType, sortType, searchQuery, page, pageSize);
    }

    public ServiceResult<MarketListResult> requestMarketList(UUID buyerId, int tradeType, int itemType, int sortType,
                                                             String searchQuery, int page, int pageSize) {
        return requestMarketList(buyerId, tradeType, itemType, sortType, searchQuery, List.of(),
                defaultSortAscending(sortType), false, page, pageSize);
    }

    public ServiceResult<MarketListResult> requestMarketList(UUID buyerId, int tradeType, int itemType, int sortType,
                                                             String searchQuery, Collection<String> localizedSearchItemIds,
                                                             int page, int pageSize) {
        return requestMarketList(buyerId, tradeType, itemType, sortType, searchQuery, localizedSearchItemIds,
                defaultSortAscending(sortType), false, page, pageSize);
    }

    public ServiceResult<MarketListResult> requestMarketList(UUID buyerId, int tradeType, int itemType, int sortType,
                                                             String searchQuery, Collection<String> localizedSearchItemIds,
                                                             boolean sortAscending, boolean ownListingsOnly,
                                                             int page, int pageSize) {
        int safePageSize = Math.max(MIN_ITEMS_PER_PAGE, Math.min(pageSize, MAX_ITEMS_PER_PAGE));
        String normalizedSearchQuery = normalizeSearchQuery(searchQuery);
        Set<String> normalizedSearchItemIds = normalizedSearchQuery.isEmpty()
                ? Set.of()
                : normalizeSearchItemIds(localizedSearchItemIds);
        Collection<MarketItem> allItems = marketItemCache.values();
        List<MarketItem> filtered = allItems.stream()
                .filter(item -> !item.isExpired())
                .filter(item -> item.isInfinite() || item.getAmount() > 0)
                .filter(item -> {
                    if (tradeType == 1) return item.getIfSell();
                    if (tradeType == 2) return !item.getIfSell();
                    return true;
                })
                .filter(item -> {
                    if (itemType == 1) return item.getIfAdmin();
                    if (itemType == 2) return !item.getIfAdmin();
                    return true;
                })
                .filter(item -> !ownListingsOnly || (buyerId != null && buyerId.equals(item.getSellerID())))
                .filter(item -> {
                    if (normalizedSearchQuery.isEmpty()) return true;
                    return matchesSearchQuery(item, normalizedSearchQuery, normalizedSearchItemIds);
                })
                .sorted((a, b) -> compareMarketItems(a, b, sortType, sortAscending))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / safePageSize));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = safePage * safePageSize;
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());
        List<MarketItem> pageItems = filtered.subList(fromIndex, toIndex);
        try {
            MarketListPageData pageData = transactionManager.executeSync(connection -> {
                double balance = 0.0;
                if (buyerId != null) {
                    playerInfoRepository.ensureExists(connection, buyerId);
                    balance = playerInfoRepository.getBalance(connection, buyerId);
                }
                List<MarketListEntry> entries = pageItems.stream()
                        .map(item -> new MarketListEntry(item, resolveRemainingCooldownAmount(connection, item, buyerId)))
                        .toList();
                return new MarketListPageData(entries, balance);
            });
            return ServiceResult.success(new MarketListResult(pageData.items(), totalPages, safePage, pageData.balance()));
        } catch (Exception e) {
            WheatMarket.LOGGER.error("Failed to build market list.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        }
    }

    public CompletableFuture<ServiceResult<PurchaseResult>> buyItem(UUID buyerId, UUID marketItemID, int amount) {
        return transactionManager.executeAsync(connection -> {
            MarketItem item = marketItemCache.get(marketItemID);
            if (item == null) {
                return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.item_not_found");
            }
            if (item.isExpired()) {
                return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.item_expired");
            }
            if (!item.getIfSell()) {
                return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.not_for_sale");
            }
            if (amount <= 0 || (!item.isInfinite() && amount > item.getAmount())) {
                return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.invalid_amount");
            }
            if (!isPositiveMoney(item.getPrice())) {
                return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.invalid_price");
            }

            playerInfoRepository.ensureExists(connection, buyerId);
            double totalCost = item.getPrice() * amount;
            if (!isPositiveMoney(totalCost)) {
                return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.invalid_price");
            }
            double buyerBalance = playerInfoRepository.getBalance(connection, buyerId);
            if (buyerBalance < totalCost) {
                return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.insufficient_balance");
            }
            double newBalance = buyerBalance - totalCost;
            if (!isNonNegativeMoney(newBalance)) {
                return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.invalid_price");
            }

            if (item.hasCooldownRestriction()) {
                ServiceResult<Void> cooldownCheck = validateCooldownRestriction(connection, item, buyerId, amount);
                if (!cooldownCheck.isSuccess()) {
                    return ServiceResult.<PurchaseResult>failure(cooldownCheck.getMessageKey(), cooldownCheck.getMessageArgs());
                }
            }

            playerInfoRepository.setBalance(connection, buyerId, newBalance);
            if (!item.getIfAdmin()) {
                playerInfoRepository.ensureExists(connection, item.getSellerID());
                double sellerBalance = playerInfoRepository.getBalance(connection, item.getSellerID());
                double newSellerBalance = sellerBalance + totalCost;
                if (!isNonNegativeMoney(newSellerBalance)) {
                    return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.invalid_price");
                }
                playerInfoRepository.setBalance(connection, item.getSellerID(), newSellerBalance);
            }

            MarketItem updatedItem = copyOf(item);
            if (!updatedItem.reduceAmount(amount)) {
                return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.invalid_amount");
            }
            if (updatedItem.isAvailable()) {
                marketItemRepository.update(connection, updatedItem);
                purchaseRecordRepository.insert(connection, UUID.randomUUID(), marketItemID, buyerId, amount);
            } else {
                purchaseRecordRepository.deleteByMarketItem(connection, marketItemID);
                marketItemRepository.delete(connection, marketItemID);
            }

            return ServiceResult.success(new PurchaseResult(copyTag(item.getItemNBTCompound()), amount, newBalance, updatedItem));
        }).thenApply(result -> {
            if (result.isSuccess()) {
                if (result.getValue().removeFromCache()) {
                    marketItemCache.remove(marketItemID);
                } else {
                    marketItemCache.put(result.getValue().updatedItem());
                }
            }
            return result;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to buy market item.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<ListItemResult>> listItem(MarketItem marketItem) {
        if (!isPositiveMoney(marketItem.getPrice())) {
            return CompletableFuture.completedFuture(ServiceResult.<ListItemResult>failure("gui.wheatmarket.operation.invalid_price"));
        }
        if (marketItem.getAmount() <= 0) {
            return CompletableFuture.completedFuture(ServiceResult.<ListItemResult>failure("gui.wheatmarket.operation.invalid_amount"));
        }
        return transactionManager.executeAsync(connection -> {
            playerInfoRepository.ensureExists(connection, marketItem.getSellerID());
            marketItemRepository.insert(connection, marketItem);
            return ServiceResult.success(new ListItemResult(marketItem));
        }).thenApply(result -> {
            if (result.isSuccess()) {
                marketItemCache.put(result.getValue().marketItem());
            }
            return result;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to list market item.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<RestockResult>> restock(UUID actorId, boolean isOp, UUID marketItemID, int amount) {
        return updateExistingItem(actorId, isOp, marketItemID, item -> {
            if (amount <= 0) {
                return ServiceResult.failure("gui.wheatmarket.operation.invalid_amount");
            }
            if (item.isInfinite()) {
                return ServiceResult.failure("gui.wheatmarket.operation.invalid_amount");
            }
            MarketItem updated = copyOf(item);
            if (item.getAmount() > Integer.MAX_VALUE - amount) {
                return ServiceResult.failure("gui.wheatmarket.operation.invalid_amount");
            }
            updated.setAmount(item.getAmount() + amount);
            return ServiceResult.success(new RestockResult(amount, updated));
        });
    }

    public CompletableFuture<ServiceResult<ItemStackResult>> withdraw(UUID actorId, boolean isOp, UUID marketItemID, int amount) {
        return updateExistingItem(actorId, isOp, marketItemID, item -> {
            if (amount <= 0 || item.isInfinite() || amount > item.getAmount()) {
                return ServiceResult.failure("gui.wheatmarket.operation.invalid_amount");
            }
            MarketItem updated = copyOf(item);
            updated.setAmount(item.getAmount() - amount);
            return ServiceResult.success(new ItemStackResult(copyTag(item.getItemNBTCompound()), amount, updated, updated.getAmount() <= 0));
        });
    }

    public CompletableFuture<ServiceResult<MarketItemResult>> changePrice(UUID actorId, boolean isOp, UUID marketItemID, double price) {
        return updateExistingItem(actorId, isOp, marketItemID, item -> {
            if (!isPositiveMoney(price)) {
                return ServiceResult.failure("gui.wheatmarket.operation.invalid_price");
            }
            MarketItem updated = copyOf(item);
            updated.setPrice(price);
            return ServiceResult.success(new MarketItemResult(updated));
        });
    }

    public CompletableFuture<ServiceResult<ItemStackResult>> delist(UUID actorId, boolean isOp, UUID marketItemID) {
        return updateExistingItem(actorId, isOp, marketItemID, item ->
                ServiceResult.success(new ItemStackResult(copyTag(item.getItemNBTCompound()), item.getAmount(), copyOf(item), true))
        );
    }

    public CompletableFuture<ServiceResult<MarketItemResult>> toggleAdmin(UUID actorId, boolean isOp, UUID marketItemID) {
        return updateExistingItem(actorId, isOp, marketItemID, item -> {
            if (!isOp) {
                return ServiceResult.failure("gui.wheatmarket.operation.no_permission");
            }
            MarketItem updated = copyOf(item);
            updated.setIfAdmin(!item.getIfAdmin());
            return ServiceResult.success(new MarketItemResult(updated));
        });
    }

    public CompletableFuture<ServiceResult<MarketItemResult>> toggleInfinite(UUID actorId, boolean isOp, UUID marketItemID) {
        return updateExistingItem(actorId, isOp, marketItemID, item -> {
            if (!isOp) {
                return ServiceResult.failure("gui.wheatmarket.operation.no_permission");
            }
            MarketItem updated = copyOf(item);
            updated.setIfInfinite(!item.isInfinite());
            if (!updated.isInfinite() && updated.getAmount() <= 0) {
                updated.setAmount(1);
            }
            return ServiceResult.success(new MarketItemResult(updated));
        });
    }

    public CompletableFuture<ServiceResult<MarketItemResult>> setCooldown(UUID actorId, boolean isOp, UUID marketItemID,
                                                                          int cooldownAmount, int cooldownTimeInMinutes) {
        return updateExistingItem(actorId, isOp, marketItemID, item -> {
            if (!isOp) {
                return ServiceResult.failure("gui.wheatmarket.operation.no_permission");
            }
            MarketItem updated = copyOf(item);
            updated.setCooldownAmount(cooldownAmount);
            updated.setCooldownTimeInMinutes(cooldownTimeInMinutes);
            return ServiceResult.success(new MarketItemResult(updated));
        });
    }

    private <T extends MarketItemMutationResult> CompletableFuture<ServiceResult<T>> updateExistingItem(
            UUID actorId, boolean isOp, UUID marketItemID, MarketItemMutation<T> mutation) {
        return transactionManager.executeAsync(connection -> {
            MarketItem item = marketItemCache.get(marketItemID);
            if (item == null) {
                return ServiceResult.<T>failure("gui.wheatmarket.operation.item_not_found");
            }
            if (!item.getSellerID().equals(actorId) && !isOp) {
                return ServiceResult.<T>failure("gui.wheatmarket.operation.no_permission");
            }

            ServiceResult<T> mutationResult = mutation.apply(item);
            if (!mutationResult.isSuccess()) {
                return mutationResult;
            }

            T value = mutationResult.getValue();
            if (value.removeFromCache()) {
                purchaseRecordRepository.deleteByMarketItem(connection, marketItemID);
                marketItemRepository.delete(connection, marketItemID);
            } else {
                marketItemRepository.update(connection, value.updatedItem());
            }
            return mutationResult;
        }).thenApply(result -> {
            if (result.isSuccess()) {
                if (result.getValue().removeFromCache()) {
                    marketItemCache.remove(marketItemID);
                } else {
                    marketItemCache.put(result.getValue().updatedItem());
                }
            }
            return result;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to update market item.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    private MarketItem copyOf(MarketItem item) {
        return new MarketItem(
                item.getMarketItemID(),
                item.getItemID(),
                copyTag(item.getItemNBTCompound()),
                item.getSellerID(),
                item.getPrice(),
                item.getAmount(),
                item.getIfInfinite(),
                item.getListingTime(),
                item.getIfAdmin(),
                item.getIfSell(),
                item.getCooldownAmount(),
                item.getCooldownTimeInMinutes(),
                item.getTimeToExpire(),
                item.getLastTradeTime()
        );
    }

    private ServiceResult<Void> validateCooldownRestriction(Connection connection, MarketItem item, UUID buyerId, int amount) throws Exception {
        if (item.getCooldownTimeInMinutes() <= 0) {
            if (item.getCooldownAmount() > 0 && amount > item.getCooldownAmount()) {
                return ServiceResult.failure("gui.wheatmarket.operation.invalid_amount");
            }
            return ServiceResult.success(null);
        }

        long cooldownMs = (long) item.getCooldownTimeInMinutes() * 60 * 1000;
        Timestamp windowStart = new Timestamp(System.currentTimeMillis() - cooldownMs);

        if (item.getCooldownAmount() > 0) {
            int remainingAmount = resolveRemainingCooldownAmount(connection, item, buyerId);
            if (amount > remainingAmount) {
                return ServiceResult.failure(
                        "gui.wheatmarket.operation.cooldown_active",
                        String.valueOf(resolveCooldownRemainingMinutes(connection, item, buyerId, windowStart, cooldownMs))
                );
            }
            return ServiceResult.success(null);
        }

        Timestamp lastPurchase = purchaseRecordRepository.getLastPurchaseTime(connection, item.getMarketItemID(), buyerId);
        if (lastPurchase == null) {
            return ServiceResult.success(null);
        }

        long elapsed = System.currentTimeMillis() - lastPurchase.getTime();
        if (elapsed < cooldownMs) {
            long remainingMin = (cooldownMs - elapsed) / 60000 + 1;
            return ServiceResult.failure("gui.wheatmarket.operation.cooldown_active", String.valueOf(remainingMin));
        }
        return ServiceResult.success(null);
    }

    private int resolveRemainingCooldownAmount(Connection connection, MarketItem item, UUID buyerId) {
        if (buyerId == null || item.getCooldownAmount() <= 0 || item.getCooldownTimeInMinutes() <= 0) {
            return Math.max(0, item.getCooldownAmount());
        }
        try {
            long cooldownMs = (long) item.getCooldownTimeInMinutes() * 60 * 1000;
            Timestamp windowStart = new Timestamp(System.currentTimeMillis() - cooldownMs);
            int purchasedAmount = purchaseRecordRepository.getPurchasedAmountSince(connection, item.getMarketItemID(), buyerId, windowStart);
            return Math.max(0, item.getCooldownAmount() - purchasedAmount);
        } catch (Exception e) {
            WheatMarket.LOGGER.error("Failed to resolve remaining cooldown amount for market item {}.", item.getMarketItemID(), e);
            return Math.max(0, item.getCooldownAmount());
        }
    }

    private long resolveCooldownRemainingMinutes(Connection connection, MarketItem item, UUID buyerId,
                                                 Timestamp windowStart, long cooldownMs) throws Exception {
        Timestamp earliestPurchase = purchaseRecordRepository.getEarliestPurchaseTimeSince(connection, item.getMarketItemID(), buyerId, windowStart);
        if (earliestPurchase == null) {
            return Math.max(1L, item.getCooldownTimeInMinutes());
        }
        long remainingMs = earliestPurchase.getTime() + cooldownMs - System.currentTimeMillis();
        return Math.max(1L, remainingMs / 60000 + 1);
    }

    private boolean matchesSearchQuery(MarketItem item, String normalizedSearchQuery, Set<String> localizedSearchItemIds) {
        String itemId = item.getItemID() == null ? "" : item.getItemID().toLowerCase(Locale.ROOT);
        return itemId.contains(normalizedSearchQuery) || localizedSearchItemIds.contains(itemId);
    }

    private int compareMarketItems(MarketItem a, MarketItem b, int sortType, boolean sortAscending) {
        int comparison = switch (sortType) {
            case 1 -> safeItemId(a).compareToIgnoreCase(safeItemId(b));
            case 2 -> Long.compare(safeLastTradeTime(a), safeLastTradeTime(b));
            default -> Long.compare(safeListingTime(a), safeListingTime(b));
        };
        return sortAscending ? comparison : -comparison;
    }

    private boolean defaultSortAscending(int sortType) {
        return sortType == 1;
    }

    private String safeItemId(MarketItem item) {
        return item.getItemID() == null ? "" : item.getItemID();
    }

    private long safeListingTime(MarketItem item) {
        return item.getListingTime() == null ? 0L : item.getListingTime().getTime();
    }

    private long safeLastTradeTime(MarketItem item) {
        return item.getLastTradeTime() == null ? 0L : item.getLastTradeTime().getTime();
    }

    private Set<String> normalizeSearchItemIds(Collection<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Set.of();
        }
        return itemIds.stream()
                .filter(itemId -> itemId != null && !itemId.isBlank())
                .map(this::normalizeSearchQuery)
                .collect(Collectors.toSet());
    }

    private String normalizeSearchQuery(String searchQuery) {
        return searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
    }

    private CompoundTag copyTag(CompoundTag tag) {
        return tag == null ? null : tag.copy();
    }

    private boolean isPositiveMoney(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }

    private boolean isNonNegativeMoney(double amount) {
        return Double.isFinite(amount) && amount >= 0;
    }

    @FunctionalInterface
    private interface MarketItemMutation<T extends MarketItemMutationResult> {
        ServiceResult<T> apply(MarketItem item);
    }

    public record MarketListResult(List<MarketListEntry> items, int totalPages, int currentPage, double balance) {
    }

    public record MarketListEntry(MarketItem item, int remainingCooldownAmount) {
    }

    private record MarketListPageData(List<MarketListEntry> items, double balance) {
    }

    public record PurchaseResult(CompoundTag itemNbt, int amount, double newBalance,
                                 MarketItem updatedItem) implements MarketItemMutationResult {
        @Override
        public boolean removeFromCache() {
            return !updatedItem.isAvailable();
        }
    }

    public record ListItemResult(MarketItem marketItem) {
    }

    public record RestockResult(int amount, MarketItem updatedItem) implements MarketItemMutationResult {
        @Override
        public boolean removeFromCache() {
            return false;
        }
    }

    public record ItemStackResult(CompoundTag itemNbt, int amount, MarketItem updatedItem,
                                  boolean removeFromCache) implements MarketItemMutationResult {
    }

    public record MarketItemResult(MarketItem updatedItem) implements MarketItemMutationResult {
        @Override
        public boolean removeFromCache() {
            return false;
        }
    }

    private interface MarketItemMutationResult {
        MarketItem updatedItem();

        boolean removeFromCache();
    }
}

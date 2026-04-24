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

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MarketService {
    private static final int ITEMS_PER_PAGE = 10;

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
        Collection<MarketItem> allItems = marketItemCache.values();
        List<MarketItem> filtered = allItems.stream()
                .filter(item -> !item.isExpired())
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
                .filter(item -> {
                    if (searchQuery == null || searchQuery.isEmpty()) return true;
                    return item.getItemID().toLowerCase().contains(searchQuery.toLowerCase());
                })
                .sorted((a, b) -> {
                    switch (sortType) {
                        case 1:
                            return a.getItemID().compareToIgnoreCase(b.getItemID());
                        case 2:
                            long aTime = a.getLastTradeTime() != null ? a.getLastTradeTime().getTime() : 0;
                            long bTime = b.getLastTradeTime() != null ? b.getLastTradeTime().getTime() : 0;
                            return Long.compare(bTime, aTime);
                        default:
                            return b.getListingTime().compareTo(a.getListingTime());
                    }
                })
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = safePage * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, filtered.size());
        return ServiceResult.success(new MarketListResult(filtered.subList(fromIndex, toIndex), totalPages, safePage));
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
            if (amount <= 0 || amount > item.getAmount()) {
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
                Timestamp lastPurchase = purchaseRecordRepository.getLastPurchaseTime(connection, marketItemID, buyerId);
                if (lastPurchase != null) {
                    long cooldownMs = (long) item.getCooldownTimeInMinutes() * 60 * 1000;
                    long elapsed = System.currentTimeMillis() - lastPurchase.getTime();
                    if (elapsed < cooldownMs) {
                        long remainingMin = (cooldownMs - elapsed) / 60000 + 1;
                        return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.cooldown_active", String.valueOf(remainingMin));
                    }
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
            updatedItem.reduceAmount(amount);
            marketItemRepository.update(connection, updatedItem);
            purchaseRecordRepository.insert(connection, UUID.randomUUID(), marketItemID, buyerId, amount);

            return ServiceResult.success(new PurchaseResult(copyTag(item.getItemNBTCompound()), amount, newBalance, updatedItem));
        }).thenApply(result -> {
            if (result.isSuccess()) {
                marketItemCache.put(result.getValue().updatedItem());
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
            MarketItem updated = copyOf(item);
            updated.setAmount(item.getAmount() + amount);
            return ServiceResult.success(new RestockResult(amount, updated));
        });
    }

    public CompletableFuture<ServiceResult<ItemStackResult>> withdraw(UUID actorId, boolean isOp, UUID marketItemID, int amount) {
        return updateExistingItem(actorId, isOp, marketItemID, item -> {
            if (amount <= 0 || amount > item.getAmount()) {
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
            updated.setAmount(item.getAmount() == Integer.MAX_VALUE ? 1 : Integer.MAX_VALUE);
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
                item.getListingTime(),
                item.getIfAdmin(),
                item.getIfSell(),
                item.getCooldownAmount(),
                item.getCooldownTimeInMinutes(),
                item.getTimeToExpire(),
                item.getLastTradeTime()
        );
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

    public record MarketListResult(List<MarketItem> items, int totalPages, int currentPage) {
    }

    public record PurchaseResult(CompoundTag itemNbt, int amount, double newBalance,
                                 MarketItem updatedItem) implements MarketItemMutationResult {
        @Override
        public boolean removeFromCache() {
            return false;
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

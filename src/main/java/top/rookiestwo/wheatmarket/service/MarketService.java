package top.rookiestwo.wheatmarket.service;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.caches.MarketItemCache;
import top.rookiestwo.wheatmarket.database.entities.DeliveryItem;
import top.rookiestwo.wheatmarket.database.entities.MarketItem;
import top.rookiestwo.wheatmarket.database.repository.DeliveryItemRepository;
import top.rookiestwo.wheatmarket.database.repository.MarketItemRepository;
import top.rookiestwo.wheatmarket.database.repository.PlayerInfoRepository;
import top.rookiestwo.wheatmarket.database.repository.PurchaseRecordRepository;
import top.rookiestwo.wheatmarket.database.transaction.TransactionManager;
import top.rookiestwo.wheatmarket.service.result.ServiceResult;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MarketService {
    private static final int DEFAULT_ITEMS_PER_PAGE = 10;
    private static final int MIN_ITEMS_PER_PAGE = 1;
    private static final int MAX_ITEMS_PER_PAGE = 64;

    private final TransactionManager transactionManager;
    private final PlayerInfoRepository playerInfoRepository;
    private final MarketItemRepository marketItemRepository;
    private final PurchaseRecordRepository purchaseRecordRepository;
    private final DeliveryItemRepository deliveryItemRepository;
    private final MarketItemCache marketItemCache;
    private final Map<UUID, UUID> editLocks = new ConcurrentHashMap<>();

    public MarketService(TransactionManager transactionManager,
                         PlayerInfoRepository playerInfoRepository,
                         MarketItemRepository marketItemRepository,
                         PurchaseRecordRepository purchaseRecordRepository,
                         DeliveryItemRepository deliveryItemRepository,
                         MarketItemCache marketItemCache) {
        this.transactionManager = transactionManager;
        this.playerInfoRepository = playerInfoRepository;
        this.marketItemRepository = marketItemRepository;
        this.purchaseRecordRepository = purchaseRecordRepository;
        this.deliveryItemRepository = deliveryItemRepository;
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
                .filter(item -> !isEditLockedByOther(item.getMarketItemID(), buyerId))
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
            if (isEditLocked(marketItemID)) {
                return ServiceResult.<PurchaseResult>failure("gui.wheatmarket.operation.item_locked");
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

    public CompletableFuture<ServiceResult<BuyOrderFulfillmentResult>> fulfillBuyOrder(UUID supplierId, UUID marketItemID,
                                                                                       int amount, ItemStack suppliedStack,
                                                                                       HolderLookup.Provider registryAccess) {
        ItemStack suppliedSnapshot = suppliedStack == null ? ItemStack.EMPTY : suppliedStack.copy();
        return transactionManager.executeAsync(connection -> {
            MarketItem item = marketItemCache.get(marketItemID);
            if (item == null) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.item_not_found");
            }
            if (isEditLocked(marketItemID)) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.item_locked");
            }
            if (item.isExpired()) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.item_expired");
            }
            if (item.getIfSell()) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.not_buy_order");
            }
            if (amount <= 0 || amount > item.getAmount()) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.invalid_amount");
            }
            if (suppliedSnapshot.isEmpty() || suppliedSnapshot.getCount() < amount) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.insufficient_items");
            }
            if (!isPositiveMoney(item.getPrice())) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.invalid_price");
            }

            ItemStack expectedTemplate = parseMarketItemStack(item, registryAccess);
            if (expectedTemplate.isEmpty()) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.invalid_item_data");
            }
            ItemStack suppliedTemplate = suppliedSnapshot.copy();
            suppliedTemplate.setCount(1);
            expectedTemplate.setCount(1);
            if (!ItemStack.isSameItemSameComponents(expectedTemplate, suppliedTemplate)) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.item_mismatch");
            }

            playerInfoRepository.ensureExists(connection, supplierId);
            if (item.hasCooldownRestriction()) {
                ServiceResult<Void> cooldownCheck = validateCooldownRestriction(connection, item, supplierId, amount);
                if (!cooldownCheck.isSuccess()) {
                    return ServiceResult.<BuyOrderFulfillmentResult>failure(cooldownCheck.getMessageKey(), cooldownCheck.getMessageArgs());
                }
            }

            double totalCost = item.getPrice() * amount;
            if (!isPositiveMoney(totalCost)) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.invalid_price");
            }

            if (!item.getIfAdmin()) {
                double frozen = item.getFrozenBalance() != null ? item.getFrozenBalance() : 0.0;
                if (!isNonNegativeMoney(frozen) || frozen < totalCost) {
                    return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.insufficient_balance");
                }
            }

            double supplierBalance = playerInfoRepository.getBalance(connection, supplierId);
            double supplierNewBalance = supplierBalance + totalCost;
            if (!isNonNegativeMoney(supplierNewBalance)) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.invalid_price");
            }
            playerInfoRepository.setBalance(connection, supplierId, supplierNewBalance);

            MarketItem originalItem = copyOf(item);
            MarketItem updatedItem = copyOf(item);
            if (!updatedItem.reduceAmount(amount)) {
                return ServiceResult.<BuyOrderFulfillmentResult>failure("gui.wheatmarket.operation.invalid_amount");
            }
            if (!item.getIfAdmin()) {
                double newFrozen = (updatedItem.getFrozenBalance() != null ? updatedItem.getFrozenBalance() : 0.0) - totalCost;
                updatedItem.setFrozenBalance(Math.max(0.0, newFrozen));
            }

            UUID purchaseRecordId = UUID.randomUUID();
            DeliveryItem deliveryItem = new DeliveryItem(
                    UUID.randomUUID(),
                    item.getSellerID(),
                    marketItemID,
                    supplierId,
                    item.getItemID(),
                    copyTag(item.getItemNBTCompound()),
                    amount,
                    amount,
                    new Timestamp(System.currentTimeMillis()),
                    null
            );

            marketItemRepository.update(connection, updatedItem);
            deliveryItemRepository.insert(connection, deliveryItem);
            purchaseRecordRepository.insert(connection, purchaseRecordId, marketItemID, supplierId, amount);

            return ServiceResult.success(new BuyOrderFulfillmentResult(originalItem, updatedItem, deliveryItem,
                    purchaseRecordId, supplierId, item.getSellerID(), totalCost, supplierNewBalance,
                    Double.NaN, item.getIfAdmin(), updatedItem.getAmount() <= 0));
        }).thenApply(result -> {
            if (result.isSuccess()) {
                marketItemCache.put(result.getValue().updatedItem());
            }
            return result;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to fulfill buy order {} for supplier {}.", marketItemID, supplierId, e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<Void>> revertBuyOrderFulfillment(BuyOrderFulfillmentResult rollbackData) {
        return transactionManager.executeAsync(connection -> {
            DeliveryItem existingDelivery = deliveryItemRepository.findById(connection, rollbackData.deliveryItem().getDeliveryID());
            if (existingDelivery != null) {
                deliveryItemRepository.delete(connection, rollbackData.deliveryItem().getDeliveryID());
            }
            purchaseRecordRepository.delete(connection, rollbackData.purchaseRecordId());

            playerInfoRepository.ensureExists(connection, rollbackData.supplierId());
            double supplierBalance = playerInfoRepository.getBalance(connection, rollbackData.supplierId());
            double revertedSupplierBalance = supplierBalance - rollbackData.totalCost();
            if (!isNonNegativeMoney(revertedSupplierBalance)) {
                return ServiceResult.<Void>failure("gui.wheatmarket.operation.failed");
            }
            playerInfoRepository.setBalance(connection, rollbackData.supplierId(), revertedSupplierBalance);

            if (!rollbackData.adminOrder()) {
                playerInfoRepository.ensureExists(connection, rollbackData.receiverId());
                double receiverBalance = playerInfoRepository.getBalance(connection, rollbackData.receiverId());
                double revertedReceiverBalance = receiverBalance + rollbackData.totalCost();
                if (!isNonNegativeMoney(revertedReceiverBalance)) {
                    return ServiceResult.<Void>failure("gui.wheatmarket.operation.failed");
                }
                playerInfoRepository.setBalance(connection, rollbackData.receiverId(), revertedReceiverBalance);
            }

            if (marketItemRepository.get(connection, rollbackData.originalItem().getMarketItemID()) == null) {
                marketItemRepository.insert(connection, copyOf(rollbackData.originalItem()));
            } else {
                marketItemRepository.update(connection, copyOf(rollbackData.originalItem()));
            }
            return ServiceResult.<Void>success(null);
        }).thenApply(result -> {
            if (result.isSuccess()) {
                marketItemCache.put(copyOf(rollbackData.originalItem()));
            }
            return result;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to revert buy order fulfillment for {}.", rollbackData.originalItem().getMarketItemID(), e);
            return ServiceResult.<Void>failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<Void>> completeBuyOrderFulfillmentCleanup(UUID marketItemID) {
        return transactionManager.executeAsync(connection -> {
            MarketItem item = marketItemCache.get(marketItemID);
            if (item == null || item.getIfSell() || item.getAmount() > 0) {
                return ServiceResult.<Void>success(null);
            }
            purchaseRecordRepository.deleteByMarketItem(connection, marketItemID);
            marketItemRepository.delete(connection, marketItemID);
            return ServiceResult.<Void>success(null);
        }).thenApply(result -> {
            if (result.isSuccess()) {
                MarketItem current = marketItemCache.get(marketItemID);
                if (current != null && !current.getIfSell() && current.getAmount() <= 0) {
                    marketItemCache.remove(marketItemID);
                    editLocks.remove(marketItemID);
                }
            }
            return result;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to cleanup completed buy order {}.", marketItemID, e);
            return ServiceResult.<Void>failure("gui.wheatmarket.operation.failed");
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
            if (!Boolean.TRUE.equals(marketItem.getIfSell()) && !Boolean.TRUE.equals(marketItem.getIfAdmin())) {
                double totalCost = marketItem.getPrice() * marketItem.getAmount();
                if (!isPositiveMoney(totalCost)) {
                    return ServiceResult.<ListItemResult>failure("gui.wheatmarket.operation.invalid_price");
                }
                double balance = playerInfoRepository.getBalance(connection, marketItem.getSellerID());
                if (balance < totalCost) {
                    return ServiceResult.<ListItemResult>failure("gui.wheatmarket.operation.insufficient_balance");
                }
                double newBalance = balance - totalCost;
                if (!isNonNegativeMoney(newBalance)) {
                    return ServiceResult.<ListItemResult>failure("gui.wheatmarket.operation.invalid_price");
                }
                playerInfoRepository.setBalance(connection, marketItem.getSellerID(), newBalance);
                marketItem.setFrozenBalance(totalCost);
            }
            marketItemRepository.insert(connection, marketItem);
            return ServiceResult.success(new ListItemResult(marketItem));
        }).thenApply(result -> {
            if (result.isSuccess()) {
                marketItemCache.put(result.getValue().marketItem());
            }
            return result;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to list market item.", e);
            return ServiceResult.failure("gui.whatmarket.operation.failed");
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

    public CompletableFuture<ServiceResult<StockEditStartResult>> beginStockEdit(UUID actorId, boolean isOp,
                                                                                 UUID marketItemID,
                                                                                 int maxStockAmount) {
        return transactionManager.executeAsync(connection -> {
            MarketItem item = marketItemCache.get(marketItemID);
            if (item == null) {
                return ServiceResult.<StockEditStartResult>failure("gui.wheatmarket.operation.item_not_found");
            }
            if (!item.getSellerID().equals(actorId) && !isOp) {
                return ServiceResult.<StockEditStartResult>failure("gui.wheatmarket.operation.no_permission");
            }
            if (!isEditLockedBy(marketItemID, actorId)) {
                return ServiceResult.<StockEditStartResult>failure("gui.wheatmarket.operation.item_locked");
            }
            if (!Boolean.TRUE.equals(item.getIfSell()) || item.isInfinite()) {
                return ServiceResult.<StockEditStartResult>failure("gui.wheatmarket.operation.invalid_amount");
            }
            int currentAmount = item.getAmount();
            if (currentAmount < 0 || currentAmount > maxStockAmount) {
                return ServiceResult.<StockEditStartResult>failure("gui.wheatmarket.operation.invalid_amount");
            }
            return ServiceResult.success(new StockEditStartResult(copyTag(item.getItemNBTCompound()), currentAmount, copyOf(item)));
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to begin stock edit.", e);
            return ServiceResult.<StockEditStartResult>failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<StockEditFinishResult>> finishStockEdit(UUID actorId, boolean isOp,
                                                                                   UUID marketItemID,
                                                                                   CompoundTag expectedItemNbt,
                                                                                   int finalAmount,
                                                                                   int maxStockAmount) {
        return updateExistingItem(actorId, isOp, marketItemID, item -> {
            if (!isEditLockedBy(marketItemID, actorId)) {
                return ServiceResult.failure("gui.wheatmarket.operation.item_locked");
            }
            if (!Boolean.TRUE.equals(item.getIfSell()) || item.isInfinite()) {
                return ServiceResult.failure("gui.wheatmarket.operation.invalid_amount");
            }
            if (finalAmount < 0 || finalAmount > maxStockAmount) {
                return ServiceResult.failure("gui.wheatmarket.operation.invalid_amount");
            }
            if (expectedItemNbt == null || !expectedItemNbt.equals(item.getItemNBTCompound())) {
                return ServiceResult.failure("gui.wheatmarket.operation.invalid_amount");
            }
            MarketItem updated = copyOf(item);
            updated.setAmount(finalAmount);
            return ServiceResult.success(new StockEditFinishResult(finalAmount, updated, finalAmount <= 0));
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
        return transactionManager.executeAsync(connection -> {
            MarketItem item = marketItemCache.get(marketItemID);
            if (item == null) {
                return ServiceResult.<ItemStackResult>failure("gui.wheatmarket.operation.item_not_found");
            }
            if (!item.getSellerID().equals(actorId) && !isOp) {
                return ServiceResult.<ItemStackResult>failure("gui.wheatmarket.operation.no_permission");
            }
            if (isEditLockedByOther(marketItemID, actorId)) {
                return ServiceResult.<ItemStackResult>failure("gui.wheatmarket.operation.item_locked");
            }
            if (!Boolean.TRUE.equals(item.getIfSell()) && item.getFrozenBalance() != null && item.getFrozenBalance() > 0) {
                double balance = playerInfoRepository.getBalance(connection, item.getSellerID());
                playerInfoRepository.setBalance(connection, item.getSellerID(), balance + item.getFrozenBalance());
            }
            purchaseRecordRepository.deleteByMarketItem(connection, marketItemID);
            marketItemRepository.delete(connection, marketItemID);
            return ServiceResult.success(new ItemStackResult(copyTag(item.getItemNBTCompound()), item.getAmount(), copyOf(item), true));
        }).thenApply(result -> {
            if (result.isSuccess()) {
                marketItemCache.remove(marketItemID);
            }
            return result;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to delist item.", e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<Integer> cleanupExpiredOrders() {
        return transactionManager.executeAsync(connection -> {
            int cleaned = 0;
            for (MarketItem item : marketItemCache.values()) {
                if (!item.isExpired()) {
                    continue;
                }
                if (!Boolean.TRUE.equals(item.getIfSell())) {
                    if (!Boolean.TRUE.equals(item.getIfAdmin())) {
                        double frozen = item.getFrozenBalance() != null ? item.getFrozenBalance() : 0.0;
                        if (frozen > 0) {
                            double balance = playerInfoRepository.getBalance(connection, item.getSellerID());
                            playerInfoRepository.setBalance(connection, item.getSellerID(), balance + frozen);
                        }
                    }
                } else if (!Boolean.TRUE.equals(item.getIfAdmin()) && !item.isInfinite()) {
                    DeliveryItem delivery = new DeliveryItem(
                            UUID.randomUUID(),
                            item.getSellerID(),
                            item.getMarketItemID(),
                            null,
                            item.getItemID(),
                            copyTag(item.getItemNBTCompound()),
                            item.getAmount(),
                            item.getAmount(),
                            new Timestamp(System.currentTimeMillis()),
                            null
                    );
                    deliveryItemRepository.insert(connection, delivery);
                }
                purchaseRecordRepository.deleteByMarketItem(connection, item.getMarketItemID());
                marketItemRepository.delete(connection, item.getMarketItemID());
                marketItemCache.remove(item.getMarketItemID());
                cleaned++;
            }
            if (cleaned > 0) {
                WheatMarket.LOGGER.info("Cleaned up {} expired orders.", cleaned);
            }
            return cleaned;
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to cleanup expired orders.", e);
            return 0;
        });
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

    public CompletableFuture<ServiceResult<MarketItemResult>> toggleInfiniteDuration(UUID actorId, boolean isOp, UUID marketItemID) {
        return updateExistingItem(actorId, isOp, marketItemID, item -> {
            if (!isOp) {
                return ServiceResult.failure("gui.wheatmarket.operation.no_permission");
            }
            MarketItem updated = copyOf(item);
            updated.setIfInfiniteDuration(!item.isInfiniteDuration());
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

    public CompletableFuture<ServiceResult<MarketItemResult>> setTimeToExpire(UUID actorId, boolean isOp, UUID marketItemID, long timeToExpire) {
        return updateExistingItem(actorId, isOp, marketItemID, item -> {
            if (!isOp) {
                return ServiceResult.failure("gui.wheatmarket.operation.no_permission");
            }
            MarketItem updated = copyOf(item);
            updated.setTimeToExpire(timeToExpire);
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
            if (isEditLockedByOther(marketItemID, actorId)) {
                return ServiceResult.<T>failure("gui.wheatmarket.operation.item_locked");
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
                    editLocks.remove(marketItemID);
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
                item.getLastTradeTime(),
                item.getFrozenBalance(),
                item.isInfiniteDuration()
        );
    }

    public ServiceResult<Void> acquireItemEditLock(UUID actorId, boolean isOp, UUID marketItemID) {
        MarketItem item = marketItemCache.get(marketItemID);
        if (item == null) {
            return ServiceResult.failure("gui.wheatmarket.operation.item_not_found");
        }
        if (item.isExpired()) {
            return ServiceResult.failure("gui.wheatmarket.operation.item_expired");
        }
        if (!item.getSellerID().equals(actorId) && !isOp) {
            return ServiceResult.failure("gui.wheatmarket.operation.no_permission");
        }

        UUID existing = editLocks.putIfAbsent(marketItemID, actorId);
        if (existing != null && !existing.equals(actorId)) {
            return ServiceResult.failure("gui.wheatmarket.operation.item_locked");
        }
        return ServiceResult.success(null);
    }

    public void releaseItemEditLock(UUID actorId, UUID marketItemID) {
        if (marketItemID != null && actorId != null) {
            editLocks.remove(marketItemID, actorId);
        }
    }

    public void releaseItemEditLocks(UUID actorId) {
        if (actorId == null) {
            return;
        }
        editLocks.entrySet().removeIf(entry -> actorId.equals(entry.getValue()));
    }

    private boolean isEditLocked(UUID marketItemID) {
        return editLocks.containsKey(marketItemID);
    }

    private boolean isEditLockedBy(UUID marketItemID, UUID actorId) {
        UUID editingPlayerId = editLocks.get(marketItemID);
        return editingPlayerId != null && editingPlayerId.equals(actorId);
    }

    private boolean isEditLockedByOther(UUID marketItemID, UUID actorId) {
        UUID editingPlayerId = editLocks.get(marketItemID);
        return editingPlayerId != null && (actorId == null || !editingPlayerId.equals(actorId));
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

    private ItemStack parseMarketItemStack(MarketItem item, HolderLookup.Provider registryAccess) {
        CompoundTag tag = item.getItemNBTCompound();
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(registryAccess, tag);
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

    public record BuyOrderFulfillmentResult(MarketItem originalItem, MarketItem updatedItem,
                                            DeliveryItem deliveryItem, UUID purchaseRecordId,
                                            UUID supplierId, UUID receiverId, double totalCost,
                                            double supplierNewBalance, double receiverNewBalance,
                                            boolean adminOrder, boolean cleanupRequired) {
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

    public record StockEditStartResult(CompoundTag itemNbt, int originalAmount,
                                       MarketItem updatedItem) implements MarketItemMutationResult {
        @Override
        public boolean removeFromCache() {
            return false;
        }
    }

    public record StockEditFinishResult(int finalAmount, MarketItem updatedItem,
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

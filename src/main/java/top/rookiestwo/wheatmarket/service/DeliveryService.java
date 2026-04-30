package top.rookiestwo.wheatmarket.service;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.database.entities.DeliveryItem;
import top.rookiestwo.wheatmarket.database.repository.DeliveryItemRepository;
import top.rookiestwo.wheatmarket.database.transaction.TransactionManager;
import top.rookiestwo.wheatmarket.service.result.ServiceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DeliveryService {
    private final TransactionManager transactionManager;
    private final DeliveryItemRepository deliveryItemRepository;

    public DeliveryService(TransactionManager transactionManager, DeliveryItemRepository deliveryItemRepository) {
        this.transactionManager = transactionManager;
        this.deliveryItemRepository = deliveryItemRepository;
    }

    public CompletableFuture<ServiceResult<List<DeliveryItem>>> listDeliveries(UUID receiverId) {
        return transactionManager.executeAsync(connection ->
                ServiceResult.success(deliveryItemRepository.findByReceiver(connection, receiverId).stream()
                        .map(this::copyOf)
                        .toList())
        ).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to list deliveries for {}.", receiverId, e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    public CompletableFuture<ServiceResult<ClaimDeliveryResult>> claimDelivery(UUID receiverId, UUID deliveryId,
                                                                               List<ItemStack> inventorySnapshot,
                                                                               HolderLookup.Provider registryAccess) {
        List<ItemStack> safeInventorySnapshot = copyInventorySnapshot(inventorySnapshot);
        return transactionManager.executeAsync(connection -> {
            DeliveryItem delivery = deliveryItemRepository.findById(connection, deliveryId);
            if (delivery == null) {
                return ServiceResult.<ClaimDeliveryResult>failure("gui.wheatmarket.operation.delivery_not_found");
            }
            if (!receiverId.equals(delivery.getReceiverID())) {
                return ServiceResult.<ClaimDeliveryResult>failure("gui.wheatmarket.operation.no_permission");
            }
            if (delivery.getRemainingAmount() <= 0) {
                return ServiceResult.<ClaimDeliveryResult>failure("gui.wheatmarket.operation.delivery_not_found");
            }

            ItemStack deliveryStack = parseDeliveryStack(registryAccess, delivery);
            if (deliveryStack.isEmpty()) {
                return ServiceResult.<ClaimDeliveryResult>failure("gui.wheatmarket.operation.invalid_item_data");
            }
            if (!canFullyStore(safeInventorySnapshot, deliveryStack)) {
                return ServiceResult.<ClaimDeliveryResult>failure("gui.wheatmarket.operation.delivery_inventory_full");
            }

            int claimedAmount = delivery.getRemainingAmount();
            deliveryItemRepository.delete(connection, deliveryId);
            return ServiceResult.success(new ClaimDeliveryResult(copyOf(delivery), copyTag(delivery.getItemNBTCompound()),
                    claimedAmount, 0, true));
        }).exceptionally(e -> {
            WheatMarket.LOGGER.error("Failed to claim delivery {} for {}.", deliveryId, receiverId, e);
            return ServiceResult.failure("gui.wheatmarket.operation.failed");
        });
    }

    private DeliveryItem copyOf(DeliveryItem item) {
        return new DeliveryItem(
                item.getDeliveryID(),
                item.getReceiverID(),
                item.getMarketItemID(),
                item.getSourcePlayerID(),
                item.getItemID(),
                copyTag(item.getItemNBTCompound()),
                item.getAmount(),
                item.getRemainingAmount(),
                item.getCreatedTime(),
                item.getClaimedTime()
        );
    }

    private CompoundTag copyTag(CompoundTag tag) {
        return tag == null ? null : tag.copy();
    }

    private List<ItemStack> copyInventorySnapshot(List<ItemStack> inventorySnapshot) {
        if (inventorySnapshot == null || inventorySnapshot.isEmpty()) {
            return List.of();
        }
        List<ItemStack> copy = new ArrayList<>(inventorySnapshot.size());
        for (ItemStack stack : inventorySnapshot) {
            copy.add(stack == null ? ItemStack.EMPTY : stack.copy());
        }
        return copy;
    }

    private ItemStack parseDeliveryStack(HolderLookup.Provider registryAccess, DeliveryItem delivery) {
        CompoundTag tag = delivery.getItemNBTCompound();
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ItemStack.parseOptional(registryAccess, tag);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        stack.setCount(delivery.getRemainingAmount());
        return stack;
    }

    private boolean canFullyStore(List<ItemStack> inventorySnapshot, ItemStack deliveryStack) {
        int remaining = deliveryStack.getCount();
        if (remaining <= 0) {
            return false;
        }

        int maxStackSize = Math.max(1, deliveryStack.getMaxStackSize());
        for (ItemStack slotStack : inventorySnapshot) {
            if (slotStack == null || slotStack.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(slotStack, deliveryStack)) {
                continue;
            }
            remaining -= Math.max(0, maxStackSize - slotStack.getCount());
            if (remaining <= 0) {
                return true;
            }
        }

        for (ItemStack slotStack : inventorySnapshot) {
            if (slotStack == null || !slotStack.isEmpty()) {
                continue;
            }
            remaining -= maxStackSize;
            if (remaining <= 0) {
                return true;
            }
        }

        return remaining <= 0;
    }

    public record ClaimDeliveryResult(DeliveryItem delivery, CompoundTag itemNbt, int amount,
                                      int remainingAmount, boolean deleteWholeRecord) {
    }
}

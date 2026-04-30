package top.rookiestwo.wheatmarket.client.gui;

import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;

import java.util.UUID;
import java.util.function.Consumer;

public record ItemSelectionRequest(
        ItemSelectionPurpose purpose,
        ItemSelectionMode mode,
        ItemStack initialSelection,
        int baselineAmount,
        ItemStack lockedStackTemplate,
        boolean allowEmpty,
        Consumer<ItemSelectionResult> onConfirm,
        UUID marketItemId
) {
    public ItemSelectionRequest {
        purpose = purpose == null ? ItemSelectionPurpose.LIST_SELL : purpose;
        mode = mode == null ? purpose.defaultMode() : mode;
        initialSelection = copyOrEmpty(initialSelection);
        baselineAmount = Math.max(0, baselineAmount);
        lockedStackTemplate = templateCopy(lockedStackTemplate);
        onConfirm = onConfirm == null ? result -> {
        } : onConfirm;
    }

    public ItemSelectionRequest(ItemSelectionPurpose purpose,
                                ItemSelectionMode mode,
                                ItemStack initialSelection,
                                int baselineAmount,
                                ItemStack lockedStackTemplate,
                                boolean allowEmpty,
                                Consumer<ItemSelectionResult> onConfirm) {
        this(purpose, mode, initialSelection, baselineAmount, lockedStackTemplate, allowEmpty, onConfirm, null);
    }

    public static ItemSelectionRequest listSell(Consumer<ItemSelectionResult> onConfirm) {
        return new ItemSelectionRequest(
                ItemSelectionPurpose.LIST_SELL,
                ItemSelectionMode.TRANSFER,
                ItemStack.EMPTY,
                0,
                ItemStack.EMPTY,
                true,
                onConfirm,
                null
        );
    }

    public static ItemSelectionRequest stockEdit(UUID marketItemId, ItemStack stack, int currentStock,
                                                 Consumer<ItemSelectionResult> onConfirm) {
        ItemStack initialStock = copyOrEmpty(stack);
        int safeStock = Math.max(0, currentStock);
        if (!initialStock.isEmpty() && safeStock > 0) {
            initialStock.setCount(safeStock);
        } else {
            initialStock = ItemStack.EMPTY;
        }
        return new ItemSelectionRequest(
                ItemSelectionPurpose.STOCK_EDIT,
                ItemSelectionMode.STOCK_EDIT,
                initialStock,
                safeStock,
                stack,
                true,
                onConfirm,
                marketItemId
        );
    }

    private static ItemStack copyOrEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    private static ItemStack templateCopy(ItemStack stack) {
        ItemStack copy = copyOrEmpty(stack);
        if (!copy.isEmpty()) {
            copy.setCount(1);
        }
        return copy;
    }

    public boolean hasLockedStackTemplate() {
        return !lockedStackTemplate.isEmpty();
    }
}

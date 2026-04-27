package top.rookiestwo.wheatmarket.client.gui;

import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;

import java.util.function.Consumer;

public record ItemSelectionRequest(
        ItemSelectionPurpose purpose,
        ItemSelectionMode mode,
        ItemStack initialSelection,
        int baselineAmount,
        ItemStack lockedStackTemplate,
        boolean allowEmpty,
        Consumer<ItemSelectionResult> onConfirm
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

    public static ItemSelectionRequest listSell(Consumer<ItemSelectionResult> onConfirm) {
        return new ItemSelectionRequest(
                ItemSelectionPurpose.LIST_SELL,
                ItemSelectionMode.TRANSFER,
                ItemStack.EMPTY,
                0,
                ItemStack.EMPTY,
                true,
                onConfirm
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

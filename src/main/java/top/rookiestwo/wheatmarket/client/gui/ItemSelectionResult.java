package top.rookiestwo.wheatmarket.client.gui;

import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;

public record ItemSelectionResult(
        ItemSelectionPurpose purpose,
        ItemSelectionMode mode,
        ItemStack selectedStack,
        int totalAmount,
        int baselineAmount,
        int deltaAmount,
        boolean empty
) {
    public ItemSelectionResult {
        purpose = purpose == null ? ItemSelectionPurpose.LIST_SELL : purpose;
        mode = mode == null ? purpose.defaultMode() : mode;
        totalAmount = Math.max(0, totalAmount);
        baselineAmount = Math.max(0, baselineAmount);
        selectedStack = templateCopy(selectedStack, totalAmount);
        deltaAmount = totalAmount - baselineAmount;
        empty = selectedStack.isEmpty() || totalAmount == 0;
    }

    private static ItemStack templateCopy(ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}

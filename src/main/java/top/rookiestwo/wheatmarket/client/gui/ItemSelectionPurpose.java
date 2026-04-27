package top.rookiestwo.wheatmarket.client.gui;

import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;

public enum ItemSelectionPurpose {
    LIST_SELL(ItemSelectionMode.TRANSFER),
    LIST_BUY(ItemSelectionMode.SAMPLE),
    EDIT_LISTING_STOCK(ItemSelectionMode.TRANSFER);

    private final ItemSelectionMode defaultMode;

    ItemSelectionPurpose(ItemSelectionMode defaultMode) {
        this.defaultMode = defaultMode;
    }

    public ItemSelectionMode defaultMode() {
        return defaultMode;
    }
}

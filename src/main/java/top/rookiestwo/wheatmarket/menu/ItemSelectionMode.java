package top.rookiestwo.wheatmarket.menu;

public enum ItemSelectionMode {
    DISABLED(0, false),
    TRANSFER(1, true),
    SAMPLE(2, false),
    STOCK_EDIT(3, true);

    private final int networkId;
    private final boolean consumesItems;

    ItemSelectionMode(int networkId, boolean consumesItems) {
        this.networkId = networkId;
        this.consumesItems = consumesItems;
    }

    public static ItemSelectionMode fromNetworkId(int networkId) {
        for (ItemSelectionMode mode : values()) {
            if (mode.networkId == networkId) {
                return mode;
            }
        }
        return DISABLED;
    }

    public int getNetworkId() {
        return networkId;
    }

    public boolean consumesItems() {
        return consumesItems;
    }
}

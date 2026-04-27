package top.rookiestwo.wheatmarket.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.rookiestwo.wheatmarket.WheatMarketRegistry;

public class WheatMarketMenu extends AbstractContainerMenu {
    public static final int ITEM_SELECTION_SLOT_INDEX = 0;
    public static final int PLAYER_INVENTORY_SLOT_START = 1;
    public static final int PLAYER_INVENTORY_SLOT_COUNT = 36;
    public static final int PLAYER_INVENTORY_SLOT_END = PLAYER_INVENTORY_SLOT_START + PLAYER_INVENTORY_SLOT_COUNT;
    public static final int ITEM_SELECTION_SLOT_X = 80;
    public static final int ITEM_SELECTION_SLOT_Y = 36;
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 84;
    public static final int HOTBAR_Y = 142;
    public static final int ITEM_SELECTION_MAX_AMOUNT = 999_999;

    private final ItemSelectionContainer itemSelectionContainer = new ItemSelectionContainer();
    private ItemSelectionMode itemSelectionMode = ItemSelectionMode.DISABLED;
    private ItemStack lockedItemSelectionTemplate = ItemStack.EMPTY;
    private int playerOwnedSelectionAmount;
    private Player player = null;

    public WheatMarketMenu(int containerId, Inventory inventory) {
        super(WheatMarketRegistry.WHEAT_MARKET_MENU.get(), containerId);
        this.player = inventory.player;
        addItemSelectionSlots(inventory);
    }

    public WheatMarketMenu(int containerId, Inventory inventory, Player player) {
        this(containerId, inventory);
        this.player = player;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        if (itemSelectionMode == ItemSelectionMode.DISABLED || i < 0 || i >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(i);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack originalStack = stack.copy();
        if (i == ITEM_SELECTION_SLOT_INDEX) {
            if (!itemSelectionMode.consumesItems()) {
                return ItemStack.EMPTY;
            }
            int movableAmount = Math.min(stack.getCount(), playerOwnedSelectionAmount);
            if (movableAmount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack moving = stack.copy();
            moving.setCount(movableAmount);
            if (moveItemStackTo(moving, PLAYER_INVENTORY_SLOT_START, PLAYER_INVENTORY_SLOT_END, true)) {
                int movedAmount = movableAmount - moving.getCount();
                shrinkSelection(movedAmount, movedAmount);
                return originalStack;
            }
            return ItemStack.EMPTY;
        }

        if (itemSelectionMode == ItemSelectionMode.SAMPLE) {
            setSample(stack);
            return ItemStack.EMPTY;
        }

        int moved = addToSelection(stack, stack.getCount());
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        stack.shrink(moved);
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return originalStack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == ITEM_SELECTION_SLOT_INDEX && itemSelectionMode == ItemSelectionMode.SAMPLE
                && clickType == ClickType.PICKUP) {
            handleSampleSlotClick(button);
            return;
        }
        if (slotId == ITEM_SELECTION_SLOT_INDEX && itemSelectionMode == ItemSelectionMode.TRANSFER
                && clickType == ClickType.PICKUP) {
            handleTransferSlotClick(button);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        itemSelectionMode = ItemSelectionMode.DISABLED;
        lockedItemSelectionTemplate = ItemStack.EMPTY;
        returnOrClearSelection(player);
        super.removed(player);
    }

    public void configureItemSelection(ItemSelectionMode mode, ItemStack initialSelection, ItemStack lockedTemplate, Player player) {
        ItemSelectionMode newMode = mode == null ? ItemSelectionMode.DISABLED : mode;
        returnOrClearSelection(player);
        this.itemSelectionMode = newMode;
        this.lockedItemSelectionTemplate = newMode == ItemSelectionMode.DISABLED
                ? ItemStack.EMPTY
                : templateCopy(lockedTemplate);
        setInitialSelection(initialSelection);
        broadcastChanges();
    }

    public void setItemSelectionMode(ItemSelectionMode mode, Player player) {
        ItemSelectionMode newMode = mode == null ? ItemSelectionMode.DISABLED : mode;
        if (this.itemSelectionMode == newMode) {
            if (newMode == ItemSelectionMode.DISABLED) {
                lockedItemSelectionTemplate = ItemStack.EMPTY;
                returnOrClearSelection(player);
                broadcastChanges();
            }
            return;
        }

        this.itemSelectionMode = newMode;
        if (newMode == ItemSelectionMode.DISABLED) {
            lockedItemSelectionTemplate = ItemStack.EMPTY;
        }
        returnOrClearSelection(player);
        broadcastChanges();
    }

    public void preserveItemSelectionForDraft(Player player) {
        itemSelectionMode = ItemSelectionMode.DISABLED;
        lockedItemSelectionTemplate = ItemStack.EMPTY;
        returnCarriedItem(player);
        clampPlayerOwnedSelectionAmount();
        broadcastChanges();
    }

    public ItemSelectionMode getItemSelectionMode() {
        return itemSelectionMode;
    }

    public ItemStack getSelectedItem() {
        return itemSelectionContainer.getItem(0);
    }

    public void clearItemSelection(Player player) {
        returnOrClearSelection(player);
        broadcastChanges();
    }

    public int getSelectedAmount() {
        ItemStack selectedItem = getSelectedItem();
        return selectedItem.isEmpty() ? 0 : selectedItem.getCount();
    }

    public boolean hasSelectedItem() {
        return !getSelectedItem().isEmpty();
    }

    public boolean consumeSelectedItemsForListing(ItemStack expectedTemplate, int amount) {
        if (expectedTemplate == null || expectedTemplate.isEmpty() || amount <= 0) {
            return false;
        }

        ItemStack selected = itemSelectionContainer.getItem(0);
        if (selected.isEmpty()
                || !ItemStack.isSameItemSameComponents(selected, expectedTemplate)
                || selected.getCount() < amount
                || playerOwnedSelectionAmount < amount) {
            return false;
        }

        shrinkSelection(amount, amount);
        broadcastChanges();
        return true;
    }

    public void clearPreservedSampleSelection(ItemStack expectedTemplate) {
        ItemStack selected = itemSelectionContainer.getItem(0);
        if (selected.isEmpty()
                || expectedTemplate == null
                || expectedTemplate.isEmpty()
                || playerOwnedSelectionAmount > 0
                || !ItemStack.isSameItemSameComponents(selected, expectedTemplate)) {
            return;
        }

        itemSelectionContainer.setItem(0, ItemStack.EMPTY);
        playerOwnedSelectionAmount = 0;
        broadcastChanges();
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private void addItemSelectionSlots(Inventory inventory) {
        addSlot(new ItemSelectionSlot(itemSelectionContainer, 0, ITEM_SELECTION_SLOT_X, ITEM_SELECTION_SLOT_Y));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventoryIndex = column + row * 9 + 9;
                addSlot(new PlayerInventorySlot(inventory, inventoryIndex,
                        PLAYER_INVENTORY_X + column * 18,
                        PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new PlayerInventorySlot(inventory, column,
                    PLAYER_INVENTORY_X + column * 18,
                    HOTBAR_Y));
        }
    }

    private void handleSampleSlotClick(int button) {
        ItemStack carried = getCarried();
        if (!carried.isEmpty()) {
            setSample(carried);
            return;
        }
        if (button == 1) {
            playerOwnedSelectionAmount = 0;
            itemSelectionContainer.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
        }
    }

    private void handleTransferSlotClick(int button) {
        ItemStack carried = getCarried();
        ItemStack selected = itemSelectionContainer.getItem(0);
        if (carried.isEmpty()) {
            if (selected.isEmpty()) {
                return;
            }

            int requestedPickupAmount = button == 1
                    ? Math.max(1, (selected.getCount() + 1) / 2)
                    : selected.getCount();
            int pickupAmount = Math.min(Math.min(requestedPickupAmount, selected.getMaxStackSize()), playerOwnedSelectionAmount);
            if (pickupAmount <= 0) {
                // Non-player-owned prefill is an editable field value; clearing it must not mint an item.
                shrinkSelection(requestedPickupAmount, 0);
                broadcastChanges();
                return;
            }
            ItemStack pickedUp = selected.copy();
            pickedUp.setCount(pickupAmount);
            shrinkSelection(pickupAmount, pickupAmount);
            setCarried(pickedUp);
            broadcastChanges();
            return;
        }

        int requestedAmount = button == 1 ? 1 : carried.getCount();
        int moved = addToSelection(carried, requestedAmount);
        if (moved > 0) {
            carried.shrink(moved);
            if (carried.isEmpty()) {
                setCarried(ItemStack.EMPTY);
            }
            broadcastChanges();
        }
    }

    private void setSample(ItemStack stack) {
        if (stack.isEmpty() || !matchesLockedTemplate(stack)) {
            return;
        }
        ItemStack sample = stack.copy();
        sample.setCount(1);
        playerOwnedSelectionAmount = 0;
        itemSelectionContainer.setItem(0, sample);
        broadcastChanges();
    }

    private int addToSelection(ItemStack source, int requestedAmount) {
        if (source.isEmpty() || requestedAmount <= 0 || !matchesLockedTemplate(source)) {
            return 0;
        }

        ItemStack selected = itemSelectionContainer.getItem(0);
        if (selected.isEmpty()) {
            int moved = Math.min(requestedAmount, ITEM_SELECTION_MAX_AMOUNT);
            ItemStack copy = source.copy();
            copy.setCount(moved);
            playerOwnedSelectionAmount = moved;
            itemSelectionContainer.setItem(0, copy);
            return moved;
        }

        if (!ItemStack.isSameItemSameComponents(selected, source)) {
            return 0;
        }

        int space = ITEM_SELECTION_MAX_AMOUNT - selected.getCount();
        if (space <= 0) {
            return 0;
        }
        int moved = Math.min(requestedAmount, space);
        selected.grow(moved);
        playerOwnedSelectionAmount += moved;
        clampPlayerOwnedSelectionAmount();
        itemSelectionContainer.setChanged();
        return moved;
    }

    private boolean canAddToSelection(ItemStack stack) {
        if (stack.isEmpty() || itemSelectionMode != ItemSelectionMode.TRANSFER) {
            return false;
        }
        if (!matchesLockedTemplate(stack)) {
            return false;
        }
        ItemStack selected = itemSelectionContainer.getItem(0);
        return selected.isEmpty() || ItemStack.isSameItemSameComponents(selected, stack);
    }

    private void setInitialSelection(ItemStack initialSelection) {
        playerOwnedSelectionAmount = 0;
        if (itemSelectionMode == ItemSelectionMode.DISABLED || initialSelection == null || initialSelection.isEmpty()) {
            itemSelectionContainer.setItem(0, ItemStack.EMPTY);
            return;
        }
        if (!matchesLockedTemplate(initialSelection)) {
            itemSelectionContainer.setItem(0, ItemStack.EMPTY);
            return;
        }
        ItemStack initial = initialSelection.copy();
        if (initial.getCount() > ITEM_SELECTION_MAX_AMOUNT) {
            initial.setCount(ITEM_SELECTION_MAX_AMOUNT);
        }
        itemSelectionContainer.setItem(0, initial);
    }

    private boolean matchesLockedTemplate(ItemStack stack) {
        return lockedItemSelectionTemplate.isEmpty() || ItemStack.isSameItemSameComponents(lockedItemSelectionTemplate, stack);
    }

    private ItemStack templateCopy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private void shrinkSelection(int amount, int playerOwnedAmount) {
        if (amount <= 0) {
            return;
        }
        ItemStack selected = itemSelectionContainer.getItem(0);
        if (selected.isEmpty()) {
            playerOwnedSelectionAmount = 0;
            return;
        }
        selected.shrink(Math.min(amount, selected.getCount()));
        playerOwnedSelectionAmount = Math.max(0, playerOwnedSelectionAmount - Math.max(0, playerOwnedAmount));
        if (selected.isEmpty()) {
            itemSelectionContainer.setItem(0, ItemStack.EMPTY);
            playerOwnedSelectionAmount = 0;
        } else {
            clampPlayerOwnedSelectionAmount();
            itemSelectionContainer.setChanged();
        }
    }

    private void clampPlayerOwnedSelectionAmount() {
        playerOwnedSelectionAmount = Math.max(0, Math.min(playerOwnedSelectionAmount, getSelectedAmount()));
    }

    private void returnOrClearSelection(Player player) {
        returnCarriedItem(player);
        ItemStack selected = itemSelectionContainer.removeItemNoUpdate(0);
        if (selected.isEmpty()) {
            playerOwnedSelectionAmount = 0;
            return;
        }
        int returnAmount = Math.min(selected.getCount(), playerOwnedSelectionAmount);
        if (returnAmount > 0 && player instanceof ServerPlayer serverPlayer) {
            ItemStack returned = selected.copy();
            returned.setCount(returnAmount);
            serverPlayer.getInventory().placeItemBackInInventory(returned);
        }
        playerOwnedSelectionAmount = 0;
    }

    private void returnCarriedItem(Player player) {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return;
        }
        setCarried(ItemStack.EMPTY);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getInventory().placeItemBackInInventory(carried);
        }
    }

    private boolean isItemSelectionActive() {
        return itemSelectionMode != ItemSelectionMode.DISABLED;
    }

    private static class ItemSelectionContainer implements Container {
        private ItemStack item = ItemStack.EMPTY;

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return item.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot == 0 ? item : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (slot != 0 || amount <= 0 || item.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = item.split(amount);
            if (item.isEmpty()) {
                item = ItemStack.EMPTY;
            }
            setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot != 0) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = item;
            item = ItemStack.EMPTY;
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot != 0) {
                return;
            }
            item = stack;
            if (!item.isEmpty() && item.getCount() > ITEM_SELECTION_MAX_AMOUNT) {
                item.setCount(ITEM_SELECTION_MAX_AMOUNT);
            }
            setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return ITEM_SELECTION_MAX_AMOUNT;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return ITEM_SELECTION_MAX_AMOUNT;
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0;
        }

        @Override
        public void clearContent() {
            item = ItemStack.EMPTY;
        }
    }

    private class ItemSelectionSlot extends Slot {
        ItemSelectionSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return canAddToSelection(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return itemSelectionMode.consumesItems() && playerOwnedSelectionAmount > 0;
        }

        @Override
        public int getMaxStackSize() {
            return ITEM_SELECTION_MAX_AMOUNT;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return ITEM_SELECTION_MAX_AMOUNT;
        }

        @Override
        public boolean isActive() {
            return isItemSelectionActive();
        }

        @Override
        public boolean isFake() {
            return !itemSelectionMode.consumesItems();
        }
    }

    private class PlayerInventorySlot extends Slot {
        PlayerInventorySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return isItemSelectionActive();
        }
    }
}

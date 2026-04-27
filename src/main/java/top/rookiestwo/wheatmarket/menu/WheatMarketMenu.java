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
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.WheatMarketRegistry;
import top.rookiestwo.wheatmarket.network.c2s.FinalizeStockEditC2SPacket;

import java.util.UUID;

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
    public static final int ITEM_SELECTION_MAX_AMOUNT = 999;

    private final ItemSelectionContainer itemSelectionContainer = new ItemSelectionContainer();
    private ItemSelectionMode itemSelectionMode = ItemSelectionMode.DISABLED;
    private ItemStack lockedItemSelectionTemplate = ItemStack.EMPTY;
    private boolean itemSelectionContainsRealItems;
    private boolean stockEditActive;
    private boolean stockEditFinalizing;
    private UUID stockEditMarketItemId;
    private int stockEditOriginalAmount;
    private ItemStack stockEditTemplate = ItemStack.EMPTY;
    private UUID editingMarketItemId;
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
        if (!isSlotInterfaceActive() || i < 0 || i >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(i);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack originalStack = stack.copy();
        if (i == ITEM_SELECTION_SLOT_INDEX) {
            if (!slot.mayPickup(player)) {
                return ItemStack.EMPTY;
            }
            quickMoveOneSelectionStackToPlayer(stack, stockEditActive);
            // Vanilla QUICK_MOVE repeats while the returned stack still matches the source slot.
            return ItemStack.EMPTY;
        }

        if (stockEditActive) {
            int moved = addToStockEdit(stack, stack.getCount());
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

        if (itemSelectionMode == ItemSelectionMode.SAMPLE) {
            setSample(stack);
            return ItemStack.EMPTY;
        }

        int moved = stockEditActive ? addToStockEdit(stack, stack.getCount()) : addToSelection(stack, stack.getCount());
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
        if (stockEditActive) {
            if (slotId == ITEM_SELECTION_SLOT_INDEX && clickType == ClickType.PICKUP) {
                handleStockEditSlotClick(button);
                return;
            }
            super.clicked(slotId, button, clickType, player);
            return;
        }
        if (isSlotInterfaceActive()) {
            if (clickType == ClickType.QUICK_CRAFT || clickType == ClickType.PICKUP_ALL) {
                return;
            }
            if (slotId == ITEM_SELECTION_SLOT_INDEX) {
                if (clickType == ClickType.PICKUP) {
                    if (stockEditActive) {
                        handleStockEditSlotClick(button);
                    } else if (itemSelectionMode == ItemSelectionMode.SAMPLE) {
                        handleSampleSlotClick(button);
                    } else if (itemSelectionMode == ItemSelectionMode.TRANSFER) {
                        handleTransferSlotClick(button);
                    }
                    return;
                }
                if (clickType != ClickType.QUICK_MOVE) {
                    return;
                }
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        if (isItemSelectionActive() && slot.index == ITEM_SELECTION_SLOT_INDEX) {
            return false;
        }
        return super.canDragTo(slot);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (isItemSelectionActive() && slot.index == ITEM_SELECTION_SLOT_INDEX) {
            return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    public void configureItemSelection(ItemSelectionMode mode, ItemStack initialSelection, ItemStack lockedTemplate, Player player) {
        ItemSelectionMode newMode = mode == null ? ItemSelectionMode.DISABLED : mode;
        ItemStack newLockedTemplate = newMode == ItemSelectionMode.DISABLED
                ? ItemStack.EMPTY
                : templateCopy(lockedTemplate);
        if (newMode == ItemSelectionMode.DISABLED) {
            setItemSelectionMode(ItemSelectionMode.DISABLED, player);
            return;
        }
        if (stockEditActive) {
            return;
        }

        boolean keepExistingSelection = canKeepExistingSelection(initialSelection, newLockedTemplate);
        if (keepExistingSelection) {
            returnCarriedItem(player);
        } else {
            returnOrClearSelection(player);
        }
        this.itemSelectionMode = newMode;
        this.lockedItemSelectionTemplate = newLockedTemplate;
        if (!keepExistingSelection && newMode == ItemSelectionMode.SAMPLE) {
            // Samples must come from an actual inventory click, not client-provided NBT.
            itemSelectionContainsRealItems = false;
            itemSelectionContainer.setItem(0, ItemStack.EMPTY);
        } else if (!keepExistingSelection) {
            setInitialSelection(initialSelection);
        }
        broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        returnCarriedItem(player);
        boolean hadStockEditSession = stockEditActive;
        if (hadStockEditSession && player instanceof ServerPlayer serverPlayer) {
            FinalizeStockEditC2SPacket.finalizeStockEdit(serverPlayer, this, false);
        } else if (hadStockEditSession) {
            clearStockEditSession();
        }
        itemSelectionMode = ItemSelectionMode.DISABLED;
        if (!hadStockEditSession) {
            returnOrClearSelection(player);
        }
        lockedItemSelectionTemplate = ItemStack.EMPTY;
        if (!hadStockEditSession && player instanceof ServerPlayer serverPlayer && WheatMarket.DATABASE != null) {
            WheatMarket.DATABASE.getMarketService().releaseItemEditLocks(serverPlayer.getUUID());
        }
        editingMarketItemId = null;
        super.removed(player);
    }

    public void setItemSelectionMode(ItemSelectionMode mode, Player player) {
        ItemSelectionMode newMode = mode == null ? ItemSelectionMode.DISABLED : mode;
        if (stockEditActive) {
            itemSelectionMode = ItemSelectionMode.DISABLED;
            lockedItemSelectionTemplate = ItemStack.EMPTY;
            returnCarriedItem(player);
            broadcastChanges();
            return;
        }
        if (this.itemSelectionMode == newMode) {
            if (newMode == ItemSelectionMode.DISABLED) {
                returnOrClearSelection(player);
                lockedItemSelectionTemplate = ItemStack.EMPTY;
                broadcastChanges();
            }
            return;
        }

        this.itemSelectionMode = newMode;
        returnOrClearSelection(player);
        if (newMode == ItemSelectionMode.DISABLED) {
            lockedItemSelectionTemplate = ItemStack.EMPTY;
        }
        broadcastChanges();
    }

    public void deactivateItemSelection(Player player) {
        itemSelectionMode = ItemSelectionMode.DISABLED;
        returnCarriedItem(player);
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
                || !itemSelectionContainsRealItems) {
            return false;
        }

        shrinkSelection(amount);
        broadcastChanges();
        return true;
    }

    public void clearSampleSelection(ItemStack expectedTemplate) {
        ItemStack selected = itemSelectionContainer.getItem(0);
        if (selected.isEmpty()
                || expectedTemplate == null
                || expectedTemplate.isEmpty()
                || !ItemStack.isSameItemSameComponents(selected, expectedTemplate)) {
            return;
        }

        itemSelectionContainer.setItem(0, ItemStack.EMPTY);
        itemSelectionContainsRealItems = false;
        broadcastChanges();
    }

    public Player getPlayer() {
        return player;
    }

    public void setEditingMarketItemId(UUID marketItemId) {
        this.editingMarketItemId = marketItemId;
    }

    public void clearEditingMarketItemId(UUID marketItemId) {
        if (marketItemId == null || marketItemId.equals(this.editingMarketItemId)) {
            this.editingMarketItemId = null;
        }
    }

    public void prepareClientStockEdit(UUID marketItemId, ItemStack lockedTemplate, Player player) {
        prepareClientStockEdit(marketItemId, lockedTemplate, 0, player);
    }

    public void prepareClientStockEdit(UUID marketItemId, ItemStack lockedTemplate, int initialAmount, Player player) {
        returnCarriedItem(player);
        returnOrClearSelection(player);
        itemSelectionMode = ItemSelectionMode.DISABLED;
        lockedItemSelectionTemplate = ItemStack.EMPTY;
        stockEditActive = true;
        stockEditFinalizing = false;
        stockEditMarketItemId = marketItemId;
        stockEditOriginalAmount = Math.max(0, initialAmount);
        stockEditTemplate = templateCopy(lockedTemplate);
        ItemStack stockStack = stockEditTemplate.copy();
        if (!stockStack.isEmpty() && stockEditOriginalAmount > 0) {
            stockStack.setCount(Math.min(stockEditOriginalAmount, ITEM_SELECTION_MAX_AMOUNT));
        } else {
            stockStack = ItemStack.EMPTY;
        }
        itemSelectionContainer.setItem(0, stockStack);
        broadcastChanges();
    }

    public void configureStockEdit(UUID marketItemId, ItemStack template, int originalAmount, Player player) {
        returnCarriedItem(player);
        returnOrClearSelection(player);
        itemSelectionMode = ItemSelectionMode.DISABLED;
        lockedItemSelectionTemplate = ItemStack.EMPTY;
        stockEditActive = true;
        stockEditFinalizing = false;
        stockEditMarketItemId = marketItemId;
        stockEditOriginalAmount = Math.max(0, originalAmount);
        stockEditTemplate = templateCopy(template);
        ItemStack stockStack = stockEditTemplate.copy();
        if (!stockStack.isEmpty() && stockEditOriginalAmount > 0) {
            stockStack.setCount(Math.min(stockEditOriginalAmount, ITEM_SELECTION_MAX_AMOUNT));
        } else {
            stockStack = ItemStack.EMPTY;
        }
        itemSelectionContainer.setItem(0, stockStack);
        broadcastChanges();
    }

    public boolean isStockEditActive() {
        return stockEditActive;
    }

    public int getStockEditAmount() {
        return stockEditActive ? getSelectedAmount() : 0;
    }

    public StockEditSnapshot beginStockEditFinalization(Player player) {
        if (!stockEditActive || stockEditFinalizing || stockEditMarketItemId == null) {
            return null;
        }
        returnCarriedItem(player);
        stockEditFinalizing = true;
        ItemStack finalStack = itemSelectionContainer.getItem(0).copy();
        int finalAmount = finalStack.isEmpty() ? 0 : finalStack.getCount();
        return new StockEditSnapshot(
                stockEditMarketItemId,
                stockEditTemplate.copy(),
                finalStack,
                stockEditOriginalAmount,
                finalAmount
        );
    }

    public void completeStockEditFinalization(boolean success) {
        if (success) {
            clearStockEditSession();
        } else {
            stockEditFinalizing = false;
        }
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private void handleSampleSlotClick(int button) {
        ItemStack carried = getCarried();
        if (!carried.isEmpty()) {
            setSample(carried);
            return;
        }
        if (button == 1) {
            itemSelectionContainsRealItems = false;
            itemSelectionContainer.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
        }
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

    private void handleTransferSlotClick(int button) {
        handleRealSlotClick(button, false);
    }

    private void handleStockEditSlotClick(int button) {
        handleRealSlotClick(button, true);
    }

    private void handleRealSlotClick(int button, boolean stockEdit) {
        ItemStack carried = getCarried();
        ItemStack selected = itemSelectionContainer.getItem(0);
        if (carried.isEmpty()) {
            if (selected.isEmpty()) {
                return;
            }
            if (!stockEdit && !itemSelectionContainsRealItems) {
                return;
            }

            int requestedPickupAmount = button == 1
                    ? Math.max(1, (selected.getCount() + 1) / 2)
                    : selected.getCount();
            if (stockEdit) {
                requestedPickupAmount = Math.min(requestedPickupAmount, normalMaxStackSize(selected));
            }
            int pickupAmount = Math.min(requestedPickupAmount, selected.getCount());
            if (pickupAmount <= 0) {
                return;
            }
            ItemStack pickedUp = selected.copy();
            pickedUp.setCount(pickupAmount);
            if (stockEdit) {
                shrinkStockEdit(pickupAmount);
            } else {
                shrinkSelection(pickupAmount);
            }
            setCarried(pickedUp);
            broadcastChanges();
            return;
        }

        if (!selected.isEmpty() && !ItemStack.isSameItemSameComponents(selected, carried)) {
            boolean selectedWasReal = stockEdit || itemSelectionContainsRealItems;
            if (button == 0 && replaceRealSlot(carried, stockEdit)) {
                setCarried(selectedWasReal ? selected.copy() : ItemStack.EMPTY);
                broadcastChanges();
            }
            return;
        }

        int requestedAmount = button == 1 ? 1 : carried.getCount();
        int moved = stockEdit ? addToStockEdit(carried, requestedAmount) : addToSelection(carried, requestedAmount);
        if (moved > 0) {
            carried.shrink(moved);
            if (carried.isEmpty()) {
                setCarried(ItemStack.EMPTY);
            }
            broadcastChanges();
        }
    }

    private boolean replaceRealSlot(ItemStack carried, boolean stockEdit) {
        if (carried.isEmpty()
                || carried.getCount() > ITEM_SELECTION_MAX_AMOUNT
                || (stockEdit ? !matchesStockEditTemplate(carried) : !matchesLockedTemplate(carried))) {
            return false;
        }
        ItemStack replacement = carried.copy();
        itemSelectionContainer.setItem(0, replacement);
        if (!stockEdit) {
            itemSelectionContainsRealItems = true;
        }
        return true;
    }

    private void setSample(ItemStack stack) {
        if (stack.isEmpty() || !matchesLockedTemplate(stack)) {
            return;
        }
        ItemStack sample = stack.copy();
        sample.setCount(1);
        itemSelectionContainsRealItems = false;
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
            itemSelectionContainer.setItem(0, copy);
            itemSelectionContainsRealItems = true;
            return moved;
        }

        if (!itemSelectionContainsRealItems) {
            return 0;
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
        itemSelectionContainsRealItems = true;
        itemSelectionContainer.setChanged();
        return moved;
    }

    private int addToStockEdit(ItemStack source, int requestedAmount) {
        if (source.isEmpty() || requestedAmount <= 0 || !matchesStockEditTemplate(source)) {
            return 0;
        }

        ItemStack selected = itemSelectionContainer.getItem(0);
        if (selected.isEmpty()) {
            int moved = Math.min(requestedAmount, ITEM_SELECTION_MAX_AMOUNT);
            ItemStack copy = source.copy();
            copy.setCount(moved);
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
        if (!selected.isEmpty() && !itemSelectionContainsRealItems) {
            return false;
        }
        return selected.isEmpty()
                || (selected.getCount() < ITEM_SELECTION_MAX_AMOUNT && ItemStack.isSameItemSameComponents(selected, stack));
    }

    private boolean canAddToStockEdit(ItemStack stack) {
        if (stack.isEmpty() || !stockEditActive || !matchesStockEditTemplate(stack)) {
            return false;
        }
        ItemStack selected = itemSelectionContainer.getItem(0);
        return selected.isEmpty()
                || (selected.getCount() < ITEM_SELECTION_MAX_AMOUNT && ItemStack.isSameItemSameComponents(selected, stack));
    }

    private void setInitialSelection(ItemStack initialSelection) {
        itemSelectionContainsRealItems = false;
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

    private boolean matchesStockEditTemplate(ItemStack stack) {
        return stockEditActive && !stockEditTemplate.isEmpty() && ItemStack.isSameItemSameComponents(stockEditTemplate, stack);
    }

    private boolean matchesTemplate(ItemStack template, ItemStack stack) {
        return template == null || template.isEmpty() || ItemStack.isSameItemSameComponents(template, stack);
    }

    private boolean canKeepExistingSelection(ItemStack initialSelection, ItemStack lockedTemplate) {
        ItemStack selected = itemSelectionContainer.getItem(0);
        if (selected.isEmpty()) {
            return initialSelection == null || initialSelection.isEmpty();
        }
        if (!matchesTemplate(lockedTemplate, selected)) {
            return false;
        }
        if (initialSelection == null || initialSelection.isEmpty()) {
            return true;
        }
        int expectedAmount = Math.min(initialSelection.getCount(), ITEM_SELECTION_MAX_AMOUNT);
        return selected.getCount() == expectedAmount
                && ItemStack.isSameItemSameComponents(selected, initialSelection);
    }

    private ItemStack selectionTemplate() {
        ItemStack selected = itemSelectionContainer.getItem(0);
        if (!selected.isEmpty()) {
            ItemStack template = selected.copy();
            template.setCount(1);
            return template;
        }
        return lockedItemSelectionTemplate;
    }

    private void shrinkSelection(int amount) {
        if (amount <= 0) {
            return;
        }
        ItemStack selected = itemSelectionContainer.getItem(0);
        if (selected.isEmpty()) {
            itemSelectionContainsRealItems = false;
            return;
        }
        selected.shrink(Math.min(amount, selected.getCount()));
        if (selected.isEmpty()) {
            itemSelectionContainer.setItem(0, ItemStack.EMPTY);
            itemSelectionContainsRealItems = false;
        } else {
            itemSelectionContainer.setChanged();
        }
    }

    private ItemStack templateCopy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private void shrinkStockEdit(int amount) {
        if (amount <= 0) {
            return;
        }
        ItemStack selected = itemSelectionContainer.getItem(0);
        if (selected.isEmpty()) {
            return;
        }
        selected.shrink(Math.min(amount, selected.getCount()));
        if (selected.isEmpty()) {
            itemSelectionContainer.setItem(0, ItemStack.EMPTY);
        } else {
            itemSelectionContainer.setChanged();
        }
    }

    private int normalMaxStackSize(ItemStack stack) {
        return Math.max(1, Math.min(stack.getMaxStackSize(), ITEM_SELECTION_MAX_AMOUNT));
    }

    private boolean quickMoveOneSelectionStackToPlayer(ItemStack selected, boolean stockEdit) {
        int moveAmount = Math.min(selected.getCount(), normalMaxStackSize(selected));
        ItemStack movingStack = selected.copy();
        movingStack.setCount(moveAmount);
        if (!moveItemStackTo(movingStack, PLAYER_INVENTORY_SLOT_START, PLAYER_INVENTORY_SLOT_END, true)) {
            return false;
        }

        int moved = moveAmount - movingStack.getCount();
        if (moved <= 0) {
            return false;
        }
        if (stockEdit) {
            shrinkStockEdit(moved);
        } else {
            shrinkSelection(moved);
        }
        return true;
    }

    private void returnOrClearSelection(Player player) {
        returnCarriedItem(player);
        ItemStack selected = itemSelectionContainer.removeItemNoUpdate(0);
        if (selected.isEmpty()) {
            itemSelectionContainsRealItems = false;
            return;
        }
        if (itemSelectionContainsRealItems && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getInventory().placeItemBackInInventory(selected);
        }
        itemSelectionContainsRealItems = false;
    }

    private void clearStockEditSession() {
        itemSelectionContainer.removeItemNoUpdate(0);
        stockEditActive = false;
        stockEditFinalizing = false;
        stockEditMarketItemId = null;
        stockEditOriginalAmount = 0;
        stockEditTemplate = ItemStack.EMPTY;
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

    private boolean isSlotInterfaceActive() {
        return isItemSelectionActive() || stockEditActive;
    }

    private boolean isItemSelectionActive() {
        return itemSelectionMode != ItemSelectionMode.DISABLED;
    }

    public record StockEditSnapshot(UUID marketItemId, ItemStack template, ItemStack finalStack,
                                    int originalAmount, int finalAmount) {
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
            return stockEditActive ? canAddToStockEdit(stack) : canAddToSelection(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return stockEditActive || (itemSelectionMode.consumesItems() && itemSelectionContainsRealItems);
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
            return isSlotInterfaceActive();
        }

        @Override
        public boolean isFake() {
            return !stockEditActive && !itemSelectionMode.consumesItems();
        }
    }

    private class PlayerInventorySlot extends Slot {
        PlayerInventorySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return isSlotInterfaceActive();
        }
    }
}

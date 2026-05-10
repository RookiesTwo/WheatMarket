package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.c2s.ListItemC2SPacket;
import top.rookiestwo.wheatmarket.network.c2s.SetItemSelectionModeC2SPacket;

public class WheatMarketListingScreen extends WheatMarketBaseScreen {
    private final Inventory inventory;
    private ItemStack stack;
    private int amount;
    private WheatMarketListingUI.ListingType listingType;
    private String priceText;
    private int buyQuantity;
    private boolean ifAdmin;
    private boolean ifInfinite;
    private boolean ifInfiniteDuration;
    private int cooldownAmount;
    private int cooldownDays;
    private int cooldownHours;
    private int cooldownMinutes;
    private int orderDays;
    private int orderHours;
    private int orderMinutes;
    private WheatMarketListingUI listingUI;
    private boolean selectionReleased;

    public WheatMarketListingScreen(WheatMarketMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, ItemStack.EMPTY, 0, WheatMarketListingUI.ListingType.SELL, "1.00", 1);
    }

    public WheatMarketListingScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                    ItemStack stack, int amount) {
        this(menu, inventory, title, stack, amount, WheatMarketListingUI.ListingType.SELL, "1.00", 1);
    }

    private WheatMarketListingScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                      ItemStack stack, int amount, WheatMarketListingUI.ListingType listingType,
                                      String priceText, int buyQuantity) {
        this(menu, inventory, title, stack, amount, listingType, priceText, buyQuantity, false, false, 0, 0, 0, 0, false, 0, 0, 0);
    }

    private WheatMarketListingScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                      ItemStack stack, int amount, WheatMarketListingUI.ListingType listingType,
                                      String priceText, int buyQuantity, boolean ifAdmin, boolean ifInfinite,
                                      int cooldownAmount, int cooldownDays, int cooldownHours, int cooldownMinutes) {
        this(menu, inventory, title, stack, amount, listingType, priceText, buyQuantity, ifAdmin, ifInfinite,
                cooldownAmount, cooldownDays, cooldownHours, cooldownMinutes, false, 0, 0, 0);
    }

    private WheatMarketListingScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                      ItemStack stack, int amount, WheatMarketListingUI.ListingType listingType,
                                      String priceText, int buyQuantity, boolean ifAdmin, boolean ifInfinite,
                                      int cooldownAmount, int cooldownDays, int cooldownHours, int cooldownMinutes,
                                      boolean ifInfiniteDuration, int orderDays, int orderHours, int orderMinutes) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.amount = this.stack.isEmpty() ? 0 : Math.max(1, amount);
        this.listingType = listingType == null ? WheatMarketListingUI.ListingType.SELL : listingType;
        this.priceText = priceText == null || priceText.isBlank() ? "1.00" : priceText;
        this.buyQuantity = Math.max(1, buyQuantity);
        this.ifAdmin = ifAdmin;
        this.ifInfinite = ifAdmin && this.listingType == WheatMarketListingUI.ListingType.SELL && ifInfinite;
        this.cooldownAmount = Math.max(0, cooldownAmount);
        this.cooldownDays = Math.max(0, cooldownDays);
        this.cooldownHours = Math.max(0, cooldownHours);
        this.cooldownMinutes = Math.max(0, cooldownMinutes);
        this.ifInfiniteDuration = ifInfiniteDuration;
        this.orderDays = Math.max(0, orderDays);
        this.orderHours = Math.max(0, orderHours);
        this.orderMinutes = Math.max(0, orderMinutes);
    }

    @Override
    protected ModularUI createModularUI() {
        this.listingUI = new WheatMarketListingUI(
                this.stack,
                this.amount,
                this.listingType,
                this.priceText,
                this.buyQuantity,
                this.ifAdmin,
                this.ifInfinite,
                this.cooldownAmount,
                this.cooldownDays,
                this.cooldownHours,
                this.cooldownMinutes,
                this::openItemSelection,
                this::submitListing,
                this::handleListingTypeChanged,
                this::returnToMain,
                this.ifInfiniteDuration,
                this.orderDays,
                this.orderHours,
                this.orderMinutes
        );
        return this.listingUI.create(this.inventory.player);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.listingUI != null) {
            this.listingUI.tick();
        }
    }

    private void openItemSelection() {
        if (this.minecraft == null) return;

        WheatMarketListingUI.Draft draft = this.listingUI == null
                ? new WheatMarketListingUI.Draft(this.stack, this.amount, this.listingType, this.priceText, this.buyQuantity,
                this.ifAdmin, this.ifInfinite, this.ifInfiniteDuration, this.cooldownAmount,
                this.cooldownDays, this.cooldownHours, this.cooldownMinutes,
                this.orderDays, this.orderHours, this.orderMinutes)
                : this.listingUI.createDraft();
        ItemSelectionPurpose purpose = draft.listingType() == WheatMarketListingUI.ListingType.BUY || draft.ifAdmin()
                ? ItemSelectionPurpose.LIST_BUY
                : ItemSelectionPurpose.LIST_SELL;
        ItemStack[] selectedStack = {draft.selectedStack()};
        int[] selectedAmount = {draft.selectedAmount()};
        ItemSelectionRequest request = new ItemSelectionRequest(
                purpose,
                purpose.defaultMode(),
                ItemStack.EMPTY,
                0,
                ItemStack.EMPTY,
                false,
                result -> {
                    selectedStack[0] = result.selectedStack();
                    if (result.empty()) {
                        selectedAmount[0] = 0;
                    } else if (draft.listingType() == WheatMarketListingUI.ListingType.BUY || draft.ifAdmin()) {
                        selectedAmount[0] = 1;
                    } else {
                        selectedAmount[0] = result.totalAmount();
                    }
                }
        );

        this.minecraft.setScreen(new WheatMarketItemSelectionScreen(this.menu, this.inventory, this.title,
                request,
                this.modularUI,
                (menu, inventory, title) -> new WheatMarketListingScreen(
                        menu, inventory, title,
                        selectedStack[0], selectedAmount[0],
                        draft.listingType(), draft.priceText(), draft.buyQuantity(),
                        draft.ifAdmin(), draft.ifInfinite(),
                        draft.cooldownAmount(),
                        draft.cooldownDays(), draft.cooldownHours(), draft.cooldownMinutes(),
                        draft.ifInfiniteDuration(),
                        draft.orderDays(), draft.orderHours(), draft.orderMinutes()
                )));
    }

    private void returnToMain() {
        releaseSelection();
        if (this.minecraft != null) {
            this.minecraft.setScreen(new WheatMarketMainScreen(this.menu, this.inventory, this.title));
        }
    }

    private void submitListing(WheatMarketListingUI.Submission submission) {
        WheatMarketNetwork.sendToServer(new ListItemC2SPacket(
                WheatMarketMenu.ITEM_SELECTION_SLOT_INDEX,
                submission.amount(),
                submission.price(),
                submission.listingType() == WheatMarketListingUI.ListingType.SELL,
                submission.ifAdmin(),
                submission.ifInfinite(),
                submission.cooldownAmount(),
                submission.cooldownTimeInMinutes(),
                submission.timeToExpireMs(),
                submission.ifInfiniteDuration()
        ));
    }

    private void handleListingTypeChanged(WheatMarketListingUI.Draft draft) {
        this.stack = draft.selectedStack();
        this.amount = draft.selectedAmount();
        this.listingType = draft.listingType();
        this.priceText = draft.priceText();
        this.buyQuantity = draft.buyQuantity();
        this.ifAdmin = draft.ifAdmin();
        this.ifInfinite = draft.ifInfinite();
        this.ifInfiniteDuration = draft.ifInfiniteDuration();
        this.cooldownAmount = draft.cooldownAmount();
        this.cooldownDays = draft.cooldownDays();
        this.cooldownHours = draft.cooldownHours();
        this.cooldownMinutes = draft.cooldownMinutes();
        this.orderDays = draft.orderDays();
        this.orderHours = draft.orderHours();
        this.orderMinutes = draft.orderMinutes();
        this.menu.setItemSelectionMode(ItemSelectionMode.DISABLED, this.inventory.player);
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(ItemSelectionMode.DISABLED));
    }

    public boolean handleOperationResult(boolean success, Component message) {
        if (this.listingUI == null || !this.listingUI.handleOperationResult(success, message)) return false;
        if (success) returnToMain();
        return true;
    }

    private void releaseSelection() {
        if (selectionReleased) return;
        selectionReleased = true;
        this.menu.setItemSelectionMode(ItemSelectionMode.DISABLED, this.inventory.player);
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(ItemSelectionMode.DISABLED));
    }

    @Override public void onClose() { releaseSelection(); super.onClose(); }
    @Override public void removed() { releaseSelection(); super.removed(); }
}

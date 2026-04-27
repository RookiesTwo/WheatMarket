package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.c2s.ListItemC2SPacket;
import top.rookiestwo.wheatmarket.network.c2s.SetItemSelectionModeC2SPacket;

public class WheatMarketListingScreen extends AbstractContainerScreen<WheatMarketMenu> {
    private final Inventory inventory;
    private ItemStack stack;
    private int amount;
    private WheatMarketListingUI.ListingType listingType;
    private String priceText;
    private int buyQuantity;
    private WheatMarketListingUI listingUI;
    private ModularUI modularUI;
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
        super(menu, inventory, title);
        this.inventory = inventory;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.amount = this.stack.isEmpty() ? 0 : Math.max(1, amount);
        this.listingType = listingType == null ? WheatMarketListingUI.ListingType.SELL : listingType;
        this.priceText = priceText == null || priceText.isBlank() ? "1.00" : priceText;
        this.buyQuantity = Math.max(1, buyQuantity);
    }

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        this.leftPos = 0;
        this.topPos = 0;

        this.listingUI = new WheatMarketListingUI(
                this.stack,
                this.amount,
                this.listingType,
                this.priceText,
                this.buyQuantity,
                this::openItemSelection,
                this::submitListing,
                this::handleListingTypeChanged,
                this::returnToMain
        );
        installModularUI(this.listingUI.create(this.inventory.player));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.listingUI != null) {
            this.listingUI.tick();
        }
    }

    private void openItemSelection() {
        if (this.minecraft == null) {
            return;
        }

        WheatMarketListingUI.Draft draft = this.listingUI == null
                ? new WheatMarketListingUI.Draft(this.stack, this.amount, this.listingType, this.priceText, this.buyQuantity)
                : this.listingUI.createDraft();
        ItemSelectionPurpose purpose = draft.listingType() == WheatMarketListingUI.ListingType.BUY
                ? ItemSelectionPurpose.LIST_BUY
                : ItemSelectionPurpose.LIST_SELL;
        ItemStack[] selectedStack = {draft.selectedStack()};
        int[] selectedAmount = {draft.selectedAmount()};
        ItemSelectionRequest request = new ItemSelectionRequest(
                purpose,
                purpose.defaultMode(),
                initialSelectionFor(draft),
                0,
                ItemStack.EMPTY,
                false,
                result -> {
                    selectedStack[0] = result.selectedStack();
                    if (result.empty()) {
                        selectedAmount[0] = 0;
                    } else if (draft.listingType() == WheatMarketListingUI.ListingType.BUY) {
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
                        menu,
                        inventory,
                        title,
                        selectedStack[0],
                        selectedAmount[0],
                        draft.listingType(),
                        draft.priceText(),
                        draft.buyQuantity()
                )));
    }

    private ItemStack initialSelectionFor(WheatMarketListingUI.Draft draft) {
        if (draft.selectedStack().isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack initialSelection = draft.selectedStack().copy();
        initialSelection.setCount(draft.listingType() == WheatMarketListingUI.ListingType.BUY
                ? 1
                : Math.max(1, draft.selectedAmount()));
        return initialSelection;
    }

    private void returnToMain() {
        releaseSelection();
        if (this.minecraft != null) {
            this.minecraft.setScreen(new WheatMarketMainScreen(this.menu, this.inventory, this.title));
        }
    }

    private void submitListing(WheatMarketListingUI.Submission submission) {
        WheatMarketNetwork.sendToServer(new ListItemC2SPacket(
                submission.amount(),
                submission.price(),
                submission.listingType() == WheatMarketListingUI.ListingType.SELL,
                false,
                false,
                0,
                0
        ));
    }

    private void handleListingTypeChanged(WheatMarketListingUI.Draft draft) {
        this.stack = draft.selectedStack();
        this.amount = draft.selectedAmount();
        this.listingType = draft.listingType();
        this.priceText = draft.priceText();
        this.buyQuantity = draft.buyQuantity();
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(ItemSelectionMode.DISABLED));
    }

    public boolean handleOperationResult(boolean success, Component message) {
        if (this.listingUI == null || !this.listingUI.handleOperationResult(success, message)) {
            return false;
        }
        if (success) {
            returnToMain();
        }
        return true;
    }

    private void releaseSelection() {
        if (selectionReleased) {
            return;
        }
        selectionReleased = true;
        this.menu.setItemSelectionMode(ItemSelectionMode.DISABLED, this.inventory.player);
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(ItemSelectionMode.DISABLED));
    }

    @Override
    public void onClose() {
        releaseSelection();
        super.onClose();
    }

    @Override
    public void removed() {
        releaseSelection();
        super.removed();
    }

    private void installModularUI(ModularUI modularUI) {
        this.modularUI = modularUI;
        this.modularUI.setMenu(this.menu);
        this.modularUI.setScreenAndInit(this);
        this.addRenderableWidget(this.modularUI.getWidget());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

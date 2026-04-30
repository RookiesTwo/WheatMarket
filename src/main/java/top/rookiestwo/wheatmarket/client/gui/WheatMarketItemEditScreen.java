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
import top.rookiestwo.wheatmarket.network.c2s.ManageItemC2SPacket;
import top.rookiestwo.wheatmarket.network.c2s.ReleaseItemEditLockC2SPacket;
import top.rookiestwo.wheatmarket.network.c2s.SetItemSelectionModeC2SPacket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

public class WheatMarketItemEditScreen extends AbstractContainerScreen<WheatMarketMenu> {
    private final Inventory inventory;
    private final MarketListS2CPacket.MarketItemSummary item;
    private final ItemStack stack;
    private WheatMarketItemEditUI.Draft draft;
    private WheatMarketItemEditUI itemEditUI;
    private ModularUI modularUI;
    private boolean selectionReleased;
    private boolean editLockReleased;
    private boolean openingStockSelection;

    public WheatMarketItemEditScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                     MarketListS2CPacket.MarketItemSummary item, ItemStack stack) {
        this(menu, inventory, title, item, stack, WheatMarketItemEditUI.Draft.from(item));
    }

    WheatMarketItemEditScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                              MarketListS2CPacket.MarketItemSummary item, ItemStack stack,
                              WheatMarketItemEditUI.Draft draft) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.item = item;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.draft = draft == null ? WheatMarketItemEditUI.Draft.from(item) : draft;
    }

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        this.leftPos = 0;
        this.topPos = 0;

        this.itemEditUI = new WheatMarketItemEditUI(
                this.item,
                this.stack,
                this.draft,
                this::openStockSelection,
                this::submitAction,
                this::returnToMain
        );
        installModularUI(this.itemEditUI.create(this.inventory.player));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.itemEditUI != null) {
            this.itemEditUI.tick();
        }
    }

    private void openStockSelection() {
        if (this.minecraft == null || this.itemEditUI == null) {
            return;
        }

        WheatMarketItemEditUI.Draft currentDraft = this.itemEditUI.createDraft();
        int[] finalStock = {currentDraft.currentStock()};
        ItemSelectionRequest request = ItemSelectionRequest.stockEdit(
                this.item.getMarketItemID(),
                this.stack,
                currentDraft.currentStock(),
                result -> finalStock[0] = result.totalAmount()
        );
        openingStockSelection = true;

        this.minecraft.setScreen(new WheatMarketItemSelectionScreen(
                this.menu,
                this.inventory,
                this.title,
                request,
                this.modularUI,
                (menu, inventory, title) -> {
                    if (finalStock[0] <= 0) {
                        releaseEditLock();
                        return new WheatMarketMainScreen(menu, inventory, title);
                    }
                    return new WheatMarketItemEditScreen(
                            menu,
                            inventory,
                            title,
                            this.item,
                            this.stack,
                            currentDraft.withStock(finalStock[0])
                    );
                }
        ));
    }

    private void submitAction(WheatMarketItemEditUI.ActionRequest request) {
        switch (request.action()) {
            case CHANGE_PRICE -> WheatMarketNetwork.sendToServer(new ManageItemC2SPacket(
                    item.getMarketItemID(),
                    ManageItemC2SPacket.ACTION_CHANGE_PRICE,
                    0,
                    request.price(),
                    0,
                    0,
                    0
            ));
            case DELIST -> WheatMarketNetwork.sendToServer(new ManageItemC2SPacket(
                    item.getMarketItemID(),
                    ManageItemC2SPacket.ACTION_DELIST,
                    0,
                    0.0D,
                    0,
                    0,
                    0
            ));
            case TOGGLE_ADMIN -> WheatMarketNetwork.sendToServer(new ManageItemC2SPacket(
                    item.getMarketItemID(),
                    ManageItemC2SPacket.ACTION_TOGGLE_ADMIN,
                    0,
                    0.0D,
                    0,
                    0,
                    0
            ));
            case TOGGLE_INFINITE -> WheatMarketNetwork.sendToServer(new ManageItemC2SPacket(
                    item.getMarketItemID(),
                    ManageItemC2SPacket.ACTION_TOGGLE_INFINITE,
                    0,
                    0.0D,
                    0,
                    0,
                    0
            ));
            case SET_COOLDOWN -> WheatMarketNetwork.sendToServer(new ManageItemC2SPacket(
                    item.getMarketItemID(),
                    ManageItemC2SPacket.ACTION_SET_COOLDOWN,
                    0,
                    0.0D,
                    0,
                    request.cooldownAmount(),
                    request.cooldownTimeInMinutes()
            ));
            default -> {
            }
        }
    }

    public boolean handleOperationResult(boolean success, Component message) {
        if (this.itemEditUI == null) {
            return false;
        }

        WheatMarketItemEditUI.Action pendingAction = this.itemEditUI.pendingAction();
        boolean shouldReturnToMain = success && pendingAction == WheatMarketItemEditUI.Action.DELIST;
        if (!this.itemEditUI.handleOperationResult(success, message)) {
            return false;
        }
        if (shouldReturnToMain) {
            returnToMain();
        }
        return true;
    }

    private void returnToMain() {
        releaseSelection();
        releaseEditLock();
        if (this.minecraft != null) {
            this.minecraft.setScreen(new WheatMarketMainScreen(this.menu, this.inventory, this.title));
        }
    }

    private void releaseSelection() {
        if (selectionReleased) {
            return;
        }
        selectionReleased = true;
        this.menu.setItemSelectionMode(ItemSelectionMode.DISABLED, this.inventory.player);
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(ItemSelectionMode.DISABLED));
    }

    private void releaseEditLock() {
        if (editLockReleased) {
            return;
        }
        editLockReleased = true;
        WheatMarketNetwork.sendToServer(new ReleaseItemEditLockC2SPacket(this.item.getMarketItemID()));
    }

    @Override
    public void onClose() {
        releaseSelection();
        releaseEditLock();
        super.onClose();
    }

    @Override
    public void removed() {
        releaseSelection();
        if (!openingStockSelection) {
            releaseEditLock();
        }
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

package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.c2s.SetItemSelectionModeC2SPacket;

public class WheatMarketDeliveryScreen extends AbstractContainerScreen<WheatMarketMenu> {
    private final Inventory inventory;
    private final WheatMarketHomeUI.State returnState;
    private WheatMarketDeliveryUI deliveryUI;
    private ModularUI modularUI;
    private boolean returningToMain;

    public WheatMarketDeliveryScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                     WheatMarketHomeUI.State returnState) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.returnState = returnState;
    }

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        this.leftPos = 0;
        this.topPos = 0;
        disableItemSelectionMode();

        this.deliveryUI = new WheatMarketDeliveryUI(this::returnToMain);
        installModularUI(this.deliveryUI.create(this.inventory.player));
        this.deliveryUI.requestCurrentPage();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.deliveryUI != null) {
            this.deliveryUI.tick();
        }
    }

    public boolean handleOperationResult(boolean success, Component message) {
        return this.deliveryUI != null && this.deliveryUI.handleOperationResult(success, message);
    }

    @Override
    public void onClose() {
        returnToMain();
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
            returnToMain();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void returnToMain() {
        if (returningToMain || this.minecraft == null) {
            return;
        }
        returningToMain = true;
        this.minecraft.setScreen(new WheatMarketMainScreen(this.menu, this.inventory, this.title, this.returnState));
    }

    private void disableItemSelectionMode() {
        this.menu.setItemSelectionMode(ItemSelectionMode.DISABLED, this.inventory.player);
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(ItemSelectionMode.DISABLED));
    }

    private void installModularUI(ModularUI modularUI) {
        this.modularUI = modularUI;
        this.modularUI.setMenu(this.menu);
        this.modularUI.setScreenAndInit(this);
        this.addRenderableWidget(this.modularUI.getWidget());
    }
}

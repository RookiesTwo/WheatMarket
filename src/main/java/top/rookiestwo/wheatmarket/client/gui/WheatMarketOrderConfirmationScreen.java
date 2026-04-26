package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

public class WheatMarketOrderConfirmationScreen extends AbstractContainerScreen<WheatMarketMenu> {
    private final Inventory inventory;
    private final MarketListS2CPacket.MarketItemSummary item;
    private final ItemStack stack;
    private WheatMarketOrderConfirmationUI orderConfirmationUI;
    private ModularUI modularUI;
    private int selectedQuantity = 1;
    private boolean initializedOnce;

    public WheatMarketOrderConfirmationScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                              MarketListS2CPacket.MarketItemSummary item, ItemStack stack) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.item = item;
        this.stack = stack.copy();
    }

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        this.leftPos = 0;
        this.topPos = 0;

        if (this.orderConfirmationUI != null) {
            this.selectedQuantity = this.orderConfirmationUI.getQuantity();
        }

        this.orderConfirmationUI = new WheatMarketOrderConfirmationUI(
                this.item,
                this.stack,
                this::returnToMain,
                !this.initializedOnce,
                this.selectedQuantity
        );
        installModularUI(this.orderConfirmationUI.create(this.inventory.player));
        this.initializedOnce = true;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.orderConfirmationUI != null) {
            this.orderConfirmationUI.tick();
        }
    }

    public boolean handleOperationResult(boolean success, Component message) {
        return this.orderConfirmationUI != null && this.orderConfirmationUI.handleOperationResult(success, message);
    }

    private void returnToMain() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new WheatMarketMainScreen(this.menu, this.inventory, this.title));
        }
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

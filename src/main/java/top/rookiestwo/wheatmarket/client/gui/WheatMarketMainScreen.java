package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

public class WheatMarketMainScreen extends AbstractContainerScreen<WheatMarketMenu> {
    private final Inventory inventory;
    private WheatMarketHomeUI homeUI;
    private ModularUI modularUI;

    public WheatMarketMainScreen(WheatMarketMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
        this.inventory = inventory;
    }

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        this.leftPos = 0;
        this.topPos = 0;
        this.homeUI = new WheatMarketHomeUI(this::showOrderConfirmation);
        installModularUI(this.homeUI.create(this.inventory.player));
        this.homeUI.requestCurrentPage();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.homeUI != null) {
            this.homeUI.tick();
        }
    }

    private void showOrderConfirmation(MarketListS2CPacket.MarketItemSummary item, ItemStack stack) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new WheatMarketOrderConfirmationScreen(this.menu, this.inventory, this.title, item, stack));
        }
    }

    private void installModularUI(ModularUI modularUI) {
        this.modularUI = modularUI;
        this.modularUI.setMenu(this.menu);
        this.modularUI.setScreenAndInit(this);
        this.addRenderableWidget(this.modularUI.getWidget());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
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
        var minecraft = this.minecraft;
        if (minecraft == null) {
            return false;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && this.homeUI != null
                && this.homeUI.submitSearchIfSearchFieldFocused()) {
            return true;
        }
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void handleBalanceUpdate(double balance) {
        if (this.homeUI != null) {
            this.homeUI.setBalance(balance);
        }
    }
}

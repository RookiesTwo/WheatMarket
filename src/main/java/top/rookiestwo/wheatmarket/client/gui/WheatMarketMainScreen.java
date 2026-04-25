package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class WheatMarketMainScreen extends AbstractContainerScreen<WheatMarketMenu> {
    private WheatMarketHomeUI homeUI;
    private ModularUI modularUI;

    public WheatMarketMainScreen(WheatMarketMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    protected void init() {
        super.init();
        this.homeUI = new WheatMarketHomeUI();
        this.modularUI = homeUI.create();
        this.modularUI.setScreenAndInit(this);
        this.addRenderableWidget(this.modularUI.getWidget());
        this.homeUI.requestCurrentPage();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (homeUI != null) {
            homeUI.tick();
        }
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

package top.rookiestwo.wheatmarket.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class WheatMarketMenuScreen extends AbstractContainerScreen<WheatMarketMenu> {

    public WheatMarketMenuScreen(WheatMarketMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {

    }
}

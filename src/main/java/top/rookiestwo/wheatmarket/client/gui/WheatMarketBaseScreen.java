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

public abstract class WheatMarketBaseScreen extends AbstractContainerScreen<WheatMarketMenu> {

    protected ModularUI modularUI;
    private boolean itemSelectionDisabled;

    protected WheatMarketBaseScreen(WheatMarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        this.leftPos = 0;
        this.topPos = 0;
        if (!itemSelectionDisabled) {
            disableItemSelectionMode();
            itemSelectionDisabled = true;
        }
        this.modularUI = createModularUI();
        if (this.modularUI != null) {
            installModularUI(this.modularUI);
        }
    }

    protected abstract ModularUI createModularUI();

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
        if (this.minecraft != null
                && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected void disableItemSelectionMode() {
        if (this.menu.getItemSelectionMode() == ItemSelectionMode.DISABLED) {
            return;
        }
        this.menu.setItemSelectionMode(ItemSelectionMode.DISABLED, this.minecraft.player);
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(ItemSelectionMode.DISABLED));
    }

    protected void installModularUI(ModularUI modularUI) {
        this.modularUI = modularUI;
        this.modularUI.setMenu(this.menu);
        this.modularUI.setScreenAndInit(this);
        this.addRenderableWidget(this.modularUI.getWidget());
    }
}

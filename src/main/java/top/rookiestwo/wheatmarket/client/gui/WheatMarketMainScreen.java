package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

public class WheatMarketMainScreen extends WheatMarketBaseScreen {
    private final Inventory inventory;
    private final WheatMarketHomeUI.State initialState;
    private WheatMarketHomeUI homeUI;

    public WheatMarketMainScreen(WheatMarketMenu abstractContainerMenu, Inventory inventory, Component component) {
        this(abstractContainerMenu, inventory, component, null);
    }

    public WheatMarketMainScreen(WheatMarketMenu abstractContainerMenu, Inventory inventory, Component component,
                                 WheatMarketHomeUI.State initialState) {
        super(abstractContainerMenu, inventory, component);
        this.inventory = inventory;
        this.initialState = initialState;
    }

    @Override
    protected ModularUI createModularUI() {
        this.homeUI = new WheatMarketHomeUI(
                this::showOrderConfirmation,
                this::showItemSelection,
                this::showDeliveryScreen,
                this.initialState
        );
        return this.homeUI.create(this.inventory.player);
    }

    @Override
    protected void init() {
        super.init();
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

    private void showItemSelection() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new WheatMarketListingScreen(this.menu, this.inventory, this.title));
        }
    }

    private void showDeliveryScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new WheatMarketDeliveryScreen(
                    this.menu,
                    this.inventory,
                    this.title,
                    this.homeUI == null ? null : this.homeUI.createState()
            ));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft == null) {
            return false;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && this.homeUI != null
                && this.homeUI.submitSearchIfSearchFieldFocused()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void handleBalanceUpdate(double balance) {
        if (this.homeUI != null) {
            this.homeUI.setBalance(balance);
        }
    }

    public void refreshMarketList() {
        if (this.homeUI != null) {
            this.homeUI.requestCurrentPage();
        }
    }
}

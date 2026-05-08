package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.c2s.AcquireItemEditLockC2SPacket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

public class WheatMarketOrderConfirmationScreen extends WheatMarketBaseScreen {
    private final Inventory inventory;
    private final MarketListS2CPacket.MarketItemSummary item;
    private final ItemStack stack;
    private WheatMarketOrderConfirmationUI orderConfirmationUI;
    private int selectedQuantity = 1;
    private boolean initializedOnce;
    private boolean openingManagement;

    public WheatMarketOrderConfirmationScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                              MarketListS2CPacket.MarketItemSummary item, ItemStack stack) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.item = item;
        this.stack = stack.copy();
    }

    @Override
    protected ModularUI createModularUI() {
        return null;
    }

    @Override
    protected void init() {
        super.init();
        if (!initializedOnce) {
            disableItemSelectionMode();
        }

        if (this.orderConfirmationUI != null) {
            this.selectedQuantity = this.orderConfirmationUI.getQuantity();
        }

        this.orderConfirmationUI = new WheatMarketOrderConfirmationUI(
                this.item,
                this.stack,
                this::returnToMain,
                this::openManagementEntry,
                !this.initializedOnce,
                this.selectedQuantity
        );
        this.modularUI = this.orderConfirmationUI.create(this.inventory.player);
        installModularUI(this.modularUI);
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
        if (openingManagement) {
            openingManagement = false;
            if (success) {
                openManagementScreen();
                return true;
            }
            return false;
        }
        return this.orderConfirmationUI != null && this.orderConfirmationUI.handleOperationResult(success, message);
    }

    private void returnToMain() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new WheatMarketMainScreen(this.menu, this.inventory, this.title));
        }
    }

    private void openManagementEntry() {
        if (openingManagement) {
            return;
        }
        openingManagement = true;
        WheatMarketNetwork.sendToServer(new AcquireItemEditLockC2SPacket(this.item.getMarketItemID()));
    }

    private void openManagementScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new WheatMarketItemEditScreen(this.menu, this.inventory, this.title, this.item, this.stack));
        }
    }
}

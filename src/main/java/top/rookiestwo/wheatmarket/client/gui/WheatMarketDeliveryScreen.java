package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class WheatMarketDeliveryScreen extends WheatMarketBaseScreen {
    private final Inventory inventory;
    private final WheatMarketHomeUI.State returnState;
    private WheatMarketDeliveryUI deliveryUI;
    private boolean returningToMain;

    public WheatMarketDeliveryScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                     WheatMarketHomeUI.State returnState) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.returnState = returnState;
    }

    @Override
    protected ModularUI createModularUI() {
        this.deliveryUI = new WheatMarketDeliveryUI(this::returnToMain);
        return this.deliveryUI.create(this.inventory.player);
    }

    @Override
    protected void init() {
        super.init();
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

    private void returnToMain() {
        if (returningToMain || this.minecraft == null) {
            return;
        }
        returningToMain = true;
        this.minecraft.setScreen(new WheatMarketMainScreen(this.menu, this.inventory, this.title, this.returnState));
    }
}

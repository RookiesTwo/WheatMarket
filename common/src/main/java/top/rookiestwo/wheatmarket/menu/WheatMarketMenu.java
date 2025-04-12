package top.rookiestwo.wheatmarket.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.rookiestwo.wheatmarket.WheatMarketRegistry;

public class WheatMarketMenu extends AbstractContainerMenu {

    private Player player=null;

    public WheatMarketMenu(int containerId, Inventory inventory) {
        super(WheatMarketRegistry.WHEAT_MARKET_MENU.get(), containerId);
    }

    public WheatMarketMenu(int containerId, Inventory inventory, Player player) {
        this(containerId, inventory);
        this.player=player;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public Player getPlayer() {
        return player;
    }
}

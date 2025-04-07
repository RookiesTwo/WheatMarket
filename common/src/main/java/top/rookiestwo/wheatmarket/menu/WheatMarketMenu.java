package top.rookiestwo.wheatmarket.menu;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.rookiestwo.wheatmarket.WheatMarketRegistry;

public class WheatMarketMenu extends AbstractContainerMenu {

    public WheatMarketMenu(int i, Inventory inventory) {
        super(WheatMarketRegistry.WHEAT_MARKET_MENU.get(), i);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }
}

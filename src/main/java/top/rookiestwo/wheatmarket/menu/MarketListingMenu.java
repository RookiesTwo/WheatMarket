package top.rookiestwo.wheatmarket.menu;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public abstract class MarketListingMenu extends AbstractContainerMenu {
    protected MarketListingMenu(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }
}

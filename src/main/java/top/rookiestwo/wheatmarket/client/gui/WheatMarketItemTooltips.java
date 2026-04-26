package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

final class WheatMarketItemTooltips {
    private WheatMarketItemTooltips() {
    }

    static HoverTooltips forStack(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        Item.TooltipContext tooltipContext = minecraft.level == null
                ? Item.TooltipContext.EMPTY
                : Item.TooltipContext.of(minecraft.level);
        List<Component> tooltipLines = stack.getTooltipLines(tooltipContext, minecraft.player, TooltipFlag.NORMAL);
        return HoverTooltips.empty()
                .append(tooltipLines.toArray(Component[]::new))
                .tooltipComponent(stack.getTooltipImage().orElse(null))
                .stack(stack);
    }
}

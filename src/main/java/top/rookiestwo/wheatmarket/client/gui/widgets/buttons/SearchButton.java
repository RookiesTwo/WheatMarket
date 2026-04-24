package top.rookiestwo.wheatmarket.client.gui.widgets.buttons;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import top.rookiestwo.wheatmarket.WheatMarket;

public class SearchButton extends WheatButton {

    private static final ResourceLocation SEARCH_ICON = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "textures/gui/search_icon.png");
    private static final ResourceLocation SEARCH_ICON_HOVERED = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "textures/gui/search_icon_hovered.png");
    private int iconOffsetX = 2;
    private int iconOffsetY = 2;

    public SearchButton(int x, int y, int width, int height, Component component, @NotNull ResourceLocation background) {
        super(x, y, width, height, component, background);
    }

    @Override
    public void onPress() {
        //搜索逻辑待添加
    }

    public void fitIcon() {
        iconOffsetX = (this.width - 16) / 2;
        iconOffsetY = (this.height - 16) / 2;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        ResourceLocation icon = SEARCH_ICON;
        if (this.isHovered) {
            icon = SEARCH_ICON_HOVERED;
        }
        guiGraphics.blit(icon, this.getX() + iconOffsetX, this.getY() + iconOffsetY, 0, 0, 16, 16, 16, 16);
    }
}

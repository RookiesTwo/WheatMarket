package top.rookiestwo.wheatmarket.client.gui.widgets.buttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import top.rookiestwo.wheatmarket.WheatMarket;

public class FilterButton<T extends Enum<T>> extends WheatButton{
    private T currentState;

    private static final ResourceLocation FILTER_ICON=ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "textures/gui/filter_icon.png");
    private static final ResourceLocation FILTER_ICON_HOVERED=ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "textures/gui/filter_icon_hovered.png");

    protected static final int textColor = 0xFFFFFF;
    protected static final int textColorHovered = 0xF9AF12;

    public FilterButton(int x, int y, int width, int height, Component component, @NotNull ResourceLocation background,T initialState) {
        super(x, y, width, height, component, background);
        this.currentState = initialState;
        this.setMessage(getStateText());
        this.active = true;
    }

    public void cycleState() {
        // 获取枚举的所有值并循环切换
        T[] values = (T[]) currentState.getDeclaringClass().getEnumConstants();
        int currentIndex = currentState.ordinal();
        this.currentState = values[(currentIndex + 1) % values.length];
    }

    public T getCurrentState() {
        return currentState;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        ResourceLocation icon = FILTER_ICON;
        int textRenderColor = textColor;
        if (this.isHovered) {
            icon = FILTER_ICON_HOVERED;
            textRenderColor = textColorHovered;
        }
        //渲染漏斗图标
        guiGraphics.blit(icon, this.getX() + 2, this.getY() + 2, 0, 0, 16, 16, 16, 16);
        //渲染文字
        Minecraft minecraft = Minecraft.getInstance();
        renderScrollingString(guiGraphics, minecraft.font, getStateText(), this.getX() + 17, this.getY() + 4, this.getX() + this.width - 16, this.getY() + this.height - 4, textRenderColor);
    }

    @Override
    public void onPress() {
        cycleState();
        this.setMessage(getStateText());
        WheatMarket.LOGGER.info("button pressed, current state: {}", currentState.name());
    }

    private Component getStateText() {
        return Component.translatable("gui.wheatmarket.filter." + currentState.name().toLowerCase());
    }
}

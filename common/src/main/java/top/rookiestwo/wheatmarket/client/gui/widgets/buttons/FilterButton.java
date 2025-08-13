package top.rookiestwo.wheatmarket.client.gui.widgets.buttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.client.gui.widgets.BlockBackgroundWidget;
import top.rookiestwo.wheatmarket.client.gui.widgets.WheatAbstractWidget;

public class FilterButton<T extends Enum<T>> extends WheatButton{
    private T currentState;

    private static final ResourceLocation FILTER_ICON=ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "textures/gui/filter_icon.png");
    private static final ResourceLocation FILTER_ICON_HOVERED=ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "textures/gui/filter_icon_hovered.png");

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
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.blitSprite(this.background, this.getX(), this.getY(), width, height);
        //guiGraphics.drawString(minecraft.font, getStateText(), this.getRenderX() + 5, this.getRenderY() + 5, 0xFFFFFF, false);
        ResourceLocation icon = this.isHovered() ? FILTER_ICON_HOVERED : FILTER_ICON;
        guiGraphics.blit(icon, this.getX() + 2, this.getY() + 2, 0, 0, 16, 16, 16, 16);
        renderScrollingString(guiGraphics, minecraft.font,getStateText(),this.getX()+4,this.getY()+4,this.getX()+this.width-4,this.getY()+this.height-4,0xFFFFFF);
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

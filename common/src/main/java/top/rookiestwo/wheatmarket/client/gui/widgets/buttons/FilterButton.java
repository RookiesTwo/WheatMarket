package top.rookiestwo.wheatmarket.client.gui.widgets.buttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import top.rookiestwo.wheatmarket.WheatMarket;

public class FilterButton<T extends Enum<T>> extends WheatButton{
    private T currentState;

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
        super.renderScrollingString(guiGraphics, minecraft.font,2,5);
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

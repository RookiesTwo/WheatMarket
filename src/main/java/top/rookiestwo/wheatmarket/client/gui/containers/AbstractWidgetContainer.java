package top.rookiestwo.wheatmarket.client.gui.containers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AbstractWidgetContainer extends AbstractWidget {

    protected Screen screen;

    public AbstractWidgetContainer(int x, int y, int width, int height, Component message, Screen screen) {
        super(x, y, width, height, message);
        this.screen = screen;
    }

    //处理组件的位置干涉关系
    public abstract void fitWidgets();

    @Override
    protected abstract void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}

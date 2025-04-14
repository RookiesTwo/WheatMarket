package top.rookiestwo.wheatmarket.client.gui.containers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import top.rookiestwo.wheatmarket.client.gui.widgets.WheatAbstractWidget;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWidgetContainer extends AbstractWidget {
    private final List<WheatAbstractWidget> widgets=new ArrayList<>();

    public AbstractWidgetContainer(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    public void addWidget(WheatAbstractWidget w) {
        widgets.add(w);
    }

    //处理组件的位置干涉关系
    public abstract void fitWidgets();

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.fitWidgets();
        for(WheatAbstractWidget w: widgets) {
            w.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}

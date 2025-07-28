package top.rookiestwo.wheatmarket.client.gui.widgets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class TitleWidget extends WheatAbstractWidget {

    private int originWidth;
    private int originHeight;

    public TitleWidget(int x, int y, int width, int height, Component message, float anchorX, float anchorY, ResourceLocation background) {
        super(x, y, width, height, message, anchorX, anchorY, background);
        this.originWidth = width;
        this.originHeight = height;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(background, getRenderX(),getRenderY(),width,height,0,0,originWidth,originHeight,256,128);
    }
}
package top.rookiestwo.wheatmarket.client.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BlockBackgroundWidget extends WheatAbstractWidget{

    public BlockBackgroundWidget(int x, int y, int width, int height, Component message, float anchorX, float anchorY, ResourceLocation background) {
        super(x, y, width, height, message, anchorX, anchorY, background);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blitSprite(this.background,this.getRenderX(),this.getRenderY(),width,height);
    }
}

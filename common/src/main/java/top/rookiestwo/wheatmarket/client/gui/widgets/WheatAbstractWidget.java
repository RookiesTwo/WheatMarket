package top.rookiestwo.wheatmarket.client.gui.widgets;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class WheatAbstractWidget extends AbstractWidget {

    protected Pair<Float, Float> anchor;
    protected ResourceLocation background;

    protected int renderWidth;
    protected int renderHeight;

    public WheatAbstractWidget(int x, int y, int width, int height, Component message, float anchorX, float anchorY, ResourceLocation background) {
        super(x,y,width,height,message);
        this.anchor = new Pair<>(anchorX, anchorY);
        this.background = background;
        this.renderWidth = width;
        this.renderHeight = height;
    }

    protected int getRenderX(){
        return (int)(this.getX()-anchor.getFirst()*width);
    }

    protected int getRenderY(){
        return (int)(this.getY()-anchor.getSecond()*height);
    }

    public void setAnchor(float anchorX, float anchorY) {
        this.anchor = new Pair<>(anchorX, anchorY);
    }

    public void setBackground(ResourceLocation background) {
        this.background = background;
    }

    public void setRenderWidth(int renderWidth) {
        this.renderWidth = renderWidth;
    }

    public void setRenderHeight(int renderHeight) {
        this.renderHeight = renderHeight;
    }

    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.blitSprite(background, (int) (this.getX()-anchor.getFirst()*width),(int) (this.getY()-anchor.getFirst()*height),this.getWidth(),this.getHeight());
    }
}
package top.rookiestwo.wheatmarket.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.rookiestwo.wheatmarket.client.gui.containers.AbstractWidgetContainer;

public class HeadWidget extends WheatAbstractWidget {

    private PlayerInfo playerInfo;

    public HeadWidget(int x, int y, int width, int height, Component message, float anchorX, float anchorY, ResourceLocation background) {
        super(x, y, width, height, message, anchorX, anchorY, background);
        if (Minecraft.getInstance().player != null) {
            playerInfo= new PlayerInfo(Minecraft.getInstance().player.getGameProfile(),false);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation skinLocation = playerInfo.getSkin().texture();
        // draw base layer
        guiGraphics.blit(skinLocation, getRenderX(), getRenderY(), width, height, 8.0f, 8, 8, 8, 64, 64);
        // draw hat
        guiGraphics.blit(skinLocation, getRenderX(), getRenderY(), width, height, 40.0f, 8, 8, 8, 64, 64);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}

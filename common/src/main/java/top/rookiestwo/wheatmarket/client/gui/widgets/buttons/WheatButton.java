package top.rookiestwo.wheatmarket.client.gui.widgets.buttons;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public abstract class WheatButton extends AbstractButton {
    @NotNull
    ResourceLocation background;

    public WheatButton(int x, int y, int width, int height, Component component, @NotNull ResourceLocation background) {
        super(x, y, width, height, component);
        this.background=background;
    }

    @Override
    public abstract void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {}
}

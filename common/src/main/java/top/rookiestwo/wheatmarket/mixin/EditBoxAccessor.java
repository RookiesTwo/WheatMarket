package top.rookiestwo.wheatmarket.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiFunction;

@Mixin(EditBox.class)
public interface EditBoxAccessor {
    @Accessor
    int getDisplayPos();
    @Accessor
    int getTextColor();
    @Accessor
    Font getFont();
    @Accessor
    long getFocusedTime();
    @Accessor
    int getHighlightPos();
    @Accessor
    BiFunction<String, Integer, FormattedCharSequence> getFormatter();
    @Accessor
    int getMaxLength();
    @Invoker("renderHighlight")
    public void invokeRenderHighlight(GuiGraphics guiGraphics, int i, int j, int k, int l);
}

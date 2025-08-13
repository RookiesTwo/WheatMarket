package top.rookiestwo.wheatmarket.client.gui.widgets;


import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class WheatEditBox extends EditBox {

    private Font font;
    private long focusedTime;

    public WheatEditBox(Font font, int x, int y, int width, int height, Component component) {
        super(font, x, y, width, height, component);
        this.font = font;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        boolean showCursor = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L;
        String text = this.getValue();
        if (!text.isEmpty()) {
            guiGraphics.drawString(this.font, text, this.getX(), this.getY(), 0x2B2D30, false);
        }
        if (showCursor) {
            int cursorX = this.getX() + this.font.width(text); // 光标在文字末尾
            int cursorY = this.getY();
            guiGraphics.drawString(this.font, "_", cursorX, cursorY, 0x2B2D30, false);
        }
    }
}
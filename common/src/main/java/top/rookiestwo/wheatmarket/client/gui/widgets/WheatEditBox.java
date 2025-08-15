package top.rookiestwo.wheatmarket.client.gui.widgets;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import top.rookiestwo.wheatmarket.WheatMarket;

public class WheatEditBox extends EditBox {

    private Font font;
    private long focusedTime;
    private int textColor;
    private int displayPos;//文本起始显示位置

    public WheatEditBox(Font font, int x, int y, int width, int height, Component component) {
        super(font, x, y, width, height, component);
        this.font = font;
    }

    public int getTextColor() {
        return this.textColor;
    }

    @Override
    public void setTextColor(int color) {
        this.textColor = color;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        WheatMarket.LOGGER.info("按下键盘: keyCode={}, scanCode={}, modifiers={}", keyCode, scanCode, modifiers);
        // 处理键盘输入逻辑
        if (keyCode == 257 || keyCode == 335) { // Enter key
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.isVisible()) {
            return;
        }

        boolean showCursor = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L;
        String fullText = this.getValue();
        this.displayPos = Math.min(this.displayPos, fullText.length());
        //从displayPos开始截取字符串，直到文本宽度超过EditBox的宽度
        String visibleText = this.font.plainSubstrByWidth(fullText.substring(this.displayPos), this.getInnerWidth());


        int cursorOffset = this.getCursorPosition() - this.displayPos;
        if (!visibleText.isEmpty()) {
            guiGraphics.drawString(this.font, visibleText, this.getX(), this.getY(), this.getTextColor(), false);
        }
        if (showCursor) {
            int cursorX = this.getX() + this.font.width(visibleText.substring(0, Math.max(Math.min(getCursorPosition(), visibleText.length()), 0))); // 光标在文字末尾
            //int cursorY = this.getY();
            //int lineHeight = this.font.lineHeight;
            //guiGraphics.fill(cursorX, cursorY, cursorX + 1, cursorY + lineHeight, this.getTextColor());
            //guiGraphics.drawString(this.font, "_", cursorX, cursorY, this.getTextColor(), false);
        }
    }
}
package top.rookiestwo.wheatmarket.client.gui.widgets;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import top.rookiestwo.wheatmarket.mixin.EditBoxAccessor;

import java.util.Objects;
import java.util.function.BiFunction;

public class WheatEditBox extends EditBox {
    private int displayPos;
    private int textColor;
    private Font font;
    private long focusedTime;
    private int highlightPos;
    private BiFunction<String, Integer, FormattedCharSequence> formatter;
    private int maxLength;

    public WheatEditBox(Font font, int x, int y, int width, int height, Component component) {
        super(font, x, y, width, height, component);
    }

    private void getPrivateValues(){
        displayPos=((EditBoxAccessor)this).getDisplayPos();
        textColor=((EditBoxAccessor)this).getTextColor();
        font=((EditBoxAccessor)this).getFont();
        focusedTime=((EditBoxAccessor)this).getFocusedTime();
        highlightPos=((EditBoxAccessor)this).getHighlightPos();
        formatter=((EditBoxAccessor)this).getFormatter();
        maxLength=((EditBoxAccessor)this).getMaxLength();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        getPrivateValues();
        if (this.isVisible()) {
            // 计算光标在显示区域内的偏移量
            int cursorOffset = this.getCursorPosition() - this.displayPos;

            // 截取适合当前控件宽度的显示文本
            String displayText = this.font.plainSubstrByWidth(this.getValue().substring(this.displayPos), this.getInnerWidth());

            // 判断光标是否在显示区域内
            boolean isCursorInDisplay = cursorOffset >= 0 && cursorOffset <= displayText.length();

            // 判断是否需要闪烁光标（聚焦状态下每300ms切换一次）
            boolean shouldBlinkCursor = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L && isCursorInDisplay;

            // 计算文本绘制的起始坐标（考虑边框偏移）
            int textX = this.getX();
            int textY = this.getY();

            // 当前绘制位置
            int currentX = textX;

            // 计算高亮区域的结束位置
            int highlightEnd = Mth.clamp(this.highlightPos - this.displayPos, 0, displayText.length());

            // 绘制文本前半部分
            if (!displayText.isEmpty()) {
                String firstPart = isCursorInDisplay ? displayText.substring(0, cursorOffset) : displayText;
                currentX = guiGraphics.drawString(
                        this.font,
                        (FormattedCharSequence)this.formatter.apply(firstPart, this.displayPos),
                        textX,
                        textY,
                        textColor,
                        false
                );
            }

            // 判断光标是否在文本末尾或已达到最大长度
            boolean isCursorAtEndOrMaxed = this.getCursorPosition() < this.getValue().length() || this.getValue().length() >= this.maxLength;

            // 计算光标位置
            int cursorX = currentX;
            if (!isCursorInDisplay) {
                // 如果光标不在显示区域，根据位置调整光标坐标
                cursorX = cursorOffset > 0 ? textX + this.width : textX;
            } else if (isCursorAtEndOrMaxed) {
                // 如果在末尾或已达最大长度，调整光标位置
                cursorX = currentX - 1;
                --currentX;
            }

            // 绘制文本后半部分
            if (!displayText.isEmpty() && isCursorInDisplay && cursorOffset < displayText.length()) {
                guiGraphics.drawString(
                        this.font,
                        (FormattedCharSequence)this.formatter.apply(displayText.substring(cursorOffset), this.getCursorPosition()),
                        currentX,
                        textY,
                        textColor,
                        false
                );
            }

            // 绘制光标或闪烁效果
            if (shouldBlinkCursor) {
                if (isCursorAtEndOrMaxed) {
                    // 绘制高亮矩形（在末尾时）
                    RenderType overlay = RenderType.guiOverlay();
                    int highlightYStart = textY - 1;
                    int highlightXEnd = cursorX + 1;
                    int highlightYEnd = textY + 1;
                    Objects.requireNonNull(this.font);
                    guiGraphics.fill(overlay, cursorX, highlightYStart, highlightXEnd, highlightYEnd + 7, 0xFF000000+textColor);
                } else {
                    // 绘制高亮矩形（在末尾时）
                    RenderType overlay = RenderType.guiOverlay();
                    int highlightYStart = textY - 1;
                    int highlightXEnd = cursorX + 1;
                    int highlightYEnd = textY + 1;
                    Objects.requireNonNull(this.font);
                    guiGraphics.fill(overlay, cursorX, highlightYStart, highlightXEnd, highlightYEnd + 7, 0xFF000000+textColor);
                    //guiGraphics.drawString(this.font, "_", cursorX, textY, textColor, false);
                }
            }

            // 绘制高亮选区（当有选中文本时）
            if (highlightEnd != cursorOffset) {
                int highlightXStart = textX + this.font.width(displayText.substring(0, highlightEnd));
                int highlightYStart = textY - 1;
                int highlightXEnd = highlightXStart - 1;
                int highlightYEnd = textY + 1;
                Objects.requireNonNull(this.font);
                ((EditBoxAccessor)this).invokeRenderHighlight(guiGraphics, cursorX, highlightYStart, highlightXEnd, highlightYEnd + 9);
            }
        }
    }
}
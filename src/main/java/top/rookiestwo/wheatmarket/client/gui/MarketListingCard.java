package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

import java.util.Locale;
import java.util.function.BiConsumer;

public final class MarketListingCard {
    private static final int CARD_WIDTH = 64;
    private static final int CARD_HEIGHT = 64;
    private static final int TEXT_COLOR = 0x151515;
    private static final int PRICE_COLOR = 0x5C4024;
    private static final int HOVER_TINT = 0xFFBEBEBE;
    private static final int ITEM_FRAME_FILL_COLOR = 0x00000000;
    private static final int ITEM_FRAME_BORDER_COLOR = 0xFF7A5532;
    private static final int ITEM_FRAME_BORDER_WIDTH = 1;
    private static final int ITEM_ICON_SIZE = 16;
    private static final int TRANSPARENT = 0x00000000;

    private MarketListingCard() {
    }

    public static UIElement create(MarketListS2CPacket.MarketItemSummary item, ItemStack stack) {
        return create(item, stack, null);
    }

    public static UIElement create(MarketListS2CPacket.MarketItemSummary item, ItemStack stack,
                                   BiConsumer<MarketListS2CPacket.MarketItemSummary, ItemStack> onClick) {
        Label tradeLabel = label(item.isIfSell() ? "gui.wheatmarket.market.sell" : "gui.wheatmarket.market.buy_order");
        tradeLabel.lss("align-self", "center");
        tradeLabel.layout(layout -> layout.width(32).height(9).flexShrink(0));

        UIElement itemFrame = new UIElement()
                .lss("align-self", "center")
                .layout(layout -> layout.width(17).height(17).flexShrink(0).paddingLeft(1)
                        .paddingRight(1)
                        .paddingTop(1)
                        .paddingBottom(1).marginBottom(2))
                .lss("align-items", "center")
                .lss("justify-content", "center")
                .style(style -> style.background(GuiTextureGroup.of(
                        new ColorRectTexture(ITEM_FRAME_FILL_COLOR),
                        new ColorBorderTexture(ITEM_FRAME_BORDER_WIDTH, ITEM_FRAME_BORDER_COLOR)
                )))
                .addChild(new UIElement()
                        .layout(layout -> layout.width(ITEM_ICON_SIZE).height(ITEM_ICON_SIZE).flexShrink(0))
                        .style(style -> style.background(new ItemStackTexture(stack))));
        itemFrame.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = WheatMarketItemTooltips.forStack(stack));

        UIElement[] cardRootRef = new UIElement[1];

        Label priceLabel = label(Component.literal(formatMoney(item.getPrice())));
        priceLabel.lss("align-self", "center");
        priceLabel.layout(layout -> layout.width(48).height(9).flexShrink(0));
        priceLabel.textStyle(style -> style.textColor(PRICE_COLOR));

        UIElement cardContent = new UIElement()
                .lss("flex-direction", "column")
                .layout(layout -> layout
                        .width(CARD_WIDTH)
                        .height(CARD_HEIGHT)
                        .paddingLeft(4)
                        .paddingRight(4)
                        .paddingTop(9)
                        .paddingBottom(8)
                        .gapAll(1)
                        .flexShrink(0))
                .style(style -> style
                        .background(IGuiTexture.dynamic(() -> WheatMarketUiTextures.wrinkledPaperTexture(
                                item.getMarketItemID(),
                                cardRootRef[0] != null && cardRootRef[0].isSelfOrChildHover() ? HOVER_TINT : -1
                        )))
                        .zIndex(1))
                .addChildren(
                        tradeLabel,
                        itemFrame,
                        priceLabel
                );

        Button cardRoot = new Button().noText();
        cardRoot.buttonStyle(style -> style
                .baseTexture(new ColorRectTexture(TRANSPARENT))
                .hoverTexture(new ColorRectTexture(TRANSPARENT))
                .pressedTexture(new ColorRectTexture(TRANSPARENT)));
        cardRoot.layout(layout -> layout.width(CARD_WIDTH).height(CARD_HEIGHT).flexShrink(0));
        cardRoot.addChildren(cardContent);
        if (onClick != null) {
            cardRoot.setOnClick(event -> onClick.accept(item, stack.copy()));
        }
        cardRootRef[0] = cardRoot;

        return cardRoot;
    }

    private static Label label(String translationKey) {
        return label(Component.translatable(translationKey));
    }

    private static Label label(Component text) {
        Label label = new Label();
        label.setText(text);
        label.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(TEXT_COLOR)
                .textShadow(false));
        return label;
    }

    private static String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

}

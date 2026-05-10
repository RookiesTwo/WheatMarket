package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.w3c.dom.Document;

public final class PaperFormFactory {

    private static final ResourceLocation PAPER_FORM_XML =
            ResourceLocation.parse("wheatmarket:ui/paper_form.xml");
    private static final int ITEM_PREVIEW_BORDER = 0xFF3A332C;
    private static final IGuiTexture EMPTY_OVERLAY = new ColorRectTexture(0x00000000);

    private PaperFormFactory() {
    }

    public static SharedPaperForm create(Player player) {
        Document xml = XmlUtils.loadXml(PAPER_FORM_XML);
        if (xml == null) {
            throw new IllegalStateException("Failed to load UI xml: " + PAPER_FORM_XML);
        }

        UI loadedUi = UI.of(xml);
        UI ui = UI.of(loadedUi.getRootElement(), loadedUi.getStylesheets(), availableSize -> availableSize);

        UIElement root = WheatMarketUiHelpers.require(ui, "paper-form-root", UIElement.class);
        UIElement paperPanel = WheatMarketUiHelpers.require(ui, "paper-panel", UIElement.class);
        ScrollerView scroller = WheatMarketUiHelpers.require(ui, "form-scroll", ScrollerView.class);
        UIElement leftExtra = WheatMarketUiHelpers.require(ui, "form-left-extra", UIElement.class);
        UIElement rightContent = WheatMarketUiHelpers.require(ui, "form-right-content", UIElement.class);
        UIElement bottomBar = WheatMarketUiHelpers.require(ui, "paper-bottom-bar", UIElement.class);
        UIElement actionContent = WheatMarketUiHelpers.require(ui, "action-column-content", UIElement.class);
        UIElement opPanel = WheatMarketUiHelpers.require(ui, "op-panel", UIElement.class);

        Label title = WheatMarketUiHelpers.require(ui, "form-title", Label.class);
        Label playerBalanceLabel = WheatMarketUiHelpers.require(ui, "player-balance", Label.class);
        Label ownerLabel = WheatMarketUiHelpers.require(ui, "owner-label", Label.class);
        Label opTitle = WheatMarketUiHelpers.require(ui, "op-title", Label.class);
        UIElement playerAvatar = WheatMarketUiHelpers.require(ui, "player-avatar", UIElement.class);
        UIElement itemPreview = WheatMarketUiHelpers.require(ui, "item-preview", UIElement.class);
        UIElement itemIcon = WheatMarketUiHelpers.require(ui, "form-item-icon", UIElement.class);
        Button toggleAdminButton = WheatMarketUiHelpers.require(ui, "toggle-admin-button", Button.class);
        Button toggleInfiniteButton = WheatMarketUiHelpers.require(ui, "toggle-infinite-button", Button.class);
        Button toggleInfiniteDurationButton = WheatMarketUiHelpers.require(ui, "toggle-infinite-duration-button", Button.class);
        TextField cooldownAmountField = WheatMarketUiHelpers.require(ui, "cooldown-amount-field", TextField.class);
        TextField cooldownDaysField = WheatMarketUiHelpers.require(ui, "cooldown-days-field", TextField.class);
        TextField cooldownHoursField = WheatMarketUiHelpers.require(ui, "cooldown-hours-field", TextField.class);
        TextField cooldownMinutesField = WheatMarketUiHelpers.require(ui, "cooldown-minutes-field", TextField.class);
        Label cooldownAmountCaption = WheatMarketUiHelpers.require(ui, "cooldown-amount-caption", Label.class);
        Label cooldownDaysCaption = WheatMarketUiHelpers.require(ui, "cooldown-days-caption", Label.class);
        Label cooldownHoursCaption = WheatMarketUiHelpers.require(ui, "cooldown-hours-caption", Label.class);
        Label cooldownMinutesCaption = WheatMarketUiHelpers.require(ui, "cooldown-minutes-caption", Label.class);
        Label orderTimeSectionTitle = WheatMarketUiHelpers.require(ui, "order-time-section-title", Label.class);
        TextField orderDaysField = WheatMarketUiHelpers.require(ui, "order-days-field", TextField.class);
        TextField orderHoursField = WheatMarketUiHelpers.require(ui, "order-hours-field", TextField.class);
        TextField orderMinutesField = WheatMarketUiHelpers.require(ui, "order-minutes-field", TextField.class);
        Label orderDaysCaption = WheatMarketUiHelpers.require(ui, "order-days-caption", Label.class);
        Label orderHoursCaption = WheatMarketUiHelpers.require(ui, "order-hours-caption", Label.class);
        Label orderMinutesCaption = WheatMarketUiHelpers.require(ui, "order-minutes-caption", Label.class);

        root.style(style -> style.background(WheatMarketUiTextures.tradingBackgroundTexture()));
        paperPanel.style(style -> style.background(WheatMarketUiTextures.tradingPaperTexture()));
        playerAvatar.style(style -> style.background(WheatMarketUiTextures.playerAvatarTexture(player)));
        playerBalanceLabel.style(style -> style.background(WheatMarketUiTextures.paperTexture()));
        itemPreview.style(style -> style.background(new ColorBorderTexture(-1, ITEM_PREVIEW_BORDER)));
        configureScroller(scroller);

        return new SharedPaperForm(
                ui, leftExtra, rightContent, bottomBar, actionContent,
                title, playerBalanceLabel, playerAvatar, itemPreview, itemIcon,
                opPanel, opTitle, toggleAdminButton, toggleInfiniteButton, toggleInfiniteDurationButton,
                cooldownAmountField, cooldownDaysField, cooldownHoursField, cooldownMinutesField,
                cooldownAmountCaption, cooldownDaysCaption, cooldownHoursCaption, cooldownMinutesCaption,
                orderTimeSectionTitle,
                orderDaysField, orderHoursField, orderMinutesField,
                orderDaysCaption, orderHoursCaption, orderMinutesCaption,
                scroller, paperPanel, root, ownerLabel
        );
    }

    private static void configureScroller(ScrollerView scroller) {
        scroller.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .verticalScrollDisplay(ScrollDisplay.NEVER)
                .scrollerViewStyle(0.0F)
                .minScrollPixel(8.0F)
                .maxScrollPixel(24.0F));
        scroller.style(style -> style.background(EMPTY_OVERLAY));
        scroller.viewPort(viewPort -> viewPort
                .layout(layout -> layout.paddingAll(0.0F))
                .style(style -> style.background(EMPTY_OVERLAY)));
        scroller.viewContainer(container -> container.style(style -> style.background(EMPTY_OVERLAY)));
        scroller.verticalContainer(container -> container.style(style -> style.background(EMPTY_OVERLAY)));
        scroller.horizontalScroller(horizontalScroller -> horizontalScroller.setDisplay(false));
        scroller.verticalScroller(verticalScroller -> verticalScroller.setDisplay(false));
    }

    public static PlayerColumn createPlayerColumn(Player player) {
        UIElement column = new UIElement();
        column.lss("id", "player-column");
        column.lss("width", "22%");
        column.lss("min-width", "68");
        column.lss("max-width", "150");
        column.lss("height", "100%");
        column.lss("flex-direction", "column");
        column.lss("align-items", "center");
        column.lss("justify-content", "center");
        column.lss("gap-all", "8");
        column.lss("flex-shrink", "1");
        column.lss("overflow", "hidden");

        UIElement avatar = new UIElement();
        avatar.lss("id", "player-avatar");
        avatar.lss("width", "54");
        avatar.lss("height", "54");
        avatar.lss("max-width", "100%");
        avatar.lss("flex-shrink", "0");
        avatar.style(style -> style.background(WheatMarketUiTextures.playerAvatarTexture(player)));

        Label balanceLabel = new Label();
        balanceLabel.lss("id", "player-balance");
        balanceLabel.lss("width", "100%");
        balanceLabel.lss("height", "22");
        balanceLabel.lss("min-height", "18");
        balanceLabel.lss("flex-shrink", "0");
        balanceLabel.lss("horizontal-align", "CENTER");
        balanceLabel.lss("vertical-align", "CENTER");
        balanceLabel.lss("text-wrap", "HIDE");
        balanceLabel.lss("text-shadow", "false");
        balanceLabel.lss("padding-left", "6");
        balanceLabel.lss("padding-right", "6");
        balanceLabel.style(style -> style.background(WheatMarketUiTextures.paperTexture()));

        column.addChildren(avatar, balanceLabel);
        return new PlayerColumn(column, avatar, balanceLabel);
    }

    public record PlayerColumn(UIElement column, UIElement avatar, Label balanceLabel) {
    }

    public record SharedPaperForm(
            UI ui,
            UIElement leftExtra,
            UIElement rightContent,
            UIElement bottomBar,
            UIElement actionContent,
            Label title,
            Label playerBalanceLabel,
            UIElement playerAvatar,
            UIElement itemPreview,
            UIElement itemIcon,
            UIElement opPanel,
            Label opTitle,
            Button toggleAdminButton,
            Button toggleInfiniteButton,
            Button toggleInfiniteDurationButton,
            TextField cooldownAmountField,
            TextField cooldownDaysField,
            TextField cooldownHoursField,
            TextField cooldownMinutesField,
            Label cooldownAmountCaption,
            Label cooldownDaysCaption,
            Label cooldownHoursCaption,
            Label cooldownMinutesCaption,
            Label orderTimeSectionTitle,
            TextField orderDaysField,
            TextField orderHoursField,
            TextField orderMinutesField,
            Label orderDaysCaption,
            Label orderHoursCaption,
            Label orderMinutesCaption,
            ScrollerView scroller,
            UIElement paperPanel,
            UIElement root,
            Label ownerLabel
    ) {
    }
}

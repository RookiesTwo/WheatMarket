package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.c2s.RequestMarketListC2SPacket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Locale;

public class WheatMarketHomeUI {
    private static final ResourceLocation HOME_XML = ResourceLocation.parse("wheatmarket:ui/market_home.xml");

    private Selector<TradeFilter> tradeSelector;
    private Selector<SourceFilter> sourceSelector;
    private Selector<SortFilter> sortSelector;
    private TextField searchField;
    private ScrollerView productScroller;
    private UIElement rootElement;
    private UIElement topBar;
    private UIElement navigationBar;
    private UIElement actionBar;
    private UIElement marketPanel;
    private UIElement paginationBar;
    private Button marketButton;
    private Button listingButton;
    private Button myItemsButton;
    private Button searchButton;
    private Button refreshButton;
    private Button previousButton;
    private Button nextButton;
    private Label pageLabel;
    private Label statusLabel;
    private Label titleLabel;
    private Label balanceLabel;
    private int requestedPage;
    private int seenListVersion = -1;

    public ModularUI create(Player player) {
        Document xml = XmlUtils.loadXml(HOME_XML);
        if (xml == null) {
            throw new IllegalStateException("Failed to load UI xml: " + HOME_XML);
        }

        UI loadedUi = UI.of(xml);
        UI ui = UI.of(loadedUi.getRootElement(), loadedUi.getStylesheets(), availableSize -> availableSize);
        bindStaticElements(ui);
        applyTextures();
        applyLogic();
        return ModularUI.of(ui, player);
    }

    public void requestCurrentPage() {
        TradeFilter tradeFilter = tradeSelector.getValue() == null ? TradeFilter.ALL : tradeSelector.getValue();
        SourceFilter sourceFilter = sourceSelector.getValue() == null ? SourceFilter.ALL : sourceSelector.getValue();
        SortFilter sortFilter = sortSelector.getValue() == null ? SortFilter.LIST_TIME : sortSelector.getValue();
        WheatMarketNetwork.sendToServer(new RequestMarketListC2SPacket(
                tradeFilter.packetValue,
                sourceFilter.packetValue,
                sortFilter.packetValue,
                searchField.getValue(),
                requestedPage
        ));
    }

    public void tick() {
        if (seenListVersion != WheatMarket.CLIENT_MARKET_LIST_VERSION) {
            seenListVersion = WheatMarket.CLIENT_MARKET_LIST_VERSION;
            rebuildMarketList();
        }
    }

    private void bindStaticElements(UI ui) {
        rootElement = require(ui, "market-root", UIElement.class);
        topBar = require(ui, "top-bar", UIElement.class);
        navigationBar = require(ui, "navigation-bar", UIElement.class);
        actionBar = require(ui, "action-bar", UIElement.class);
        marketPanel = require(ui, "market-panel", UIElement.class);
        paginationBar = require(ui, "pagination-bar", UIElement.class);

        titleLabel = require(ui, "title-label", Label.class);
        balanceLabel = require(ui, "balance-label", Label.class);
        statusLabel = require(ui, "status-label", Label.class);
        pageLabel = require(ui, "page-label", Label.class);

        marketButton = require(ui, "market-button", Button.class);
        listingButton = require(ui, "listing-button", Button.class);
        myItemsButton = require(ui, "my-items-button", Button.class);
        searchButton = require(ui, "search-button", Button.class);
        refreshButton = require(ui, "refresh-button", Button.class);
        previousButton = require(ui, "previous-button", Button.class);
        nextButton = require(ui, "next-button", Button.class);

        tradeSelector = requireSelector(ui, "trade-selector");
        sourceSelector = requireSelector(ui, "source-selector");
        sortSelector = requireSelector(ui, "sort-selector");
        searchField = require(ui, "search-field", TextField.class);
        productScroller = require(ui, "product-scroller", ScrollerView.class);
    }

    private void applyTextures() {
        rootElement.style(style -> style.background(WheatMarketUiTextures.rootBackground()));
        topBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        navigationBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        actionBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        marketPanel.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        paginationBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        productScroller.viewPort(viewPort -> viewPort.style(style -> style.background(WheatMarketUiTextures.panelTexture())));

        styleButton(marketButton, WheatMarketUiTextures.SHOPPING_CART_ICON_TEXTURE);
        styleButton(listingButton, WheatMarketUiTextures.SELL_ICON_TEXTURE);
        styleButton(myItemsButton, WheatMarketUiTextures.FAVORITE_ICON_TEXTURE);
        styleButton(searchButton, WheatMarketUiTextures.SEARCH_ICON_TEXTURE);
        styleButton(refreshButton, WheatMarketUiTextures.SETTINGS_ICON_TEXTURE);
        styleButton(previousButton, WheatMarketUiTextures.SUBTRACT_ICON_TEXTURE);
        styleButton(nextButton, WheatMarketUiTextures.ADD_ICON_TEXTURE);

        styleField(tradeSelector);
        styleField(sourceSelector);
        styleField(sortSelector);
        styleField(searchField);
    }

    private void applyLogic() {
        titleLabel.setText("gui.wheatmarket.main_title", true);
        balanceLabel.setText(Component.translatable("gui.wheatmarket.balance", formatMoney(WheatMarket.CLIENT_BALANCE)));
        statusLabel.setText("gui.wheatmarket.market.loading", true);
        pageLabel.setText(Component.translatable("gui.wheatmarket.market.page", 1, 1));

        tradeSelector.setCandidates(List.of(TradeFilter.ALL, TradeFilter.SELL, TradeFilter.BUY))
                .setSelected(TradeFilter.ALL)
                .setOnValueChanged(value -> resetAndRequest());
        sourceSelector.setCandidates(List.of(SourceFilter.ALL, SourceFilter.SYSTEM, SourceFilter.PLAYER))
                .setSelected(SourceFilter.ALL)
                .setOnValueChanged(value -> resetAndRequest());
        sortSelector.setCandidates(List.of(SortFilter.LIST_TIME, SortFilter.ITEM_ID, SortFilter.LAST_TRADE))
                .setSelected(SortFilter.LIST_TIME)
                .setOnValueChanged(value -> resetAndRequest());

        searchField.setText("");
        searchField.textFieldStyle(style -> style.placeholder(Component.translatable("gui.wheatmarket.searchbar")));

        searchButton.setOnClick(event -> resetAndRequest());
        refreshButton.setOnClick(event -> requestCurrentPage());
        previousButton.setOnClick(event -> {
            if (requestedPage > 0) {
                requestedPage--;
                requestCurrentPage();
            }
        });
        nextButton.setOnClick(event -> {
            if (requestedPage + 1 < Math.max(1, WheatMarket.CLIENT_TOTAL_PAGES)) {
                requestedPage++;
                requestCurrentPage();
            }
        });
    }

    private void resetAndRequest() {
        requestedPage = 0;
        requestCurrentPage();
    }

    private void rebuildMarketList() {
        productScroller.clearAllScrollViewChildren();
        List<MarketListS2CPacket.MarketItemSummary> items = WheatMarket.CLIENT_MARKET_LIST;
        if (items == null) {
            statusLabel.setText("gui.wheatmarket.market.loading", true);
            return;
        }

        statusLabel.setText(Component.translatable("gui.wheatmarket.market.result_count", items.size()));
        requestedPage = WheatMarket.CLIENT_CURRENT_PAGE;
        pageLabel.setText(Component.translatable(
                "gui.wheatmarket.market.page",
                WheatMarket.CLIENT_CURRENT_PAGE + 1,
                Math.max(1, WheatMarket.CLIENT_TOTAL_PAGES)
        ));

        if (items.isEmpty()) {
            productScroller.addScrollViewChild(new Label()
                    .setText("gui.wheatmarket.market.empty", true)
                    .layout(layout -> layout.widthPercent(100).height(18)));
            return;
        }

        UIElement grid = new UIElement()
                .lss("flex-direction", "row")
                .lss("flex-wrap", "wrap")
                .layout(layout -> layout.widthPercent(100).gapAll(8));
        for (MarketListS2CPacket.MarketItemSummary item : items) {
            grid.addChild(createProductCard(item));
        }
        productScroller.addScrollViewChild(grid);
    }

    private UIElement createProductCard(MarketListS2CPacket.MarketItemSummary item) {
        ItemStack stack = itemStackFromSummary(item);
        UIElement icon = new UIElement()
                .layout(layout -> layout.width(28).height(28))
                .style(style -> style.background(GuiTextureGroup.of(
                        WheatMarketUiTextures.paperTexture(),
                        new ItemStackTexture(stack)
                )));
        UIElement badges = new UIElement()
                .lss("flex-direction", "row")
                .layout(layout -> layout.widthPercent(100).height(14).gapAll(4))
                .addChildren(
                        new Label().setText(item.isIfSell() ? "gui.wheatmarket.market.sell" : "gui.wheatmarket.market.buy_order", true),
                        new Label().setText(item.isIfAdmin() ? "gui.wheatmarket.market.system_shop" : "gui.wheatmarket.market.player_shop", true)
                );

        if (item.isHasCooldown()) {
            badges.addChild(new Label().setText("gui.wheatmarket.market.cooldown", true));
        }

        Button detailButton = new Button().setText("gui.wheatmarket.market.detail", true);
        detailButton.layout(layout -> layout.widthPercent(100).height(18));
        styleButton(detailButton, WheatMarketUiTextures.EDIT_ICON_TEXTURE);

        UIElement card = new UIElement()
                .addClass("panel_bg")
                .layout(layout -> layout.width(138).height(122).paddingAll(6).gapAll(4))
                .addChildren(
                        icon,
                        new Label().setText(stack.getHoverName()).layout(layout -> layout.widthPercent(100).height(14)),
                        new Label().setText(Component.translatable("gui.wheatmarket.market.price", formatMoney(item.getPrice()))),
                        new Label().setText(Component.translatable("gui.wheatmarket.market.stock", formatAmount(item.getAmount()))),
                        new Label().setText(Component.translatable("gui.wheatmarket.market.seller", shortSeller(item))),
                        badges,
                        detailButton
                );
        card.style(style -> style.background(WheatMarketUiTextures.cardTexture()));
        return card;
    }

    private void styleButton(Button button, String iconTexturePath) {
        button.buttonStyle(style -> style
                .baseTexture(WheatMarketUiTextures.buttonBaseTexture())
                .hoverTexture(WheatMarketUiTextures.buttonPressedTexture())
                .pressedTexture(WheatMarketUiTextures.buttonPressedTexture())
        );
        button.addPreIcon(WheatMarketUiTextures.iconTexture(iconTexturePath));
    }

    private void styleField(UIElement element) {
        element.style(style -> style.background(WheatMarketUiTextures.paperTexture()));
    }

    private <T> T require(UI ui, String id, Class<T> type) {
        return ui.selectId(id, type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing UI element: " + id));
    }

    @SuppressWarnings("unchecked")
    private <T> Selector<T> requireSelector(UI ui, String id) {
        return (Selector<T>) require(ui, id, Selector.class);
    }

    private ItemStack itemStackFromSummary(MarketListS2CPacket.MarketItemSummary item) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || item.getItemNBT() == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ItemStack.parseOptional(minecraft.level.registryAccess(), item.getItemNBT());
        stack.setCount(1);
        return stack;
    }

    private String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatAmount(int amount) {
        return amount == Integer.MAX_VALUE
                ? Component.translatable("gui.wheatmarket.market.infinite").getString()
                : String.valueOf(amount);
    }

    private String shortSeller(MarketListS2CPacket.MarketItemSummary item) {
        if (item.isIfAdmin()) {
            return Component.translatable("gui.wheatmarket.market.system_shop").getString();
        }
        String value = item.getSellerID().toString();
        return value.substring(0, Math.min(8, value.length()));
    }

    private enum TradeFilter {
        ALL("gui.wheatmarket.filter.all", 0),
        SELL("gui.wheatmarket.filter.sell", 1),
        BUY("gui.wheatmarket.filter.buy", 2);

        private final String translationKey;
        private final int packetValue;

        TradeFilter(String translationKey, int packetValue) {
            this.translationKey = translationKey;
            this.packetValue = packetValue;
        }

        @Override
        public String toString() {
            return Component.translatable(translationKey).getString();
        }
    }

    private enum SourceFilter {
        ALL("gui.wheatmarket.filter.all", 0),
        SYSTEM("gui.wheatmarket.filter.system", 1),
        PLAYER("gui.wheatmarket.filter.player", 2);

        private final String translationKey;
        private final int packetValue;

        SourceFilter(String translationKey, int packetValue) {
            this.translationKey = translationKey;
            this.packetValue = packetValue;
        }

        @Override
        public String toString() {
            return Component.translatable(translationKey).getString();
        }
    }

    private enum SortFilter {
        LIST_TIME("gui.wheatmarket.filter.list_time", 0),
        ITEM_ID("gui.wheatmarket.filter.item_id", 1),
        LAST_TRADE("gui.wheatmarket.filter.last_trade", 2);

        private final String translationKey;
        private final int packetValue;

        SortFilter(String translationKey, int packetValue) {
            this.translationKey = translationKey;
            this.packetValue = packetValue;
        }

        @Override
        public String toString() {
            return Component.translatable(translationKey).getString();
        }
    }
}

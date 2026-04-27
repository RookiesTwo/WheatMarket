package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaPositionType;
import org.w3c.dom.Document;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.c2s.RequestMarketListC2SPacket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public class WheatMarketHomeUI {
    private static final ResourceLocation HOME_XML = ResourceLocation.parse("wheatmarket:ui/market_home.xml");
    private static final int PRODUCT_CARD_WIDTH = 64;
    private static final int PRODUCT_CARD_HEIGHT = 64;
    private static final int PRODUCT_CARD_GAP = 10;
    private static final int MAX_PRODUCTS_PER_PAGE = 64;
    private static final int MAX_LOCALIZED_SEARCH_IDS = 2048;
    private static final IGuiTexture EMPTY_TEXTURE = new ColorRectTexture(0x00000000);

    private final BiConsumer<MarketListS2CPacket.MarketItemSummary, ItemStack> orderRequestHandler;

    private Selector<TradeFilter> tradeSelector;
    private Selector<SourceFilter> sourceSelector;
    private Selector<SortFilter> sortSelector;
    private TextField searchField;
    private UIElement productBoard;
    private UIElement rootElement;
    private UIElement titleLogo;
    private UIElement topBar;
    private UIElement sideBar;
    private UIElement actionBar;
    private UIElement marketPanel;
    private UIElement paginationBar;
    private UIElement myItemsOverlay;
    private Button listingButton;
    private Button sortButton;
    private Button myItemsButton;
    private Button searchButton;
    private Button refreshButton;
    private Button previousButton;
    private Button nextButton;
    private Label pageLabel;
    private Label balanceLabel;
    private boolean sortAscending = SortFilter.LIST_TIME.defaultAscending;
    private boolean ownListingsOnly;
    private int requestedPage;
    private int seenListVersion = -1;

    public WheatMarketHomeUI() {
        this(null);
    }

    public WheatMarketHomeUI(BiConsumer<MarketListS2CPacket.MarketItemSummary, ItemStack> orderRequestHandler) {
        this.orderRequestHandler = orderRequestHandler;
    }

    public ModularUI create(Player player) {
        Document xml = XmlUtils.loadXml(HOME_XML);
        if (xml == null) {
            throw new IllegalStateException("Failed to load UI xml: " + HOME_XML);
        }

        UI loadedUi = UI.of(xml);
        UI ui = UI.of(loadedUi.getRootElement(), loadedUi.getStylesheets(), availableSize -> availableSize);
        bindStaticElements(ui);
        applyTextures(player);
        applyLogic();
        return ModularUI.of(ui, player);
    }

    public void requestCurrentPage() {
        TradeFilter tradeFilter = tradeSelector.getValue() == null ? TradeFilter.ALL : tradeSelector.getValue();
        SourceFilter sourceFilter = sourceSelector.getValue() == null ? SourceFilter.ALL : sourceSelector.getValue();
        SortFilter sortFilter = selectedSortFilter();
        WheatMarketNetwork.sendToServer(new RequestMarketListC2SPacket(
                tradeFilter.packetValue,
                sourceFilter.packetValue,
                sortFilter.packetValue,
                sortAscending,
                ownListingsOnly,
                searchField.getValue(),
                resolveLocalizedSearchItemIds(searchField.getValue()),
                requestedPage,
                calculateProductsPerPage()
        ));
    }

    public boolean submitSearchIfSearchFieldFocused() {
        if (searchField == null || !searchField.isFocused()) {
            return false;
        }
        resetAndRequest();
        return true;
    }

    public void tick() {
        if (seenListVersion != WheatMarket.CLIENT_MARKET_LIST_VERSION) {
            seenListVersion = WheatMarket.CLIENT_MARKET_LIST_VERSION;
            rebuildMarketList();
        }
    }

    public void setBalance(double balance) {
        balanceLabel.setText(Component.translatable("gui.wheatmarket.balance", formatMoney(balance)));
    }

    private void bindStaticElements(UI ui) {
        rootElement = require(ui, "market-root", UIElement.class);
        titleLogo = require(ui, "title-logo", UIElement.class);
        topBar = require(ui, "top-bar", UIElement.class);
        sideBar = require(ui, "side-bar", UIElement.class);
        actionBar = require(ui, "action-bar", UIElement.class);
        marketPanel = require(ui, "market-panel", UIElement.class);
        paginationBar = require(ui, "pagination-bar", UIElement.class);

        balanceLabel = require(ui, "balance-label", Label.class);
        pageLabel = require(ui, "page-label", Label.class);

        listingButton = require(ui, "listing-button", Button.class);
        sortButton = require(ui, "sort-button", Button.class);
        myItemsButton = require(ui, "my-items-button", Button.class);
        searchButton = require(ui, "search-button", Button.class);
        refreshButton = require(ui, "refresh-button", Button.class);
        previousButton = require(ui, "previous-button", Button.class);
        nextButton = require(ui, "next-button", Button.class);

        tradeSelector = requireSelector(ui, "trade-selector");
        sourceSelector = requireSelector(ui, "source-selector");
        sortSelector = requireSelector(ui, "sort-selector");
        searchField = require(ui, "search-field", TextField.class);
        productBoard = require(ui, "product-board", UIElement.class);
    }

    private void applyTextures(Player player) {
        rootElement.style(style -> style.background(WheatMarketUiTextures.rootBackground()));
        titleLogo.style(style -> style.background(WheatMarketUiTextures.titleTexture()));
        topBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        sideBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        actionBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        marketPanel.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        paginationBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        balanceLabel.style(style -> style.background(WheatMarketUiTextures.paperTexture()));

        styleButtonWithFixedHoverIcon(
                listingButton,
                WheatMarketUiTextures.SELL_ICON_TEXTURE,
                WheatMarketUiTextures.SELL_HOVERED_ICON_TEXTURE
        );
        styleButton(sortButton, WheatMarketUiTextures.FILTER_ICON_TEXTURE);
        styleIconButton(myItemsButton, WheatMarketUiTextures.playerAvatarTexture(player));
        installMyItemsOverlay();
        stylePlainButton(searchButton);
        stylePlainButton(refreshButton);
        stylePlainButton(previousButton);
        stylePlainButton(nextButton);

        styleField(tradeSelector);
        styleField(sourceSelector);
        styleField(sortSelector);
        styleField(searchField);
    }

    private void applyLogic() {
        balanceLabel.textStyle(style -> style
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(0x2B2116)
                .textShadow(false));
        setBalance(WheatMarket.CLIENT_BALANCE);
        pageLabel.setText(Component.translatable("gui.wheatmarket.market.page", 1, 1));

        tradeSelector.setCandidates(List.of(TradeFilter.ALL, TradeFilter.SELL, TradeFilter.BUY))
                .setSelected(TradeFilter.ALL)
                .setOnValueChanged(value -> resetAndRequest());
        sourceSelector.setCandidates(List.of(SourceFilter.ALL, SourceFilter.SYSTEM, SourceFilter.PLAYER))
                .setSelected(SourceFilter.ALL)
                .setOnValueChanged(value -> resetAndRequest());
        sortSelector.setCandidates(List.of(SortFilter.LIST_TIME, SortFilter.ITEM_ID, SortFilter.LAST_TRADE))
                .setSelected(SortFilter.LIST_TIME)
                .setOnValueChanged(value -> {
                    sortAscending = (value == null ? SortFilter.LIST_TIME : value).defaultAscending;
                    resetAndRequest();
                });

        searchField.setText("");
        searchField.textFieldStyle(style -> style
                .placeholder(Component.translatable("gui.wheatmarket.searchbar"))
                .textColor(0x151515)
                .cursorColor(0xFF151515)
                .errorColor(0x8C1D18)
                .focusOverlay(WheatMarketUiTextures.searchFieldFocusTexture())
                .textShadow(false));

        searchButton.setOnClick(event -> resetAndRequest());
        sortButton.setOnClick(event -> toggleSortDirection());
        myItemsButton.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = myItemsTooltips());
        myItemsButton.setOnClick(event -> toggleOwnListingsOnly());
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

    private void toggleSortDirection() {
        sortAscending = !sortAscending;
        resetAndRequest();
    }

    private void toggleOwnListingsOnly() {
        ownListingsOnly = !ownListingsOnly;
        resetAndRequest();
    }

    private int calculateProductsPerPage() {
        int boardWidth = resolveProductBoardWidth();
        int boardHeight = resolveProductBoardHeight();

        int columns = Math.max(1, (boardWidth + PRODUCT_CARD_GAP) / (PRODUCT_CARD_WIDTH + PRODUCT_CARD_GAP));
        int rows = Math.max(1, (boardHeight + PRODUCT_CARD_GAP) / (PRODUCT_CARD_HEIGHT + PRODUCT_CARD_GAP));
        return Math.max(1, Math.min(columns * rows, MAX_PRODUCTS_PER_PAGE));
    }

    private int resolveProductBoardWidth() {
        int boardWidth = Math.max(0, (int) productBoard.getContentWidth());
        if (boardWidth > 0) {
            return boardWidth;
        }
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return Math.max(PRODUCT_CARD_WIDTH, screenWidth - 112);
    }

    private int resolveProductBoardHeight() {
        int boardHeight = Math.max(0, (int) productBoard.getContentHeight());
        if (boardHeight > 0) {
            return boardHeight;
        }
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return Math.max(PRODUCT_CARD_HEIGHT, screenHeight - 132);
    }

    private void rebuildMarketList() {
        productBoard.clearAllChildren();
        List<MarketListS2CPacket.MarketItemSummary> items = WheatMarket.CLIENT_MARKET_LIST;
        if (items == null) {
            productBoard.addChild(new Label()
                    .setText("gui.wheatmarket.market.loading", true)
                    .layout(layout -> layout.widthPercent(100).height(18)));
            return;
        }

        requestedPage = WheatMarket.CLIENT_CURRENT_PAGE;
        pageLabel.setText(Component.translatable(
                "gui.wheatmarket.market.page",
                WheatMarket.CLIENT_CURRENT_PAGE + 1,
                Math.max(1, WheatMarket.CLIENT_TOTAL_PAGES)
        ));

        if (items.isEmpty()) {
            productBoard.addChild(new Label()
                    .setText("gui.wheatmarket.market.empty", true)
                    .layout(layout -> layout.widthPercent(100).height(18)));
            return;
        }

        for (MarketListS2CPacket.MarketItemSummary item : items) {
            ItemStack stack = itemStackFromSummary(item);
            productBoard.addChild(MarketListingCard.create(item, stack, orderRequestHandler));
        }
    }

    private void styleButton(Button button, String iconTexturePath) {
        stylePlainButton(button);
        button.addPreIcon(WheatMarketUiTextures.iconTexture(iconTexturePath));
    }

    private void stylePlainButton(Button button) {
        button.buttonStyle(style -> style
                .baseTexture(WheatMarketUiTextures.buttonBaseTexture())
                .hoverTexture(WheatMarketUiTextures.buttonPressedTexture())
                .pressedTexture(WheatMarketUiTextures.buttonPressedTexture())
        );
    }

    private void styleIconButton(Button button, IGuiTexture iconTexture) {
        button.buttonStyle(style -> style
                .baseTexture(new ColorRectTexture(0x00000000))
                .hoverTexture(new ColorRectTexture(0x00000000))
                .pressedTexture(new ColorRectTexture(0x00000000))
        );
        button.noText();
        button.addPreIcon(iconTexture);
    }

    private void installMyItemsOverlay() {
        myItemsOverlay = new UIElement()
                .layout(layout -> layout
                        .positionType(YogaPositionType.ABSOLUTE)
                        .left(0)
                        .top(0)
                        .widthPercent(100)
                        .heightPercent(100))
                .style(style -> style
                        .background(IGuiTexture.dynamic(() -> ownListingsOnly
                                ? WheatMarketUiTextures.myItemsSelectedOverlayTexture()
                                : EMPTY_TEXTURE))
                        .zIndex(5));
        myItemsOverlay.setAllowHitTest(false);
        myItemsButton.addChild(myItemsOverlay);
    }

    private void styleButtonWithFixedHoverIcon(Button button, String iconTexturePath, String hoveredIconTexturePath) {
        stylePlainButton(button);
        button.addChildAt(new UIElement()
                .layout(layout -> layout
                        .widthPercent(70)
                        .maxWidthPercent(70)
                        .maxHeightPercent(70)
                        .aspectRatio(1)
                        .flexShrink(1))
                .style(style -> style.background(IGuiTexture.dynamic(() -> switch (button.getState()) {
                    case HOVERED, PRESSED -> WheatMarketUiTextures.iconTexture(hoveredIconTexturePath);
                    default -> WheatMarketUiTextures.iconTexture(iconTexturePath);
                }))), 0);
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

    private List<String> resolveLocalizedSearchItemIds(String searchQuery) {
        String normalizedQuery = normalizeSearchQuery(searchQuery);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> localizedItemNameMatches(item, normalizedQuery))
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .limit(MAX_LOCALIZED_SEARCH_IDS)
                .toList();
    }

    private boolean localizedItemNameMatches(Item item, String normalizedQuery) {
        String localizedName = item.getDefaultInstance().getHoverName().getString();
        return normalizeSearchQuery(localizedName).contains(normalizedQuery);
    }

    private String normalizeSearchQuery(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private SortFilter selectedSortFilter() {
        return sortSelector.getValue() == null ? SortFilter.LIST_TIME : sortSelector.getValue();
    }

    private HoverTooltips myItemsTooltips() {
        Component current = Component.translatable(ownListingsOnly
                ? "gui.wheatmarket.my_items.current_own"
                : "gui.wheatmarket.my_items.current_all");
        Component next = Component.translatable(ownListingsOnly
                ? "gui.wheatmarket.my_items.switch_to_all"
                : "gui.wheatmarket.my_items.switch_to_own");
        return HoverTooltips.empty().append(current, next);
    }

    private String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
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
        LIST_TIME("gui.wheatmarket.filter.list_time", 0, false),
        ITEM_ID("gui.wheatmarket.filter.item_id", 1, true),
        LAST_TRADE("gui.wheatmarket.filter.last_trade", 2, false);

        private final String translationKey;
        private final int packetValue;
        private final boolean defaultAscending;

        SortFilter(String translationKey, int packetValue, boolean defaultAscending) {
            this.translationKey = translationKey;
            this.packetValue = packetValue;
            this.defaultAscending = defaultAscending;
        }

        @Override
        public String toString() {
            return Component.translatable(translationKey).getString();
        }
    }
}

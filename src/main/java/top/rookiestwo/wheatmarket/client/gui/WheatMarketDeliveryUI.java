package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
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
import org.w3c.dom.Document;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.c2s.ClaimDeliveryC2SPacket;
import top.rookiestwo.wheatmarket.network.c2s.RequestDeliveryListC2SPacket;
import top.rookiestwo.wheatmarket.network.s2c.DeliveryListS2CPacket;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class WheatMarketDeliveryUI {
    private static final ResourceLocation DELIVERY_XML = ResourceLocation.parse("wheatmarket:ui/delivery.xml");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final int TEXT_COLOR = 0x231A11;
    private static final int MUTED_TEXT_COLOR = 0x695748;
    private static final int SUCCESS_TEXT_COLOR = 0x2F6E3A;
    private static final int FAILURE_TEXT_COLOR = 0xA33629;
    private static final int INFO_TEXT_COLOR = 0x5C4024;
    private static final int ITEM_FRAME_BORDER = 0xFF7A5532;
    private static final int DELIVERY_ROW_HEIGHT = 48;
    private static final int DELIVERY_ROW_GAP = 6;
    private static final int MAX_DELIVERIES_PER_PAGE = 10;

    private final Runnable onBack;

    private UIElement rootElement;
    private UIElement titleBar;
    private UIElement listPanel;
    private UIElement paginationBar;
    private UIElement footerBar;
    private UIElement deliveryBoard;
    private Label titleLabel;
    private Label pageLabel;
    private Label statusLabel;
    private Button refreshButton;
    private Button backButton;
    private Button previousButton;
    private Button nextButton;
    private int requestedPage;
    private int seenListVersion = -1;
    private UUID pendingClaimId;
    private boolean loading = true;
    private Component lastFeedback = Component.empty();
    private int lastFeedbackColor = INFO_TEXT_COLOR;

    public WheatMarketDeliveryUI(Runnable onBack) {
        this.onBack = onBack == null ? () -> {
        } : onBack;
    }

    public ModularUI create(Player player) {
        Document xml = XmlUtils.loadXml(DELIVERY_XML);
        if (xml == null) {
            throw new IllegalStateException("Failed to load UI xml: " + DELIVERY_XML);
        }

        UI loadedUi = UI.of(xml);
        UI ui = UI.of(loadedUi.getRootElement(), loadedUi.getStylesheets(), availableSize -> availableSize);
        bindStaticElements(ui);
        applyTextures();
        applyLogic(player);
        return ModularUI.of(ui, player);
    }

    public void requestCurrentPage() {
        loading = true;
        showStatus(Component.translatable("gui.wheatmarket.delivery.loading"), INFO_TEXT_COLOR);
        updateControls();
        WheatMarketNetwork.sendToServer(new RequestDeliveryListC2SPacket(requestedPage, calculatePageSize()));
    }

    public void tick() {
        if (seenListVersion != WheatMarket.CLIENT_DELIVERY_LIST_VERSION) {
            seenListVersion = WheatMarket.CLIENT_DELIVERY_LIST_VERSION;
            loading = false;
            pendingClaimId = null;
            rebuildDeliveryList();
        }
    }

    public boolean handleOperationResult(boolean success, Component message) {
        if (pendingClaimId == null) {
            return false;
        }
        lastFeedback = message;
        lastFeedbackColor = success ? SUCCESS_TEXT_COLOR : FAILURE_TEXT_COLOR;
        pendingClaimId = null;
        if (success) {
            requestCurrentPage();
        } else {
            loading = false;
            rebuildDeliveryList();
        }
        return true;
    }

    private void bindStaticElements(UI ui) {
        rootElement = WheatMarketUiHelpers.require(ui, "delivery-root", UIElement.class);
        titleBar = WheatMarketUiHelpers.require(ui, "title-bar", UIElement.class);
        listPanel = WheatMarketUiHelpers.require(ui, "list-panel", UIElement.class);
        paginationBar = WheatMarketUiHelpers.require(ui, "pagination-bar", UIElement.class);
        footerBar = WheatMarketUiHelpers.require(ui, "footer-bar", UIElement.class);
        deliveryBoard = WheatMarketUiHelpers.require(ui, "delivery-board", UIElement.class);
        titleLabel = WheatMarketUiHelpers.require(ui, "title-label", Label.class);
        pageLabel = WheatMarketUiHelpers.require(ui, "page-label", Label.class);
        statusLabel = WheatMarketUiHelpers.require(ui, "status-label", Label.class);
        refreshButton = WheatMarketUiHelpers.require(ui, "refresh-button", Button.class);
        backButton = WheatMarketUiHelpers.require(ui, "back-button", Button.class);
        previousButton = WheatMarketUiHelpers.require(ui, "previous-button", Button.class);
        nextButton = WheatMarketUiHelpers.require(ui, "next-button", Button.class);
    }

    private void applyTextures() {
        rootElement.style(style -> style.background(WheatMarketUiTextures.rootBackground()));
        titleBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        listPanel.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        paginationBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        footerBar.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        statusLabel.style(style -> style.background(WheatMarketUiTextures.paperTexture()));

        styleButton(refreshButton);
        styleButton(backButton);
        styleButton(previousButton);
        styleButton(nextButton);
    }

    private void applyLogic(Player player) {
        titleLabel.setText(Component.translatable("gui.wheatmarket.delivery.title"));
        pageLabel.setText(Component.translatable("gui.wheatmarket.delivery.page", 1, 1));
        showStatus(Component.translatable("gui.wheatmarket.delivery.loading"), INFO_TEXT_COLOR);

        WheatMarketUiHelpers.styleLabel(titleLabel, TEXT_COLOR, Horizontal.CENTER);
        WheatMarketUiHelpers.styleLabel(pageLabel, TEXT_COLOR, Horizontal.CENTER);
        WheatMarketUiHelpers.styleLabel(statusLabel, INFO_TEXT_COLOR, Horizontal.LEFT);

        refreshButton.setOnClick(event -> requestCurrentPage());
        backButton.setOnClick(event -> onBack.run());
        previousButton.setOnClick(event -> {
            if (requestedPage > 0) {
                requestedPage--;
                requestCurrentPage();
            }
        });
        nextButton.setOnClick(event -> {
            if (requestedPage + 1 < Math.max(1, WheatMarket.CLIENT_DELIVERY_TOTAL_PAGES)) {
                requestedPage++;
                requestCurrentPage();
            }
        });

        rebuildDeliveryList(player);
    }

    private void rebuildDeliveryList() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        rebuildDeliveryList(player);
    }

    private void rebuildDeliveryList(Player player) {
        deliveryBoard.clearAllChildren();
        List<DeliveryListS2CPacket.DeliverySummary> deliveries = WheatMarket.CLIENT_DELIVERY_LIST;
        if (deliveries == null) {
            showStatus(Component.translatable("gui.wheatmarket.delivery.loading"), INFO_TEXT_COLOR);
            updateControls();
            return;
        }

        requestedPage = WheatMarket.CLIENT_DELIVERY_CURRENT_PAGE;
        pageLabel.setText(Component.translatable(
                "gui.wheatmarket.delivery.page",
                WheatMarket.CLIENT_DELIVERY_CURRENT_PAGE + 1,
                Math.max(1, WheatMarket.CLIENT_DELIVERY_TOTAL_PAGES)
        ));

        if (deliveries.isEmpty()) {
            showStatus(Component.translatable("gui.wheatmarket.delivery.empty"), MUTED_TEXT_COLOR);
            updateControls();
            return;
        }

        for (DeliveryListS2CPacket.DeliverySummary delivery : deliveries) {
            deliveryBoard.addChild(createDeliveryRow(player, delivery));
        }

        if (!lastFeedback.equals(Component.empty())) {
            showStatus(lastFeedback, lastFeedbackColor);
        } else {
            showStatus(Component.translatable("gui.wheatmarket.delivery.result_count", deliveries.size()), INFO_TEXT_COLOR);
        }
        updateControls();
    }

    private UIElement createDeliveryRow(Player player, DeliveryListS2CPacket.DeliverySummary delivery) {
        ItemStack stack = resolveItemStack(delivery);
        Component itemName = stack.isEmpty()
                ? Component.literal(delivery.getItemId())
                : stack.getHoverName();
        Component amountText = Component.translatable("gui.wheatmarket.delivery.amount", delivery.getRemainingAmount());
        Component sourceText = Component.translatable("gui.wheatmarket.delivery.source", resolveSourceText(delivery));
        Component timeText = Component.translatable("gui.wheatmarket.delivery.created", formatTime(delivery.getCreatedTime()));

        UIElement itemFrame = new UIElement()
                .layout(layout -> layout.width(22).height(22).paddingLeft(3).paddingRight(3).paddingTop(3).paddingBottom(3))
                .style(style -> style.background(GuiTextureGroup.of(
                        WheatMarketUiTextures.paperTexture(),
                        new ColorBorderTexture(1, ITEM_FRAME_BORDER)
                )))
                .addChild(new UIElement()
                        .layout(layout -> layout.width(16).height(16).flexShrink(0))
                        .style(style -> style.background(new ItemStackTexture(stack))));
        itemFrame.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = WheatMarketItemTooltips.forStack(stack));

        Label itemNameLabel = new Label();
        itemNameLabel.setText(itemName);
        itemNameLabel.layout(layout -> layout.widthPercent(100).height(16));
        WheatMarketUiHelpers.styleLabel(itemNameLabel, TEXT_COLOR, Horizontal.LEFT);
        itemNameLabel.addEventListener(UIEvents.HOVER_TOOLTIPS,
                event -> event.hoverTooltips = HoverTooltips.empty().append(itemName));

        Label sourceLabel = new Label();
        sourceLabel.setText(sourceText);
        sourceLabel.layout(layout -> layout.width(0).flex(1).height(14).minWidth(0));
        WheatMarketUiHelpers.styleLabel(sourceLabel, MUTED_TEXT_COLOR, Horizontal.LEFT);
        sourceLabel.addEventListener(UIEvents.HOVER_TOOLTIPS,
                event -> event.hoverTooltips = buildSourceTooltips(player, delivery));

        Label timeLabel = new Label();
        timeLabel.setText(timeText);
        timeLabel.layout(layout -> layout.width(0).flex(1).height(14).minWidth(0));
        WheatMarketUiHelpers.styleLabel(timeLabel, MUTED_TEXT_COLOR, Horizontal.LEFT);
        timeLabel.addEventListener(UIEvents.HOVER_TOOLTIPS,
                event -> event.hoverTooltips = HoverTooltips.empty().append(timeText));

        UIElement metaRow = new UIElement()
                .layout(layout -> layout.widthPercent(100).height(14).gapAll(6).minHeight(14))
                .lss("flex-direction", "row")
                .addChildren(sourceLabel, timeLabel);

        UIElement infoColumn = new UIElement()
                .layout(layout -> layout.width(0).flex(1).heightPercent(100).gapAll(2).minWidth(0))
                .lss("flex-direction", "column")
                .lss("justify-content", "center")
                .addChildren(itemNameLabel, metaRow);

        Label amountLabel = new Label();
        amountLabel.setText(amountText);
        amountLabel.layout(layout -> layout.width(72).height(18).flexShrink(0));
        WheatMarketUiHelpers.styleLabel(amountLabel, TEXT_COLOR, Horizontal.CENTER);

        Button claimButton = new Button();
        claimButton.enableText();
        claimButton.layout(layout -> layout.width(68).height(22).flexShrink(0));
        styleButton(claimButton);
        boolean claiming = delivery.getDeliveryId().equals(pendingClaimId);
        claimButton.setText(Component.translatable(claiming
                ? "gui.wheatmarket.delivery.claiming"
                : "gui.wheatmarket.delivery.claim"));
        claimButton.setActive(!claiming && pendingClaimId == null && !loading);
        claimButton.setOnClick(event -> claimDelivery(delivery));

        return new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(DELIVERY_ROW_HEIGHT)
                        .paddingLeft(8)
                        .paddingRight(8)
                        .paddingTop(6)
                        .paddingBottom(6)
                        .gapAll(8))
                .lss("flex-direction", "row")
                .lss("align-items", "center")
                .style(style -> style.background(WheatMarketUiTextures.paperTexture()))
                .addChildren(itemFrame, infoColumn, amountLabel, claimButton);
    }

    private void claimDelivery(DeliveryListS2CPacket.DeliverySummary delivery) {
        if (pendingClaimId != null) {
            return;
        }
        pendingClaimId = delivery.getDeliveryId();
        lastFeedback = Component.translatable("gui.wheatmarket.delivery.claiming");
        lastFeedbackColor = INFO_TEXT_COLOR;
        loading = false;
        showStatus(lastFeedback, lastFeedbackColor);
        updateControls();
        rebuildDeliveryList();
        WheatMarketNetwork.sendToServer(new ClaimDeliveryC2SPacket(delivery.getDeliveryId()));
    }

    private HoverTooltips buildSourceTooltips(Player player, DeliveryListS2CPacket.DeliverySummary delivery) {
        HoverTooltips tooltips = HoverTooltips.empty().append(
                Component.translatable("gui.wheatmarket.delivery.source", resolveSourceText(delivery))
        );
        if (delivery.getSourcePlayerId() != null) {
            tooltips.append(Component.translatable(
                    "gui.wheatmarket.delivery.source_player",
                    resolveSourcePlayerName(player, delivery.getSourcePlayerId())
            ));
        }
        return tooltips;
    }

    private Component resolveSourceText(DeliveryListS2CPacket.DeliverySummary delivery) {
        return switch (delivery.getSourceType()) {
            case DeliveryListS2CPacket.DeliverySummary.SOURCE_BUY_ORDER ->
                    Component.translatable("gui.wheatmarket.delivery.source.buy_order");
            case DeliveryListS2CPacket.DeliverySummary.SOURCE_EXPIRED ->
                    Component.translatable("gui.wheatmarket.delivery.source.expired");
            case DeliveryListS2CPacket.DeliverySummary.SOURCE_DELIST ->
                    Component.translatable("gui.wheatmarket.delivery.source.delist");
            default -> Component.translatable("gui.wheatmarket.delivery.source.unknown");
        };
    }

    private Component resolveSourcePlayerName(Player player, UUID sourcePlayerId) {
        if (sourcePlayerId.equals(player.getUUID())) {
            return player.getName();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Player sourcePlayer = minecraft.level.getPlayerByUUID(sourcePlayerId);
            if (sourcePlayer != null) {
                return sourcePlayer.getName();
            }
        }
        return Component.literal(sourcePlayerId.toString().substring(0, 8));
    }

    private ItemStack resolveItemStack(DeliveryListS2CPacket.DeliverySummary delivery) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && delivery.getItemNbt() != null) {
            ItemStack stack = ItemStack.parseOptional(minecraft.level.registryAccess(), delivery.getItemNbt());
            if (!stack.isEmpty()) {
                stack.setCount(1);
                return stack;
            }
        }
        ResourceLocation itemKey = ResourceLocation.tryParse(delivery.getItemId());
        if (itemKey != null) {
            Item item = BuiltInRegistries.ITEM.get(itemKey);
            if (item != null) {
                return item.getDefaultInstance();
            }
        }
        return ItemStack.EMPTY;
    }

    private Component formatTime(long time) {
        if (time <= 0) {
            return Component.translatable("gui.wheatmarket.delivery.unknown_time");
        }
        return Component.literal(Instant.ofEpochMilli(time)
                .atZone(ZoneId.systemDefault())
                .format(TIME_FORMAT));
    }

    private void updateControls() {
        int totalPages = Math.max(1, WheatMarket.CLIENT_DELIVERY_TOTAL_PAGES);
        boolean idle = pendingClaimId == null && !loading;
        refreshButton.setActive(idle);
        previousButton.setActive(idle && requestedPage > 0);
        nextButton.setActive(idle && requestedPage + 1 < totalPages);
        backButton.setActive(true);
    }

    private int calculatePageSize() {
        int boardHeight = Math.max(0, (int) deliveryBoard.getContentHeight());
        if (boardHeight <= 0) {
            boardHeight = Math.max(DELIVERY_ROW_HEIGHT, Minecraft.getInstance().getWindow().getGuiScaledHeight() - 144);
        }
        int rows = Math.max(1, (boardHeight + DELIVERY_ROW_GAP) / (DELIVERY_ROW_HEIGHT + DELIVERY_ROW_GAP));
        return Math.min(rows, MAX_DELIVERIES_PER_PAGE);
    }

    private void showStatus(Component message, int color) {
        statusLabel.setText(message);
        WheatMarketUiHelpers.styleLabel(statusLabel, color, Horizontal.LEFT);
    }

    private void styleButton(Button button) {
        button.enableText();
        button.buttonStyle(style -> style
                .baseTexture(WheatMarketUiTextures.buttonBaseTexture())
                .hoverTexture(WheatMarketUiTextures.buttonPressedTexture())
                .pressedTexture(WheatMarketUiTextures.buttonPressedTexture()));
    }

}

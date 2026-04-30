package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.w3c.dom.Document;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

public class WheatMarketItemEditUI {
    private static final ResourceLocation ITEM_EDIT_XML = ResourceLocation.parse("wheatmarket:ui/item_edit.xml");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final int TEXT_COLOR = 0x19140D;
    private static final int NOTICE_TEXT_COLOR = 0x665A4D;
    private static final int FAILURE_TEXT_COLOR = 0xA33629;
    private static final int BUTTON_BORDER = 0xFF3A332C;
    private static final int ITEM_PREVIEW_BORDER = 0xFF3A332C;
    private static final int BLUE_BUTTON = 0xFF78C6EA;
    private static final int BLUE_BUTTON_HOVER = 0xFF8CD3F2;
    private static final int BLUE_BUTTON_PRESSED = 0xFF5BAFD6;
    private static final int RED_BUTTON = 0xFFE86276;
    private static final int RED_BUTTON_HOVER = 0xFFF07688;
    private static final int RED_BUTTON_PRESSED = 0xFFD84B61;
    private static final IGuiTexture FIELD_TEXTURE = new ColorBorderTexture(-1, BUTTON_BORDER);
    private static final IGuiTexture EMPTY_OVERLAY = new ColorRectTexture(0x00000000);

    private final MarketListS2CPacket.MarketItemSummary item;
    private final ItemStack stack;
    private final Runnable onSelectStock;
    private final Consumer<ActionRequest> onSubmit;
    private final Runnable onCancel;

    private Label playerBalanceLabel;
    private Label stockLabel;
    private Label currentStockLabel;
    private Label feedbackLabel;
    private UIElement opPanel;
    private TextField priceField;
    private TextField cooldownAmountField;
    private TextField cooldownTimeField;
    private Button priceApplyButton;
    private Button stockEditButton;
    private Button delistButton;
    private Button toggleAdminButton;
    private Button toggleInfiniteButton;
    private Button cooldownApplyButton;
    private Button cancelButton;

    private double currentPrice;
    private int currentStock;
    private boolean currentAdmin;
    private boolean currentInfinite;
    private int currentCooldownAmount;
    private int currentCooldownTime;
    private double seenBalance = Double.NaN;
    private boolean operator;
    private boolean submitting;
    private boolean syncingPriceField;
    private boolean syncingCooldownAmountField;
    private boolean syncingCooldownTimeField;
    private boolean awaitingDelistConfirmation;
    private Action pendingAction = Action.NONE;
    private double submittedPrice;
    private int submittedCooldownAmount;
    private int submittedCooldownTime;

    public WheatMarketItemEditUI(MarketListS2CPacket.MarketItemSummary item, ItemStack stack, Draft draft,
                                 Runnable onSelectStock, Consumer<ActionRequest> onSubmit, Runnable onCancel) {
        this.item = item;
        this.stack = templateCopy(stack);
        this.onSelectStock = onSelectStock == null ? () -> {
        } : onSelectStock;
        this.onSubmit = onSubmit == null ? request -> {
        } : onSubmit;
        this.onCancel = onCancel == null ? () -> {
        } : onCancel;

        Draft initialDraft = draft == null ? Draft.from(item) : draft;
        this.currentPrice = initialDraft.price();
        this.currentStock = Math.max(0, initialDraft.currentStock());
        this.currentAdmin = initialDraft.ifAdmin();
        this.currentInfinite = initialDraft.ifInfinite();
        this.currentCooldownAmount = Math.max(0, initialDraft.cooldownAmount());
        this.currentCooldownTime = Math.max(0, initialDraft.cooldownTimeInMinutes());
    }

    private static ItemStack templateCopy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    public ModularUI create(Player player) {
        Document xml = XmlUtils.loadXml(ITEM_EDIT_XML);
        if (xml == null) {
            throw new IllegalStateException("Failed to load UI xml: " + ITEM_EDIT_XML);
        }

        UI loadedUi = UI.of(xml);
        UI ui = UI.of(loadedUi.getRootElement(), loadedUi.getStylesheets(), availableSize -> availableSize);
        bindAndPopulate(ui, player);
        return ModularUI.of(ui, player);
    }

    public void tick() {
        updateBalanceLabel();
        normalizePriceFieldIfBlurred();
        normalizeCooldownFieldIfBlurred(cooldownAmountField, currentCooldownAmount, true);
        normalizeCooldownFieldIfBlurred(cooldownTimeField, currentCooldownTime, false);
    }

    public Draft createDraft() {
        return new Draft(
                parsePrice(rawText(priceField), currentPrice),
                rawText(priceField),
                currentStock,
                currentAdmin,
                currentInfinite,
                parseNonNegativeInt(rawText(cooldownAmountField), currentCooldownAmount),
                parseNonNegativeInt(rawText(cooldownTimeField), currentCooldownTime),
                rawText(cooldownAmountField),
                rawText(cooldownTimeField)
        );
    }

    public Action pendingAction() {
        return pendingAction;
    }

    public int currentStock() {
        return currentStock;
    }

    public boolean handleOperationResult(boolean success, Component message) {
        if (pendingAction == Action.NONE) {
            return false;
        }

        Action completedAction = pendingAction;
        pendingAction = Action.NONE;
        submitting = false;
        awaitingDelistConfirmation = false;
        if (success) {
            applySuccessfulAction(completedAction);
        }
        showFeedback(message, success ? NOTICE_TEXT_COLOR : FAILURE_TEXT_COLOR);
        updateDynamicText();
        updateControlState();
        return true;
    }

    private void bindAndPopulate(UI ui, Player player) {
        UIElement root = require(ui, "item-edit-root", UIElement.class);
        UIElement editPaper = require(ui, "item-edit-paper", UIElement.class);
        UIElement playerAvatar = require(ui, "player-avatar", UIElement.class);
        UIElement itemPreview = require(ui, "item-preview", UIElement.class);
        UIElement itemIcon = require(ui, "edit-item-icon", UIElement.class);

        Label titleLabel = require(ui, "item-edit-title", Label.class);
        Label ownerLabel = require(ui, "owner-label", Label.class);
        Label itemNameLabel = require(ui, "item-name-label", Label.class);
        Label typeLabel = require(ui, "type-label", Label.class);
        stockLabel = require(ui, "stock-label", Label.class);
        Label listingTimeLabel = require(ui, "listing-time-label", Label.class);
        Label lastTradeLabel = require(ui, "last-trade-label", Label.class);
        Label priceCaption = require(ui, "price-caption", Label.class);
        Label stockCaption = require(ui, "stock-caption", Label.class);
        currentStockLabel = require(ui, "current-stock-label", Label.class);
        Label opTitle = require(ui, "op-title", Label.class);
        Label cooldownAmountCaption = require(ui, "cooldown-amount-caption", Label.class);
        Label cooldownTimeCaption = require(ui, "cooldown-time-caption", Label.class);
        playerBalanceLabel = require(ui, "player-balance", Label.class);
        feedbackLabel = require(ui, "feedback-label", Label.class);

        opPanel = require(ui, "op-panel", UIElement.class);
        priceField = require(ui, "price-field", TextField.class);
        cooldownAmountField = require(ui, "cooldown-amount-field", TextField.class);
        cooldownTimeField = require(ui, "cooldown-time-field", TextField.class);
        priceApplyButton = require(ui, "price-apply-button", Button.class);
        stockEditButton = require(ui, "stock-edit-button", Button.class);
        delistButton = require(ui, "delist-button", Button.class);
        toggleAdminButton = require(ui, "toggle-admin-button", Button.class);
        toggleInfiniteButton = require(ui, "toggle-infinite-button", Button.class);
        cooldownApplyButton = require(ui, "cooldown-apply-button", Button.class);
        cancelButton = require(ui, "cancel-button", Button.class);

        root.style(style -> style.background(WheatMarketUiTextures.tradingBackgroundTexture()));
        root.addEventListener(UIEvents.MOUSE_DOWN, event -> onRootMouseDown(event.x, event.y), true);
        editPaper.style(style -> style.background(WheatMarketUiTextures.tradingPaperTexture()));
        playerAvatar.style(style -> style.background(WheatMarketUiTextures.playerAvatarTexture(player)));
        playerBalanceLabel.style(style -> style.background(WheatMarketUiTextures.paperTexture()));
        itemPreview.style(style -> style.background(new ColorBorderTexture(1, ITEM_PREVIEW_BORDER)));
        itemIcon.style(style -> style.background(new ItemStackTexture(stack)));
        itemPreview.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = WheatMarketItemTooltips.forStack(stack));

        styleLabel(titleLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(ownerLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(itemNameLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(typeLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(stockLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(listingTimeLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(lastTradeLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(priceCaption, TEXT_COLOR, Horizontal.RIGHT);
        styleLabel(stockCaption, TEXT_COLOR, Horizontal.RIGHT);
        styleLabel(currentStockLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(opTitle, NOTICE_TEXT_COLOR, Horizontal.CENTER);
        styleLabel(cooldownAmountCaption, TEXT_COLOR, Horizontal.RIGHT);
        styleLabel(cooldownTimeCaption, TEXT_COLOR, Horizontal.RIGHT);
        styleLabel(playerBalanceLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(feedbackLabel, NOTICE_TEXT_COLOR, Horizontal.CENTER);

        styleActionButton(priceApplyButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(stockEditButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(toggleAdminButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(toggleInfiniteButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(cooldownApplyButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(delistButton, RED_BUTTON, RED_BUTTON_HOVER, RED_BUTTON_PRESSED);
        styleActionButton(cancelButton, RED_BUTTON, RED_BUTTON_HOVER, RED_BUTTON_PRESSED);
        enableButtonText(priceApplyButton, stockEditButton, toggleAdminButton,
                toggleInfiniteButton, cooldownApplyButton, delistButton, cancelButton);

        configurePriceField();
        configureCooldownField(cooldownAmountField, true);
        configureCooldownField(cooldownTimeField, false);

        operator = player.hasPermissions(2);
        titleLabel.setText(Component.translatable("gui.wheatmarket.edit.title"));
        ownerLabel.setText(Component.translatable(item.isIfSell()
                ? "gui.wheatmarket.confirm.seller"
                : "gui.wheatmarket.confirm.buyer", resolveOwnerName(player)));
        itemNameLabel.setText(Component.translatable("gui.wheatmarket.confirm.item_name", stack.getHoverName()));
        typeLabel.setText(Component.translatable(item.isIfSell()
                ? "gui.wheatmarket.market.sell"
                : "gui.wheatmarket.market.buy_order"));
        listingTimeLabel.setText(Component.translatable("gui.wheatmarket.confirm.listing_time", formatTime(item.getListingTime())));
        lastTradeLabel.setText(Component.translatable("gui.wheatmarket.edit.last_trade", formatTime(item.getLastTradeTime())));
        priceCaption.setText(Component.translatable("gui.wheatmarket.listing.price_label"));
        stockCaption.setText(Component.translatable("gui.wheatmarket.edit.current_stock"));
        opTitle.setText(Component.translatable("gui.wheatmarket.edit.op_options"));
        cooldownAmountCaption.setText(Component.translatable("gui.wheatmarket.edit.cooldown_amount"));
        cooldownTimeCaption.setText(Component.translatable("gui.wheatmarket.edit.cooldown_time"));
        priceApplyButton.setText(Component.translatable("gui.wheatmarket.edit.apply_price"));
        stockEditButton.setText(Component.translatable("gui.wheatmarket.edit.edit_stock"));
        cooldownApplyButton.setText(Component.translatable("gui.wheatmarket.edit.apply_cooldown"));
        cancelButton.setText(Component.translatable("gui.wheatmarket.edit.back"));

        syncPriceFieldText(formatMoney(currentPrice));
        syncCooldownAmountText(String.valueOf(currentCooldownAmount));
        syncCooldownTimeText(String.valueOf(currentCooldownTime));
        feedbackLabel.setText(Component.empty());

        priceApplyButton.setOnClick(event -> submitPriceChange());
        stockEditButton.setOnClick(event -> onSelectStock.run());
        delistButton.setOnClick(event -> submitDelist());
        toggleAdminButton.setOnClick(event -> submitSimpleAction(Action.TOGGLE_ADMIN));
        toggleInfiniteButton.setOnClick(event -> submitSimpleAction(Action.TOGGLE_INFINITE));
        cooldownApplyButton.setOnClick(event -> submitCooldownChange());
        cancelButton.setOnClick(event -> onCancel.run());

        updateBalanceLabel();
        updateDynamicText();
        updateControlState();
    }

    private void configurePriceField() {
        priceField.setAnyString();
        priceField.setCharValidator(character -> Character.isDigit(character) || character == '.');
        priceField.setTextValidator(this::isPriceInputValid);
        priceField.style(style -> style.background(FIELD_TEXTURE));
        priceField.textFieldStyle(style -> style
                .placeholder(Component.translatable("gui.wheatmarket.listing.price_placeholder"))
                .textColor(TEXT_COLOR)
                .cursorColor(0xFF19140D)
                .errorColor(FAILURE_TEXT_COLOR)
                .textShadow(false)
                .focusOverlay(EMPTY_OVERLAY));
        priceField.registerValueListener(rawValue -> {
            if (!syncingPriceField) {
                awaitingDelistConfirmation = false;
                hideFeedback();
                updateControlState();
            }
        });
        priceField.addEventListener(UIEvents.BLUR, event -> normalizePriceFieldIfBlurred());
    }

    private void configureCooldownField(TextField field, boolean amountField) {
        field.setAnyString();
        field.setCharValidator(Character::isDigit);
        field.setTextValidator(text -> text.isBlank() || text.chars().allMatch(Character::isDigit));
        field.style(style -> style.background(FIELD_TEXTURE));
        field.textFieldStyle(style -> style
                .placeholder(Component.translatable("gui.wheatmarket.confirm.empty"))
                .textColor(TEXT_COLOR)
                .cursorColor(0xFF19140D)
                .errorColor(FAILURE_TEXT_COLOR)
                .textShadow(false)
                .focusOverlay(EMPTY_OVERLAY));
        field.registerValueListener(rawValue -> {
            if (amountField ? !syncingCooldownAmountField : !syncingCooldownTimeField) {
                awaitingDelistConfirmation = false;
                hideFeedback();
                updateControlState();
            }
        });
        field.addEventListener(UIEvents.BLUR, event -> normalizeCooldownFieldIfBlurred(field,
                amountField ? currentCooldownAmount : currentCooldownTime,
                amountField));
    }

    private void submitPriceChange() {
        if (submitting) {
            return;
        }
        double price = parsePrice(rawText(priceField), 0.0D);
        if (price <= 0.0D || !Double.isFinite(price)) {
            showFeedback(Component.translatable("gui.wheatmarket.operation.invalid_price"), FAILURE_TEXT_COLOR);
            return;
        }
        submittedPrice = price;
        beginSubmit(Action.CHANGE_PRICE, Component.translatable("gui.wheatmarket.edit.updating"));
        onSubmit.accept(new ActionRequest(Action.CHANGE_PRICE, 0, price, 0, 0));
    }

    private void submitDelist() {
        if (submitting) {
            return;
        }
        if (!item.isIfSell()) {
            showFeedback(Component.translatable("gui.wheatmarket.edit.buy_order_delist_unsupported"), FAILURE_TEXT_COLOR);
            return;
        }
        if (!awaitingDelistConfirmation) {
            awaitingDelistConfirmation = true;
            delistButton.setText(Component.translatable("gui.wheatmarket.edit.confirm_delist"));
            showFeedback(Component.translatable("gui.wheatmarket.edit.delist_warning"), FAILURE_TEXT_COLOR);
            return;
        }
        beginSubmit(Action.DELIST, Component.translatable("gui.wheatmarket.edit.updating"));
        onSubmit.accept(new ActionRequest(Action.DELIST, 0, 0.0D, 0, 0));
    }

    private void submitSimpleAction(Action action) {
        if (submitting || !operator) {
            return;
        }
        beginSubmit(action, Component.translatable("gui.wheatmarket.edit.updating"));
        onSubmit.accept(new ActionRequest(action, 0, 0.0D, 0, 0));
    }

    private void submitCooldownChange() {
        if (submitting || !operator) {
            return;
        }
        int amount = parseNonNegativeInt(rawText(cooldownAmountField), -1);
        int time = parseNonNegativeInt(rawText(cooldownTimeField), -1);
        if (amount < 0 || time < 0) {
            showFeedback(Component.translatable("gui.wheatmarket.operation.invalid_amount"), FAILURE_TEXT_COLOR);
            return;
        }
        submittedCooldownAmount = amount;
        submittedCooldownTime = time;
        beginSubmit(Action.SET_COOLDOWN, Component.translatable("gui.wheatmarket.edit.updating"));
        onSubmit.accept(new ActionRequest(Action.SET_COOLDOWN, 0, 0.0D, amount, time));
    }

    private void beginSubmit(Action action, Component message) {
        pendingAction = action;
        submitting = true;
        awaitingDelistConfirmation = false;
        showFeedback(message, NOTICE_TEXT_COLOR);
        updateControlState();
    }

    private void applySuccessfulAction(Action action) {
        switch (action) {
            case CHANGE_PRICE -> currentPrice = submittedPrice;
            case TOGGLE_ADMIN -> currentAdmin = !currentAdmin;
            case TOGGLE_INFINITE -> {
                currentInfinite = !currentInfinite;
                if (!currentInfinite && currentStock <= 0) {
                    currentStock = 1;
                }
            }
            case SET_COOLDOWN -> {
                currentCooldownAmount = submittedCooldownAmount;
                currentCooldownTime = submittedCooldownTime;
            }
            default -> {
            }
        }
    }

    private void updateDynamicText() {
        stockLabel.setText(Component.translatable("gui.wheatmarket.confirm.stock", formatStock(currentStock, currentInfinite)));
        currentStockLabel.setText(formatStock(currentStock, currentInfinite));
        toggleAdminButton.setText(Component.translatable(currentAdmin
                ? "gui.wheatmarket.edit.system_shop_on"
                : "gui.wheatmarket.edit.system_shop_off"));
        toggleInfiniteButton.setText(Component.translatable(currentInfinite
                ? "gui.wheatmarket.edit.infinite_on"
                : "gui.wheatmarket.edit.infinite_off"));
        if (!awaitingDelistConfirmation) {
            delistButton.setText(Component.translatable("gui.wheatmarket.edit.delist"));
        }
    }

    private void updateControlState() {
        boolean idle = !submitting;
        boolean sellOrder = item.isIfSell();
        boolean stockEditable = sellOrder && !currentInfinite;
        double price = parsePrice(rawText(priceField), 0.0D);
        int cooldownAmount = parseNonNegativeInt(rawText(cooldownAmountField), -1);
        int cooldownTime = parseNonNegativeInt(rawText(cooldownTimeField), -1);

        priceApplyButton.setActive(idle && price > 0.0D && Double.isFinite(price));
        stockEditButton.setActive(idle && stockEditable);
        delistButton.setActive(idle && sellOrder);
        toggleAdminButton.setActive(idle && operator);
        toggleInfiniteButton.setActive(idle && operator);
        cooldownApplyButton.setActive(idle && operator && cooldownAmount >= 0 && cooldownTime >= 0);
        cancelButton.setActive(idle);
        priceField.setActive(idle);
        cooldownAmountField.setActive(idle && operator);
        cooldownTimeField.setActive(idle && operator);
        setShown(opPanel, operator);
    }

    private void updateBalanceLabel() {
        if (playerBalanceLabel == null || Double.compare(seenBalance, WheatMarket.CLIENT_BALANCE) == 0) {
            return;
        }
        seenBalance = WheatMarket.CLIENT_BALANCE;
        playerBalanceLabel.setText(Component.translatable("gui.wheatmarket.balance", formatMoney(seenBalance)));
    }

    private void normalizePriceFieldIfBlurred() {
        if (priceField == null || priceField.isFocused()) {
            return;
        }
        double price = parsePrice(rawText(priceField), currentPrice);
        if (price <= 0.0D || !Double.isFinite(price)) {
            price = currentPrice;
        }
        syncPriceFieldText(formatMoney(price));
    }

    private void normalizeCooldownFieldIfBlurred(TextField field, int fallback, boolean amountField) {
        if (field == null || field.isFocused()) {
            return;
        }
        int value = parseNonNegativeInt(rawText(field), fallback);
        if (amountField) {
            syncCooldownAmountText(String.valueOf(Math.max(0, value)));
        } else {
            syncCooldownTimeText(String.valueOf(Math.max(0, value)));
        }
    }

    private void onRootMouseDown(float mouseX, float mouseY) {
        blurIfOutside(priceField, mouseX, mouseY);
        blurIfOutside(cooldownAmountField, mouseX, mouseY);
        blurIfOutside(cooldownTimeField, mouseX, mouseY);
    }

    private void blurIfOutside(TextField field, float mouseX, float mouseY) {
        if (field != null && !field.isMouseOver(mouseX, mouseY)) {
            field.blur();
        }
    }

    private void syncPriceFieldText(String value) {
        if (priceField != null && !value.equals(priceField.getRawText())) {
            syncingPriceField = true;
            priceField.setValue(value, false);
            syncingPriceField = false;
        }
    }

    private void syncCooldownAmountText(String value) {
        if (cooldownAmountField != null && !value.equals(cooldownAmountField.getRawText())) {
            syncingCooldownAmountField = true;
            cooldownAmountField.setValue(value, false);
            syncingCooldownAmountField = false;
        }
    }

    private void syncCooldownTimeText(String value) {
        if (cooldownTimeField != null && !value.equals(cooldownTimeField.getRawText())) {
            syncingCooldownTimeField = true;
            cooldownTimeField.setValue(value, false);
            syncingCooldownTimeField = false;
        }
    }

    private boolean isPriceInputValid(String text) {
        if (text == null || text.isBlank() || ".".equals(text)) {
            return true;
        }
        int dotIndex = text.indexOf('.');
        if (dotIndex != text.lastIndexOf('.')) {
            return false;
        }
        if (dotIndex >= 0 && text.length() - dotIndex - 1 > 2) {
            return false;
        }
        try {
            double value = Double.parseDouble(text);
            return Double.isFinite(value) && value <= 999_999.99D;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private double parsePrice(String rawValue, double fallback) {
        if (rawValue == null || rawValue.isBlank() || ".".equals(rawValue)) {
            return fallback;
        }
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int parseNonNegativeInt(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(rawValue);
            return Math.max(0, value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Component resolveOwnerName(Player player) {
        if (currentAdmin) {
            return Component.translatable("gui.wheatmarket.market.system_shop");
        }
        UUID ownerId = item.getSellerID();
        if (ownerId.equals(player.getUUID())) {
            return player.getName();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Player owner = minecraft.level.getPlayerByUUID(ownerId);
            if (owner != null) {
                return owner.getName();
            }
        }
        return Component.literal(ownerId.toString().substring(0, 8));
    }

    private Component formatStock(int amount, boolean infinite) {
        if (infinite) {
            return Component.translatable("gui.wheatmarket.market.infinite");
        }
        return Component.literal(String.valueOf(amount));
    }

    private Component formatTime(long time) {
        if (time <= 0) {
            return Component.translatable("gui.wheatmarket.confirm.unknown_time");
        }
        return Component.literal(Instant.ofEpochMilli(time)
                .atZone(ZoneId.systemDefault())
                .format(TIME_FORMAT));
    }

    private void showFeedback(Component message, int color) {
        styleLabel(feedbackLabel, color, Horizontal.CENTER);
        feedbackLabel.setText(message);
        setShown(feedbackLabel, true);
    }

    private void hideFeedback() {
        if (feedbackLabel != null) {
            feedbackLabel.setText(Component.empty());
        }
    }

    private void enableButtonText(Button... buttons) {
        for (Button button : buttons) {
            button.enableText();
        }
    }

    private void styleLabel(Label label, int color, Horizontal horizontal) {
        label.textStyle(style -> style
                .textAlignHorizontal(horizontal)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(color)
                .textShadow(false));
    }

    private void styleActionButton(Button button, int baseColor, int hoverColor, int pressedColor) {
        button.buttonStyle(style -> style
                .baseTexture(buttonTexture(baseColor))
                .hoverTexture(buttonTexture(hoverColor))
                .pressedTexture(buttonTexture(pressedColor)));
        button.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(TEXT_COLOR)
                .textShadow(false));
    }

    private IGuiTexture buttonTexture(int color) {
        return GuiTextureGroup.of(
                new ColorRectTexture(color),
                new ColorBorderTexture(1, BUTTON_BORDER)
        );
    }

    private void setShown(UIElement element, boolean shown) {
        element.setDisplay(shown);
        element.setVisible(shown);
    }

    private <T> T require(UI ui, String id, Class<T> type) {
        return ui.selectId(id, type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing UI element: " + id));
    }

    private String rawText(TextField field) {
        return field == null ? "" : field.getRawText();
    }

    private String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public enum Action {
        NONE,
        CHANGE_PRICE,
        DELIST,
        TOGGLE_ADMIN,
        TOGGLE_INFINITE,
        SET_COOLDOWN
    }

    public record Draft(double price, String priceText, int currentStock,
                        boolean ifAdmin, boolean ifInfinite, int cooldownAmount, int cooldownTimeInMinutes,
                        String cooldownAmountText, String cooldownTimeText) {
        public static Draft from(MarketListS2CPacket.MarketItemSummary item) {
            String priceText = String.format(Locale.ROOT, "%.2f", item.getPrice());
            String cooldownAmountText = String.valueOf(Math.max(0, item.getCooldownAmount()));
            String cooldownTimeText = String.valueOf(Math.max(0, item.getCooldownTimeInMinutes()));
            return new Draft(
                    item.getPrice(),
                    priceText,
                    Math.max(0, item.getAmount()),
                    item.isIfAdmin(),
                    item.isIfInfinite(),
                    Math.max(0, item.getCooldownAmount()),
                    Math.max(0, item.getCooldownTimeInMinutes()),
                    cooldownAmountText,
                    cooldownTimeText
            );
        }

        public Draft withStock(int stock) {
            int safeStock = Math.max(0, stock);
            return new Draft(price, priceText, safeStock, ifAdmin, ifInfinite,
                    cooldownAmount, cooldownTimeInMinutes, cooldownAmountText, cooldownTimeText);
        }
    }

    public record ActionRequest(Action action, int amount, double price, int cooldownAmount,
                                int cooldownTimeInMinutes) {
    }
}

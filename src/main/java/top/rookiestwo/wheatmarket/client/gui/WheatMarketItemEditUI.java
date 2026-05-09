package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

public class WheatMarketItemEditUI {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final int TEXT_COLOR = 0x19140D;
    private static final int NOTICE_TEXT_COLOR = 0x665A4D;
    private static final int FAILURE_TEXT_COLOR = 0xA33629;
    private static final int BUTTON_BORDER = 0xFF3A332C;
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
    private UIElement root;

    public WheatMarketItemEditUI(MarketListS2CPacket.MarketItemSummary item, ItemStack stack, Draft draft,
                                 Runnable onSelectStock, Consumer<ActionRequest> onSubmit, Runnable onCancel) {
        this.item = item;
        this.stack = WheatMarketUiHelpers.templateCopy(stack);
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

    public ModularUI create(Player player) {
        PaperFormFactory.SharedPaperForm shared = PaperFormFactory.create(player);
        populate(shared, player);
        return ModularUI.of(shared.ui(), player);
    }

    public void tick() {
        updateBalanceLabel();
        normalizePriceFieldIfBlurred();
        normalizeCooldownFieldIfBlurred(cooldownAmountField, currentCooldownAmount, true);
        normalizeCooldownFieldIfBlurred(cooldownTimeField, currentCooldownTime, false);
    }

    public Draft createDraft() {
        return new Draft(
                WheatMarketUiHelpers.parsePrice(WheatMarketUiHelpers.rawText(priceField), currentPrice),
                WheatMarketUiHelpers.rawText(priceField),
                currentStock,
                currentAdmin,
                currentInfinite,
                WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(cooldownAmountField), currentCooldownAmount),
                WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(cooldownTimeField), currentCooldownTime),
                WheatMarketUiHelpers.rawText(cooldownAmountField),
                WheatMarketUiHelpers.rawText(cooldownTimeField)
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

    private void populate(PaperFormFactory.SharedPaperForm shared, Player player) {
        root = shared.root();
        root.addEventListener(UIEvents.MOUSE_DOWN, event -> onRootMouseDown(event.x, event.y), true);

        playerBalanceLabel = shared.playerBalanceLabel();
        WheatMarketUiHelpers.styleLabel(playerBalanceLabel, TEXT_COLOR, Horizontal.CENTER);

        shared.itemIcon().style(style -> style.background(new ItemStackTexture(stack)));
        shared.itemPreview().addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = WheatMarketItemTooltips.forStack(stack));

        WheatMarketUiHelpers.styleLabel(shared.title(), TEXT_COLOR, Horizontal.CENTER);
        WheatMarketUiHelpers.styleLabel(shared.ownerLabel(), TEXT_COLOR, Horizontal.CENTER);

        opPanel = shared.opPanel();
        toggleAdminButton = shared.toggleAdminButton();
        toggleInfiniteButton = shared.toggleInfiniteButton();
        cooldownAmountField = shared.cooldownAmountField();
        cooldownTimeField = shared.cooldownTimeField();

        WheatMarketUiHelpers.styleLabel(shared.opTitle(), NOTICE_TEXT_COLOR, Horizontal.CENTER);
        WheatMarketUiHelpers.styleFormCaption(shared.cooldownAmountCaption());
        WheatMarketUiHelpers.styleFormCaption(shared.cooldownTimeCaption());

        operator = player.hasPermissions(2);
        shared.title().setText(Component.translatable("gui.wheatmarket.edit.title"));
        shared.ownerLabel().setText(Component.translatable(item.isIfSell()
                ? "gui.wheatmarket.confirm.seller"
                : "gui.wheatmarket.confirm.buyer", resolveOwnerName(player)));
        shared.opTitle().setText(Component.translatable("gui.wheatmarket.edit.op_options"));
        shared.cooldownAmountCaption().setText(Component.translatable("gui.wheatmarket.edit.cooldown_amount"));
        shared.cooldownTimeCaption().setText(Component.translatable("gui.wheatmarket.edit.cooldown_time"));

        configureCooldownField(cooldownAmountField, true);
        configureCooldownField(cooldownTimeField, false);

        enableButtonText(toggleAdminButton, toggleInfiniteButton);
        WheatMarketUiTextures.styleColoredActionButton(toggleAdminButton, WheatMarketUiTextures.BLUE_BUTTON_COLOR, WheatMarketUiTextures.BLUE_BUTTON_HOVER_COLOR, WheatMarketUiTextures.BLUE_BUTTON_PRESSED_COLOR, -1);
        WheatMarketUiTextures.styleColoredActionButton(toggleInfiniteButton, WheatMarketUiTextures.BLUE_BUTTON_COLOR, WheatMarketUiTextures.BLUE_BUTTON_HOVER_COLOR, WheatMarketUiTextures.BLUE_BUTTON_PRESSED_COLOR, -1);
        toggleAdminButton.setOnClick(event -> submitSimpleAction(Action.TOGGLE_ADMIN));
        toggleInfiniteButton.setOnClick(event -> submitSimpleAction(Action.TOGGLE_INFINITE));

        buildLeftExtra(shared, player);
        buildRightContent(shared);
        buildBottomBar(shared);
        buildActionColumn(shared);

        cooldownApplyButton = new Button();
        cooldownApplyButton.enableText();
        cooldownApplyButton.layout(layout -> layout.widthPercent(70).minWidth(74).maxWidth(120).height(22).flexShrink(0));
        WheatMarketUiTextures.styleColoredActionButton(cooldownApplyButton, WheatMarketUiTextures.BLUE_BUTTON_COLOR, WheatMarketUiTextures.BLUE_BUTTON_HOVER_COLOR, WheatMarketUiTextures.BLUE_BUTTON_PRESSED_COLOR, -1);
        cooldownApplyButton.setText(Component.translatable("gui.wheatmarket.edit.apply_cooldown"));
        cooldownApplyButton.setOnClick(event -> submitCooldownChange());
        shared.opPanel().addChild(cooldownApplyButton);

        syncPriceFieldText(WheatMarketUiHelpers.formatMoney(currentPrice));
        syncCooldownAmountText(String.valueOf(currentCooldownAmount));
        syncCooldownTimeText(String.valueOf(currentCooldownTime));
        feedbackLabel.setText(Component.empty());

        updateBalanceLabel();
        updateDynamicText();
        updateControlState();
    }

    private void buildLeftExtra(PaperFormFactory.SharedPaperForm shared, Player player) {
        Label itemNameLabel = createSummaryLabel();
        WheatMarketUiHelpers.styleLabel(itemNameLabel, TEXT_COLOR, Horizontal.CENTER);
        itemNameLabel.setText(Component.translatable("gui.wheatmarket.confirm.item_name", stack.getHoverName()));

        Label typeLabel = createSummaryLabel();
        WheatMarketUiHelpers.styleLabel(typeLabel, TEXT_COLOR, Horizontal.CENTER);
        typeLabel.setText(Component.translatable(item.isIfSell()
                ? "gui.wheatmarket.market.sell"
                : "gui.wheatmarket.market.buy_order"));

        stockLabel = createSummaryLabel();
        WheatMarketUiHelpers.styleLabel(stockLabel, TEXT_COLOR, Horizontal.CENTER);

        shared.leftExtra().addChildren(itemNameLabel, typeLabel, stockLabel);
    }

    private void buildRightContent(PaperFormFactory.SharedPaperForm shared) {
        Label listingTimeLabel = createLabel(Horizontal.CENTER);
        WheatMarketUiHelpers.styleLabel(listingTimeLabel, TEXT_COLOR, Horizontal.CENTER);
        listingTimeLabel.setText(Component.translatable("gui.wheatmarket.confirm.listing_time", formatTime(item.getListingTime())));

        Label lastTradeLabel = createLabel(Horizontal.CENTER);
        WheatMarketUiHelpers.styleLabel(lastTradeLabel, TEXT_COLOR, Horizontal.CENTER);
        lastTradeLabel.setText(Component.translatable("gui.wheatmarket.edit.last_trade", formatTime(item.getLastTradeTime())));

        Label priceCaption = createLabel(Horizontal.RIGHT);
        WheatMarketUiHelpers.styleFormCaption(priceCaption);
        priceCaption.setText(Component.translatable("gui.wheatmarket.listing.price_label"));

        priceField = new TextField();
        priceField.lss("text-color", String.format("#%06X", TEXT_COLOR));
        priceField.lss("cursor-color", "#19140D");
        priceField.lss("error-color", String.format("#%06X", FAILURE_TEXT_COLOR));
        priceField.lss("text-shadow", "false");
        priceField.lss("flex-shrink", "1");
        priceField.setValue("1.00", false);
        priceField.style(style -> style.background(FIELD_TEXTURE));
        priceField.layout(layout -> layout.width(0).flex(1).minWidth(48).maxWidth(86).height(22).paddingLeft(6).paddingRight(6).flexShrink(1));

        UIElement priceGroup = new UIElement()
                .layout(layout -> layout.maxWidthPercent(100).height(22).gapAll(4).flexShrink(1));
        priceGroup.lss("flex-direction", "row");
        priceGroup.lss("align-items", "center");
        priceGroup.lss("justify-content", "center");
        priceGroup.lss("overflow", "hidden");
        priceGroup.addChildren(priceCaption, priceField);

        UIElement priceRow = new UIElement()
                .layout(layout -> layout.widthPercent(100).maxWidthPercent(100).height(22).flexShrink(0));
        priceRow.lss("flex-direction", "row");
        priceRow.lss("align-items", "center");
        priceRow.lss("justify-content", "center");
        priceRow.lss("overflow", "hidden");
        priceRow.addChild(priceGroup);

        configurePriceField();

        priceApplyButton = new Button();
        priceApplyButton.enableText();
        priceApplyButton.layout(layout -> layout.width(86).minWidth(64).maxWidth(98).height(22).flexShrink(0));
        priceApplyButton.lss("align-self", "center");
        WheatMarketUiTextures.styleColoredActionButton(priceApplyButton, WheatMarketUiTextures.BLUE_BUTTON_COLOR, WheatMarketUiTextures.BLUE_BUTTON_HOVER_COLOR, WheatMarketUiTextures.BLUE_BUTTON_PRESSED_COLOR, -1);
        priceApplyButton.setText(Component.translatable("gui.wheatmarket.edit.apply_price"));
        priceApplyButton.setOnClick(event -> submitPriceChange());

        Label stockCaption = createLabel(Horizontal.RIGHT);
        WheatMarketUiHelpers.styleFormCaption(stockCaption);
        stockCaption.setText(Component.translatable("gui.wheatmarket.edit.current_stock"));

        currentStockLabel = createLabel(Horizontal.CENTER);
        WheatMarketUiHelpers.styleLabel(currentStockLabel, TEXT_COLOR, Horizontal.CENTER);
        currentStockLabel.layout(layout -> layout.width(0).flex(1).minWidth(48).maxWidth(86).height(22).flexShrink(1));

        UIElement stockGroup = new UIElement()
                .layout(layout -> layout.maxWidthPercent(100).height(22).gapAll(4).flexShrink(1));
        stockGroup.lss("flex-direction", "row");
        stockGroup.lss("align-items", "center");
        stockGroup.lss("justify-content", "center");
        stockGroup.lss("overflow", "hidden");
        stockGroup.addChildren(stockCaption, currentStockLabel);

        UIElement stockRow = new UIElement()
                .layout(layout -> layout.widthPercent(100).maxWidthPercent(100).height(22).flexShrink(0));
        stockRow.lss("flex-direction", "row");
        stockRow.lss("align-items", "center");
        stockRow.lss("justify-content", "center");
        stockRow.lss("overflow", "hidden");
        stockRow.addChild(stockGroup);

        stockEditButton = new Button();
        stockEditButton.enableText();
        stockEditButton.layout(layout -> layout.width(86).minWidth(64).maxWidth(98).height(22).flexShrink(0));
        stockEditButton.lss("align-self", "center");
        WheatMarketUiTextures.styleColoredActionButton(stockEditButton, WheatMarketUiTextures.BLUE_BUTTON_COLOR, WheatMarketUiTextures.BLUE_BUTTON_HOVER_COLOR, WheatMarketUiTextures.BLUE_BUTTON_PRESSED_COLOR, -1);
        stockEditButton.setText(Component.translatable("gui.wheatmarket.edit.edit_stock"));
        stockEditButton.setOnClick(event -> onSelectStock.run());

        shared.rightContent().addChildren(listingTimeLabel, lastTradeLabel, priceRow, priceApplyButton, stockRow, stockEditButton);
    }

    private void buildBottomBar(PaperFormFactory.SharedPaperForm shared) {
        feedbackLabel = createLabel(Horizontal.CENTER);
        WheatMarketUiHelpers.styleLabel(feedbackLabel, NOTICE_TEXT_COLOR, Horizontal.CENTER);
        feedbackLabel.layout(layout -> layout.widthPercent(100).height(18).flexShrink(0));
        shared.bottomBar().addChild(feedbackLabel);
    }

    private void buildActionColumn(PaperFormFactory.SharedPaperForm shared) {
        delistButton = new Button();
        delistButton.enableText();
        delistButton.layout(layout -> layout.widthPercent(100).minWidth(64).maxWidth(96).height(36).flexShrink(0));
        WheatMarketUiTextures.styleColoredActionButton(delistButton, WheatMarketUiTextures.RED_BUTTON_COLOR, WheatMarketUiTextures.RED_BUTTON_HOVER_COLOR, WheatMarketUiTextures.RED_BUTTON_PRESSED_COLOR, -1);
        delistButton.setText(Component.translatable("gui.wheatmarket.edit.delist"));
        delistButton.setOnClick(event -> submitDelist());

        cancelButton = new Button();
        cancelButton.enableText();
        cancelButton.layout(layout -> layout.widthPercent(100).minWidth(64).maxWidth(96).height(52).flexShrink(0));
        WheatMarketUiTextures.styleColoredActionButton(cancelButton, WheatMarketUiTextures.RED_BUTTON_COLOR, WheatMarketUiTextures.RED_BUTTON_HOVER_COLOR, WheatMarketUiTextures.RED_BUTTON_PRESSED_COLOR, -1);
        cancelButton.setText(Component.translatable("gui.wheatmarket.edit.back"));
        cancelButton.setOnClick(event -> onCancel.run());

        shared.actionContent().addChildren(delistButton, cancelButton);
    }

    private Label createLabel(Horizontal horizontal) {
        Label label = new Label();
        label.layout(layout -> layout.widthPercent(100).height(18).flexShrink(0));
        label.lss("horizontal-align", horizontal.name());
        label.lss("vertical-align", "CENTER");
        label.lss("text-wrap", "HIDE");
        label.lss("text-shadow", "false");
        return label;
    }

    private Label createSummaryLabel() {
        Label label = new Label();
        label.layout(layout -> layout.widthPercent(100).height(21).flexShrink(0));
        label.lss("horizontal-align", "CENTER");
        label.lss("vertical-align", "CENTER");
        label.lss("text-wrap", "HIDE");
        label.lss("text-shadow", "false");
        return label;
    }

    private void configurePriceField() {
        priceField.setAnyString();
        priceField.setCharValidator(character -> Character.isDigit(character) || character == '.');
        priceField.setTextValidator(WheatMarketUiHelpers::isPriceInputValid);
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
        double price = WheatMarketUiHelpers.parsePrice(WheatMarketUiHelpers.rawText(priceField), 0.0D);
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
        int amount = WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(cooldownAmountField), -1);
        int time = WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(cooldownTimeField), -1);
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
        double price = WheatMarketUiHelpers.parsePrice(WheatMarketUiHelpers.rawText(priceField), 0.0D);
        int cooldownAmount = WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(cooldownAmountField), -1);
        int cooldownTime = WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(cooldownTimeField), -1);

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
        WheatMarketUiHelpers.setShown(opPanel, operator);
    }

    private void updateBalanceLabel() {
        if (playerBalanceLabel == null || Double.compare(seenBalance, WheatMarket.CLIENT_BALANCE) == 0) {
            return;
        }
        seenBalance = WheatMarket.CLIENT_BALANCE;
        playerBalanceLabel.setText(Component.translatable("gui.wheatmarket.balance", WheatMarketUiHelpers.formatMoney(seenBalance)));
    }

    private void normalizePriceFieldIfBlurred() {
        if (priceField == null || priceField.isFocused()) {
            return;
        }
        double price = WheatMarketUiHelpers.parsePrice(WheatMarketUiHelpers.rawText(priceField), currentPrice);
        if (price <= 0.0D || !Double.isFinite(price)) {
            price = currentPrice;
        }
        syncPriceFieldText(WheatMarketUiHelpers.formatMoney(price));
    }

    private void normalizeCooldownFieldIfBlurred(TextField field, int fallback, boolean amountField) {
        if (field == null || field.isFocused()) {
            return;
        }
        int value = WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(field), fallback);
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
        WheatMarketUiHelpers.styleLabel(feedbackLabel, color, Horizontal.CENTER);
        feedbackLabel.setText(message);
        WheatMarketUiHelpers.setShown(feedbackLabel, true);
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

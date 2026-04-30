package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.w3c.dom.Document;
import top.rookiestwo.wheatmarket.WheatMarket;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class WheatMarketListingUI {
    private static final ResourceLocation LISTING_XML = ResourceLocation.parse("wheatmarket:ui/listing.xml");
    private static final int MAX_LISTING_QUANTITY = 999;
    private static final int DEFAULT_LISTING_DURATION_DAYS = 7;
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

    private final Consumer<Draft> onListingTypeChanged;
    private ItemStack stack;
    private final ListingType initialListingType;
    private final String initialPriceText;
    private final int initialBuyQuantity;
    private final boolean initialAdmin;
    private final boolean initialInfinite;
    private final int initialCooldownAmount;
    private final int initialCooldownTime;
    private final Runnable onSelectItem;
    private final Consumer<Submission> onSubmit;
    private int selectedAmount;
    private final Runnable onCancel;

    private Selector<ListingType> listingTypeSelector;
    private TextField priceField;
    private TextField quantityField;
    private Label listingTitle;
    private Label selectedItemLabel;
    private Label quantityCaption;
    private Label playerBalanceLabel;
    private Label totalLabel;
    private Label feedbackLabel;
    private UIElement opPanel;
    private UIElement quantityRow;
    private UIElement itemIcon;
    private TextField cooldownAmountField;
    private TextField cooldownTimeField;
    private Button chooseItemButton;
    private Button decreaseButton;
    private Button increaseButton;
    private Button confirmButton;
    private Button cancelButton;
    private Button toggleAdminButton;
    private Button toggleInfiniteButton;
    private int quantity = 1;
    private int buyQuantity = 1;
    private int cooldownAmount;
    private int cooldownTime;
    private double unitPrice = 1.0D;
    private double seenBalance = Double.NaN;
    private boolean syncingQuantityField;
    private boolean syncingPriceField;
    private boolean syncingCooldownAmountField;
    private boolean syncingCooldownTimeField;
    private boolean submitting;
    private boolean operator;
    private boolean currentAdmin;
    private boolean currentInfinite;
    private ListingType currentListingType;

    public WheatMarketListingUI(ItemStack stack, int selectedAmount, ListingType initialListingType,
                                String initialPriceText, int initialBuyQuantity,
                                boolean initialAdmin, boolean initialInfinite,
                                int initialCooldownAmount, int initialCooldownTime,
                                Runnable onSelectItem, Consumer<Submission> onSubmit,
                                Consumer<Draft> onListingTypeChanged, Runnable onCancel) {
        this.stack = templateCopy(stack);
        this.selectedAmount = this.stack.isEmpty() ? 0 : Math.max(1, selectedAmount);
        this.initialListingType = initialListingType == null ? ListingType.SELL : initialListingType;
        this.currentListingType = this.initialListingType;
        this.initialPriceText = initialPriceText == null || initialPriceText.isBlank() ? "1.00" : initialPriceText;
        this.initialBuyQuantity = Mth.clamp(initialBuyQuantity, 1, MAX_LISTING_QUANTITY);
        this.initialAdmin = initialAdmin;
        this.initialInfinite = initialInfinite;
        this.initialCooldownAmount = Math.max(0, initialCooldownAmount);
        this.initialCooldownTime = Math.max(0, initialCooldownTime);
        this.currentAdmin = initialAdmin;
        this.currentInfinite = initialInfinite;
        this.cooldownAmount = Math.max(0, initialCooldownAmount);
        this.cooldownTime = Math.max(0, initialCooldownTime);
        this.onSelectItem = onSelectItem;
        this.onSubmit = onSubmit == null ? submission -> {
        } : onSubmit;
        this.onListingTypeChanged = onListingTypeChanged == null ? draft -> {
        } : onListingTypeChanged;
        this.onCancel = onCancel;
    }

    public ModularUI create(Player player) {
        Document xml = XmlUtils.loadXml(LISTING_XML);
        if (xml == null) {
            throw new IllegalStateException("Failed to load UI xml: " + LISTING_XML);
        }

        UI loadedUi = UI.of(xml);
        UI ui = UI.of(loadedUi.getRootElement(), loadedUi.getStylesheets(), availableSize -> availableSize);
        bindAndPopulate(ui, player);
        return ModularUI.of(ui, player);
    }

    public void tick() {
        updateBalanceLabel();
        normalizeQuantityFieldIfBlurred();
        normalizePriceFieldIfBlurred();
        normalizeCooldownFieldIfBlurred(cooldownAmountField, cooldownAmount, true);
        normalizeCooldownFieldIfBlurred(cooldownTimeField, cooldownTime, false);
    }

    public Draft createDraft() {
        String priceText = priceField == null ? initialPriceText : priceField.getRawText();
        return new Draft(stack, selectedAmount, selectedListingType(), priceText, Math.max(1, buyQuantity),
                currentAdmin, currentInfinite,
                parseNonNegativeInt(rawText(cooldownAmountField), cooldownAmount),
                parseNonNegativeInt(rawText(cooldownTimeField), cooldownTime));
    }

    public boolean handleOperationResult(boolean success, Component message) {
        if (!submitting) {
            return false;
        }
        submitting = false;
        showFeedback(message, success ? NOTICE_TEXT_COLOR : FAILURE_TEXT_COLOR);
        updateFormState();
        return true;
    }

    private void bindAndPopulate(UI ui, Player player) {
        UIElement root = require(ui, "listing-root", UIElement.class);
        UIElement listingPaper = require(ui, "listing-paper", UIElement.class);
        ScrollerView listingScroller = require(ui, "listing-scroll", ScrollerView.class);
        UIElement playerAvatar = require(ui, "player-avatar", UIElement.class);
        UIElement itemPreview = require(ui, "item-preview", UIElement.class);
        itemIcon = require(ui, "listing-item-icon", UIElement.class);

        listingTitle = require(ui, "listing-title", Label.class);
        Label ownerLabel = require(ui, "owner-label", Label.class);
        selectedItemLabel = require(ui, "selected-item-label", Label.class);
        Label typeCaption = require(ui, "type-caption", Label.class);
        Label priceCaption = require(ui, "price-caption", Label.class);
        quantityCaption = require(ui, "quantity-caption", Label.class);
        Label opTitle = require(ui, "op-title", Label.class);
        Label cooldownAmountCaption = require(ui, "cooldown-amount-caption", Label.class);
        Label cooldownTimeCaption = require(ui, "cooldown-time-caption", Label.class);
        Label expiryLabel = require(ui, "expiry-label", Label.class);
        playerBalanceLabel = require(ui, "player-balance", Label.class);
        totalLabel = require(ui, "total-label", Label.class);
        feedbackLabel = require(ui, "feedback-label", Label.class);

        opPanel = require(ui, "op-panel", UIElement.class);
        quantityRow = require(ui, "quantity-row", UIElement.class);
        listingTypeSelector = requireSelector(ui, "listing-type-selector");
        priceField = require(ui, "price-field", TextField.class);
        quantityField = require(ui, "quantity-field", TextField.class);
        cooldownAmountField = require(ui, "cooldown-amount-field", TextField.class);
        cooldownTimeField = require(ui, "cooldown-time-field", TextField.class);
        chooseItemButton = require(ui, "choose-item-button", Button.class);
        decreaseButton = require(ui, "decrease-button", Button.class);
        increaseButton = require(ui, "increase-button", Button.class);
        confirmButton = require(ui, "confirm-button", Button.class);
        cancelButton = require(ui, "cancel-button", Button.class);
        toggleAdminButton = require(ui, "toggle-admin-button", Button.class);
        toggleInfiniteButton = require(ui, "toggle-infinite-button", Button.class);

        root.style(style -> style.background(WheatMarketUiTextures.tradingBackgroundTexture()));
        root.addEventListener(UIEvents.MOUSE_DOWN, event -> onRootMouseDown(event.x, event.y), true);
        listingPaper.style(style -> style.background(WheatMarketUiTextures.tradingPaperTexture()));
        configureListingScroller(listingScroller);
        playerAvatar.style(style -> style.background(WheatMarketUiTextures.playerAvatarTexture(player)));
        playerBalanceLabel.style(style -> style.background(WheatMarketUiTextures.paperTexture()));
        itemPreview.style(style -> style.background(new ColorBorderTexture(-1, ITEM_PREVIEW_BORDER)));
        itemIcon.style(style -> style.background(new ItemStackTexture(stack)));
        itemPreview.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = WheatMarketItemTooltips.forStack(stack));
        listingTypeSelector.style(style -> style.background(WheatMarketUiTextures.paperTexture()));
        priceField.style(style -> style.background(FIELD_TEXTURE));
        quantityField.style(style -> style.background(FIELD_TEXTURE));

        styleLabel(listingTitle, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(ownerLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(selectedItemLabel, TEXT_COLOR, Horizontal.CENTER);
        styleFormCaption(typeCaption);
        styleFormCaption(priceCaption);
        styleFormCaption(quantityCaption);
        styleLabel(opTitle, NOTICE_TEXT_COLOR, Horizontal.CENTER);
        styleFormCaption(cooldownAmountCaption);
        styleFormCaption(cooldownTimeCaption);
        styleLabel(expiryLabel, NOTICE_TEXT_COLOR, Horizontal.CENTER);
        styleLabel(playerBalanceLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(totalLabel, TEXT_COLOR, Horizontal.CENTER);
        styleLabel(feedbackLabel, NOTICE_TEXT_COLOR, Horizontal.CENTER);
        styleActionButton(chooseItemButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(decreaseButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED, -1);
        styleActionButton(increaseButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED, -1);
        styleActionButton(toggleAdminButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED, -1);
        styleActionButton(toggleInfiniteButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED, -1);
        styleActionButton(confirmButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(cancelButton, RED_BUTTON, RED_BUTTON_HOVER, RED_BUTTON_PRESSED);
        chooseItemButton.enableText();
        toggleAdminButton.enableText();
        toggleInfiniteButton.enableText();
        confirmButton.enableText();

        operator = player.hasPermissions(2);
        if (!operator) {
            currentAdmin = false;
            currentInfinite = false;
            cooldownAmount = 0;
            cooldownTime = 0;
        }
        ownerLabel.setText(Component.translatable("gui.wheatmarket.listing.owner", player.getName()));
        typeCaption.setText(Component.translatable("gui.wheatmarket.listing.type_label"));
        priceCaption.setText(Component.translatable("gui.wheatmarket.listing.price_label"));
        opTitle.setText(Component.translatable("gui.wheatmarket.edit.op_options"));
        cooldownAmountCaption.setText(Component.translatable("gui.wheatmarket.edit.cooldown_amount"));
        cooldownTimeCaption.setText(Component.translatable("gui.wheatmarket.edit.cooldown_time"));
        expiryLabel.setText(Component.translatable("gui.wheatmarket.listing.expiry", DEFAULT_LISTING_DURATION_DAYS));
        chooseItemButton.setText(Component.translatable("gui.wheatmarket.listing.select_item"));
        feedbackLabel.setText(Component.empty());

        configureTypeSelector();
        configurePriceField();
        configureQuantityField();
        configureCooldownField(cooldownAmountField, true);
        configureCooldownField(cooldownTimeField, false);

        chooseItemButton.setOnClick(event -> onSelectItem.run());
        decreaseButton.setOnClick(event -> updateBuyQuantity(quantity - 1));
        increaseButton.setOnClick(event -> updateBuyQuantity(quantity + 1));
        toggleAdminButton.setOnClick(event -> toggleAdminListing());
        toggleInfiniteButton.setOnClick(event -> toggleInfiniteListing());
        confirmButton.setOnClick(event -> submitListing());
        cancelButton.setOnClick(event -> onCancel.run());

        unitPrice = parsePrice(initialPriceText);
        syncPriceFieldText(initialPriceText);
        syncCooldownAmountText(String.valueOf(cooldownAmount));
        syncCooldownTimeText(String.valueOf(cooldownTime));
        buyQuantity = initialBuyQuantity;
        updateQuantityForSelectedType();
        updateBalanceLabel();
        updateListingTypeText();
        updateSelectedItemLabel();
        updateFormState();
    }

    private void configureTypeSelector() {
        listingTypeSelector.setCandidates(List.of(ListingType.SELL, ListingType.BUY))
                .setSelected(initialListingType)
                .setOnValueChanged(value -> {
                    ListingType nextListingType = value == null ? initialListingType : value;
                    if (nextListingType != currentListingType) {
                        currentListingType = nextListingType;
                        if (currentListingType != ListingType.SELL) {
                            currentInfinite = false;
                        }
                        clearSelectedItem();
                        onListingTypeChanged.accept(createDraft());
                    }
                    updateQuantityForSelectedType();
                    updateListingTypeText();
                    updateSelectedItemLabel();
                    hideFeedback();
                });
    }

    private void configureListingScroller(ScrollerView scroller) {
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

    private void configurePriceField() {
        priceField.setAnyString();
        priceField.setCharValidator(character -> Character.isDigit(character) || character == '.');
        priceField.setTextValidator(this::isPriceInputValid);
        priceField.textFieldStyle(style -> style
                .placeholder(Component.translatable("gui.wheatmarket.listing.price_placeholder"))
                .textColor(TEXT_COLOR)
                .cursorColor(0xFF19140D)
                .errorColor(FAILURE_TEXT_COLOR)
                .textShadow(false)
                .focusOverlay(EMPTY_OVERLAY));
        priceField.registerValueListener(this::onPriceFieldChanged);
        priceField.addEventListener(UIEvents.BLUR, event -> normalizePriceFieldIfBlurred());
    }

    private void configureQuantityField() {
        quantityField.setAnyString();
        quantityField.setCharValidator(Character::isDigit);
        quantityField.setTextValidator(text -> text.isBlank() || text.chars().allMatch(Character::isDigit));
        quantityField.textFieldStyle(style -> style
                .placeholder(Component.translatable("gui.wheatmarket.confirm.empty"))
                .textColor(TEXT_COLOR)
                .cursorColor(0xFF19140D)
                .errorColor(FAILURE_TEXT_COLOR)
                .textShadow(false)
                .focusOverlay(EMPTY_OVERLAY));
        quantityField.registerValueListener(this::onQuantityFieldChanged);
        quantityField.addEventListener(UIEvents.BLUR, event -> normalizeQuantityFieldIfBlurred());
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
                if (amountField) {
                    cooldownAmount = parseNonNegativeInt(rawValue, cooldownAmount);
                } else {
                    cooldownTime = parseNonNegativeInt(rawValue, cooldownTime);
                }
                hideFeedback();
                updateFormState();
            }
        });
        field.addEventListener(UIEvents.BLUR, event -> normalizeCooldownFieldIfBlurred(field,
                amountField ? cooldownAmount : cooldownTime,
                amountField));
    }

    private void onPriceFieldChanged(String rawValue) {
        if (syncingPriceField) {
            return;
        }
        unitPrice = parsePrice(rawValue);
        hideFeedback();
        updateFormState();
    }

    private void onQuantityFieldChanged(String rawValue) {
        if (syncingQuantityField || !usesQuantityField()) {
            return;
        }
        if (rawValue == null || rawValue.isBlank()) {
            quantity = 0;
            hideFeedback();
            updateFormState();
            return;
        }
        updateBuyQuantity(parseQuantityValue(rawValue));
        hideFeedback();
    }

    private void toggleAdminListing() {
        if (!operator || submitting) {
            return;
        }
        currentAdmin = !currentAdmin;
        if (!currentAdmin || selectedListingType() != ListingType.SELL) {
            currentInfinite = false;
        }
        hideFeedback();
        updateQuantityForSelectedType();
        updateSelectedItemLabel();
        updateFormState();
    }

    private void toggleInfiniteListing() {
        if (!operator || submitting || !currentAdmin || selectedListingType() != ListingType.SELL) {
            return;
        }
        currentInfinite = !currentInfinite;
        hideFeedback();
        updateQuantityForSelectedType();
        updateFormState();
    }

    private void updateQuantityForSelectedType() {
        if (usesQuantityField()) {
            updateBuyQuantity(buyQuantity);
        } else {
            quantity = selectedAmount;
            updateFormState();
        }
    }

    private void updateBuyQuantity(int newQuantity) {
        buyQuantity = Mth.clamp(newQuantity, 1, MAX_LISTING_QUANTITY);
        quantity = buyQuantity;
        syncQuantityFieldText();
        updateFormState();
    }

    private int activeQuantity() {
        if (currentAdmin && selectedListingType() == ListingType.SELL && currentInfinite) {
            return 1;
        }
        return usesQuantityField() ? quantity : selectedAmount;
    }

    private boolean usesQuantityField() {
        return selectedListingType() == ListingType.BUY
                || (currentAdmin && selectedListingType() == ListingType.SELL && !currentInfinite);
    }

    private void updateFormState() {
        boolean buyOrder = selectedListingType() == ListingType.BUY;
        boolean quantityEditable = usesQuantityField();
        setShown(quantityRow, quantityEditable);
        quantityCaption.setText(Component.translatable(currentAdmin && selectedListingType() == ListingType.SELL
                ? "gui.wheatmarket.listing.stock_quantity_label"
                : "gui.wheatmarket.listing.buy_quantity_label"));
        if (selectedListingType() != ListingType.SELL) {
            currentInfinite = false;
        }

        boolean hasValidItem = !stack.isEmpty();
        boolean hasValidPrice = unitPrice > 0.0D && Double.isFinite(unitPrice);
        boolean hasValidQuantity = activeQuantity() > 0;
        double total = hasValidPrice && hasValidQuantity ? unitPrice * activeQuantity() : 0.0D;
        totalLabel.setText(Component.translatable("gui.wheatmarket.listing.total", formatMoney(total)));
        confirmButton.setActive(!submitting && hasValidItem && hasValidPrice && hasValidQuantity);
        chooseItemButton.setActive(!submitting);
        listingTypeSelector.setActive(!submitting);
        priceField.setActive(!submitting);
        quantityField.setActive(!submitting && quantityEditable);
        decreaseButton.setActive(quantityEditable && quantity > 1);
        increaseButton.setActive(quantityEditable && quantity < MAX_LISTING_QUANTITY);
        decreaseButton.setVisible(quantityEditable && quantity > 1);
        increaseButton.setVisible(quantityEditable && quantity < MAX_LISTING_QUANTITY);
        setShown(opPanel, operator);
        toggleAdminButton.setText(Component.translatable(currentAdmin
                ? "gui.wheatmarket.edit.system_shop_on"
                : "gui.wheatmarket.edit.system_shop_off"));
        toggleInfiniteButton.setText(Component.translatable(currentInfinite
                ? "gui.wheatmarket.edit.infinite_on"
                : "gui.wheatmarket.edit.infinite_off"));
        toggleAdminButton.setActive(!submitting && operator);
        toggleInfiniteButton.setActive(!submitting && operator && currentAdmin && selectedListingType() == ListingType.SELL);
        cooldownAmountField.setActive(!submitting && operator);
        cooldownTimeField.setActive(!submitting && operator);
    }

    private void updateListingTypeText() {
        ListingType type = selectedListingType();
        listingTitle.setText(Component.translatable(type == ListingType.SELL
                ? "gui.wheatmarket.listing.sell_title"
                : "gui.wheatmarket.listing.buy_title"));
    }

    private void updateSelectedItemLabel() {
        if (stack.isEmpty()) {
            selectedItemLabel.setText(Component.translatable("gui.wheatmarket.listing.no_selected_item"));
            return;
        }
        if (currentAdmin && selectedListingType() == ListingType.SELL) {
            selectedItemLabel.setText(Component.translatable("gui.wheatmarket.listing.selected_system_item", stack.getHoverName()));
            return;
        }
        if (selectedListingType() == ListingType.BUY) {
            selectedItemLabel.setText(Component.translatable("gui.wheatmarket.listing.selected_buy_item", stack.getHoverName()));
        } else {
            selectedItemLabel.setText(Component.translatable("gui.wheatmarket.listing.selected_sell_item", stack.getHoverName(), selectedAmount));
        }
    }

    private void clearSelectedItem() {
        stack = ItemStack.EMPTY;
        selectedAmount = 0;
        if (itemIcon != null) {
            itemIcon.style(style -> style.background(new ItemStackTexture(stack)));
        }
    }

    private void submitListing() {
        if (submitting) {
            return;
        }

        ListingType listingType = selectedListingType();
        int listingAmount = activeQuantity();
        if (stack.isEmpty()) {
            showFeedback(Component.translatable("gui.wheatmarket.operation.no_selected_item"), FAILURE_TEXT_COLOR);
            return;
        }
        if (unitPrice <= 0.0D || !Double.isFinite(unitPrice)) {
            showFeedback(Component.translatable("gui.wheatmarket.operation.invalid_price"), FAILURE_TEXT_COLOR);
            return;
        }
        if (listingAmount <= 0) {
            showFeedback(Component.translatable("gui.wheatmarket.operation.invalid_amount"), FAILURE_TEXT_COLOR);
            return;
        }
        int submissionCooldownAmount = operator ? parseNonNegativeInt(rawText(cooldownAmountField), -1) : 0;
        int submissionCooldownTime = operator ? parseNonNegativeInt(rawText(cooldownTimeField), -1) : 0;
        if (submissionCooldownAmount < 0 || submissionCooldownTime < 0) {
            showFeedback(Component.translatable("gui.wheatmarket.operation.invalid_amount"), FAILURE_TEXT_COLOR);
            return;
        }

        submitting = true;
        showFeedback(Component.translatable("gui.wheatmarket.listing.submitting"), NOTICE_TEXT_COLOR);
        updateFormState();
        onSubmit.accept(new Submission(listingType, unitPrice, listingAmount,
                operator && currentAdmin,
                operator && currentAdmin && listingType == ListingType.SELL && currentInfinite,
                submissionCooldownAmount,
                submissionCooldownTime));
    }

    private void showFeedback(Component message, int color) {
        styleLabel(feedbackLabel, color, Horizontal.CENTER);
        feedbackLabel.setText(message);
        feedbackLabel.setDisplay(true);
        feedbackLabel.setVisible(true);
    }

    private void hideFeedback() {
        feedbackLabel.setText(Component.empty());
    }

    private void normalizeQuantityFieldIfBlurred() {
        if (!usesQuantityField()) {
            return;
        }
        if (quantityField != null && !quantityField.isFocused() && quantityField.getRawText().isBlank()) {
            updateBuyQuantity(1);
        }
    }

    private void normalizePriceFieldIfBlurred() {
        if (priceField == null || priceField.isFocused()) {
            return;
        }
        if (unitPrice <= 0.0D || !Double.isFinite(unitPrice)) {
            unitPrice = 1.0D;
        }
        syncPriceFieldText(formatMoney(unitPrice));
        updateFormState();
    }

    private void onRootMouseDown(float mouseX, float mouseY) {
        if (priceField != null && !priceField.isMouseOver(mouseX, mouseY)) {
            priceField.blur();
        }
        if (quantityField != null && !quantityField.isMouseOver(mouseX, mouseY)) {
            quantityField.blur();
        }
        if (cooldownAmountField != null && !cooldownAmountField.isMouseOver(mouseX, mouseY)) {
            cooldownAmountField.blur();
        }
        if (cooldownTimeField != null && !cooldownTimeField.isMouseOver(mouseX, mouseY)) {
            cooldownTimeField.blur();
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

    private double parsePrice(String rawValue) {
        if (rawValue == null || rawValue.isBlank() || ".".equals(rawValue)) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }

    private int parseQuantityValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return 1;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ignored) {
            return 1;
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

    private void syncQuantityFieldText() {
        String quantityText = String.valueOf(quantity);
        if (!quantityText.equals(quantityField.getRawText())) {
            syncingQuantityField = true;
            quantityField.setValue(quantityText, false);
            syncingQuantityField = false;
        }
    }

    private void syncPriceFieldText(String priceText) {
        if (!priceText.equals(priceField.getRawText())) {
            syncingPriceField = true;
            priceField.setValue(priceText, false);
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

    private void normalizeCooldownFieldIfBlurred(TextField field, int fallback, boolean amountField) {
        if (field == null || field.isFocused()) {
            return;
        }
        int value = Math.max(0, parseNonNegativeInt(field.getRawText(), fallback));
        if (amountField) {
            cooldownAmount = value;
            syncCooldownAmountText(String.valueOf(value));
        } else {
            cooldownTime = value;
            syncCooldownTimeText(String.valueOf(value));
        }
    }

    private String rawText(TextField field) {
        return field == null ? "" : field.getRawText();
    }

    private ListingType selectedListingType() {
        ListingType selected = listingTypeSelector.getValue();
        return selected == null ? initialListingType : selected;
    }

    private void updateBalanceLabel() {
        if (Double.compare(seenBalance, WheatMarket.CLIENT_BALANCE) == 0) {
            return;
        }
        seenBalance = WheatMarket.CLIENT_BALANCE;
        playerBalanceLabel.setText(Component.translatable("gui.wheatmarket.balance", formatMoney(seenBalance)));
    }

    private void styleLabel(Label label, int color, Horizontal horizontal) {
        label.textStyle(style -> style
                .textAlignHorizontal(horizontal)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(color)
                .textShadow(false));
    }

    private void styleFormCaption(Label label) {
        label.textStyle(style -> style
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(TEXT_COLOR)
                .textShadow(false)
                .adaptiveWidth(true));
    }

    private void styleActionButton(Button button, int baseColor, int hoverColor, int pressedColor) {
        styleActionButton(button, baseColor, hoverColor, pressedColor, 1);
    }

    private void styleActionButton(Button button, int baseColor, int hoverColor, int pressedColor, int borderSize) {
        button.buttonStyle(style -> style
                .baseTexture(buttonTexture(baseColor, borderSize))
                .hoverTexture(buttonTexture(hoverColor, borderSize))
                .pressedTexture(buttonTexture(pressedColor, borderSize)));
        button.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(TEXT_COLOR)
                .textShadow(false));
    }

    private IGuiTexture buttonTexture(int color) {
        return buttonTexture(color, 1);
    }

    private IGuiTexture buttonTexture(int color, int borderSize) {
        return GuiTextureGroup.of(
                new ColorRectTexture(color),
                new ColorBorderTexture(borderSize, BUTTON_BORDER)
        );
    }

    private void setShown(UIElement element, boolean shown) {
        element.setDisplay(shown);
        element.setVisible(shown);
    }

    private ItemStack templateCopy(ItemStack source) {
        if (source == null || source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = source.copy();
        copy.setCount(1);
        return copy;
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

    private String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public enum ListingType {
        SELL("gui.wheatmarket.filter.sell"),
        BUY("gui.wheatmarket.filter.buy");

        private final String translationKey;

        ListingType(String translationKey) {
            this.translationKey = translationKey;
        }

        @Override
        public String toString() {
            return Component.translatable(translationKey).getString();
        }
    }

    public record Draft(ItemStack selectedStack, int selectedAmount, ListingType listingType,
                        String priceText, int buyQuantity, boolean ifAdmin, boolean ifInfinite,
                        int cooldownAmount, int cooldownTimeInMinutes) {
        public Draft {
            selectedStack = selectedStack == null || selectedStack.isEmpty() ? ItemStack.EMPTY : selectedStack.copy();
            if (!selectedStack.isEmpty()) {
                selectedStack.setCount(1);
            }
            selectedAmount = selectedStack.isEmpty() ? 0 : Math.max(1, selectedAmount);
            listingType = listingType == null ? ListingType.SELL : listingType;
            priceText = priceText == null || priceText.isBlank() ? "1.00" : priceText;
            buyQuantity = Mth.clamp(buyQuantity, 1, MAX_LISTING_QUANTITY);
            ifInfinite = ifAdmin && listingType == ListingType.SELL && ifInfinite;
            cooldownAmount = Math.max(0, cooldownAmount);
            cooldownTimeInMinutes = Math.max(0, cooldownTimeInMinutes);
        }
    }

    public record Submission(ListingType listingType, double price, int amount, boolean ifAdmin, boolean ifInfinite,
                             int cooldownAmount, int cooldownTimeInMinutes) {
        public Submission {
            listingType = listingType == null ? ListingType.SELL : listingType;
            amount = Math.max(1, amount);
            ifInfinite = ifAdmin && listingType == ListingType.SELL && ifInfinite;
            cooldownAmount = Math.max(0, cooldownAmount);
            cooldownTimeInMinutes = Math.max(0, cooldownTimeInMinutes);
        }
    }
}

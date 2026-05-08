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
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.rookiestwo.wheatmarket.WheatMarket;

import java.util.List;
import java.util.function.Consumer;

public class WheatMarketListingUI {
    private static final int MAX_LISTING_QUANTITY = 999;
    private static final int DEFAULT_LISTING_DURATION_DAYS = 7;
    private static final int TEXT_COLOR = 0x19140D;
    private static final int NOTICE_TEXT_COLOR = 0x665A4D;
    private static final int FAILURE_TEXT_COLOR = 0xA33629;
    private static final int BUTTON_BORDER = 0xFF3A332C;
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
    private UIElement root;

    public WheatMarketListingUI(ItemStack stack, int selectedAmount, ListingType initialListingType,
                                String initialPriceText, int initialBuyQuantity,
                                boolean initialAdmin, boolean initialInfinite,
                                int initialCooldownAmount, int initialCooldownTime,
                                Runnable onSelectItem, Consumer<Submission> onSubmit,
                                Consumer<Draft> onListingTypeChanged, Runnable onCancel) {
        this.stack = WheatMarketUiHelpers.templateCopy(stack);
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
        PaperFormFactory.SharedPaperForm shared = PaperFormFactory.create(player);
        populate(shared, player);
        return ModularUI.of(shared.ui(), player);
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
                WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(cooldownAmountField), cooldownAmount),
                WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(cooldownTimeField), cooldownTime));
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

    private void populate(PaperFormFactory.SharedPaperForm shared, Player player) {
        root = shared.root();
        root.addEventListener(UIEvents.MOUSE_DOWN, event -> onRootMouseDown(event.x, event.y), true);

        listingTitle = shared.title();
        playerBalanceLabel = shared.playerBalanceLabel();
        itemIcon = shared.itemIcon();
        itemIcon.style(style -> style.background(new ItemStackTexture(stack)));
        shared.itemPreview().addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = WheatMarketItemTooltips.forStack(stack));

        opPanel = shared.opPanel();
        toggleAdminButton = shared.toggleAdminButton();
        toggleInfiniteButton = shared.toggleInfiniteButton();
        cooldownAmountField = shared.cooldownAmountField();
        cooldownTimeField = shared.cooldownTimeField();

        WheatMarketUiHelpers.styleLabel(shared.ownerLabel(), TEXT_COLOR, Horizontal.CENTER);
        WheatMarketUiHelpers.styleLabel(shared.opTitle(), NOTICE_TEXT_COLOR, Horizontal.CENTER);
        WheatMarketUiHelpers.styleFormCaption(shared.cooldownAmountCaption());
        WheatMarketUiHelpers.styleFormCaption(shared.cooldownTimeCaption());
        WheatMarketUiHelpers.styleLabel(listingTitle, TEXT_COLOR, Horizontal.CENTER);
        WheatMarketUiHelpers.styleLabel(playerBalanceLabel, TEXT_COLOR, Horizontal.CENTER);

        operator = player.hasPermissions(2);
        if (!operator) {
            currentAdmin = false;
            currentInfinite = false;
            cooldownAmount = 0;
            cooldownTime = 0;
        }

        shared.ownerLabel().setText(Component.translatable("gui.wheatmarket.listing.owner", player.getName()));
        shared.opTitle().setText(Component.translatable("gui.wheatmarket.edit.op_options"));
        shared.cooldownAmountCaption().setText(Component.translatable("gui.wheatmarket.edit.cooldown_amount"));
        shared.cooldownTimeCaption().setText(Component.translatable("gui.wheatmarket.edit.cooldown_time"));

        toggleAdminButton.enableText();
        toggleInfiniteButton.enableText();
        WheatMarketUiTextures.styleColoredActionButton(toggleAdminButton, WheatMarketUiTextures.BLUE_BUTTON_COLOR, WheatMarketUiTextures.BLUE_BUTTON_HOVER_COLOR, WheatMarketUiTextures.BLUE_BUTTON_PRESSED_COLOR, -1);
        WheatMarketUiTextures.styleColoredActionButton(toggleInfiniteButton, WheatMarketUiTextures.BLUE_BUTTON_COLOR, WheatMarketUiTextures.BLUE_BUTTON_HOVER_COLOR, WheatMarketUiTextures.BLUE_BUTTON_PRESSED_COLOR, -1);
        toggleAdminButton.setOnClick(event -> toggleAdminListing());
        toggleInfiniteButton.setOnClick(event -> toggleInfiniteListing());
        configureCooldownField(cooldownAmountField, true);
        configureCooldownField(cooldownTimeField, false);

        buildLeftExtra(shared);
        buildRightContent(shared);
        buildBottomBar(shared);
        buildActionColumn(shared);

        syncCooldownAmountText(String.valueOf(cooldownAmount));
        syncCooldownTimeText(String.valueOf(cooldownTime));
        buyQuantity = initialBuyQuantity;
        unitPrice = WheatMarketUiHelpers.parsePrice(initialPriceText, 0.0D);
        syncPriceFieldText(initialPriceText);
        updateQuantityForSelectedType();
        updateBalanceLabel();
        updateListingTypeText();
        updateSelectedItemLabel();
        updateFormState();
    }

    private void buildLeftExtra(PaperFormFactory.SharedPaperForm shared) {
        selectedItemLabel = createLabel();
        WheatMarketUiHelpers.styleLabel(selectedItemLabel, TEXT_COLOR, Horizontal.CENTER);

        chooseItemButton = new Button();
        chooseItemButton.enableText();
        chooseItemButton.layout(layout -> layout.widthPercent(88).maxWidthPercent(100).height(22).flexShrink(0));
        WheatMarketUiTextures.styleColoredActionButton(chooseItemButton, WheatMarketUiTextures.BLUE_BUTTON_COLOR, WheatMarketUiTextures.BLUE_BUTTON_HOVER_COLOR, WheatMarketUiTextures.BLUE_BUTTON_PRESSED_COLOR, 1);
        chooseItemButton.setText(Component.translatable("gui.wheatmarket.listing.select_item"));
        chooseItemButton.setOnClick(event -> onSelectItem.run());

        shared.leftExtra().addChildren(selectedItemLabel, chooseItemButton);
    }

    private void buildRightContent(PaperFormFactory.SharedPaperForm shared) {
        listingTypeSelector = new Selector<>();
        listingTypeSelector.layout(layout -> layout.width(0).flex(1).minWidth(46).maxWidth(76).height(22).flexShrink(1));
        listingTypeSelector.style(style -> style.background(WheatMarketUiTextures.paperTexture()));
        listingTypeSelector.lss("text-color", "#19140D");
        listingTypeSelector.lss("text-shadow", "false");

        Label typeCaption = createLabel();
        WheatMarketUiHelpers.styleFormCaption(typeCaption);
        typeCaption.setText(Component.translatable("gui.wheatmarket.listing.type_label"));

        UIElement typeRow = formRow(formGroup(typeCaption, listingTypeSelector));

        priceField = createTextField("1.00");
        priceField.style(style -> style.background(FIELD_TEXTURE));
        Label priceCaption = createLabel();
        WheatMarketUiHelpers.styleFormCaption(priceCaption);
        priceCaption.setText(Component.translatable("gui.wheatmarket.listing.price_label"));
        UIElement priceRow = formRow(formGroup(priceCaption, priceField));

        quantityCaption = createLabel();
        WheatMarketUiHelpers.styleFormCaption(quantityCaption);
        quantityField = createTextField("1");
        quantityField.style(style -> style.background(FIELD_TEXTURE));
        quantityField.layout(layout -> layout.width(26).minWidth(26).maxWidth(50).height(22).paddingLeft(2).paddingRight(2).flexShrink(0));
        decreaseButton = createQuantityButton("-");
        increaseButton = createQuantityButton("+");
        UIElement quantityControls = new UIElement()
                .layout(layout -> layout.width(62).minWidth(62).maxWidth(68).height(22).gapAll(2).flexShrink(1));
        quantityControls.lss("flex-direction", "row");
        quantityControls.addChildren(decreaseButton, quantityField, increaseButton);
        UIElement quantityFormGroup = new UIElement()
                .layout(layout -> layout.maxWidthPercent(100).height(22).gapAll(4).flexShrink(1));
        quantityFormGroup.lss("flex-direction", "row");
        quantityFormGroup.lss("overflow", "hidden");
        quantityFormGroup.addChildren(quantityCaption, quantityControls);
        quantityRow = formRow(quantityFormGroup);

        Label expiryLabel = createLabel();
        WheatMarketUiHelpers.styleLabel(expiryLabel, NOTICE_TEXT_COLOR, Horizontal.CENTER);
        expiryLabel.layout(layout -> layout.widthPercent(100).maxWidthPercent(100).height(20).flexShrink(0));
        expiryLabel.setText(Component.translatable("gui.wheatmarket.listing.expiry", DEFAULT_LISTING_DURATION_DAYS));

        totalLabel = createLabel();
        WheatMarketUiHelpers.styleLabel(totalLabel, TEXT_COLOR, Horizontal.CENTER);
        totalLabel.layout(layout -> layout.widthPercent(100).maxWidthPercent(100).height(22).flexShrink(0));

        shared.rightContent().addChildren(typeRow, priceRow, quantityRow, expiryLabel, totalLabel);

        configureTypeSelector();
        configurePriceField();
        configureQuantityField();
    }

    private void buildBottomBar(PaperFormFactory.SharedPaperForm shared) {
        confirmButton = new Button();
        confirmButton.enableText();
        confirmButton.layout(layout -> layout.widthPercent(100).height(28).flexShrink(0));
        WheatMarketUiTextures.styleColoredActionButton(confirmButton, WheatMarketUiTextures.BLUE_BUTTON_COLOR, WheatMarketUiTextures.BLUE_BUTTON_HOVER_COLOR, WheatMarketUiTextures.BLUE_BUTTON_PRESSED_COLOR, 1);
        confirmButton.setOnClick(event -> submitListing());

        feedbackLabel = createLabel();
        WheatMarketUiHelpers.styleLabel(feedbackLabel, NOTICE_TEXT_COLOR, Horizontal.CENTER);
        feedbackLabel.layout(layout -> layout.widthPercent(100).height(16).flexShrink(0));
        feedbackLabel.setText(Component.empty());

        UIElement confirmArea = new UIElement()
                .layout(layout -> layout.widthPercent(62).minWidth(130).maxWidth(220).height(46)
                        .gapAll(2).paddingBottom(2).flexShrink(0));
        confirmArea.lss("flex-direction", "column");
        confirmArea.lss("align-items", "center");
        confirmArea.addChildren(confirmButton, feedbackLabel);

        shared.bottomBar().addChild(confirmArea);
    }

    private void buildActionColumn(PaperFormFactory.SharedPaperForm shared) {
        cancelButton = new Button();
        cancelButton.layout(layout -> layout.widthPercent(100).minWidth(64).maxWidth(96).height(76).flexShrink(0));
        WheatMarketUiTextures.styleColoredActionButton(cancelButton, WheatMarketUiTextures.RED_BUTTON_COLOR, WheatMarketUiTextures.RED_BUTTON_HOVER_COLOR, WheatMarketUiTextures.RED_BUTTON_PRESSED_COLOR, 1);
        cancelButton.setOnClick(event -> onCancel.run());
        shared.actionContent().addChild(cancelButton);
    }

    private Button createQuantityButton(String text) {
        Button button = new Button();
        button.enableText();
        button.layout(layout -> layout.width(14).height(22).flexShrink(0));
        button.setText(Component.literal(text));
        WheatMarketUiTextures.styleColoredActionButton(button, WheatMarketUiTextures.BLUE_BUTTON_COLOR, WheatMarketUiTextures.BLUE_BUTTON_HOVER_COLOR, WheatMarketUiTextures.BLUE_BUTTON_PRESSED_COLOR, -1);
        return button;
    }

    private TextField createTextField(String value) {
        TextField field = new TextField();
        field.layout(layout -> layout.width(0).flex(1).minWidth(46).maxWidth(86).height(22)
                .paddingLeft(6).paddingRight(6).flexShrink(1));
        field.lss("text-color", String.format("#%06X", TEXT_COLOR));
        field.lss("cursor-color", String.format("#%06X", TEXT_COLOR));
        field.lss("error-color", String.format("#%06X", FAILURE_TEXT_COLOR));
        field.lss("text-shadow", "false");
        field.setValue(value, false);
        return field;
    }

    private Label createLabel() {
        Label label = new Label();
        label.lss("flex-shrink", "0");
        return label;
    }

    private UIElement formRow(UIElement... children) {
        UIElement row = new UIElement()
                .layout(layout -> layout.widthPercent(100).maxWidthPercent(100).height(22).flexShrink(0));
        row.lss("flex-direction", "row");
        row.lss("align-items", "center");
        row.lss("justify-content", "center");
        row.lss("overflow", "hidden");
        row.addChildren(children);
        return row;
    }

    private UIElement formGroup(UIElement... children) {
        UIElement group = new UIElement()
                .layout(layout -> layout.maxWidthPercent(100).height(22).gapAll(4).flexShrink(1));
        group.lss("flex-direction", "row");
        group.lss("align-items", "center");
        group.lss("justify-content", "center");
        group.lss("overflow", "hidden");
        group.addChildren(children);
        return group;
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
                    cooldownAmount = WheatMarketUiHelpers.parseNonNegativeInt(rawValue, cooldownAmount);
                } else {
                    cooldownTime = WheatMarketUiHelpers.parseNonNegativeInt(rawValue, cooldownTime);
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
        unitPrice = WheatMarketUiHelpers.parsePrice(rawValue, 0.0D);
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
        WheatMarketUiHelpers.setShown(quantityRow, quantityEditable);
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
        totalLabel.setText(Component.translatable("gui.wheatmarket.listing.total", WheatMarketUiHelpers.formatMoney(total)));
        confirmButton.setActive(!submitting && hasValidItem && hasValidPrice && hasValidQuantity);
        chooseItemButton.setActive(!submitting);
        listingTypeSelector.setActive(!submitting);
        priceField.setActive(!submitting);
        quantityField.setActive(!submitting && quantityEditable);
        decreaseButton.setActive(quantityEditable && quantity > 1);
        increaseButton.setActive(quantityEditable && quantity < MAX_LISTING_QUANTITY);
        decreaseButton.setVisible(quantityEditable && quantity > 1);
        increaseButton.setVisible(quantityEditable && quantity < MAX_LISTING_QUANTITY);
        WheatMarketUiHelpers.setShown(opPanel, operator);
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
        int submissionCooldownAmount = operator ? WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(cooldownAmountField), -1) : 0;
        int submissionCooldownTime = operator ? WheatMarketUiHelpers.parseNonNegativeInt(WheatMarketUiHelpers.rawText(cooldownTimeField), -1) : 0;
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
        WheatMarketUiHelpers.styleLabel(feedbackLabel, color, Horizontal.CENTER);
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
        syncPriceFieldText(WheatMarketUiHelpers.formatMoney(unitPrice));
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
        int value = Math.max(0, WheatMarketUiHelpers.parseNonNegativeInt(field.getRawText(), fallback));
        if (amountField) {
            cooldownAmount = value;
            syncCooldownAmountText(String.valueOf(value));
        } else {
            cooldownTime = value;
            syncCooldownTimeText(String.valueOf(value));
        }
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
        playerBalanceLabel.setText(Component.translatable("gui.wheatmarket.balance", WheatMarketUiHelpers.formatMoney(seenBalance)));
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

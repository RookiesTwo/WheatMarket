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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaPositionType;
import org.w3c.dom.Document;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.WheatMarketRegistry;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.c2s.BuyItemC2SPacket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class WheatMarketOrderConfirmationUI {
    private static final ResourceLocation ORDER_XML = ResourceLocation.parse("wheatmarket:ui/order_confirmation.xml");
    private static final DateTimeFormatter LISTING_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final int INFINITE_ORDER_QUANTITY_LIMIT = 999;
    private static final int TEXT_COLOR = 0x19140D;
    private static final int WARNING_TEXT_COLOR = 0xFFFF5A1F;
    private static final int PROCESSING_TEXT_COLOR = 0x665A4D;
    private static final int SIGNATURE_TEXT_COLOR = 0x7A7468;
    private static final int FAILURE_TEXT_COLOR = 0xA33629;
    private static final int BLUE_BUTTON = 0xFF78C6EA;
    private static final int BLUE_BUTTON_HOVER = 0xFF8CD3F2;
    private static final int BLUE_BUTTON_PRESSED = 0xFF5BAFD6;
    private static final int RED_BUTTON = 0xFFE86276;
    private static final int RED_BUTTON_HOVER = 0xFFF07688;
    private static final int RED_BUTTON_PRESSED = 0xFFD84B61;
    private static final int BUTTON_BORDER = 0xFF3A332C;
    private static final int ITEM_PREVIEW_BORDER = 0xFF3A332C;
    private static final int STAMP_MAX_ALPHA = 235;
    private static final int SIGNATURE_CHAR_INTERVAL = 2;
    private static final int SIGNATURE_HOLD_TICKS = 6;
    private static final int STAMP_ANIMATION_TICKS = 10;
    private static final int SUCCESS_HOLD_TICKS = 20;
    private static final int FAILURE_PROGRESS_TICKS = 40;
    private static final float SIGNATURE_FONT_SIZE = 18.0F;
    private static final int SIGNATURE_HEIGHT = 28;
    private static final int SIGNATURE_MIN_WIDTH = 72;
    private static final int SIGNATURE_PADDING_X = 6;
    private static final int SIGNATURE_PAPER_MARGIN = 12;
    private static final int QUANTITY_FIELD_BASE_WIDTH = 22;
    private static final int QUANTITY_FIELD_DIGIT_WIDTH = 8;
    private static final int QUANTITY_FIELD_MIN_WIDTH = 22;
    private static final int QUANTITY_FIELD_MAX_WIDTH = 54;
    private static final int QUANTITY_FIELD_TRAILING_PADDING = 1;
    private static final int FAILURE_PROGRESS_TRACK_FILL = 0x333A332C;
    private static final int FAILURE_PROGRESS_FILL = 0xFFA33629;
    private static final IGuiTexture EMPTY_OVERLAY = new ColorRectTexture(0x00000000);
    private static final IGuiTexture QUANTITY_FIELD_TEXTURE = new ColorBorderTexture(1, BUTTON_BORDER);
    private static final float STAMP_SIZE_RATIO = 0.75F;
    private static final float STAMP_X_ANCHOR_RATIO = 0.82F;
    private static final float STAMP_Y_ANCHOR_RATIO = 0.72F;

    private final MarketListS2CPacket.MarketItemSummary item;
    private final ItemStack stack;
    private final Runnable onCancel;
    private final Runnable onManage;
    private final boolean playEntrySound;
    private final int initialQuantity;

    private UIElement orderPaper;
    private UIElement stampOverlay;
    private Label playerBalanceLabel;
    private Label processingLabel;
    private Label signatureLabel;
    private Label feedbackLabel;
    private UIElement errorProgressTrack;
    private UIElement errorProgressFill;
    private UIElement quantityRow;
    private Button decreaseButton;
    private Button increaseButton;
    private Button confirmButton;
    private Button manageButton;
    private Button cancelButton;
    private TextField quantityField;
    private int quantity = 1;
    private int maxQuantity = 1;
    private double seenBalance = Double.NaN;
    private AnimationState animationState = AnimationState.IDLE;
    private int animationTick;
    private int shownSignatureLength;
    private String signerName = "";
    private boolean syncingQuantityField;
    private boolean initialFocusCleared;
    private boolean hasSignatureCenter;
    private float signatureCenterX;
    private float signatureCenterY;
    private boolean ownListing;
    private boolean operator;

    public WheatMarketOrderConfirmationUI(MarketListS2CPacket.MarketItemSummary item, ItemStack stack,
                                          Runnable onCancel, Runnable onManage,
                                          boolean playEntrySound, int initialQuantity) {
        this.item = item;
        this.stack = stack.copy();
        this.stack.setCount(1);
        this.onCancel = onCancel;
        this.onManage = onManage == null ? () -> {
        } : onManage;
        this.playEntrySound = playEntrySound;
        this.initialQuantity = Math.max(1, initialQuantity);
    }

    public ModularUI create(Player player) {
        Document xml = XmlUtils.loadXml(ORDER_XML);
        if (xml == null) {
            throw new IllegalStateException("Failed to load UI xml: " + ORDER_XML);
        }

        UI loadedUi = UI.of(xml);
        UI ui = UI.of(loadedUi.getRootElement(), loadedUi.getStylesheets(), availableSize -> availableSize);
        bindAndPopulate(ui, player);
        if (playEntrySound) {
            playPaperFlippingSound();
        }
        return ModularUI.of(ui, player);
    }

    public void tick() {
        updateBalanceLabel();
        clearInitialQuantityFieldFocus();
        normalizeQuantityFieldIfBlurred();
        tickAnimation();
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean handleOperationResult(boolean success, Component message) {
        if (animationState != AnimationState.SUBMITTING) {
            return false;
        }
        if (success) {
            startSigning();
        } else {
            showFailure(message);
        }
        return true;
    }

    private void bindAndPopulate(UI ui, Player player) {
        UIElement root = require(ui, "order-root", UIElement.class);
        orderPaper = require(ui, "order-paper", UIElement.class);
        stampOverlay = require(ui, "stamp-overlay", UIElement.class);
        orderPaper.addEventListener(UIEvents.LAYOUT_CHANGED, event -> updateStampLayout());
        root.addEventListener(UIEvents.MOUSE_DOWN, event -> onRootMouseDown(event.x, event.y), true);
        UIElement playerAvatar = require(ui, "player-avatar", UIElement.class);
        UIElement itemPreview = require(ui, "item-preview", UIElement.class);
        UIElement itemIcon = require(ui, "order-item-icon", UIElement.class);
        UIElement restrictionPanel = require(ui, "restriction-panel", UIElement.class);
        quantityRow = require(ui, "quantity-row", UIElement.class);

        Label orderTitle = require(ui, "order-title", Label.class);
        Label ownerLabel = require(ui, "owner-label", Label.class);
        Label unitPriceLabel = require(ui, "unit-price-label", Label.class);
        Label itemNameLabel = require(ui, "item-name-label", Label.class);
        Label listingTimeLabel = require(ui, "listing-time-label", Label.class);
        Label stockLabel = require(ui, "stock-label", Label.class);
        Label restrictionTimeLabel = require(ui, "restriction-time-label", Label.class);
        Label restrictionAmountLabel = require(ui, "restriction-amount-label", Label.class);
        playerBalanceLabel = require(ui, "player-balance", Label.class);
        Label quantityCaption = require(ui, "quantity-caption", Label.class);
        processingLabel = require(ui, "processing-label", Label.class);
        signatureLabel = require(ui, "signature-label", Label.class);
        feedbackLabel = require(ui, "feedback-label", Label.class);
        errorProgressTrack = require(ui, "error-progress-track", UIElement.class);
        errorProgressFill = require(ui, "error-progress-fill", UIElement.class);

        decreaseButton = require(ui, "decrease-button", Button.class);
        increaseButton = require(ui, "increase-button", Button.class);
        confirmButton = require(ui, "confirm-button", Button.class);
        manageButton = require(ui, "manage-button", Button.class);
        cancelButton = require(ui, "cancel-button", Button.class);
        quantityField = require(ui, "quantity-field", TextField.class);

        root.style(style -> style.background(WheatMarketUiTextures.tradingBackgroundTexture()));
        orderPaper.style(style -> style.background(WheatMarketUiTextures.tradingPaperTexture()));
        stampOverlay.setAllowHitTest(false);
        stampOverlay.style(style -> style
                .background(IGuiTexture.dynamic(this::buildStampOverlay))
                .zIndex(20));
        playerAvatar.style(style -> style.background(WheatMarketUiTextures.playerAvatarTexture(player)));
        playerBalanceLabel.style(style -> style.background(WheatMarketUiTextures.paperTexture()));
        itemPreview.style(style -> style.background(new ColorBorderTexture(1, ITEM_PREVIEW_BORDER)));
        itemIcon.style(style -> style.background(new ItemStackTexture(stack)));
        itemPreview.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = WheatMarketItemTooltips.forStack(stack));
        signatureLabel.setAllowHitTest(false);
        signatureLabel.style(style -> style.zIndex(18));
        errorProgressTrack.setAllowHitTest(false);
        errorProgressTrack.style(style -> style.background(GuiTextureGroup.of(
                new ColorRectTexture(FAILURE_PROGRESS_TRACK_FILL),
                new ColorBorderTexture(1, BUTTON_BORDER)
        )));
        errorProgressFill.setAllowHitTest(false);
        errorProgressFill.style(style -> style.background(new ColorRectTexture(FAILURE_PROGRESS_FILL)));

        styleLabel(orderTitle, TEXT_COLOR);
        styleLabel(ownerLabel, TEXT_COLOR);
        styleLabel(unitPriceLabel, TEXT_COLOR);
        styleLabel(itemNameLabel, TEXT_COLOR);
        styleLabel(listingTimeLabel, TEXT_COLOR);
        styleLabel(stockLabel, TEXT_COLOR);
        styleLabel(restrictionTimeLabel, WARNING_TEXT_COLOR);
        styleLabel(restrictionAmountLabel, WARNING_TEXT_COLOR);
        styleLabel(playerBalanceLabel, TEXT_COLOR);
        quantityCaption.textStyle(style -> style
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(TEXT_COLOR)
                .textShadow(false));
        styleLabel(processingLabel, PROCESSING_TEXT_COLOR);
        styleLabel(feedbackLabel, FAILURE_TEXT_COLOR);
        signatureLabel.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(SIGNATURE_TEXT_COLOR)
                .textShadow(false)
                .fontSize(SIGNATURE_FONT_SIZE));

        styleActionButton(decreaseButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(increaseButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(confirmButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(manageButton, BLUE_BUTTON, BLUE_BUTTON_HOVER, BLUE_BUTTON_PRESSED);
        styleActionButton(cancelButton, RED_BUTTON, RED_BUTTON_HOVER, RED_BUTTON_PRESSED);
        confirmButton.enableText();
        manageButton.enableText();
        styleQuantityField();

        signerName = player.getName().getString();
        ownListing = isOwnListing(player);
        operator = player.hasPermissions(2);
        orderTitle.setText(Component.translatable(item.isIfSell()
                ? "gui.wheatmarket.confirm.sell_title"
                : "gui.wheatmarket.confirm.buy_title"));
        quantityCaption.setText(Component.translatable("gui.wheatmarket.confirm.quantity_label"));
        ownerLabel.setText(Component.translatable(item.isIfSell()
                ? "gui.wheatmarket.confirm.seller"
                : "gui.wheatmarket.confirm.buyer", resolveOwnerName(player)));
        unitPriceLabel.setText(Component.translatable("gui.wheatmarket.confirm.unit_price", formatMoney(item.getPrice())));
        itemNameLabel.setText(Component.translatable("gui.wheatmarket.confirm.item_name", stack.getHoverName()));
        listingTimeLabel.setText(Component.translatable("gui.wheatmarket.confirm.listing_time", formatListingTime(item.getListingTime())));
        stockLabel.setText(Component.translatable("gui.wheatmarket.confirm.stock", formatStock()));
        processingLabel.setText(Component.translatable("gui.wheatmarket.confirm.processing"));
        feedbackLabel.setText(Component.empty());
        updateConfirmButtonText();
        manageButton.setText(Component.translatable("gui.wheatmarket.confirm.manage"));
        setShown(manageButton, operator);
        setShown(quantityRow, !ownListing);

        populateRestriction(restrictionPanel, restrictionTimeLabel, restrictionAmountLabel);
        maxQuantity = calculateMaxQuantity();
        updateQuantity(initialQuantity);
        updateStampLayout();
        showIdleState();

        decreaseButton.setOnClick(event -> updateQuantity(quantity - 1));
        increaseButton.setOnClick(event -> updateQuantity(quantity + 1));
        quantityField.registerValueListener(this::onQuantityFieldChanged);
        quantityField.addEventListener(UIEvents.BLUR, event -> onQuantityFieldBlur());
        confirmButton.setOnClick(event -> {
            rememberSignatureCenter(event.x, event.y);
            submitOrder();
        });
        manageButton.setOnClick(event -> onManage.run());
        cancelButton.setOnClick(event -> onCancel.run());

        updateBalanceLabel();
        quantityField.blur();
    }

    private void populateRestriction(UIElement restrictionPanel, Label restrictionTimeLabel, Label restrictionAmountLabel) {
        int cooldownMinutes = item.getCooldownTimeInMinutes();
        int cooldownAmount = item.getCooldownAmount();
        boolean hasRestriction = cooldownMinutes > 0 || cooldownAmount > 0 || item.isHasCooldown();
        restrictionPanel.setDisplay(hasRestriction);
        restrictionPanel.setVisible(hasRestriction);
        if (!hasRestriction) {
            return;
        }

        if (cooldownMinutes > 0) {
            restrictionTimeLabel.setText(Component.translatable("gui.wheatmarket.confirm.restriction_time", cooldownMinutes));
        } else {
            restrictionTimeLabel.setText(Component.translatable("gui.wheatmarket.confirm.restriction_time_unknown"));
        }

        restrictionAmountLabel.setText(Component.translatable(
                "gui.wheatmarket.confirm.restriction_amount",
                Math.max(0, cooldownAmount)
        ));
    }

    private void submitOrder() {
        if (animationState != AnimationState.IDLE) {
            return;
        }
        if (ownListing) {
            onManage.run();
            return;
        }
        if (!item.isIfSell()) {
            showFailure(Component.translatable("gui.wheatmarket.confirm.buy_order_unsupported"));
            return;
        }

        animationState = AnimationState.SUBMITTING;
        animationTick = 0;
        feedbackLabel.setText(Component.empty());
        setShown(feedbackLabel, false);
        setShown(confirmButton, false);
        setShown(processingLabel, true);
        setShown(signatureLabel, false);
        setShown(errorProgressTrack, false);
        updateFailureProgress(0.0F);
        applyControlState();
        WheatMarketNetwork.sendToServer(new BuyItemC2SPacket(item.getMarketItemID(), quantity));
    }

    private void startSigning() {
        animationState = AnimationState.SIGNING;
        animationTick = 0;
        shownSignatureLength = 0;
        signatureLabel.setText(Component.empty());
        updateSignatureLayout();
        setShown(processingLabel, false);
        setShown(signatureLabel, true);
        playPencilWritingSound();
    }

    private void startStamping() {
        animationState = AnimationState.STAMPING;
        animationTick = 0;
    }

    private void showIdleState() {
        animationState = AnimationState.IDLE;
        animationTick = 0;
        shownSignatureLength = 0;
        setShown(confirmButton, true);
        setShown(processingLabel, false);
        setShown(signatureLabel, false);
        setShown(feedbackLabel, false);
        setShown(errorProgressTrack, false);
        updateFailureProgress(0.0F);
        applyControlState();
    }

    private void showFailure(Component message) {
        animationState = AnimationState.FAILURE;
        animationTick = 0;
        shownSignatureLength = 0;
        feedbackLabel.setText(message);
        setShown(confirmButton, false);
        setShown(processingLabel, false);
        setShown(signatureLabel, false);
        setShown(feedbackLabel, true);
        setShown(errorProgressTrack, true);
        updateFailureProgress(0.0F);
        applyControlState();
    }

    private void tickAnimation() {
        switch (animationState) {
            case SUBMITTING -> animationTick++;
            case SIGNING -> tickSigning();
            case STAMPING -> {
                animationTick++;
                if (animationTick >= STAMP_ANIMATION_TICKS) {
                    playOrderStampSound();
                    animationState = AnimationState.COMPLETED;
                    animationTick = 0;
                }
            }
            case COMPLETED -> {
                animationTick++;
                if (animationTick >= SUCCESS_HOLD_TICKS) {
                    onCancel.run();
                }
            }
            case FAILURE -> tickFailure();
            default -> {
            }
        }
    }

    private void tickFailure() {
        animationTick++;
        updateFailureProgress(Mth.clamp(animationTick / (float) FAILURE_PROGRESS_TICKS, 0.0F, 1.0F));
        if (animationTick >= FAILURE_PROGRESS_TICKS) {
            showIdleState();
        }
    }

    private void tickSigning() {
        animationTick++;
        int targetLength = Math.min(signerName.length(), animationTick / SIGNATURE_CHAR_INTERVAL);
        if (targetLength != shownSignatureLength) {
            shownSignatureLength = targetLength;
            signatureLabel.setText(Component.literal(signerName.substring(0, shownSignatureLength)));
        }
        if (shownSignatureLength >= signerName.length()
                && animationTick >= signerName.length() * SIGNATURE_CHAR_INTERVAL + SIGNATURE_HOLD_TICKS) {
            startStamping();
        }
    }

    private void updateQuantity(int newQuantity) {
        if (maxQuantity <= 0) {
            quantity = 0;
        } else {
            quantity = Mth.clamp(newQuantity, 1, maxQuantity);
        }
        syncQuantityFieldText();
        updateConfirmButtonText();
        applyControlState();
    }

    private void applyControlState() {
        boolean interactive = animationState == AnimationState.IDLE;
        int lowerBound = resolveQuantityLowerBound();
        boolean showDecrease = interactive && quantity > lowerBound;
        boolean showIncrease = interactive && quantity < maxQuantity;
        setButtonShownPreserveLayout(decreaseButton, showDecrease);
        setButtonShownPreserveLayout(increaseButton, showIncrease);
        decreaseButton.setActive(showDecrease);
        increaseButton.setActive(showIncrease);
        confirmButton.setActive(interactive && (ownListing || maxQuantity > 0));
        manageButton.setActive(interactive && operator);
        cancelButton.setActive(interactive);
        quantityField.setActive(interactive && maxQuantity >= 0);
    }

    private void updateConfirmButtonText() {
        if (confirmButton == null) {
            return;
        }
        if (ownListing) {
            confirmButton.setText(Component.translatable("gui.wheatmarket.confirm.edit_item"));
            return;
        }
        confirmButton.setText(Component.translatable("gui.wheatmarket.confirm.confirm_total", formatMoney(item.getPrice() * quantity)));
    }

    private boolean isOwnListing(Player player) {
        return !item.isIfAdmin() && item.getSellerID().equals(player.getUUID());
    }

    private IGuiTexture buildStampOverlay() {
        if (animationState != AnimationState.STAMPING && animationState != AnimationState.COMPLETED) {
            return EMPTY_OVERLAY;
        }
        float progress = animationState == AnimationState.COMPLETED
                ? 1.0F
                : Mth.clamp(animationTick / (float) STAMP_ANIMATION_TICKS, 0.0F, 1.0F);
        float easedProgress = 1.0F - (1.0F - progress) * (1.0F - progress);
        int alpha = Mth.floor(STAMP_MAX_ALPHA * easedProgress);
        float scale = Mth.lerp(easedProgress, 1.89F, 1.0F);
        float rotation = Mth.lerp(easedProgress, -18.0F, -10.0F);
        return WheatMarketUiTextures.completedStampTexture(alpha, scale, rotation);
    }

    private void updateStampLayout() {
        if (stampOverlay == null || orderPaper == null) {
            return;
        }

        float paperWidth = orderPaper.getSizeWidth();
        float paperHeight = orderPaper.getSizeHeight();
        if (paperWidth <= 0 || paperHeight <= 0) {
            return;
        }

        float size = Math.min(paperWidth, paperHeight) * STAMP_SIZE_RATIO;
        float availableX = Math.max(0.0F, paperWidth - size);
        float availableY = Math.max(0.0F, paperHeight - size);
        float stampX = orderPaper.getPositionX() + availableX * STAMP_X_ANCHOR_RATIO;
        float stampY = orderPaper.getPositionY() + availableY * STAMP_Y_ANCHOR_RATIO;

        stampOverlay.layout(layout -> layout
                .positionType(YogaPositionType.ABSOLUTE)
                .left(stampX)
                .top(stampY)
                .width(size)
                .height(size));
    }

    private int calculateMaxQuantity() {
        int stockLimit = item.isIfInfinite()
                ? INFINITE_ORDER_QUANTITY_LIMIT
                : Math.max(1, item.getAmount());
        if (item.getCooldownAmount() > 0) {
            stockLimit = Math.min(stockLimit, item.getCooldownAmount());
        }
        if (item.isHasCooldown() && item.getCooldownAmount() <= 0) {
            return 0;
        }
        return Math.max(0, stockLimit);
    }

    private void styleQuantityField() {
        quantityField.setAnyString();
        quantityField.setCharValidator(Character::isDigit);
        quantityField.setTextValidator(text -> text.isBlank() || text.chars().allMatch(Character::isDigit));
        quantityField.style(style -> style.background(IGuiTexture.dynamic(this::quantityFieldBackgroundTexture)));
        quantityField.textFieldStyle(style -> style
                .placeholder(Component.translatable("gui.wheatmarket.confirm.empty"))
                .textColor(TEXT_COLOR)
                .cursorColor(0xFF19140D)
                .errorColor(FAILURE_TEXT_COLOR)
                .textShadow(false)
                .focusOverlay(EMPTY_OVERLAY));
    }

    private void onQuantityFieldChanged(String rawValue) {
        if (syncingQuantityField) {
            return;
        }
        if (rawValue == null || rawValue.isBlank()) {
            updateQuantityFieldWidth(rawValue);
            return;
        }
        updateQuantity(parseQuantityValue(rawValue));
    }

    private void onQuantityFieldBlur() {
        if (quantityField.getRawText().isBlank()) {
            updateQuantity(resolveQuantityLowerBound());
        }
    }

    private void normalizeQuantityFieldIfBlurred() {
        if (quantityField != null && !quantityField.isFocused() && quantityField.getRawText().isBlank()) {
            updateQuantity(resolveQuantityLowerBound());
        }
    }

    private void clearInitialQuantityFieldFocus() {
        if (!initialFocusCleared && quantityField != null) {
            quantityField.blur();
            initialFocusCleared = true;
        }
    }

    private void onRootMouseDown(float mouseX, float mouseY) {
        if (quantityField != null && !quantityField.isMouseOver(mouseX, mouseY)) {
            quantityField.blur();
            if (quantityField.getRawText().isBlank()) {
                updateQuantity(resolveQuantityLowerBound());
            }
        }
    }

    private int parseQuantityValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return resolveQuantityLowerBound();
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ignored) {
            return resolveQuantityLowerBound();
        }
    }

    private int resolveQuantityLowerBound() {
        return maxQuantity <= 0 ? 0 : 1;
    }

    private void syncQuantityFieldText() {
        String quantityText = String.valueOf(quantity);
        if (quantityField != null && !quantityText.equals(quantityField.getRawText())) {
            syncingQuantityField = true;
            quantityField.setValue(quantityText, false);
            syncingQuantityField = false;
        }
        updateQuantityFieldWidth(quantityText);
    }

    private IGuiTexture quantityFieldBackgroundTexture() {
        return animationState == AnimationState.IDLE ? QUANTITY_FIELD_TEXTURE : EMPTY_OVERLAY;
    }

    private void updateQuantityFieldWidth(String text) {
        int digitCount = text == null || text.isBlank() ? 1 : text.length();
        int width = QUANTITY_FIELD_BASE_WIDTH + digitCount * QUANTITY_FIELD_DIGIT_WIDTH;
        width = Mth.clamp(width, QUANTITY_FIELD_MIN_WIDTH, QUANTITY_FIELD_MAX_WIDTH);
        int textWidth = measureQuantityTextWidth(text);
        int leftPadding = Math.max(1, (width - textWidth) / 2);
        int finalWidth = width;
        quantityField.layout(layout -> layout
                .width(finalWidth)
                .paddingLeft(leftPadding)
                .paddingRight(QUANTITY_FIELD_TRAILING_PADDING));
    }

    private void updateFailureProgress(float progress) {
        if (errorProgressFill == null || errorProgressTrack == null) {
            return;
        }
        float width = errorProgressTrack.getSizeWidth() * progress;
        errorProgressFill.layout(layout -> layout.width(width));
    }

    private void rememberSignatureCenter(float mouseX, float mouseY) {
        hasSignatureCenter = true;
        signatureCenterX = mouseX;
        signatureCenterY = mouseY;
    }

    private void updateSignatureLayout() {
        if (signatureLabel == null || orderPaper == null || (!hasSignatureCenter && confirmButton == null)) {
            return;
        }

        float paperWidth = orderPaper.getSizeWidth();
        float paperHeight = orderPaper.getSizeHeight();
        if (paperWidth <= 0 || paperHeight <= 0) {
            return;
        }

        float paperLeft = orderPaper.getPositionX() + SIGNATURE_PAPER_MARGIN;
        float paperTop = orderPaper.getPositionY() + SIGNATURE_PAPER_MARGIN;
        float paperRight = orderPaper.getPositionX() + paperWidth - SIGNATURE_PAPER_MARGIN;
        float paperBottom = orderPaper.getPositionY() + paperHeight - SIGNATURE_PAPER_MARGIN;
        float availableWidth = Math.max(SIGNATURE_MIN_WIDTH, paperRight - paperLeft);
        float signatureWidth = Mth.clamp(measureSignatureWidth(), SIGNATURE_MIN_WIDTH, availableWidth);

        float centerX = hasSignatureCenter
                ? signatureCenterX
                : confirmButton.getPositionX() + confirmButton.getSizeWidth() / 2.0F;
        float centerY = hasSignatureCenter
                ? signatureCenterY
                : confirmButton.getPositionY() + confirmButton.getSizeHeight() / 2.0F;

        float maxX = Math.max(paperLeft, paperRight - signatureWidth);
        float signatureX = Mth.clamp(centerX - signatureWidth / 2.0F, paperLeft, maxX);
        float maxY = Math.max(paperTop, paperBottom - SIGNATURE_HEIGHT);
        float signatureY = Mth.clamp(centerY - SIGNATURE_HEIGHT / 2.0F, paperTop, maxY);

        signatureLabel.layout(layout -> layout
                .positionType(YogaPositionType.ABSOLUTE)
                .left(signatureX)
                .top(signatureY)
                .width(signatureWidth)
                .height(SIGNATURE_HEIGHT)
                .paddingLeft(SIGNATURE_PADDING_X)
                .paddingRight(SIGNATURE_PADDING_X));
    }

    private int measureSignatureWidth() {
        if (signerName.isBlank()) {
            return SIGNATURE_MIN_WIDTH;
        }
        float fontScale = SIGNATURE_FONT_SIZE / 9.0F;
        return Math.round(Minecraft.getInstance().font.width(signerName) * fontScale) + SIGNATURE_PADDING_X * 2;
    }

    private int measureQuantityTextWidth(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        float fontScale = quantityField.getTextFieldStyle().fontSize() / 9.0F;
        return Math.round(Minecraft.getInstance().font.width(text) * fontScale);
    }

    private Component resolveOwnerName(Player player) {
        if (item.isIfAdmin()) {
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

    private Component formatStock() {
        if (item.isIfInfinite()) {
            return Component.translatable("gui.wheatmarket.market.infinite");
        }
        return Component.literal(String.valueOf(item.getAmount()));
    }

    private Component formatListingTime(long listingTime) {
        if (listingTime <= 0) {
            return Component.translatable("gui.wheatmarket.confirm.unknown_time");
        }
        return Component.literal(Instant.ofEpochMilli(listingTime)
                .atZone(ZoneId.systemDefault())
                .format(LISTING_TIME_FORMAT));
    }

    private void updateBalanceLabel() {
        if (Double.compare(seenBalance, WheatMarket.CLIENT_BALANCE) == 0) {
            return;
        }
        seenBalance = WheatMarket.CLIENT_BALANCE;
        playerBalanceLabel.setText(Component.translatable("gui.wheatmarket.balance", formatMoney(seenBalance)));
    }

    private void playPaperFlippingSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || WheatMarketRegistry.PAPER_FLIPPING == null) {
            return;
        }
        float pitch = 0.9F + minecraft.player.getRandom().nextFloat() * 0.2F;
        minecraft.player.playSound(WheatMarketRegistry.PAPER_FLIPPING.get(), 1.0F, pitch);
    }

    private void playPencilWritingSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || WheatMarketRegistry.PENCIL_WRITING == null) {
            return;
        }
        minecraft.player.playSound(WheatMarketRegistry.PENCIL_WRITING.get(), 1.35F, 1.0F);
    }

    private void playOrderStampSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || WheatMarketRegistry.ORDER_STAMP == null) {
            return;
        }
        minecraft.player.playSound(WheatMarketRegistry.ORDER_STAMP.get(), 1.0F, 1.0F);
    }

    private void styleLabel(Label label, int color) {
        label.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
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

    private void setButtonShownPreserveLayout(UIElement element, boolean shown) {
        element.setDisplay(true);
        element.setVisible(shown);
        element.setAllowHitTest(shown);
    }

    private <T> T require(UI ui, String id, Class<T> type) {
        return ui.selectId(id, type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing UI element: " + id));
    }

    private String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private enum AnimationState {
        IDLE,
        SUBMITTING,
        SIGNING,
        STAMPING,
        COMPLETED,
        FAILURE
    }
}

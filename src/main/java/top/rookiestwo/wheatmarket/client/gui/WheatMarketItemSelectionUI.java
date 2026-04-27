package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaPositionType;
import org.w3c.dom.Document;
import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class WheatMarketItemSelectionUI {
    private static final ResourceLocation ITEM_SELECTION_XML = ResourceLocation.parse("wheatmarket:ui/item_selection.xml");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 174;
    private static final int TEXT_COLOR = 0xFFE8D8B8;
    private static final int MUTED_TEXT_COLOR = 0xFFBCA985;
    private static final int SLOT_BACKGROUND = 0xCCF3E0B8;
    private static final int SLOT_BORDER = 0xFF3A332C;
    private static final int DIM_OVERLAY = 0x99000000;
    private static final int SLOT_SIZE = 18;
    private static final IGuiTexture SLOT_BACKGROUND_TEXTURE = GuiTextureGroup.of(
            new ColorRectTexture(SLOT_BACKGROUND),
            new ColorBorderTexture(1, SLOT_BORDER)
    );

    private final WheatMarketMenu menu;
    private final int panelLeft;
    private final int panelTop;
    private final ItemSelectionRequest request;
    private final Runnable confirmHandler;
    private final Runnable backHandler;

    private UIElement rootElement;
    private UIElement dimOverlay;
    private UIElement panelElement;
    private UIElement slotBackgroundLayer;
    private Button confirmButton;
    private Label modeLabel;
    private Label restrictionLabel;
    private Label selectedItemLabel;
    private ItemSelectionMode seenMode;
    private ItemStack seenSelectedItem = ItemStack.EMPTY;
    private int seenSelectedAmount = -1;

    public WheatMarketItemSelectionUI(WheatMarketMenu menu,
                                      int panelLeft,
                                      int panelTop,
                                      ItemSelectionRequest request,
                                      Runnable confirmHandler,
                                      Runnable backHandler) {
        this.menu = menu;
        this.panelLeft = panelLeft;
        this.panelTop = panelTop;
        this.request = request;
        this.confirmHandler = confirmHandler;
        this.backHandler = backHandler;
    }

    public ModularUI create(Player player) {
        Document xml = XmlUtils.loadXml(ITEM_SELECTION_XML);
        if (xml == null) {
            throw new IllegalStateException("Failed to load UI xml: " + ITEM_SELECTION_XML);
        }

        UI loadedUi = UI.of(xml);
        UI ui = UI.of(loadedUi.getRootElement(), loadedUi.getStylesheets(), availableSize -> availableSize);
        bindStaticElements(ui);
        applyTextures();
        applyLogic();
        return ModularUI.of(ui, player);
    }

    public void tick() {
        refreshConfirmState();
        refreshSelectedItemText();
    }

    public void refreshConfirmState() {
        if (confirmButton != null) {
            confirmButton.setActive(request.allowEmpty() || menu.hasSelectedItem());
        }
    }

    private void bindStaticElements(UI ui) {
        rootElement = require(ui, "item-selection-root", UIElement.class);
        dimOverlay = require(ui, "dim-overlay", UIElement.class);
        panelElement = require(ui, "item-selection-panel", UIElement.class);
        slotBackgroundLayer = require(ui, "slot-background-layer", UIElement.class);
        panelElement.layout(layout -> layout
                .left(panelLeft)
                .top(panelTop)
                .width(GUI_WIDTH)
                .height(GUI_HEIGHT));

        Label titleLabel = require(ui, "title-label", Label.class);
        modeLabel = require(ui, "mode-label", Label.class);
        Label selectionSlotLabel = require(ui, "selection-slot-label", Label.class);
        restrictionLabel = require(ui, "restriction-label", Label.class);
        Label inventoryLabel = require(ui, "inventory-label", Label.class);
        selectedItemLabel = require(ui, "selected-item-label", Label.class);

        confirmButton = require(ui, "confirm-button", Button.class);
        Button backButton = require(ui, "back-button", Button.class);

        titleLabel.setText(Component.translatable("gui.wheatmarket.item_selection.title"));
        modeLabel.setText(modeText(request.mode()));
        selectionSlotLabel.setText(Component.translatable("gui.wheatmarket.item_selection.selection_slot"));
        restrictionLabel.setText(restrictionText());
        inventoryLabel.setText(Component.translatable("container.inventory"));

        styleLabel(titleLabel, TEXT_COLOR);
        styleLabel(modeLabel, MUTED_TEXT_COLOR);
        styleLabel(selectionSlotLabel, MUTED_TEXT_COLOR);
        styleLabel(restrictionLabel, MUTED_TEXT_COLOR);
        styleLabel(inventoryLabel, MUTED_TEXT_COLOR);
        styleLabel(selectedItemLabel, TEXT_COLOR);
        styleButton(confirmButton);
        styleButton(backButton);

        confirmButton.setOnClick(event -> confirmHandler.run());
        backButton.setOnClick(event -> backHandler.run());
    }

    private void applyTextures() {
        dimOverlay.setAllowHitTest(false);
        dimOverlay.style(style -> style.background(new ColorRectTexture(DIM_OVERLAY)));
        panelElement.style(style -> style.background(WheatMarketUiTextures.panelTexture()));
        slotBackgroundLayer.setAllowHitTest(false);
        installSlotBackground(WheatMarketMenu.ITEM_SELECTION_SLOT_X, WheatMarketMenu.ITEM_SELECTION_SLOT_Y);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                installSlotBackground(
                        WheatMarketMenu.PLAYER_INVENTORY_X + column * 18,
                        WheatMarketMenu.PLAYER_INVENTORY_Y + row * 18
                );
            }
        }

        for (int column = 0; column < 9; column++) {
            installSlotBackground(WheatMarketMenu.PLAYER_INVENTORY_X + column * 18, WheatMarketMenu.HOTBAR_Y);
        }
    }

    private void applyLogic() {
        seenMode = null;
        seenSelectedAmount = -1;
        seenSelectedItem = ItemStack.EMPTY;
        refreshConfirmState();
        refreshSelectedItemText();
    }

    private void installSlotBackground(int slotX, int slotY) {
        UIElement background = new UIElement()
                .layout(layout -> layout
                        .positionType(YogaPositionType.ABSOLUTE)
                        .left(slotX - 1)
                        .top(slotY - 1)
                        .width(SLOT_SIZE)
                        .height(SLOT_SIZE))
                .style(style -> style.background(SLOT_BACKGROUND_TEXTURE));
        background.setAllowHitTest(false);
        slotBackgroundLayer.addChild(background);
    }

    private void refreshSelectedItemText() {
        ItemStack selected = menu.getSelectedItem();
        int selectedAmount = menu.getSelectedAmount();
        ItemSelectionMode selectedMode = request.mode();
        if (ItemStack.matches(seenSelectedItem, selected)
                && seenSelectedAmount == selectedAmount
                && seenMode == selectedMode) {
            return;
        }

        seenSelectedItem = selected.copy();
        seenSelectedAmount = selectedAmount;
        seenMode = selectedMode;
        selectedItemLabel.setText(selectedItemText(selected, selectedAmount, selectedMode));
        refreshConfirmState();
    }

    private Component selectedItemText(ItemStack selected, int selectedAmount, ItemSelectionMode selectedMode) {
        if (selected.isEmpty()) {
            return Component.translatable("gui.wheatmarket.item_selection.empty");
        }
        if (selectedMode == ItemSelectionMode.SAMPLE) {
            return Component.translatable("gui.wheatmarket.item_selection.sampled", selected.getHoverName());
        }
        return Component.translatable("gui.wheatmarket.item_selection.selected_amount", selected.getHoverName(), selectedAmount);
    }

    private Component modeText(ItemSelectionMode mode) {
        return Component.translatable(mode == ItemSelectionMode.SAMPLE
                ? "gui.wheatmarket.item_selection.mode_sample"
                : "gui.wheatmarket.item_selection.mode_transfer");
    }

    private Component restrictionText() {
        if (request.hasLockedStackTemplate()) {
            return Component.translatable("gui.wheatmarket.item_selection.locked", request.lockedStackTemplate().getHoverName());
        }
        return Component.translatable("gui.wheatmarket.item_selection.unlocked");
    }

    private void styleLabel(Label label, int color) {
        label.textStyle(style -> style
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(color)
                .textShadow(false));
    }

    private void styleButton(Button button) {
        button.buttonStyle(style -> style
                .baseTexture(WheatMarketUiTextures.buttonBaseTexture())
                .hoverTexture(WheatMarketUiTextures.buttonPressedTexture())
                .pressedTexture(WheatMarketUiTextures.buttonPressedTexture()));
        button.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(0xFF2B2116)
                .textShadow(false));
    }

    private <T> T require(UI ui, String id, Class<T> type) {
        return ui.selectId(id, type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing UI element: " + id));
    }
}

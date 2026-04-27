package top.rookiestwo.wheatmarket.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import top.rookiestwo.wheatmarket.menu.ItemSelectionMode;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.c2s.SetItemSelectionModeC2SPacket;

public class WheatMarketItemSelectionScreen extends AbstractContainerScreen<WheatMarketMenu> {
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 174;
    private static final int PANEL_BACKGROUND = 0xEE2B2116;
    private static final int PANEL_BORDER = 0xFF7A5532;
    private static final int SLOT_BACKGROUND = 0xCCF3E0B8;
    private static final int SLOT_BORDER = 0xFF3A332C;
    private static final int TEXT_COLOR = 0xFFE8D8B8;
    private static final int MUTED_TEXT_COLOR = 0xFFBCA985;
    private static final int SLOT_SIZE = 18;

    private final Inventory inventory;
    private ItemSelectionMode selectedMode;
    private Button transferModeButton;
    private Button sampleModeButton;
    private boolean deactivated;

    public WheatMarketItemSelectionScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                          ItemSelectionMode initialMode) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.selectedMode = initialMode == null ? ItemSelectionMode.TRANSFER : initialMode;
    }

    @Override
    protected void init() {
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        super.init();

        setSelectionMode(selectedMode);
        int buttonY = this.topPos + 16;
        this.transferModeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.wheatmarket.item_selection.transfer_mode"),
                        button -> setSelectionMode(ItemSelectionMode.TRANSFER))
                .bounds(this.leftPos + 8, buttonY, 54, 20)
                .build());
        this.sampleModeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.wheatmarket.item_selection.sample_mode"),
                        button -> setSelectionMode(ItemSelectionMode.SAMPLE))
                .bounds(this.leftPos + 64, buttonY, 54, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.wheatmarket.item_selection.back"),
                        button -> returnToMain())
                .bounds(this.leftPos + 120, buttonY, 48, 20)
                .build());
        updateModeButtons();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(this.leftPos - 2, this.topPos - 2, this.leftPos + this.imageWidth + 2,
                this.topPos + this.imageHeight + 2, PANEL_BORDER);
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight, PANEL_BACKGROUND);
        renderSlotBackground(guiGraphics, WheatMarketMenu.ITEM_SELECTION_SLOT_X, WheatMarketMenu.ITEM_SELECTION_SLOT_Y);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotBackground(guiGraphics,
                        WheatMarketMenu.PLAYER_INVENTORY_X + column * 18,
                        WheatMarketMenu.PLAYER_INVENTORY_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotBackground(guiGraphics,
                    WheatMarketMenu.PLAYER_INVENTORY_X + column * 18,
                    WheatMarketMenu.HOTBAR_Y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("gui.wheatmarket.item_selection.title"),
                8, 5, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.wheatmarket.item_selection.selection_slot"),
                8, 40, MUTED_TEXT_COLOR, false);
        guiGraphics.drawString(this.font, selectedItemText(), 8, 62, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"),
                8, 73, MUTED_TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            returnToMain();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        deactivateSelectionMode();
        super.onClose();
    }

    @Override
    public void removed() {
        deactivateSelectionMode();
        super.removed();
    }

    private void setSelectionMode(ItemSelectionMode mode) {
        selectedMode = mode == null ? ItemSelectionMode.TRANSFER : mode;
        deactivated = false;
        this.menu.setItemSelectionMode(selectedMode, this.inventory.player);
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(selectedMode));
        updateModeButtons();
    }

    private void deactivateSelectionMode() {
        if (deactivated) {
            return;
        }
        deactivated = true;
        this.menu.setItemSelectionMode(ItemSelectionMode.DISABLED, this.inventory.player);
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(ItemSelectionMode.DISABLED));
    }

    private void updateModeButtons() {
        if (transferModeButton != null) {
            transferModeButton.active = selectedMode != ItemSelectionMode.TRANSFER;
        }
        if (sampleModeButton != null) {
            sampleModeButton.active = selectedMode != ItemSelectionMode.SAMPLE;
        }
    }

    private void returnToMain() {
        deactivateSelectionMode();
        if (this.minecraft != null) {
            this.minecraft.setScreen(new WheatMarketMainScreen(this.menu, this.inventory, this.title));
        }
    }

    private Component selectedItemText() {
        ItemStack selected = this.menu.getSelectedItem();
        if (selected.isEmpty()) {
            return Component.translatable("gui.wheatmarket.item_selection.empty");
        }
        if (selectedMode == ItemSelectionMode.SAMPLE) {
            return Component.translatable("gui.wheatmarket.item_selection.sampled", selected.getHoverName());
        }
        return Component.translatable("gui.wheatmarket.item_selection.selected_amount",
                selected.getHoverName(), this.menu.getSelectedAmount());
    }

    private void renderSlotBackground(GuiGraphics guiGraphics, int x, int y) {
        int left = this.leftPos + x - 1;
        int top = this.topPos + y - 1;
        guiGraphics.fill(left, top, left + SLOT_SIZE, top + SLOT_SIZE, SLOT_BORDER);
        guiGraphics.fill(left + 1, top + 1, left + SLOT_SIZE - 1, top + SLOT_SIZE - 1, SLOT_BACKGROUND);
    }
}

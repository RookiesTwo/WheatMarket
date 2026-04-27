package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
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

    private final Inventory inventory;
    private final ItemSelectionRequest request;
    private final ItemSelectionMode selectedMode;
    private final ModularUI parentBackgroundModularUI;
    private final ParentScreenFactory parentScreenFactory;
    private WheatMarketItemSelectionUI itemSelectionUI;
    private ModularUI itemSelectionModularUI;
    private boolean deactivated;
    private boolean configuredSelection;

    public WheatMarketItemSelectionScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                          ItemSelectionRequest request,
                                          ModularUI parentBackgroundModularUI,
                                          ParentScreenFactory parentScreenFactory) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.request = request == null ? ItemSelectionRequest.listSell(null) : request;
        this.selectedMode = this.request.mode();
        this.parentBackgroundModularUI = parentBackgroundModularUI;
        this.parentScreenFactory = parentScreenFactory;
    }

    @Override
    protected void init() {
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        super.init();

        if (!configuredSelection) {
            configureSelection();
            configuredSelection = true;
        }
        installParentBackgroundModularUI();

        this.itemSelectionUI = new WheatMarketItemSelectionUI(
                this.menu,
                this.leftPos,
                this.topPos,
                this.request,
                this::confirmSelection,
                () -> returnToParent(false)
        );
        installItemSelectionModularUI(this.itemSelectionUI.create(this.inventory.player));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.itemSelectionUI != null) {
            this.itemSelectionUI.tick();
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirmSelection();
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

    private void configureSelection() {
        deactivated = false;
        this.menu.configureItemSelection(selectedMode, this.request.initialSelection(), this.request.lockedStackTemplate(), this.inventory.player);
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(
                selectedMode,
                stackToNbt(this.request.initialSelection()),
                stackAmount(this.request.initialSelection()),
                stackToNbt(this.request.lockedStackTemplate())
        ));
    }

    private void confirmSelection() {
        if (deactivated) {
            return;
        }
        if (!this.request.allowEmpty() && !this.menu.hasSelectedItem()) {
            return;
        }
        this.request.onConfirm().accept(selectionResult());
        returnToParent(shouldPreserveConfirmedSelection());
    }

    private ItemSelectionResult selectionResult() {
        int totalAmount = this.menu.getSelectedAmount();
        int baselineAmount = this.request.baselineAmount();
        return new ItemSelectionResult(
                this.request.purpose(),
                this.selectedMode,
                this.menu.getSelectedItem(),
                totalAmount,
                baselineAmount,
                totalAmount - baselineAmount,
                totalAmount == 0
        );
    }

    private CompoundTag stackToNbt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ItemStack template = stack.copy();
        template.setCount(1);
        return (CompoundTag) template.save(this.inventory.player.level().registryAccess());
    }

    private int stackAmount(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0 : stack.getCount();
    }

    private boolean shouldPreserveConfirmedSelection() {
        return this.request.purpose() == ItemSelectionPurpose.LIST_SELL
                && this.selectedMode == ItemSelectionMode.TRANSFER
                && this.menu.hasSelectedItem();
    }

    private void returnToParent(boolean preserveSelection) {
        deactivateSelectionMode(preserveSelection);
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreenFactory.create(this.menu, this.inventory, this.title));
        }
    }

    private void deactivateSelectionMode() {
        deactivateSelectionMode(false);
    }

    private void deactivateSelectionMode(boolean preserveSelection) {
        if (deactivated) {
            return;
        }
        deactivated = true;
        if (preserveSelection) {
            this.menu.preserveItemSelectionForDraft(this.inventory.player);
        } else {
            this.menu.setItemSelectionMode(ItemSelectionMode.DISABLED, this.inventory.player);
        }
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(ItemSelectionMode.DISABLED, preserveSelection));
    }

    private void installParentBackgroundModularUI() {
        if (this.parentBackgroundModularUI == null) {
            return;
        }
        this.parentBackgroundModularUI.clearFocus();
        this.parentBackgroundModularUI.setDrawDrag(false);
        this.parentBackgroundModularUI.setDrawTooltips(false);
        this.parentBackgroundModularUI.getAllElements()
                .forEach(element -> element.setAllowHitTest(false));
        this.parentBackgroundModularUI.setScreenAndInit(this);
        this.addRenderableOnly(this.parentBackgroundModularUI.getWidget());
    }

    private void installItemSelectionModularUI(ModularUI modularUI) {
        this.itemSelectionModularUI = modularUI;
        installModularUI(this.itemSelectionModularUI);
    }

    private void installModularUI(ModularUI modularUI) {
        modularUI.setMenu(this.menu);
        modularUI.setScreenAndInit(this);
        this.addRenderableWidget(modularUI.getWidget());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @FunctionalInterface
    public interface ParentScreenFactory {
        Screen create(WheatMarketMenu menu, Inventory inventory, Component title);
    }
}

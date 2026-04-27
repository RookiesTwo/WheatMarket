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
import top.rookiestwo.wheatmarket.network.c2s.BeginStockEditC2SPacket;
import top.rookiestwo.wheatmarket.network.c2s.FinalizeStockEditC2SPacket;
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
    private boolean beginFailed;
    private boolean stockEditFinalizing;
    private boolean stockEditFinalized;
    private int submittedFinalStock;

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
                this::handleBackAction
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
        if (isStockEdit()) {
            finishStockEdit();
            return;
        }
        deactivateSelectionMode();
        super.onClose();
    }

    @Override
    public void removed() {
        if (isStockEdit()) {
            if (!stockEditFinalized && !stockEditFinalizing && configuredSelection) {
                finishStockEdit();
            }
            super.removed();
            return;
        }
        deactivateSelectionMode();
        super.removed();
    }

    private void configureSelection() {
        deactivated = false;
        if (isStockEdit()) {
            this.menu.prepareClientStockEdit(
                    this.request.marketItemId(),
                    this.request.lockedStackTemplate(),
                    stackAmount(this.request.initialSelection()),
                    this.inventory.player
            );
            WheatMarketNetwork.sendToServer(new BeginStockEditC2SPacket(this.request.marketItemId()));
            return;
        }
        this.menu.configureItemSelection(selectedMode, clientInitialSelection(), this.request.lockedStackTemplate(), this.inventory.player);
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(
                selectedMode,
                stackToNbt(this.request.initialSelection()),
                stackAmount(this.request.initialSelection()),
                stackToNbt(this.request.lockedStackTemplate())
        ));
    }

    private ItemStack clientInitialSelection() {
        return this.request.initialSelection();
    }

    private void confirmSelection() {
        if (isStockEdit()) {
            finishStockEdit();
            return;
        }
        if (deactivated) {
            return;
        }
        if (!this.request.allowEmpty() && !this.menu.hasSelectedItem()) {
            return;
        }
        this.request.onConfirm().accept(selectionResult());
        returnToParent(shouldKeepConfirmedSelection());
    }

    private void handleBackAction() {
        if (isStockEdit()) {
            finishStockEdit();
            return;
        }
        returnToParent(false);
    }

    public boolean handleOperationResult(boolean success, Component message) {
        if (!isStockEdit()) {
            return false;
        }
        if (!stockEditFinalizing) {
            if (!success && this.itemSelectionUI != null) {
                beginFailed = true;
                this.itemSelectionUI.showFailure(message);
                return true;
            }
            return false;
        }

        stockEditFinalizing = false;
        if (!success) {
            if (this.itemSelectionUI != null) {
                this.itemSelectionUI.setFinalizing(false);
                this.itemSelectionUI.showFailure(message);
            }
            return true;
        }

        stockEditFinalized = true;
        this.request.onConfirm().accept(submittedStockEditResult());
        if (this.menu.isStockEditActive()) {
            this.menu.completeStockEditFinalization(true);
        }
        deactivated = true;
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreenFactory.create(this.menu, this.inventory, this.title));
        }
        return true;
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

    private ItemSelectionResult submittedStockEditResult() {
        ItemStack selectedStack = ItemStack.EMPTY;
        if (submittedFinalStock > 0 && !this.request.lockedStackTemplate().isEmpty()) {
            selectedStack = this.request.lockedStackTemplate().copy();
            selectedStack.setCount(submittedFinalStock);
        }
        return new ItemSelectionResult(
                this.request.purpose(),
                this.selectedMode,
                selectedStack,
                submittedFinalStock,
                this.request.baselineAmount(),
                submittedFinalStock - this.request.baselineAmount(),
                submittedFinalStock <= 0
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

    private boolean shouldKeepConfirmedSelection() {
        return (this.request.purpose() == ItemSelectionPurpose.LIST_SELL
                || this.request.purpose() == ItemSelectionPurpose.LIST_BUY)
                && this.menu.hasSelectedItem();
    }

    private boolean isStockEdit() {
        return this.request.purpose() == ItemSelectionPurpose.STOCK_EDIT;
    }

    private void finishStockEdit() {
        if (stockEditFinalized || stockEditFinalizing) {
            return;
        }
        if (beginFailed) {
            stockEditFinalized = true;
            if (this.menu.isStockEditActive()) {
                this.menu.completeStockEditFinalization(true);
            }
            deactivated = true;
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreenFactory.create(this.menu, this.inventory, this.title));
            }
            return;
        }

        stockEditFinalizing = true;
        submittedFinalStock = this.menu.getStockEditAmount();
        if (this.itemSelectionUI != null) {
            this.itemSelectionUI.setFinalizing(true);
        }
        WheatMarketNetwork.sendToServer(new FinalizeStockEditC2SPacket());
    }

    private void returnToParent(boolean keepSelection) {
        deactivateSelectionMode(keepSelection);
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreenFactory.create(this.menu, this.inventory, this.title));
        }
    }

    private void deactivateSelectionMode() {
        deactivateSelectionMode(false);
    }

    private void deactivateSelectionMode(boolean keepSelection) {
        if (deactivated) {
            return;
        }
        if (isStockEdit()) {
            finishStockEdit();
            return;
        }
        deactivated = true;
        if (keepSelection) {
            this.menu.deactivateItemSelection(this.inventory.player);
        } else {
            this.menu.setItemSelectionMode(ItemSelectionMode.DISABLED, this.inventory.player);
        }
        WheatMarketNetwork.sendToServer(new SetItemSelectionModeC2SPacket(ItemSelectionMode.DISABLED, keepSelection));
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

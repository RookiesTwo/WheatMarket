package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;
import top.rookiestwo.wheatmarket.network.WheatMarketNetwork;
import top.rookiestwo.wheatmarket.network.c2s.BeginStockEditC2SPacket;
import top.rookiestwo.wheatmarket.network.c2s.FinalizeStockEditC2SPacket;
import top.rookiestwo.wheatmarket.network.s2c.MarketListS2CPacket;

public class WheatMarketStockEditScreen extends AbstractContainerScreen<WheatMarketMenu> {
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 174;

    private final Inventory inventory;
    private final MarketListS2CPacket.MarketItemSummary item;
    private final ItemStack stack;
    private final WheatMarketItemEditUI.Draft draft;
    private final ModularUI parentBackgroundModularUI;
    private WheatMarketStockEditUI stockEditUI;
    private ModularUI stockEditModularUI;
    private boolean beginSent;
    private boolean beginFailed;
    private boolean finalizing;
    private boolean finalized;
    private int submittedFinalStock;

    public WheatMarketStockEditScreen(WheatMarketMenu menu, Inventory inventory, Component title,
                                      MarketListS2CPacket.MarketItemSummary item, ItemStack stack,
                                      WheatMarketItemEditUI.Draft draft,
                                      ModularUI parentBackgroundModularUI) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.item = item;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.draft = draft == null ? WheatMarketItemEditUI.Draft.from(item) : draft;
        this.parentBackgroundModularUI = parentBackgroundModularUI;
    }

    @Override
    protected void init() {
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        super.init();

        if (!beginSent) {
            beginStockEdit();
            beginSent = true;
        }
        installParentBackgroundModularUI();

        this.stockEditUI = new WheatMarketStockEditUI(
                this.menu,
                this.leftPos,
                this.topPos,
                lockedStackTemplate(),
                this::finishStockEdit
        );
        installStockEditModularUI(this.stockEditUI.create(this.inventory.player));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.stockEditUI != null) {
            this.stockEditUI.tick();
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            finishStockEdit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        finishStockEdit();
    }

    @Override
    public void removed() {
        if (!finalized && !finalizing && beginSent) {
            finishStockEdit();
        }
        super.removed();
    }

    public boolean handleOperationResult(boolean success, Component message) {
        if (!finalizing) {
            if (!success && this.stockEditUI != null) {
                beginFailed = true;
                this.stockEditUI.showFailure(message);
                return true;
            }
            return false;
        }

        finalizing = false;
        if (!success) {
            if (this.stockEditUI != null) {
                this.stockEditUI.setFinalizing(false);
                this.stockEditUI.showFailure(message);
            }
            return true;
        }

        finalized = true;
        if (this.menu.isStockEditActive()) {
            this.menu.completeStockEditFinalization(true);
        }
        if (this.minecraft != null) {
            if (submittedFinalStock <= 0) {
                this.minecraft.setScreen(new WheatMarketMainScreen(this.menu, this.inventory, this.title));
            } else {
                this.minecraft.setScreen(new WheatMarketItemEditScreen(
                        this.menu,
                        this.inventory,
                        this.title,
                        this.item,
                        this.stack,
                        this.draft.withStock(submittedFinalStock)
                ));
            }
        }
        return true;
    }

    private void beginStockEdit() {
        this.menu.prepareClientStockEdit(this.item.getMarketItemID(), lockedStackTemplate(), this.inventory.player);
        WheatMarketNetwork.sendToServer(new BeginStockEditC2SPacket(this.item.getMarketItemID()));
    }

    private void finishStockEdit() {
        if (finalized || finalizing) {
            return;
        }
        if (beginFailed) {
            finalized = true;
            if (this.menu.isStockEditActive()) {
                this.menu.completeStockEditFinalization(true);
            }
            if (this.minecraft != null) {
                this.minecraft.setScreen(new WheatMarketItemEditScreen(
                        this.menu,
                        this.inventory,
                        this.title,
                        this.item,
                        this.stack,
                        this.draft
                ));
            }
            return;
        }
        finalizing = true;
        submittedFinalStock = this.menu.getStockEditAmount();
        if (this.stockEditUI != null) {
            this.stockEditUI.setFinalizing(true);
        }
        WheatMarketNetwork.sendToServer(new FinalizeStockEditC2SPacket());
    }

    private ItemStack lockedStackTemplate() {
        if (this.stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack lockedStack = this.stack.copy();
        lockedStack.setCount(1);
        return lockedStack;
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

    private void installStockEditModularUI(ModularUI modularUI) {
        this.stockEditModularUI = modularUI;
        this.stockEditModularUI.setMenu(this.menu);
        this.stockEditModularUI.setScreenAndInit(this);
        this.addRenderableWidget(this.stockEditModularUI.getWidget());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }
}

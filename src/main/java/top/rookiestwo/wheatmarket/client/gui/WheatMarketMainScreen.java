package top.rookiestwo.wheatmarket.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.client.gui.containers.MainContentContainer;
import top.rookiestwo.wheatmarket.client.gui.containers.TitleContainer;
import top.rookiestwo.wheatmarket.client.gui.widgets.BlockBackgroundWidget;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class WheatMarketMainScreen extends AbstractContainerScreen<WheatMarketMenu> {

    private static final ResourceLocation SCREEN_BACKGROUND = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/widget_bg");

    private static BlockBackgroundWidget backgroundWidget;

    private static final int TitleX = 30;
    private static final int TitleY = 15;

    private TitleContainer titleContainer;
    private MainContentContainer mainContentContainer;

    public WheatMarketMainScreen(WheatMarketMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu,inventory,component);
    }

    public void addButton(AbstractButton button){
        this.addRenderableWidget(button);
    }

    public void addWidget(AbstractWidget widget){
        super.addWidget(widget);
    }

    @Override
    protected void init() {
        this.titleContainer=new TitleContainer(TitleX,TitleY,this.width,this.height,Component.empty(),this);
        this.mainContentContainer=new MainContentContainer(0,0,this.width,this.height,Component.empty(),this,titleContainer);
        backgroundWidget=new BlockBackgroundWidget(-7, -7, this.width+14, this.height+14, Component.empty(), 0.0f, 0.0f, SCREEN_BACKGROUND);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBg(guiGraphics,f,i,j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        backgroundWidget.render(guiGraphics,i,j,f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta){
        this.renderBackground(guiGraphics,mouseX,mouseY,delta);
        this.titleContainer.render(guiGraphics,mouseX,mouseY,delta);
        this.mainContentContainer.render(guiGraphics,mouseX,mouseY,delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode,scanCode,modifiers);
    }

    @Override
    public void onClose(){
        super.onClose();
    }
}

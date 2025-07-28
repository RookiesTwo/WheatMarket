package top.rookiestwo.wheatmarket.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.client.gui.containers.MainContentContainer;
import top.rookiestwo.wheatmarket.client.gui.containers.TitleContainer;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

@Environment(EnvType.CLIENT)
public class WheatMarketMenuScreen extends AbstractContainerScreen<WheatMarketMenu> {

    private static final ResourceLocation BACKGROUND_LOCATION = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"textures/gui/background.png");

    private static final int TitleX = 30;
    private static final int TitleY = 15;

    private TitleContainer titleContainer;
    private MainContentContainer mainContentContainer;

    public WheatMarketMenuScreen(WheatMarketMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu,inventory,component);
    }

    @Override
    protected void init() {
        this.titleContainer=new TitleContainer(TitleX,TitleY,this.width,this.height,Component.empty(),this);
        this.mainContentContainer=new MainContentContainer(0,0,this.width,this.height,Component.empty(),this,titleContainer);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBg(guiGraphics,f,i,j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        guiGraphics.blit(BACKGROUND_LOCATION, 0, 0, 0, 0,width,height,16,16);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta){
        this.renderBackground(guiGraphics,mouseX,mouseY,delta);
        this.titleContainer.render(guiGraphics,mouseX,mouseY,delta);
        this.mainContentContainer.render(guiGraphics,mouseX,mouseY,delta);
    }

    @Override
    public void onClose(){
        super.onClose();
    }
}

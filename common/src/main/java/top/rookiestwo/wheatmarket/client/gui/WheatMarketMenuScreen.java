package top.rookiestwo.wheatmarket.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.multiplayer.PlayerInfo;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class WheatMarketMenuScreen extends AbstractContainerScreen<WheatMarketMenu> {
    private static final ResourceLocation BACKGROUND_LOCATION = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"textures/gui/background.png");
    private static final ResourceLocation MARKET_MENU_TITLE = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"textures/gui/market_menu_title.png");
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/frame");

    private static final int TitleX = 30;
    private static final int TitleY = 15;
    private static final int TitleOriginWidth=249;
    private static final int TitleOriginHeight=25;
    private static int TitleRenderWidth=TitleOriginWidth;
    private static int TitleRenderHeight=TitleOriginHeight;

    private static final int HeadX = 100;
    private static final int HeadY = TitleY+5;
    private static final int HeadWidth = 16;

    private static int FilterFrameWidth;
    private static int FilterFrameHeight=16;

    private static int ClassFrameWidth=60;
    private static int ClassFrameHeight=90;

    private static int AddGoodsButtonWidth=ClassFrameWidth;
    private static int AddGoodsButtonHeight=ClassFrameHeight;

    private static int GoodsFrameWidth;
    private static int GoodsFrameHeight;

    private PlayerInfo playerInfo;

    public WheatMarketMenuScreen(WheatMarketMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu,inventory,component);
    }

    @Override
    protected void init() {
        if (Minecraft.getInstance().player != null) {
            playerInfo= new PlayerInfo(Minecraft.getInstance().player.getGameProfile(),false);
        }
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
        this.drawTitle(guiGraphics);
        this.drawPlayerHead(guiGraphics);
        this.drawPlayerID(guiGraphics);
        this.drawFilterColumn(guiGraphics);
        this.drawClassificationColumn(guiGraphics);
        this.drawAddGoodsButton(guiGraphics);
        this.drawGoodsColumn(guiGraphics);
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        TitleRenderWidth=TitleOriginWidth;
        TitleRenderHeight=TitleOriginHeight;
        if(TitleX+TitleOriginWidth+HeadX>this.width){
            TitleRenderWidth=this.width-TitleX-HeadX-5;
            float temp= (float) TitleRenderWidth /TitleOriginWidth;
            TitleRenderHeight=(int)(temp*TitleOriginHeight);
        }
        guiGraphics.blit(MARKET_MENU_TITLE, TitleX, TitleY+TitleOriginHeight-TitleRenderHeight, TitleOriginWidth, TitleRenderHeight, 0, 0, 249, 25, 256, 128);
    }

    private void drawPlayerHead(GuiGraphics guiGraphics) {
        ResourceLocation skinLocation = playerInfo.getSkin().texture();
        // draw base layer
        guiGraphics.blit(skinLocation, this.width-HeadX, HeadY, HeadWidth, HeadWidth, 8.0f, 8, 8, 8, 64, 64);
        // draw hat
        guiGraphics.blit(skinLocation, this.width-HeadX, HeadY, HeadWidth, HeadWidth, 40.0f, 8, 8, 8, 64, 64);
    }

    private void drawPlayerID(GuiGraphics guiGraphics){
        guiGraphics.drawString(font,Component.literal(playerInfo.getProfile().getName()),this.width-HeadX+HeadWidth+10,HeadY+5,0xFFFFFF,true);
    }

    private void drawFilterColumn(GuiGraphics guiGraphics) {
        FilterFrameWidth=this.width-125;
        guiGraphics.blitSprite(FRAME,TitleX+ClassFrameWidth+5,TitleY+TitleRenderHeight+5,FilterFrameWidth,FilterFrameHeight);
    }

    private void drawClassificationColumn(GuiGraphics guiGraphics) {
        guiGraphics.blitSprite(FRAME,TitleX,TitleY+TitleRenderHeight+5,ClassFrameWidth,ClassFrameHeight);
    }

    private void drawAddGoodsButton(GuiGraphics guiGraphics) {
        guiGraphics.blitSprite(FRAME,TitleX,TitleY+TitleRenderHeight+5+ClassFrameHeight+5,AddGoodsButtonWidth,AddGoodsButtonHeight);
    }

    private void drawGoodsColumn(GuiGraphics guiGraphics) {
        GoodsFrameWidth=FilterFrameWidth;
        GoodsFrameHeight=ClassFrameHeight+AddGoodsButtonHeight-FilterFrameHeight;
        guiGraphics.blitSprite(FRAME,TitleX+ClassFrameWidth+5,TitleY+TitleRenderHeight+5+FilterFrameHeight+5,GoodsFrameWidth,GoodsFrameHeight);
    }
}

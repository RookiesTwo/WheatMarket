package top.rookiestwo.wheatmarket.client.gui.containers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.client.gui.widgets.PlayerHeadWidget;
import top.rookiestwo.wheatmarket.client.gui.widgets.TitleWidget;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class TitleContainer extends AbstractWidgetContainer {
    private TitleWidget titleWidget;
    private static final ResourceLocation MARKET_MENU_TITLE = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "textures/gui/market_menu_title.png");
    private static final int TitleOriginWidth=249;
    private static final int TitleOriginHeight=25;

    private PlayerHeadWidget playerHeadWidget;
    private static final ResourceLocation HEAD = null;
    private static final int HeadWidth = 16;

    private static final int ContainerPaddingX = 30;

    public TitleContainer(int x, int y, int width, int height, Component message, AbstractContainerScreen<WheatMarketMenu> screen) {
        super(x, y, width, height, message,screen);
        this.setX(ContainerPaddingX);
        this.setY(15);
        titleWidget = new TitleWidget(x, y, TitleOriginWidth, TitleOriginHeight, message, 0.0f, 0.5f, MARKET_MENU_TITLE);
        titleWidget.setY(this.getY() + 12);
        playerHeadWidget =new PlayerHeadWidget(x, y, HeadWidth, HeadWidth, message, 1.0f, 0.5f, MARKET_MENU_TITLE);
        playerHeadWidget.setY(this.getY() + 12);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.fitWidgets();
        titleWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        playerHeadWidget.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void fitWidgets() {
        //重设容器大小
        this.setWidth(this.screen.width-ContainerPaddingX*2);
        //设置head为右对齐
        playerHeadWidget.setX(this.getX() + this.getWidth());
        if(titleWidget.getWidth()+ playerHeadWidget.getWidth()>this.getWidth()) {
            //因为有锚点存在，可以直接重新设置大小，而不改变（视觉上的）绝对位置，Title的锚点在左中心（左边的中点），Head的锚点在右中心
            titleWidget.setWidth(this.getWidth()- playerHeadWidget.getWidth()-5);
            titleWidget.setHeight((int)((float)TitleOriginHeight*((float)titleWidget.getWidth()/(float)TitleOriginWidth)));
        }
    }
}

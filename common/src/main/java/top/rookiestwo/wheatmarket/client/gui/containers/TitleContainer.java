package top.rookiestwo.wheatmarket.client.gui.containers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.client.gui.widgets.HeadWidget;
import top.rookiestwo.wheatmarket.client.gui.widgets.TitleWidget;
import top.rookiestwo.wheatmarket.menu.WheatMarketMenu;

public class TitleContainer extends AbstractWidgetContainer {
    private TitleWidget titleWidget;
    private static final ResourceLocation MARKET_MENU_TITLE = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID, "textures/gui/market_menu_title.png");
    private static final int TitleOriginWidth=249;
    private static final int TitleOriginHeight=25;

    private HeadWidget headWidget;
    private static final ResourceLocation HEAD = null;
    private static final int HeadWidth = 16;

    private static final int ContainerPaddingX = 30;

    public TitleContainer(int x, int y, int width, int height, Component message, AbstractContainerScreen<WheatMarketMenu> screen) {
        super(x, y, width, height, message,screen);
        this.setX(ContainerPaddingX);
        this.setY(15);
        titleWidget = new TitleWidget(x, y, TitleOriginWidth, TitleOriginHeight, message, 0.0f, 0.5f, MARKET_MENU_TITLE);
        titleWidget.setY(this.getY() + 12);
        headWidget=new HeadWidget(x, y, HeadWidth, HeadWidth, message, 1.0f, 0.5f, MARKET_MENU_TITLE);
        headWidget.setY(this.getY() + 12);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.fitWidgets();
        titleWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        headWidget.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void fitWidgets() {
        this.setWidth(this.screen.width-ContainerPaddingX*2);
        headWidget.setX(this.getX() + this.getWidth());
        if(titleWidget.getWidth()+headWidget.getWidth()>this.getWidth()) {
            titleWidget.setWidth(this.getWidth()-headWidget.getWidth()-5);
            titleWidget.setHeight((int)((float)TitleOriginHeight*((float)titleWidget.getWidth()/(float)TitleOriginWidth)));
            WheatMarket.LOGGER.info("Title width changed to "+titleWidget.getWidth());
            WheatMarket.LOGGER.info("Title height changed to "+titleWidget.getHeight());
        }
    }

}

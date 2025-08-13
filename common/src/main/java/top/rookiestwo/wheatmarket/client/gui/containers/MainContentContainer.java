package top.rookiestwo.wheatmarket.client.gui.containers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.client.gui.WheatMarketMenuScreen;
import top.rookiestwo.wheatmarket.client.gui.widgets.BlockBackgroundWidget;
import top.rookiestwo.wheatmarket.client.gui.widgets.buttons.FilterButton;
import top.rookiestwo.wheatmarket.client.gui.widgets.buttons.ItemType;
import top.rookiestwo.wheatmarket.client.gui.widgets.buttons.SortType;
import top.rookiestwo.wheatmarket.client.gui.widgets.buttons.TradeType;

@Environment(EnvType.CLIENT)
public class MainContentContainer extends AbstractWidgetContainer{

    /**
     * 主内容容器，包含所有主内容组件
     */

    private static final ResourceLocation BOARD = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/board");
    private static final ResourceLocation BUTTON = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/button");
    private static final ResourceLocation WIDGET_BG=ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/widget_bg");
    private BlockBackgroundWidget searchBackgroundWidget;
    private BlockBackgroundWidget goodsBackgroundWidget;

    private FilterButton<TradeType> filter1Button;
    private FilterButton<SortType> filter2Button;
    private FilterButton<ItemType> filter3Button;

    private static int containerPaddingX = 15;
    private static int containerPaddingY = 15;

    private static int widgetCommonPadding = 5;

    private static int defaultFilterWidth = 60;

    private static int defaultSearchBarHeight = 32;

    private AbstractWidgetContainer parentContainer;

    public MainContentContainer(int x, int y, int width, int height, Component message, Screen screen,AbstractWidgetContainer parentContainer) {
        super(x, y, width, height, message, screen);

        this.parentContainer = parentContainer;

        this.setX(containerPaddingX);
        this.setY(parentContainer.getY() + parentContainer.getHeight() + containerPaddingY);

        searchBackgroundWidget = new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, BOARD);
        goodsBackgroundWidget = new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, BOARD);

        filter1Button= new FilterButton<>(
                x,
                y,
                defaultFilterWidth,
                16,
                Component.empty(),
                BUTTON,
                TradeType.ALL
        );
        filter2Button = new FilterButton<>(
                x,
                y,
                defaultFilterWidth,
                16,
                Component.empty()
                ,BUTTON
                ,SortType.LAST_TRADE
        );
        filter3Button = new FilterButton<>(
                x,
                y,
                defaultFilterWidth,
                16,
                Component.empty(),
                BUTTON,
                ItemType.ALL
        );
        this.fitWidgets();
        this.addButtons();
    }

    @Override
    public void fitWidgets() {
        //重设容器位置
        this.setX(containerPaddingX);
        this.setY(parentContainer.getY() + parentContainer.getHeight());

        //重设容器大小
        this.setWidth(this.screen.width - containerPaddingX * 2);
        this.setHeight(this.screen.height - parentContainer.getY() - parentContainer.getHeight() - containerPaddingY);

        //设置组件的大小
        int filterWidth = Integer.max(defaultFilterWidth, this.getWidth()/10);
        int filterHeight = (this.getHeight()-4*widgetCommonPadding)/3; //将高度分为三等分

        filter1Button.setWidth(filterWidth);
        filter1Button.setHeight(filterHeight);
        filter2Button.setWidth(filterWidth);
        filter2Button.setHeight(filterHeight);
        filter3Button.setWidth(filterWidth);
        filter3Button.setHeight(filterHeight);

        searchBackgroundWidget.setWidth(this.getWidth()-3*widgetCommonPadding-filter1Button.getWidth());
        searchBackgroundWidget.setHeight(Integer.max(defaultSearchBarHeight,this.getHeight()/10));

        goodsBackgroundWidget.setWidth(searchBackgroundWidget.getWidth());
        goodsBackgroundWidget.setHeight(this.getHeight()- searchBackgroundWidget.getHeight() - 3 * widgetCommonPadding);

        //设置组件的位置
        filter1Button.setX(this.getX() + widgetCommonPadding);
        filter1Button.setY(this.getY() + widgetCommonPadding);

        filter2Button.setX(this.getX() + widgetCommonPadding);
        filter2Button.setY(filter1Button.getY()+ filter1Button.getHeight() + widgetCommonPadding);

        filter3Button.setX(this.getX()+ widgetCommonPadding);
        filter3Button.setY(filter2Button.getY()+ filter2Button.getHeight() + widgetCommonPadding);

        searchBackgroundWidget.setX(filter1Button.getX() + filter1Button.getWidth() + widgetCommonPadding);
        searchBackgroundWidget.setY(filter1Button.getY());

        goodsBackgroundWidget.setX(searchBackgroundWidget.getX());
        goodsBackgroundWidget.setY(searchBackgroundWidget.getY() + searchBackgroundWidget.getHeight() + widgetCommonPadding);
    }

    private void addButtons(){
        if(this.screen instanceof WheatMarketMenuScreen){
            ((WheatMarketMenuScreen) this.screen).addButton(filter1Button);
            ((WheatMarketMenuScreen) this.screen).addButton(filter2Button);
            ((WheatMarketMenuScreen) this.screen).addButton(filter3Button);;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //渲染背景
        //filter1BackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        //filter2BackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        //filter3BackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        searchBackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        goodsBackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        //渲染按钮
        filter1Button.render(guiGraphics, mouseX, mouseY, partialTick);
        filter2Button.render(guiGraphics, mouseX, mouseY, partialTick);
        filter3Button.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

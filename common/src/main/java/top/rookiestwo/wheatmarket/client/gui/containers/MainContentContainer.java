package top.rookiestwo.wheatmarket.client.gui.containers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/widget_bg");
    private BlockBackgroundWidget filter1BackgroundWidget;
    private BlockBackgroundWidget filter2BackgroundWidget;
    private BlockBackgroundWidget filter3BackgroundWidget;
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

        filter1BackgroundWidget = new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, BACKGROUND);
        filter2BackgroundWidget = new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, BACKGROUND);
        filter3BackgroundWidget = new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, BACKGROUND);
        searchBackgroundWidget = new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, BACKGROUND);
        goodsBackgroundWidget = new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, BACKGROUND);

        filter1Button= new FilterButton<>(
                x,
                y,
                defaultFilterWidth,
                16,
                Component.empty(),
                BACKGROUND,
                TradeType.ALL
        );
        //filter2Button = new FilterButton<>(x,y,defaultFilterWidth,16,Component.empty(),)
        this.fitWidgets();
        this.addButtons();
    }

    @Override
    public void fitWidgets() {
        //重设容器位置
        this.setX(containerPaddingX);
        this.setY(parentContainer.getY() + parentContainer.getHeight() + containerPaddingY);

        //重设容器大小
        this.setWidth(this.screen.width - containerPaddingX * 2);
        this.setHeight(this.screen.height - parentContainer.getY() - parentContainer.getHeight() - containerPaddingY * 2);

        //设置背景组件的大小
        int filterWidth = Integer.max(defaultFilterWidth, this.getWidth()/10);
        int filterHeight = (this.getHeight()-4*widgetCommonPadding)/3; //将高度分为三等分
        filter1BackgroundWidget.setWidth(filterWidth);
        filter1BackgroundWidget.setHeight(filterHeight);
        filter2BackgroundWidget.setWidth(filterWidth);
        filter2BackgroundWidget.setHeight(filterHeight);
        filter3BackgroundWidget.setWidth(filterWidth);
        filter3BackgroundWidget.setHeight(filterHeight);

        searchBackgroundWidget.setWidth(this.getWidth()-3*widgetCommonPadding-filter1BackgroundWidget.getWidth());
        searchBackgroundWidget.setHeight(Integer.max(defaultSearchBarHeight,this.getHeight()/10));

        goodsBackgroundWidget.setWidth(searchBackgroundWidget.getWidth());
        goodsBackgroundWidget.setHeight(this.getHeight()- searchBackgroundWidget.getHeight() - 3 * widgetCommonPadding);

        //设置背景组件的位置
        filter1BackgroundWidget.setX(this.getX() + widgetCommonPadding);
        filter1BackgroundWidget.setY(this.getY() + widgetCommonPadding);

        filter2BackgroundWidget.setX(this.getX() + widgetCommonPadding);
        filter2BackgroundWidget.setY(filter1BackgroundWidget.getY()+ filter1BackgroundWidget.getHeight() + widgetCommonPadding);

        filter3BackgroundWidget.setX(this.getX()+ widgetCommonPadding);
        filter3BackgroundWidget.setY(filter2BackgroundWidget.getY()+ filter2BackgroundWidget.getHeight() + widgetCommonPadding);

        searchBackgroundWidget.setX(filter1BackgroundWidget.getX() + filter1BackgroundWidget.getWidth() + widgetCommonPadding);
        searchBackgroundWidget.setY(filter1BackgroundWidget.getY());

        goodsBackgroundWidget.setX(searchBackgroundWidget.getX());
        goodsBackgroundWidget.setY(searchBackgroundWidget.getY() + searchBackgroundWidget.getHeight() + widgetCommonPadding);

        //设置按钮大小和位置
        filter1Button.setX(filter1BackgroundWidget.getX() + 2);
        filter1Button.setY(filter1BackgroundWidget.getY() + 2);
        filter1Button.setWidth(filter1BackgroundWidget.getWidth() - 4);
        filter1Button.setHeight(filter1BackgroundWidget.getHeight() - 4);
    }

    private void addButtons(){
        if(this.screen instanceof WheatMarketMenuScreen){
            ((WheatMarketMenuScreen) this.screen).addButton(filter1Button);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //渲染背景
        filter1BackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        filter2BackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        filter3BackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        searchBackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        goodsBackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        //渲染按钮
        filter1Button.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

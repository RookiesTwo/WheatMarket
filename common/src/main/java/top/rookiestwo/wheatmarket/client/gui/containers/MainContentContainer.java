package top.rookiestwo.wheatmarket.client.gui.containers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.client.gui.WheatMarketMenuScreen;
import top.rookiestwo.wheatmarket.client.gui.widgets.BlockBackgroundWidget;
import top.rookiestwo.wheatmarket.client.gui.widgets.buttons.FilterButton;
import top.rookiestwo.wheatmarket.client.gui.widgets.buttons.ItemType;
import top.rookiestwo.wheatmarket.client.gui.widgets.buttons.SortType;
import top.rookiestwo.wheatmarket.client.gui.widgets.buttons.TradeType;

public class MainContentContainer extends AbstractWidgetContainer{

    /**
     * 主内容容器，包含所有主内容组件
     */

    private static final ResourceLocation BOARD = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/board");
    private static final ResourceLocation BUTTON = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/button");
    private static final ResourceLocation WIDGET_BG=ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/widget_bg");
    private static final ResourceLocation PAPER =ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/paper");
    private static final ResourceLocation PAPER_WITH_WRINKLES =ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/paper_with_wrinkles");

    //搜索框及其背景
    private BlockBackgroundWidget searchBackgroundWidget;
    private BlockBackgroundWidget searchBarBackgroundWidget;
    private EditBox searchEditBox;

    private BlockBackgroundWidget goodsBackgroundWidget;

    private FilterButton<TradeType> filter1Button;
    private FilterButton<SortType> filter2Button;
    private FilterButton<ItemType> filter3Button;

    private static int containerPaddingX = 15;
    private static int containerPaddingY = 15;

    private static int searchBarPaddingX = 8;

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

        filter1Button= new FilterButton<>(x, y, defaultFilterWidth, 16, Component.empty(), BUTTON, TradeType.ALL);
        filter2Button = new FilterButton<>(x, y, defaultFilterWidth, 16, Component.empty(),BUTTON,SortType.LAST_TRADE);
        filter3Button = new FilterButton<>(x, y, defaultFilterWidth, 16, Component.empty(), BUTTON, ItemType.ALL);

        searchBarBackgroundWidget=new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, PAPER);
        searchEditBox=new EditBox(Minecraft.getInstance().font, x, y, 0, 0, Component.translatable("gui.wheatmarket.searchbar"));
        searchEditBox.setVisible(true);
        searchEditBox.setBordered(false);
        searchEditBox.setMaxLength(50);
        searchEditBox.setTextColor(0x2B2D30);
        this.fitWidgets();
        this.addWidgets();
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
        searchEditBox.setWidth(searchBackgroundWidget.getWidth()/5*4-searchBarPaddingX*2);
        searchEditBox.setHeight(searchBackgroundWidget.getHeight()/5*4);
        searchBarBackgroundWidget.setWidth(searchBackgroundWidget.getWidth()/5*4);
        searchBarBackgroundWidget.setHeight(searchBackgroundWidget.getHeight()/5*4);

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
        searchBarBackgroundWidget.setX(searchBackgroundWidget.getX()+searchBarPaddingX);
        searchBarBackgroundWidget.setY(searchBackgroundWidget.getY()+searchBackgroundWidget.getHeight()/10);
        searchEditBox.setX(searchBarBackgroundWidget.getX()+searchBarPaddingX);
        searchEditBox.setY(searchBarBackgroundWidget.getY()+(searchBarBackgroundWidget.getHeight()-8)/2);

        goodsBackgroundWidget.setX(searchBackgroundWidget.getX());
        goodsBackgroundWidget.setY(searchBackgroundWidget.getY() + searchBackgroundWidget.getHeight() + widgetCommonPadding);
    }

    private void addWidgets(){
        if(this.screen instanceof WheatMarketMenuScreen){
            ((WheatMarketMenuScreen) this.screen).addButton(filter1Button);
            ((WheatMarketMenuScreen) this.screen).addButton(filter2Button);
            ((WheatMarketMenuScreen) this.screen).addButton(filter3Button);
            ((WheatMarketMenuScreen) this.screen).addWidget(searchEditBox);
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
        //渲染搜索框
        searchBarBackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        searchEditBox.render(guiGraphics, mouseX, mouseY, partialTick);
        int x=goodsBackgroundWidget.getX()+8;
        int y=goodsBackgroundWidget.getY() + 8;
        int width=(goodsBackgroundWidget.getHeight()-24)/2;
        for(int i=0;i<10;i++){
            if(i==5)y= y+8+width;
            if(i<5){
                x =goodsBackgroundWidget.getX() + 8 + i * (width+8);
            }
            else{
                x =goodsBackgroundWidget.getX() + 8 + (i-5) * (width+8);
            }
            guiGraphics.blitSprite(PAPER_WITH_WRINKLES, x, y, width, width);
        }
    }
}

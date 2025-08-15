package top.rookiestwo.wheatmarket.client.gui.containers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.rookiestwo.wheatmarket.WheatMarket;
import top.rookiestwo.wheatmarket.client.gui.WheatMarketMenuScreen;
import top.rookiestwo.wheatmarket.client.gui.widgets.BlockBackgroundWidget;
import top.rookiestwo.wheatmarket.client.gui.widgets.WheatEditBox;
import top.rookiestwo.wheatmarket.client.gui.widgets.buttons.*;

import java.util.Random;

public class MainContentContainer extends AbstractWidgetContainer{

    /**
     * 主内容容器，包含所有主内容组件
     */

    private static final ResourceLocation BOARD = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/board");
    private static final ResourceLocation BUTTON = ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/button");
    private static final ResourceLocation WIDGET_BG=ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/widget_bg");
    private static final ResourceLocation PAPER =ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"screen/main_menu/paper");
    private static final ResourceLocation[] PAPER_WITH_WRINKLES = new ResourceLocation[5];
    private static final int[] randomNumbers = new int[10];

    //搜索框及其背景
    private BlockBackgroundWidget searchBackgroundWidget;
    private BlockBackgroundWidget searchBarBackgroundWidget;
    private WheatEditBox searchEditBox;

    private BlockBackgroundWidget goodsBackgroundWidget;

    private FilterButton<TradeType> filter1Button;
    private FilterButton<SortType> filter2Button;
    private FilterButton<ItemType> filter3Button;

    private static final int containerPaddingX = 15;
    private static final int containerPaddingY = 15;
    private static final int searchBarPaddingX = 8;
    private static final int widgetCommonPadding = 5;
    private static final int defaultFilterWidth = 40;
    private static final int defaultSearchBarHeight = 25;
    private SearchButton searchButton;

    private AbstractWidgetContainer parentContainer;

    public MainContentContainer(int x, int y, int width, int height, Component message, Screen screen,AbstractWidgetContainer parentContainer) {
        super(x, y, width, height, message, screen);

        for (int i = 0; i < 5; i++) {
            PAPER_WITH_WRINKLES[i]=ResourceLocation.fromNamespaceAndPath(WheatMarket.MOD_ID,"textures/gui/paper_with_wrinkles_"+i+".png");
        }

        this.parentContainer = parentContainer;

        this.setX(containerPaddingX);
        this.setY(parentContainer.getY() + parentContainer.getHeight() + containerPaddingY);

        searchBackgroundWidget = new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, BOARD);
        goodsBackgroundWidget = new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, BOARD);

        filter1Button = new FilterButton<>(x, y, defaultFilterWidth, 16, Component.empty(), BUTTON, TradeType.ALL);
        filter2Button = new FilterButton<>(x, y, defaultFilterWidth, 16, Component.empty(), BUTTON, SortType.LAST_TRADE);
        filter3Button = new FilterButton<>(x, y, defaultFilterWidth, 16, Component.empty(), BUTTON, ItemType.ALL);

        searchButton = new SearchButton(x, y, 16, 16, Component.empty(), BUTTON);

        searchBarBackgroundWidget = new BlockBackgroundWidget(x, y, width, 16, Component.empty(), 0.0f, 0.0f, PAPER);
        searchEditBox = new WheatEditBox(Minecraft.getInstance().font, x, y, 0, 0, Component.translatable("gui.wheatmarket.searchbar"));
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
        int filterWidth = Integer.max(defaultFilterWidth, this.getWidth() / 5);
        int filterHeight = Integer.max(defaultSearchBarHeight, this.getHeight() / 15);

        filter1Button.setWidth(filterWidth);
        filter1Button.setHeight(filterHeight);
        filter2Button.setWidth(filterWidth);
        filter2Button.setHeight(filterHeight);
        filter3Button.setWidth(filterWidth);
        filter3Button.setHeight(filterHeight);

        searchBackgroundWidget.setWidth(this.getWidth() - 5*widgetCommonPadding - 3*filter1Button.getWidth());
        searchBackgroundWidget.setHeight(filterHeight);

        searchBarBackgroundWidget.setWidth((searchBackgroundWidget.getWidth() - searchBarPaddingX * 3) / 4 * 3);
        searchBarBackgroundWidget.setHeight(searchBackgroundWidget.getHeight() / 4 * 3);

        searchEditBox.setWidth(searchBarBackgroundWidget.getWidth() - searchBarPaddingX);
        searchEditBox.setHeight(searchBackgroundWidget.getHeight() / 4 * 3);

        searchButton.setWidth((searchBackgroundWidget.getWidth() - searchBarPaddingX * 3) / 4);
        searchButton.setHeight(Math.max(searchBarBackgroundWidget.getHeight(), 20));

        goodsBackgroundWidget.setWidth(this.getWidth()-2*widgetCommonPadding);
        goodsBackgroundWidget.setHeight(this.getHeight()- searchBackgroundWidget.getHeight() - 3 * widgetCommonPadding);

        //设置组件的位置
        filter1Button.setX(this.getX() + widgetCommonPadding);
        filter1Button.setY(this.getY() + widgetCommonPadding);

        filter2Button.setX(filter1Button.getX() + filter1Button.getWidth() + widgetCommonPadding);
        filter2Button.setY(this.getY() + widgetCommonPadding);

        filter3Button.setX(filter2Button.getX() + filter2Button.getWidth() + widgetCommonPadding);
        filter3Button.setY(this.getY() + widgetCommonPadding);

        searchBackgroundWidget.setX(filter3Button.getX() + filter3Button.getWidth() + widgetCommonPadding);
        searchBackgroundWidget.setY(filter1Button.getY());

        searchBarBackgroundWidget.setX(searchBackgroundWidget.getX()+searchBarPaddingX);
        searchBarBackgroundWidget.setY(searchBackgroundWidget.getY() + searchBackgroundWidget.getHeight() / 10 + 1);

        searchEditBox.setX(searchBarBackgroundWidget.getX()+searchBarPaddingX);
        searchEditBox.setY(searchBarBackgroundWidget.getY()+(searchBarBackgroundWidget.getHeight()-8)/2);

        searchButton.setX(searchBarBackgroundWidget.getX() + searchBarBackgroundWidget.getWidth() + searchBarPaddingX);
        searchButton.setY(searchBarBackgroundWidget.getY() - 2);
        searchButton.fitIcon();

        goodsBackgroundWidget.setX(filter1Button.getX());
        goodsBackgroundWidget.setY(filter1Button.getY() + filter1Button.getHeight() + widgetCommonPadding);
        //以下为暂用的渲染商品用的代码，以后需要删除
        generateRandomNum();
    }

    private void addWidgets(){
        if(this.screen instanceof WheatMarketMenuScreen){
            ((WheatMarketMenuScreen) this.screen).addButton(filter1Button);
            ((WheatMarketMenuScreen) this.screen).addButton(filter2Button);
            ((WheatMarketMenuScreen) this.screen).addButton(filter3Button);
            ((WheatMarketMenuScreen) this.screen).addButton(searchButton);
            ((WheatMarketMenuScreen) this.screen).addWidget(searchEditBox);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //渲染背景
        searchBackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        goodsBackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        //渲染按钮
        filter1Button.render(guiGraphics, mouseX, mouseY, partialTick);
        filter2Button.render(guiGraphics, mouseX, mouseY, partialTick);
        filter3Button.render(guiGraphics, mouseX, mouseY, partialTick);
        //渲染搜索框
        searchBarBackgroundWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        searchEditBox.render(guiGraphics, mouseX, mouseY, partialTick);
        searchButton.render(guiGraphics, mouseX, mouseY, partialTick);
        //以下为暂用的渲染商品用的代码，以后需要删除
        renderGoods(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void generateRandomNum() {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            randomNumbers[i] = random.nextInt(5);
        }
    }

    private void renderGoods(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = goodsBackgroundWidget.getX() + 8;
        int y = goodsBackgroundWidget.getY() + 8;
        int width = 64;
        for (int i = 0; i < 10; i++) {
            if (i == 5) y = y + 8 + width;
            if (i < 5) {
                x = goodsBackgroundWidget.getX() + 8 + i * (width + 8);
            } else {
                x = goodsBackgroundWidget.getX() + 8 + (i - 5) * (width + 8);
            }
            guiGraphics.blit(PAPER_WITH_WRINKLES[randomNumbers[i]], x, y, 0, 0, width, width, 64, 64);
        }
    }
}

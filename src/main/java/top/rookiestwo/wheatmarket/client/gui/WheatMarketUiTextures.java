package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class WheatMarketUiTextures {
    public static final String SEARCH_ICON_TEXTURE = "wheatmarket:textures/gui/search_icon.png";
    public static final String SHOPPING_CART_ICON_TEXTURE = "wheatmarket:textures/gui/shopping_cart.png";
    public static final String SELL_ICON_TEXTURE = "wheatmarket:textures/gui/sell.png";
    public static final String SELL_HOVERED_ICON_TEXTURE = "wheatmarket:textures/gui/sell_hovered.png";
    public static final String FAVORITE_ICON_TEXTURE = "wheatmarket:textures/gui/favorite.png";
    public static final String EDIT_ICON_TEXTURE = "wheatmarket:textures/gui/edit.png";
    public static final String SETTINGS_ICON_TEXTURE = "wheatmarket:textures/gui/settings.png";
    public static final String ADD_ICON_TEXTURE = "wheatmarket:textures/gui/add.png";
    public static final String SUBTRACT_ICON_TEXTURE = "wheatmarket:textures/gui/subtract.png";
    public static final String FILTER_ICON_TEXTURE = "wheatmarket:textures/gui/filter_icon.png";
    public static final String MY_ITEMS_CHECK_TEXTURE = "wheatmarket:textures/gui/my_items_check.png";

    private static final String PANEL_SPRITE = "wheatmarket:screen/main_menu/board";
    private static final String PAPER_SPRITE = "wheatmarket:screen/main_menu/paper";
    private static final String TRADING_PAPER_SPRITE = "wheatmarket:screen/main_menu/paper_2";
    private static final String WRINKLED_PAPER_TEXTURE_PREFIX = "wheatmarket:textures/gui/paper_with_wrinkles_";
    private static final String WRINKLED_PAPER_TEXTURE_SUFFIX = ".png";
    private static final String BUTTON_SPRITE = "wheatmarket:screen/main_menu/button";
    private static final String BUTTON_PRESSED_SPRITE = "wheatmarket:screen/main_menu/button_pressed";
    private static final String TITLE_TEXTURE = "wheatmarket:textures/gui/market_menu_title.png";
    private static final String TRADING_BACKGROUND_TEXTURE = "wheatmarket:textures/gui/trading_background.png";
    private static final String COMPLETED_STAMP_TEXTURE = "wheatmarket:textures/gui/completed_stamp.png";
    private static final String AVATAR_PLACEHOLDER_TEXTURE = "minecraft:textures/block/grass_block_top.png";

    private static final int PAPER_FRAME_COLOR = 0x7F5C4024;
    private static final int ROOT_BG_COLOR = 0xFFA25A1F;
    private static final int WRINKLED_PAPER_BORDER = 4;
    private static final int SEARCH_FOCUS_BORDER_COLOR = 0x995A4024;
    private static final int SELECTED_OVERLAY_COLOR = 0x66000000;

    public static final int BLUE_BUTTON_COLOR = 0xFF78C6EA;
    public static final int BLUE_BUTTON_HOVER_COLOR = 0xFF8CD3F2;
    public static final int BLUE_BUTTON_PRESSED_COLOR = 0xFF5BAFD6;
    public static final int RED_BUTTON_COLOR = 0xFFE86276;
    public static final int RED_BUTTON_HOVER_COLOR = 0xFFF07688;
    public static final int RED_BUTTON_PRESSED_COLOR = 0xFFD84B61;
    private static final int COLORED_BUTTON_BORDER = 0xFF3A332C;

    private WheatMarketUiTextures() {
    }

    public static IGuiTexture rootBackground() {
        return new ColorRectTexture(ROOT_BG_COLOR);
    }

    public static IGuiTexture tradingBackgroundTexture() {
        return SpriteTexture.of(TRADING_BACKGROUND_TEXTURE)
                .setWrapMode(SpriteTexture.WrapMode.REPEAT);
    }

    public static IGuiTexture panelTexture() {
        return VanillaSpriteTexture.of(PANEL_SPRITE);
    }

    public static IGuiTexture paperTexture() {
        return GuiTextureGroup.of(
                VanillaSpriteTexture.of(PAPER_SPRITE),
                new ColorBorderTexture(-1, PAPER_FRAME_COLOR)
        );
    }

    public static IGuiTexture tradingPaperTexture() {
        return VanillaSpriteTexture.of(TRADING_PAPER_SPRITE);
    }

    public static IGuiTexture cardTexture() {
        return wrinkledPaperTexture(new UUID(0L, 0L));
    }

    public static IGuiTexture wrinkledPaperTexture(UUID marketItemId) {
        return wrinkledPaperTexture(marketItemId, -1);
    }

    public static IGuiTexture wrinkledPaperTexture(UUID marketItemId, int color) {
        int paperIndex = Math.floorMod(marketItemId.hashCode(), 10);
        return SpriteTexture.of(WRINKLED_PAPER_TEXTURE_PREFIX + paperIndex + WRINKLED_PAPER_TEXTURE_SUFFIX)
                .setColor(color)
                .setBorder(WRINKLED_PAPER_BORDER)
                .setWrapMode(SpriteTexture.WrapMode.REPEAT);
    }

    public static IGuiTexture buttonBaseTexture() {
        return VanillaSpriteTexture.of(BUTTON_SPRITE);
    }

    public static IGuiTexture buttonPressedTexture() {
        return VanillaSpriteTexture.of(BUTTON_PRESSED_SPRITE);
    }

    public static IGuiTexture searchFieldFocusTexture() {
        return new ColorBorderTexture(1, SEARCH_FOCUS_BORDER_COLOR);
    }

    public static IGuiTexture iconTexture(String texturePath) {
        return SpriteTexture.of(texturePath);
    }

    public static IGuiTexture avatarPlaceholderTexture() {
        return SpriteTexture.of(AVATAR_PLACEHOLDER_TEXTURE);
    }

    public static IGuiTexture playerAvatarTexture(Player player) {
        if (player instanceof AbstractClientPlayer clientPlayer) {
            return IGuiTexture.dynamic(() -> {
                ResourceLocation skinTexture = clientPlayer.getSkin().texture();
                return GuiTextureGroup.of(
                        SpriteTexture.of(skinTexture).setSprite(8, 8, 8, 8),
                        SpriteTexture.of(skinTexture).setSprite(40, 8, 8, 8)
                );
            });
        }
        return avatarPlaceholderTexture();
    }

    public static IGuiTexture myItemsSelectedOverlayTexture() {
        return GuiTextureGroup.of(
                new ColorRectTexture(SELECTED_OVERLAY_COLOR),
                SpriteTexture.of(MY_ITEMS_CHECK_TEXTURE).scale(0.72F)
        );
    }

    public static IGuiTexture titleTexture() {
        return SpriteTexture.of(TITLE_TEXTURE).setSprite(0, 0, 256, 32);
    }

    public static IGuiTexture completedStampTexture(int alpha, float scale, float rotation) {
        return SpriteTexture.of(COMPLETED_STAMP_TEXTURE)
                .setColor((alpha << 24) | 0x00FFFFFF)
                .scale(scale)
                .rotate(rotation);
    }

    public static IGuiTexture coloredButtonTexture(int fillColor, int borderSize) {
        return GuiTextureGroup.of(
                new ColorRectTexture(fillColor),
                new ColorBorderTexture(borderSize, COLORED_BUTTON_BORDER)
        );
    }

    public static void styleColoredActionButton(Button button, int baseColor, int hoverColor, int pressedColor,
                                                int borderSize) {
        button.buttonStyle(style -> style
                .baseTexture(coloredButtonTexture(baseColor, borderSize))
                .hoverTexture(coloredButtonTexture(hoverColor, borderSize))
                .pressedTexture(coloredButtonTexture(pressedColor, borderSize)));
        button.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(0x19140D)
                .textShadow(false));
    }
}

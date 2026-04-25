package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.VanillaSpriteTexture;

public final class WheatMarketUiTextures {
    public static final String SEARCH_ICON_TEXTURE = "wheatmarket:textures/gui/search_icon.png";
    public static final String SHOPPING_CART_ICON_TEXTURE = "wheatmarket:textures/gui/shopping_cart.png";
    public static final String SELL_ICON_TEXTURE = "wheatmarket:textures/gui/sell.png";
    public static final String FAVORITE_ICON_TEXTURE = "wheatmarket:textures/gui/favorite.png";
    public static final String EDIT_ICON_TEXTURE = "wheatmarket:textures/gui/edit.png";
    public static final String SETTINGS_ICON_TEXTURE = "wheatmarket:textures/gui/settings.png";
    public static final String ADD_ICON_TEXTURE = "wheatmarket:textures/gui/add.png";
    public static final String SUBTRACT_ICON_TEXTURE = "wheatmarket:textures/gui/subtract.png";

    private static final String ROOT_BG_SPRITE = "wheatmarket:screen/main_menu/widget_bg";
    private static final String PANEL_SPRITE = "wheatmarket:screen/main_menu/board";
    private static final String FRAME_SPRITE = "wheatmarket:screen/main_menu/frame";
    private static final String PAPER_SPRITE = "wheatmarket:screen/main_menu/paper";
    private static final String WRINKLED_PAPER_TEXTURE = "wheatmarket:textures/gui/paper_with_wrinkles_0.png";
    private static final String BUTTON_SPRITE = "wheatmarket:screen/main_menu/button";
    private static final String BUTTON_PRESSED_SPRITE = "wheatmarket:screen/main_menu/button_pressed";

    private static final int PAPER_FRAME_COLOR = 0x7F5C4024;

    private static final int WRINKLED_PAPER_BORDER = 4;

    private WheatMarketUiTextures() {
    }

    public static IGuiTexture rootBackground() {
        return VanillaSpriteTexture.of(ROOT_BG_SPRITE);
    }

    public static IGuiTexture panelTexture() {
        return GuiTextureGroup.of(
                VanillaSpriteTexture.of(PANEL_SPRITE),
                VanillaSpriteTexture.of(FRAME_SPRITE)
        );
    }

    public static IGuiTexture paperTexture() {
        return GuiTextureGroup.of(
                VanillaSpriteTexture.of(PAPER_SPRITE),
                new ColorBorderTexture(-1, PAPER_FRAME_COLOR)
        );
    }

    public static IGuiTexture cardTexture() {
        return GuiTextureGroup.of(
                SpriteTexture.of(WRINKLED_PAPER_TEXTURE).setBorder(WRINKLED_PAPER_BORDER).setWrapMode(SpriteTexture.WrapMode.REPEAT),
                new ColorBorderTexture(-1, PAPER_FRAME_COLOR)
        );
    }

    public static IGuiTexture buttonBaseTexture() {
        return VanillaSpriteTexture.of(BUTTON_SPRITE);
    }

    public static IGuiTexture buttonPressedTexture() {
        return VanillaSpriteTexture.of(BUTTON_PRESSED_SPRITE);
    }

    public static IGuiTexture iconTexture(String texturePath) {
        return SpriteTexture.of(texturePath);
    }
}

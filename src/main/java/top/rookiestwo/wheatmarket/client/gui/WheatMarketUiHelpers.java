package top.rookiestwo.wheatmarket.client.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class WheatMarketUiHelpers {

    private WheatMarketUiHelpers() {
    }

    public static <T> T require(UI ui, String id, Class<T> type) {
        return ui.selectId(id, type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing UI element: " + id));
    }

    @SuppressWarnings("unchecked")
    public static <T> Selector<T> requireSelector(UI ui, String id) {
        return (Selector<T>) require(ui, id, Selector.class);
    }

    public static String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static void setShown(UIElement element, boolean shown) {
        element.setDisplay(shown);
        element.setVisible(shown);
    }

    public static void styleLabel(Label label, int color, Horizontal horizontal) {
        label.textStyle(style -> style
                .textAlignHorizontal(horizontal)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(color)
                .textShadow(false));
    }

    public static void styleLabel(Label label, int color) {
        styleLabel(label, color, Horizontal.CENTER);
    }

    public static void styleFormCaption(Label label) {
        label.textStyle(style -> style
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(0x19140D)
                .textShadow(false)
                .adaptiveWidth(true));
    }

    public static boolean isPriceInputValid(String text) {
        if (text == null || text.isBlank() || ".".equals(text)) {
            return true;
        }
        int dotIndex = text.indexOf('.');
        if (dotIndex != text.lastIndexOf('.')) {
            return false;
        }
        if (dotIndex >= 0 && text.length() - dotIndex - 1 > 2) {
            return false;
        }
        try {
            double value = Double.parseDouble(text);
            return Double.isFinite(value) && value <= 999_999.99D;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static double parsePrice(String rawValue, double fallback) {
        if (rawValue == null || rawValue.isBlank() || ".".equals(rawValue)) {
            return fallback;
        }
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static int parseNonNegativeInt(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(rawValue);
            return Math.max(0, value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static ItemStack templateCopy(ItemStack source) {
        if (source == null || source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = source.copy();
        copy.setCount(1);
        return copy;
    }

    public static String rawText(TextField field) {
        return field == null ? "" : field.getRawText();
    }
}

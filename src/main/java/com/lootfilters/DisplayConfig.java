package com.lootfilters;

import com.lootfilters.model.SoundProvider;
import com.lootfilters.model.BufferedImageProvider;
import com.lootfilters.model.FontType;
import com.lootfilters.model.TextAccent;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.runelite.client.ui.FontManager;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class DisplayConfig {
    public static Builder builder() {
        return new Builder();
    }

    public static final Color DEFAULT_MENU_TEXT_COLOR = Color.decode("#ff9040");

    private final Color textColor;
    private final Color backgroundColor;
    private final Color borderColor;
    private final Boolean hidden;
    private final Boolean showLootbeam;
    private final Boolean showValue;
    private final Boolean compact; // compact is currently a config-only setting and not supported in rs2f
    private final Boolean showDespawn;
    private final Boolean notify;
    private final TextAccent textAccent;
    private final Color textAccentColor;
    private final Color lootbeamColor;
    private final FontType fontType;
    private final Color menuTextColor;
    private final Boolean hideOverlay;

    private final Boolean highlightTile;
    private final Color tileStrokeColor;
    private final Color tileFillColor;

    private final SoundProvider sound;
    private final Integer menuSort;
    private final BufferedImageProvider icon;

    private final List<Integer> evalTrace;

    private DisplayConfig(Builder builder, List<Integer> evalTrace) {
        compact = Box.unwrap(builder.compact);
        hidden = Box.unwrap(builder.hidden);
        hideOverlay = Box.unwrap(builder.hideOverlay);
        highlightTile = Box.unwrap(builder.highlightTile);
        notify = Box.unwrap(builder.notify);
        showDespawn = Box.unwrap(builder.showDespawn);
        showLootbeam = Box.unwrap(builder.showLootbeam);
        showValue = Box.unwrap(builder.showValue);
        icon = Box.unwrap(builder.icon);
        backgroundColor = Box.unwrap(builder.backgroundColor);
        borderColor = Box.unwrap(builder.borderColor);
        lootbeamColor = Box.unwrap(builder.lootbeamColor);
        menuTextColor = Box.unwrap(builder.menuTextColor);
        textAccentColor = Box.unwrap(builder.textAccentColor);
        textColor = Box.unwrap(builder.textColor);
        tileFillColor = Box.unwrap(builder.tileFillColor);
        tileStrokeColor = Box.unwrap(builder.tileStrokeColor);
        fontType = Box.unwrap(builder.fontType);
        menuSort = Box.unwrap(builder.menuSort);
        sound = Box.unwrap(builder.sound);
        textAccent = Box.unwrap(builder.textAccent);
        this.evalTrace = evalTrace;
    }

    public SoundProvider getSound() {
        return !isHidden() ? sound : null;
    }

    public Color getLootbeamColor() {
        return lootbeamColor != null ? lootbeamColor : textColor;
    }

    public Color getTextColor() {
        return textColor != null ? textColor : Color.WHITE;
    }

    public BufferedImageProvider getIcon() {
        return isCompact() ? new BufferedImageProvider.CurrentItem() : icon;
    }

    public Color getMenuTextColor() {
        if (isHidden()) {
            return DEFAULT_MENU_TEXT_COLOR;
        }
        if (menuTextColor != null) {
            return menuTextColor;
        }
        return textColor != null && !textColor.equals(Color.WHITE) ? textColor : DEFAULT_MENU_TEXT_COLOR;
    }

    public int getMenuSort() {
        return menuSort != null ? menuSort : 0;
    }

    public Font getFont() {
        if (fontType == null || fontType == FontType.NORMAL) {
            return FontManager.getRunescapeSmallFont();
        }
        if (fontType == FontType.LARGER) {
            return FontManager.getRunescapeFont();
        }
        return FontManager.getRunescapeBoldFont();
    }

    public Color getTileStrokeColor() {
        return tileStrokeColor != null ? tileStrokeColor : textColor;
    }

    public boolean isHidden() {
        return hidden != null && hidden;
    }

    public boolean isShowLootbeam() {
        return !isHidden() && showLootbeam != null && showLootbeam;
    }

    public boolean isShowValue() {
        return showValue != null && showValue;
    }

    public boolean isShowDespawn() {
        return showDespawn != null && showDespawn;
    }

    public boolean isNotify() {
        return !isHidden() && notify != null && notify;
    }

    public boolean isHighlightTile() {
        return !isHidden() && highlightTile != null && highlightTile;
    }

    public boolean isHideOverlay() {
        return isHidden() || (hideOverlay != null && hideOverlay);
    }

    public boolean isCompact() {
        return compact != null && compact;
    }

    public static class Builder {
        private Box<Boolean> compact;
        private Box<Boolean> hidden;
        private Box<Boolean> hideOverlay;
        private Box<Boolean> highlightTile;
        private Box<Boolean> notify;
        private Box<Boolean> showDespawn;
        private Box<Boolean> showLootbeam;
        private Box<Boolean> showValue;
        private Box<BufferedImageProvider> icon;
        private Box<Color> backgroundColor;
        private Box<Color> borderColor;
        private Box<Color> lootbeamColor;
        private Box<Color> menuTextColor;
        private Box<Color> textAccentColor;
        private Box<Color> textColor;
        private Box<Color> tileFillColor;
        private Box<Color> tileStrokeColor;
        private Box<FontType> fontType;
        private Box<Integer> menuSort;
        private Box<SoundProvider> sound;
        private Box<TextAccent> textAccent;

        private Builder() {
        }

        public Builder compact(Boolean v) {
            compact = Box.wrap(v);
            return this;
        }

        public Builder hidden(Boolean v) {
            hidden = Box.wrap(v);
            return this;
        }

        public Builder hideOverlay(Boolean v) {
            hideOverlay = Box.wrap(v);
            return this;
        }

        public Builder highlightTile(Boolean v) {
            highlightTile = Box.wrap(v);
            return this;
        }

        public Builder notify(Boolean v) {
            notify = Box.wrap(v);
            return this;
        }

        public Builder showDespawn(Boolean v) {
            showDespawn = Box.wrap(v);
            return this;
        }

        public Builder showLootbeam(Boolean v) {
            showLootbeam = Box.wrap(v);
            return this;
        }

        public Builder showValue(Boolean v) {
            showValue = Box.wrap(v);
            return this;
        }

        public Builder icon(BufferedImageProvider v) {
            icon = Box.wrap(v);
            return this;
        }

        public Builder backgroundColor(Color v) {
            backgroundColor = Box.wrap(v);
            return this;
        }

        public Builder borderColor(Color v) {
            borderColor = Box.wrap(v);
            return this;
        }

        public Builder lootbeamColor(Color v) {
            lootbeamColor = Box.wrap(v);
            return this;
        }

        public Builder menuTextColor(Color v) {
            menuTextColor = Box.wrap(v);
            return this;
        }

        public Builder textAccentColor(Color v) {
            textAccentColor = Box.wrap(v);
            return this;
        }

        public Builder textColor(Color v) {
            textColor = Box.wrap(v);
            return this;
        }

        public Builder tileFillColor(Color v) {
            tileFillColor = Box.wrap(v);
            return this;
        }

        public Builder tileStrokeColor(Color v) {
            tileStrokeColor = Box.wrap(v);
            return this;
        }

        public Builder fontType(FontType v) {
            fontType = Box.wrap(v);
            return this;
        }

        public Builder menuSort(Integer v) {
            menuSort = Box.wrap(v);
            return this;
        }

        public Builder sound(SoundProvider v) {
            sound = Box.wrap(v);
            return this;
        }

        public Builder textAccent(TextAccent v) {
            textAccent = Box.wrap(v);
            return this;
        }

        public void apply(Builder other) {
            if (other.compact != null) {
                compact = other.compact;
            }
            if (other.hidden != null) {
                hidden = other.hidden;
            }
            if (other.hideOverlay != null) {
                hideOverlay = other.hideOverlay;
            }
            if (other.highlightTile != null) {
                highlightTile = other.highlightTile;
            }
            if (other.notify != null) {
                notify = other.notify;
            }
            if (other.showDespawn != null) {
                showDespawn = other.showDespawn;
            }
            if (other.showLootbeam != null) {
                showLootbeam = other.showLootbeam;
            }
            if (other.showValue != null) {
                showValue = other.showValue;
            }
            if (other.icon != null) {
                icon = other.icon;
            }
            if (other.backgroundColor != null) {
                backgroundColor = other.backgroundColor;
            }
            if (other.borderColor != null) {
                borderColor = other.borderColor;
            }
            if (other.lootbeamColor != null) {
                lootbeamColor = other.lootbeamColor;
            }
            if (other.menuTextColor != null) {
                menuTextColor = other.menuTextColor;
            }
            if (other.textAccentColor != null) {
                textAccentColor = other.textAccentColor;
            }
            if (other.textColor != null) {
                textColor = other.textColor;
            }
            if (other.tileFillColor != null) {
                tileFillColor = other.tileFillColor;
            }
            if (other.tileStrokeColor != null) {
                tileStrokeColor = other.tileStrokeColor;
            }
            if (other.fontType != null) {
                fontType = other.fontType;
            }
            if (other.menuSort != null) {
                menuSort = other.menuSort;
            }
            if (other.sound != null) {
                sound = other.sound;
            }
            if (other.textAccent != null) {
                textAccent = other.textAccent;
            }
        }

        public DisplayConfig build(List<Integer> evalTrace) {
            return new DisplayConfig(this, evalTrace);
        }
    }

    @AllArgsConstructor
    private static class Box<T> {
        public static <V> Box<V> wrap(V v) {
            return new Box<>(v);
        }

        public static <V> V unwrap(Box<V> b) {
            return b != null ? b.value : null;
        }

        private final T value;
    }
}

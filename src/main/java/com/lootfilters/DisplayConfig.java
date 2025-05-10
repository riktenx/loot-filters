package com.lootfilters;

import com.lootfilters.model.SoundProvider;
import com.lootfilters.model.BufferedImageProvider;
import com.lootfilters.rule.FontType;
import com.lootfilters.rule.TextAccent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.runelite.client.ui.FontManager;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class DisplayConfig {
    public static final Color DEFAULT_MENU_TEXT_COLOR = Color.decode("#ff9040");

    private final Color textColor;
    private final Color backgroundColor;
    private final Color borderColor;
    private final Boolean hidden;
    private final Boolean showLootbeam;
    private final Boolean showValue;
    private final Boolean compact;
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

    public DisplayConfig(Color textColor) {
        this.textColor = textColor;
        backgroundColor = null;
        borderColor = null;
        hidden = false;
        showLootbeam = false;
        showValue = false;
        compact = false;
        showDespawn = false;
        notify = false;
        textAccent = null;
        textAccentColor = null;
        lootbeamColor = null;
        fontType = null;
        menuTextColor = null;
        highlightTile = null;
        tileStrokeColor = null;
        tileFillColor = null;
        hideOverlay = null;
        sound = null;
        menuSort = null;
        icon = null;
    }
    // ideally this would be in EvalDisplayConfig which extends DisplayConfig but that's just more code tbh
    private final List<Integer> evalSource;

    public DisplayConfig(Color textColor) {
        this.textColor = textColor;
        backgroundColor = null;
        borderColor = null;
        hidden = false;
        showLootbeam = false;
        showValue = false;
        showDespawn = false;
        notify = false;
        textAccent = null;
        textAccentColor = null;
        lootbeamColor = null;
        fontType = null;
        menuTextColor = null;
        highlightTile = null;
        tileStrokeColor = null;
        tileFillColor = null;
        hideOverlay = null;
        sound = null;
        menuSort = null;
        icon = null;
        evalSource = null;
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

    public boolean isHidden() { return hidden != null && hidden; }
    public boolean isShowLootbeam() { return !isHidden() && showLootbeam != null && showLootbeam; }
    public boolean isShowValue() { return showValue != null && showValue; }
    public boolean isShowDespawn() { return showDespawn != null && showDespawn; }
    public boolean isNotify() { return !isHidden() && notify != null && notify; }
    public boolean isHighlightTile() { return !isHidden() && highlightTile != null && highlightTile; }
    public boolean isHideOverlay() { return isHidden() || (hideOverlay != null && hideOverlay); }

    public boolean isCompact() {
        return !isHidden() && compact != null && compact;
    }

    public DisplayConfig merge(DisplayConfig other) {
        var b = toBuilder();
        if (other.textColor != null) { b.textColor(other.textColor); }
        if (other.backgroundColor != null) { b.backgroundColor(other.backgroundColor); }
        if (other.borderColor != null) { b.borderColor(other.borderColor); }
        if (other.hidden != null) { b.hidden(other.hidden); }
        if (other.showLootbeam != null) { b.showLootbeam(other.showLootbeam); }
        if (other.showValue != null) { b.showValue(other.showValue); }
        if (other.compact != null) { b.compact(other.compact); }
        if (other.showDespawn != null) { b.showDespawn(other.showDespawn); }
        if (other.notify != null) { b.notify(other.notify); }
        if (other.textAccent != null) { b.textAccent(other.textAccent); }
        if (other.textAccentColor != null) { b.textAccentColor(other.textAccentColor); }
        if (other.lootbeamColor != null) { b.lootbeamColor(other.lootbeamColor); }
        if (other.fontType != null) { b.fontType(other.fontType); }
        if (other.menuTextColor != null) { b.menuTextColor(other.menuTextColor); }
        if (other.highlightTile != null) { b.highlightTile(other.highlightTile); }
        if (other.tileStrokeColor != null) { b.tileStrokeColor(other.tileStrokeColor); }
        if (other.tileFillColor != null) { b.tileFillColor(other.tileFillColor); }
        if (other.hideOverlay != null) { b.hideOverlay(other.hideOverlay); }
        if (other.sound != null) { b.sound(other.sound); }
        if (other.menuSort != null) { b.menuSort(other.menuSort); }
        if (other.icon != null) { b.icon(other.icon); }
        return b.build();
    }
}

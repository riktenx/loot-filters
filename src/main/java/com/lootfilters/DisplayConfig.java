package com.lootfilters;

import com.lootfilters.rule.FontType;
import com.lootfilters.rule.Sound;
import com.lootfilters.rule.TextAccent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.client.ui.FontManager;

import java.awt.Color;
import java.awt.Font;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@EqualsAndHashCode
public class DisplayConfig {
    @Builder.Default
    private final Color textColor = Color.WHITE;

    private final Color backgroundColor;
    private final Color borderColor;
    private final boolean hidden;
    private final boolean showLootbeam;
    private final boolean showValue;
    private final boolean showDespawn;
    private final boolean notify;
    private final TextAccent textAccent;
    private final Sound sound;
    private final Color textAccentColor;
    private final Color lootbeamColor;
    private final FontType fontType;

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
        sound = null;
        textAccentColor = null;
        lootbeamColor = null;
        fontType = null;
    }

    public Color getLootbeamColor() {
        return lootbeamColor != null ? lootbeamColor : textColor;
    }

    public Font getFont() {
        if (fontType == null || fontType == FontType.NORMAL) {
            return FontManager.getRunescapeSmallFont();
        }
        return FontManager.getRunescapeFont();
    }

}

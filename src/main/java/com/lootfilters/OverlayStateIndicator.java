package com.lootfilters;

import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

import javax.inject.Inject;
import java.awt.Color;

public class OverlayStateIndicator extends InfoBox {
    private final LootFiltersPlugin plugin;
    private final LootFiltersConfig config;

    @Inject
    public OverlayStateIndicator(LootFiltersPlugin plugin, LootFiltersConfig config) {
        super(Icons.OVERLAY_DISABLED, plugin);
        this.plugin = plugin;
        this.config = config;
        setPriority(InfoBoxPriority.LOW);
    }

    @Override
    public boolean render() {
        return config.hotkeyStateIndicator() && !plugin.isOverlayEnabled();
    }

    @Override
    public String getTooltip() {
        return "[Loot Filters]: The text overlay is currently <col=ff0000>disabled</col>.<br>" +
                "Tap <col=00ffff>" + config.hotkey() + "</col> once to re-enable it.<br><br>" +
                "<col=a0a0a0>You can disable this indicator in plugin config:<br>" +
                "Loot Filters -> Hotkey -> Overlay state indicator</col>";
    }

    @Override
    public String getText() {
        return "";
    }

    @Override
    public Color getTextColor() {
        return null;
    }
}

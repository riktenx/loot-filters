package com.lootfilters.debug;

import com.lootfilters.LootbeamIndex;
import com.lootfilters.TileItemIndex;
import com.lootfilters.model.DisplayConfigIndex;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class LootFiltersDebugOverlay extends Overlay {
    private static final Color DEBUG_BG = new Color(0, 0, 0, 0x80);

    private final LootFiltersDebugConfig config;
    private final TileItemIndex tileItemIndex;
    private final DisplayConfigIndex displayConfigIndex;
    private final LootbeamIndex lootbeamIndex;

    @Inject
    public LootFiltersDebugOverlay(
            LootFiltersDebugConfig config,
            TileItemIndex tileItemIndex,
            DisplayConfigIndex displayConfigIndex,
            LootbeamIndex lootbeamIndex
    ) {
        this.config = config;
        this.tileItemIndex = tileItemIndex;
        this.displayConfigIndex = displayConfigIndex;
        this.lootbeamIndex = lootbeamIndex;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D g) {
        if (!config.showOverlay()) {
            return null;
        }

        int itemCount = 0;
        int screenY = 96;
        for (var entry : tileItemIndex.entrySet()) {
            var tile = entry.getKey();
            var items = entry.getValue();

            var errs = "";
            var errno = 0;
            var loc = tile.getLocalLocation();
            if (loc == null) {
                ++errno;
                errs += "[LOC]";
            }
            if (tile.getItemLayer() == null) {
                ++errno;
                errs += "[IL]";
            }

            var coords = tile.getWorldLocation().getX() + ", " + tile.getWorldLocation().getY();
            var sz = items.size();
            g.setColor(errno > 0 ? Color.RED : Color.WHITE);
            g.drawString(coords + " " + sz + " " + errs, 0, screenY);

            itemCount += sz;
            screenY += 16;
        }
        g.setColor(DEBUG_BG);
        g.fillRect(0, 18, 96, 64);
        g.setColor(Color.WHITE);
        g.drawString("items: " + itemCount + "," + tileItemIndex.pointIndexSize(), 0, 32);
        g.drawString("displays: " + displayConfigIndex.size(), 0, 64);
        g.drawString("lootbeams: " + lootbeamIndex.size(), 0, 48);

        return null;
    }
}

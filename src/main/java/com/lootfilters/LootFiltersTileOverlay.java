package com.lootfilters;

import com.lootfilters.model.PluginTileItem;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

import static net.runelite.api.Perspective.getCanvasTilePoly;

/**
 * Draws tile highlights only. Item labels are drawn by {@link LootFiltersTextOverlay}, which has a higher priority so
 * labels always render on top of tiles.
 */
public class LootFiltersTileOverlay extends Overlay {
    private final Client client;
    private final LootFiltersPlugin plugin;

    @Inject
    public LootFiltersTileOverlay(Client client, LootFiltersPlugin plugin) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(PRIORITY_DEFAULT);
        this.client = client;
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D g) {
        if (!plugin.isOverlayEnabled()) {
            return null;
        }

        var player = client.getLocalPlayer();
        for (var entry : plugin.getTileItemIndex().entrySet()) {
            var tile = entry.getKey();
            if (tile.getItemLayer() == null) {
                continue;
            }
            if (!LootFiltersTextOverlay.inRenderRange(client, player, tile)) {
                continue;
            }

            highlightTiles(g, tile, entry.getValue());
        }
        return null;
    }

    private void highlightTiles(Graphics2D g, Tile tile, List<PluginTileItem> items) {
        if (tile.getLocalLocation() == null || tile.getPlane() != client.getTopLevelWorldView().getPlane()) {
            return;
        }

        for (var item : items) {
            var match = plugin.getDisplayIndex().get(item);
            if (match.isHighlightTile()) {
                highlightTile(g, tile, match);
            }
        }
    }

    private void highlightTile(Graphics2D g, Tile tile, DisplayConfig display) {
        var poly = getCanvasTilePoly(client, tile.getLocalLocation(), tile.getItemLayer().getHeight());
        if (poly == null) {
            return;
        }

        var origStroke = g.getStroke();
        g.setColor(display.getTileStrokeColor());
        g.setStroke(new BasicStroke(2));
        g.draw(poly);
        if (display.getTileFillColor() != null) {
            g.setColor(display.getTileFillColor());
            g.fill(poly);
        }
        g.setStroke(origStroke);
    }
}

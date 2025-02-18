package com.lootfilters;

import com.lootfilters.model.DespawnTimerType;
import com.lootfilters.model.DualValueDisplayType;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.util.TextComponent;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import static com.lootfilters.util.TextUtil.abbreviate;
import static com.lootfilters.util.TextUtil.abbreviateValue;
import static com.lootfilters.util.TextUtil.withParentheses;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static net.runelite.api.Perspective.getCanvasTextLocation;

public class LootFiltersOverlay extends Overlay {
    private static final int Z_STACK_OFFSET = 16;
    private static final int BOX_PAD = 2;
    private static final Color COLOR_HIDDEN = Color.GRAY.brighter();

    private final Client client;
    private final LootFiltersPlugin plugin;
    private final LootFiltersConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    public LootFiltersOverlay(Client client, LootFiltersPlugin plugin, LootFiltersConfig config) {
        setPosition(OverlayPosition.DYNAMIC);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D g) {
        if (plugin.isDebugEnabled()) {
            renderDebugOverlay(g);
        }

        if (!plugin.isOverlayEnabled()) {
            return null;
        }

        var activeFilter = plugin.getActiveFilter();
        var mouse = client.getMouseCanvasPosition();
        var hoveredItem = -1;

        for (var entry : plugin.getTileItemIndex().entrySet()) {
            var items = entry.getValue();
            var itemCounts = items.stream()
                    .collect(groupingBy(TileItem::getId, counting()));

            var tile = entry.getKey();
            var currentOffset = 0;
            var rendered = new ArrayList<Integer>();
            for (var item : items) {
                if (rendered.contains(item.getId())) {
                    continue;
                }
                rendered.add(item.getId());

                var count = itemCounts.get(item.getId());

                var match = activeFilter.findMatch(plugin, item);
                if (match == null) {
                    continue;
                }

                var overrideHidden = plugin.isHotkeyActive() && config.hotkeyShowHiddenItems();
                if (match.isHidden() && !overrideHidden) {
                    continue;
                }

                var loc = LocalPoint.fromWorld(client.getTopLevelWorldView(), tile.getWorldLocation());
                if (loc == null) {
                    continue;
                }
                if (tile.getItemLayer() == null) {
                    continue;
                }

                g.setFont(match.getFont());

                var displayText = buildDisplayText(item, count, match);
                var textPoint = getCanvasTextLocation(client, g, loc, displayText, tile.getItemLayer().getHeight() + Z_STACK_OFFSET);
                if (textPoint == null) {
                    continue;
                }

                var fm = g.getFontMetrics(match.getFont());
                var textWidth = fm.stringWidth(displayText);
                var textHeight = fm.getHeight();

                var text = new TextComponent();
                text.setText(displayText);
                text.setColor(match.isHidden() ? COLOR_HIDDEN : match.getTextColor());
                text.setPosition(new Point(textPoint.getX(), textPoint.getY() - currentOffset));
                if (match.getTextAccentColor() != null) {
                    text.setAccentColor(match.getTextAccentColor());
                }
                if (match.getTextAccent() != null ) {
                    text.setTextAccent(match.getTextAccent());
                }

                var boundingBox = new Rectangle(
                        textPoint.getX() - BOX_PAD, textPoint.getY() - currentOffset - textHeight - BOX_PAD,
                        textWidth + 2 * BOX_PAD, textHeight + 2 * BOX_PAD
                );

                if (match.getBackgroundColor() != null) {
                    g.setColor(match.getBackgroundColor());
                    g.fillRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
                }
                if (match.getBorderColor() != null) {
                    g.setColor(match.getBorderColor());
                    g.drawRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
                }
                if (plugin.isHotkeyActive() && boundingBox.contains(mouse.getX(), mouse.getY())) {
                    hoveredItem = item.getId();

                    g.setColor(match.isHidden() ? COLOR_HIDDEN : Color.WHITE);
                    g.drawRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
                }

                text.render(g);

                if (match.isShowDespawn()) {
                    renderDespawnTimer(g, item, textPoint, textWidth, fm.getHeight(), currentOffset);
                }

                currentOffset += textHeight + BOX_PAD + 3;
            }
        }

        plugin.setHoveredItem(hoveredItem);
        return null;
    }

    private Color getDespawnTextColor(PluginTileItem item) {
        if (item.getDespawnTime() - client.getTickCount() < 100) {
            return Color.RED;
        }
        if (!item.isPrivate() && item.getVisibleTime() <= client.getTickCount()) {
            return Color.YELLOW;
        }
        return Color.GREEN;
    }

    private String buildDisplayText(TileItem item, long unstackedCount, DisplayConfig display) {
        var text = itemManager.getItemComposition(item.getId()).getName();

        if (item.getQuantity() > 1) {
            text += " (" + abbreviate(item.getQuantity()) + ")";
        } else if (unstackedCount > 1) {
            text += " x" + unstackedCount; // we want these to be visually different
        }

        var isMoney = item.getId() == ItemID.COINS_995 || item.getId() == ItemID.PLATINUM_TOKEN; // value is redundant
        if (isMoney || !display.isShowValue()) {
            return text;
        }

        var ge = itemManager.getItemPrice(item.getId());
        var ha = itemManager.getItemComposition(item.getId()).getHaPrice();
        switch (config.valueDisplayType()) {
            case HIGHEST:
                return ge == 0 && ha == 0 ? text
                        : text + " " + formatDualValueText(config.dualValueDisplay(), ge, ha, false);
            case GE:
                return ge == 0 ? text : String.format("%s (%s)", text, abbreviateValue(ge));
            case HA:
                return ha == 0 ? text : String.format("%s (%s)", text, abbreviateValue(ha));
            default: // BOTH
                return ge == 0 && ha == 0 ? text
                        : text + " " + formatDualValueText(config.dualValueDisplay(), ge, ha, true);
        }
    }

    private String formatDualValueText(DualValueDisplayType displayType, int geValue, int haValue, boolean showBoth) {
        var geFmt = abbreviateValue(geValue);
        var haFmt = abbreviateValue(haValue);
        var geFmtStr = displayType == DualValueDisplayType.COMPACT ? "%s" : "(GE: %s)";
        var haFmtStr = displayType == DualValueDisplayType.COMPACT ? "*%s" : "(HA: %s)";

        if (!showBoth) {
            var text = geValue >= haValue ? String.format(geFmtStr, geFmt) : String.format(haFmtStr, haFmt);
            return displayType == DualValueDisplayType.COMPACT ? withParentheses(text) : text;
        }

        var parts = new ArrayList<String>();
        if (geValue > 0) {
            parts.add(String.format(geFmtStr, geFmt));
        }
        if (haValue > 0) {
            parts.add(String.format(haFmtStr, haFmt));
        }
        var text = String.join(displayType == DualValueDisplayType.COMPACT ? "/" : " ", parts);
        return displayType == DualValueDisplayType.COMPACT ? withParentheses(text) : text;
    }

    private void renderDebugOverlay(Graphics2D g) {
        int itemCount = 0;
        int screenY = 64;
        for (var entry : plugin.getTileItemIndex().entrySet()) {
            var tile = entry.getKey();
            var items = entry.getValue();

            var errs = "";
            var errno = 0;
            var loc = LocalPoint.fromWorld(client.getTopLevelWorldView(), tile.getWorldLocation());
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
            g.drawString(coords+" "+sz+" "+errs, 0, screenY);

            itemCount += sz;
            screenY += 16;
        }
        g.setColor(Color.WHITE);
        g.drawString("items: " + itemCount + "," + plugin.getTileItemIndex().pointIndexSize(), 0, 32);
        g.drawString("lootbeams: " + plugin.getLootbeamIndex().size(), 0, 48);
    }

    private void renderDespawnTimer(Graphics2D g, PluginTileItem item, net.runelite.api.Point textPoint, int textWidth, int textHeight, int yOffset) {
        var ticksRemaining = item.getDespawnTime() - client.getTickCount();
        if (ticksRemaining < 0) { // doesn't despawn
            return;
        }
        if (config.despawnThreshold() > 0 && ticksRemaining > config.despawnThreshold()) {
            return;
        }

        var type = config.despawnTimerType();
        if (type == DespawnTimerType.TICKS || type == DespawnTimerType.SECONDS) {
            var text = new TextComponent();
            text.setText(type == DespawnTimerType.TICKS
                    ? Integer.toString(ticksRemaining)
                    : String.format("%.1f", (Duration.between(Instant.now(), item.getDespawnInstant())).toMillis() / 1000f));
            text.setColor(getDespawnTextColor(item));
            text.setPosition(new Point(textPoint.getX() + textWidth + 2 + 1, textPoint.getY() - yOffset));
            text.render(g);
        } else {
            var timer = new ProgressPieComponent();
            var total = item.getDespawnTime() - item.getSpawnTime();
            var remaining = item.getDespawnTime() - plugin.getClient().getTickCount();
            var radius = textHeight / 2;
            timer.setPosition(new net.runelite.api.Point(textPoint.getX() + textWidth + 2 + 1 + radius,
                    textPoint.getY() - yOffset - radius));
            timer.setProgress(remaining / (double) total);
            timer.setDiameter(textHeight);
            timer.setFill(getDespawnTextColor(item));
            timer.setBorderColor(getDespawnTextColor(item));
            timer.render(g);
        }
    }
}

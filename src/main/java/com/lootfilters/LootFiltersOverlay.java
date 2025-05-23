package com.lootfilters;

import com.lootfilters.model.BufferedImageProvider;
import com.lootfilters.model.DespawnTimerType;
import com.lootfilters.model.DualValueDisplayType;
import com.lootfilters.model.FontMode;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.model.ValueDisplayType;
import com.lootfilters.util.TextComponent;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.lootfilters.util.TextUtil.abbreviate;
import static com.lootfilters.util.TextUtil.abbreviateValue;
import static com.lootfilters.util.TextUtil.withParentheses;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static net.runelite.api.Perspective.getCanvasTextLocation;
import static net.runelite.api.Perspective.getCanvasTilePoly;

public class LootFiltersOverlay extends Overlay {
    private static final int BOX_PAD = 2;
    private static final int CLICKBOX_SIZE = 8;
    private static final int TIMER_RADIUS = 5;

    private final Client client;
    private final LootFiltersPlugin plugin;
    private final LootFiltersConfig config;

    @Inject
    public LootFiltersOverlay(Client client, LootFiltersPlugin plugin, LootFiltersConfig config) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(config.overlayPriority().getValue());
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

        var mouse = client.getMouseCanvasPosition();
        var hoveredItem = -1;
        var hoveredHide = new AtomicInteger(-1);
        var hoveredHighlight = new AtomicInteger(-1);

        for (var entry : plugin.getTileItemIndex().entrySet()) { // all tile draws have to go first so text is on top
            highlightTiles(g, entry.getKey(), entry.getValue());
        }
        for (var entry : plugin.getTileItemIndex().entrySet()) {
            var items = entry.getValue();
            var itemCounts = items.stream()
                    .collect(groupingBy(it -> new OverlayKey(it.getId(), it.getQuantity()), counting()));

            var tile = entry.getKey();
            var currentOffset = 0;
            var rendered = new ArrayList<OverlayKey>();
            for (var item : items) {
                var leftOffset = 0;

                var renderKey = new OverlayKey(item.getId(), item.getQuantity());
                if (rendered.contains(renderKey)) {
                    continue;
                }
                rendered.add(renderKey);

                var count = itemCounts.get(renderKey);

                var match = plugin.getDisplayIndex().get(item);
                if (match == null) {
                    continue;
                }

                var overrideHidden = plugin.isHotkeyActive() && config.hotkeyShowHiddenItems();
                if (match.isHideOverlay() && !overrideHidden) {
                    continue;
                }

                var loc = LocalPoint.fromWorld(client.getTopLevelWorldView(), tile.getWorldLocation());
                if (loc == null) {
                    continue;
                }
                if (tile.getItemLayer() == null) {
                    continue;
                }

                if (config.fontMode() == FontMode.PLUGIN) {
                    g.setFont(match.getFont());
                } // otherwise we don't have to do anything, the font is already set

                var displayText = buildDisplayText(item, count, match);
                var textPoint = getCanvasTextLocation(client, g, loc, displayText, tile.getItemLayer().getHeight() + config.overlayZOffset());
                if (textPoint == null) {
                    continue;
                }

                var fm = g.getFontMetrics(g.getFont());
                var textWidth = fm.stringWidth(displayText);
                var textHeight = fm.getHeight();

                var text = new TextComponent();
                text.setText(displayText);
                text.setColor(match.isHidden() ? config.hiddenColor() : match.getTextColor());
                text.setPosition(new Point(textPoint.getX(), textPoint.getY() - currentOffset));
                if (match.getTextAccentColor() != null) {
                    text.setAccentColor(match.getTextAccentColor());
                }
                if (match.getTextAccent() != null) {
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

                    g.setColor(match.isHidden() ? config.hiddenColor() : Color.WHITE);
                    g.drawRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
                }
                if (config.hotkeyShowClickboxes() && plugin.isHotkeyActive()) {
                    renderClickboxes(g, boundingBox, item, match, hoveredHide::set, hoveredHighlight::set);
                }

                text.render(g);

                if (match.getIcon() != null) {
                    var cacheKey = match.getIcon().getCacheKey(item);
                    var d = renderIcon(g, cacheKey, textPoint, currentOffset);
                    leftOffset += d.width;
                }
                if (match.isShowDespawn() || plugin.isHotkeyActive()) {
                    var type = plugin.isHotkeyActive() ? DespawnTimerType.PIE : config.despawnTimerType();
                    renderDespawnTimer(g, type, item, textPoint, textWidth, fm.getHeight(), currentOffset, leftOffset);
                }

                currentOffset += textHeight + BOX_PAD + 3;
            }
        }

        plugin.setHoveredItem(hoveredItem);
        plugin.setHoveredHide(hoveredHide.get());
        plugin.setHoveredHighlight(hoveredHighlight.get());
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

    private String buildDisplayText(PluginTileItem item, long unstackedCount, DisplayConfig display) {
        var text = item.getName();

        // BOTH of these can be true, we want them to be visually different either way
        if (item.getQuantity() > 1) {
            text += " (" + abbreviate(item.getQuantity()) + ")";
        }
        if (unstackedCount > 1) {
            text += " x" + unstackedCount;
        }

        var isMoney = item.getId() == ItemID.COINS_995 || item.getId() == ItemID.PLATINUM_TOKEN; // value is redundant
        var showBecauseHotkey = config.hotkeyShowValues() && plugin.isHotkeyActive();
        if (isMoney || !(display.isShowValue() || showBecauseHotkey)) {
            return text;
        }

        var ge = item.getGePrice() * item.getQuantity();
        var ha = item.getHaPrice() * item.getQuantity();
        switch (showBecauseHotkey ? ValueDisplayType.BOTH : config.valueDisplayType()) {
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
        g.drawString("debug", 0, g.getFontMetrics().getHeight());
        int itemCount = 0;
        int screenY = 96;
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
            g.drawString(coords + " " + sz + " " + errs, 0, screenY);


            itemCount += sz;
            screenY += 16;
        }
        g.setColor(Color.WHITE);
        g.drawString("items: " + itemCount + "," + plugin.getTileItemIndex().pointIndexSize(), 0, 32);
        g.drawString("lootbeams: " + plugin.getLootbeamIndex().size(), 0, 48);
        g.drawString("displays: " + plugin.getDisplayIndex().size(), 0, 64);
        g.drawString("audio: " + plugin.getQueuedAudio().size() + ", icon: " + plugin.getIconIndex().size(), 0, 80);
    }

    private void renderDespawnTimer(Graphics2D g, DespawnTimerType type, PluginTileItem item, net.runelite.api.Point textPoint, int textWidth, int textHeight, int yOffset, int leftOffset) {
        var ticksRemaining = item.getDespawnTime() - client.getTickCount();
        if (ticksRemaining < 0) { // doesn't despawn
            return;
        }
        if (config.despawnThreshold() > 0 && ticksRemaining > config.despawnThreshold()) {
            return;
        }

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
            timer.setPosition(new net.runelite.api.Point(textPoint.getX() - TIMER_RADIUS - BOX_PAD - 2 - leftOffset,
                    textPoint.getY() - yOffset - TIMER_RADIUS));
            timer.setProgress(remaining / (double) total);
            timer.setDiameter(TIMER_RADIUS * 2);
            timer.setFill(getDespawnTextColor(item));
            timer.setBorderColor(getDespawnTextColor(item));
            timer.render(g);
        }
    }

    private void renderClickboxes(Graphics2D g, Rectangle textBox, TileItem item, DisplayConfig display,
                                  Consumer<Integer> onHoverHide, Consumer<Integer> onHoverHighlight) {
        var y = textBox.y + (textBox.height - CLICKBOX_SIZE) / 2;
        var hide = new Rectangle(textBox.x + textBox.width + 2, y, CLICKBOX_SIZE, CLICKBOX_SIZE);
        var show = new Rectangle(textBox.x + textBox.width + 4 + CLICKBOX_SIZE, y, CLICKBOX_SIZE, CLICKBOX_SIZE);

        var mouse = client.getMouseCanvasPosition();
        if (hide.contains(mouse.getX(), mouse.getY())) {
            onHoverHide.accept(item.getId());
            g.setColor(Color.RED);
        } else {
            g.setColor(display.isHidden() ? config.hiddenColor() : display.getTextColor());
        }
        g.drawRect(hide.x, hide.y, hide.width, hide.height);
        g.setColor(Color.WHITE);
        g.drawLine(hide.x + 2, hide.y + hide.height / 2, hide.x + hide.width - 2, hide.y + hide.height / 2);

        if (show.contains(mouse.getX(), mouse.getY())) {
            onHoverHighlight.accept(item.getId());
            g.setColor(Color.GREEN);
        } else {
            g.setColor(display.isHidden() ? config.hiddenColor() : display.getTextColor());
        }
        g.drawRect(show.x, show.y, show.width, show.height);
        g.setColor(Color.WHITE);
        g.drawLine(show.x + 2, show.y + show.height / 2, show.x + show.width - 2, show.y + show.height / 2);
        g.drawLine(show.x + show.width / 2, show.y + 2, show.x + show.width / 2, show.y + show.height - 2);
    }

    private void highlightTiles(Graphics2D g, Tile tile, List<PluginTileItem> items) {
        if (tile.getLocalLocation() == null || tile.getPlane() != client.getTopLevelWorldView().getPlane()) {
            return;
        }

        for (var item : items) {
            var match = plugin.getDisplayIndex().get(item);
            if (match != null && match.isHighlightTile()) {
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

    private Dimension renderIcon(Graphics2D g, BufferedImageProvider.CacheKey cacheKey, net.runelite.api.Point textPoint, int yOffset) {
        var image = plugin.getIconIndex().get(cacheKey);
        if (image == null) {
            return new Dimension(0, 0);
        }

        var fontHeight = g.getFontMetrics().getHeight();
        var x = textPoint.getX() - image.getWidth() - BOX_PAD - 1;
        var y = textPoint.getY() - fontHeight - yOffset + (fontHeight - image.getHeight()) / 2;
        g.drawImage(image, x, y, null);
        return new Dimension(image.getWidth() + BOX_PAD, image.getHeight());
    }

    @Value
    private static class OverlayKey {
        int id, quantity;
    }
}

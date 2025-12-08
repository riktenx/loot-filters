package com.lootfilters;

import com.lootfilters.model.BufferedImageProvider;
import com.lootfilters.model.DespawnTimerType;
import com.lootfilters.model.DualValueDisplayType;
import com.lootfilters.model.FontMode;
import com.lootfilters.model.IconPosition;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.model.ValueDisplayType;
import com.lootfilters.util.TextComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.lootfilters.util.TextUtil.abbreviate;
import static com.lootfilters.util.TextUtil.abbreviateValue;
import static com.lootfilters.util.TextUtil.withParentheses;
import static net.runelite.api.Perspective.getCanvasTilePoly;
import static net.runelite.client.ui.FontManager.getRunescapeSmallFont;

public class LootFiltersOverlay extends Overlay {
    private static final int BOX_PAD = 2;
    private static final int CLICKBOX_SIZE = 8;
    private static final int TIMER_RADIUS = 5;
    private static final int DEFAULT_IMAGE_HEIGHT = 32;
    private static final int DEFAULT_IMAGE_WIDTH = 36;
    private static final int MAX_DISTANCE = 24 * 128; // in LocalPoint units

    private final Client client;
    private final LootFiltersPlugin plugin;
    private final LootFiltersConfig config;

    @Inject
    public LootFiltersOverlay(Client client, LootFiltersPlugin plugin, LootFiltersConfig config) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        // we want to explicitly draw above overlays of plugins on the ABOVE_SCENE layer that do the "clipping" logic
        //   - Improved Tile Indicators is explicitly set to 0.6f
        //   - Better NPC Highlight is defaulted at 0.25f
        setPriority(PRIORITY_HIGH);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D g) {
        if (!plugin.isOverlayEnabled()) {
            return null;
        }

        var mouse = client.getMouseCanvasPosition();
        var hoveredItem = new AtomicInteger(-1);
        var hoveredHide = new AtomicInteger(-1);
        var hoveredHighlight = new AtomicInteger(-1);

        var player = client.getLocalPlayer();

        for (var entry : plugin.getTileItemIndex().entrySet()) { // all tile draws have to go first so text is on top
            if (inRenderRange(player, entry.getKey())) {
                highlightTiles(g, entry.getKey(), entry.getValue());
            }
        }
        for (var entry : plugin.getTileItemIndex().entrySet()) {
            var tile = entry.getKey();
            if (!inRenderRange(player, tile)) {
                continue;
            }

            var items = createItemCollection(entry.getValue());
            var currentOffset = 0;
            var compactRowPosition = 0;


            var remainingCompactMatches = items.get(true).size();

            for (var item : items.get(true)) {
                var effectiveRowLength = config.compactRenderRowLength() > remainingCompactMatches ? remainingCompactMatches : config.compactRenderRowLength();
                var textHeight = renderCompact(item.getOverlayKey().getDisplayConfig(), g, item.getFirstItem(), tile,
                        item.getCounts().getCount(), item.getCounts().getQuantity(), currentOffset, mouse,
                        hoveredItem::set, compactRowPosition, effectiveRowLength);
                if (textHeight == -1) {
                    continue; // didn't render
                }

                if (compactRowPosition >= config.compactRenderRowLength() - 1) {
                    compactRowPosition = 0;
                    currentOffset += textHeight + BOX_PAD + 3;
                    remainingCompactMatches -= config.compactRenderRowLength();
                } else {
                    compactRowPosition++;
                }
            }

            if (!items.get(true).isEmpty()) {
                currentOffset += config.compactRenderSize() / 2;
                if (compactRowPosition > 0) {
                    currentOffset += config.compactRenderSize() + BOX_PAD + 2;
                }
            }

            // non-compact

            for (var item : items.get(false)) {
                var leftOffset = 0;
                var match = item.getOverlayKey().getDisplayConfig();

                var overrideHidden = plugin.isHotkeyActive() && config.hotkeyShowHiddenItems();
                if (match.isHideOverlay() && !overrideHidden) {
                    continue;
                }

                // we look ahead at the icon width (if there is one) because it will affect text offset further down
                // depending on the configured icon position
                var iconWidth = 0;
                if (match.getIcon() != null) {
                    var key = match.getIcon().getCacheKey(item.getFirstItem());
                    var icon = plugin.getIconIndex().get(key);
                    if (icon != null) {
                        iconWidth = icon.getWidth();
                    }
                }

                var loc = LocalPoint.fromWorld(tile.getItemLayer().getWorldView(), tile.getWorldLocation());
                if (loc == null) {
                    continue;
                }
                if (tile.getItemLayer() == null) {
                    continue;
                }

                if (config.fontMode() == FontMode.PLUGIN) {
                    g.setFont(match.getFont());
                } // otherwise we don't have to do anything, the font is already set

                var displayText = buildDisplayText(item.getFirstItem(), item.getCounts().getCount(), item.getCounts().getQuantity(), match);
                var textPoint = getCanvasTextLocation(client, g, loc, displayText, tile.getItemLayer().getHeight() + config.overlayZOffset());
                if (textPoint == null) {
                    continue;
                }

                // move the text HALF of the image width to the right
                if (config.iconPosition() == IconPosition.INSIDE) {
                    textPoint.x += iconWidth / 2;
                }

                var fm = g.getFontMetrics(g.getFont());
                var textWidth = fm.stringWidth(displayText);
                var textHeight = fm.getHeight();

                var text = new TextComponent();
                text.setText(displayText);
                text.setColor(match.isHidden() ? config.hiddenColor() : match.getTextColor());
                text.setPosition(new Point(textPoint.x, textPoint.y - currentOffset));
                if (match.getTextAccentColor() != null) {
                    text.setAccentColor(match.getTextAccentColor());
                }
                if (match.getTextAccent() != null) {
                    text.setTextAccent(match.getTextAccent());
                }

                var boundingBox = new Rectangle(
                        textPoint.x - BOX_PAD, textPoint.y - currentOffset - textHeight - BOX_PAD,
                        textWidth + 2 * BOX_PAD, textHeight + 2 * BOX_PAD
                );

                if (config.iconPosition() == IconPosition.INSIDE) { // correct for previous image offset
                    boundingBox.x -= iconWidth;
                    boundingBox.width += iconWidth;
                }

                if (match.getBackgroundColor() != null) {
                    g.setColor(match.getBackgroundColor());
                    g.fillRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
                }
                if (match.getBorderColor() != null) {
                    g.setColor(match.getBorderColor());
                    g.drawRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
                }
                if (plugin.isHotkeyActive() && boundingBox.contains(mouse.getX(), mouse.getY())) {
                    hoveredItem.set(item.getOverlayKey().getId());
                    g.setColor(match.isHidden() ? config.hiddenColor() : Color.WHITE);
                    g.drawRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
                }
                if (config.hotkeyShowClickboxes() && plugin.isHotkeyActive()) {
                    renderClickboxes(g, boundingBox, item.getFirstItem(), match, hoveredHide::set, hoveredHighlight::set);
                }

                text.render(g);

                if (match.getIcon() != null) {
                    var cacheKey = match.getIcon().getCacheKey(item.getFirstItem());
                    var d = renderIcon(g, cacheKey, textPoint, currentOffset);
                    leftOffset += d.width;
                }
                if (match.isShowDespawn() || plugin.isHotkeyActive()) {
                    var type = plugin.isHotkeyActive() ? DespawnTimerType.PIE : config.despawnTimerType();
                    renderDespawnTimer(g, type, item.getFirstItem(), textPoint, textWidth, currentOffset, leftOffset, false);
                }

                currentOffset += textHeight + BOX_PAD + 3;
            }
        }

        plugin.setHoveredItem(hoveredItem.get());
        plugin.setHoveredHide(hoveredHide.get());
        plugin.setHoveredHighlight(hoveredHighlight.get());
        return null;
    }

    private int renderCompact(DisplayConfig display, Graphics2D g, PluginTileItem item, Tile tile, long count, long quantity,
                              int currentOffset, net.runelite.api.Point mouse, Consumer<Integer> onHoveredItem, int rowOffset,
                              int rowSize) {
        var overrideHidden = plugin.isHotkeyActive() && config.hotkeyShowHiddenItems();
        if (display.isHideOverlay() && !overrideHidden) {
            return -1;
        }

        var loc = LocalPoint.fromWorld(tile.getItemLayer().getWorldView(), tile.getWorldLocation());
        if (loc == null) {
            return -1;
        }
        if (tile.getItemLayer() == null) {
            return -1;
        }

        g.setFont(getRunescapeSmallFont()); // force the small font in compact mode because it's small

        var fm = g.getFontMetrics(g.getFont());
        var boxHeight = config.compactRenderSize();
        var boxWidth = Math.round(DEFAULT_IMAGE_WIDTH * boxHeight / (float) DEFAULT_IMAGE_HEIGHT);

        var image = plugin.getIconIndex().get(display.getIcon().getCacheKey(item, config.compactRenderSize()));
        if (image == null) {
            return -1;
        }

        boxHeight = image.getHeight();
        boxWidth = image.getWidth();
        var fullBoxWidth = boxWidth + 4;
        var imagePoint = getCanvasImageLocation(client, loc, image, tile.getItemLayer().getHeight() + config.overlayZOffset());
        if (imagePoint == null) {
            return -1;
        }
        // item square size- 1 px padding outside the bounding box, 1 px inside, item width, 1 px, 1 px.
        var boundingBox = new Rectangle(
                imagePoint.x + (fullBoxWidth) * rowOffset - Math.round(fullBoxWidth * ((rowSize - 1) / 2f)), imagePoint.y - currentOffset - boxHeight,
                boxWidth + 2, boxHeight + 2
        );

        if (display.getBackgroundColor() != null) {
            g.setColor(display.getBackgroundColor());
            g.fillRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
        }
        if (display.getBorderColor() != null) {
            g.setColor(display.getBorderColor());
            g.drawRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
        }
        if (plugin.isHotkeyActive() && boundingBox.contains(mouse.getX(), mouse.getY())) {
            onHoveredItem.accept(item.getId());

            g.setColor(display.isHidden() ? config.hiddenColor() : Color.WHITE);
            g.drawRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height);
        }
        var displayText = "";
        if (quantity > 1) {
            if (quantity < 1000) {
                displayText += quantity;
            } else if (quantity < 99500) {
                displayText += String.format("%.0fK", (float) quantity / 1e3);
            } else {
                displayText += "Lots!";
            }
        }

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, display.getTextColor().getAlpha() / 255f));
        g.drawImage(image, boundingBox.x + BOX_PAD / 2, boundingBox.y + BOX_PAD / 2, null);
        if (display.isShowDespawn() || plugin.isHotkeyActive()) {
            var type = plugin.isHotkeyActive() ? DespawnTimerType.PIE : config.despawnTimerType();
            renderDespawnTimer(g, type, item, new Point(boundingBox.x + BOX_PAD / 2, boundingBox.y + boundingBox.height), boxWidth + 2, 0, 0, true);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
        if (count > 1) {
            displayText += "x" + count;
        }
        var text = new TextComponent();
        text.setText(displayText);
        text.setColor(display.isHidden() ? config.hiddenColor() : display.getTextColor());
        text.setPosition(new Point(boundingBox.x + BOX_PAD / 2, boundingBox.y + fm.getHeight() + BOX_PAD / 2));
        text.render(g);

        return image.getHeight();
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

    private String buildDisplayText(PluginTileItem item, int unstackedCount, int quantity, DisplayConfig display) {
        var text = item.getName();

        // BOTH of these can be true, we want them to be visually different either way
        if (quantity > 1) {
            text += " (" + abbreviate(quantity) + ")";
        }
        if (unstackedCount > 1) {
            text += " x" + unstackedCount;
        }

        var isMoney = item.getId() == ItemID.COINS || item.getId() == ItemID.PLATINUM; // value is redundant
        var showBecauseHotkey = config.hotkeyShowValues() && plugin.isHotkeyActive();
        if (isMoney || !(display.isShowValue() || showBecauseHotkey)) {
            return text;
        }

        var ge = item.getGePrice() * quantity;
        var ha = item.getHaPrice() * quantity;
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

    private void renderDespawnTimer(Graphics2D g, DespawnTimerType type, PluginTileItem
            item, Point textPoint, int textWidth, int yOffset, int leftOffset, boolean compact) {
        var ticksRemaining = item.getDespawnTime() - client.getTickCount();
        if (ticksRemaining < 0) { // doesn't despawn
            return;
        }
        if (!plugin.isHotkeyActive() && config.despawnThreshold() > 0 && ticksRemaining > config.despawnThreshold()) {
            return;
        }

        if (compact) { // bar
            var total = item.getDespawnTime() - item.getSpawnTime();
            var remaining = item.getDespawnTime() - plugin.getClient().getTickCount();
            var bar = new Rectangle(textPoint.x, textPoint.y - 3, textWidth * remaining / total, 3);
            g.setColor(getDespawnTextColor(item));
            g.fillRect(bar.x, bar.y, bar.width, bar.height);
        } else if (type == DespawnTimerType.TICKS || type == DespawnTimerType.SECONDS) {
            var text = new TextComponent();
            var timeRounding = "%.1f";
            var displyString = type == DespawnTimerType.TICKS
                    ? Integer.toString(ticksRemaining)
                    : String.format(timeRounding, (Duration.between(Instant.now(), item.getDespawnInstant())).toMillis() / 1000f);
            text.setText(displyString);
            text.setColor(getDespawnTextColor(item));
            text.setPosition(new Point(textPoint.x + textWidth + 2 + 1, textPoint.y - yOffset));
            text.render(g);
        } else { // pie
            var timer = new ProgressPieComponent();
            var total = item.getDespawnTime() - item.getSpawnTime();
            var remaining = item.getDespawnTime() - plugin.getClient().getTickCount();
            timer.setPosition(new net.runelite.api.Point(textPoint.x - TIMER_RADIUS - BOX_PAD - 2 - leftOffset,
                    textPoint.y - yOffset - TIMER_RADIUS));
            timer.setProgress(remaining / (double) total);
            timer.setDiameter(TIMER_RADIUS * 2);
            timer.setFill(getDespawnTextColor(item));
            timer.setBorderColor(getDespawnTextColor(item));
            timer.render(g);
        }
    }

    private void renderClickboxes(Graphics2D g, Rectangle textBox, PluginTileItem item, DisplayConfig display,
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

    private Dimension renderIcon(Graphics2D g, BufferedImageProvider.CacheKey cacheKey, Point textPoint, int yOffset) {
        var image = plugin.getIconIndex().get(cacheKey);
        if (image == null) {
            return new Dimension(0, 0);
        }

        var xpad = config.iconPosition() == IconPosition.OUTSIDE ? BOX_PAD : 0;
        var fontHeight = g.getFontMetrics().getHeight();
        var x = textPoint.x - image.getWidth() - xpad - 1;
        var y = textPoint.y - fontHeight - yOffset + (fontHeight - image.getHeight()) / 2;
        g.drawImage(image, x, y, null);
        return new Dimension(image.getWidth() + xpad, image.getHeight());
    }

    private Map<Boolean, Deque<RenderItem>> createItemCollection(List<PluginTileItem> items) {
        Map<Boolean, Deque<RenderItem>> itemCollection = new HashMap<>();
        itemCollection.put(true, new ArrayDeque<>());
        itemCollection.put(false, new ArrayDeque<>());

        var countMap = new HashMap<OverlayKey, ItemCounts>();
        for (var item : items) {
            var match = plugin.getDisplayIndex().get(item);
            var key = new OverlayKey(item.getId(), match);
            var existingVal = countMap.get(key);
            if (existingVal == null) {
                var quant = new ItemCounts(1, item.getQuantity());
                countMap.put(key, quant);
                itemCollection.get(key.displayConfig.isCompact()).add(new RenderItem(item, key, quant));
            } else {
                if (item.isStackable()) {
                    existingVal.quantity += item.getQuantity();
                }
                existingVal.count++;
            }
        }
        return itemCollection;
    }

    @Value
    private static class OverlayKey {
        int id;
        DisplayConfig displayConfig;
    }

    @Value
    private static class RenderItem {
        PluginTileItem firstItem;
        OverlayKey overlayKey;
        ItemCounts counts;
    }

    @Getter
    @Setter
    private static class ItemCounts {
        //Number of individual items/stacks
        int count;
        //Total sum of all stacks combined
        int quantity;

        public ItemCounts(int count, int quantity) {
            this.count = count;
            this.quantity = quantity;
        }
    }

    private boolean inRenderRange(Player player, Tile tile) {
        var pLoc = player.getLocalLocation();
        var tLoc = tile.getLocalLocation();
        if (pLoc.getWorldView() == tLoc.getWorldView()) { // avoid transforming unless we absolutely must
            return pLoc.distanceTo(tLoc) <= MAX_DISTANCE;
        }

        if (pLoc.getWorldView() != -1) {
            pLoc = toTopLevel(client, pLoc);
        }
        if (tLoc.getWorldView() != -1) {
            tLoc = toTopLevel(client, tLoc);
        }
        return pLoc.distanceTo(tLoc) <= MAX_DISTANCE;
    }

    private static LocalPoint toTopLevel(Client client, LocalPoint loc) {
        return client.getTopLevelWorldView()
                .worldEntities()
                .byIndex(loc.getWorldView())
                .transformToMainWorld(loc);
    }

    private static Point getCanvasImageLocation(@Nonnull Client client, @Nonnull LocalPoint localLocation, @Nonnull BufferedImage image, int zOffset) {
        var point = Perspective.getCanvasImageLocation(client, localLocation, image, zOffset);
        return point != null ? new Point(point.getX(), point.getY()) : null;
    }

    private static Point getCanvasTextLocation(@Nonnull Client client, @Nonnull Graphics2D graphics, @Nonnull LocalPoint localLocation, @Nullable String text, int zOffset) {
        var point = Perspective.getCanvasTextLocation(client, graphics, localLocation, text, zOffset);
        return point != null ? new Point(point.getX(), point.getY()) : null;
    }
}


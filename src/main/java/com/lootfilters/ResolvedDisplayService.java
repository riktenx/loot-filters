package com.lootfilters;

import com.lootfilters.model.BufferedImageProvider;
import com.lootfilters.model.FontType;
import com.lootfilters.model.SoundProvider;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.client.events.PluginMessage;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ResolvedDisplayService {
    @Inject private LootFiltersPlugin plugin;
    @Inject private ResolvedImageProvider images;

    void onPluginMessage(PluginMessage message) {
        if (!"loot-filters".equals(message.getNamespace())
                || !"resolved-display-request-v1".equals(message.getName())) {
            return;
        }

        Map<String, Object> data = message.getData();
        data.remove("display");
        data.remove("error");
        if (!plugin.getClient().isClientThread()) {
            invalidRequest(data, "Requests must be posted on the client thread");
            return;
        }
        for (String field : List.of("x", "y", "z", "itemId")) {
            if (!(data.get(field) instanceof Integer)) {
                invalidRequest(data, field + " must be an Integer");
                return;
            }
        }
        if (data.containsKey("worldViewId") && !(data.get("worldViewId") instanceof Integer)) {
            invalidRequest(data, "worldViewId must be an Integer");
            return;
        }
        int plane = (Integer) data.get("z");
        int itemId = (Integer) data.get("itemId");
        if (plane < 0 || plane > 3 || itemId < 0) {
            invalidRequest(data, "z must be between 0 and 3 and itemId must be nonnegative");
            return;
        }

        int worldViewId = (Integer) data.getOrDefault("worldViewId", WorldView.TOPLEVEL);
        if (worldViewId < WorldView.TOPLEVEL) {
            invalidRequest(data, "worldViewId must be -1 or nonnegative");
            return;
        }
        Tile tile = findTile(worldViewId, (Integer) data.get("x"), (Integer) data.get("y"), plane);
        Map<String, Object> reply = tile == null ? Map.of("status", "not-found") : resolve(tile, itemId);
        data.putAll(reply);
    }

    private static void invalidRequest(Map<String, Object> data, String error) {
        data.put("status", "invalid-request");
        data.put("error", error);
    }

    private Tile findTile(int worldViewId, int x, int y, int z) {
        var view = plugin.getClient().getWorldView(worldViewId);
        if (view == null || view.getScene() == null) {
            return null;
        }
        var tiles = view.getScene().getTiles();
        long sceneX = (long) x - view.getBaseX();
        long sceneY = (long) y - view.getBaseY();
        if (tiles == null || z < 0 || z >= tiles.length || tiles[z] == null
                || sceneX < 0 || sceneX >= tiles[z].length || tiles[z][(int) sceneX] == null
                || sceneY < 0 || sceneY >= tiles[z][(int) sceneX].length) {
            return null;
        }
        return tiles[z][(int) sceneX][(int) sceneY];
    }

    Map<String, Object> resolve(Tile tile, int itemId) {
        var items = plugin.getTileItemIndex().findItem(tile, itemId);
        if (items == null || items.isEmpty()) {
            return Map.of("status", "not-found");
        }
        Map<String, Object> chosen = null;
        BufferedImageProvider.CacheKey chosenIcon = null;
        for (var item : items) {
            var display = plugin.getDisplayIndex().get(item);
            if (display == null) {
                return Map.of("status", "not-ready");
            }
            var provider = display.getIcon();
            var key = provider == null ? null : provider.getCacheKey(item,
                    display.isCompact() ? plugin.getConfig().compactRenderSize() : 16);
            var snapshot = snapshot(display, key);
            if (chosen != null && !chosen.equals(snapshot)) {
                return Map.of("status", "ambiguous");
            }
            chosen = snapshot;
            chosenIcon = key;
        }

        if (chosenIcon != null) {
            Map<String, Object> icon = new HashMap<>(icon(chosenIcon));
            icon.put("getImage", images.provider(chosenIcon));
            chosen = new HashMap<>(chosen);
            chosen.put("icon", Collections.unmodifiableMap(icon));
        }
        return Map.of(
                "status", "ok",
                "display", Collections.unmodifiableMap(chosen)
        );
    }

    private static Map<String, Object> snapshot(DisplayConfig display, BufferedImageProvider.CacheKey icon) {
        Map<String, Object> result = new HashMap<>();
        result.put("textColor", display.getTextColor());
        result.put("backgroundColor", display.getBackgroundColor());
        result.put("borderColor", display.getBorderColor());
        result.put("hidden", display.isHidden());
        result.put("showLootbeam", display.isShowLootbeam());
        result.put("showValue", display.isShowValue());
        result.put("compact", display.isCompact());
        result.put("showDespawn", display.isShowDespawn());
        result.put("notify", display.isNotify());
        result.put("textAccent", display.getTextAccent() == null ? null : display.getTextAccent().name());
        result.put("textAccentColor", display.getTextAccentColor());
        result.put("lootbeamColor", display.getLootbeamColor());
        result.put("fontType", (display.getFontType() == null ? FontType.NORMAL : display.getFontType()).name());
        result.put("menuTextColor", display.getMenuTextColor());
        result.put("hideOverlay", display.isHideOverlay());
        result.put("highlightTile", display.isHighlightTile());
        result.put("tileStrokeColor", display.getTileStrokeColor());
        result.put("tileFillColor", display.getTileFillColor());
        result.put("sound", sound(display.getSound()));
        result.put("menuSort", display.getMenuSort());
        result.put("icon", icon(icon));
        result.put("evalTrace", display.getEvalTrace() == null ? List.of() : List.copyOf(display.getEvalTrace()));
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Object> icon(BufferedImageProvider.CacheKey key) {
        if (key == null) {
            return null;
        }
        switch (key.getType()) {
            case 0:
                return Map.of(
                        "type", "sprite",
                        "id", key.getParam0(),
                        "index", key.getParam1()
                );
            case 1:
                return Map.of(
                        "type", "item",
                        "id", key.getParam0(),
                        "height", key.getHeight()
                );
            case 2:
                return Map.of(
                        "type", "file",
                        "filename", key.getParam2()
                );
            case 3:
                return Map.of(
                        "type", "current-item",
                        "id", key.getParam0(),
                        "quantity", key.getParam1(),
                        "height", key.getHeight()
                );
            default:
                return Map.of("type", "unknown");
        }
    }

    private static Map<String, Object> sound(SoundProvider sound) {
        if (sound instanceof SoundProvider.SoundEffect) {
            return Map.of(
                    "type", "sound-effect",
                    "id", ((SoundProvider.SoundEffect) sound).getId()
            );
        }
        if (sound instanceof SoundProvider.File) {
            return Map.of(
                    "type", "file",
                    "filename", ((SoundProvider.File) sound).getFilename()
            );
        }
        return sound == null ? null : Map.of("type", "unknown");
    }
}

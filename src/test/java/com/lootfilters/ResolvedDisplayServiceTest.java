package com.lootfilters;

import com.lootfilters.model.BufferedImageProvider;
import com.lootfilters.model.DisplayConfigIndex;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.model.SoundProvider;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ResolvedDisplayServiceTest {
    @Mock private LootFiltersPlugin plugin;
    @Mock private ResolvedImageProvider images;
    @Mock private Client client;
    @Mock private TileItemIndex items;
    @Mock private DisplayConfigIndex displays;
    @Mock private WorldView view;
    @Mock private Scene scene;
    @Mock private Tile tile;
    @InjectMocks private ResolvedDisplayService service;

    private EventBus eventBus;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(plugin.getClient()).thenReturn(client);
        when(client.isClientThread()).thenReturn(true);
        when(plugin.getTileItemIndex()).thenReturn(items);
        when(plugin.getDisplayIndex()).thenReturn(displays);
        when(client.getWorldView(WorldView.TOPLEVEL)).thenReturn(view);
        when(view.getBaseX()).thenReturn(3200);
        when(view.getBaseY()).thenReturn(6400);
        when(view.getScene()).thenReturn(scene);
        when(scene.getTiles()).thenReturn(new Tile[][][] {{{tile}}});
        eventBus = new EventBus(error -> {
            throw new AssertionError(error);
        });
        eventBus.register(PluginMessage.class, service::onPluginMessage, 0);
    }

    @Test
    public void ignoresUnrelatedMessages() {
        Map<String, Object> data = new HashMap<>();
        eventBus.post(new PluginMessage("other", "resolved-display-request-v1", data));
        eventBus.post(new PluginMessage("loot-filters", "filter-reloaded", data));
        assertTrue(data.isEmpty());
        verifyNoInteractions(client);
    }

    @Test
    public void malformedFieldsReturnErrorAndClearPreviousDisplay() {
        for (String field : List.of("x", "y", "z", "itemId")) {
            for (Object badValue : new Object[] {null, "1", 1L, 1.5}) {
                Map<String, Object> data = request();
                data.put(field, badValue);
                data.put("display", Map.of("hidden", true));
                post(data);
                assertEquals("invalid-request", data.get("status"));
                assertTrue(data.get("error").toString().contains(field));
                assertFalse(data.containsKey("display"));
            }
            Map<String, Object> data = request();
            data.remove(field);
            post(data);
            assertEquals("invalid-request", data.get("status"));
        }
        verify(client, never()).getWorldView(anyInt());
    }

    @Test
    public void rejectsInvalidOptionalWorldViewAndValueRanges() {
        for (Object value : new Object[] {null, "1", 1L, -2}) {
            Map<String, Object> data = request();
            data.put("worldViewId", value);
            post(data);
            assertEquals("invalid-request", data.get("status"));
        }
        for (Map.Entry<String, Integer> value : Map.of(
                "z", 4,
                "itemId", -1
        ).entrySet()) {
            Map<String, Object> data = request();
            data.put(value.getKey(), value.getValue());
            post(data);
            assertEquals("invalid-request", data.get("status"));
        }
    }

    @Test
    public void wrongThreadReportsFailureWithoutAccessingScene() {
        when(client.isClientThread()).thenReturn(false);
        Map<String, Object> data = request();
        post(data);
        assertEquals("invalid-request", data.get("status"));
        assertTrue(data.get("error").toString().contains("client thread"));
        verify(client, never()).getWorldView(anyInt());
    }

    @Test
    public void resolvesTopLevelAndNestedWorldViewsDuringDispatch() {
        addItem(DisplayConfig.builder().textColor(Color.CYAN).build());
        for (int id : new int[] {WorldView.TOPLEVEL, 7}) {
            when(client.getWorldView(id)).thenReturn(view);
            Map<String, Object> data = request();
            data.put("error", "previous error");
            if (id != WorldView.TOPLEVEL) {
                data.put("worldViewId", id);
            }
            post(data);
            assertEquals("ok", data.get("status"));
            assertEquals(Color.CYAN, display(data).get("textColor"));
            assertFalse(data.containsKey("error"));
            verify(client).getWorldView(id);
        }
    }

    @Test
    public void absentWorldViewSceneAndOutOfSceneCoordinatesReturnNotFound() {
        Map<String, Object> data = request();
        data.put("worldViewId", 99);
        post(data);
        assertEquals("not-found", data.get("status"));
        for (int coordinate : new int[] {3199, 3201, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            data = request();
            data.put("x", coordinate);
            post(data);
            assertEquals("not-found", data.get("status"));
        }
        when(view.getScene()).thenReturn(null);
        data = request();
        post(data);
        assertEquals("not-found", data.get("status"));
    }

    @Test
    public void distinguishesMissingUnevaluatedAndAmbiguousStacks() {
        assertEquals("not-found", service.resolve(tile, 4151).get("status"));
        PluginTileItem first = addItem(null);
        assertEquals("not-ready", service.resolve(tile, 4151).get("status"));
        PluginTileItem second = mock(PluginTileItem.class);
        when(items.findItem(tile, 4151)).thenReturn(List.of(first, second));
        when(displays.get(first)).thenReturn(DisplayConfig.builder().textColor(Color.RED).build());
        when(displays.get(second)).thenReturn(DisplayConfig.builder().textColor(Color.BLUE).build());
        assertEquals("ambiguous", service.resolve(tile, 4151).get("status"));
        verifyNoInteractions(images);
    }

    @Test
    public void equivalentStacksShareOneLazyProviderAndReadOnlySnapshot() {
        DisplayConfig config = DisplayConfig.builder().icon(new BufferedImageProvider.Item(4151)).build();
        PluginTileItem first = addItem(config);
        PluginTileItem second = mock(PluginTileItem.class);
        when(items.findItem(tile, 4151)).thenReturn(List.of(first, second));
        when(displays.get(second)).thenReturn(config);
        Supplier<AsyncBufferedImage> image = mock(Supplier.class);
        when(images.provider(any())).thenReturn(image);
        Map<String, Object> result = service.resolve(tile, 4151);
        Map<String, Object> display = display(result);
        Map<String, Object> icon = (Map<String, Object>) display.get("icon");
        assertEquals("ok", result.get("status"));
        assertSame(image, icon.get("getImage"));
        assertEquals(16, icon.get("height"));
        verify(images).provider(new BufferedImageProvider.CacheKey(1, 4151, 0, "", 16));
        verifyNoInteractions(image);
        assertReadOnly(display);
        assertReadOnly(icon);
    }

    @Test
    public void compactIconsPreserveItemQuantityAndConfiguredHeight() {
        LootFiltersConfig config = mock(LootFiltersConfig.class);
        when(plugin.getConfig()).thenReturn(config);
        when(config.compactRenderSize()).thenReturn(24);
        PluginTileItem item = addItem(DisplayConfig.builder().compact(true).build());
        when(item.getId()).thenReturn(4151);
        when(item.getQuantity()).thenReturn(100);
        Map<String, Object> icon = (Map<String, Object>) display(service.resolve(tile, 4151)).get("icon");
        assertEquals("current-item", icon.get("type"));
        assertEquals(100, icon.get("quantity"));
        assertEquals(24, icon.get("height"));
    }

    @Test
    public void snapshotUsesHiddenDefaultsAndCopiesEvaluationTrace() {
        List<Integer> trace = new ArrayList<>(List.of(2, 5));
        addItem(DisplayConfig.builder().hidden(true).showLootbeam(true).notify(true).highlightTile(true)
                .sound(new SoundProvider.SoundEffect(42)).evalTrace(trace).build());
        Map<String, Object> display = display(service.resolve(tile, 4151));
        assertEquals(true, display.get("hideOverlay"));
        assertEquals(false, display.get("showLootbeam"));
        assertEquals(false, display.get("notify"));
        assertEquals(false, display.get("highlightTile"));
        assertNull(display.get("sound"));
        assertEquals(DisplayConfig.DEFAULT_MENU_TEXT_COLOR, display.get("menuTextColor"));
        assertEquals("NORMAL", display.get("fontType"));
        trace.add(8);
        assertEquals(List.of(2, 5), display.get("evalTrace"));
    }

    private PluginTileItem addItem(DisplayConfig display) {
        PluginTileItem item = mock(PluginTileItem.class);
        when(items.findItem(tile, 4151)).thenReturn(List.of(item));
        when(displays.get(item)).thenReturn(display);
        return item;
    }

    private Map<String, Object> request() {
        return new HashMap<>(Map.of(
                "x", 3200,
                "y", 6400,
                "z", 0,
                "itemId", 4151
        ));
    }

    private void post(Map<String, Object> data) {
        eventBus.post(new PluginMessage("loot-filters", "resolved-display-request-v1", data));
    }

    private Map<String, Object> display(Map<String, Object> result) {
        return (Map<String, Object>) result.get("display");
    }

    private void assertReadOnly(Map<String, Object> map) {
        try {
            map.put("changed", true);
            fail("Snapshot should be read-only");
        } catch (UnsupportedOperationException expected) {
            assertFalse(map.containsKey("changed"));
        }
    }
}

package com.lootfilters;

import com.lootfilters.model.BufferedImageProvider;
import com.lootfilters.model.IconIndex;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ResolvedImageProviderTest {
    @Mock private LootFiltersPlugin plugin;
    @Mock private ItemManager items;
    @Mock private SpriteManager sprites;
    @Mock private IconIndex icons;
    @Mock private ClientThread clientThread;
    @InjectMocks private ResolvedImageProvider provider;
    private final List<Runnable> clientTasks = new ArrayList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(plugin.getClientThread()).thenReturn(clientThread);
        when(plugin.getItemManager()).thenReturn(items);
        when(plugin.getSpriteManager()).thenReturn(sprites);
        when(plugin.getIconIndex()).thenReturn(icons);
        doAnswer(call -> {
            clientTasks.add(call.getArgument(0));
            return null;
        }).when(clientThread).invokeLater(any(Runnable.class));
    }

    @Test
    public void itemImageKeepsNativeReadinessAndDimensions() {
        AsyncBufferedImage original = new AsyncBufferedImage(clientThread, 36, 32, BufferedImage.TYPE_INT_ARGB);
        when(items.getImage(4151)).thenReturn(original);
        var supplier = provider.provider(new BufferedImageProvider.CacheKey(1, 4151, 0, "", 16));
        verifyNoInteractions(items);
        AsyncBufferedImage result = supplier.get();
        assertSame(original, result);
        AtomicBoolean ready = new AtomicBoolean();
        result.onLoaded(() -> ready.set(true));
        assertFalse(ready.get());
        original.setRGB(5, 5, 0xff40a080);
        original.loaded();
        assertTrue(ready.get());
        assertEquals(0xff40a080, result.getRGB(5, 5));
        assertEquals(36, result.getWidth());
        assertEquals(32, result.getHeight());
    }

    @Test
    public void currentItemUsesCapturedQuantity() {
        AsyncBufferedImage image = new AsyncBufferedImage(clientThread, 36, 32, BufferedImage.TYPE_INT_ARGB);
        when(items.getImage(995, 10000, false)).thenReturn(image);
        assertSame(image, provider.provider(new BufferedImageProvider.CacheKey(3, 995, 10000, "", 24)).get());
        verify(items).getImage(995, 10000, false);
    }

    @Test
    public void spriteCanBeRetriedOnceArchiveIsReady() {
        BufferedImage original = image(47, 23);
        when(sprites.getSprite(100, 2)).thenReturn(null, original);
        var supplier = provider.provider(new BufferedImageProvider.CacheKey(0, 100, 2, "", 16));
        assertNull(supplier.get());
        assertLoadedPixels(original, supplier.get());
    }

    @Test
    public void fileUsesEvaluatedCacheAndKeepsDimensionsAndAlpha() {
        BufferedImage original = image(71, 29);
        var key = new BufferedImageProvider.CacheKey(2, 0, 0, "custom.png", 16);
        when(icons.get(key)).thenReturn(original);
        assertLoadedPixels(original, provider.provider(key).get());
        verifyNoInteractions(items, sprites);
    }

    @Test
    public void unavailableAndRemovedFileImagesReturnNull() {
        var key = new BufferedImageProvider.CacheKey(2, 0, 0, "custom.png", 16);
        when(icons.get(key)).thenReturn(null, image(10, 20), null);
        var supplier = provider.provider(key);
        assertNull(supplier.get());
        assertNotNull(supplier.get());
        assertNull(supplier.get());
    }

    @Test
    public void unknownProviderReturnsNull() {
        assertNull(provider.provider(new BufferedImageProvider.CacheKey(99, 0, 0, "", 16)).get());
        verifyNoInteractions(items, sprites, icons);
    }

    private BufferedImage image(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(3, 4, 0x804080c0);
        return image;
    }

    private void assertLoadedPixels(BufferedImage original, AsyncBufferedImage result) {
        assertEquals(original.getWidth(), result.getWidth());
        assertEquals(original.getHeight(), result.getHeight());
        AtomicBoolean ready = new AtomicBoolean();
        result.onLoaded(() -> ready.set(true));
        clientTasks.forEach(Runnable::run);
        assertTrue(ready.get());
        assertEquals(original.getRGB(3, 4), result.getRGB(3, 4));
        assertEquals(0, result.getRGB(0, 0));
    }
}

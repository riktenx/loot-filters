package com.lootfilters.model;

import com.lootfilters.LootFiltersPlugin;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import static com.lootfilters.LootFiltersPlugin.ICON_DIRECTORY;

@Slf4j
public abstract class BufferedImageProvider {
    public abstract BufferedImage getImage(LootFiltersPlugin plugin, PluginTileItem item);

    public abstract CacheKey getCacheKey(PluginTileItem item);

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static final class Sprite extends BufferedImageProvider {
        private final int spriteId;
        private final int index;

        @Override
        public BufferedImage getImage(LootFiltersPlugin plugin, PluginTileItem item) {
            try {
                return plugin.getSpriteManager().getSprite(spriteId, index);
            } catch (ArrayIndexOutOfBoundsException e) {
                log.warn("sprite index out of bounds", e);
                return null;
            }
        }

        @Override
        public CacheKey getCacheKey(PluginTileItem item) {
            return new CacheKey(0, spriteId, index, "");
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static final class Item extends BufferedImageProvider {
        private final int itemId;

        @Override
        public BufferedImage getImage(LootFiltersPlugin plugin, PluginTileItem item) {
            var image = plugin.getItemManager().getImage(itemId);
            return ImageUtil.resizeImage(image, image.getWidth() / 2, image.getHeight() / 2);
        }

        @Override
        public CacheKey getCacheKey(PluginTileItem item) {
            return new CacheKey(1, itemId, 0, "");
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static final class File extends BufferedImageProvider {
        private final String filename;

        @Override
        public BufferedImage getImage(LootFiltersPlugin plugin, PluginTileItem item) {
            try {
                return ImageIO.read(new java.io.File(ICON_DIRECTORY, filename));
            } catch (Exception e) {
                log.warn("load image file {}", filename, e);
                return null;
            }
        }

        @Override
        public CacheKey getCacheKey(PluginTileItem item) {
            return new CacheKey(2, 0, 0, filename);
        }
    }

    @EqualsAndHashCode(callSuper = false)
    public static final class CurrentItem extends BufferedImageProvider {
        @Override
        public BufferedImage getImage(LootFiltersPlugin plugin, PluginTileItem item) {
            var image = plugin.getItemManager().getImage(item.getId(), item.getQuantity(), false);
            return ImageUtil.resizeImage(image, image.getWidth() / 2, image.getHeight() / 2);
        }

        @Override
        public CacheKey getCacheKey(PluginTileItem item) {
            return new CacheKey(3, item.getId(), item.getQuantity(), "");
        }
    }

    @Value
    public static class CacheKey {
        int type;
        int param0, param1;
        String param2;
    }
}

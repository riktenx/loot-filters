package com.lootfilters.model;

import com.lootfilters.LootFiltersPlugin;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import static com.lootfilters.LootFiltersPlugin.ICON_DIRECTORY;

@Slf4j
public abstract class BufferedImageProvider {
    public abstract BufferedImage get(LootFiltersPlugin plugin);

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static final class Sprite extends BufferedImageProvider {
        private final int spriteId;
        private final int index;

        @Override
        public BufferedImage get(LootFiltersPlugin plugin) {
            try {
                return plugin.getSpriteManager().getSprite(spriteId, index);
            } catch (ArrayIndexOutOfBoundsException e) {
                log.warn("sprite index out of bounds", e);
                return null;
            }
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static final class Item extends BufferedImageProvider {
        private final int itemId;

        @Override
        public BufferedImage get(LootFiltersPlugin plugin) {
            var image = plugin.getItemManager().getImage(itemId);
            return ImageUtil.resizeImage(image, image.getWidth() / 2, image.getHeight() / 2);
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static final class File extends BufferedImageProvider {
        private final String filename;

        @Override
        public BufferedImage get(LootFiltersPlugin plugin) {
            try {
                return ImageIO.read(new java.io.File(ICON_DIRECTORY, filename));
            } catch (Exception e) {
                log.warn("load image file {}", filename, e);
                return null;
            }
        }
    }
}

package com.lootfilters;

import com.lootfilters.model.BufferedImageProvider;
import net.runelite.client.util.AsyncBufferedImage;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

class ResolvedImageProvider {
    @Inject private LootFiltersPlugin plugin;

    Supplier<AsyncBufferedImage> provider(BufferedImageProvider.CacheKey key) {
        switch (key.getType()) {
            case 0:
                return () -> loaded(new BufferedImageProvider.Sprite(key.getParam0(), key.getParam1())
                        .getImage(plugin, null));
            case 1:
                return () -> plugin.getItemManager().getImage(key.getParam0());
            case 2:
                return () -> loaded(plugin.getIconIndex().get(key));
            case 3:
                return () -> plugin.getItemManager().getImage(key.getParam0(), key.getParam1(), false);
            default:
                return () -> null;
        }
    }

    private AsyncBufferedImage loaded(BufferedImage image) {
        if (image == null) {
            return null;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        var result = new AsyncBufferedImage(plugin.getClientThread(), width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, image.getRGB(0, 0, width, height, null, 0, width), 0, width);
        result.loaded();
        return result;
    }
}

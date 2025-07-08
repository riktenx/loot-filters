package com.lootfilters;

import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

public class Icons {
    public static final BufferedImage FOLDER;
    public static final BufferedImage OVERLAY_DISABLED;
    public static final BufferedImage PANEL_ICON;
    public static final BufferedImage CLIPBOARD_PASTE;
    public static final BufferedImage RELOAD;

    static {
        FOLDER = ImageUtil.loadImageResource(Icons.class, "/com/lootfilters/icons/folder_icon.png");
        OVERLAY_DISABLED = ImageUtil.loadImageResource(Icons.class, "/com/lootfilters/icons/overlay_disabled.png");
        PANEL_ICON = ImageUtil.loadImageResource(Icons.class, "/com/lootfilters/icons/panel.png");
        CLIPBOARD_PASTE = ImageUtil.loadImageResource(Icons.class, "/com/lootfilters/icons/paste_icon.png");
        RELOAD = ImageUtil.loadImageResource(Icons.class, "/com/lootfilters/icons/reload_icon.png");
    }

    private Icons() {
    }
}

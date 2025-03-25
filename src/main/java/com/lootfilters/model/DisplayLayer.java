package com.lootfilters.model;

import net.runelite.client.ui.overlay.OverlayLayer;

public enum DisplayLayer {
    DEFAULT,
    BELOW,
    ABOVE;

    public OverlayLayer toOverlayLayer() {
        switch (this) {
            case BELOW:
                return OverlayLayer.ABOVE_SCENE;
            case ABOVE:
                return OverlayLayer.ABOVE_WIDGETS;
            default:
                return OverlayLayer.UNDER_WIDGETS;
        }
    }

    public static DisplayLayer fromOverlayLayer(OverlayLayer overlayLayer) {
        switch (overlayLayer) {
            case ABOVE_SCENE:
                return DisplayLayer.BELOW;
            case ABOVE_WIDGETS:
                return DisplayLayer.ABOVE;
            default:
                return DisplayLayer.DEFAULT;
        }
    }
}

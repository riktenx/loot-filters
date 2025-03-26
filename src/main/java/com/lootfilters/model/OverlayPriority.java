package com.lootfilters.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.ui.overlay.Overlay;

@Getter
@AllArgsConstructor
public enum OverlayPriority {
    LOW(Overlay.PRIORITY_LOW),
    DEFAULT(Overlay.PRIORITY_DEFAULT),
    MED(Overlay.PRIORITY_MED),
    HIGH(Overlay.PRIORITY_HIGH),
    HIGHEST(Overlay.PRIORITY_HIGHEST);

    private final float value;
}

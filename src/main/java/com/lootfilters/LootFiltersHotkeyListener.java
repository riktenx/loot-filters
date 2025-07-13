package com.lootfilters;

import com.google.inject.Inject;
import net.runelite.client.util.HotkeyListener;

import java.time.Duration;
import java.time.Instant;

public class LootFiltersHotkeyListener extends HotkeyListener {
    private final LootFiltersPlugin plugin;

    private Instant lastPressed = Instant.EPOCH;

    @Inject
    private LootFiltersHotkeyListener(LootFiltersPlugin plugin) {
        super(plugin.getConfig()::hotkey);

        this.plugin = plugin;
    }

    @Override
    public void hotkeyPressed() {
        plugin.setHotkeyActive(true);

        var now = Instant.now();
        if (shouldToggleOverlay(now)) {
            plugin.setOverlayEnabled(!plugin.isOverlayEnabled());
            lastPressed = Instant.EPOCH;
        } else {
            lastPressed = now;
        }
    }

    @Override
    public void hotkeyReleased() {
        plugin.setHotkeyActive(false);
    }

    private boolean shouldToggleOverlay(Instant now) {
        if (!plugin.getConfig().hotkeyDoubleTapTogglesOverlay()) {
            return false;
        }

        return !plugin.isOverlayEnabled() && plugin.isHotkeyActive()
                || Duration.between(lastPressed, now).toMillis() < plugin.getConfig().hotkeyDoubleTapDelay();
    }
}
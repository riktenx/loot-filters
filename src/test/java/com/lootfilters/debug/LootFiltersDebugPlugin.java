package com.lootfilters.debug;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
        name = "Loot Filters (Debug)",
        developerPlugin = true
)
public class LootFiltersDebugPlugin extends Plugin {
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private LootFiltersDebugOverlay overlay;

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
    }

    @Provides
    public LootFiltersDebugConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LootFiltersDebugConfig.class);
    }
}

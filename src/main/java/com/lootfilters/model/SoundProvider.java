package com.lootfilters.model;

import com.lootfilters.LootFiltersPlugin;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

public abstract class SoundProvider {
    public abstract void play(LootFiltersPlugin plugin);

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static final class SoundEffect extends SoundProvider {
        private final int id;

        @Override
        public void play(LootFiltersPlugin plugin) {
            var client = plugin.getClient();
            var userVolume = client.getPreferences().getSoundEffectVolume();
            var effectVolume = plugin.getConfig().soundVolume();

            client.getPreferences().setSoundEffectVolume(effectVolume);
            client.playSoundEffect(id, effectVolume);
            client.getPreferences().setSoundEffectVolume(userVolume);
        }
    }
}

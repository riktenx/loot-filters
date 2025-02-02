package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.runelite.client.RuneLite;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;

import static com.lootfilters.LootFiltersPlugin.PLUGIN_DIR;
import static com.lootfilters.LootFiltersPlugin.SOUND_DIR;

public abstract class Sound {
    public abstract void play(LootFiltersPlugin plugin);

    // Unused - direct sound effect playback is a little awkward because it will use your game sound effect volume if
    // unmuted - only when it's actually muted will the config setting be used. Deferring on supporting this until we
    // figure out better UX.
    @AllArgsConstructor
    public static class ID extends Sound {
        private final int soundId;

        @Override
        public void play(LootFiltersPlugin plugin) {
            if (plugin.getConfig().soundVolume() > 0) {
                plugin.getClient().playSoundEffect(soundId, plugin.getConfig().soundVolume());
            }
        }
    }

    @AllArgsConstructor
    public static class File extends Sound {
        private static final java.io.File soundDir = new java.io.File(
                new java.io.File(RuneLite.RUNELITE_DIR, PLUGIN_DIR), SOUND_DIR
        );

        private final String filename;

        @Override
        public void play(LootFiltersPlugin plugin) {
            if (plugin.getConfig().soundVolume() == 0) {
                return;
            }

            try {
                var stream = AudioSystem.getAudioInputStream(new java.io.File(soundDir, filename));
                var clip = getClip();
                clip.open(stream);
                var control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                // explanation of linear to logarithmic conversion:
                // https://stackoverflow.com/questions/40514910/set-volume-of-java-clip
                // the example sets the floor at -20db which seems appropriately quiet in testing
                control.setValue(20f * (float) Math.log10(plugin.getConfig().soundVolume() / 100f));
                clip.start();
            } catch (Exception ignored) {
            }
        }

        @SneakyThrows
        private Clip getClip() {
            var clip = AudioSystem.getClip();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            return clip;
        }
    }
}

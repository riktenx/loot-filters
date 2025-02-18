package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;

import static com.lootfilters.LootFiltersPlugin.PLUGIN_DIR;
import static com.lootfilters.LootFiltersPlugin.SOUND_DIR;

@Slf4j
public class Sound {
    private static final java.io.File soundDir = new java.io.File(
            new java.io.File(RuneLite.RUNELITE_DIR, PLUGIN_DIR), SOUND_DIR
    );

    public static void play(LootFiltersPlugin plugin, String filename) {
        if (plugin.getConfig().soundVolume() == 0) {
            return;
        }

        try (var stream = AudioSystem.getAudioInputStream(new java.io.File(soundDir, filename))) {
            var clip = getClip();
            clip.open(stream);
            var control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            // explanation of linear to logarithmic conversion:
            // https://stackoverflow.com/questions/40514910/set-volume-of-java-clip
            // the example sets the floor at -20db which seems appropriately quiet in testing
            control.setValue(20f * (float) Math.log10(plugin.getConfig().soundVolume() / 100f));
            clip.start();
        } catch (Exception e) {
            log.warn("play sound failure", e);
        }
    }

    @SneakyThrows
    private static Clip getClip() {
        var clip = AudioSystem.getClip();
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                clip.close();
            }
        });
        return clip;
    }
}

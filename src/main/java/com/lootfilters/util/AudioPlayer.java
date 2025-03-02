// abbreviated copy of the AudioPlayer added in https://github.com/runelite/runelite/pull/18745 until that gets merged
package com.lootfilters.util;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

@Slf4j
public class AudioPlayer {
    private Line prevLine;

    public void play(File file, float gain) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(file))
        {
            DataLine line = getSelfClosingLine(audio);
            if (gain != 0) {
                trySetGain(line, gain);
            }
            line.start();
        }
    }

    private DataLine getSelfClosingLine(AudioInputStream stream) throws IOException, LineUnavailableException
    {
        Clip clip = AudioSystem.getClip();
        try {
            clip.open(stream);
        } catch (IOException e) {
            clip.close();
            throw e;
        }

        clip.addLineListener(event -> {
            if (event.getType() != LineEvent.Type.STOP) {
                return;
            }

            synchronized (this) { // https://discordapp.com/channels/301497432909414422/419891709883973642/1341801141381632000
                if (prevLine != null) {
                    prevLine.close();
                }
                prevLine = clip;
            }
        });
        return clip;
    }

    private void trySetGain(DataLine line, float gain) {
        try {
            FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            control.setValue(gain);
        } catch (Exception e) {
            log.warn("Failed to set gain: {}", e.getMessage());
        }
    }
}
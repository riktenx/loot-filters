package com.lootfilters;

import com.lootfilters.model.LootbeamHeight;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.callback.ClientThread;

import java.awt.Color;

public class Lootbeam {
    private static final int MODEL_ID = 43330;
    private static final short FACE_COLOR_0 = JagexColor.packHSL(25, 6, 64);
    private static final short FACE_COLOR_1 = JagexColor.packHSL(25, 7, 88);

    private final ClientThread clientThread;

    private final RuneLiteObject rlObject;

    public Lootbeam(LootFiltersConfig config, Client client, ClientThread clientThread, LocalPoint localPoint, Color color) {
        this.clientThread = clientThread;

        rlObject = new RuneLiteObject(client);
        rlObject.setModel(loadModel(client, color, config.lootbeamHeight()));
        rlObject.setLocation(localPoint, client.getTopLevelWorldView().getPlane());
        rlObject.setAnimation(client.loadAnimation(AnimationID.FX_BEAM_IDLE));
    }

    public void setActive(boolean active) {
        clientThread.invoke(() -> {
            rlObject.setActive(active);
        });
    }

    // preserve adjusted RGB->HSV conversion from core so people don't crash out:
    // https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/grounditems/Lootbeam.java#L65
    private Model loadModel(Client client, Color color, LootbeamHeight height) {
        var hsl = JagexColor.rgbToHSL(color.getRGB(), 1);
        var h = JagexColor.unpackHue(hsl);
        var s = JagexColor.unpackSaturation(hsl);
        var l = JagexColor.unpackLuminance(hsl);

        return client.loadModelData(MODEL_ID)
                .cloneVertices()
                .scale(128, height.getMultiplier() * 128, 128)
                .cloneColors()
                .recolor(FACE_COLOR_0, JagexColor.packHSL(h, s > 2 ? s - 1 : s, l))
                .recolor(FACE_COLOR_1, JagexColor.packHSL(h, s, Math.min(l + 24, JagexColor.LUMINANCE_MAX)))
                .light(75 + ModelData.DEFAULT_AMBIENT, 1875 + ModelData.DEFAULT_CONTRAST,
                        ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
    }
}

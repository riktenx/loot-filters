package com.lootfilters.util;

import com.lootfilters.model.TextAccent;
import lombok.Setter;
import net.runelite.client.ui.overlay.RenderableEntity;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;

// simplified version of runelite's TextComponent API for our purposes, adding support for _no_ text accent as well as
// text accent color
@Setter
public class TextComponent implements RenderableEntity {
    private String text;
    private Color color = Color.WHITE;
    private Color accentColor = Color.BLACK;
    private TextAccent textAccent = TextAccent.SHADOW;
    private Point position;

    @Override
    public Dimension render(Graphics2D g) {
        var origColor = g.getColor();

        g.setColor(accentColor);

        switch (textAccent) {
            case SHADOW:
                g.drawString(text, position.x + 1, position.y + 1);
                break;
            case OUTLINE:
                g.drawString(text, position.x + 1, position.y);
                g.drawString(text, position.x, position.y + 1);
                g.drawString(text, position.x - 1, position.y);
                g.drawString(text, position.x, position.y - 1);
                break;
            case SHADOW_BOLD:
                g.drawString(text, position.x + 1, position.y);
                g.drawString(text, position.x - 1, position.y);

                g.drawString(text, position.x, position.y + 1);
                g.drawString(text, position.x, position.y - 1);

                g.drawString(text, position.x + 1, position.y + 1);
                g.drawString(text, position.x - 1, position.y - 1);

                g.drawString(text, position.x + 1, position.y - 1);
                g.drawString(text, position.x - 1, position.y + 1);

                g.drawString(text, position.x + 2, position.y);
                g.drawString(text, position.x, position.y + 2);

                g.drawString(text, position.x + 2, position.y + 1);
                g.drawString(text, position.x + 1, position.y + 2);
                break;
            default:
                break;
        }

        g.setColor(color);
        g.drawString(text, position.x, position.y);

        g.setColor(origColor);
        return null;
    }
}

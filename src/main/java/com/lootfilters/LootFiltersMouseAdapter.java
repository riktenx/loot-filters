package com.lootfilters;

import net.runelite.client.input.MouseAdapter;

import javax.inject.Inject;
import java.awt.event.MouseEvent;

import static com.lootfilters.util.TextUtil.unsetCsv;
import static com.lootfilters.util.TextUtil.toggleCsv;
import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isMiddleMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

public class LootFiltersMouseAdapter extends MouseAdapter {
    @Inject private LootFiltersPlugin plugin;

    @Override
    public MouseEvent mousePressed(MouseEvent e) {
        if (!plugin.isHotkeyActive()) {
            return e;
        } else if (plugin.getHoveredHide() != -1) {
            return handleToggleHide(e);
        } else if (plugin.getHoveredHighlight() != -1) {
            return handleToggleHighlight(e);
        } // else, hovered over the bounding box itself

        var hover = plugin.getHoveredItem();
        if (hover == -1) {
            return e;
        }

        var highlights = plugin.getConfig().highlightedItems();
        var hides = plugin.getConfig().hiddenItems();

        if (isLeftMouseButton(e)) {
            plugin.getClientThread().invoke(() -> {
                var item = plugin.getItemName(hover).toLowerCase();

                plugin.getConfig().setHighlightedItems(toggleCsv(highlights, item));
                plugin.getConfig().setHiddenItems(unsetCsv(hides, item));
            });
            e.consume();
        } else if (isRightMouseButton(e)) {
            plugin.getClientThread().invoke(() -> {
                var item = plugin.getItemName(hover).toLowerCase();

                plugin.getConfig().setHiddenItems(toggleCsv(hides, item));
                plugin.getConfig().setHighlightedItems(unsetCsv(highlights, item));
            });
            e.consume();
        } else if (isMiddleMouseButton(e)) {
            plugin.getClientThread().invoke(() -> {
                var item = plugin.getItemName(hover).toLowerCase();
                plugin.getConfig().setHighlightedItems(unsetCsv(highlights, item));
                plugin.getConfig().setHiddenItems(unsetCsv(hides, item));
            });
            e.consume();
        }
        return e;
    }

    private MouseEvent handleToggleHide(MouseEvent e) {
        plugin.getClientThread().invoke(() -> {
            var item = plugin.getItemName(plugin.getHoveredHide()).toLowerCase();
            plugin.getConfig().setHiddenItems(toggleCsv(plugin.getConfig().hiddenItems(), item));
            plugin.getConfig().setHighlightedItems(unsetCsv(plugin.getConfig().highlightedItems(), item));
        });
        e.consume();
        return e;
    }

    private MouseEvent handleToggleHighlight(MouseEvent e) {
        plugin.getClientThread().invoke(() -> {
            var item = plugin.getItemName(plugin.getHoveredHighlight()).toLowerCase();
            plugin.getConfig().setHighlightedItems(toggleCsv(plugin.getConfig().highlightedItems(), item));
            plugin.getConfig().setHiddenItems(unsetCsv(plugin.getConfig().hiddenItems(), item));
        });
        e.consume();
        return e;
    }
}

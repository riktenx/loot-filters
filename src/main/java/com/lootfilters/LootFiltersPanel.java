package com.lootfilters;

import com.lootfilters.lang.CompileException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.lootfilters.util.FilterUtil.configToFilterSource;
import static com.lootfilters.util.TextUtil.quote;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showInputDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static net.runelite.client.util.ImageUtil.loadImageResource;

@Slf4j
public class LootFiltersPanel extends PluginPanel {
    private static final String NONE_ITEM = "<none>";
    private static final String NONE_DESCRIPTION = "Select a filter to show its description.";
    private static final String BLANK_DESCRIPTION = "<no description provided>";

    private final LootFiltersPlugin plugin;
    private final JComboBox<String> filterSelect;
    private final JPanel root;
    private final JTextArea filterDescription;

    public LootFiltersPanel(LootFiltersPlugin plugin) {
        this.plugin = plugin;

        filterSelect = new JComboBox<>();
        filterDescription = new JTextArea();
        filterDescription.setLineWrap(true);
        filterDescription.setEditable(false);

        root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        init();
    }

    private void init() {
        var top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
        var mid = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));

        var label = new JLabel("Active filter:");
        var importClipboard = createIconButton("paste_icon",
                "Import filter from clipboard.",
                this::onImportClipboard);

        var reloadFilters = createIconButton("reload_icon",
                "Reload filters from disk.",
                this::onReloadFilters);
        var browseFolder = createIconButton("folder_icon",
                "View the plugin directory, where filters, sound files, and icon files should be placed, in the system file browser.",
                this::onBrowseFolder);

        var openFiltersite = new JLabel("<html><u>filterscape.xyz</u></html>");
        openFiltersite.setForeground(Color.decode("#00a0ff"));
        openFiltersite.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(URI.create("https://filterscape.xyz"));
                } catch (Exception ex) {
                    log.warn("open filterscape.xyz", ex);
                }
            }
        });

        top.add(Box.createHorizontalStrut(1));
        top.add(importClipboard);
        top.add(Box.createGlue());
        top.add(reloadFilters);
        top.add(browseFolder);

        mid.add(label);

        bottom.add(openFiltersite);

        root.add(top);
        root.add(mid);
        root.add(filterSelect);
        root.add(bottom);
        root.add(filterDescription);

        add(root);

        reflowFilterSelect(plugin.getLoadedFilters(), plugin.getSelectedFilterName());
        reflowFilterDescription();
    }

    private void onImportClipboard() {
        var newSrc = getClipboard();
        if (newSrc == null) {
            plugin.addChatMessage("Import failed: no text in clipboard.");
            return;
        }

        if (newSrc.startsWith("https://filterscape.xyz/import")) {
            plugin.addChatMessage("This is a link to import a filter on filterscape.xyz, taking you there now...");
            try {
                Desktop.getDesktop().browse(URI.create(newSrc + "&pluginRedirect=true"));
            } catch (Exception ex) {
                log.warn("open filterscape.xyz", ex);
            }
            return;
        }

        LootFilter newFilter;
        try {
            plugin.addChatMessage("Importing...");
            newFilter = LootFilter.fromSourcesWithPreamble(Map.of("clipboard", newSrc));
        } catch (CompileException e) {
            plugin.addChatMessage("Import failed: " + e.getMessage());
            return;
        }

        if (newFilter.getName() == null || newFilter.getName().isBlank()) {
            plugin.addChatMessage("Import failed: this filter does not have a name.");
            return;
        }

        var existing = plugin.getLoadedFilters();
        for (var filter : existing) {
            if (!filter.getName().equals(newFilter.getName())) {
                continue;
            }
            if (!confirm("Filter " + quote(filter.getName()) + " already exists. Update it?")) {
                return;
            }

            try {
                plugin.getFilterManager().updateFilter(filter.getFilename(), newSrc);
            } catch (Exception e) {
                plugin.addChatMessage("Import failed: " + e.getMessage());
                return;
            }

            plugin.setSelectedFilterName(newFilter.getName());
            onReloadFilters();
            return;
        }

        try {
            plugin.getFilterManager().saveNewFilter(newFilter.getName(), newSrc);
        } catch (Exception e) {
            plugin.addChatMessage("Import failed: " + e.getMessage());
            return;
        }

        plugin.setSelectedFilterName(newFilter.getName());
        onReloadFilters();
    }

    private void onFilterSelect(ActionEvent event) {
        var selected = (String) filterSelect.getSelectedItem();
        plugin.setSelectedFilterName(NONE_ITEM.equals(selected) ? null : selected);
    }

    private JButton createIconButton(String iconSource, String tooltip, Runnable onClick) {
        var button = new JButton("", icon(iconSource));
        button.setToolTipText(tooltip);
        button.setBackground(null);
        button.setBorder(null);
        button.addActionListener(it -> onClick.run());
        return button;
    }

    private boolean confirm(String confirmText) {
        var result = showConfirmDialog(this, confirmText, "Confirm", JOptionPane.YES_NO_OPTION);
        return result == JOptionPane.YES_OPTION;
    }

    private static String getClipboard() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (Exception e) {
            return null;
        }
    }

    private static ImageIcon icon(String name) {
        var img = loadImageResource(LootFiltersPanel.class, "/com/lootfilters/icons/" + name + ".png");
        return new ImageIcon(img);
    }

    private void onReloadFilters() {
        plugin.reloadFilters();
        reflowFilterSelect(plugin.getLoadedFilters(), plugin.getSelectedFilterName());
        reflowFilterDescription();
    }

    private void onBrowseFolder() {
        try {
            Desktop.getDesktop().open(LootFiltersPlugin.PLUGIN_DIRECTORY);
        } catch (Exception e) {
            log.warn("browse filters", e);
        }
    }

    public void reflowFilterSelect(List<LootFilter> filters, String selected) {
        for (var l : filterSelect.getActionListeners()) {
            filterSelect.removeActionListener(l);
        }

        filterSelect.removeAllItems();
        filterSelect.addItem(NONE_ITEM);
        for (var filter : filters) {
            filterSelect.addItem(filter.getName());
        }

        if (plugin.hasFilter(selected)) { // selected filter could be gone
            filterSelect.setSelectedItem(selected);
        } else {
            plugin.setSelectedFilterName(null);
        }
        filterSelect.addActionListener(this::onFilterSelect);
    }

    public void reflowFilterDescription() {
        if (plugin.getSelectedFilterName() == null) {
            filterDescription.setText(NONE_DESCRIPTION);
            return;
        }

        var filter = plugin.getActiveFilter();
        var desc = filter.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = BLANK_DESCRIPTION;
        }
        filterDescription.setText(desc.replaceAll("<br>", "\n"));
    }
}

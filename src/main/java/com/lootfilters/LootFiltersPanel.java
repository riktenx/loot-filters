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
    private static final String TUTORIAL_TEXT = "// Welcome to the loot filter\n" +
            "// For more information on \n" +
            "// usage, please check\n" +
            "// https://github.com/riktenx/loot-filters/blob/userguide/filter-lang.md";
    private static final String EXAMPLE_TEXT = "// Here's an example:\nif (name:\"Herring\") {\n  color = RED;\n}";

    private final LootFiltersPlugin plugin;
    private final JComboBox<String> filterSelect;
    private final JPanel root;

    public LootFiltersPanel(LootFiltersPlugin plugin) {
        this.plugin = plugin;

        filterSelect = new JComboBox<>();

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
        var createNew = createIconButton("create_filter",
                "Create a new empty filter.",
                this::onCreateEmptyFilter);
        var importClipboard = createIconButton("paste_icon",
                "Import filter from clipboard.",
                this::onImportClipboard);
        var importConfig = createIconButton("import_config",
                "Import item highlight and hide lists into a new filter. Doing this will also reset those lists.",
                this::onImportConfig);

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

        add(root);

        reflowFilterSelect(plugin.getLoadedFilters(), plugin.getSelectedFilterName());
    }

    private void onCreateEmptyFilter() {
        String[] templateOptions = {"blank script", "loot-filters/filterscape"};
        var template = JOptionPane.showInputDialog(this, "Choose a template:", "Create new filter",
                JOptionPane.PLAIN_MESSAGE, null, templateOptions, "blank script");
        if (template == null) {
            return;
        }
        if (template.equals(templateOptions[1])) {
            showMessageDialog(this, "To set up filterscape, use the link in the plugin panel to navigate to filterscape.xyz");
            return;
        }

        var newName = showInputDialog(this, "Please enter a name:");
        if (newName == null || newName.isBlank()) {
            return;
        }
        if (plugin.hasFilter(newName)) {
            plugin.addChatMessage("There's already a filter named " + quote(newName) + ", abort.");
            return;
        }

        String newSrc;
        if (template.equals(templateOptions[0])) {
            newSrc = "meta { name = " + quote(newName) + "; }\n" +
                    String.join("\n", "", TUTORIAL_TEXT, "", EXAMPLE_TEXT);
        } else { // ?
            log.warn("selected nonexistent filter template");
            return;
        }

        try {
            plugin.getFilterManager().saveNewFilter(newName, newSrc);
        } catch (Exception e) {
            log.warn("create new filter", e);
            return;
        }

        onReloadFilters();
    }

    private void onImportClipboard() {
        var newSrc = getClipboard();
        if (newSrc == null) {
            plugin.addChatMessage("Import failed: no text in clipboard.");
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

    private void onImportConfig() {
        var initialName = plugin.getClient().getLocalPlayer() != null
                ? plugin.getClient().getLocalPlayer().getName() + "/"
                : "player/";
        var finalName = showInputDialog(this, "Enter a filter name:", initialName);
        if (finalName == null) {
            return;
        }
        if (plugin.hasFilter(finalName)) {
            plugin.addChatMessage("There's already a filter named " + quote(finalName) + ", abort.");
            return;
        }

        var src = configToFilterSource(plugin.getConfig(), finalName, TUTORIAL_TEXT);
        try {
            plugin.getFilterManager().saveNewFilter(finalName, src);
        } catch (Exception e) {
            log.warn("import filter from config", e);
            return;
        }

        plugin.getConfig().setHighlightedItems("");
        plugin.getConfig().setHiddenItems("");
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
}

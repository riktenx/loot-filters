package com.lootfilters;

import com.lootfilters.lang.Sources;
import lombok.SneakyThrows;
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
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.lootfilters.util.FilterUtil.configToFilterSource;
import static com.lootfilters.util.TextUtil.quote;
import static javax.swing.JOptionPane.showInputDialog;
import static net.runelite.client.util.ImageUtil.loadImageResource;

@Slf4j
public class LootFiltersPanel extends PluginPanel {
    private static final String NONE_ITEM = "<none>";
    private static final String TUTORIAL_TEXT = "// Welcome to the loot filter\n" +
            "// For more information on \n" +
            "// usage, please check\n" +
            "// https://github.com/riktenx/loot-filters/blob/main/guides/loot-filters.md";
    private static final String EXAMPLE_TEXT = "// Here's an example:\nif (name:\"Herring\") {\n  color = RED;\n}";

    private final LootFiltersPlugin plugin;
    private final JComboBox<String> filterSelect;
    private final JPanel root;

    public LootFiltersPanel(LootFiltersPlugin plugin) throws Exception {
        this.plugin = plugin;

        filterSelect = new JComboBox<>();

        root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        init();
    }

    private void init() {
        var top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var mid = new JPanel(new FlowLayout(FlowLayout.LEFT));

        var label = new JLabel("Active filter:");
        var createNew = createIconButton("create_filter",
                "Create a new empty filter.",
                this::onCreateEmptyFilter);
        var importConfig = createIconButton("import_config",
                "Import item highlight and hide lists into a new filter. Doing this will also reset those lists.",
                this::onImportConfig);

        var reloadFilters = createIconButton("reload_icon",
                "Reload filters from disk.",
                this::onReloadFilters);
        var browseFolder = createIconButton("folder_icon",
                "View the filters directory in the system file browser.",
                this::onBrowseFolder);

        top.add(createNew);
        top.add(importConfig);
        top.add(Box.createHorizontalStrut(130));
        top.add(reloadFilters);
        top.add(browseFolder);

        mid.add(label);

        root.add(top);
        root.add(mid);
        root.add(filterSelect);

        add(root);

        reflowFilterSelect(plugin.getFilterManager().loadFilters(), plugin.getSelectedFilterName());
    }

    @SneakyThrows
    private void onCreateEmptyFilter() {
        String[] templateOptions = {"blank script", "loot-filters/filterscape"};
        var template = JOptionPane.showInputDialog(this, "Choose a template:","Create new filter",
                JOptionPane.PLAIN_MESSAGE, null, templateOptions, "blank script");
        if (template == null) {
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
                    String.join("\n","", TUTORIAL_TEXT, "", EXAMPLE_TEXT);
        } else { // loot-filters/filterscape
            newSrc = Sources.getReferenceSource()
                    .replace("    name = \"loot-filters/filterscape\";", "name = " + quote(newName) + ";");
        }

        try {
            plugin.getFilterManager().saveNewFilter(newName, newSrc);
        } catch (Exception e) {
            log.warn("create new filter", e);
            return;
        }

        onReloadFilters();
    }

    @SneakyThrows
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

    private static ImageIcon icon(String name) {
        var img = loadImageResource(LootFiltersPanel.class, "/com/lootfilters/icons/" + name + ".png");
        return new ImageIcon(img);
    }

    private void onReloadFilters() {
        plugin.reloadFilters();
        reflowFilterSelect(plugin.getParsedUserFilters(), plugin.getSelectedFilterName());
    }

    private void onBrowseFolder() {
        try {
            Desktop.getDesktop().open(LootFiltersPlugin.FILTER_DIRECTORY);
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

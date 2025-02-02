package com.lootfilters;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.lootfilters.util.FilterUtil.configToFilterSource;
import static com.lootfilters.util.TextUtil.quote;
import static java.awt.Desktop.getDesktop;
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
        initControls();
    }

    private void init() {
        var top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var mid = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var textPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

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
        root.add(textPanel);

        add(root);
    }

    private void initControls() {
        reflowFilterSelect(plugin.getStorageManager().loadFilters(), plugin.getSelectedFilterName());
    }

    @SneakyThrows
    private void onCreateEmptyFilter(){
        var newName = showInputDialog(this, "Please enter a name:");
        if (newName == null || newName.isBlank()) {
            return;
        }
        if (plugin.hasFilter(newName)) {
            plugin.addChatMessage("There's already a filter named " + quote(newName) + ", abort.");
            return;
        }

        try {
            var newSrc = String.join("\n","", TUTORIAL_TEXT,"", EXAMPLE_TEXT);
            newSrc = "meta { name = " + quote(newName) + "; }\n" + newSrc;
            plugin.getStorageManager().saveNewFilter(newName, newSrc);

        } catch (Exception e) {
            log.warn(e.getMessage());
            return;
        }

        onReloadFilters();
    }

    @SneakyThrows
    private void onImportConfig() {
        var initialName = plugin.getClient().getLocalPlayer() != null
                ? plugin.getClient().getLocalPlayer().getName() + "/"
                : "player/";
        var newName = showInputDialog(this, "Enter a filter name:", initialName);
        if (newName == null) {
            return;
        }
        if (plugin.hasFilter(newName)) {
            plugin.addChatMessage("There's already a filter named " + quote(newName) + ", abort.");
            return;
        }

        try {
            var src = configToFilterSource(plugin.getConfig(), newName, TUTORIAL_TEXT);
            plugin.getStorageManager().saveNewFilter(newName, src);
        } catch (Exception e) {
            log.warn(e.getMessage());
            return;
        }

        plugin.getConfig().setHighlightedItems("");
        plugin.getConfig().setHiddenItems("");
        onReloadFilters();
    }

    private void onReloadFilters() {
        plugin.reloadFilters();
        reflowFilterSelect(plugin.getParsedUserFilters(), plugin.getSelectedFilterName());
    }

    private void onBrowseFolder() {
        try {
            getDesktop().open(FilterStorageManager.filterDirectory());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
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

    private static ImageIcon icon(String name) {
        var img = loadImageResource(LootFiltersPanel.class, "/com/lootfilters/icons/" + name + ".png");
        return new ImageIcon(img);
    }
}

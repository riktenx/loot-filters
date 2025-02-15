package com.lootfilters;

import com.lootfilters.lang.CompileException;
import com.lootfilters.lang.Sources;
import lombok.SneakyThrows;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;

import static com.lootfilters.util.CollectionUtil.append;
import static com.lootfilters.util.FilterUtil.configToFilterSource;
import static com.lootfilters.util.TextUtil.quote;
import static java.util.Collections.emptyList;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showInputDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static net.runelite.client.util.ImageUtil.loadImageResource;

public class LootFiltersPanel extends PluginPanel {
    private static final String NONE_ITEM = "<none>";
    private static final String NONE_TEXT = "Select a filter to display its source.";
    private static final String TUTORIAL_TEXT = "// Welcome to the loot filter\n" +
            "// For more information on \n" +
            "// usage, please check\n" +
            "// https://github.com/riktenx/loot-filters/blob/main/guides/loot-filters.md";
    private static final String EXAMPLE_TEXT = "// Here's an example:\nif (name:\"Herring\") {\n  color = RED;\n}";
    private static final Font TEXT_FONT_ACTIVE = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final Color TEXT_BG_ACTIVE = Color.decode("#1e1e1e");

    private final LootFiltersPlugin plugin;
    private final JComboBox<String> filterSelect;
    private final JTextArea filterText;
    private final JButton saveChanges;
    private final JPanel root;

    public LootFiltersPanel(LootFiltersPlugin plugin) throws Exception {
        this.plugin = plugin;

        filterSelect = new JComboBox<>();
        filterText = new JTextArea(23, 30);
        saveChanges = new JButton("Save");

        root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        init();
        initControls();
    }

    private void init() {
        var top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var textButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var textPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        var label = new JLabel("Active filter:");
        var createNew = createIconButton("create_filter",
                "Create a new empty filter.",
                this::onCreateEmptyFilter);
        var importClipboard = createIconButton("paste_icon",
                "Import new filter from clipboard.",
                this::onImportClipboard);
        var importConfig = createIconButton("import_config",
                "Import item highlight and hide lists into a new filter. Doing this will also reset those lists.",
                this::onImportConfig);
        var deleteActive = createIconButton("delete_active",
                "Delete the currently active filter.",
                this::onDeleteActive);
        var deleteAll = new JButton("Delete all");
        deleteAll.addActionListener(it -> onDeleteAll());
        saveChanges.addActionListener(it -> onSaveChanges());

        top.add(label);
        top.add(createNew);
        top.add(importClipboard);
        top.add(importConfig);
        top.add(deleteActive);
        textButtons.add(deleteAll);
        textButtons.add(Box.createHorizontalStrut(50));
        textButtons.add(saveChanges);
        textPanel.add(new JScrollPane(filterText));

        root.add(top);
        root.add(filterSelect);
        root.add(textButtons);
        root.add(textPanel);

        add(root);
    }

    private void initControls() throws IOException {
        var filters = plugin.getUserFilters();
        filterSelect.addItem(NONE_ITEM);
        for (var filter : filters) {
            filterSelect.addItem(LootFilter.fromSource(filter).getName());
        }

        var index = plugin.getUserFilterIndex();
        if (index <= filters.size() - 1) {
            filterSelect.setSelectedIndex(index + 1);
        }

        filterSelect.addActionListener(this::onFilterSelect);

        filterText.setLineWrap(true);
        filterText.getDocument().addDocumentListener(new DocumentListener() {
            private void onChange() {
                var index = plugin.getUserFilterIndex();
                if (index == -1) {
                    saveChanges.setVisible(false);
                    return;
                }

                var existingSrc = plugin.getUserFilters().get(index);
                saveChanges.setVisible(!existingSrc.equals(filterText.getText()));
            }

            @Override public void insertUpdate(DocumentEvent e) { onChange(); }
            @Override public void removeUpdate(DocumentEvent e) { onChange(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange(); }
        });
        updateFilterText(index);
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

        String newSrc;
        if (template.equals(templateOptions[0])) {
            newSrc = "meta { name = " + quote(newName) + "; }\n" +
                    String.join("\n","", TUTORIAL_TEXT, "", EXAMPLE_TEXT);
        } else { // loot-filters/filterscape
            newSrc = Sources.getReferenceSource()
                    .replace("    name = \"loot-filters/filterscape\";", "name = " + quote(newName) + ";");
        }

        if (tryUpdateExisting(newName, newSrc)) {
            return;
        }

        addNewFilter(newName, newSrc);
    }

    private void onImportClipboard() {
        var newSrc = getClipboard();
        if (newSrc == null) {
            plugin.addChatMessage("No text in clipboard.");
            return;
        }

        LootFilter newFilter;
        try {
            newFilter = LootFilter.fromSource(newSrc);
        } catch (CompileException e) {
            plugin.addChatMessage("Failed to load filter from clipboard: " + e.getMessage());
            return;
        }

        if (newFilter.getName() == null || newFilter.getName().isBlank()) {
            var newName = showInputDialog(this, "This filter does not have a name. Enter one:");
            if (newName == null || newName.isBlank()) {
                return;
            }
            newSrc = "meta { name = " + quote(newName) + "; }\n" + newSrc;
            newFilter.setName(newName);
        }

        if (tryUpdateExisting(newFilter.getName(), newSrc)) {
            return;
        }

        addNewFilter(newFilter.getName(), newSrc);
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

        var src = configToFilterSource(plugin.getConfig(), finalName, TUTORIAL_TEXT);
        if (tryUpdateExisting(finalName, src)) {
            return;
        }

        plugin.getConfig().setHighlightedItems("");
        plugin.getConfig().setHiddenItems("");
        addNewFilter(finalName, src);
    }

    private void onFilterSelect(ActionEvent event) {
        var realIndex = filterSelect.getSelectedIndex() - 1;
        plugin.setUserFilterIndex(realIndex);
        updateFilterText(realIndex);
    }

    private void onDeleteActive() {
        var toDelete = filterSelect.getSelectedIndex() - 1;
        if (plugin.getUserFilters().isEmpty() || toDelete == -1) {
            return;
        }
        if (!confirm("Delete the active loot filter?")) {
            return;
        }

        var newCfg = new ArrayList<>(plugin.getUserFilters());
        newCfg.remove(toDelete);

        filterSelect.removeItemAt(toDelete + 1);
        filterSelect.setSelectedIndex(0);
        plugin.setUserFilters(newCfg);
        plugin.setUserFilterIndex(-1);
        updateFilterText(-1);
    }

    private void onDeleteAll() {
        if (!confirm("Delete all loot filters?")) { return; }
        if (!confirm("Are you sure?")) { return; }

        filterSelect.removeActionListener(this::onFilterSelect);
        filterSelect.removeAllItems();
        filterSelect.addItem(NONE_ITEM);
        filterSelect.setSelectedIndex(0);
        plugin.setUserFilters(emptyList());
        plugin.setUserFilterIndex(-1);
        updateFilterText(-1);
        invokeLater(() -> filterSelect.addActionListener(this::onFilterSelect));
    }

    private void onSaveChanges() {
        var newSrc = filterText.getText();
        try {
            LootFilter.fromSource(newSrc);
        } catch (CompileException e) {
            plugin.addChatMessage("Cannot update active filter: " + e.getMessage());
            return;
        }

        if (!confirm("Save changes to the active filter?")) {
            return;
        }

        var filters = plugin.getUserFilters();
        filters.set(plugin.getUserFilterIndex(), newSrc);
        plugin.setUserFilters(filters);
        saveChanges.setVisible(false);
    }

    private boolean tryUpdateExisting(String newName, String newSrc) {
        var existing = plugin.getUserFilters();
        for (int i = 0; i < filterSelect.getItemCount(); ++i) {
            if (!filterSelect.getItemAt(i).equals(newName)) {
                continue;
            }
            if (!confirm("Filter " + quote(newName) + " already exists. Update it?")) {
                return true;
            }

            existing.set(i - 1, newSrc);
            plugin.setUserFilters(existing);
            return true;
        }
        return false;
    }

    private void updateFilterText(int index) {
        if (index > -1) {
            filterText.setText(plugin.getUserFilters().get(index));
            filterText.setEnabled(true);
            filterText.setFont(TEXT_FONT_ACTIVE);
        } else {
            filterText.setText(NONE_TEXT);
            filterText.setEnabled(false);
            filterText.setFont(FontManager.getRunescapeFont());
        }
        filterText.setCaretPosition(0);
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

    private void addNewFilter(String name, String src) {
        filterSelect.addItem(name);
        plugin.setUserFilters(append(plugin.getUserFilters(), src));
        plugin.setUserFilterIndex(filterSelect.getItemCount() - 2);
        invokeLater(() -> filterSelect.setSelectedIndex(filterSelect.getItemCount() - 1));
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
}

package com.lootfilters;

import com.lootfilters.lang.CompileException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static com.lootfilters.util.TextUtil.quote;
import static javax.swing.JOptionPane.showConfirmDialog;

@Slf4j
@Singleton
public class LootFiltersPanel extends PluginPanel {
    private static final String NONE_ITEM = "<none>";
    private static final String NONE_DESCRIPTION = "Select a filter to show its description.";
    private static final String BLANK_DESCRIPTION = "<no description provided>";

    private final LootFiltersPlugin plugin;
	private final LootFilterManager lootFilterManager;

    private JPanel root;
	private JComboBox<String> filterSelect;
    private JTextArea filterDescription;

	@Inject
    public LootFiltersPanel(LootFiltersPlugin plugin, LootFilterManager lootFilterManager) {
        this.plugin = plugin;
		this.lootFilterManager = lootFilterManager;

		init();
    }

    public void init() {
		root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

		filterSelect = new JComboBox<>();

		filterDescription = new JTextArea();
		filterDescription.setLineWrap(true);
		filterDescription.setEditable(false);

        var top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
        var mid = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));

        var label = new JLabel("Active filter:");
        var importClipboard = createIconButton(Icons.CLIPBOARD_PASTE,
                "Import filter from clipboard.",
                this::onImportClipboard);

        var reloadFilters = createIconButton(Icons.RELOAD,
                "Reload filters from disk.",
                this::onReloadFilters);
        var browseFolder = createIconButton(Icons.FOLDER,
                "View the plugin directory, where filters, sound files, and icon files should be placed, in the system file browser.",
                this::onBrowseFolder);

        var openFiltersite = new JLabel("<html><u>filterscape.xyz</u></html>");
        openFiltersite.setForeground(Color.decode("#00a0ff"));
        openFiltersite.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                LinkBrowser.browse("https://filterscape.xyz");
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
    }

    private void onImportClipboard() {
        var newSrc = getClipboard();
        if (newSrc == null) {
            plugin.addChatMessage("Import failed: no text in clipboard.");
            return;
        }

        if (newSrc.startsWith("https://filterscape.xyz/import")) {
            plugin.addChatMessage("This is a link to import a filter on filterscape.xyz, taking you there now...");
            LinkBrowser.browse(newSrc + "&pluginRedirect=true");
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

        var existing = lootFilterManager.getFilenames();
		var filename = LootFilterManager.toFilename(newFilter.getName());
		if (existing.contains(filename)) {
			if (!confirm("File " + quote(filename) + " already exists. Update it?")) {
				return;
			}

			try {
				lootFilterManager.updateFilter(filename, newSrc);
			} catch (Exception e) {
				plugin.addChatMessage("Import failed: " + e.getMessage());
				return;
			}

			plugin.addChatMessage("Import ok.");
			plugin.setSelectedFilter(filename);
			return;
		}

        try {
            lootFilterManager.createFilter(newFilter.getName(), newSrc);
        } catch (Exception e) {
            plugin.addChatMessage("Import failed: " + e.getMessage());
            return;
        }

		plugin.addChatMessage("Import ok.");
        plugin.setSelectedFilter(filename);
    }

    private void onFilterSelect(ActionEvent event) {
        var selected = (String) filterSelect.getSelectedItem();
        plugin.setSelectedFilter(NONE_ITEM.equals(selected) ? null : selected);
    }

    private JButton createIconButton(BufferedImage icon, String tooltip, Runnable onClick) {
        var button = new JButton("", new ImageIcon(icon));
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

    private void onReloadFilters() {
		lootFilterManager.reload().thenAccept(plugin::onSelectedFilterReloaded);
    }

    private void onBrowseFolder() {
        LinkBrowser.open(LootFiltersPlugin.PLUGIN_DIRECTORY.getAbsolutePath());
    }

    public void reflowFilterSelect(List<String> filters, String selected) {
        for (var l : filterSelect.getActionListeners()) {
            filterSelect.removeActionListener(l);
        }

        filterSelect.removeAllItems();
        filterSelect.addItem(NONE_ITEM);
        for (var filter : filters) {
            filterSelect.addItem(filter);
        }

        if (filters.contains(selected)) { // selected filter could be gone
            filterSelect.setSelectedItem(selected);
        } else {
            plugin.setSelectedFilter(null);
        }
        filterSelect.addActionListener(this::onFilterSelect);
    }

    public void reflowFilterDescription() {
        if (plugin.getSelectedFilter() == null) {
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

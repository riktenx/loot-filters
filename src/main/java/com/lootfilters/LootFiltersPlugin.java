package com.lootfilters;

import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.Notifier;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.lootfilters.util.FilterUtil.withConfigMatchers;
import static com.lootfilters.util.TextUtil.quote;
import static net.runelite.client.util.ImageUtil.loadImageResource;

@Slf4j
@PluginDescriptor(
	name = "Loot Filters"
)
@Getter
public class LootFiltersPlugin extends Plugin {
	public static final String CONFIG_GROUP = "loot-filters";
	public static final String SELECTED_FILTER_KEY = "selected-filter";
	public static final String PLUGIN_DIR = "loot-filters";
	public static final String SOUND_DIR = "sounds";
	public static final String FILTER_DIR = "filters";

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar clientToolbar;

	@Inject private LootFiltersConfig config;
	@Inject private LootFiltersOverlay overlay;
	@Inject private LootFiltersMouseAdapter mouseAdapter;
	@Inject private LootFiltersHotkeyListener hotkeyListener;

	@Inject private Gson gson;
	@Inject private OverlayManager overlayManager;
	@Inject private KeyManager keyManager;
	@Inject private MouseManager mouseManager;
	@Inject private ConfigManager configManager;
	@Inject private ItemManager itemManager;
	@Inject private Notifier notifier;

	private LootFiltersPanel pluginPanel;
	private NavigationButton pluginPanelNav;

	private final TileItemIndex tileItemIndex = new TileItemIndex();
	private final LootbeamIndex lootbeamIndex = new LootbeamIndex(this);
	private final MenuEntryComposer menuEntryComposer = new MenuEntryComposer(this);
	private final FilterStorageManager storageManager = new FilterStorageManager(this);

	private LootFilter activeFilter;
	private LootFilter currentAreaFilter;
	private List<LootFilter> parsedUserFilters;

	@Setter private int hoveredItem = -1;
	@Setter private boolean hotkeyActive = false;
	@Setter private boolean overlayEnabled = true;

	public LootFilter getActiveFilter() {
		return currentAreaFilter != null ? currentAreaFilter : activeFilter;
	}

	public String getSelectedFilterName() {
		return configManager.getConfiguration(CONFIG_GROUP, SELECTED_FILTER_KEY);
	}

	public void setSelectedFilterName(String name) {
		if (name != null) {
			configManager.setConfiguration(CONFIG_GROUP, SELECTED_FILTER_KEY, name);
		} else {
			configManager.unsetConfiguration(CONFIG_GROUP, SELECTED_FILTER_KEY);
		}
	}

	public LootFilter getSelectedFilter() {
		return parsedUserFilters.stream()
				.filter(it -> it.getName().equals(getSelectedFilterName()))
				.findFirst().orElse(LootFilter.Nop);
	}

	public boolean hasFilter(String name) {
		return parsedUserFilters.stream().anyMatch(it -> it.getName().equals(name));
	}

	public void addChatMessage(String msg) {
		clientThread.invoke(() -> {
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, "loot-filters", false);
		});
	}

	public String getItemName(int id) {
		return itemManager.getItemComposition(id).getName();
	}

	@Override
	protected void startUp() throws Exception {
		initPluginDirectory();

		overlayManager.add(overlay);

		parsedUserFilters = storageManager.loadFilters();
		loadSelectedFilter();

		pluginPanel = new LootFiltersPanel(this);
		pluginPanelNav = NavigationButton.builder()
				.tooltip("Loot Filters")
				.icon(loadImageResource(this.getClass(), "/com/lootfilters/icons/panel.png"))
				.panel(pluginPanel)
				.build();
		clientToolbar.addNavigation(pluginPanelNav);
		keyManager.registerKeyListener(hotkeyListener);
		mouseManager.registerMouseListener(mouseAdapter);

		Migrations.run(this);
	}

	private void initPluginDirectory() {
		var root = new File(RuneLite.RUNELITE_DIR, PLUGIN_DIR);
		var sounds = new File(root, SOUND_DIR);
		var filters = new File(root, FILTER_DIR);
		root.mkdir();
		sounds.mkdir();
		filters.mkdir();
	}

	@Override
	protected void shutDown() {
		overlayManager.remove(overlay);

		tileItemIndex.clear();
		lootbeamIndex.clear();

		clientToolbar.removeNavigation(pluginPanelNav);
		keyManager.unregisterKeyListener(hotkeyListener);
		mouseManager.unregisterMouseListener(mouseAdapter);
	}

	@Provides
	LootFiltersConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(LootFiltersConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) throws Exception {
		if (!event.getGroup().equals(CONFIG_GROUP)) {
			return;
		}

		loadSelectedFilter();
		if (!config.autoToggleFilters()) {
			currentAreaFilter = null;
		} // if we're transitioning TO enabled, do nothing - onGameTick() will handle it
		clientThread.invoke(lootbeamIndex::reset);
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event) {
		var tile = event.getTile();
		var item = event.getItem();
		tileItemIndex.put(tile, item);

		var match = getActiveFilter().findMatch(this, item);
		if (match == null) {
			return;
		}

		if (match.isShowLootbeam()) {
			var beam = new Lootbeam(client, clientThread, tile.getWorldLocation(), match.getLootbeamColor(), Lootbeam.Style.MODERN);
			lootbeamIndex.put(tile, item, beam);
		}
		if (match.isNotify()) {
			notifier.notify(getItemName(item.getId()));
		}
		if (match.getSound() != null) {
			match.getSound().play(this);
		}
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event) {
		var tile = event.getTile();
		var item = event.getItem();
		tileItemIndex.remove(tile, item);
		lootbeamIndex.remove(tile, item); // idempotent, we don't care if there wasn't a beam
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		scanAreaFilter();
		reapTileItems();
	}

	@Subscribe
	private void onMenuEntryAdded(MenuEntryAdded event) {
		menuEntryComposer.onMenuEntryAdded(event.getMenuEntry());
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event) {
		menuEntryComposer.onMenuOpened();
	}

	private void loadSelectedFilter() {
		activeFilter = withConfigMatchers(getSelectedFilter(), config);
	}

	private void scanAreaFilter() {
		if (!config.autoToggleFilters()) {
			return;
		}

		var player = client.getLocalPlayer();
		if (player == null) {
			return;
		}

		var p = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		var match = parsedUserFilters.stream()
				.filter(it -> it.isInActivationArea(p))
				.findFirst().orElse(null);
		if (match != null && (currentAreaFilter == null || !Objects.equals(match.getName(), currentAreaFilter.getName()))) {
			addChatMessage("Entering area for filter " + quote(match.getName()));
			currentAreaFilter = withConfigMatchers(match, config);
		} else if (match == null && currentAreaFilter != null) {
			addChatMessage("Leaving area for filter " + quote(currentAreaFilter.getName()));
			currentAreaFilter = null;
		}
	}

	public void reloadFilters() {
		parsedUserFilters = storageManager.loadFilters();
		loadSelectedFilter();
	}

	private void reapTileItems() {
		var toDispatch = tileItemIndex.entrySet().stream()
				.filter(it -> it.getKey().getItemLayer() == null)
				.flatMap(entry -> entry.getValue().stream()
						.map(item -> new ItemDespawned(entry.getKey(), item)))
				.collect(Collectors.toList());
		for (var event : toDispatch) {
			onItemDespawned(event);
		}
	}
}

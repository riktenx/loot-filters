package com.lootfilters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import com.lootfilters.migration.Migrate_133_140;
import com.lootfilters.model.DisplayConfigIndex;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.util.AudioPlayer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.Notifier;
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
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.lootfilters.util.FilterUtil.withConfigMatchers;
import static com.lootfilters.util.TextUtil.quote;
import static net.runelite.client.RuneLite.RUNELITE_DIR;
import static net.runelite.client.util.ImageUtil.loadImageResource;

@Slf4j
@PluginDescriptor(
	name = "Loot Filters",
	description = "Alternative implementation of the ground items plugin with scriptable loot filters.",
	tags = {"loot", "filters", "improved", "ground", "items"},
	conflicts = {"Ground Items"}
)
@Getter
public class LootFiltersPlugin extends Plugin {
	public static final String CONFIG_GROUP = "loot-filters";
	public static final String USER_FILTERS_KEY = "user-filters";
	public static final String USER_FILTERS_INDEX_KEY = "user-filters-index";
	public static final String SELECTED_FILTER_KEY = "selected-filter";

	public static final File PLUGIN_DIRECTORY = new File(RUNELITE_DIR, "loot-filters");
	public static final File FILTER_DIRECTORY = new File(PLUGIN_DIRECTORY, "filters");
	public static final File SOUND_DIRECTORY = new File(PLUGIN_DIRECTORY, "sounds");

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
	private final DisplayConfigIndex displayIndex = new DisplayConfigIndex(this);
	private final MenuEntryComposer menuEntryComposer = new MenuEntryComposer(this);
	private final LootFilterManager filterManager = new LootFilterManager(this);
	private final AudioPlayer audioPlayer = new AudioPlayer(); // remove when https://github.com/runelite/runelite/pull/18745 is merged
	private final ExecutorService audioDispatcher = Executors.newSingleThreadExecutor();
	private final Set<String> queuedAudio = new HashSet<>();

	private LootFilter activeFilter;
	private LootFilter currentAreaFilter;
	private List<LootFilter> parsedUserFilters;

	@Setter private int hoveredItem = -1;
	@Setter private int hoveredHide = -1;
	@Setter private int hoveredHighlight = -1;
	@Setter private boolean hotkeyActive = false;
	@Setter private boolean overlayEnabled = true;

	@Inject @Named("developerMode") boolean developerMode;

	@Getter private boolean debugEnabled = false;

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

	public List<String> getUserFilters() {
		var cfg = configManager.getConfiguration(CONFIG_GROUP, USER_FILTERS_KEY);
		if (cfg == null || cfg.isEmpty()) {
			return new ArrayList<>();
		}

		var type = new TypeToken<List<String>>(){}.getType();
        return gson.fromJson(configManager.getConfiguration(CONFIG_GROUP, USER_FILTERS_KEY), type);
	}

	public int getUserFilterIndex() {
		var indexCfg = configManager.getConfiguration(CONFIG_GROUP, USER_FILTERS_INDEX_KEY);
        var index = indexCfg == null || indexCfg.isEmpty()
				? -1
				: Integer.parseInt(indexCfg);
		if (index > getUserFilters().size() -1) {
			log.warn("User filter index {} is out of bounds, number of filters: {}", index, getUserFilters().size());
			return -1;
		}
		return Math.max(index, -1);
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

		parsedUserFilters = filterManager.loadFilters();
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

		Migrate_133_140.run(this);
	}

	@Override
	protected void shutDown() {
		overlayManager.remove(overlay);

		clearIndices();

		clientToolbar.removeNavigation(pluginPanelNav);
		keyManager.unregisterKeyListener(hotkeyListener);
		mouseManager.unregisterMouseListener(mouseAdapter);
	}

	private void initPluginDirectory() {
		PLUGIN_DIRECTORY.mkdir();
		FILTER_DIRECTORY.mkdir();
		SOUND_DIRECTORY.mkdir();
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
		clientThread.invoke(() -> {
			displayIndex.reset();
			lootbeamIndex.reset();
		});
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event) {
		var tile = event.getTile();
		var item = new PluginTileItem(this, event.getItem());
		tileItemIndex.put(tile, item);

		var match = getActiveFilter().findMatch(this, item);
		if (match == null) {
			return;
		}

		displayIndex.put(item, match);
		if (match.isShowLootbeam()) {
			var beam = new Lootbeam(client, clientThread, tile.getWorldLocation(), match.getLootbeamColor(), Lootbeam.Style.MODERN);
			lootbeamIndex.put(tile, item, beam);
		}
		if (match.isNotify()) {
			notifier.notify(item.getName());
		}
		if (match.getSound() != null && config.soundVolume() > 0) {
			queuedAudio.add(match.getSound());
		}
	}

	@Subscribe
	public void onItemQuantityChanged(ItemQuantityChanged event) {
		onItemDespawned(new ItemDespawned(event.getTile(), event.getItem()));
		clientThread.invokeLater(() -> onItemSpawned(new ItemSpawned(event.getTile(), event.getItem())));
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event) {
		var tile = event.getTile();
		var item = new PluginTileItem(this, event.getItem());
		tileItemIndex.remove(tile, item); // all of these are ultimately idempotent
		lootbeamIndex.remove(tile, item);
		displayIndex.remove(item);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOADING) {
			clearIndices();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		scanAreaFilter();
	}

	@Subscribe
	private void onMenuEntryAdded(MenuEntryAdded event) {
		menuEntryComposer.onMenuEntryAdded(event.getMenuEntry());
	}

	@Subscribe
	public void onClientTick(ClientTick event) {
		menuEntryComposer.onClientTick();

		if (!queuedAudio.isEmpty()) {
			flushAudio();
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event) {
		if (developerMode && event.getCommand().equals("lfDebug")) {
			debugEnabled = !debugEnabled;
		}
	}

	private void flushAudio() {
		for (var filename : queuedAudio) {
			audioDispatcher.execute(() -> {
				try {
					var soundFile = new File(SOUND_DIRECTORY, filename);
					var gain = 20f * (float) Math.log10(config.soundVolume() / 100f);
					audioPlayer.play(soundFile, gain);
				} catch (Exception e) {
					log.warn("play audio {}", filename, e);
				}
			});
		}
		queuedAudio.clear();
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

	private void clearIndices() {
		tileItemIndex.clear();
		lootbeamIndex.clear();
		displayIndex.clear();
	}

	public void reloadFilters() {
		parsedUserFilters = filterManager.loadFilters();
		loadSelectedFilter();
		clientThread.invoke(() -> {
			displayIndex.reset();
			lootbeamIndex.reset();
		});
	}

	private void loadSelectedFilter() {
		activeFilter = withConfigMatchers(getSelectedFilter(), config);
	}
}

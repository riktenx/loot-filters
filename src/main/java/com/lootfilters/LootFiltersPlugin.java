package com.lootfilters;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.lootfilters.migration.Migrate_133_140;
import com.lootfilters.model.DisplayConfigIndex;
import com.lootfilters.model.IconIndex;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.model.SoundProvider;
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
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import okhttp3.OkHttpClient;

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

import static com.lootfilters.util.FilterUtil.withConfigRules;
import static com.lootfilters.util.TextUtil.quote;
import static net.runelite.client.RuneLite.RUNELITE_DIR;
import static net.runelite.client.util.ImageUtil.loadImageResource;

@Slf4j
@PluginDescriptor(
	name = "Loot Filters",
	description = "Improved ground items plugin with scriptable loot filters.",
	tags = {"loot", "filters", "improved", "ground", "items"},
	conflicts = {"Ground Items"}
)
@Getter
public class LootFiltersPlugin extends Plugin {
	public static final String CONFIG_GROUP = "loot-filters";
	public static final String SELECTED_FILTER_KEY = "selected-filter";

	public static final File PLUGIN_DIRECTORY = new File(RUNELITE_DIR, "loot-filters");
	public static final File FILTER_DIRECTORY = new File(PLUGIN_DIRECTORY, "filters");
	public static final File SOUND_DIRECTORY = new File(PLUGIN_DIRECTORY, "sounds");
	public static final File ICON_DIRECTORY = new File(PLUGIN_DIRECTORY, "icons");

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
	@Inject private SpriteManager spriteManager;
	@Inject private OkHttpClient okHttpClient;

	private LootFiltersPanel pluginPanel;
	private NavigationButton pluginPanelNav;

	private final TileItemIndex tileItemIndex = new TileItemIndex();
	private final LootbeamIndex lootbeamIndex = new LootbeamIndex(this);
	private final DisplayConfigIndex displayIndex = new DisplayConfigIndex(this);
	private final IconIndex iconIndex = new IconIndex(this);

	private final MenuEntryComposer menuEntryComposer = new MenuEntryComposer(this);
	private final LootFilterManager filterManager = new LootFilterManager(this);
	private final AudioPlayer audioPlayer = new AudioPlayer(); // remove when https://github.com/runelite/runelite/pull/18745 is merged
	private final ExecutorService audioDispatcher = Executors.newSingleThreadExecutor();
	private final Set<SoundProvider> queuedAudio = new HashSet<>();
	private final List<String> queuedChatMessages = new ArrayList<>();

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

	public List<LootFilter> getLoadedFilters() {
		var filters = new ArrayList<LootFilter>();
		if (filterManager.getDefaultFilter() != null) {
			filters.add(filterManager.getDefaultFilter());
		}
		filters.addAll(parsedUserFilters);
		return filters;
	}

	public LootFilter getSelectedFilter() {
		return getLoadedFilters().stream()
				.filter(it -> it.getName().equals(getSelectedFilterName()))
				.findFirst().orElse(LootFilter.Nop);
	}

	public boolean hasFilter(String name) {
		return getLoadedFilters().stream().anyMatch(it -> it.getName().equals(name));
	}

	public void addChatMessage(String msg) {
		queuedChatMessages.add(msg);
	}

	public String getItemName(int id) {
		return itemManager.getItemComposition(id).getName();
	}

	@Override
	protected void startUp() {
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

		if (config.fetchDefaultFilter()) {
			filterManager.fetchDefaultFilter(this::onFetchDefaultFilter);
		}

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
		ICON_DIRECTORY.mkdir();
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

		if (event.getKey().equals(LootFiltersConfig.CONFIG_KEY_OVERLAY_PRIORITY)) {
			overlay.setPriority(config.overlayPriority().getValue());
			overlayManager.resetOverlay(overlay);
		}

		if (event.getKey().equals(LootFiltersConfig.CONFIG_KEY_FETCH_DEFAULT_FILTER)) {
			if (config.fetchDefaultFilter()) {
				filterManager.fetchDefaultFilter(this::onFetchDefaultFilter);
			} else {
				var selected = getSelectedFilterName();
				filterManager.setDefaultFilter(null);
				if (selected != null && selected.equals(LootFilterManager.DEFAULT_FILTER_NAME)) {
					selected = null;
					setSelectedFilterName(null);
				}
				pluginPanel.reflowFilterSelect(getLoadedFilters(), selected);
			}
		}

		if (event.getKey().equals(SELECTED_FILTER_KEY)) {
			var selected = getSelectedFilterName();
			if (selected != null && selected.equals(LootFilterManager.DEFAULT_FILTER_NAME)) {
				addChatMessage("Loaded the default filter. Visit filterscape.xyz to configure it.");
			}
		}

		loadSelectedFilter();
		if (!config.autoToggleFilters()) {
			currentAreaFilter = null;
		} // if we're transitioning TO enabled, do nothing - onGameTick() will handle it
		resetDisplay();
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event) {
		var tile = event.getTile();
		var item = new PluginTileItem(this, tile, event.getItem());
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
		if (match.getIcon() != null) {
			iconIndex.inc(match.getIcon(), item);
		}
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event) {
		var tile = event.getTile();
		var item = new PluginTileItem(this, tile, event.getItem());
		var display = displayIndex.get(item);
		tileItemIndex.remove(tile, item); // all of these are ultimately idempotent
		lootbeamIndex.remove(tile, item);
		displayIndex.remove(item);
		if (display != null && display.getIcon() != null) {
			iconIndex.dec(display.getIcon(), item);
		}
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
		if (!queuedChatMessages.isEmpty()) {
			flushChatMessages();
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event) {
		if (developerMode && event.getCommand().equals("lfDebug")) {
			debugEnabled = !debugEnabled;
		}
	}

	private void flushAudio() {
		for (var provider : queuedAudio) {
			audioDispatcher.execute(() -> provider.play(this));
		}
		queuedAudio.clear();
	}

	private void flushChatMessages() {
		for (var msg : queuedChatMessages) {
			var chatMsg = String.format("<col=00ffff>[Loot Filters]</col>: %s", msg);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", chatMsg, "loot-filters", false);
		}
		queuedChatMessages.clear();
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
		var match = getLoadedFilters().stream()
				.filter(it -> it.isInActivationArea(p))
				.findFirst().orElse(null);
		if (match != null && (currentAreaFilter == null || !Objects.equals(match.getName(), currentAreaFilter.getName()))) {
			addChatMessage("Entering area for filter " + quote(match.getName()));
			currentAreaFilter = withConfigRules(match, config);
			resetDisplay();
		} else if (match == null && currentAreaFilter != null) {
			addChatMessage("Leaving area for filter " + quote(currentAreaFilter.getName()));
			currentAreaFilter = null;
			resetDisplay();
		}
	}

	private void clearIndices() {
		tileItemIndex.clear();
		lootbeamIndex.clear();
		displayIndex.clear();
		iconIndex.clear();
	}

	public void reloadFilters() {
		parsedUserFilters = filterManager.loadFilters();
		loadSelectedFilter();
		resetDisplay();
	}

	private void loadSelectedFilter() {
		activeFilter = withConfigRules(getSelectedFilter(), config);
	}

	private void resetDisplay() {
		clientThread.invoke(() -> {
			displayIndex.reset();
			lootbeamIndex.reset();
			iconIndex.reset();
		});
	}

	private void onFetchDefaultFilter() {
		if (getSelectedFilterName() == null) {
			setSelectedFilterName(LootFilterManager.DEFAULT_FILTER_NAME);
		}
		pluginPanel.reflowFilterSelect(getLoadedFilters(), getSelectedFilterName());
	}
}

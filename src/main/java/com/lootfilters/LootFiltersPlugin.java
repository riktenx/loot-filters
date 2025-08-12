package com.lootfilters;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.lootfilters.migration.Migrate_133_140;
import com.lootfilters.model.DisplayConfigIndex;
import com.lootfilters.model.IconIndex;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.model.SoundProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Tile;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.Notifier;
import net.runelite.client.audio.AudioPlayer;
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
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.lootfilters.util.FilterUtil.withConfigRules;
import static net.runelite.client.RuneLite.RUNELITE_DIR;
import static net.runelite.client.util.ColorUtil.colorToHexCode;
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
	@Inject private OverlayStateIndicator overlayStateIndicator;

	@Inject private Gson gson;
	@Inject private OverlayManager overlayManager;
	@Inject private KeyManager keyManager;
	@Inject private MouseManager mouseManager;
	@Inject private ConfigManager configManager;
	@Inject private ItemManager itemManager;
	@Inject private Notifier notifier;
	@Inject private SpriteManager spriteManager;
	@Inject private OkHttpClient okHttpClient;
	@Inject private AudioPlayer audioPlayer;
	@Inject private InfoBoxManager infoBoxManager;

	private LootFiltersPanel pluginPanel;
	private NavigationButton pluginPanelNav;

	private final TileItemIndex tileItemIndex = new TileItemIndex();
	private final LootbeamIndex lootbeamIndex = new LootbeamIndex(this);
	private final DisplayConfigIndex displayIndex = new DisplayConfigIndex(this);
	private final IconIndex iconIndex = new IconIndex(this);

	private final MenuEntryComposer menuEntryComposer = new MenuEntryComposer(this);
	private final LootFilterManager filterManager = new LootFilterManager(this);
	private final ExecutorService audioDispatcher = Executors.newSingleThreadExecutor();
	private final Set<SoundProvider> queuedAudio = new HashSet<>();
	private final List<String> queuedChatMessages = new ArrayList<>();

	@Getter private LootFilter activeFilter;
	private List<LootFilter> parsedUserFilters;

	@Setter private int hoveredItem = -1;
	@Setter private int hoveredHide = -1;
	@Setter private int hoveredHighlight = -1;
	@Setter private boolean hotkeyActive = false;
	@Setter private boolean isOverlayEnabled = true;

	@Inject @Named("developerMode") boolean developerMode;

	@Getter private boolean debugEnabled = false;

	public boolean isDeveloperMode() {
		return developerMode && debugEnabled;
	}

	public String getSelectedFilterName() {
		return configManager.getConfiguration(CONFIG_GROUP, SELECTED_FILTER_KEY);
	}

	public void setSelectedFilterName(String name) {
		if (name != null) {
			configManager.setConfiguration(CONFIG_GROUP, SELECTED_FILTER_KEY, name);
			if (name.equals(DefaultFilter.FILTERSCAPE.getName()) || name.equals(DefaultFilter.JOESFILTER.getName())) {
				config.setPreferredDefault(name);
			}
		} else {
			configManager.unsetConfiguration(CONFIG_GROUP, SELECTED_FILTER_KEY);
		}
	}

	public List<LootFilter> getLoadedFilters() {
		var filters = new ArrayList<LootFilter>();
		filters.addAll(filterManager.getDefaultFilters());
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
		debugEnabled = !System.getProperty("com.lootfilters.debug", "").isBlank();

		initPluginDirectory();
		overlayManager.add(overlay);
		infoBoxManager.addInfoBox(overlayStateIndicator);

		parsedUserFilters = filterManager.loadFilters();
		loadSelectedFilter();

		pluginPanel = new LootFiltersPanel(this);
		pluginPanelNav = NavigationButton.builder()
				.tooltip("Loot Filters")
				.icon(Icons.PANEL_ICON)
				.panel(pluginPanel)
				.build();
		if (config.showPluginPanel()) {
			clientToolbar.addNavigation(pluginPanelNav);
		}
		keyManager.registerKeyListener(hotkeyListener);
		mouseManager.registerMouseListener(mouseAdapter);

		if (config.fetchDefaultFilters()) {
			filterManager.fetchDefaultFilters(this::onFetchDefaultFilters);
		}

		Migrate_133_140.run(this);
	}

	@Override
	protected void shutDown() {
		overlayManager.remove(overlay);
		infoBoxManager.removeInfoBox(overlayStateIndicator);

		filterManager.getDefaultFilters().clear();
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
	public void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals(CONFIG_GROUP)) {
			return;
		}

		if (event.getKey().equals(LootFiltersConfig.SHOW_PLUGIN_PANEL)) {
			if (config.showPluginPanel()) {
				clientToolbar.addNavigation(pluginPanelNav);
			} else {
				clientToolbar.removeNavigation(pluginPanelNav);
			}
		}

		if (event.getKey().equals(LootFiltersConfig.CONFIG_KEY_FETCH_DEFAULT_FILTERS)) {
			if (config.fetchDefaultFilters()) {
				filterManager.fetchDefaultFilters(this::onFetchDefaultFilters);
			} else {
				var selected = getSelectedFilterName();
				filterManager.getDefaultFilters().clear();
				if (selected != null && selected.equals(DefaultFilter.FILTERSCAPE.getName())) {
					selected = null;
					setSelectedFilterName(null);
				}
				pluginPanel.reflowFilterSelect(getLoadedFilters(), selected);
			}
		}

		if (event.getKey().equals(SELECTED_FILTER_KEY)) {
			var selected = getSelectedFilterName();
			if (DefaultFilter.all().stream().anyMatch(it -> it.getName().equals(selected))) {
				addChatMessage("Loaded " + selected + ". Visit filterscape.xyz to configure it.");
			}
		}

		loadSelectedFilter();
		resetDisplay();

		pluginPanel.reflowFilterDescription();
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event) {
		var tile = event.getTile();
		var item = new PluginTileItem(this, tile, event.getItem());

		tileItemIndex.put(tile, item);
		addItem(tile, item);
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event) {
		var tile = event.getTile();
		var item = tileItemIndex.findItem(event.getItem());

		tileItemIndex.remove(tile, item);
		removeItem(tile, item);
	}

	@Subscribe
	public void onItemQuantityChanged(ItemQuantityChanged event) {
		var tile = event.getTile();
		var item = tileItemIndex.findItem(event.getItem());

		item.setQuantityOverride(event.getNewQuantity());
		removeItem(tile, item);
		addItem(tile, item);
	}

	private void addItem(Tile tile, PluginTileItem item) {
		var match = getActiveFilter().findMatch(this, item);

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
		if (match.getIcon() != null && !match.isCompact()) {
			iconIndex.inc(match.getIcon(), item);
		}
		if(match.isCompact()){
			iconIndex.inc(match.getIcon(), item,config.compactRenderSize());
		}
	}

	private void removeItem(Tile tile, PluginTileItem item) {
		var display = displayIndex.get(item);
		lootbeamIndex.remove(tile, item);
		displayIndex.remove(item);
		if (display != null && display.getIcon() != null) {
			iconIndex.dec(display.getIcon(), item, display.isCompact()? config.compactRenderSize() : 16);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOADING) {
			clearIndices();
		}
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

	private void flushAudio() {
		for (var provider : queuedAudio) {
			audioDispatcher.execute(() -> provider.play(this));
		}
		queuedAudio.clear();
	}

	private void flushChatMessages() {
		for (var msg : queuedChatMessages) {
			// CA_ID:{id}|<msg> is used to embed CA information into a GAMEMESSAGE chat. A side effect of this is that
			// whenever you have |s in a chat message, everything before the first | gets cut off. So, we just throw one
			// at the front of everything as a prophylactic measure since we might show || tokens in parse errors.
			var chatMsg = String.format("|<col=%s>[Loot Filters]</col>: %s", colorToHexCode(config.chatPrefixColor()), msg);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", chatMsg, "loot-filters", false);
		}
		queuedChatMessages.clear();
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

	private void onFetchDefaultFilters() {
		if (getSelectedFilterName() == null) {
			setSelectedFilterName(config.getPreferredDefault());
		}
		pluginPanel.reflowFilterSelect(getLoadedFilters(), getSelectedFilterName());
	}
}

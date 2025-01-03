package com.lootfilters;

import com.google.inject.Provides;
import com.lootfilters.lang.Lexer;
import com.lootfilters.lang.Parser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lootfilters.FilterConfig.findMatch;
import static net.runelite.client.util.ColorUtil.colorTag;

@Slf4j
@PluginDescriptor(
	name = "Loot Filters"
)
@Getter
public class LootFiltersPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private LootFiltersConfig config;
	@Inject private LootFiltersOverlay overlay;
	@Inject private OverlayManager overlayManager;
	@Inject private ConfigManager configManager;
	@Inject private ItemManager itemManager;

	private final TileItemIndex tileItemIndex = new TileItemIndex();
	private final LootbeamIndex lootbeamIndex = new LootbeamIndex();

	private List<FilterConfig> filterConfigs;

	private void loadFilterConfig() throws Exception {
		var userFilters = new Parser(new Lexer(config.filterConfig()).tokenize()).parse();

		filterConfigs = new ArrayList<>();
		filterConfigs.add(FilterConfig.ownershipFilter(config.ownershipFilter()));

		filterConfigs.addAll(userFilters);

		filterConfigs.add(FilterConfig.highlight(config.highlightedItems(), config.highlightColor()));
		filterConfigs.add(FilterConfig.hide(config.hiddenItems()));

		filterConfigs.add(FilterConfig.valueTier(config.enableInsaneItemValueTier(), config.insaneValue(), config.insaneValueColor(), true));
		filterConfigs.add(FilterConfig.valueTier(config.enableHighItemValueTier(), config.highValue(), config.highValueColor(), true));
		filterConfigs.add(FilterConfig.valueTier(config.enableMediumItemValueTier(), config.mediumValue(), config.mediumValueColor(), false));
		filterConfigs.add(FilterConfig.valueTier(config.enableLowItemValueTier(), config.lowValue(), config.lowValueColor(), false));

		filterConfigs.add(FilterConfig.showUnmatched(config.showUnmatchedItems()));

		if (config.alwaysShowValue()) {
			filterConfigs = filterConfigs.stream()
					.map(it -> new FilterConfig(it.getRule(), it.getDisplay().toBuilder()
							.showValue(true)
							.build()))
					.collect(Collectors.toList());
		}
		if (config.alwaysShowDespawn()) {
			filterConfigs = filterConfigs.stream()
					.map(it -> new FilterConfig(it.getRule(), it.getDisplay().toBuilder()
							.showDespawn(true)
							.build()))
					.collect(Collectors.toList());
		}
	}

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);

		loadFilterConfig();
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);

		tileItemIndex.clear();
		lootbeamIndex.clear();
	}

	@Provides
	LootFiltersConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(LootFiltersConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) throws Exception {
		if (event.getGroup().equals("loot-filters")) {
			loadFilterConfig();
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event) {
		var tile = event.getTile();
		var item = event.getItem();
		tileItemIndex.put(tile, item);

		// lootbeams
		var match = filterConfigs.stream()
				.filter(it -> it.test(this, item))
				.findFirst().orElse(null);
		if (match == null || !match.getDisplay().isShowLootbeam()) {
			return;
		}

		var beam = new Lootbeam(client, clientThread, tile.getWorldLocation(), match.getDisplay().getTextColor(), Lootbeam.Style.MODERN);
		lootbeamIndex.put(tile, item, beam);
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event) {
		var tile = event.getTile();
		var item = event.getItem();
		tileItemIndex.remove(tile, item);
		lootbeamIndex.remove(tile, item); // idempotent, we don't care if there wasn't a beam
	}

	@Subscribe
	public void onClientTick(ClientTick event) {
		var entries = client.getMenu().getMenuEntries();
		var wv = client.getTopLevelWorldView();
		var seen = new HashMap<MenuEntry, Boolean>();
		var itemCounts = Stream.of(entries)
				.filter(this::isGroundItem)
				.collect(Collectors.groupingBy(it -> it, Collectors.counting()));

		var newEntries = new ArrayList<MenuEntry>();
		for (var entry : entries) {
			if (seen.containsKey(entry)) {
				continue;
			}
			if (!isGroundItem(entry)) {
				newEntries.add(entry);
				continue;
			}

			seen.put(entry, true);
			newEntries.add(entry);
		}
		for (var entry : newEntries) {
			if (!isGroundItem(entry)) {
				continue;
			}

			var point = WorldPoint.fromScene(wv, entry.getParam0(), entry.getParam1(), wv.getPlane());
			var item = tileItemIndex.findItem(point, entry.getIdentifier());
			var match = findMatch(filterConfigs, this, item);
			if (match != null && !match.isHidden()) {
				if (itemCounts.get(entry) > 1) {
					entry.setTarget(colorTag(match.getTextColor()) + itemManager.getItemComposition(item.getId()).getName() + " x" + itemCounts.get(entry));
				} else if (!entry.getTarget().startsWith(colorTag(match.getTextColor()))) { // shitty idempotency
					entry.setTarget(colorTag(match.getTextColor()) + itemManager.getItemComposition(item.getId()).getName());
				}
			} else {
				entry.setDeprioritized(true);
			}
		}
		client.getMenu().setMenuEntries(newEntries.toArray(MenuEntry[]::new));
	}

	private boolean isGroundItem(MenuEntry entry) {
		var type = entry.getType();
		return type == MenuAction.GROUND_ITEM_FIRST_OPTION
				|| type == MenuAction.GROUND_ITEM_SECOND_OPTION
				|| type == MenuAction.GROUND_ITEM_THIRD_OPTION
				|| type == MenuAction.GROUND_ITEM_FOURTH_OPTION
				|| type == MenuAction.GROUND_ITEM_FIFTH_OPTION
				|| type == MenuAction.EXAMINE_ITEM_GROUND;
	}
}

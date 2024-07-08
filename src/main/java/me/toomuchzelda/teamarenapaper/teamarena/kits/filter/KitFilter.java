package me.toomuchzelda.teamarenapaper.teamarena.kits.filter;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Filter kits based on their kit name.
 * By convention, kit filters are lowercase.
 */
public class KitFilter {

	private static final Map<NamespacedKey, FilterRule> globalRules = new LinkedHashMap<>();
	/** @implNote teams are addressed by their simple name */
	private static final Map<String, Map<NamespacedKey, FilterRule>> teamRules = new HashMap<>();
	private static final Map<String, Map<NamespacedKey, FilterRule>> playerRules = new HashMap<>();

	public static final NamespacedKey ADMIN_KEY = new NamespacedKey(Main.getPlugin(), "admin");

	public static final FilterRule DEFAULT_ADMIN_RULE = new FilterRule(ADMIN_KEY, "Admin rule", FilterAction.block("sniper", "shortbow"));
	private static FilterPreset preset;

	static {
		addGlobalRule(DEFAULT_ADMIN_RULE);
	}

	public record FilterPreset(String name, String desc, ItemStack displayItem, @Nullable FilterAction action) {
		public FilterPreset {
			displayItem = displayItem.clone();
		}

		public FilterPreset(String name, String desc, Material display, @Nullable FilterAction action) {
			this(name, desc, ItemBuilder.of(display)
				.displayName(Component.text(name, NamedTextColor.BLUE))
				.lore(TextUtils.wrapString(desc, Style.style(NamedTextColor.GRAY)))
				.build(), action);
		}

		public FilterPreset(String name, String desc, @Nullable FilterAction action) {
			this(name, desc, Material.PAPER, action);
		}
	}

	public static void addGlobalRule(FilterRule rule) {
		globalRules.put(rule.key(), rule);
	}

	public static void removeGlobalRule(NamespacedKey key) {
		globalRules.remove(key);
	}

	public static void addTeamRule(String team, FilterRule rule) {
		teamRules.computeIfAbsent(team, key -> new HashMap<>()).put(rule.key(), rule);
	}

	public static void removeTeamRule(String team, NamespacedKey key) {
		teamRules.computeIfPresent(team, (ignored, value) -> {
			value.remove(key);
			return value.isEmpty() ? null : value;
		});
	}

	public static void addPlayerRule(String playerName, FilterRule rule) {
		playerRules.computeIfAbsent(playerName, key -> new HashMap<>()).put(rule.key(), rule);
	}

	public static void removePlayerRule(String playerName, NamespacedKey key) {
		playerRules.computeIfPresent(playerName, (ignored, value) -> {
			value.remove(key);
			return value.isEmpty() ? null : value;
		});
	}

	public static Collection<NamespacedKey> getGlobalRules() {
		return Collections.unmodifiableSet(globalRules.keySet());
	}

	public static Collection<NamespacedKey> getTeamRules(String team) {
		Map<NamespacedKey, FilterRule> rules = teamRules.get(team);
		if (rules != null) {
			return rules.keySet();
		} else {
			return Set.of();
		}
	}

	public static Collection<NamespacedKey> getPlayerRules(String player) {
		Map<NamespacedKey, FilterRule> rules = playerRules.get(player);
		if (rules != null) {
			return rules.keySet();
		} else {
			return Set.of();
		}
	}

	public static final Map<String, FilterPreset> PRESETS = Stream.of(
			new FilterPreset("Default", "The default Team Arena™ experience", null),
			// TODO define RWF default here
			new FilterPreset("Red Warfare Default", "Who used Kit Rewind on SnD??", FilterAction.allow("trooper")),
			new FilterPreset("Sniper Duel", "The best FPS player shall prevail", Material.SPYGLASS, FilterAction.allow("sniper")),
			new FilterPreset("Ghost Town", "Where'd everyone go?", Material.WHITE_STAINED_GLASS, FilterAction.allow("ghost")),
			new FilterPreset("Imposter Game", "There is an imposter... ඞ", FilterAction.allow("spy")),
			new FilterPreset("Close-Range Combat", "A battle between true warriors", Material.STONE_SWORD,
				FilterAction.block("burst", "ghost", "engineer", "shortbow", "longbow", "pyro", "sniper")),
			new FilterPreset("Archer Duel", "Bow-spammers rise!", Material.BOW, FilterAction.allow("shortbow", "longbow", "pyro"))
		)
		.collect(Collectors.toUnmodifiableMap(
			p -> p.name.toLowerCase(Locale.ENGLISH).replaceAll("\\W", "_"), p -> p
		));

	// a more efficient way of filtering kits from a set
	private static void mutateSet(Collection<FilterRule> rules, Set<String> set) {
		for (FilterRule rule : rules) {
			switch (rule.action()) {
				case FilterAction.Allow(Set<String> allowedKits) -> set.retainAll(allowedKits);
				case FilterAction.Block(Set<String> blockedKits) -> set.removeAll(blockedKits);
			}
		}
	}


	public static Kit filterKit(TeamArena game, TeamArenaTeam team, Player player, Kit kit) {
		String name = kit.getName().toLowerCase(Locale.ENGLISH);
		Predicate<FilterRule> predicate = rule -> rule.action().filter(name);
		if (globalRules.values().stream().allMatch(predicate)) {
			if (teamRules.getOrDefault(team.getSimpleName(), Map.of()).values().stream().allMatch(predicate)) {
				if (playerRules.getOrDefault(player.getName(), Map.of()).values().stream().allMatch(predicate)) {
					return kit;
				}
			}
		}

		return calculateKits(game, player).iterator().next();
	}

	public static Map<Player, Set<Kit>> calculateKits(TeamArena game, Collection<? extends Player> players) {
		Set<String> allKits = game.getKits().stream()
			.map(Kit::getName)
			.map(kitName -> kitName.toLowerCase(Locale.ENGLISH))
			.collect(Collectors.toSet());
		// 1. apply global rules
		mutateSet(globalRules.values(), allKits);

		Map<TeamArenaTeam, Set<String>> allTeamKits = new HashMap<>();
		Set<Kit> globalKits = null;
		Map<Player, Set<Kit>> allPlayerKits = new HashMap<>();
		for (Player player : players) {
			var playerTeam = Main.getPlayerInfo(player).team;
			// 2. apply team rules
			Set<String> teamKits = allTeamKits.computeIfAbsent(playerTeam, team -> {
				var rules = teamRules.get(team.getSimpleName());
				if (rules == null)
					return allKits;
				var kits = new HashSet<>(allKits);
				mutateSet(rules.values(), kits);
				return kits;
			});
			// 3. apply player rules
			var rules = playerRules.get(player.getName());
			Set<String> kits;
			if (rules == null) {
				kits = teamKits;
			} else {
				kits = new HashSet<>(teamKits);
				mutateSet(rules.values(), kits);
			}
			// finally look up kits by their names
			// FAST PATH: assuming that per-team and per-player rules are rarely used,
			// cache the lookup results for the global rules
			if (kits == allKits) {
				if (globalKits == null)
					globalKits = allKits.stream()
						// not null
						.map(name -> Objects.requireNonNull(game.findKit(name)))
						.collect(Collectors.toUnmodifiableSet());
				allPlayerKits.put(player, globalKits);
			} else {
				allPlayerKits.put(player, kits.stream()
					// not null
					.map(name -> Objects.requireNonNull(game.findKit(name)))
					.collect(Collectors.toUnmodifiableSet()));
			}
		}
		return Map.copyOf(allPlayerKits);
	}

	public static Set<Kit> calculateKits(TeamArena game, Player player) {
		return calculateKits(game, List.of(player)).get(player);
	}

	public static boolean canUseKit(TeamArena game, Player player, Kit kit) {
		return calculateKits(game, player).contains(kit);
	}

	public static void setPreset(@NotNull TeamArena game, FilterPreset preset) {
		if (preset.action == null)
			addGlobalRule(DEFAULT_ADMIN_RULE);
		else
			addGlobalRule(new FilterRule(ADMIN_KEY, "Preset: " + preset.name, preset.action));
		KitFilter.preset = preset;
	}

	public static void setAdminAllowed(TeamArena game, Collection<String> allowed) throws IllegalArgumentException {
		Optional<Kit> fallbackOpt = game.getKits().stream()
			.filter(kit -> allowed.contains(kit.getName().toLowerCase(Locale.ENGLISH)))
			.findFirst();

		if (fallbackOpt.isEmpty()) {
			addGlobalRule(DEFAULT_ADMIN_RULE);
			throw new IllegalArgumentException("Cannot block all kits");
		}

		addGlobalRule(new FilterRule(ADMIN_KEY, "Admin allowed kits", FilterAction.allow(new HashSet<>(allowed))));
		updateKitsFor(game, Bukkit.getOnlinePlayers());
	}

	public static void setAdminBlocked(TeamArena game, Collection<String> blocked) throws IllegalArgumentException {
		preset = null;
		// For anyone not using an allowed kit, set it to a fallback one.
		Optional<Kit> fallbackOpt = game.getKits().stream()
			.filter(kit -> !blocked.contains(kit.getName().toLowerCase(Locale.ENGLISH)))
			.findFirst();

		if (fallbackOpt.isEmpty()) {
			addGlobalRule(DEFAULT_ADMIN_RULE);
			throw new IllegalArgumentException("Cannot block all kits");
		}

		addGlobalRule(new FilterRule(ADMIN_KEY, "Admin blocked kits", FilterAction.block(new HashSet<>(blocked))));
		updateKitsFor(game, Bukkit.getOnlinePlayers());
	}

	public static void updateKitsFor(TeamArena game, Collection<? extends Player> players) {
		var globalFallback = game.getKits().iterator().next();
		var playerAllowedKits = calculateKits(game, players);
		playerAllowedKits.forEach((player, allowedKits) -> {
			var info = Main.getPlayerInfo(player);
			boolean kitAvailable = info.kit == null || allowedKits.contains(info.kit);
			boolean activeKitAvailable = info.activeKit == null || allowedKits.contains(info.activeKit);
			Kit fallbackKit;
			if (allowedKits.isEmpty()) {
				// (in)sanity check
				Main.componentLogger().error(getKitFallbackDebugMessage(game, player, globalFallback));
				fallbackKit = globalFallback;
			} else {
				fallbackKit = allowedKits.iterator().next();
			}
			if (fallbackKit == null) {
				fallbackKit = globalFallback;
			}
			if (!kitAvailable) {
				info.kit = fallbackKit;
			}
			if (!activeKitAvailable) {
				// also change active kit, very safe operation!
				info.activeKit.removeKit(player, info);
				// selected kit is guaranteed to be available
				Main.getGame().givePlayerItems(player, info, true);
			}

			Component message = null;
			if (!activeKitAvailable) {
				message = getActiveKitMessage(fallbackKit);
				player.showTitle(getActiveKitTitle(fallbackKit));
			} else if (!kitAvailable) {
				message = getSelectedKitMessage(fallbackKit);
			}
			if (message != null)
				player.sendMessage(message);
		});
	}

	public static void resetFilter() {
		addGlobalRule(DEFAULT_ADMIN_RULE);
	}

	public static Component inspectRules(@Nullable String team, @Nullable String player) {
		var indent = Component.text("  ");
		var teamRules = team != null ? KitFilter.teamRules.get(team) : null;
		var playerRules = player != null ? KitFilter.playerRules.get(player) : null;
		var lines = new ArrayList<Component>();
		if (!globalRules.isEmpty()) {
			lines.add(Component.text("Global rules:", NamedTextColor.GOLD));
			for (FilterRule rule : globalRules.values()) {
				lines.add(Component.textOfChildren(indent, rule.toComponent()));
			}
		}
		if (teamRules != null && !teamRules.isEmpty()) {
			lines.add(Component.textOfChildren(
				Component.text("Rules specific to "),
				Component.text(team, NamedTextColor.AQUA),
				Component.text(":")
			));
			for (FilterRule rule : teamRules.values()) {
				lines.add(Component.textOfChildren(indent, rule.toComponent()));
			}
		}
		if (playerRules != null && !playerRules.isEmpty()) {
			lines.add(Component.textOfChildren(
				Component.text("Rules specific to "),
				Component.text(player, NamedTextColor.DARK_AQUA),
				Component.text(":")
			));
			for (FilterRule rule : playerRules.values()) {
				lines.add(Component.textOfChildren(indent, rule.toComponent()));
			}
		}
		return Component.join(JoinConfiguration.newlines(), lines);
	}

	public static Component getActiveKitMessage(Kit newKit) {
		return Component.textOfChildren(
			Component.text("The kit you are using has been disabled by an admin.\nYou are now using ", NamedTextColor.YELLOW),
			newKit.getDisplayName()
		);
	}

	public static Title getActiveKitTitle(Kit newKit) {
		return Title.title(
			Component.text("Kit updated", NamedTextColor.GOLD),
			Component.textOfChildren(
				Component.text("You are now using ", NamedTextColor.YELLOW),
				newKit.getDisplayName().decorate(TextDecoration.UNDERLINED)
			),
			Title.DEFAULT_TIMES
		);
	}

	public static Component getSelectedKitMessage(Kit newKit) {
		return Component.textOfChildren(
			Component.text("The kit you have selected has been disabled by an admin.\nYou now have "),
			newKit.getDisplayName(),
			Component.text(" selected.")
		).color(NamedTextColor.YELLOW);
	}

	public static Component getKitFallbackDebugMessage(TeamArena game, Player player, Kit fallbackKit) {
		// insane
		return Component.textOfChildren(
			Component.text("Failed to find any eligible kits for " + player.getName() + "\n", NamedTextColor.DARK_RED),
			Component.text("Kit Filter Rules:\n", NamedTextColor.YELLOW),
			KitFilter.inspectRules(Main.getPlayerInfo(player).team.getSimpleName(), player.getName()),
			Component.text("\nAll kits:\n", NamedTextColor.GOLD),
			Component.text(game.getKits().stream().map(kit -> kit.getName().toLowerCase(Locale.ENGLISH)).collect(Collectors.joining(", ")), NamedTextColor.YELLOW),
			Component.text("\nFalling back to kit " + fallbackKit.getName(), NamedTextColor.DARK_RED)
		);
	}
}

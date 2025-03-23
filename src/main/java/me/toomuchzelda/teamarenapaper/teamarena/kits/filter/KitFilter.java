package me.toomuchzelda.teamarenapaper.teamarena.kits.filter;

import com.google.common.collect.Sets;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Filter kits based on their kit name.
 * By convention, kit filters are lowercase.
 *
 * FilterRules controlled by gamemodes must be added and removed at game start+end
 * FilterRules added by admin commands will remain until removed
 */
public class KitFilter {

	private static final Map<NamespacedKey, FilterRule> globalRules = new LinkedHashMap<>();
	/** @implNote teams are addressed by their simple name */
	private static final Map<String, Map<NamespacedKey, FilterRule>> teamRules = new HashMap<>();
	private static final Map<String, Map<NamespacedKey, FilterRule>> playerRules = new HashMap<>();

	public static final NamespacedKey ADMIN_KEY = new NamespacedKey(Main.getPlugin(), "admin");

	public static final FilterRule DEFAULT_ADMIN_RULE = new FilterRule(ADMIN_KEY, "Admin rule", FilterAction.block());

	static {
		addGlobalRule(DEFAULT_ADMIN_RULE);
	}

	public static void addGlobalRule(FilterRule rule) {
		globalRules.put(rule.key(), rule);
	}

	public static boolean removeGlobalRule(NamespacedKey key) {
		return globalRules.remove(key) != null;
	}

	public static void addTeamRule(String team, FilterRule rule) {
		teamRules.computeIfAbsent(team, key -> new HashMap<>()).put(rule.key(), rule);
	}

	public static boolean removeTeamRule(String team, NamespacedKey key) {
		boolean[] removed = {false};
		teamRules.computeIfPresent(team, (ignored, value) -> {
			removed[0] = value.remove(key) != null;
			return value.isEmpty() ? null : value;
		});
		return removed[0];
	}

	public static void addPlayerRule(String playerName, FilterRule rule) {
		playerRules.computeIfAbsent(playerName, key -> new HashMap<>()).put(rule.key(), rule);
	}

	public static boolean removePlayerRule(String playerName, NamespacedKey key) {
		boolean[] removed = {false};
		playerRules.computeIfPresent(playerName, (ignored, value) -> {
			removed[0] = value.remove(key) != null;
			return value.isEmpty() ? null : value;
		});
		return removed[0];
	}

	public static boolean removeRule(NamespacedKey key) {
		boolean b = false;
		b = b || removeGlobalRule(key);
		for (var teamEntry : Map.copyOf(teamRules).entrySet()) {
			b = b || removeTeamRule(teamEntry.getKey(), key);
		}
		for (var playerEntry : Map.copyOf(playerRules).entrySet()) {
			b = b || removePlayerRule(playerEntry.getKey(), key);
		}

		return b;
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

	public static Set<NamespacedKey> getAllRules() {
		// Not a perfectly sized allocation
		Set<NamespacedKey> all = Sets.newHashSetWithExpectedSize(globalRules.size() + teamRules.size() + playerRules.size());

		all.addAll(globalRules.keySet());
		for (var teamEntry : teamRules.entrySet()) all.addAll(teamEntry.getValue().keySet());
		for (var playerEntry : playerRules.entrySet()) all.addAll(playerEntry.getValue().keySet());

		return all;
	}

	public static Component listAllRules() {
		TextComponent.Builder builder = Component.text();

		for (var entry : globalRules.entrySet()) {
			builder.append(Component.text(entry.getKey().toString() + ": ", NamedTextColor.GREEN));
			builder.append(entry.getValue().toComponent());
			builder.append(Component.newline());
		}

		appendToList(builder, teamRules, "team", NamedTextColor.AQUA);
		appendToList(builder, playerRules, "player", NamedTextColor.DARK_AQUA);

		return builder.build();
	}
	private static void appendToList(TextComponent.Builder builder,
									 Map<String, Map<NamespacedKey, FilterRule>> map,
									 String type,
									 TextColor color) {
		for (var entry : map.entrySet()) {
			builder.append(Component.text("Rules for " + type + ": " + entry.getKey()));
			builder.append(Component.newline());
			for (var rule : entry.getValue().entrySet()) {
				builder.append(Component.text("  " + rule.getKey().toString() + ": ", color));
				builder.append(rule.getValue().toComponent());
				builder.append(Component.newline());
			}
		}
	}

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
		String name = kit.getKey();
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
			.map(Kit::getKey)
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
		//return Map.copyOf(allPlayerKits);
		return allPlayerKits;
	}

	public static Set<Kit> calculateKits(TeamArena game, Player player) {
		return calculateKits(game, List.of(player)).get(player);
	}

	public static boolean canUseKit(TeamArena game, Player player, Kit kit) {
		return calculateKits(game, player).contains(kit);
	}

	public static void setAdminAllowed(TeamArena game, Collection<String> allowed) throws IllegalArgumentException {
		Optional<Kit> fallbackOpt = game.getKits().stream()
			.filter(kit -> allowed.contains(kit.getKey()))
			.findFirst();

		if (fallbackOpt.isEmpty()) {
			addGlobalRule(DEFAULT_ADMIN_RULE);
			throw new IllegalArgumentException("Cannot block all kits");
		}

		addGlobalRule(new FilterRule(ADMIN_KEY, "Admin allowed kits", FilterAction.allow(new HashSet<>(allowed))));
		updateKitsFor(game, Bukkit.getOnlinePlayers());
	}

	public static void setAdminBlocked(TeamArena game, Collection<String> blocked) throws IllegalArgumentException {
		// For anyone not using an allowed kit, set it to a fallback one.
		Optional<Kit> fallbackOpt = game.getKits().stream()
			.filter(kit -> !blocked.contains(kit.getKey()))
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
				assert CompileAsserts.OMIT || !game.isDead(player);
				// also change active kit, very safe operation!
				info.activeKit.removeKit(player, info);
				// selected kit is guaranteed to be available
				game.giveKitAndGameItems(player, info, true);
			}

			Component message = null;
			if (!activeKitAvailable) {
				message = getActiveKitMessage(fallbackKit);
				player.showTitle(getActiveKitTitle(fallbackKit));
			} else if (!kitAvailable) {
				message = getSelectedKitMessage(info.kit.getName(), fallbackKit);
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

	public static Component getSelectedKitMessage(String preferredKit, Kit newKit) {
		return Component.textOfChildren(
			Component.text("Kit " + preferredKit + " has been disabled by an admin.\nYou now have "),
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
			Component.text(game.getKits().stream().map(Kit::getKey).collect(Collectors.joining(", ")), NamedTextColor.YELLOW),
			Component.text("\nFalling back to kit " + fallbackKit.getName(), NamedTextColor.DARK_RED)
		);
	}
}

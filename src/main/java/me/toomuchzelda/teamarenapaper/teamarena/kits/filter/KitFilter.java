package me.toomuchzelda.teamarenapaper.teamarena.kits.filter;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Filter kits based on their kit name.
 * By convention, kit filters are lowercase.
 */
public class KitFilter {
	public static final Set<String> DEFAULT_BLOCKED_KITS = Set.of("sniper", "longbow");
	private static FilterPreset preset;
	private static Set<String> blockedKits = DEFAULT_BLOCKED_KITS;

	public record FilterPreset(String name, String desc, ItemStack displayItem, boolean allow, Set<String> blockedKits) {
		public FilterPreset {
			displayItem = displayItem.clone();
			blockedKits = Set.copyOf(blockedKits);
			if (allow && blockedKits.isEmpty()) {
				throw new IllegalArgumentException("Cannot block all kits");
			}
		}

		public FilterPreset(String name, String desc, Material display, boolean allow, Set<String> blockedKits) {
			this(name, desc, ItemBuilder.of(display)
				.displayName(Component.text(name, NamedTextColor.BLUE))
				.lore(TextUtils.wrapString(desc, Style.style(NamedTextColor.GRAY)))
				.build(), allow, blockedKits);
		}

		public FilterPreset(String name, String desc, boolean allow, Set<String> blockedKits) {
			this(name, desc, Material.PAPER, allow, blockedKits);
		}
	}


	public static final Map<String, FilterPreset> PRESETS = Stream.of(
			new FilterPreset("Default", "The default Team Arena™ experience", false, DEFAULT_BLOCKED_KITS),
			// TODO define RWF default here
			new FilterPreset("Red Warfare Default", "Who used Kit Rewind on SnD??", true, Set.of("trooper")),
			new FilterPreset("Sniper Duel", "The best FPS player shall prevail", Material.SPYGLASS, true, Set.of("sniper")),
			new FilterPreset("Ghost Town", "Where'd everyone go?", Material.WHITE_STAINED_GLASS, true, Set.of("ghost")),
			new FilterPreset("Imposter Game", "There is an imposter... ඞ", true, Set.of("spy")),
			new FilterPreset("Close-Range Combat", "A battle between true warriors", Material.STONE_SWORD,
				false, Set.of("burst", "ghost", "engineer", "shortbow", "longbow", "pyro", "sniper")),
			new FilterPreset("Archer Duel", "Bow-spammers rise!", Material.BOW, true, Set.of("shortbow", "longbow", "pyro"))
		)
		.collect(Collectors.toUnmodifiableMap(
			p -> p.name.toLowerCase(Locale.ENGLISH).replaceAll("\\W", "_"), p -> p
		));


	public static Kit filterKit(TeamArena game, Kit kit) {
		if (!isAllowed(kit))
			return game.getKits().stream()
					.filter(KitFilter::isAllowed)
					.findFirst().orElse(null);
		else
			return kit;
	}

	public static boolean isAllowed(Kit kit) {
		return !blockedKits.contains(kit.getName().toLowerCase(Locale.ENGLISH));
	}

	@Nullable
	public static FilterPreset getPreset() {
		return preset;
	}

	public static void setPreset(@NotNull TeamArena game, FilterPreset preset) {
		if (preset.allow) {
			setAllowed(game, preset.blockedKits);
		} else {
			setBlocked(game, preset.blockedKits);
		}
		KitFilter.preset = preset;
	}

	public static void setAllowed(TeamArena game, Collection<String> allowed) throws IllegalArgumentException {
		var allowedKits = new HashSet<>(allowed);

		setBlocked(game, game.getKits().stream()
			.map(Kit::getName)
			.map(name -> name.toLowerCase(Locale.ENGLISH))
			.filter(name -> !allowedKits.contains(name))
			.toList()
		);
	}

	public static void setBlocked(TeamArena game, Collection<String> blocked) throws IllegalArgumentException {
		preset = null;
		blockedKits = Set.copyOf(blocked);
		// For anyone not using an allowed kit, set it to a fallback one.
		Optional<Kit> fallbackOpt = game.getKits().stream()
			.filter(KitFilter::isAllowed)
			.findFirst();

		if (fallbackOpt.isPresent()) {
			final Kit fallbackKit = fallbackOpt.get();
			Main.forEachPlayerInfo((player, info) -> {
				boolean kitAvailable = info.kit == null || KitFilter.isAllowed(info.kit);
				boolean activeKitAvailable = info.activeKit == null || KitFilter.isAllowed(info.activeKit);
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
		} else { // Tried to block all kits
			blockedKits = DEFAULT_BLOCKED_KITS;
			throw new IllegalArgumentException("Cannot block all kits");
		}
	}

	public static Set<String> getBlockedKits() {
		return blockedKits;
	}

	public static void allowKit(TeamArena game, String kitName) {
		var set = new HashSet<>(blockedKits);
		if (set.remove(kitName.toLowerCase(Locale.ENGLISH))) {
			setBlocked(game, set);
		}
	}

	public static void blockKit(TeamArena game, String kitName) {
		var set = new HashSet<>(blockedKits);
		if (set.add(kitName.toLowerCase(Locale.ENGLISH))) {
			setBlocked(game, set);
		}
	}

	public static void resetFilter() {
		blockedKits = DEFAULT_BLOCKED_KITS;
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
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.filter;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

/**
 * Moved from CommandDebug and adjusted by toomuchzelda, original author jacky8399
 */
public class KitFilter {

	private static final Set<String> DEFAULT_BLOCKED_KITS = Set.of("sniper");
	private static Set<String> blockedKits = DEFAULT_BLOCKED_KITS;

	// TODO add presets
	record FilterPreset(String name, String desc, Set<String> blockedKits) {}
	private static final List<FilterPreset> PRESETS = List.of();


	public static Kit filterKit(Kit kit) {
		if (!isAllowed(kit))
			return Main.getGame().getKits().stream()
					.filter(KitFilter::isAllowed)
					.findFirst().orElse(null);
		else
			return kit;
	}

	public static boolean isAllowed(Kit kit) {
		return !blockedKits.contains(kit.getName().toLowerCase(Locale.ENGLISH));
	}

	public static void setAllowed(Collection<String> allowed) throws IllegalArgumentException {
		var allowedKits = new HashSet<>(allowed);

		setBlocked(Main.getGame().getKits().stream()
			.map(Kit::getName)
			.map(name -> name.toLowerCase(Locale.ENGLISH))
			.filter(name -> !allowedKits.contains(name))
			.toList()
		);
	}

	public static void setBlocked(Collection<String> blocked) throws IllegalArgumentException {
		blockedKits = Set.copyOf(blocked);
		// For anyone not using an allowed kit, set it to a fallback one.
		Optional<Kit> fallbackOpt = Main.getGame().getKits().stream()
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

	public static void allowKit(String kitName) {
		var set = new HashSet<>(blockedKits);
		if (set.remove(kitName)) {
			setBlocked(set);
		}
	}

	public static void blockKit(String kitName) {
		var set = new HashSet<>(blockedKits);
		if (set.add(kitName)) {
			setBlocked(set);
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

	public static Component getSelectedKitMessage(Kit newKit) {
		return Component.textOfChildren(
			Component.text("The kit you have selected has been disabled by an admin.\nYou now have "),
			newKit.getDisplayName(),
			Component.text(" selected.")
		).color(NamedTextColor.YELLOW);
	}
}

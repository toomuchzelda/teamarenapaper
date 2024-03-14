package me.toomuchzelda.teamarenapaper.teamarena.kits.filter;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSniper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Moved from CommandDebug and adjusted by toomuchzelda, original author jacky8399
 */
public class KitFilter
{

	private static final Predicate<Kit> DEFAULT_KIT_PREDICATE = kit -> !(kit instanceof KitSniper);
	private static Predicate<Kit> kitPredicate = DEFAULT_KIT_PREDICATE;

	public static Kit filterKit(Kit kit) {
		if (!kitPredicate.test(kit))
			return Main.getGame().getKits().stream()
					.filter(kitPredicate)
					.findFirst().orElse(null);
		else
			return kit;
	}

	public static boolean isAllowed(Kit kit) {
		return kitPredicate.test(kit);
	}

	public enum SetAttempt {
		SUCCESS, CANT_BLOCK_ALL
	}
	public static SetAttempt setPredicate(Predicate<Kit> predicate) {
		kitPredicate = predicate;
		// For anyone not using an allowed kit, set it to a fallback one.
		Optional<Kit> fallbackOpt = Main.getGame().getKits().stream()
			.filter(KitFilter.kitPredicate)
			.findFirst();

		if (fallbackOpt.isPresent()) {
			final Kit fallbackKit = fallbackOpt.get();
			Main.forEachPlayerInfo((player, playerInfo) -> {
				if (playerInfo.kit != null && !KitFilter.kitPredicate.test(playerInfo.kit)) {
					playerInfo.kit = fallbackKit;
					player.sendMessage(Component.text("The kit you have selected has been disabled by an admin. " +
						"It has been replaced with: " + fallbackKit.getName(), NamedTextColor.YELLOW));
				}
				if (playerInfo.activeKit != null && !KitFilter.kitPredicate.test(playerInfo.activeKit)) {
					// also change active kit
					playerInfo.activeKit.removeKit(player, playerInfo);
					Main.getGame().givePlayerItems(player, playerInfo, true);
					player.sendMessage(Component.text("The kit you are using has been disabled by an admin. " +
						"It has been replaced with your selected kit.", NamedTextColor.YELLOW));
				}
			});
		}
		else { // Tried to block all kits
			kitPredicate = DEFAULT_KIT_PREDICATE;
			return SetAttempt.CANT_BLOCK_ALL;
		}

		return SetAttempt.SUCCESS;
	}

	public static void resetPredicate() {
		KitFilter.kitPredicate = KitFilter.DEFAULT_KIT_PREDICATE;
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CompassKillStreak extends KillStreak
{
	private static final ItemStack COMPASS;
	private static final TextColor COLOUR = TextColor.color(207, 255, 253);

	private static final Component NO_ENEMIES = Component.text("No enemies on the map.", NamedTextColor.RED);

	static {
		COMPASS = new ItemStack(Material.COMPASS);
		COMPASS.editMeta(itemMeta -> {
			itemMeta.displayName(ItemUtils.noItalics(Component.text("Tracker Compass", COLOUR)));
			itemMeta.lore(TextUtils.wrapString("Right click: Receive intel on the nearest enemy",
					Style.style(TextUtils.RIGHT_CLICK_TO).decoration(TextDecoration.ITALIC, false), TextUtils.DEFAULT_WIDTH));
		});
	}

	CompassKillStreak() {
		super("Tracker Compass", "Get some intel on nearby enemies.....", COLOUR, COMPASS, new CompassAbility());
	}

	static class CompassAbility extends Ability {
		private static final int COOLDOWN = 2 * 20;
		private static final Map<Player, Player> COMPASS_USES = new HashMap<>();

		@Override
		public void removeAbility(Player player) {
			COMPASS_USES.remove(player);
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(event.useItemInHand() == Event.Result.DENY)
				return;

			final ItemStack item = event.getItem();
			if(item != null && item.isSimilar(COMPASS)) { // Need to be holding compass
				PlayerInfo pinfo = Main.getPlayerInfo(event.getPlayer());
				if(pinfo.messageHasCooldowned("ksCmpas", COOLDOWN)) { // COOLDOWN time has passed
					// Find the nearest enemy and tell the user info
					final TeamArenaTeam playersTeam = pinfo.team;
					final Location usersLoc = event.getPlayer().getLocation();
					double distSqr = 99999999999d;
					Player closestEnemy = null;
					for(Player candidate : Main.getGame().getPlayers()) {
						if(playersTeam.getPlayerMembers().contains(candidate)) // Skip players on same team
							continue;

						double distToCand = candidate.getLocation().distanceSquared(usersLoc);
						if(distToCand < distSqr) {
							distSqr = distToCand;
							closestEnemy = candidate;
						}
					}

					// Notify the chosen candidate they have been tracked, but only on the first "tracking event"
					notifyPlayer(event.getPlayer(), closestEnemy, Math.sqrt(distSqr));
				}
			}
		}

		private void notifyPlayer(final Player user, @Nullable final Player target, double distance) {
			if(target == null) {
				user.sendMessage(NO_ENEMIES);
				return;
			}

			final Player lastTarget = COMPASS_USES.put(user, target);
			if(lastTarget != target) {
				target.sendMessage(
						Component.text()
						.append(user.playerListName())
						.append(Component.text(" has tracked you with their tracker compass", NamedTextColor.RED))
						.build()
				);

				target.playSound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 4f, 0f);
			}

			final boolean targetInvis = Kit.getActiveKit(target).isInvisKit();
			if(targetInvis)
				distance = distance + MathUtils.randomRange(-5d, 5d);

			final int intDist = (int) (distance + 0.5d); // Round to the nearest whole.

			user.sendMessage(Component.text()
					.append(target.playerListName())
					.append(Component.text(" is " + intDist + " blocks away" + (targetInvis ? "???" : "."),
							NamedTextColor.RED))
					.build()
			);
		}
	}
}

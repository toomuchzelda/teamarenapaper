package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import com.google.common.collect.EvictingQueue;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.hideandseek.HideAndSeek;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.*;

public class KitRadarSeeker extends Kit {
	private static final ItemStack RADAR = ItemBuilder.of(Material.WARPED_FUNGUS_ON_A_STICK)
		.displayName(Component.text("\"Radar\"", NamedTextColor.GREEN))
		.lore(TextUtils.wrapString("Note: Questioning your equipment is in violation of your contract " +
			"with Hiders Enforcement LLC and may result in a termination.", Style.style(TextColors.ERROR_RED)))
		.build();
	public KitRadarSeeker(TeamArena game) {
		super("Radar", "Seek out the sneakier Hiders with your Radar", Material.RECOVERY_COMPASS);

		this.setItems(new ItemStack(Material.STONE_SWORD), RADAR);
		//this.setArmor(new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE),
		//	new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_BOOTS));
		ItemStack hat = new ItemStack(Material.LEATHER_HELMET);
		ItemUtils.colourLeatherArmor(Color.PURPLE, hat);
		this.setArmor(hat, null, null, null);
		this.setAbilities(new RadarSeekerAbility(game));
	}

	private static class RadarSeekerAbility extends Ability {

		public static final TextComponent ACCURACY = Component.text("Signal Strength: ", TextColor.color(0x138c97));
		public static final TextComponent ACCURACY_HINT = Component.text(" (Stand still for better signal)", NamedTextColor.GRAY);
		private static final TextComponent KING_NEAR_HINT = Component.textOfChildren(
			Component.text(" (", NamedTextColor.GOLD),
			Component.text("King", NamedTextColor.GREEN),
			Component.text(" nearby)", NamedTextColor.GOLD)
		).decorate(TextDecoration.BOLD);

		public static final int RADAR_PERIOD = 40;
		public static final Location[] RADAR_LOCATIONS = new Location[RADAR_PERIOD];
		static {
			for (int i = 0; i < RADAR_PERIOD; i++) {
				double theta = i * 2 * Math.PI / RADAR_PERIOD;
				RADAR_LOCATIONS[i] = new Location(null, Math.sin(theta) * 10000, 0, Math.cos(theta) * 10000);
			}
		}

		private final TeamArena game;

		public RadarSeekerAbility(TeamArena game) { this.game = game; }

		@Override
		protected void giveAbility(Player player) {
			player.sendMessage(Component.textOfChildren(
				Component.text("Use your "),
				Component.text("Radar", NamedTextColor.GREEN),
				Component.text(" to locate hiders and eliminate them."),
				Component.newline(),
				Component.text("Pay attention to the "),
				Component.text("pitch", NamedTextColor.YELLOW),
				Component.text(" of the sounds your radar emits.")
			).color(TextColor.color(0x138c97)));
			player.sendMessage(Component.text("""
				Note: Prolonged use of radar may result in negative health effects, \
				Such as hair loss, eye damage, hallucinations, and seeing moving blocks. \
				Hiders Enforcement LLC is not responsible for any bodily harm.""", TextColor.color(0x0d0d0d)));
			playerMovement.put(player, EvictingQueue.create(RADAR_PERIOD));
		}

		@Override
		protected void removeAbility(Player player) {
			playerMovement.remove(player);
		}

		final Map<Player, EvictingQueue<Vector>> playerMovement = new HashMap<>();

		private static final Random RANDOM = new Random();

		@Override
		public void onPlayerTick(Player player) {
			if (!(this.game instanceof HideAndSeek hideAndSeek))
				return;
			// update player location
			EvictingQueue<Vector> locations = playerMovement.get(player);
			Location playerLocation = player.getLocation();
			locations.add(playerLocation.toVector());

			// compass targets don't work if they're outside the client world border
			PlayerInventory inventory = player.getInventory();

			if (TeamArena.getGameTick() % 10 != 0)
				return;
			// 50% accuracy penalty for dual-wielding
			boolean radarInUse = false;
			boolean radarPenalty = false;
			if (inventory.getItemInMainHand().getType() == RADAR.getType()) {
				radarInUse = true;
				radarPenalty = inventory.getItemInOffHand().getType() != Material.AIR;
			} else if (inventory.getItemInOffHand().getType() == RADAR.getType()) {
				radarInUse = true;
				radarPenalty = true;
			}
			if (!radarInUse)
				return;

			double totalMovement = 0;
			var iterator = locations.iterator();
			Vector lastLocation = iterator.next();
			while (iterator.hasNext()) {
				Vector location = iterator.next();
				totalMovement += location.distance(lastLocation);
				lastLocation = location;
			}
			double inaccuracy = Math.min(5, totalMovement / 3);
			int accuracyBlocks = (int) ((1 - inaccuracy / 5d) * 10 * (radarPenalty ? 0.5 : 1));
			Component actionBar = Component.textOfChildren(
				ACCURACY,
				Component.text("|".repeat(accuracyBlocks), NamedTextColor.GREEN),
				Component.text("|".repeat(10 - accuracyBlocks), NamedTextColor.DARK_GRAY)
			);

			boolean appendAccuracy = true;
			// play sound every 2 seconds
			if (TeamArena.getGameTick() % RADAR_PERIOD == 0) {
				ArrayList<Player> candidates = new ArrayList<>(hideAndSeek.hiderTeam.getPlayerMembers());
				candidates.removeIf(this.game::isDead);
				Player closest = getClosestPlayer(candidates, playerLocation);
				if (closest == null)
					return;
				Location inaccurateLocation = addInaccuracy(closest.getLocation(), inaccuracy);

				double distance = inaccurateLocation.distance(playerLocation);
				float pitch;
				if (distance < 3) {
					pitch = 2;
				} else if (distance > 15) {
					pitch = 0;
				} else {
					pitch = (float) (2 * (1 - (distance - 3) / (15 - 3)));
				}

				player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.PLAYERS, 1f, pitch);
				if (distance <= 6 && closest == hideAndSeek.getPresident()) {
					player.playSound(player, Sound.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.PLAYERS, 1f, 1f);
					appendAccuracy = false;
				}
			}

			if (appendAccuracy)
				actionBar = actionBar.append(ACCURACY_HINT);
			else
				actionBar = actionBar.append(KING_NEAR_HINT);

			player.sendActionBar(actionBar);
		}

		private static Player getClosestPlayer(Collection<? extends Player> players, Location location) {
			if (players.isEmpty())
				return null;
			Location temp = location.clone();
			var iter = players.iterator();
			Player candidate = iter.next();
			double candidateDistance = candidate.getLocation(temp).distance(location);
			while (iter.hasNext()) {
				Player other = iter.next();
				double distance = other.getLocation(temp).distance(location);
				if (distance < candidateDistance) {
					candidate = other;
					candidateDistance = distance;
				}
			}
			return candidate;
		}

		private static Location addInaccuracy(Location location, double inaccuracy) {
			double theta = RANDOM.nextDouble();
			double length = Math.sqrt(RANDOM.nextDouble()) * inaccuracy;
			return location.add(Math.sin(theta) * length, 0, Math.cos(theta) * length);
		}
	}
}

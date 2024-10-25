package me.toomuchzelda.teamarenapaper.teamarena.kits.rewind;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

//Kit Description:
/*
	Main Ability: Rewind
        CD: 15 seconds
            Every 15 seconds, this kit can travel to its previous location 15 seconds ago, with an extra buff depending on
            a 15 second cycle with 3 equivalent parts (each last for 5 seconds):
                First Section: Regeneration
                Second Section: Time Dilation (AoE Slow)
                Third Section: Knockback (KB Explosion)
	Sub Ability: Stasis
		CD: 12 sec
        Active Duration: 14 ticks
            Provides Rewind with temporary invulnerability, but it is unable to attack during this time
*/

/**
 * @author onett425
 */

public class KitRewind extends Kit {
	public static final int STASIS_DURATION = 14;
	public static final TextColor REGEN_COLOR = TextColor.color(245, 204, 91);
	public static final TextColor DILATE_COLOR = TextColor.color(237, 132, 83);
	public static final TextColor KB_COLOR = TextColor.color(123, 101, 235);

	public static final ItemStack TIME_MACHINE;

	static {
		if (KitOptions.rewindClockPhases) {
			TIME_MACHINE = ItemBuilder.of(Material.CLOCK)
				.displayName(Component.text("Time Machine"))
				.lore(
					/*
					Teleport to your location 15 seconds ago!
					You get a buff based on your current State
					The State changes every 5 seconds
					<regeneration>: Regen II for 3.5 Seconds
					<time_dilation>: Slowness III + Prevents
						Jumping from enemies within 4 Blocks
						for 3 Seconds
					<knockback>: Blasts nearby enemies away
					Note: <time_dilation> and <knockback>
						are applied at departure and arrival
						location
					 */
					Component.text("Teleport to your location 15 seconds ago!", TextColors.LIGHT_YELLOW),
					Component.text("You get a buff based on your current State", TextColors.LIGHT_YELLOW),
					Component.text("The State changes every 5 seconds", TextColors.LIGHT_YELLOW),
					Component.textOfChildren(
						Component.text("Regeneration", REGEN_COLOR),
						Component.text(": Regen II for 3.5 Seconds", TextColors.LIGHT_YELLOW)
					),
					Component.textOfChildren(
						Component.text("Time Dilation", DILATE_COLOR),
						Component.text(": Slowness III + Prevents", TextColors.LIGHT_YELLOW)
					),
					Component.text("	Jumping from enemies within 4 Blocks", TextColors.LIGHT_YELLOW),
					Component.text("	for 3 Seconds", TextColors.LIGHT_YELLOW),
					Component.textOfChildren(
						Component.text("Knockback", KB_COLOR),
						Component.text(": Blasts nearby enemies away", TextColors.LIGHT_YELLOW)
					),
					Component.textOfChildren(
						Component.text("Note: "),
						Component.text("Time Dilation", DILATE_COLOR),
						Component.text(" and "),
						Component.text("Knockback", KB_COLOR)
					).color(TextColors.LIGHT_YELLOW),
					Component.text("	are applied at departure and arrival", TextColors.LIGHT_YELLOW),
					Component.text("	location", TextColors.LIGHT_YELLOW)
				)
				.build();
		}
		else {
			TIME_MACHINE = ItemBuilder.of(Material.CLOCK)
				.displayName(Component.text("Time Machine", NamedTextColor.YELLOW))
				.lore(
					TextUtils.wrapString("Right Click: Teleport to your location 15 seconds ago.", Style.style(TextUtils.RIGHT_CLICK_TO))
				)
				.build();
		}
	}

	public static final ItemStack TIME_STASIS = ItemBuilder.of(Material.SHULKER_SHELL)
			.displayName(Component.text("Time Stasis"))
			.lore(Component.text("Duration: 0.7 seconds", KB_COLOR),
					Component.text("Briefly become invulnerable but", TextColors.LIGHT_YELLOW),
					Component.text("unable to attack", TextColors.LIGHT_YELLOW))
			.build();

	public KitRewind() {
		super("Rewind", "A hit and runner, or for those who want to cheat death. " +
				"A strong melee fighter who can travel 15 seconds back in time with its rewind clock.\n" +
				(KitOptions.rewindClockPhases ?
				"Depending on the time, you gain a different buff.\n" : "") +
				(KitOptions.rewindStasis ?
				"Time stasis provides brief invulnerability at the cost of being disarmed." : "")
				, Material.CLOCK);

		setArmor(new ItemStack(Material.CHAINMAIL_HELMET),
				ItemBuilder.of(Material.IRON_CHESTPLATE)
						.enchant(Enchantment.PROTECTION, 1)
						.build(),
				new ItemStack(Material.IRON_LEGGINGS),
				new ItemStack(Material.IRON_BOOTS));

		if (KitOptions.rewindStasis)
			setItems(new ItemStack(Material.IRON_SWORD), TIME_MACHINE, TIME_STASIS);
		else
			setItems(new ItemStack(Material.IRON_SWORD), TIME_MACHINE);

		setAbilities(new RewindAbility());

		setCategory(KitCategory.FIGHTER);
	}

	public static class RewindAbility extends Ability {

		public static final int TICK_CYCLE = 15 * 20;

		record RewindInfo(int startingTick, Queue<Location> rewindLocations, RewindMarker marker) {}
		private final WeakHashMap<Player, RewindInfo> rewindInfo = new WeakHashMap<>();

		record StasisInfo(int startingTick, ItemStack[] armor, HashMap<Integer, ? extends ItemStack> swords) {}
		private final WeakHashMap<Player, StasisInfo> stasisInfo = new WeakHashMap<>();

		//clean up
		@Override
		public void unregisterAbility() {
			rewindInfo.clear();
			stasisInfo.clear();
		}

		@Override
		public void giveAbility(Player player) {
			player.setCooldown(Material.CLOCK, TICK_CYCLE);
			// can only rewind to TICK_CYCLE ticks away, so only TICK_CYCLE locations will be stored
			rewindInfo.put(player, new RewindInfo(TeamArena.getGameTick(), new ArrayDeque<>(), new RewindMarker(player)));
		}

		@Override
		public void removeAbility(Player player) {
			//Fixes the display of the clock in kit selection menu
			player.setCooldown(Material.CLOCK, 0);
			rewindInfo.remove(player).marker().remove();
		}

		@Override
		public void onTick() {
			final int currentTick = TeamArena.getGameTick();

			// stasis
			statisTick(currentTick);
		}

		@Override
		public void onPlayerTick(Player player) {
			Location playerLoc = player.getLocation();
			RewindInfo info = rewindInfo.get(player);
			Block currBlock = playerLoc.getBlock().getRelative(BlockFace.DOWN);

			//Checking that the current location is a valid rewind location, if it is, add it to possible rewind locations.
			final Queue<Location> locations = info.rewindLocations();
			RewindMarker marker = info.marker();
			//if (player.isFlying() || player.isGliding() || (!currBlock.isEmpty() && currBlock.getType() != Material.LAVA)) {
			locations.add(playerLoc);
			if (locations.size() >= TICK_CYCLE) {
				locations.remove();
				marker.setRed(false);
			}
			//}

			if (!locations.isEmpty()) {
				marker.updatePos(locations.peek().clone().add(0d, (player.getHeight() / 2d), 0d));
			}

			if (KitOptions.rewindClockPhases)
				clockStateTick(player, info);

		}

		//Cancels damage that is received while in stasis
		@Override
		public void onAttemptedDamage(DamageEvent event) {
			if (!KitOptions.rewindStasis) return;
			Player player = event.getPlayerVictim();
			if (player.getCooldown(Material.SHULKER_SHELL) >= (12 * 20 - STASIS_DURATION)) {
				event.setCancelled(true);
			}
		}

		//Cancels attacks that are attempted while in stasis
		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if (!KitOptions.rewindStasis) return;
			Player player = (Player) event.getFinalAttacker();
			// Check to make sure the immediate attacker is the final attacker
			// this isn't always the case e.g a rewind's wolf attacks someone.
			if (player == event.getAttacker()) {
				if (player.getCooldown(Material.SHULKER_SHELL) >= (12 * 20 - STASIS_DURATION)) {
					event.setCancelled(true);
				}
			}
		}

		private static final ItemStack DISABLED_ITEM = ItemBuilder.of(Material.BARRIER)
				.displayName(Component.text("Item Disabled", TextColors.ERROR_RED))
				.build();
		@Override
		public void onInteract(PlayerInteractEvent event) {
			Material mat = event.getMaterial();
			Player player = event.getPlayer();
			PlayerInventory inventory = player.getInventory();
			World world = player.getWorld();

			//Rewind Clock implementation
			if (mat == Material.CLOCK && player.getCooldown(Material.CLOCK) == 0) {
				//No Rewinding w/ Flag
				if (Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player)) {
					Component cannotUseAbilityMsg = Component.text("You can't use Time Machine while holding the flag!", TextColor.color(255, 98, 20));
					player.sendMessage(cannotUseAbilityMsg);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 2, 0.5f);
				} else {
					int currTick = TeamArena.getGameTick();
					RewindInfo info = rewindInfo.get(player);

					Location dest = info.rewindLocations().poll();
					if (dest != null) {
						//Past Location succesfully found
						//Apply buff at departure AND arrival location
						if (KitOptions.rewindClockPhases)
							rewindBuff(player, info, currTick);

						player.setFallDistance(0f);
						world.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
						player.teleport(dest);
						world.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);

						player.setFireTicks(0);
						if (KitOptions.rewindClockPhases) {
							rewindBuff(player, info, currTick);
						}
						else {
							player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1));
						}
						player.setCooldown(Material.CLOCK, 15 * 20);
						info.rewindLocations().clear();
						info.marker().setRed(true);
					} else {
						//Failure
						Component warning = Component.text("The past seems uncertain...", NamedTextColor.LIGHT_PURPLE);
						player.sendMessage(warning);
						player.setCooldown(Material.CLOCK, 10);
					}
				}
			}

			//Time Stasis implementation
			if (KitOptions.rewindStasis && mat == Material.SHULKER_SHELL && player.getCooldown(Material.SHULKER_SHELL) == 0) {
				//No Time Stasis w/ Flag
				if (Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player)) {
					Component cannotUseAbilityMsg = Component.text("You can't use Time Stasis while holding the flag!").color(TextColor.color(255, 98, 20));
					player.sendMessage(cannotUseAbilityMsg);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 2, 0.5f);
				} else {
					//"Glitching" aesthetic effect + particles
					ItemStack[] armor = player.getInventory().getArmorContents();
					var swordSlots = player.getInventory().all(Material.IRON_SWORD);

					//Replacing all swords with barriers
					for (int slot : swordSlots.keySet()) {
						inventory.setItem(slot, DISABLED_ITEM);
					}

					player.setInvisible(true);
					inventory.setArmorContents(null);
					player.stopSound(Sound.ITEM_ARMOR_EQUIP_IRON);
					player.stopSound(Sound.ITEM_ARMOR_EQUIP_CHAIN);
					world.playSound(player, Sound.BLOCK_BELL_RESONATE, 1f, 1.8f);

					stasisInfo.put(player, new StasisInfo(TeamArena.getGameTick(), armor, swordSlots));
					player.setCooldown(Material.SHULKER_SHELL, 12 * 20);
				}
			}

			//Prevents players from placing barriers
			if (event.useItemInHand() != Event.Result.DENY && DISABLED_ITEM.isSimilar(event.getItem())) {
				event.setUseItemInHand(Event.Result.DENY);
			}
		}

		private void statisTick(int currentTick) {
			for (var iter = stasisInfo.entrySet().iterator(); iter.hasNext();) {
				var entry = iter.next();
				var player = entry.getKey();
				var inventory = player.getInventory();
				var stasisInfo = entry.getValue();

				int tickElapsed = currentTick - stasisInfo.startingTick();

				if (tickElapsed >= STASIS_DURATION) {
					player.setInvisible(false);
					inventory.setArmorContents(stasisInfo.armor());
					player.stopSound(Sound.ITEM_ARMOR_EQUIP_IRON);
					player.stopSound(Sound.ITEM_ARMOR_EQUIP_CHAIN);
					//Replacing the barriers with swords again
					for (var sword : stasisInfo.swords().entrySet()) {
						inventory.setItem(sword.getKey(), sword.getValue());
					}
					iter.remove();
				} else {
					//Add particle effect which will show where the stasis player is
					// what???
					player.getWorld().spawnParticle(Particle.DUST, player.getLocation(), 8,
						1,
						1,
						1,
						16, new Particle.DustOptions(Main.getPlayerInfo(player).team.getColour(), 2));
					if (tickElapsed % 2 == 0) {
						player.setInvisible(true);
						inventory.setArmorContents(null);
					} else {
						player.setInvisible(false);
						inventory.setArmorContents(stasisInfo.armor());
					}
					player.stopSound(Sound.ITEM_ARMOR_EQUIP_IRON);
					player.stopSound(Sound.ITEM_ARMOR_EQUIP_CHAIN);
				}
			}
		}

		private void clockStateTick(Player player, RewindInfo info) {
			int currTick = TeamArena.getGameTick();
			int elapsedTick = (currTick - info.startingTick()) % TICK_CYCLE;
			PlayerInfo pinfo = Main.getPlayerInfo(player);
			PlayerInventory inv = player.getInventory();

			//Sound to signify a time-cycle change
			if (elapsedTick % (TICK_CYCLE / 3) == 99) {
				player.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.MASTER, 1.0f, 1.0f);
			}

			//Displaying the current cycle in the action bar
			if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
				Component currState;
				if (elapsedTick >= 0 && elapsedTick < 5 * 20) {
					currState = Component.text("Current State: Regeneration", REGEN_COLOR);
				} else if (elapsedTick < 10 * 20) {
					currState = Component.text("Current State: Time Dilation", DILATE_COLOR);
				} else {
					currState = Component.text("Current State: Knockback", KB_COLOR);
				}
				player.sendActionBar(currState);

				// Update all click items if the preference is changed mid-game
				var timeMachineDisplayName = TIME_MACHINE.displayName();
				for (var iterator = inv.iterator(); iterator.hasNext(); ) {
					var stack = iterator.next();
					if (stack == null || stack.getType() != Material.CLOCK)
						continue;
					var meta = stack.getItemMeta();
					var displayName = meta.displayName();
					if (!timeMachineDisplayName.equals(displayName)) {
						iterator.set(TIME_MACHINE);
					}
				}
			}
			//Those who disable action bar can see state based on the name of clock in hand
			else if (player.getInventory().contains(Material.CLOCK)) {
				Component currState;
				if (elapsedTick >= 0 && elapsedTick < 5 * 20) {
					currState = ItemUtils.noItalics(Component.text("Regeneration", REGEN_COLOR));
				} else if (elapsedTick < 10 * 20) {
					currState = ItemUtils.noItalics(Component.text("Time Dilation", DILATE_COLOR));
				} else {
					currState = ItemUtils.noItalics(Component.text("Knockback", KB_COLOR));
				}

				HashMap<Integer, ? extends ItemStack> clocks = player.getInventory().all(Material.CLOCK);
				clocks.forEach((k, v) -> {
					ItemMeta clockMeta = v.getItemMeta();
					clockMeta.displayName(currState);
					v.setItemMeta(clockMeta);
				});
			}
		}

		//When rewinding, a buff is given based on a 15 second cycle with 3 sections, each with a 5 second timeframe
		private void rewindBuff(Player player, RewindInfo info, int currTick) {
			final TeamArena game = Main.getGame();
			//Returns how far the currTick is in the cycle
			//[0, 299]
			int elapsedTick = (currTick - info.startingTick()) % TICK_CYCLE;
			if (elapsedTick >= 0 && elapsedTick < 5 * 20) {
				//Regen 2 for 7.5 seconds => 3 hearts healed
				player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 150, 1));
			} else if (elapsedTick < 10 * 20) {
				//Time Dilation: Gives nearby enemies Slow 3 + No Jump for 3 seconds
				List<Entity> affectedEnemies = player.getNearbyEntities(4, 4, 4);
				for (Entity entity : affectedEnemies) {
					if (entity instanceof LivingEntity victim) {
						if (victim instanceof ArmorStand stand && stand.isMarker()) continue;
						if (victim instanceof Player pVictim && !game.canAttack(player, pVictim)) continue;
						if (!game.isDead(victim)) {
							//change to 3*20 tick duration, extended for testing purposes
							victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 3 * 20, 250, true));
							victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 2, true));
						}
					}
				}
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.AMBIENT, 0.5f, 1f);
			} else {
				//Knockback: KB in an AoE
				player.getWorld().createExplosion(player, 0f, false, false);

				//KB Amp
				List<Entity> affectedEnemies = player.getNearbyEntities(3, 3, 3);
				for (Entity entity : affectedEnemies) {
					if (game.isDead(entity)) continue;
					if (entity instanceof LivingEntity victim && !(entity instanceof ArmorStand)) {
						if(!(victim instanceof Player p) || game.canAttack(player, p)) {
							Vector currVel = victim.getVelocity().clone();
							Vector playerLoc = player.getLocation().clone().toVector();
							Vector victimLoc = victim.getLocation().clone().toVector();
							Vector diff = victimLoc.subtract(playerLoc);
							diff = restrictExtremes(diff);

							Vector amp = new Vector(1,1,1).divide(diff);
							victim.setVelocity(currVel.add(amp.multiply(new Vector(0.75, 0.5, 0.75))));
						}
					}
				}

				player.stopSound(Sound.ENTITY_GENERIC_EXPLODE);
				player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.4f, 1.3f);
			}
		}

		public Vector restrictExtremes(Vector vec){
			double x = vec.getX();
			double y = vec.getY();
			double z = vec.getZ();
			Vector newVec = vec.clone();
			if(x <= 1 && x >= 0){
				newVec.setX(1);
			}
			else if(x >= -1 && x <= 0){
				newVec.setX(-1);
			}
			if(y <= 1 && y >= 0){
				newVec.setY(1);
			}
			else if(y >= -1 && y <= 0){
				newVec.setY(-1);
			}
			if(z <= 1 && z >= 0){
				newVec.setZ(1);
			}
			else if(z >= -1 && z <= 0){
				newVec.setZ(-1);
			}
			return newVec;
		}
	}
}

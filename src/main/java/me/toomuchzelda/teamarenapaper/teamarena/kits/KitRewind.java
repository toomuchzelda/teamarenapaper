package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
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
            a 15 second cycle with 3 equivalent parts (each last for 5 seconds).
            They are denoted by the current time of day:
                Day: Regeneration
                Sunset: Time Dilation (AoE Slow)
                Night: Knockback (KB Explosion)
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

	static{
		TIME_MACHINE = ItemBuilder.of(Material.CLOCK)
				.displayName(ItemUtils.noItalics(Component.text("Time Machine")))
				.build();
	}

	public KitRewind() {
		super("Rewind", """
				Travel 15 seconds back in time with your rewind clock. \
				Depending on the time, you gain a different buff. \
				Time stasis allows you to not take damage but you cannot deal damage.\
				""", Material.CLOCK);
		setArmor(new ItemStack(Material.CHAINMAIL_HELMET),
				ItemBuilder.of(Material.IRON_CHESTPLATE)
						.enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
						.build(),
				new ItemStack(Material.IRON_LEGGINGS),
				new ItemStack(Material.IRON_BOOTS));

		ItemStack timeStasis = ItemBuilder.of(Material.SHULKER_SHELL)
				.displayName(ItemUtils.noItalics(Component.text("Time Stasis")))
				.build();
		setItems(new ItemStack(Material.IRON_SWORD), TIME_MACHINE, timeStasis);

		setAbilities(new RewindAbility());
	}

	public static class RewindAbility extends Ability {

		public static final int TICK_CYCLE = 15 * 20;

		record RewindInfo(int startingTick, Queue<Vector> rewindLocations) {}
		public final WeakHashMap<Player, RewindInfo> rewindInfo = new WeakHashMap<>();

		record StasisInfo(int startingTick, ItemStack[] armor, HashMap<Integer, ? extends ItemStack> swords) {}
		public final WeakHashMap<Player, StasisInfo> stasisInfo = new WeakHashMap<>();

		//clean up
		@Override
		public void unregisterAbility() {
			rewindInfo.clear();
		}

		@Override
		public void giveAbility(Player player) {
			player.setCooldown(Material.CLOCK, TICK_CYCLE);
			// can only rewind to TICK_CYCLE ticks away, so only TICK_CYCLE locations will be stored
			rewindInfo.put(player, new RewindInfo(TeamArena.getGameTick(), new LinkedList<>()));
		}

		@Override
		public void removeAbility(Player player) {
			//Fixes the display of the clock in kit selection menu
			player.setCooldown(Material.CLOCK, 0);
			rewindInfo.remove(player);
		}

		@Override
		public void onTick() {
			int currentTick = TeamArena.getGameTick();

			// stasis
			for (var iter = stasisInfo.entrySet().iterator(); iter.hasNext();) {
				var entry = iter.next();
				var player = entry.getKey();
				var inventory = player.getInventory();
				var stasisInfo = entry.getValue();

				int tickElapsed = currentTick - stasisInfo.startingTick();

				if (tickElapsed >= STASIS_DURATION) {
					player.setInvisible(false);
					inventory.setArmorContents(stasisInfo.armor());
					player.getWorld().stopSound(SoundStop.named(Sound.ITEM_ARMOR_EQUIP_IRON));
					player.getWorld().stopSound(SoundStop.named(Sound.ITEM_ARMOR_EQUIP_CHAIN));
					//Replacing the barriers with swords again
					for (var sword : stasisInfo.swords().entrySet()) {
						inventory.setItem(sword.getKey(), sword.getValue());
					}
					iter.remove();
				} else {
					//Add particle effect which will show where the stasis player is
					// what???
					player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 8,
							1,
							1,
							1,
							16, new Particle.DustOptions(Main.getPlayerInfo(player).team.getColour(), 2));
					if (tickElapsed % 2 == 0) {
						player.setInvisible(true);
						inventory.setArmorContents(null);
						player.getWorld().stopSound(SoundStop.named(Sound.ITEM_ARMOR_EQUIP_IRON));
						player.getWorld().stopSound(SoundStop.named(Sound.ITEM_ARMOR_EQUIP_CHAIN));
					} else {
						player.setInvisible(false);
						inventory.setArmorContents(stasisInfo.armor());
						player.getWorld().stopSound(SoundStop.named(Sound.ITEM_ARMOR_EQUIP_IRON));
						player.getWorld().stopSound(SoundStop.named(Sound.ITEM_ARMOR_EQUIP_CHAIN));
					}
				}
			}


		}

		@Override
		public void onPlayerTick(Player player) {
			//Player tick is used to determine cooldowns + abilities
			//Time tick is purely aesthetic
			Location loc = player.getLocation();
			RewindInfo info = rewindInfo.get(player);
			Block currBlock = loc.getBlock().getRelative(BlockFace.DOWN);
			int currTick = TeamArena.getGameTick();
			int elapsedTick = (currTick - info.startingTick()) % TICK_CYCLE;
			PlayerInfo pinfo = Main.getPlayerInfo(player);
			PlayerInventory inv = player.getInventory();

			//Checking that the current location is a valid rewind location, if it is, add it to possible rewind locations.
			if (player.isFlying() || player.isGliding() || (!currBlock.isEmpty() && currBlock.getType() != Material.LAVA)) {
				info.rewindLocations().add(player.getLocation().toVector());
				if(info.rewindLocations().size() >= TICK_CYCLE)
					info.rewindLocations().remove();
			}

			//Sound to signify a time-cycle change
			if(elapsedTick % (TICK_CYCLE/3) == 99){
				player.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.MASTER, 1.0f, 1.0f);
			}

			//Displaying the current cycle in the action bar
			if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
				Component currState;
				if (elapsedTick >= 0 && elapsedTick < 5 * 20) {
					currState = Component.text("Current State: Regeneration").color(REGEN_COLOR);
				} else if (elapsedTick < 10 * 20) {
					currState = Component.text("Current State: Time Dilation").color(DILATE_COLOR);
				} else {
					currState = Component.text("Current State: Knockback").color(KB_COLOR);
				}
				player.sendActionBar(currState);

				//Resetting clock name if the preference is changed mid-game
				ItemStack clockCheck = inv.getItem((inv.first(Material.CLOCK)));
				if(!clockCheck.displayName().contains(TIME_MACHINE.displayName().asComponent())){
					HashMap<Integer, ? extends ItemStack> clocks = player.getInventory().all(Material.CLOCK);
					clocks.forEach(
							(k, v)
									-> {inv.setItem(k, TIME_MACHINE);
							}
					);
				}
			}
			//Those who disable action bar can see state based on the name of clock in hand
			else{
				if(player.getInventory().contains(Material.CLOCK)){
					Component currState;
					if (elapsedTick >= 0 && elapsedTick < 5 * 20) {
						currState = ItemUtils.noItalics(Component.text("Regeneration").color(REGEN_COLOR));
					} else if (elapsedTick < 10 * 20) {
						currState = ItemUtils.noItalics(Component.text("Time Dilation").color(DILATE_COLOR));
					} else {
						currState = ItemUtils.noItalics(Component.text("Knockback").color(KB_COLOR));
					}

					HashMap<Integer, ? extends ItemStack> clocks = player.getInventory().all(Material.CLOCK);
					clocks.forEach(
							(k, v)
									-> {ItemMeta clockMeta = v.getItemMeta();
								clockMeta.displayName(currState);
								v.setItemMeta(clockMeta);
							}
					);
				}
			}

		}

		//Cancels damage that is received while in stasis
		@Override
		public void onAttemptedDamage(DamageEvent event) {
			Player player = event.getPlayerVictim();
			if (player.getCooldown(Material.SHULKER_SHELL) >= (12 * 20 - STASIS_DURATION)) {
				event.setCancelled(true);
			}
		}

		//Cancels attacks that are attempted while in stasis
		@Override
		public void onAttemptedAttack(DamageEvent event) {
			Player player = (Player) event.getAttacker();
			if (player.getCooldown(Material.SHULKER_SHELL) >= (12 * 20 - STASIS_DURATION)) {
				event.setCancelled(true);
			}
		}

		private static final ItemStack DISABLED_ITEM = ItemBuilder.of(Material.BARRIER)
				.displayName(Component.text("Item Disabled", TextUtils.ERROR_RED))
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
					Component cannotUseAbilityMsg = Component.text("You can't use Time Machine while holding the flag!").color(TextColor.color(255, 98, 20));
					player.sendMessage(cannotUseAbilityMsg);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 2, 0.5f);
				} else {
					int currTick = TeamArena.getGameTick();
					RewindInfo info = rewindInfo.get(player);

					Vector dest = info.rewindLocations().poll();
					if (dest != null) {
						//Past Location succesfully found
						//Apply buff at departure AND arrival location
						rewindBuff(player, info, currTick);
						player.teleport(dest.toLocation(world));
						world.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
						rewindBuff(player, info, currTick);
						player.setCooldown(Material.CLOCK, 15 * 20);
						info.rewindLocations().clear();
					} else {
						//Failure
						Component warning = Component.text("The past seems uncertain...");
						player.sendMessage(warning);
						player.setCooldown(Material.CLOCK, 10);
					}
				}
			}

			//Time Stasis implementation
			if (mat == Material.SHULKER_SHELL && player.getCooldown(Material.SHULKER_SHELL) == 0) {
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
					player.getWorld().stopSound(SoundStop.named(Sound.ITEM_ARMOR_EQUIP_IRON));
					player.getWorld().stopSound(SoundStop.named(Sound.ITEM_ARMOR_EQUIP_CHAIN));
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

		//When rewinding, a buff is given based on a 15 second cycle with 3 sections, each with a 5 second timeframe
		public void rewindBuff(Player player, RewindInfo info, int currTick) {
			//Returns how far the currTick is in the cycle
			//[0, 299]
			int elapsedTick = (currTick - info.startingTick()) % TICK_CYCLE;
			if (elapsedTick >= 0 && elapsedTick < 5 * 20) {
				//Regen 2 for 7.5 seconds => 3 hearts healed
				player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 150, 1));
			} else if (elapsedTick < 10 * 20) {
				//Time Dilation: Gives nearby enemies Slow 3 + No Jump for 3 seconds
				List<Entity> affectedEnemies = player.getNearbyEntities(8, 8, 8);
				for (Entity entity : affectedEnemies) {
					if (entity instanceof LivingEntity victim && !(entity instanceof ArmorStand)) {
						//change to 3*20 tick duration, extended for testing purposes
						victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 3 * 20, 250, true));
						victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 3 * 20, 2, true));
					}
				}
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.AMBIENT, 0.5f, 1f);
			} else {
				//Knockback: KB in an AoE
				player.getWorld().createExplosion(player, 0f, false, false);

				//KB Amp
				List<Entity> affectedEnemies = player.getNearbyEntities(3, 3, 3);
				for (Entity entity : affectedEnemies) {
					if (entity instanceof LivingEntity victim && !(entity instanceof ArmorStand)) {
						if(!(victim instanceof Player p) || Main.getGame().canAttack(player, p)) {
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
				player.getWorld().stopSound(SoundStop.named(Sound.ENTITY_GENERIC_EXPLODE));
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

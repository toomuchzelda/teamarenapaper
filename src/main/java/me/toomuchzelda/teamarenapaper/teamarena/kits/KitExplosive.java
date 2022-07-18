package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;
/*
//Kit Explosive:
// Primary: Utility
// Secondary: Ranged

RWF explosive but...

RPG has lower cooldown and lower dmg + Rocket Jump
Grenade has more up-time but cannot be spammed as much

Overall lower cooldowns and less burst damage, so it has more consistent damage output
 */

/**
 * @author onett425
 */
public class KitExplosive extends Kit {
	// yet another shade of yellow? why??
	private static final TextColor ITEM_YELLOW = TextColor.color(255, 241, 120);

	public static final ItemStack GRENADE = ItemBuilder.of(Material.FIREWORK_STAR)
			.displayName(Component.text("Grenade", ITEM_YELLOW)).build();
	public static final ItemStack RPG = ItemBuilder.of(Material.EGG)
			.displayName(Component.text("RPG", ITEM_YELLOW)).build();

	public KitExplosive() {
		super("Explosive", "Destroy waves of enemies with the power of explosives!", Material.FIREWORK_STAR);

		ItemStack sword = new ItemStack(Material.STONE_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
		sword.setItemMeta(swordMeta);

		setItems(sword, RPG.asQuantity(2), GRENADE.asQuantity(5));
		setArmor(new ItemStack(Material.DIAMOND_HELMET), new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.GOLDEN_LEGGINGS), new ItemStack(Material.DIAMOND_BOOTS));
		setAbilities(new ExplosiveAbility());
	}

	public static class ExplosiveAbility extends Ability {

		//recharge time = time to get +1 item back
		//max active = max # of items you can have active at a time
		//max in inv = max # of items you can have in inventory
		public static final int RPG_RECHARGE_TIME = 240;
		public static final int RPG_MAX_IN_INV = 2;
		public static final int RPG_CD = 10;
		public static final double RPG_BLAST_RADIUS = 4.5;

		public static final int GRENADE_RECHARGE_TIME = 80;
		public static final int GRENADE_MAX_ACTIVE = 3;
		public static final int GRENADE_MAX_IN_INV = 5;
		public static final int GRENADE_FUSE_TIME = 60;

		public static final DamageType SELF_RPG = new DamageType(DamageType.EXPLOSIVE_RPG, "%Killed% shot their RPG a bit too close to themselves");

		private record RPGInfo(Arrow rpgArrow, Egg rpgEgg, Player thrower, int spawnTime) {}
		private record GrenadeInfo(Item grenade, Player thrower, Color color, int spawnTime) {}

		//ACTIVE handles behavior of active explosives
		//RECHARGES handles giving players explosives
		private static final Map<Player, List<GrenadeInfo>> ACTIVE_GRENADES = new LinkedHashMap<>();
		private final Map<Player, Integer> GRENADE_RECHARGES = new LinkedHashMap<>();
		private static final Map<Player, List<RPGInfo>> ACTIVE_RPG = new LinkedHashMap<>();
		private final Map<Player, Integer> RPG_RECHARGES = new LinkedHashMap<>();

		@Override
		public void unregisterAbility() {
			ACTIVE_GRENADES.forEach((player, grenades) -> {
				grenades.forEach(grenadeInfo -> grenadeInfo.grenade().remove());
				grenades.clear();
			});
			ACTIVE_GRENADES.clear();
			GRENADE_RECHARGES.clear();

			ACTIVE_RPG.forEach((player, rpgs) -> {
				rpgs.forEach(rpgInfo -> rpgInfo.rpgArrow().remove());
				rpgs.clear();
			});
			ACTIVE_RPG.clear();
			RPG_RECHARGES.clear();
		}

		@Override
		public void giveAbility(Player player) {
			GRENADE_RECHARGES.put(player, TeamArena.getGameTick());
			RPG_RECHARGES.put(player, TeamArena.getGameTick());
		}

		@Override
		public void removeAbility(Player player) {
			GRENADE_RECHARGES.remove(player);
			RPG_RECHARGES.remove(player);

			List<RPGInfo> rpgs = ACTIVE_RPG.remove(player);
			if(rpgs != null) {
				rpgs.forEach(rpgInfo -> rpgInfo.rpgArrow().remove());
				rpgs.clear();
			}

			List<GrenadeInfo> grenades = ACTIVE_GRENADES.remove(player);
			if(grenades != null) {
				grenades.forEach(grenadeInfo -> grenadeInfo.grenade().remove());
				grenades.clear();
			}
		}

		//Prevent RPG arrow from hitting players
		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if (event.getDamageType().is(DamageType.PROJECTILE)) {
				event.setCancelled(true);
				event.getAttacker().remove();
			}
		}

		@Override
		public void onTick() {
			//Handling Grenade Behavior
			var allGrenadesIter = ACTIVE_GRENADES.entrySet().iterator();
			while(allGrenadesIter.hasNext()) {
				var playerGrenadesIter = allGrenadesIter.next().getValue().iterator();
				while(playerGrenadesIter.hasNext()) {
					GrenadeInfo grenadeInfo = playerGrenadesIter.next();

					World world = grenadeInfo.thrower().getWorld();
					Item grenade = grenadeInfo.grenade();
					Particle.DustOptions particleOptions = new Particle.DustOptions(grenadeInfo.color(), 1);

					//Explode grenade if fuse time passes
					if (TeamArena.getGameTick() - grenadeInfo.spawnTime >= GRENADE_FUSE_TIME) {
						//real thrower info is passed on through grenade's thrower field
						TeamArenaExplosion explosion = new TeamArenaExplosion(null, 2, 0.5,
								9, 3.5, 0.35, DamageType.EXPLOSIVE_GRENADE, grenade);
						explosion.explode();

						grenade.remove();
						playerGrenadesIter.remove();
					}
					//Grenade particles
					else {
						//Particles for when grenade has landed
						if (grenade.isOnGround()) {
							world.spawnParticle(Particle.REDSTONE, grenade.getLocation(),
									1, 0.25, 0.25, 0.25, particleOptions);
						}
						//Particles for when grenade is in motion
						else {
							world.spawnParticle(Particle.REDSTONE, grenade.getLocation(),
									1, particleOptions);
						}
					}
				}
			}

			//Handling RPG Behavior
			var allRpgIter = ACTIVE_RPG.entrySet().iterator();
			while(allRpgIter.hasNext()) {
				var playerRpgIter = allRpgIter.next().getValue().iterator();
				while (playerRpgIter.hasNext()) {
					RPGInfo rpgInfo = playerRpgIter.next();

					World world = rpgInfo.thrower().getWorld();
					Player thrower = rpgInfo.thrower();
					Arrow rpgArrow = rpgInfo.rpgArrow();
					Egg rpgEgg = rpgInfo.rpgEgg();

					rpgEgg.remove();

					//Explode RPG if it hits block or player
					if (rpgArrow.isInBlock() || rpgArrow.isOnGround() || rpgArrow.isDead()) {
						rpgBlast(rpgArrow.getLocation(), thrower);

						world.playSound(rpgArrow.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
						world.spawnParticle(Particle.EXPLOSION_LARGE, rpgArrow.getLocation(), 1);
						rpgArrow.remove();
						playerRpgIter.remove();
					}
					//RPG particle trail
					else {
						if ((TeamArena.getGameTick() - rpgInfo.spawnTime()) % 2 == 0) {
							world.spawnParticle(Particle.EXPLOSION_LARGE, rpgArrow.getLocation(), 1);
						}
					}
				}
			}

			//Handling giving explosives to players
			GRENADE_RECHARGES.forEach((player, lastUsedTick) -> {
				itemDist(player, lastUsedTick, GRENADE_MAX_IN_INV, GRENADE_RECHARGE_TIME, GRENADE);
			});
			RPG_RECHARGES.forEach((player, lastUsedTick) -> {
				itemDist(player, lastUsedTick, RPG_MAX_IN_INV, RPG_RECHARGE_TIME, RPG);
			});
		}

		@Override
		public void onAttemptedDamage(DamageEvent event) {
			if(event.getDamageType().is(SELF_RPG)) {
				event.setFinalDamage(5); //self RPG always does 5 damage
			}
		}

		public void rpgBlast(Location explodeLoc, Player owner) {
			//self damage multiplier does not matter here, is overridden in attempted damage
			SelfHarmingExplosion explosion = new SelfHarmingExplosion(explodeLoc, RPG_BLAST_RADIUS, 1d,
					8.1, 0.18, 1.575, DamageType.EXPLOSIVE_RPG, owner, 5,0, SELF_RPG);
			explosion.explode();
		}

		//Based on the lastUsedTick, itemDist gives the player the desiredItem
		//@ rechargeTime until maxCount is reached
		public void itemDist(Player player, int lastUsedTick,
							 int maxCount, int rechargeTime, ItemStack desiredItem) {
			PlayerInventory inv = player.getInventory();
			int itemCount = ItemUtils.getMaterialCount(inv, desiredItem.getType());

			if (itemCount < maxCount &&
					(TeamArena.getGameTick() - lastUsedTick) % rechargeTime == 0) {
				if (inv.getItemInOffHand().isSimilar(desiredItem)) {
					inv.getItemInOffHand().add();
				} else {
					inv.addItem(desiredItem);
				}
			}
		}

		@Override
		public void onPlayerTick(Player player) {
			//Fixing glitch where player can get extra explosives by "hiding" grenades
			//in inventory's crafting menu
			PlayerInventory inv = player.getInventory();
			//Ignore excess explosives if the player is in creative mode and is admin abusing
			if (player.getGameMode() != GameMode.CREATIVE) {
				ItemUtils.maxItemAmount(inv, GRENADE, GRENADE_MAX_IN_INV);
				ItemUtils.maxItemAmount(inv, RPG, RPG_MAX_IN_INV);
			}
		}

		@Override
		public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
			Material mat = event.getItemStack().getType();
			Projectile proj = event.getProjectile();
			Player shooter = event.getPlayer();
			World world = shooter.getWorld();
			PlayerInventory inv = shooter.getInventory();

			//Launching RPG
			if (mat == Material.EGG) {
				//Only apply CD when thrower is not in creative mode to allow for admin abuse
				if (shooter.getGameMode() != GameMode.CREATIVE) {
					shooter.setCooldown(Material.EGG, RPG_CD);
				}
				//Resetting RPG recharge time
				if (ItemUtils.getMaterialCount(inv, RPG.getType()) == RPG_MAX_IN_INV) {
					RPG_RECHARGES.put(shooter, TeamArena.getGameTick());
				}

				//Replacing the Egg with an arrow to get the appropriate trajectory
				Vector vel = proj.getVelocity();
				Location loc = proj.getLocation();
				Egg rpgEgg = (Egg) proj;

				Arrow rpgArrow = world.spawn(loc, Arrow.class, arrow -> {
					arrow.setVelocity(vel);
					arrow.setSilent(true);
					arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
					arrow.setShooter(shooter);
				});

				List<RPGInfo> list = ACTIVE_RPG.computeIfAbsent(shooter, player -> new LinkedList<>());
				list.add(new RPGInfo(rpgArrow, rpgEgg, shooter, TeamArena.getGameTick()));
			}
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			ItemStack item = event.getItem();
			Material mat = item != null ? item.getType() : Material.AIR;
			Player player = event.getPlayer();
			World world = player.getWorld();
			Color teamColor = Main.getPlayerInfo(player).team.getColour();

			//Launching Grenade
			if (mat != Material.FIREWORK_STAR || !event.getAction().isRightClick()) {
				return;
			}

			//Finding all the currently active grenades that are owned by the current thrower
			List<GrenadeInfo> currActiveGrenades = ACTIVE_GRENADES.computeIfAbsent(player, player1 -> new LinkedList<>());

			//Throw grenade if # of active grenades doesn't exceed the cap
			if (player.getGameMode() != GameMode.CREATIVE && currActiveGrenades.size() >= GRENADE_MAX_ACTIVE) {
				player.sendMessage(Component.text("Only " + GRENADE_MAX_ACTIVE + " Grenades may be active at once!",
						ITEM_YELLOW));
				return;
			}
			//Creating the grenade item to be thrown
			PlayerInventory inv = player.getInventory();
			ItemStack grenade = new ItemStack(Material.FIREWORK_STAR);
			FireworkEffectMeta grenadeMeta = (FireworkEffectMeta) grenade.getItemMeta();
			FireworkEffect fireworkColor = FireworkEffect.builder().withColor(teamColor).build();
			grenadeMeta.setEffect(fireworkColor);
			grenade.setItemMeta(grenadeMeta);

			//Initializing the grenade Item entity
			Location initialPoint = player.getEyeLocation().clone().subtract(0, 0.2, 0);
			Item grenadeDrop = world.dropItem(initialPoint, grenade, entity -> {
				entity.setCanMobPickup(false);
				entity.setCanPlayerPickup(false);
				entity.setUnlimitedLifetime(true);
				entity.setWillAge(false);
				entity.setThrower(player.getUniqueId());
			});
			world.playSound(grenadeDrop, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.1f);

			//Throwing the grenade and activating it
			Vector vel = player.getLocation().getDirection().multiply(0.8);
			grenadeDrop.setVelocity(vel);
			currActiveGrenades.add(new GrenadeInfo(grenadeDrop, player, teamColor, TeamArena.getGameTick()));

			//Resetting Grenade recharge time
			if (ItemUtils.getMaterialCount(inv, GRENADE.getType()) == GRENADE_MAX_IN_INV) {
				GRENADE_RECHARGES.put(player, TeamArena.getGameTick());
			}

			//Remove grenade from Inventory after it is thrown
			if (event.getHand() == EquipmentSlot.HAND) {
				inv.setItemInMainHand(item.subtract());
			} else {
				inv.setItemInOffHand(item.subtract());
			}

		}
	}
}

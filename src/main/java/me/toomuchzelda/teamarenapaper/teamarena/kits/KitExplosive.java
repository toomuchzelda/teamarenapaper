package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.frost.ProjDeflect;
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
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

	public static ItemStack GRENADE = ItemBuilder.of(Material.FIREWORK_STAR)
			.displayName(Component.text("Grenade", ITEM_YELLOW)).build();
	public static ItemStack RPG = ItemBuilder.of(Material.EGG)
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
		public static final double RPG_BLAST_RADIUS_SQRD = 20.25;
		public static final double RPG_BLAST_STRENGTH = 0.35d;

		public static final int GRENADE_RECHARGE_TIME = 80;
		public static final int GRENADE_MAX_ACTIVE = 3;
		public static final int GRENADE_MAX_IN_INV = 5;
		public static final int GRENADE_FUSE_TIME = 60;

		//ACTIVE handles behavior of active explosives
		//RECHARGES handles giving players explosives
		public static final List<GrenadeInfo> ACTIVE_GRENADES = new ArrayList<>();
		public final Map<Player, Integer> GRENADE_RECHARGES = new LinkedHashMap<>();
		public static final List<RPGInfo> ACTIVE_RPG = new ArrayList<>();
		public final Map<Player, Integer> RPG_RECHARGES = new LinkedHashMap<>();
		//Color is only used for identifying arrow as an RPG
		public static final Color RPG_ARROW_COLOR = Color.fromRGB(3, 13, 0);

		@Override
		public void unregisterAbility() {
			ACTIVE_GRENADES.forEach(grenadeInfo -> grenadeInfo.grenade().remove());
			ACTIVE_GRENADES.clear();
			GRENADE_RECHARGES.clear();

			ACTIVE_RPG.forEach(rpgInfo -> rpgInfo.rpgArrow().remove());
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
			List<GrenadeInfo> staleGrenades = new ArrayList<>();
			List<RPGInfo> staleRPG = new ArrayList<>();

			//Handling Grenade Behavior
			ACTIVE_GRENADES.forEach(grenadeInfo -> {
				World world = grenadeInfo.thrower().getWorld();
				Player thrower = grenadeInfo.thrower();
				Item grenade = grenadeInfo.grenade();

				if(ProjDeflect.getShooterOverride(grenade) != null){
					thrower = ProjDeflect.getShooterOverride(grenade);

					Color teamColor = Main.getPlayerInfo(thrower).team.getColour();
					ItemStack grenadeItem = grenade.getItemStack();
					FireworkEffectMeta grenadeMeta = (FireworkEffectMeta) grenadeItem.getItemMeta();
					FireworkEffect fireworkColor = FireworkEffect.builder().withColor(teamColor).build();
					grenadeMeta.setEffect(fireworkColor);
					grenade.getItemStack().setItemMeta(grenadeMeta);
					grenade.setItemStack(grenadeItem);
				}

				Color color = Main.getPlayerInfo(thrower).team.getColour();
				Particle.DustOptions particleOptions = new Particle.DustOptions(color, 1);

				//Explode grenade if fuse time passes
				if (TeamArena.getGameTick() - grenadeInfo.spawnTime >= GRENADE_FUSE_TIME) {
					//Only explode if the thrower is still alive
					if (!Main.getGame().isDead(thrower)) {
						world.createExplosion(grenade.getLocation(), 1.5f, false, false, thrower);
					}
					grenade.remove();
					staleGrenades.add(grenadeInfo);
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
			});

			//Handling RPG Behavior
			ACTIVE_RPG.forEach(rpgInfo -> {
				Player thrower = (Player) rpgInfo.rpgArrow().getShooter();
				World world = thrower.getWorld();
				Arrow rpgArrow = rpgInfo.rpgArrow();
				Egg rpgEgg = rpgInfo.rpgEgg();

				//For Kit Frost when it deflects ability projectiles
				//Indirectly changes the shooter of the projectile
				if(ProjDeflect.getShooterOverride(rpgArrow) != null){
					thrower = ProjDeflect.getShooterOverride(rpgArrow);
				}

				rpgEgg.remove();
				//Hiding arrow
				Bukkit.getOnlinePlayers().forEach(player -> {
					List<Integer> entityIDList = new ArrayList<>();
					entityIDList.add(rpgArrow.getEntityId());

					ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
					PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
					packet.getIntLists().write(0, entityIDList);

					protocolManager.sendServerPacket(player, packet);
				});

				//Explode RPG if it hits block or player
				if (rpgArrow.isInBlock() || rpgArrow.isOnGround() || rpgArrow.isDead()) {
					rpgBlast(rpgArrow.getLocation().clone(), thrower);

					world.playSound(rpgArrow.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
					world.spawnParticle(Particle.EXPLOSION_LARGE, rpgArrow.getLocation(), 1);
					rpgArrow.remove();
					staleRPG.add(rpgInfo);
				}
				//RPG particle trail
				else {
					if ((TeamArena.getGameTick() - rpgInfo.spawnTime()) % 2 == 0) {
						world.spawnParticle(Particle.EXPLOSION_LARGE, rpgArrow.getLocation(), 1);
					}
				}
			});

			//Handling giving explosives to players
			GRENADE_RECHARGES.forEach((player, lastUsedTick) -> {
				itemDist(player, lastUsedTick, GRENADE_MAX_IN_INV, GRENADE_RECHARGE_TIME, GRENADE);
			});
			RPG_RECHARGES.forEach((player, lastUsedTick) -> {
				itemDist(player, lastUsedTick, RPG_MAX_IN_INV, RPG_RECHARGE_TIME, RPG);
			});

			//Cleaning up Stale RPG + Grenades
			staleGrenades.forEach(ACTIVE_GRENADES::remove);
			staleRPG.forEach(ACTIVE_RPG::remove);
		}

		public void rpgBlast(Location explodeLoc, Player owner) {
			//Stolen from toomuchzelda
			//create a sort of explosion that pushes everyone away
			World world = owner.getWorld();
			Vector explodeLocVec = explodeLoc.toVector();
			RayTraceResult result;
			TeamArenaTeam team = Main.getPlayerInfo(owner).team;
			for (Player p : Main.getGame().getPlayers()) {
				if (!team.getPlayerMembers().contains(p) || p.equals(owner)) {
					double blastStrength = RPG_BLAST_STRENGTH;
					//Owner receives more KB from RPG blast
					if (p.equals(owner)) {

					} else {
						blastStrength /= 1.5;
					}
					//add half of height so aim for middle of body not feet
					Vector vector = p.getLocation().add(0, p.getHeight() / 2, 0).toVector().subtract(explodeLocVec);

					result = world.rayTrace(explodeLoc, vector, RPG_BLAST_RADIUS, FluidCollisionMode.SOURCE_ONLY, true, 0,
							e -> e == p);
					//Bukkit.broadcastMessage(result.toString());
					double lengthSqrd = vector.lengthSquared();
					boolean affect = false;
					if (result != null && result.getHitEntity() == p) {
						affect = true;
					}
					//even if raytrace didn't hit, if they are within 1.1 block count it anyway
					else if (lengthSqrd <= 1.21d) {
						affect = true;
					}

					if (affect) {
						//weaker knockback the further they are from mine base
						double power = Math.sqrt(RPG_BLAST_RADIUS_SQRD - lengthSqrd);
						vector.normalize();
						vector.add(p.getVelocity().multiply(0.4));
						vector.multiply(power * blastStrength);
						PlayerUtils.sendVelocity(p, PlayerUtils.noNonFinites(vector));

						//RPG Custom Damage, so it is more consistent. Ignore KB since it is already handled above
						if (p.equals(owner) && p.getGameMode() == GameMode.SURVIVAL) {
							//Self Damage deals a consistent amount
							DamageEvent dEvent = DamageEvent.newDamageEvent(owner, 5.0d, DamageType.EXPLOSION, null, false);
							dEvent.setNoKnockback();
							Main.getGame().queueDamage(dEvent);
						} else if (!team.getPlayerMembers().contains(p) && p.getGameMode() == GameMode.SURVIVAL) {
							//Enemy Damage is based on distance from bomb
							double damage = Math.max(power, 0.1d) * 1.8d;
							DamageEvent dEvent = DamageEvent.newDamageEvent(p, damage, DamageType.EXPLOSION, owner, false);
							dEvent.setNoKnockback();
							Main.getGame().queueDamage(dEvent);
						}
					}
				}
			}
		}

		//Based on the lastUsedTick, itemDist gives the player the desiredItem
		//@ rechargeTime until maxCount is reached
		public void itemDist(Player player, int lastUsedTick,
							 int maxCount, int rechargeTime, ItemStack desiredItem) {
			PlayerInventory inv = player.getInventory();
			int itemCount = getInvCount(inv, desiredItem);

			if (itemCount < maxCount &&
					(TeamArena.getGameTick() - lastUsedTick) % rechargeTime == 0) {
				if (inv.getItemInOffHand().isSimilar(desiredItem)) {
					inv.getItemInOffHand().add();
				} else {
					inv.addItem(desiredItem);
				}
			}
		}

		//Returns a count of how much of desiredItem is in inv
		public int getInvCount(PlayerInventory inv, ItemStack desiredItem) {
			ItemStack[] items = inv.getContents();
			int itemCount = 0;
			for (ItemStack item : items) {
				if (item != null && item.getType() == desiredItem.getType()) {
					itemCount += item.getAmount();
				}
			}
			return itemCount;
		}

		//If the inventory exceeds the maxCount of the targetItem, set the quantity
		//of that item equal to maxCount
		public void correctInv(PlayerInventory inv, ItemStack targetItem, int maxCount) {
			int count = 0;
			for (var iterator = inv.iterator(); iterator.hasNext(); ) {
				ItemStack stack = iterator.next();
				if (stack == null || !targetItem.isSimilar(stack))
					continue;
				int amount = stack.getAmount();
				if (count + amount > maxCount) {
					if (maxCount - count > 0) {
						stack.setAmount(maxCount - count);
						count = maxCount;
						iterator.set(stack);
					} else {
						iterator.set(null);
					}
				} else {
					count += amount;
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
				correctInv(inv, GRENADE, GRENADE_MAX_IN_INV);
				correctInv(inv, RPG, RPG_MAX_IN_INV);
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
				if (getInvCount(inv, RPG) == RPG_MAX_IN_INV) {
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
					arrow.setColor(RPG_ARROW_COLOR);
				});

				ACTIVE_RPG.add(new RPGInfo(rpgArrow, rpgEgg, TeamArena.getGameTick()));
			}
		}

		public record RPGInfo(Arrow rpgArrow, Egg rpgEgg, int spawnTime) {}

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
			List<GrenadeInfo> currActiveGrenades = ACTIVE_GRENADES.stream()
					.filter(grenadeInfo -> grenadeInfo.thrower().equals(player)).toList();

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
			});
			world.playSound(grenadeDrop, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.1f);

			//Throwing the grenade and activating it
			Vector vel = player.getLocation().getDirection().multiply(0.8);
			grenadeDrop.setVelocity(vel);
			ACTIVE_GRENADES.add(new GrenadeInfo(grenadeDrop, player, TeamArena.getGameTick()));

			//Resetting Grenade recharge time
			if (getInvCount(inv, GRENADE) == GRENADE_MAX_IN_INV) {
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

	public record GrenadeInfo(Item grenade, Player thrower, int spawnTime) {}
}

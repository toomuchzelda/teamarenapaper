package me.toomuchzelda.teamarenapaper.teamarena.kits.explosive;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.ProjectileReflectEvent;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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
public class KitExplosive extends Kit
{
	private static final TextColor ITEM_YELLOW = TextColor.color(255, 241, 120);

	public static final ItemStack GRENADE = ItemBuilder.of(Material.FIREWORK_STAR)
			.displayName(Component.text("Grenade", ITEM_YELLOW))
			.lore(TextUtils.wrapString("Right click to throw one. You can only have "
					+ ExplosiveAbility.GRENADE_MAX_ACTIVE + " out at a time", Style.style(TextUtils.RIGHT_CLICK_TO)))
			.build();

	public static final ItemStack RPG = ItemBuilder.of(Material.EGG)
			.displayName(Component.text("RPG", ITEM_YELLOW))
			.lore(TextUtils.wrapString("Right click to charge one up. Once charged, it'll fire itself forward!", Style.style(TextUtils.RIGHT_CLICK_TO)))
			.build();

	public KitExplosive() {
		super("Explosive", "Destroy waves of enemies with the power of explosives!\n\n" +
				"Deal massive boom damage from a range with the RPG (actually a very explosive egg)!\n" +
				"Make enemies run for their lives by throwing out grenades!\n\n" +
				"You'll never have enough explosives with Kit Explosive!", Material.FIREWORK_STAR);

		ItemStack sword = new ItemStack(Material.STONE_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
		sword.setItemMeta(swordMeta);

		setItems(sword, RPG.asQuantity(2), GRENADE.asQuantity(5));
		setArmor(new ItemStack(Material.DIAMOND_HELMET), new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.GOLDEN_LEGGINGS), new ItemStack(Material.DIAMOND_BOOTS));
		setAbilities(new ExplosiveAbility());

		this.setCategory(KitCategory.UTILITY);
	}

	public static class ExplosiveAbility extends Ability {

		//recharge time = time to get +1 item back
		//max active = max # of items you can have active at a time
		//max in inv = max # of items you can have in inventory
		public static final int RPG_RECHARGE_TIME = 16 * 20;
		public static final int RPG_MAX_IN_INV = 2;
		public static final int RPG_CD = 7 * 20;
		public static final double RPG_BLAST_RADIUS = 8;
		private static final int RPG_CHARGEUP_TIME = 35; //1.75 secs

		private static final Component RPG_CHARGE_BOSSBAR_NAME = Component.text("CHARGING", NamedTextColor.YELLOW, TextDecoration.BOLD);
		private static final Component RPG_CHARGE_ALMOST_READY = Component.text("CHARGING", NamedTextColor.GOLD, TextDecoration.BOLD);

		public static final int GRENADE_RECHARGE_TIME = 80;
		public static final int GRENADE_MAX_ACTIVE = 3;
		public static final int GRENADE_MAX_IN_INV = 5;
		public static final int GRENADE_FUSE_TIME = 60;

		private static class ExplosiveInfo {
			private RPGChargeInfo chargingRpg;
			private final List<RPGInfo> activeRpgs;
			private int rpgRecharge;
			private final List<GrenadeInfo> activeGrenades;
			private int grenadeRecharge;

			public ExplosiveInfo() {
				this.activeRpgs = new ArrayList<>(1);
				this.activeGrenades = new ArrayList<>(GRENADE_MAX_ACTIVE);

				final int currentTick = TeamArena.getGameTick();
				rpgRecharge = currentTick;
				grenadeRecharge = currentTick;
			}
		}

		//info for charging up rpgs
		private record RPGChargeInfo(BossBar bossbar, int throwTime) {}
		//info for thrown rpgs
		private record RPGInfo(Arrow rpgArrow, int spawnTime) {}
		private record GrenadeInfo(Item grenade, Color color, int spawnTime) {}

		private final Map<Player, ExplosiveInfo> explosiveInfos = new HashMap<>();
		private final Map<Player, RPGInfo> reflectedRpgs = new HashMap<>();

		@Override
		public void unregisterAbility() {
			for (ExplosiveInfo einfo : explosiveInfos.values()) {
				einfo.activeGrenades.forEach(grenadeInfo -> grenadeInfo.grenade.remove());
				einfo.activeGrenades.clear();

				einfo.activeRpgs.forEach(rpgInfo -> rpgInfo.rpgArrow.remove());
				einfo.activeRpgs.clear();
			}
			explosiveInfos.clear();

			for (var entry : reflectedRpgs.entrySet()) {
				entry.getValue().rpgArrow.remove();
			}
			reflectedRpgs.clear();
		}

		@Override
		public void giveAbility(Player player) {
			explosiveInfos.put(player, new ExplosiveInfo());
		}

		@Override
		public void removeAbility(Player player) {
			final ExplosiveInfo einfo = explosiveInfos.remove(player);

			if (einfo.chargingRpg != null)
				player.hideBossBar(einfo.chargingRpg.bossbar);

			einfo.activeRpgs.forEach(rpgInfo -> rpgInfo.rpgArrow.remove());
			einfo.activeRpgs.clear();

			einfo.activeGrenades.forEach(grenadeInfo -> grenadeInfo.grenade.remove());
			einfo.activeGrenades.clear();
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if (event.getDamageType().is(DamageType.PROJECTILE)) { //Prevent RPG arrow from hitting players
				event.setCancelled(true);
				event.getAttacker().remove();
			}
		}

		@Override
		public void onTick() {
			//Handling Grenade Behavior
			grenadeTick();

			//Handling RPG Behavior
			for (var entry : explosiveInfos.entrySet()) {
				var playerRpgIter = entry.getValue().activeRpgs.iterator();
				while (playerRpgIter.hasNext()) {
					final RPGInfo rpgInfo = playerRpgIter.next();

					if (rpgTick(entry.getKey(), rpgInfo, false)) {
						playerRpgIter.remove();
					}
				}
			}
			// Tick reflected RPGs
            for (var iterator = reflectedRpgs.entrySet().iterator(); iterator.hasNext(); ) {
                var entry = iterator.next();
				if (rpgTick(entry.getKey(), entry.getValue(), true)) {
					iterator.remove();
				}
            }

			//Tick RPG launches that are charging up
			rpgChargeTick();

			final int currentTick = TeamArena.getGameTick();
			for (var entry : explosiveInfos.entrySet()) {
				ItemUtils.addRechargedItem(entry.getKey(), currentTick, entry.getValue().grenadeRecharge, GRENADE_MAX_IN_INV,
					GRENADE_RECHARGE_TIME, GRENADE);
				ItemUtils.addRechargedItem(entry.getKey(), currentTick, entry.getValue().rpgRecharge, RPG_MAX_IN_INV,
					RPG_RECHARGE_TIME, RPG);
			}
		}

		private void rpgChargeTick() {
			for (var entry : explosiveInfos.entrySet()) {
				final ExplosiveInfo einfo = entry.getValue();
				final RPGChargeInfo cinfo = einfo.chargingRpg;
				if (cinfo == null)
					continue;

				final int currentTick = TeamArena.getGameTick();
				final Player thrower = entry.getKey();

				final int timeSince = currentTick - cinfo.throwTime();
				if(timeSince % 6 == 0)
					thrower.getWorld().playSound(thrower, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.65f, 0.8f);
				if(timeSince % 2 == 0)
					EntityUtils.playCritEffect(thrower);

				if(timeSince >= RPG_CHARGEUP_TIME) {
					einfo.chargingRpg = null;
					thrower.hideBossBar(cinfo.bossbar());

					rpgLaunch(thrower, einfo);
				}
				//still charging: increment the progress bar
				else {
					float newProgress = ((float) timeSince) / (float) RPG_CHARGEUP_TIME;
					newProgress = MathUtils.clamp(0f, 1f, newProgress);
					cinfo.bossbar().progress(newProgress);

					if(newProgress >= 0.5f) {
						cinfo.bossbar().name(RPG_CHARGE_ALMOST_READY);
					}
				}
			}
		}

		// return true if RPG is done and cleanup needed
		private boolean rpgTick(final Player thrower, RPGInfo rpgInfo, boolean reflected) {
			final World world = thrower.getWorld();
			final Arrow rpgArrow = rpgInfo.rpgArrow();
			final Location arrowLoc = rpgArrow.getLocation();

			//Explode RPG if it hits block or player
			if (rpgArrow.isInBlock() || rpgArrow.isOnGround() || rpgArrow.isDead() ||
					rpgArrow.getTicksLived() >= 38) {
				rpgBlast(arrowLoc, thrower, reflected);

				rpgArrow.remove();
				return true;
			}
			//RPG particle trail
			else {
				final int currentTick = TeamArena.getGameTick();
				if ((currentTick - rpgInfo.spawnTime()) % 5 == 0) {
					world.spawnParticle(Particle.EXPLOSION_LARGE, arrowLoc, 1);
				}

				if((currentTick - rpgInfo.spawnTime()) % 2 == 0) {
					ParticleUtils.colouredRedstone(arrowLoc, Main.getPlayerInfo(thrower).team.getColour(), 1d, 3f);
				}

				return false;
			}
		}

		private void grenadeTick() {
			final int currentTick = TeamArena.getGameTick();
			for (var entry : explosiveInfos.entrySet()) {
				var playerGrenadesIter = entry.getValue().activeGrenades.iterator();
				while (playerGrenadesIter.hasNext()) {
					final GrenadeInfo grenadeInfo = playerGrenadesIter.next();

					final Item grenade = grenadeInfo.grenade();
					final Particle.DustOptions particleOptions = new Particle.DustOptions(grenadeInfo.color(), 1);

					//Explode grenade if fuse time passes
					if (currentTick - grenadeInfo.spawnTime >= GRENADE_FUSE_TIME) {
						//real thrower info is passed on through grenade's thrower field
						TeamArenaExplosion explosion = new TeamArenaExplosion(null, 3, 0.5, 10, 3.5, 0.35, DamageType.EXPLOSIVE_GRENADE, grenade);
						explosion.explode();

						grenade.remove();
						playerGrenadesIter.remove();
					}
					//Grenade particles
					else {
						//Particles for when grenade has landed
						World world = entry.getKey().getWorld();
						if (grenade.isOnGround()) {
							world.spawnParticle(Particle.REDSTONE, grenade.getLocation(), 1, 0.25, 0.25, 0.25, particleOptions);
						}
						//Particles for when grenade is in motion
						else {
							world.spawnParticle(Particle.REDSTONE, grenade.getLocation(), 1, particleOptions);
						}
					}
				}
			}
		}

		@Override
		public void onAttemptedDamage(DamageEvent event) {
			if(event.getDamageType().is(DamageType.EXPLOSIVE_RPG_SELF)) {
				event.setFinalDamage(5); //self RPG always does 5 damage
			}
		}

		public void rpgBlast(Location explodeLoc, Player owner, boolean reflected) {
			//self damage multiplier does not matter here, is overridden in attempted damage
			double selfDamageMult, selfKnockbackMult;
			if (!reflected) {
				selfDamageMult = 1.2d; selfKnockbackMult = 1d;
			}
			else {
				selfDamageMult = 0d; selfKnockbackMult = 0d;
			}
			RPGExplosion explosion = new RPGExplosion(explodeLoc, RPG_BLAST_RADIUS, 1.4d,
				25, 2, 1.7, DamageType.EXPLOSIVE_RPG, owner,
				selfDamageMult, selfKnockbackMult, DamageType.EXPLOSIVE_RPG_SELF);

			explosion.explode();
		}

		/*@Override // No longer needed
		public void onPlayerTick(Player player) {
			//Fixing glitch where player can get extra explosives by "hiding" grenades
			//in inventory's crafting menu
			PlayerInventory inv = player.getInventory();
			//Ignore excess explosives if the player is in creative mode and is admin abusing
			if (player.getGameMode() != GameMode.CREATIVE) {
				ItemUtils.maxItemAmount(inv, GRENADE, GRENADE_MAX_IN_INV);
				ItemUtils.maxItemAmount(inv, RPG, RPG_MAX_IN_INV);
			}
		}*/

		@Override
		public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
			final Player shooter = event.getPlayer();
			//Launching RPG
			if (event.getItemStack().getType() == RPG.getType()) {
				event.setCancelled(true);
				//make sure they're not already charging up a shot
				final ExplosiveInfo einfo = explosiveInfos.get(shooter);
				if(einfo.chargingRpg == null) {
					shooter.getWorld().playSound(shooter, Sound.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 3f, 1.1f);

					//start charging their rpg charge
					BossBar chargeBar = BossBar.bossBar(RPG_CHARGE_BOSSBAR_NAME, 0f, BossBar.Color.YELLOW,
							BossBar.Overlay.PROGRESS);
					shooter.showBossBar(chargeBar);
					einfo.chargingRpg = new RPGChargeInfo(chargeBar, TeamArena.getGameTick());
				}
			}
		}

		private void rpgLaunch(final Player shooter, final ExplosiveInfo einfo) {
			//Only apply CD when thrower is not in creative mode to allow for admin abuse
			if (shooter.getGameMode() != GameMode.CREATIVE) {
				shooter.setCooldown(Material.EGG, RPG_CD);
			}
			//Resetting RPG recharge time
			final PlayerInventory inv = shooter.getInventory();
			//remove one egg for launch rpg
			List<ItemStack> eggs = ItemUtils.getItemsInInventory(RPG, inv);
			if(eggs.isEmpty()) {
				Main.logger().severe(shooter.getName() + " is firing an RPG but does not have any eggs in their inventory?!");
				Thread.dumpStack();
			}
			else {
				ItemStack eggStack = eggs.get(0);
				eggStack.setAmount(eggStack.getAmount() - 1);
			}

			if (!CompileAsserts.OMIT && ItemUtils.getMaterialCount(inv, RPG.getType()) == RPG_MAX_IN_INV) {
				einfo.rpgRecharge = TeamArena.getGameTick();
				Main.logger().severe("The forbidden code has run. KitExplosive.ExplosiveAbility.rpgLaunch()");
			}

			Location loc = shooter.getEyeLocation();
			Vector vel = loc.getDirection().multiply(1.2d);

			World world = shooter.getWorld();
			Arrow rpgArrow = world.spawn(loc, Arrow.class, arrow -> {
				arrow.setVelocity(vel);
				arrow.setSilent(true);
				arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
				arrow.setShooter(shooter);
			});

			//sound effect
			shooter.getWorld().playSound(shooter, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 2f, 0.5f);

			List<RPGInfo> list = einfo.activeRpgs;
			list.add(new RPGInfo(rpgArrow, TeamArena.getGameTick()));
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			final ItemStack item = event.getItem();
			final Material mat = item != null ? item.getType() : Material.AIR;
			final Player thrower = event.getPlayer();

			final Color teamColor = Main.getPlayerInfo(thrower).team.getColour();

			//Launching Grenade
			if (mat != Material.FIREWORK_STAR || !event.getAction().isRightClick()) {
				return;
			}

			//Finding all the currently active grenades that are owned by the current thrower
			final ExplosiveInfo einfo = explosiveInfos.get(thrower);
			final List<GrenadeInfo> currActiveGrenades = einfo.activeGrenades;

			//Throw grenade if # of active grenades doesn't exceed the cap
			if (thrower.getGameMode() != GameMode.CREATIVE && currActiveGrenades.size() >= GRENADE_MAX_ACTIVE) {
				thrower.sendMessage(Component.text("Only " + GRENADE_MAX_ACTIVE + " Grenades may be active at once!",
						ITEM_YELLOW));
				return;
			}
			//Creating the grenade item to be thrown
			final ItemStack grenade = new ItemStack(Material.FIREWORK_STAR);
			FireworkEffectMeta grenadeMeta = (FireworkEffectMeta) grenade.getItemMeta();
			FireworkEffect fireworkColor = FireworkEffect.builder().withColor(teamColor).build();
			grenadeMeta.setEffect(fireworkColor);
			grenade.setItemMeta(grenadeMeta);

			//Initializing the grenade Item entity
			final World world = thrower.getWorld();
			Location initialPoint = thrower.getEyeLocation().subtract(0, 0.2, 0);
			Item grenadeDrop = world.dropItem(initialPoint, grenade, entity -> {
				entity.setCanMobPickup(false);
				entity.setCanPlayerPickup(false);
				entity.setUnlimitedLifetime(true);
				entity.setWillAge(false);
				entity.setThrower(thrower.getUniqueId());
			});

			//Throwing the grenade and activating it
			Vector vel = thrower.getLocation().getDirection().multiply(0.8);
			grenadeDrop.setVelocity(vel);
			world.playSound(grenadeDrop, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.1f);

			currActiveGrenades.add(new GrenadeInfo(grenadeDrop, teamColor, TeamArena.getGameTick()));

			final PlayerInventory inv = thrower.getInventory();
			//Resetting Grenade recharge time
			if (ItemUtils.getMaterialCount(inv, GRENADE.getType()) == GRENADE_MAX_IN_INV) {
				einfo.grenadeRecharge = TeamArena.getGameTick();
			}

			//Remove grenade from Inventory after it is thrown
			if (event.getHand() == EquipmentSlot.HAND) {
				inv.setItemInMainHand(item.subtract());
			} else {
				inv.setItemInOffHand(item.subtract());
			}
		}

		//stop player from switching items when charging RPG
		@Override
		public void onSwitchItemSlot(PlayerItemHeldEvent event) {
			if(explosiveInfos.get(event.getPlayer()).chargingRpg != null) {
				event.setCancelled(true);
			}
		}
		@Override
		public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
			if (explosiveInfos.get(event.getPlayer()).chargingRpg != null) {
				event.setCancelled(true);
			}
		}
		@Override
		public void onInventoryClick(InventoryClickEvent event) {
			if (explosiveInfos.get((Player) event.getWhoClicked()).chargingRpg != null) {
				event.setCancelled(true);
			}
		}
		@Override
		public void onInventoryDrag(InventoryDragEvent event) {
			if (explosiveInfos.get((Player) event.getWhoClicked()).chargingRpg != null) {
				event.setCancelled(true);
			}
		}

		@Override
		public void onReflect(ProjectileReflectEvent event) {
			// Maybe an RPG
			if (event.projectile instanceof Arrow arrow && event.shooter instanceof Player shooter) {
				final ExplosiveInfo einfo = explosiveInfos.get(shooter);
				if (einfo != null) {
                    for (Iterator<RPGInfo> iterator = einfo.activeRpgs.iterator(); iterator.hasNext(); ) {
                        RPGInfo rinfo = iterator.next();
                        if (rinfo.rpgArrow.equals(arrow)) { // Indeed an explosive's RPG
                            if (!CompileAsserts.OMIT && Main.getPlayerInfo(event.reflector).team.hasMember(shooter)) {
                                Main.logger().warning("ExplosiveAbility.onReflect(), RPG was reflected by teammate");
                                Thread.dumpStack();
                            }

							// Set shooter to explosive so event handlers here can cancel the arrow's damage
							arrow.setShooter(shooter);
							event.overrideShooter = false;
							// Remove from explosive's ownership and add entry for the reflector
							iterator.remove();
							reflectedRpgs.put(event.reflector, new RPGInfo(arrow, TeamArena.getGameTick()));
                            break;
                        }
                    }
				}
			}
		}
	}
}

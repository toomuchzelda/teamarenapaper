package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DetailedProjectileHitEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.ProjectileReflectEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * @author onett425
 */
public class KitSniper extends Kit {

	// shared between all kits with grenades
	public static final Component BOMB_LORE = Component.text("Left Click to throw with high velocity", TextUtils.LEFT_CLICK_TO);
	public static final Component BOMB_LORE2 = Component.text("Right Click to toss with low velocity", TextUtils.RIGHT_CLICK_TO);

	public static final ItemStack GRENADE = ItemBuilder.of(Material.TURTLE_HELMET)
			.displayName(Component.text("Frag Grenade"))
			.lore(Component.text("A grenade that deals high explosive damage.", TextColors.LIGHT_YELLOW),
					Component.text("First click to pull the pin, Then click again to throw it!", TextColors.LIGHT_YELLOW),
					Component.text("Make sure to pay attention to its fuse time... (item cd)", TextColors.LIGHT_YELLOW),
					BOMB_LORE,
					BOMB_LORE2)
			.build();

	public static final ItemStack SNIPER = ItemBuilder.of(Material.SPYGLASS)
			.displayName(Component.text("CheyTac Intervention"))
			.build();

	public static final AttributeModifier KNIFE_SPEED = new AttributeModifier(
			UUID.fromString("743e8aec-10f7-44c7-b0b0-cf1f32634c72"),
			"Sniper Knife", 0.2, //20% = speed 1
			AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.HAND);

	public KitSniper() {
		super("Sniper", "Be careful when sniping... Too much movement and your aim will worsen. " +
				"Make sure to aim for the head! Don't forget to throw the grenade if you pull the pin btw.", Material.SPYGLASS);

		setArmor(new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE),
				new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_BOOTS));

		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		ItemMeta meta = sword.getItemMeta();
		meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, KNIFE_SPEED);
		sword.setItemMeta(meta);

		setItems(sword, SNIPER, GRENADE);
		setAbilities(new SniperAbility());

		setCategory(KitCategory.RANGED);
	}

	public static class SniperAbility extends Ability {
		private final Set<UUID> RECEIVED_SNIPER_CHAT_MESSAGE = new HashSet<>();
		private final Set<UUID> RECEIVED_GRENADE_CHAT_MESSAGE = new HashSet<>();
		public static final TextColor SNIPER_MSG_COLOR = TextColor.color(89, 237, 76);
		public static final TextColor GRENADE_MSG_COLOR = TextColor.color(66, 245, 158);

		private static final double MIN_HEIGHT_FOR_HEADHSHOT = 1.5d;

		private static final DamageType REFLECTED_SNIPER = new DamageType(DamageType.SNIPER_HEADSHOT,
			"%Cause% did not have enough map knowledge",
			"%Killed% suffered from %Cause%'s skill issue");

		final List<BukkitTask> GRENADE_TASKS = new ArrayList<>();
		final Random gunInaccuracy = new Random();

		@Override
		public void unregisterAbility() {
			GRENADE_TASKS.forEach(BukkitTask::cancel);
			GRENADE_TASKS.clear();
		}

		@Override
		public void giveAbility(Player player) {
			player.setExp(0.999f);
		}

		@Override
		public void removeAbility(Player player) {
			player.setExp(0);
			player.getInventory().remove(Material.TURTLE_HELMET);
			var uuid = player.getUniqueId();
			RECEIVED_SNIPER_CHAT_MESSAGE.remove(uuid);
			RECEIVED_GRENADE_CHAT_MESSAGE.remove(uuid);
		}

		public void throwGrenade(Player player, double amp) {
			World world = player.getWorld();
			Location origin = player.getEyeLocation();
			Item grenade = world.dropItem(origin, new ItemStack(Material.TURTLE_HELMET), item -> {
				item.setCanMobPickup(false);
				item.setCanPlayerPickup(false);
				item.setUnlimitedLifetime(true);
				item.setVelocity(origin.getDirection().multiply(amp));
			});
			world.playSound(origin, Sound.ENTITY_CREEPER_PRIMED, 1f, 1.1f);

			// schedule task
			TeamArenaTeam team = Main.getPlayerInfo(player).team;
			Particle.DustOptions particleOptions = new Particle.DustOptions(team.getColour(), 2);
			BukkitTask runnable = new BukkitRunnable() {
				//Grenade explosion
				int timer = player.getCooldown(Material.TURTLE_HELMET);

				public void run() {
					//Grenade Particles when it is thrown
					//In Motion
					if (!grenade.isOnGround()) {
						world.spawnParticle(Particle.REDSTONE, grenade.getLocation(), 1, particleOptions);
					} else {
						//On the ground
						world.spawnParticle(Particle.REDSTONE, grenade.getLocation(),
								2, 0.5, 0.5,0.5, particleOptions);
					}
					if (timer <= 0) {
						world.createExplosion(grenade.getLocation(), 1.7f, false, false);
						player.getInventory().addItem(GRENADE);
						grenade.remove();
						cancel();
					}
					timer--;
				}
			}.runTaskTimer(Main.getPlugin(), 0, 0);
			GRENADE_TASKS.add(runnable);
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			Material mat = event.getMaterial();
			ItemStack item = event.getItem();
			Player player = event.getPlayer();
			World world = player.getWorld();
			PlayerInventory inv = player.getInventory();
			Action action = event.getAction();
			PlayerInfo pinfo = Main.getPlayerInfo(player);

			//Grenade Pull Pin
			if (mat == Material.TURTLE_HELMET && !player.hasCooldown(Material.TURTLE_HELMET) && player.getExp() == 0.999f && player.getInventory().getItemInMainHand().getType() == Material.TURTLE_HELMET) {
				Component actionBar = Component.text("Left Click to THROW    Right Click to TOSS").color(TextColor.color(242, 44, 44));
				Component text = Component.text("Left Click to throw the grenade, Right Click to lightly toss it").color(TextColor.color(242, 44, 44));
				if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				if (pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					player.sendMessage(text);
				}
				player.setExp(0);
				player.setCooldown(Material.TURTLE_HELMET, (int) 3.5 * 20);
				world.playSound(player, Sound.ITEM_FLINTANDSTEEL_USE, 2.0f, 1.5f);
			}
			//Grenade Throw
			//Main Hand ONLY
			else if (mat == Material.TURTLE_HELMET) {
				event.setCancelled(true);
				if (player.hasCooldown(Material.TURTLE_HELMET) && event.getHand() == EquipmentSlot.HAND) {
					//Removes 1 grenade from hand
					inv.setItemInMainHand(item.subtract());
					if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
						//Left Click => Hard Throw
						throwGrenade(player, 1.5d);
					} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
						//Right Click => Soft Toss
						throwGrenade(player, 0.8d);
					}
				}
			}
		}

		//Headshot
		@Override
		public void onProjectileHit(DetailedProjectileHitEvent detEvent) {
			final ProjectileHitEvent event = detEvent.projectileHitEvent;
			if (event.isCancelled()) return;
			// TODO use BlockHitResult
			final Projectile projectile = event.getEntity();
			if (!(projectile instanceof Arrow))
				return;

			final Player shooter = (Player) projectile.getShooter();
			if (event.getHitEntity() instanceof Player playerVictim) {
				final Location victimLoc = playerVictim.getLocation();
				final double minYForHeadshot = victimLoc.getY() + MIN_HEIGHT_FOR_HEADHSHOT;

				final Location projectileCentreLoc = projectile.getLocation().add(0, projectile.getHeight() / 2d, 0);

				final Vector projVelocity = projectile.getVelocity();
				final double speed = projVelocity.length(); // Distance it will travel in the next tick

				final RayTraceResult rayTrace = projectile.getWorld().rayTrace(projectileCentreLoc, projVelocity, speed,
					FluidCollisionMode.NEVER, true, projectile.getWidth(), entity -> entity != shooter && entity != projectile && !Main.getGame().isDead(entity));

				if (rayTrace != null && rayTrace.getHitEntity() != null &&
					rayTrace.getHitPosition().getY() > minYForHeadshot &&
					projectile.getOrigin().distanceSquared(projectileCentreLoc) > 5 * 5) { // Min 5 blocks away to headshot

					DamageEvent dEvent = DamageEvent.newDamageEvent(playerVictim, 999d, DamageType.SNIPER_HEADSHOT, shooter, false);
					Main.getGame().queueDamage(dEvent);

					//Hitmarker Sound effect
					shooter.playSound(shooter.getLocation(), Sound.ENTITY_ITEM_FRAME_PLACE, 2f, 2.0f);
				}
			}
		}

		@Override
		public void onReflect(ProjectileReflectEvent event) {
			final Player shooter = (Player) event.projectile.getShooter();
			event.attackFunc = dEvent -> { // All reflected sniper shots are insta kill
				dEvent.setDamageTypeCause(shooter);
				dEvent.setDamageType(REFLECTED_SNIPER);
			};
		}

		static double calcInaccuracy(Player player) {
			//Inaccuracy based on movement
			//player.getVelocity() sux so i will base movement on the player's state
			if (player.isSprinting() || player.isGliding() || player.isJumping() || !EntityUtils.isOnGround(player) ||
				player.getVelocity().lengthSquared() > 1) {
				return 0.2;
			} else if (player.isInWater() || player.isSwimming()) {
				return 0.1;
			}
			return 0;
		}

		//Sniper Rifle Shooting
		public void onPlayerDropItem(PlayerDropItemEvent event) {
			Player player = event.getPlayer();
			Item item = event.getItemDrop();
			World world = player.getWorld();
			if (item.getItemStack().getType() == Material.SPYGLASS && !player.hasCooldown(Material.SPYGLASS)) {
				double inaccuracy = calcInaccuracy(player);
				if (KitOptions.sniperAccuracy) {
					player.sendActionBar(Component.text("Inaccuracy: " + inaccuracy));
				}
				Vector velocity = player.getLocation().getDirection();
				if (inaccuracy != 0) {
					var random = new Vector(gunInaccuracy.nextGaussian(), gunInaccuracy.nextGaussian(), gunInaccuracy.nextGaussian());
					random.normalize().multiply(inaccuracy);
					velocity.add(random);
				}
				velocity.multiply(10);
				//Shooting Rifle + Arrow Properties
				Arrow arrow = player.launchProjectile(Arrow.class, velocity);
				arrow.setShotFromCrossbow(true);
				arrow.setGravity(false);
				arrow.setKnockbackStrength(1);
				arrow.setCritical(false);
				arrow.setPickupStatus(PickupStatus.DISALLOWED);
				arrow.setPierceLevel(100);
				arrow.setDamage(0.65d);
				world.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 2.5f);

				//Sniper Cooldown + deleting the dropped sniper and returning a new one.
				if (!KitOptions.sniperAccuracy) {
					player.setCooldown(Material.SPYGLASS, 50);
				}
				event.setCancelled(true);
			}
		}

		@Override
		public void onPlayerTick(Player player) {
			var uuid = player.getUniqueId();
			float exp = player.getExp();
			World world = player.getWorld();
			var inventory = player.getInventory();

			//Grenade Cooldown
			if (exp == 0.999f) {

			} else if (exp + 0.005f >= 1) {
				player.setExp(0.999f);
			} else {
				player.setExp(exp + 0.005f);
			}

			//Sniper Information message
			if (inventory.getItemInMainHand().getType() == Material.SPYGLASS) {
				Component actionBar = KitOptions.sniperAccuracy ?
					Component.text("Inaccuracy: " + calcInaccuracy(player)) :
					Component.text("Drop Spyglass in hand to shoot", SNIPER_MSG_COLOR);
				Component text = Component.text("Drop Spyglass in your main hand to shoot", SNIPER_MSG_COLOR);
				PlayerInfo pinfo = Main.getPlayerInfo(player);
				if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				//Chat Message is only sent once per life
				if (pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES) && RECEIVED_SNIPER_CHAT_MESSAGE.add(uuid)) {
					player.sendMessage(text);
				}
			}

			//Grenade Information message
			if (inventory.getItemInMainHand().getType() == Material.TURTLE_HELMET && player.getExp() == 0.999f) {

				Component actionBar = Component.text("Left/Right Click to Arm", GRENADE_MSG_COLOR);
				Component text = Component.text("Click to arm the grenade", GRENADE_MSG_COLOR);
				PlayerInfo pinfo = Main.getPlayerInfo(player);
				if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				//Chat Message is only sent once per life
				if (pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES) && RECEIVED_GRENADE_CHAT_MESSAGE.add(uuid)) {
					player.sendMessage(text);
				}
			}

			//Grenade Fail Check
			//Check if inventory has any grenades, maybe update later to allow for admin abuse grenade spam
			if (inventory.getHelmet() != null && inventory.getHelmet().getType() == Material.TURTLE_HELMET) {
				player.sendMessage(Component.text("Please do not wear the grenade on your head. Thank you.", GRENADE_MSG_COLOR));

				DamageEvent dEvent = DamageEvent.newDamageEvent(player, 999d, DamageType.EXPLOSION, null, false);
				Main.getGame().queueDamage(dEvent);

				world.createExplosion(player.getLocation(), 2.5f, false, false);
			} else if (player.getCooldown(Material.TURTLE_HELMET) == 1 && inventory.contains(Material.TURTLE_HELMET)) {
				DamageEvent dEvent = DamageEvent.newDamageEvent(player, 999d, DamageType.SNIPER_GRENADE_FAIL, null, false);
				Main.getGame().queueDamage(dEvent);

				world.createExplosion(player.getLocation(), 2.5f, false, false);
			}
			//Sniper Reload Sound
			if (player.getCooldown(Material.SPYGLASS) == 15) {
				player.playSound(player, Sound.ITEM_ARMOR_EQUIP_CHAIN, 2f, 0.8f);
			}
		}
	}
}

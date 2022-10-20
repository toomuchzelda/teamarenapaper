package me.toomuchzelda.teamarenapaper.teamarena.kits;

import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.toomuchzelda.teamarenapaper.teamarena.kits.KitBurst.BurstAbility.ROCKET_CD;

//Modified by onett425
public class KitBurst extends Kit
{
	public KitBurst() {
		super("Burst", "firework shooty shooty and rocket launcher boom", Material.FIREWORK_ROCKET);

		ItemStack[] armour = new ItemStack[4];
		armour[3] = new ItemStack(Material.CHAINMAIL_HELMET);
		armour[2] = new ItemStack(Material.IRON_CHESTPLATE);
		armour[1] = new ItemStack(Material.CHAINMAIL_LEGGINGS);
		armour[0] = new ItemStack(Material.CHAINMAIL_BOOTS);
		this.setArmour(armour);

		ItemStack sword = new ItemStack(Material.WOODEN_SWORD);

		ItemStack crossbow = new ItemStack(Material.CROSSBOW);
		ItemMeta bowMeta = crossbow.getItemMeta();
		List<Component> crossbowLore = List.of(
				Component.text("Right click to load and launch a firework rocket", TextUtils.RIGHT_CLICK_TO),
				Component.text("Left click while loaded to burst the firework right in front of you", TextUtils.LEFT_CLICK_TO)
		);
		bowMeta.lore(crossbowLore);
		//bowMeta.addEnchant(Enchantment.QUICK_CHARGE, 1, true);
		crossbow.setItemMeta(bowMeta);

		ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET, 64);

		ItemStack rocketLauncher = ItemBuilder.of(Material.FURNACE_MINECART)
				.displayName(Component.text("Rocket Launcher"))
				.lore(Component.text("Right click to fire an explosive Rocket!", TextColors.LIGHT_YELLOW),
				Component.text("Aim carefully, the blast radius is not very large...", TextColors.LIGHT_YELLOW),
						Component.text("Cooldown: " + ROCKET_CD/20 + " seconds", TextColors.LIGHT_BROWN))
				.build();

		setItems(sword, crossbow, rocketLauncher, firework);

		setAbilities(new BurstAbility());

		setCategory(KitCategory.RANGED);
	}

	public static class BurstAbility extends Ability
	{
		private static final List<ShulkerBullet> ACTIVE_ROCKETS = new ArrayList<>();
		//possible firework effects for fired fireworks
		private static final List<FireworkEffect.Type> FIREWORK_EFFECTS;
		private static final double SHOTGUN_DISTANCE = 7d;
		private static final double SHOTGUN_DIST_SQR = SHOTGUN_DISTANCE * SHOTGUN_DISTANCE;
		private static final double SHOTGUN_TARGETTING_ANGLE = Math.PI / 4d;

		public static final int ROCKET_CD = 120;
		public static final double ROCKET_BLAST_RADIUS = 2.5;
		public static final DamageType ROCKET_HURT_SELF = new DamageType(DamageType.BURST_ROCKET,
				"%Killed% was caught in their own Rocket explosion");

		static {
			FIREWORK_EFFECTS = new ArrayList<>(FireworkEffect.Type.values().length);
			FIREWORK_EFFECTS.addAll(Arrays.asList(FireworkEffect.Type.values()));
			FIREWORK_EFFECTS.remove(FireworkEffect.Type.BALL_LARGE);
			FIREWORK_EFFECTS.remove(FireworkEffect.Type.BURST);
		}

		@Override
		public void unregisterAbility() {
			ACTIVE_ROCKETS.forEach(Entity::remove);
			ACTIVE_ROCKETS.clear();
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			//Prevent damage from direct player collision with rockets
			if(event.getAttacker() instanceof ShulkerBullet) {
				Player shooter = (Player) event.getFinalAttacker();
				event.setCancelled(true);
				event.getAttacker().remove();

				shooter.playSound(shooter, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
			}
			//reduce knockback from fireworks
			else if(event.getAttacker() instanceof Firework) {
				//buff damage a bit
				event.setFinalDamage(event.getFinalDamage() * 1.6d);
				if(event.hasKnockback()) {
					event.setKnockback(event.getKnockback().multiply(0.55d));
				}
			}
		}

		@Override
		public void onShootBow(EntityShootBowEvent event) {
			if(event.getProjectile() instanceof Firework firework && event.getEntity() instanceof Player p) {
				TeamArenaTeam team = Main.getPlayerInfo(p).team;

				FireworkMeta meta = firework.getFireworkMeta();
				meta.clearEffects();
				FireworkEffect effect = FireworkEffect.builder()
						.trail(true)
						.with(FIREWORK_EFFECTS.get(MathUtils.randomMax(FIREWORK_EFFECTS.size() - 1)))
						.flicker(true)
						.withColor(team.getColour())
						.build();

				meta.addEffect(effect);
				//meta.setPower(1);
				firework.setFireworkMeta(meta);
				firework.setVelocity(firework.getVelocity().multiply(0.7));
			}
		}

		@Override
		public void onLoadCrossbow(EntityLoadCrossbowEvent event) {
			event.setConsumeItem(false);
			((Player) event.getEntity()).updateInventory(); //do this to undo client prediction of using the firework
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			final Material mat = event.getMaterial();
			final Player player = event.getPlayer();

			//stop them from accidentally placing the firework down and using it
			if(event.useItemInHand() != Event.Result.DENY) {
				if (mat == Material.FIREWORK_ROCKET) {
					event.setUseItemInHand(Event.Result.DENY);
				}
				//Firing Rocket
				else if (mat == Material.FURNACE_MINECART) {
					event.setUseItemInHand(Event.Result.DENY);
					if (event.getAction().isRightClick() && !player.hasCooldown(mat)) {
						fireRocket(player);
					}
				}
			}

			//left-clicking loaded crossbow: fire the shotgun firework
			if (mat == Material.CROSSBOW && event.getAction().isLeftClick()) {
				CrossbowMeta meta = (CrossbowMeta) event.getItem().getItemMeta();
				if(meta.hasChargedProjectiles()) {
					meta.setChargedProjectiles(null);
					event.getItem().setItemMeta(meta);

					fireShotgun(player);
				}
			}
		}

		private void fireShotgun(Player shooter) {
			//get all players within the firing angle
			Location shooterEyeLoc = shooter.getEyeLocation();
			Vector direction = shooterEyeLoc.getDirection();

			record targettedPlayer(Player target, double distance, double angle) {}
			List<targettedPlayer> targetCandidates = new ArrayList<>(Bukkit.getOnlinePlayers().size() / 5);

			//get all players caught in blast
			for (Player potentialTarget : Main.getGame().getPlayers()) {
				Location targetLoc = potentialTarget.getLocation();
				double distSqr = targetLoc.distanceSquared(shooterEyeLoc);
				if (distSqr <= SHOTGUN_DIST_SQR) {
					Vector playerToPoint = targetLoc.subtract(shooterEyeLoc).toVector().normalize();
					double angle = playerToPoint.angle(direction);

					if (angle <= SHOTGUN_TARGETTING_ANGLE) {
						targetCandidates.add(new targettedPlayer(potentialTarget, Math.sqrt(distSqr), angle));
					}
				}
			}

			for(targettedPlayer hit : targetCandidates) {
				shooter.sendMessage("angle:" + hit.angle + "\ndistance:" + hit.distance + "\nplayer:" + hit.target.getName() + "\n==========");
				//TODO: hurt hit players
			}

			//play the firework effect
			Location spawnLoc = shooterEyeLoc.toVector().add(direction).toLocation(shooter.getWorld());
			//spawnLoc.setDirection(direction); //firework needs to face this way for the effect??
			Firework fireworkEntity = shooter.getWorld().spawn(spawnLoc, Firework.class);

			//when looking straight, it kind of goes slightly higher than where the user is looking so add a slight
			// downwards component the more straight they are facing
			double y = 1d - Math.abs(direction.getY()); //0 = completely up or down, 1 = completely straight
			y = 0.16d * y;
			direction.setY(direction.getY() - y);
			direction.normalize();

			fireworkEntity.setVelocity(direction.multiply(2.5d)); //needed for firework effect direction
			//set the effect(s)
			FireworkMeta meta = fireworkEntity.getFireworkMeta();
			meta.clearEffects();
			FireworkEffect effect = FireworkEffect.builder()
					.with(FireworkEffect.Type.BURST)
					.flicker(true)
					.trail(false)
					.withColor(Main.getPlayerInfo(shooter).team.getColour())
					.withFade(Color.BLACK)
					.build();

			meta.addEffect(effect);
			fireworkEntity.setFireworkMeta(meta);

			//explode immediately
			fireworkEntity.detonate();
		}

		public void fireRocket(Player player) {
			World world = player.getWorld();
			Location eyeLoc = player.getEyeLocation();
			Vector dir = eyeLoc.getDirection();

			ShulkerBullet rocket = world.spawn(eyeLoc, ShulkerBullet.class, bullet -> {
				bullet.setShooter(player);
				bullet.setTarget(null);
				bullet.setVelocity(dir);
				bullet.setGravity(false);
			});
			//Push player back slightly upon firing, no vertical boost allowed
			player.setVelocity(player.getVelocity().subtract(dir.setY(0)));
			world.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 2.5f);

			ACTIVE_ROCKETS.add(rocket);
			player.setCooldown(Material.FURNACE_MINECART, ROCKET_CD);
		}

		@Override
		public void onTick() {
			List<ShulkerBullet> deadRockets = new ArrayList<>();

			ACTIVE_ROCKETS.forEach(rocket -> {
				int tick = rocket.getTicksLived();
				Location loc = rocket.getLocation();
				Player shooter = (Player) rocket.getShooter();
				Color teamColor = Main.getPlayerInfo(shooter).team.getColour();
				World world = rocket.getWorld();
				Particle.DustOptions particleOptions = new Particle.DustOptions(teamColor, 3);

				//Since the location extends past the block when it hits it,
				//correct location by reversing trajectory slightly
				Vector dir = rocket.getVelocity().normalize().multiply(0.1d);
				while(loc.getBlock().isSolid()) {
					loc.subtract(dir);
				}

				//Explosion on impact
				//Since shulker bullets do not despawn naturally, that is also handled here
				if(rocket.isDead() || tick > 600) {
					rocketBlast(loc, shooter);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

					rocket.remove();
					deadRockets.add(rocket);
				}
				//If rocket is travelling through the air, so create particle trail
				else if (tick % 2 == 0) {
					world.spawnParticle(Particle.REDSTONE, loc, 1,
							0, 0, 0, 5.5, particleOptions);
				}
			});

			ACTIVE_ROCKETS.removeAll(deadRockets);
		}

		public void rocketBlast(Location explodeLoc, Player owner) {
			SelfHarmingExplosion burstRocket = new SelfHarmingExplosion(explodeLoc, ROCKET_BLAST_RADIUS, 0.3,
					7.5, 0.3, 0.625, DamageType.BURST_ROCKET, owner, 1.5, 0, ROCKET_HURT_SELF);
			burstRocket.explode();
		}
	}
}

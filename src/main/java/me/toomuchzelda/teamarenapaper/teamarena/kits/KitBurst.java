package me.toomuchzelda.teamarenapaper.teamarena.kits;

import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.ArrowImpaleStatus;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

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
				Component.text("Left click while loaded to burst the firework right in front of you. Use it like a shotgun!", TextUtils.LEFT_CLICK_TO)
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

		setItems(sword, crossbow, /*rocketLauncher,*/ firework);

		setAbilities(new BurstAbility());

		setCategory(KitCategory.RANGED);
	}

	public static class BurstAbility extends Ability
	{
		private static final List<ShulkerBullet> ACTIVE_ROCKETS = new ArrayList<>();
		static final int ROCKET_CD = 120;
		private static final double ROCKET_BLAST_RADIUS = 2.5;
		private static final DamageType ROCKET_HURT_SELF = new DamageType(DamageType.BURST_ROCKET,
				"%Killed% was caught in their own Rocket explosion");

		//possible firework effects for fired fireworks
		private static final List<FireworkEffect.Type> FIREWORK_EFFECTS;

		private static final List<Arrow> SHOTGUN_ARROWS = new LinkedList<>();
		private static final Map<Component, Set<Entity>> BLAST_HIT_ENTITIES = new HashMap<>();

		//used for identifying the entity in events
		private static final Component SHOTUGUN_FIREWORK_NAME = Component.text("burstfw");
		private static final double SHOTGUN_SELF_DAMAGE = 7d;
		private static final double SHOTGUN_MAX_DAMAGE = 17d;
		private static final int SHOTGUN_ARROW_LIVE_TICKS = 17;
		public static boolean HIDE_SHOTGUN_ARROWS = true;

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

			var shotIter = SHOTGUN_ARROWS.iterator();
			while(shotIter.hasNext()) {
				shotIter.next().remove();
				shotIter.remove();
			}

			BLAST_HIT_ENTITIES.clear();
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
			else if(event.getAttacker() instanceof Firework fw) {
				//if it's damage from the firework used for shotgun visual effect
				if (SHOTUGUN_FIREWORK_NAME.equals(fw.customName())) {
					event.setCancelled(true);
				}
				else { //it's a firework rocket
					//buff damage a bit and reduce kb
					event.setFinalDamage(event.getFinalDamage() * 1.6d);
					if (event.hasKnockback()) {
						event.setKnockback(event.getKnockback().multiply(0.55d));
					}

					//if the burst is in their own explosion range un-cancel the damage
					// and make them not the final attacker, so they don't get kill credit for it
					if (event.getFinalAttacker() == event.getVictim()) {
						event.setCancelled(false);
						event.setFinalAttacker(null);
						event.setDamageType(DamageType.BURST_FIREWORK_SELF);
					}
					else {
						event.setDamageType(DamageType.BURST_FIREWORK);
					}
				}
			}
			else if (event.getAttacker() instanceof Arrow arrow) { //may be shotgun
				//this check is necessary in addition to the projectilehitevent one because of how damage events are
				// queued and processed later in the tick.
				Set<Entity> shotgunHitEntities = BLAST_HIT_ENTITIES.get(arrow.customName());
				if(shotgunHitEntities != null) { //definitely shotgun
					if(!shotgunHitEntities.add(event.getVictim())) {
						event.setCancelled(true);
					}
					else { //hit
						//calculate damage linearly on the arrow's ticks lived.
						double mult = SHOTGUN_ARROW_LIVE_TICKS - arrow.getTicksLived();
						mult /= SHOTGUN_ARROW_LIVE_TICKS;
						mult = SHOTGUN_MAX_DAMAGE * mult;

						event.setRawDamage(mult);
					}
					event.setDamageType(DamageType.BURST_SHOTGUN);
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
						.flicker(false)
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
				//Bukkit.broadcastMessage("left click for crossbow fired");
				CrossbowMeta meta = (CrossbowMeta) event.getItem().getItemMeta();
				if(meta.hasChargedProjectiles()) {
					meta.setChargedProjectiles(null);
					event.getItem().setItemMeta(meta);

					fireShotgun(player);
				}
			}
		}

		private void fireShotgun(Player shooter) {
			final Location shooterEyeLoc = shooter.getEyeLocation();
			final Vector direction = shooterEyeLoc.getDirection();

			//play the firework effect
			{
				Vector fWorkDirection = direction.clone();
				Location spawnLoc = shooterEyeLoc.toVector().add(fWorkDirection).toLocation(shooter.getWorld());
				//spawnLoc.setDirection(direction); //firework needs to face this way for the effect??
				Firework fireworkEntity = shooter.getWorld().spawn(spawnLoc, Firework.class);

				//when looking straight, it kind of goes slightly higher than where the user is looking so add a slight
				// downwards component the more straight they are facing
				double y = 1d - Math.abs(fWorkDirection.getY()); //0 = completely up or down, 1 = completely straight
				y = 0.16d * y;
				fWorkDirection.setY(fWorkDirection.getY() - y);
				fWorkDirection.normalize();

				fireworkEntity.setVelocity(fWorkDirection.multiply(2d)); //needed for firework effect direction
				//set the effect(s)
				FireworkMeta meta = fireworkEntity.getFireworkMeta();
				meta.clearEffects();
				FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BURST).flicker(false)
						.trail(false).withColor(Main.getPlayerInfo(shooter).team.getColour()).withFade(Color.BLACK)
						.build();

				meta.addEffect(effect);
				fireworkEntity.setFireworkMeta(meta);
				//make this firework identifiable in damage events so can stop it doing any damage to entities
				//fireworkEntity.customName(SHOTUGUN_FIREWORK_NAME);
				fireworkEntity.customName(SHOTUGUN_FIREWORK_NAME);
				fireworkEntity.setShooter(shooter);

				//explode immediately
				fireworkEntity.detonate();

				//play firework launch sound
				shooter.getWorld().playSound(shooter, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.PLAYERS, 2f, 0.5f);
			}

			//shoot a bunch of arrows to act like the firework sparks and do damage
			{
				//each arrow will be given a unique name like this. this is to prevent multiple arrows hitting
				// the same player consecutively, by being able to identify the shotgun blast that an arrow came from
				// in the projectile hit event.
				final Component arrowShotId = Component.text(shooter.getName() + TeamArena.getGameTick());
				BLAST_HIT_ENTITIES.put(arrowShotId, new HashSet<>(5));

				for (int i = 0; i < 40; i++) {
					Arrow arrow = shooter.getWorld().spawnArrow(shooterEyeLoc, direction, 0.8f, 25f);
					arrow.setShooter(shooter);
					arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
					arrow.setGravity(false);
					arrow.setPierceLevel(5);
					arrow.setDamage(2d);
					arrow.setSilent(true);

					arrow.customName(arrowShotId);

					if(HIDE_SHOTGUN_ARROWS) {
						for(Player viewer : Bukkit.getOnlinePlayers()) {
							viewer.hideEntity(Main.getPlugin(), arrow);
						}
					}

					//set so won't leave arrows in victims bodies.
					// Ordinarily it wouldn't since the DamageType is being replaced in the listener
					// However, if the burst fires, dies, and then hits an arrow, it will leave one because
					// the burst is no longer considered a current ability user
					ArrowImpaleStatus.setImpaling(arrow, false);

					SHOTGUN_ARROWS.add(arrow);
				}
			}

			//deal self damage to the burst
			{
				//something like a normal punch knockback but in the reverse direction of where they're looking,
				// disregarding Y
				Vector kb = direction.clone().setY(0d).normalize().multiply(0.4d).setY(0.4d).multiply(-1d);

				DamageEvent selfDmg = DamageEvent.newDamageEvent(
						shooter, SHOTGUN_SELF_DAMAGE, DamageType.BURST_SHOTGUN_SELF, null, false);
				selfDmg.setKnockback(kb);
				Main.getGame().queueDamage(selfDmg);
			}
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
			//tick rockets
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


			//tick shotgun arrows
			var shotIter = SHOTGUN_ARROWS.iterator();
			while(shotIter.hasNext()) {
				Arrow arrow = shotIter.next();
				if(arrow.getTicksLived() >= SHOTGUN_ARROW_LIVE_TICKS) {
					arrow.remove();
					shotIter.remove();
				}
				else {
					Vector newVel = arrow.getVelocity().multiply(0.96);
					arrow.setVelocity(newVel);
				}
			}
		}

		@Override
		public void onProjectileHit(ProjectileHitEvent event) {
			//all arrows shot by burst are from the shotgun
			// despawn immediately if hit a block
			if (event.getEntity() instanceof Arrow arrow) {
				if (event.getHitBlock() != null) {
					arrow.remove();
					event.setCancelled(true);
				}
				else if (event.getHitEntity() != null) {
					//see if this arrow is from a shotgun blast, and if the hit player was hit by this blast already
					Set<Entity> set = BLAST_HIT_ENTITIES.get(arrow.customName());
					//is a shotgun blast and hit an already hit victim
					if(set != null && set.contains(event.getHitEntity())) {
						event.setCancelled(true);
					}
				}
			}
		}

		public void rocketBlast(Location explodeLoc, Player owner) {
			SelfHarmingExplosion burstRocket = new SelfHarmingExplosion(explodeLoc, ROCKET_BLAST_RADIUS, 0.3,
					7.5, 0.3, 0.625, DamageType.BURST_ROCKET, owner, 1.5, 0, ROCKET_HURT_SELF);
			burstRocket.explode();
		}
	}
}

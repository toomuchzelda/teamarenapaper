package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.damage.ArrowManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DetailedProjectileHitEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.ProjectileReflectEvent;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;

public class KitPorcupine extends Kit {
	private static final ItemStack SWORD = ItemBuilder.of(Material.IRON_SWORD).build();
	private static final ItemStack SHIELD = ItemBuilder.of(Material.SHIELD)
		.displayName(Component.text("Reflector"))
		.lore(List.of(Component.text("TODO TODO TODO")))
		.build();

	public KitPorcupine() {
		super("Reflector", "\"Life is a mirror and will reflect back to the thinker what he thinks into it.\" - Ernest Holmes",
			Material.DEAD_BUSH);

		this.setArmor(
			new ItemStack(Material.IRON_HELMET),
			new ItemStack(Material.CHAINMAIL_CHESTPLATE),
			new ItemStack(Material.CHAINMAIL_LEGGINGS),
			new ItemStack(Material.IRON_BOOTS)
		);

		setItems(SWORD); //, SHIELD);

		setCategory(KitCategory.UTILITY);

		setAbilities(new PorcupineAbility());
	}

	public enum CleanupReason {
		PORC_DIED,
	}
	public interface CleanupFunc { // So other Abilities can handle cleanups
		void cleanup(Player porc, Projectile projectile, CleanupReason reason);
	}
	public interface OnHitFunc {
		void onHit(Player porc, DetailedProjectileHitEvent event);
	}
	public interface OnAttackFunc {
		void onAttack(DamageEvent event);
	}

	public static class PorcupineAbility extends Ability {
		private static final class ReflectedInfo {
			private final CleanupFunc cleanupFunc;
			private final OnHitFunc hitFunc;
			private final OnAttackFunc attackFunc;
			private boolean remove = false;

			private ReflectedInfo(CleanupFunc cleanupFunc, OnHitFunc hitFunc, OnAttackFunc attackFunc) {
				this.cleanupFunc = cleanupFunc;
				this.hitFunc = hitFunc;
				this.attackFunc = attackFunc;
			}
		}
		private final Map<Player, Map<Projectile, ReflectedInfo>> reflectedProjectiles = new HashMap<>();

		// To globally enforce 1 reflection per projectile
		private final WeakHashMap<Projectile, Void> reflectedProjLookup = new WeakHashMap<>();

		private Map<Projectile, ReflectedInfo> getProjectiles(Player porc) {
			return reflectedProjectiles.computeIfAbsent(porc, player -> new HashMap<>());
		}

		@Override
		public void unregisterAbility() {
			reflectedProjectiles.forEach((player, projectileAndFunc) -> {
				projectileAndFunc.forEach((projectile, func) -> {
					// Each ability's unregisterAbility should handle cleanup for porcs
					//if (func != null)
					//	func.accept(player, projectile);

					projectile.remove();
				});
			});

			reflectedProjLookup.clear();
		}

		@Override
		public void removeAbility(Player player) {
			var projs = reflectedProjectiles.remove(player);
			if (projs != null) {
				projs.forEach((projectile, reflectedInfo) -> {
					if (reflectedInfo.cleanupFunc != null)
						reflectedInfo.cleanupFunc.cleanup(player, projectile, CleanupReason.PORC_DIED);
					else
						projectile.remove();

					reflectedProjLookup.remove(projectile);
				});
			}
		}

		@Override
		public void onTick() {
			// Cleanup projectiles that are dead.
			reflectedProjectiles.forEach((player, projectileMap) -> {
				projectileMap.entrySet().removeIf(entry -> {
					Projectile projectile = entry.getKey();
					ReflectedInfo rinfo = entry.getValue();
					if (rinfo.remove) {
						reflectedProjLookup.remove(projectile);
						return true;
					}
					else if (!projectile.isValid()) {
						rinfo.remove = true; // Defer removal until next tick as Damage handler may need its presence
						return false;
					}

					return false;
				});
			});
		}

		@Override
		public void onProjectileHit(DetailedProjectileHitEvent event) {
			final Player porc = (Player) event.projectileHitEvent.getEntity().getShooter();
			Map<Projectile, ReflectedInfo> reflections = this.reflectedProjectiles.get(porc);
			if (reflections != null) {
				ReflectedInfo rinfo = reflections.get(event.projectileHitEvent.getEntity());
				if (rinfo != null) {
					if (rinfo.hitFunc != null)
						rinfo.hitFunc.onHit(porc, event);
				}
			}
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			// DamageType may sometimes be explosion and not projectile
			if (event.getAttacker() instanceof Projectile projectile) {
				final Player porc = (Player) event.getFinalAttacker();
				final Map<Projectile, ReflectedInfo> reflections = this.reflectedProjectiles.get(porc);
				if (reflections != null) {
					ReflectedInfo rinfo = reflections.get(projectile);
					if (rinfo != null) {
						if (rinfo.attackFunc != null)
							rinfo.attackFunc.onAttack(event);
					}
				}
			}
		}

		private static int dbgCounter = 0;
		@Override
		public void onHitByProjectile(DetailedProjectileHitEvent detailedEvent) {
			final ProjectileHitEvent projectileEvent = detailedEvent.projectileHitEvent;

			final Player porc = (Player) projectileEvent.getHitEntity();
			final Projectile projectile = projectileEvent.getEntity();

			// fishhooks are buggy
			if (projectile instanceof FishHook)
				return;

			// No projectile may be reflected twice
			if (this.reflectedProjLookup.containsKey(projectile)) return;

			final ProjectileSource projShooter = projectile.getShooter();
			if (projShooter instanceof LivingEntity lShooter &&
				!Main.getGame().canAttack(porc, lShooter)) {
				return;
			}

			final Map<Projectile, ReflectedInfo> history = getProjectiles(porc);
			assert CompileAsserts.OMIT || !history.containsKey(projectile);

			projectileEvent.setCancelled(true);

			// Inform the shooter's Ability to allow it to handle special details
			final ProjectileReflectEvent reflectEvent = new ProjectileReflectEvent(porc, projectile, projShooter);
			if (projShooter instanceof Player pShooter) {
				Kit.getAbilities(pShooter).forEach(ability -> ability.onReflect(reflectEvent));
			}
			if (reflectEvent.cancelled)
				return;

			history.put(projectile, new ReflectedInfo(reflectEvent.cleanupFunc, reflectEvent.hitFunc, reflectEvent.attackFunc));
			this.reflectedProjLookup.put(projectile, null);

			if (!(projectile instanceof EnderPearl)) // Don't take ownership of epearls
				projectile.setShooter(porc);

			Location hitLoc = projectile.getLocation();
			Vector hitPos = detailedEvent.getEntityHitResult().getHitPosition();
			if (ArrowManager.spawnArrowMarkers) {
				PacketHologram hologram = new PacketHologram(hitPos.toLocation(hitLoc.getWorld()), null, player -> true, Component.text("" + dbgCounter++));
				hologram.respawn();
			}
			hitLoc.setX(hitPos.getX());
			hitLoc.setY(hitPos.getY());
			hitLoc.setZ(hitPos.getZ());
			hitLoc.setDirection(hitLoc.getDirection().multiply(-1));
			final Vector vel = projectile.getVelocity(); // Get before teleport
			projectile.teleport(hitLoc);
			projectile.setVelocity(vel.multiply(-1d));
			projectile.setHasLeftShooter(true);

			if (projectile instanceof AbstractArrow arrow) {
				arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
			}

			if (projShooter instanceof Player pShooter) {
				pShooter.playSound(pShooter, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1f, 2f);
			}

			reflectEffect(porc, hitLoc, true);
		}

		public static void reflectEffect(Entity reflector, Location hitLoc, boolean large) {
			final World world = hitLoc.getWorld();
			world.playSound(reflector, Sound.BLOCK_NOTE_BLOCK_SNARE, 2f, 1f);

			world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 10);
			if (large) {
				Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
					world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 10);
				}, 1L);
				Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
					world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 6);
				}, 2L);
			}
		}
	}
}

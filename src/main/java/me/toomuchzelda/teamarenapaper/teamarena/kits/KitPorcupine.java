package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.damage.ArrowManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DetailedProjectileHitEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.ProjectileReflectEvent;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class KitPorcupine extends Kit {
	private static final ItemStack SWORD = ItemBuilder.of(Material.IRON_SWORD).build();
	private static final ItemStack SHIELD = ItemBuilder.of(Material.SHIELD)
		.displayName(Component.text("Reflector"))
		.lore(List.of(Component.text("TODO TODO TODO")))
		.build();

	public KitPorcupine() {
		super("Porcupine", "Ouch!", Material.DEAD_BUSH);

		this.setArmor(
			new ItemStack(Material.CHAINMAIL_HELMET),
			new ItemStack(Material.IRON_CHESTPLATE),
			new ItemStack(Material.CHAINMAIL_LEGGINGS),
			new ItemStack(Material.IRON_BOOTS)
		);

		setItems(SWORD); //, SHIELD);

		setCategory(KitCategory.UTILITY);

		setAbilities(new PorcupineAbility());
	}

	public enum CleanupReason {
		PORC_DIED,
		PROJ_DIED
	}

	public interface CleanupFunc {
		void cleanup(Player porc, Projectile projectile, CleanupReason reason);
	}

	private static class PorcupineAbility extends Ability {
		// Also keeps the cleanup func for each one
		private final Map<Player, Map<Projectile, CleanupFunc>> reflectedProjectiles = new HashMap<>();

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
		}

		@Override
		public void removeAbility(Player player) {
			var projs = reflectedProjectiles.remove(player);
			if (projs != null) {
				projs.forEach((projectile, cleanupFunc) -> {
					if (cleanupFunc != null)
						cleanupFunc.cleanup(player, projectile, CleanupReason.PORC_DIED);
				});
			}
		}

		private Map<Projectile, CleanupFunc> getProjectiles(Player porc) {
			return reflectedProjectiles.computeIfAbsent(porc, player -> new HashMap<>());
		}

		@Override
		public void onTick() {
			// Cleanup projectiles that are dead.
			reflectedProjectiles.forEach((player, projectileMap) -> {
				projectileMap.entrySet().removeIf(entry -> {
					Projectile projectile = entry.getKey();
					if (!projectile.isValid()) {
						CleanupFunc cleanupFunc = entry.getValue();
						if (cleanupFunc != null)
							cleanupFunc.cleanup(player, projectile, CleanupReason.PROJ_DIED);
						return true;
					}
					else return false;
				});
			});
		}

		private static int dbgCounter = 0;
		@Override
		public void onHitByProjectile(DetailedProjectileHitEvent detailedEvent) {
			final ProjectileHitEvent projectileEvent = detailedEvent.projectileHitEvent;

			final Player porc = (Player) projectileEvent.getHitEntity();
			final Projectile projectile = projectileEvent.getEntity();

			final ProjectileSource projShooter = projectile.getShooter();
			if (projShooter instanceof LivingEntity lShooter &&
				!Main.getGame().canAttack(porc, lShooter)) {
				return;
			}

			final Map<Projectile, CleanupFunc> history = getProjectiles(porc);
			if (history.containsKey(projectile)) return;

			projectileEvent.setCancelled(true);

			// Inform the shooter's Ability to allow it to handle special details
			ProjectileReflectEvent reflectEvent = new ProjectileReflectEvent(porc, projectile, projShooter);
			if (projShooter instanceof Player pShooter) {
				Kit.getAbilities(pShooter).forEach(ability -> ability.onReflect(reflectEvent));
			}
			if (reflectEvent.cancelled)
				return;

			history.put(projectile, reflectEvent.cleanupFunc);

			if (reflectEvent.overrideShooter)
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
			porc.getWorld().playSound(porc, Sound.BLOCK_NOTE_BLOCK_SNARE, 2f, 1f);

			porc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 10);
			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
				porc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 10);
			}, 1L);
		}

		// TODO DamageType
	}
}

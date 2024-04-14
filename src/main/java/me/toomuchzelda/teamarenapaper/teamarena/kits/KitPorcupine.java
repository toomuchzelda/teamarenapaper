package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DetailedProjectileHitEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.ProjectileReflectEvent;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

		setItems(SWORD, SHIELD);

		setCategory(KitCategory.UTILITY);

		setAbilities(new PorcupineAbility());
	}

	private static class PorcupineAbility extends Ability {
		// Also keeps the cleanup func for each one
		private final Map<Player, Map<Projectile, BiConsumer<Player, Projectile>>> reflectedProjectiles = new HashMap<>();

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
			projs.forEach((projectile, cleanupFunc) -> {
				if (cleanupFunc != null)
					cleanupFunc.accept(player, projectile);
			});
		}

		private Map<Projectile, BiConsumer<Player, Projectile>> getProjectiles(Player porc) {
			return reflectedProjectiles.computeIfAbsent(porc, player -> new HashMap<>());
		}

		@Override
		public void onTick() {
			// Cleanup projectiles that are dead.
			reflectedProjectiles.forEach((player, projectileMap) -> {
				projectileMap.entrySet().removeIf(entry -> {
					Projectile projectile = entry.getKey();
					if (!projectile.isValid()) {
						BiConsumer<Player, Projectile> cleanupFunc = entry.getValue();
						if (cleanupFunc != null)
							cleanupFunc.accept(player, projectile);
						return true;
					}
					else return false;
				});
			});
		}

		@Override
		public void onHitByProjectile(DetailedProjectileHitEvent detailedEvent) {
			// TODO adapt this handler to use detailed and then test
			//  arrows again
			final ProjectileHitEvent projectileEvent = detailedEvent.projectileHitEvent;

			final Player porc = (Player) projectileEvent.getHitEntity();
			final Projectile projectile = projectileEvent.getEntity();

			if (projectile.getShooter() instanceof LivingEntity shooter &&
				!Main.getGame().canAttack(porc, shooter)) {
				return;
			}

			final Map<Projectile, BiConsumer<Player, Projectile>> history = getProjectiles(porc);
			if (history.containsKey(projectile)) return;

			if (true || porc.isBlocking()) {
				projectileEvent.setCancelled(true);

				// Inform the shooter's Ability to allow it to handle special details
				ProjectileReflectEvent reflectEvent = new ProjectileReflectEvent(porc, projectile, projectile.getShooter());
				if (projectile.getShooter() instanceof Player pShooter) {
					Kit.getAbilities(pShooter).forEach(ability -> ability.onReflect(reflectEvent));
				}
				if (reflectEvent.cancelled)
					return;

				history.put(projectile, reflectEvent.cleanupFunc);

				/*final List<Player> viewers = EntityUtils.getTrackedPlayers(projectile);
				for (Player viewer : viewers) {
					viewer.hideEntity(Main.getPlugin(), projectile);
				}*/

				if (reflectEvent.overrideShooter)
					projectile.setShooter(porc);

				// ProjectileHitEvent doesn't give where the hit actually occured, only where it was
				// Also flip the looking direction too
				// TODO explosive use cleanUp func
				// TODO test this v
				Location hitLoc = projectile.getLocation().add(projectile.getVelocity());
				hitLoc.setDirection(hitLoc.getDirection().multiply(-1));
				projectile.teleport(hitLoc);

				projectile.setVelocity(projectile.getVelocity().multiply(-1d));
				projectile.setHasLeftShooter(false);

                if (projectile instanceof AbstractArrow arrow) {
					arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
				}

				// Re-spawn the entity for clients that predicted it disappearing
				/*for (Player viewer : viewers) {
					viewer.showEntity(Main.getPlugin(), projectile);
				}*/

				porc.getWorld().playSound(porc, Sound.BLOCK_NOTE_BLOCK_SNARE, 2f, 1f);
				// TODO vfx
			}
		}

		// TODO DamageType
	}
}

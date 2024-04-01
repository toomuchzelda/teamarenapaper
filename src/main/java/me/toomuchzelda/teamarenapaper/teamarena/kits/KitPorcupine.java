package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

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
		@Override
		public void onHitByProjectile(ProjectileHitEvent event) {
			final Player porc = (Player) event.getHitEntity();
			if (true || porc.isBlocking()) {
				event.setCancelled(true);
				final Projectile projectile = event.getEntity();
				projectile.setVelocity(projectile.getVelocity().multiply(-1d));
				projectile.setHasLeftShooter(true);
				projectile.setShooter(porc);

				Vector dir = projectile.getLocation().getDirection().multiply(-1d);
				Location loc = projectile.getLocation().setDirection(dir);

				net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) projectile).getHandle();
				nmsEntity.setXRot(loc.getPitch());
				nmsEntity.setYRot(loc.getYaw());

				if (projectile instanceof AbstractArrow arrow) {
					arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
				}

				// Re-spawn the entity for clients that predicted it disappearing
				for (Player viewer : projectile.getTrackedPlayers()) {
					viewer.hideEntity(Main.getPlugin(), projectile);
					viewer.showEntity(Main.getPlugin(), projectile);
				}

				porc.getWorld().playSound(porc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1f);
			}
		}

		// TODO DamageType
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.damage;

import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftLivingEntity;
import org.bukkit.entity.*;

import javax.annotation.Nullable;

public class DamageSourceCreator {

    public static DamageSource getMelee(LivingEntity damager) {
        net.minecraft.world.entity.LivingEntity nmsLiving = ((CraftLivingEntity) damager).getHandle();
        if (damager instanceof Player) {
            return DamageSource.playerAttack((net.minecraft.world.entity.player.Player) nmsLiving);
        } else if (damager instanceof Bee) {
            return DamageSource.sting(nmsLiving);
        } else {
            return DamageSource.mobAttack(nmsLiving);
        }
    }

    public static DamageSource getProjectile(Entity projectile, @Nullable LivingEntity shooter) {
        net.minecraft.world.entity.Entity nmsProjectile = ((CraftEntity) projectile).getHandle();
        net.minecraft.world.entity.Entity nmsEntity;
        if (shooter != null)
            nmsEntity = ((CraftEntity) shooter).getHandle();
        else
            nmsEntity = null;

        if (projectile instanceof AbstractArrow) {
            if (projectile instanceof Trident) {
                return DamageSource.trident(nmsProjectile, nmsEntity);
            } else {
                net.minecraft.world.entity.projectile.AbstractArrow nmsAA = (net.minecraft.world.entity.projectile.AbstractArrow) nmsProjectile;
                return DamageSource.arrow(nmsAA, nmsEntity);
            }
        } else {
            return DamageSource.thrown(nmsProjectile, nmsEntity);
        }
    }

	public static DamageSource getExplosion(Entity explodingEntity, Entity explosionSource) {
		net.minecraft.world.entity.Entity nmsExploder = explodingEntity == null ? null : ((CraftEntity) explodingEntity).getHandle();
		net.minecraft.world.entity.Entity nmsSource = explosionSource == null ? null : ((CraftEntity) explosionSource).getHandle();
		return DamageSource.explosion(nmsExploder, nmsSource);
	}

	private static boolean isAnvil(Material material) {
		return material == Material.ANVIL || material == Material.CHIPPED_ANVIL || material == Material.DAMAGED_ANVIL;
	}

	public static DamageSource getFallingBlock(Entity block) {
		if (block instanceof FallingBlock blockEntity) {
			Material mat = blockEntity.getBlockData().getMaterial();
			if (mat == Material.POINTED_DRIPSTONE)
				return DamageSource.fallingStalactite(((CraftEntity) blockEntity).getHandle());
			else if (isAnvil(mat))
				return DamageSource.anvil(((CraftEntity) blockEntity).getHandle());
			else
				return DamageSource.fallingBlock(((CraftEntity) block).getHandle());
		}
		else {
			return DamageSource.fallingBlock(((CraftEntity) block).getHandle());
		}
	}
}

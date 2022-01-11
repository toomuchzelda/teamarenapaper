package me.toomuchzelda.teamarenapaper.teamarena.damage;

import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.*;

import javax.annotation.Nullable;

public class DamageSourceCreator {

    public static DamageSource getMelee(LivingEntity damager) {
        if(damager instanceof Player) {
            net.minecraft.world.entity.player.Player nmsPlayer = ((CraftPlayer) damager).getHandle();
            return DamageSource.playerAttack(nmsPlayer);
        }
        else if(damager instanceof Bee) {
            net.minecraft.world.entity.LivingEntity nmsLiving = ((CraftLivingEntity) damager).getHandle();
            return DamageSource.sting(nmsLiving);
        }
        else {
            net.minecraft.world.entity.LivingEntity nmsLiving = ((CraftLivingEntity) damager).getHandle();
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

        if(projectile instanceof AbstractArrow aa) {
            if(projectile instanceof Trident trident) {
                return DamageSource.trident(nmsProjectile, nmsEntity);
            }
            else {
                net.minecraft.world.entity.projectile.AbstractArrow nmsAA = (net.minecraft.world.entity.projectile.AbstractArrow) nmsProjectile;
                return DamageSource.arrow(nmsAA, nmsEntity);
            }
        }
        else {
            return DamageSource.thrown(nmsProjectile, nmsEntity);
        }
    }
}

package me.toomuchzelda.teamarenapaper.teamarena.damage;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;

public class DamageNumbers
{
	private static final double[] MATERIAL_BASE_DAMAGE;
	private static final double DAMAGE_PER_STRENGTH_LEVEL = 3d;
	private static final double DAMAGE_PER_WEAKNESS_LEVEL = 4d;

	static {
		MATERIAL_BASE_DAMAGE = new double[Material.values().length];

		Arrays.fill(MATERIAL_BASE_DAMAGE, 1d);

		setBaseDamage(Material.WOODEN_SWORD, 4d);
		setBaseDamage(Material.GOLDEN_SWORD, 4d);
		setBaseDamage(Material.STONE_SWORD, 5d);
		setBaseDamage(Material.IRON_SWORD, 6d);
		setBaseDamage(Material.DIAMOND_SWORD, 7d);
		setBaseDamage(Material.NETHERITE_SWORD, 8d);

		setBaseDamage(Material.WOODEN_AXE, 7d);
		setBaseDamage(Material.GOLDEN_AXE, 7d);
		setBaseDamage(Material.STONE_AXE, 9d);
		setBaseDamage(Material.IRON_AXE, 9d);
		setBaseDamage(Material.DIAMOND_AXE, 9d);
		setBaseDamage(Material.NETHERITE_AXE, 10d);

		setBaseDamage(Material.WOODEN_PICKAXE, 2d);
		setBaseDamage(Material.GOLDEN_PICKAXE, 2d);
		setBaseDamage(Material.STONE_PICKAXE, 3d);
		setBaseDamage(Material.IRON_PICKAXE, 4d);
		setBaseDamage(Material.DIAMOND_PICKAXE, 5d);
		setBaseDamage(Material.NETHERITE_PICKAXE, 6d);

		setBaseDamage(Material.WOODEN_SHOVEL, 2.5d);
		setBaseDamage(Material.GOLDEN_SHOVEL, 2.5d);
		setBaseDamage(Material.STONE_SHOVEL, 3.5d);
		setBaseDamage(Material.IRON_SHOVEL, 4.5d);
		setBaseDamage(Material.DIAMOND_SHOVEL, 5.5d);
		setBaseDamage(Material.NETHERITE_SHOVEL, 6.5d);

		setBaseDamage(Material.TRIDENT, 9d);
	}

	private static void setBaseDamage(Material mat, double d) {
		MATERIAL_BASE_DAMAGE[mat.ordinal()] = d;
	}

	public static double getDamageForPotionEffect(PotionEffect effect) {
		double damage = 0d;
		if(effect.getType() == PotionEffectType.INCREASE_DAMAGE) {
			damage = DAMAGE_PER_STRENGTH_LEVEL;
		}
		else if(effect.getType() == PotionEffectType.WEAKNESS) {
			damage = DAMAGE_PER_WEAKNESS_LEVEL;
		}

		return damage * effect.getAmplifier();
	}

	public static double getCritDamage(double d) {
		return d * 1.5d;
	}

	public static double getEnchantmentDamage(Enchantment enchantment, int levels, LivingEntity victim) {
		return (double) enchantment.getDamageIncrease(levels, victim.getCategory());
	}

	public static double getMaterialBaseDamage(Material mat) {
		return MATERIAL_BASE_DAMAGE[mat.ordinal()];
	}
}

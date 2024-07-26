package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DamageNumbers
{
	private static final double[] MATERIAL_BASE_DAMAGE;// base amount of damage an item deals
	private static final double[] ARMOR_BASE_PROTECTION;// armour defense points, amount of armor bar it fills up
	private static final Map<Enchantment, Double> ENCHANTMENT_VALUES; //Enchantment Protection Factor per level
	private static final double DAMAGE_PER_STRENGTH_LEVEL = 3d;
	private static final double DAMAGE_PER_WEAKNESS_LEVEL = -4d;

	static {
		//***** ITEM DAMAGE *****
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


		//***** ARMOR DEFENSE POINTS
		ARMOR_BASE_PROTECTION = new double[Material.values().length];
		Arrays.fill(ARMOR_BASE_PROTECTION, 0d);

		setDefensePoints(Material.TURTLE_HELMET, 2);

		setDefensePoints(Material.LEATHER_HELMET, 1d);
		setDefensePoints(Material.LEATHER_CHESTPLATE, 3d);
		setDefensePoints(Material.LEATHER_LEGGINGS, 2d);
		setDefensePoints(Material.LEATHER_BOOTS, 1d);

		setDefensePoints(Material.GOLDEN_HELMET, 2d);
		setDefensePoints(Material.GOLDEN_CHESTPLATE, 5d);
		setDefensePoints(Material.GOLDEN_LEGGINGS, 3d);
		setDefensePoints(Material.GOLDEN_BOOTS, 1d);

		setDefensePoints(Material.CHAINMAIL_HELMET, 2d);
		setDefensePoints(Material.CHAINMAIL_CHESTPLATE, 5d);
		setDefensePoints(Material.CHAINMAIL_LEGGINGS, 4d);
		setDefensePoints(Material.CHAINMAIL_BOOTS, 1d);

		setDefensePoints(Material.IRON_HELMET, 2d);
		setDefensePoints(Material.IRON_CHESTPLATE, 6d);
		setDefensePoints(Material.IRON_LEGGINGS, 5d);
		setDefensePoints(Material.IRON_BOOTS, 2d);

		setDefensePoints(Material.DIAMOND_HELMET, 3d);
		setDefensePoints(Material.DIAMOND_CHESTPLATE, 8d);
		setDefensePoints(Material.DIAMOND_LEGGINGS, 6d);
		setDefensePoints(Material.DIAMOND_BOOTS, 3d);

		setDefensePoints(Material.NETHERITE_HELMET, 3d);
		setDefensePoints(Material.NETHERITE_CHESTPLATE, 8d);
		setDefensePoints(Material.NETHERITE_LEGGINGS, 6d);
		setDefensePoints(Material.NETHERITE_BOOTS, 3d);


		//******ENCHANTMENTS
		Enchantment[] enchs = Enchantment.values();
		ENCHANTMENT_VALUES = new HashMap<>(enchs.length, 0.5f);
		ENCHANTMENT_VALUES.put(Enchantment.PROTECTION, 1d);
		ENCHANTMENT_VALUES.put(Enchantment.FIRE_PROTECTION, 2d);
		ENCHANTMENT_VALUES.put(Enchantment.BLAST_PROTECTION, 2d);
		ENCHANTMENT_VALUES.put(Enchantment.PROJECTILE_PROTECTION, 2d);
		ENCHANTMENT_VALUES.put(Enchantment.FEATHER_FALLING, 3d);
	}

	private static void setBaseDamage(Material mat, double d) {
		MATERIAL_BASE_DAMAGE[mat.ordinal()] = d;
	}

	private static void setDefensePoints(Material mat, double d) {
		ARMOR_BASE_PROTECTION[mat.ordinal()] = d;
	}

	public static double getPotionEffectDamage(PotionEffect effect) {
		double damage = 0d;
		if(effect.getType() == PotionEffectType.STRENGTH) {
			damage = DAMAGE_PER_STRENGTH_LEVEL;
		}
		else if(effect.getType() == PotionEffectType.WEAKNESS) {
			damage = DAMAGE_PER_WEAKNESS_LEVEL;
		}

		return damage * effect.getAmplifier();
	}

	public static double getMaterialBaseDamage(Material mat) {
		return MATERIAL_BASE_DAMAGE[mat.ordinal()];
	}

	public static double getCritDamage(double d) {
		return d * 1.5d;
	}

	public static double getBaseDefensePoints(Material mat) {
		return ARMOR_BASE_PROTECTION[mat.ordinal()];
	}

	public static double getEnchantDefensePoints(Enchantment enchantment, int levels) {
		return Math.min(20d, ENCHANTMENT_VALUES.get(enchantment) * levels);
	}

	/**
	 * amount of Enchantment Protection Factor to take effect for this DamageType. refer to mc wiki.
	 * capped at 20
	 */
	public static double getEnchantedDefensePointsForDamageType(DamageType type, Map<Enchantment, Integer> enchantments) {
		double points = 0;

		int levels;
		for(Enchantment ench : type.getApplicableEnchantments()) {
			levels = enchantments.getOrDefault(ench, 0);
			points += getEnchantDefensePoints(ench, levels);
		}

		return Math.min(20d, points);
	}

	/**
	 * convert from EPF to percent of damage blocked, maxed at 80%. Percent format is in range 0 to 0.8
	 */
	public static double getEnchantedPercentBlockedForDamageType(DamageType type, Map<Enchantment, Integer> enchantments) {
		return getEnchantedDefensePointsForDamageType(type, enchantments) / 25d;
	}

	public static double calcArrowDamage(double arrowDamage, double arrowSpeed) {
		return MathUtils.clamp(0, 2.147483647E9d, arrowDamage * arrowSpeed);
	}
}

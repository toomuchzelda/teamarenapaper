package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class DamageCalculator
{
	/**
	 * Calculate the damage a LivingEntity would do to an Entity victim in a regular melee attack,
	 * considering all enchantments, armour etc.
	 * @return a double[] with values:
	 * 0: Item base damage.
	 * 1: Item enchantment damage.
	 * 2: Damage dealt on the victim after considering armour.
	 */
	public static double[] calcItemDamageOnEntity(LivingEntity attacker, ItemStack weapon, DamageType damageType,
														boolean critical, Entity victim) {
		double[] results = new double[3];

		//recalculate the damage done by the item
		double itemDamage;
		if(weapon.getType().isAir()) // If not item held, get the mob's own attack damage
		{
			AttributeInstance attribute = attacker.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
			if (attribute != null)
				itemDamage = attribute.getValue();
			else {
				Main.logger().warning(attacker + " " + attacker.getName() +
					" does not have GENERIC_ATTACK_DAMAGE attribute for weapon=" + weapon + ", damageType=" +
					damageType + ", critical=" + critical+ ", victim=" + victim.getName()
				);
				Thread.dumpStack();

				itemDamage = 1d; // not sure
			}
		} else
			itemDamage = DamageNumbers.getMaterialBaseDamage(weapon.getType());
		//add damage from potion effects (strength and weakness)
		for(PotionEffect potEffect : attacker.getActivePotionEffects()) {
			itemDamage += DamageNumbers.getPotionEffectDamage(potEffect);
		}
		//crit
		if(critical) {
			itemDamage = DamageNumbers.getCritDamage(itemDamage);
		}

		results[0] = itemDamage;

		//add enchantments
		if(victim instanceof LivingEntity livingVictim) {
			double enchDamage = DamageCalculator.calcItemEnchantDamage(attacker.getWorld(), weapon, livingVictim,
				damageType, itemDamage);
			results[1] = enchDamage;
			itemDamage += enchDamage;

			//do armor calc on victim
			results[2] = DamageCalculator.calcArmorReducedDamage(damageType, itemDamage, livingVictim);
		}
		else {
			results[2] = itemDamage;
		}

		if(damageType.is(DamageType.SWEEP_ATTACK)) {
			double sweepLevels = weapon.getEnchantmentLevel(Enchantment.SWEEPING_EDGE);
			//1 + Attack_Damage × (Sweeping_Edge_Level / (Sweeping_Edge_Level + 1));
			results[2] = 1d + (results[2] * (sweepLevels / (sweepLevels + 1d)));
			//set the weapon base damage to half a heart?
			// sweep attacks do half a heart if there is no sweeping edge enchantment
			// so i suppose item base damage should be 1, and enchantment damage should be the damage added by
			// sweeping edge, if any.
			results[1] = Math.max(0d, results[2] - 1d);
			results[0] = 1d;
		}

		return results;
	}

	/**
	 * Percentage of damage to be blocked by the victim's armor, ignoring enchantments, for this DamageType
	 * @return percent in range 0.0 to 1.0
	 */
	public static double calcBlockedDamagePercent(DamageType type, LivingEntity victim) {
		double percentBlocked = 0d;
		if(!type.isIgnoreArmor()) {
			double armorPoints = 0d;
			ItemStack[] armor = getArmor(victim);

			for(ItemStack armorPiece : armor) {
				if(armorPiece != null)
					armorPoints += DamageNumbers.getBaseDefensePoints(armorPiece.getType());
			}
			armorPoints = Math.min(20d, armorPoints);
			percentBlocked = armorPoints / 25d;
		}

		return percentBlocked;
	}

	/**
	 * amount of enchanted defense points activated by this damage type on this
	 * living entity with the armor it's currently wearing
	 * @return enchanted defense points, maxed at 20
	 */
	public static double calcEnchantDefensePointsForDamageTypeOnLivingEntity(DamageType type, LivingEntity victim) {
		double points = 0;
		ItemStack[] armor = getArmor(victim);

		for(ItemStack armorPiece : armor) {
			if(armorPiece != null)
				points += DamageNumbers.getEnchantedDefensePointsForDamageType(type, armorPiece.getEnchantments());
		}

		return Math.min(20d, points);
	}

	/**
	 * Percent of damage blocked on this damage type by enchantments on the living entity's armour
	 */
	public static double calcEnchantDefensePercentForDamageTypeOnLivingEntity(DamageType type, LivingEntity victim) {
		return calcEnchantDefensePointsForDamageTypeOnLivingEntity(type, victim) / 25d;
	}

	public static double calcItemEnchantDamage(World world, ItemStack item, LivingEntity victim, DamageType type, double base) {
		double d = EnchantmentHelper.modifyDamage(
			((CraftWorld) world).getHandle(),
			CraftItemStack.asNMSCopy(item),
			((CraftEntity) victim).getHandle(),
			type.getDamageSource(),
			(float) base
		);

		d = Math.max(0d, d - base); // modify damage returns itemBase + enchant

		/*for(Map.Entry<Enchantment, Integer> ench : item.getEnchantments().entrySet()) {
			d += DamageNumbers.getEnchantmentDamage(ench.getKey(), ench.getValue(), victim);
		}*/

		return d;
	}

	/**
	 * Get the amount of damage done to a LivingEntity for a given raw damage and DamageType, considering their armour
	 * and armour enchantments.
	 */
	public static double calcArmorReducedDamage(DamageType damageType, double damage, LivingEntity victim) {
		//get the amount of damage blocked by their base armor (armor bars above their hotbar)
		double percentBaseBlocked = 1d - DamageCalculator.calcBlockedDamagePercent(damageType, victim);
		damage = damage * percentBaseBlocked;

		//get reduction by enchantments
		double percentEnchBlocked = 1d - DamageCalculator.calcEnchantDefensePercentForDamageTypeOnLivingEntity(
				damageType, victim);

		damage *= percentEnchBlocked;

		return damage;
	}

	private static ItemStack[] getArmor(LivingEntity living) {
		ItemStack[] armor;
		if(living.getEquipment() != null)
			armor = living.getEquipment().getArmorContents();
		else
			armor = new ItemStack[0];

		return armor;
	}
}

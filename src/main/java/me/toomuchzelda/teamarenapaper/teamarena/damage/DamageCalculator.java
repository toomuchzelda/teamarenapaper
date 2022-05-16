package me.toomuchzelda.teamarenapaper.teamarena.damage;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DamageCalculator
{
	/**
	 * Percentage of damage to be blocked by the victim's armor, ignoring enchantments, for this DamageType
	 * @return percent in range 0.0 to 1.0
	 */
	public static double calcBlockedDamagePercent(DamageType type, double damage, LivingEntity victim) {
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


	//PlayerInventory.getArmorContents doesn't create copies of ItemStacks, probably faster than
	// EntityEquipment.getArmorContents
	private static ItemStack[] getArmor(LivingEntity living) {
		ItemStack[] armor = null;
		if(living instanceof Player p)
			armor = p.getInventory().getArmorContents();
		else if(living.getEquipment() != null)
			armor = living.getEquipment().getArmorContents();

		if(armor == null)
			armor = new ItemStack[0];

		return armor;
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.damage;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class DamageCalculator
{
	/**
	 * Percentage of damage to be blocked by the victim's armor, ignoring enchantments, for this DamageType
	 * @return percent in range 0.0 to 1.0
	 */
	public static double getDamageBlockedPercent(DamageType type, double damage, LivingEntity victim) {
		double percentBlocked = 0d;
		if(!type.isIgnoreArmor()) {
			double armorPoints = 0d;
			ItemStack[] armor;
			//PlayerInventory.getArmorContents doesn't create copies of ItemStacks, probably faster than
			// EntityEquipment.getArmorContents
			if(victim instanceof Player p) {
				armor = p.getInventory().getArmorContents();
			}
			else if(victim.getEquipment() != null){
				armor = victim.getEquipment().getArmorContents();
			}
			else {
				armor = new ItemStack[0];
			}

			for(ItemStack armorPiece : armor) {
				armorPoints += DamageNumbers.getDefensePoints(armorPiece.getType());
			}
			armorPoints = Math.min(20d, armorPoints);
			percentBlocked = armorPoints / 25d;
		}

		return percentBlocked;
	}
}

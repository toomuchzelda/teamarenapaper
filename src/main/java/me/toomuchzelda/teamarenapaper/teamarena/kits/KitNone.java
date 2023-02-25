package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.Damageable;

public class KitNone extends Kit {

	public KitNone()
	{
		super("None", "You get nothing except 1 random item.", Material.COARSE_DIRT);

		setCategory(KitCategory.FIGHTER);
		setAbilities(new NoneAbility());
	}

	private static class NoneAbility extends Ability
	{
		@Override
		public void giveAbility(Player player) {
			Material chosenMat;
			do {
				chosenMat = MathUtils.randomElement(Material.values());
			}
			while (!chosenMat.isItem());

			ItemBuilder builder = ItemBuilder.of(chosenMat);

			for (Enchantment ench : Enchantment.values()) {
				if (MathUtils.random.nextDouble() <= 0.04) {
					builder.enchant(ench, MathUtils.randomMax(ench.getMaxLevel() * 2));
				}
			}

			player.getInventory().addItem(builder.build());
		}
	}
}

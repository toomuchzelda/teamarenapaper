package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.SoundUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class KitNone extends Kit {

	public KitNone()
	{
		super("None", "You get nothing except 1 random item.", Material.COARSE_DIRT);

		setCategory(KitCategory.FIGHTER);
		setAbilities(new NoneAbility());
	}

	private static class NoneAbility extends Ability
	{
		private static final Material[] ALL_MATS = Material.values();

		@Override
		public void giveAbility(Player player) {
			Material chosenMat;

			do {
				chosenMat = MathUtils.randomElement(ALL_MATS);
			}
			while (!chosenMat.isItem() || chosenMat.isAir());

			ItemBuilder builder = ItemBuilder.of(chosenMat);

			for (Enchantment ench : Enchantment.values()) {
				if (MathUtils.random.nextDouble() <= 0.04) {
					builder.enchant(ench, MathUtils.randomMax(ench.getMaxLevel() * MathUtils.randomRange(2, 4)));
				}
			}

			ItemStack item = builder.build();
			Map<Enchantment, Integer> ench = item.getEnchantments();
			if (ench.containsKey(Enchantment.FEATHER_FALLING) ||
				ench.containsKey(Enchantment.DEPTH_STRIDER) ||
				ench.containsKey(Enchantment.SWIFT_SNEAK) ||
				ench.containsKey(Enchantment.FROST_WALKER)) {

				player.getEquipment().setBoots(item);
			}
			else if (ench.containsKey(Enchantment.PROTECTION) ||
				ench.containsKey(Enchantment.BLAST_PROTECTION) ||
				ench.containsKey(Enchantment.PROJECTILE_PROTECTION) ||
				ench.containsKey(Enchantment.FIRE_PROTECTION)) {

				player.getEquipment().setHelmet(item);
			}
			else {
				player.getInventory().addItem(item);
			}
		}

		/** User takes no damage from Kit Sniper */
		@Override
		public void onAttemptedDamage(DamageEvent event) {
			Entity finalAttacker = event.getFinalAttacker();
			if (finalAttacker instanceof Player playerAttacker) {
				Kit kit = Kit.getActiveKit(playerAttacker);
				if (kit instanceof KitSniper) {
					event.setCancelled(true);
					if (event.getDamageType().is(DamageType.SNIPER_HEADSHOT)) {
						PlayerUtils.sendTitle(playerAttacker, Component.empty(), Component.text("L bozo aim better"), 0, 3 * 20, 3 * 20);
						playerAttacker.playSound(playerAttacker, SoundUtils.getRandomObnoxiousSound(), 2f, 2f);
					}
				}
			}
		}
	}
}

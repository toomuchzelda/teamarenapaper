package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.CommonAbilityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

public class KitMarine extends Kit {
	private static final Color CHESTPLATE_COLOR = Color.fromRGB(0x2dc4c4);
	private static final TextColor TEXT_COLOR = TextColor.color(0xEBAD6F);

	public KitMarine(CommonAbilityManager commonAbilityManager) {
		super("Fish", "🐟🐠🐡🦈\n\nIn Loving Memory of T_0_E_D", ItemBuilder.of(Material.COD).enchantmentGlint(true).build());
		setArmor(
			ItemBuilder.of(Material.TURTLE_HELMET)
				.name(Component.text("Diving Helmet", TEXT_COLOR))
				.build(),
			ItemBuilder.of(Material.LEATHER_CHESTPLATE)
				.name(Component.text("Diving Chestplate", TEXT_COLOR))
				.color(CHESTPLATE_COLOR)
				.build(),
			ItemBuilder.of(Material.LEATHER_LEGGINGS)
				.name(Component.text("Diving Leggings", TEXT_COLOR))
				.color(Color.BLACK)
				.build(),
			ItemBuilder.of(Material.LEATHER_BOOTS)
				.name(Component.text("Diving Boots", TEXT_COLOR))
				.color(Color.BLACK)
				.enchant(Enchantment.FEATHER_FALLING, 2)
				.build()
		);
		// trident item is given by the ability
		setAbilities(commonAbilityManager.riptide);
		setCategory(KitCategory.FIGHTER);
	}
}

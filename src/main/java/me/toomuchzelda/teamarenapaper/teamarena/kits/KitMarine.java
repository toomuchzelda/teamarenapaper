package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.CommonAbilityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class KitMarine extends Kit {
	private static final TextColor STUPID_BIRD_COLOR = TextColor.color(0xfce9a4);
	public static final Component T_0_E_D = Component.text("T_0_E_D", Style.style(STUPID_BIRD_COLOR, ShadowColor.shadowColor(STUPID_BIRD_COLOR, 128)));

	private static final Color CHESTPLATE_COLOR = Color.fromRGB(0x2dc4c4);
	private static final TextColor TEXT_COLOR = TextColor.color(0xEBAD6F);

	private static final Style STYLE = DESC_STYLE;
	private static final Style STYLE_ST = DESC_STYLE.decorate(TextDecoration.STRIKETHROUGH);
	private static final List<Component> DESC = List.of(
		Component.text("Sure, here's a description of the Marine Kit:", STYLE_ST),
		Component.text("🐟🐠🐡🦈", STYLE_ST),
		Component.text("You are a fish. You are a fish. You are a fish.", STYLE),
		Component.text("You are going on an amphibious assault mission.", STYLE),
		Component.text("Impale your enemy with your trident.", STYLE),
		Component.text("Ride the waves with your riptide ability.", STYLE),
		Component.empty(),
		Component.text("In Loving Memory of", NamedTextColor.WHITE),
		T_0_E_D
	);

	public KitMarine(CommonAbilityManager commonAbilityManager) {
		super("fish", "Fish", DESC, new ItemStack(Material.COD));
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
				.enchant(Enchantment.DEPTH_STRIDER, 1)
				.build()
		);
		// trident item is given by the ability
		setAbilities(commonAbilityManager.riptide);
		setCategory(KitCategory.FIGHTER);
	}
}

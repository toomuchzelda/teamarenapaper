package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.RiptideAbility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.List;

public class KitMarine extends Kit {
	private static final TextColor STUPID_BIRD_COLOR = TextColor.color(0xfce9a4);
	public static final Component T_0_E_D = Component.text("T_0_E_D", Style.style(STUPID_BIRD_COLOR, ShadowColor.shadowColor(STUPID_BIRD_COLOR, 128)));

	private static final TextColor TEXT_COLOR = TextColor.color(0xEBAD6F);

	private static final Style STYLE = DESC_STYLE;
	private static final List<Component> DESC = List.of(
		Component.text("You are a fish. You are a fish. You are a fish.", STYLE),
		Component.text("You are going on an amphibious assault mission.", STYLE),
		Component.text("Impale your enemy with your trident.", STYLE),
		Component.text("Ride the waves with your riptide ability.", STYLE),
		Component.empty(),
		Component.text("In Loving Memory of", NamedTextColor.WHITE),
		Component.textOfChildren(Component.text(" ".repeat(6)), T_0_E_D),
		Component.empty()
	);

	public KitMarine() {
		super("fish", "Fish", DESC, new ItemStack(Material.COD));
		setArmor(
			ItemBuilder.of(Material.TURTLE_HELMET)
				.name(Component.text("Diving Helmet", TEXT_COLOR))
				.trim(TrimMaterial.LAPIS, TrimPattern.TIDE)
				.build(),
			ItemBuilder.of(Material.LEATHER_CHESTPLATE)
				.name(Component.text("Diving Chestplate", TEXT_COLOR))
				.color(DyeColor.GRAY.getColor())
				.trim(TrimMaterial.LAPIS, TrimPattern.SILENCE)
				.build(),
			ItemBuilder.of(Material.LEATHER_LEGGINGS)
				.name(Component.text("Diving Leggings", TEXT_COLOR))
				.color(Color.BLACK)
				.trim(TrimMaterial.LAPIS, TrimPattern.SILENCE)
				.build(),
			ItemBuilder.of(Material.LEATHER_BOOTS)
				.name(Component.text("Tidal Boots", TEXT_COLOR))
				.color(Color.BLACK)
				.trim(TrimMaterial.LAPIS, TrimPattern.FLOW)
				.enchantmentGlint(false)
				.meta(meta -> {
					// Depth Strider 1.5
					meta.addAttributeModifier(Attribute.WATER_MOVEMENT_EFFICIENCY,
						new AttributeModifier(new NamespacedKey(Main.getPlugin(), "kits/fish/tidal_boots/depth_strider"),
						0.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
					// Feather Falling 4.17
					meta.addAttributeModifier(Attribute.FALL_DAMAGE_MULTIPLIER,
						new AttributeModifier(new NamespacedKey(Main.getPlugin(), "kits/fish/tidal_boots/feather_falling"),
							-0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.FEET));
				})
				.build()
		);
		// trident item is given by the ability
		setAbilities(new RiptideAbility());
		setCategory(KitCategory.FIGHTER);
	}
}

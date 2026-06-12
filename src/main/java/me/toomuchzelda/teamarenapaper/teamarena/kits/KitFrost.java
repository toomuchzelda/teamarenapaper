package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class KitFrost extends Kit {

	private static final NamespacedKey SWORD_KEY = new NamespacedKey(Main.getPlugin(), "frost_sword");

	public KitFrost() {
		super("frost", "Frost", "kit frost", new ItemStack(Material.ICE));

		final Color color = Color.WHITE.mixColors(Color.AQUA);
		final ItemStack[] armour = {
			ItemBuilder.of(Material.LEATHER_BOOTS)
				.color(color)
				.trim(TrimMaterial.DIAMOND, TrimPattern.SENTRY)
				.build(),
			ItemBuilder.of(Material.LEATHER_LEGGINGS)
				.color(color)
				.trim(TrimMaterial.DIAMOND, TrimPattern.SENTRY)
				.build(),
			ItemBuilder.of(Material.LEATHER_CHESTPLATE)
				.color(color)
				.trim(TrimMaterial.DIAMOND, TrimPattern.COAST)
				.build(),
			ItemBuilder.of(Material.LEATHER_HELMET)
				.color(color)
				.trim(TrimMaterial.DIAMOND, TrimPattern.SENTRY)
				.build()
		};
		this.setArmour(armour);

		final ItemStack sword = ItemBuilder.of(Material.IRON_SWORD)
			.name(Component.text("Icey sword", NamedTextColor.AQUA))
			.setPDC(SWORD_KEY, PersistentDataType.BOOLEAN, true)
			.build();
		this.setItems(sword);

		this.setCategory(KitCategory.UTILITY);

		this.setAbilities(new FrostAbility());
	}

	private static class FrostAbility extends Ability {

	}
}

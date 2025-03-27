package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.CritAbility;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.Arrays;
import java.util.List;

public class KitSplitter extends Kit {
	private static final Material SWORD_TYPE = Material.STONE_SWORD;

	public KitSplitter(TeamArena game, KitTrooper trooper) {
		super(trooper, "Splitter" ,
			"My cursed technique allows me to forcibly create a weak point on any opponent.\n\n" +
				"One to one. Striking the point at that ratio when you draw a line along the target's " +
				"height causes a critical hit.\n\nThat allows me to deal a fair amount of damage to those " +
				"ranked higher than me, and if their cursed energy is weak, I can cut them in two, even " +
				"with a blunt blade.",
			new ItemStack(SWORD_TYPE));

		// blonde hair with 'glasses'
		/*ItemStack head = ItemBuilder.of(Material.GOLDEN_HELMET)
			.armourTrim(new ArmorTrim(
				TrimMaterial.NETHERITE, TrimPattern.WILD
			))
			.build(); */
		ItemStack head = new ItemStack(Material.AIR);

		// White blazer with blue shirt
		ItemStack shirt = ItemBuilder.of(Material.LEATHER_CHESTPLATE)
			.color(Color.fromRGB(0xFFFFFF))
			.armourTrim(new ArmorTrim(
				TrimMaterial.LAPIS, TrimPattern.VEX // The undershirt
			))
			.enchant(Enchantment.PROTECTION, 6)
			.build();

		// white pants, brown shoes with black bottoms
		ItemStack pants = ItemBuilder.of(Material.IRON_LEGGINGS).build();
		ItemStack shoes = ItemBuilder.of(Material.IRON_BOOTS)
			.armourTrim(new ArmorTrim(
				TrimMaterial.COPPER, TrimPattern.SNOUT
			))
			.build();
		this.setArmor(head, shirt, pants, shoes);

		ItemStack sword = ItemBuilder.of(SWORD_TYPE)
			.displayName(Component.text("Splitter sword"))
			.lore(
				List.of(Component.text("Hit enemies right in the middle to inflict critical damage", TextUtils.LEFT_CLICK_TO))
			).build();

		// Don't use the sword inherited from trooper
		ItemStack[] items = Arrays.copyOf(this.getItems(), this.getItems().length);
		items[0] = sword;
		this.setItems(items);

		Ability[] abilities = Arrays.copyOf(this.getAbilities(), this.getAbilities().length + 1);
		abilities[this.getAbilities().length] = new CritAbility(game);
		this.setAbilities(abilities);
	}

	public CritAbility getCritAbility() { return (CritAbility) this.getAbilities()[1]; }
}

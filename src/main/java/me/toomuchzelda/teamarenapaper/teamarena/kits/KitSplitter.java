package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.CritAbility;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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

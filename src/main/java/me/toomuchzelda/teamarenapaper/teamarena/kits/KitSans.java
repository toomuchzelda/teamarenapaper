package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.GasterBlasterAbility;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class KitSans extends Kit {

	public KitSans() {
		super("sans", "Sans Undertale", "The easiest enemy. Can only deal 1 damage.", new ItemStack(Material.SKELETON_SKULL));

		this.setArmor(
			ItemUtils.createPlayerHead("f7416e3a14ad48c467276f374d5581ea72290c2fece85f6385e66d4dedb281f9"),
			ItemBuilder.of(Material.LEATHER_CHESTPLATE).color(Color.BLUE).build(),
			ItemBuilder.of(Material.LEATHER_LEGGINGS).color(Color.BLACK).build(),
			ItemBuilder.of(Material.LEATHER_BOOTS).color(Color.FUCHSIA).build()
		);

		this.setItems(GasterBlasterAbility.ITEM);
	}

	private static class SansAbility extends Ability {



		@Override
		public void onReceiveDamage(DamageEvent event) {
			event.setFinalDamage(0d);
			//event.setNoKnockback();
		}
	}
}

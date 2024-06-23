package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.Material;

public class KitSeeker extends Kit {

	public static final String NAME = "Seeker";

	public KitSeeker() {
		super(NAME, "Hunt down and kill the Hiders", Material.IRON_SWORD);

		this.setAbilities(new SeekerAbility());
	}

	private static class SeekerAbility extends Ability {

	}
}

package me.toomuchzelda.teamarenapaper.teamarena.abilities;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Manage abilities which are not part of kits
 */
public class AbilityManager {
	public final RailgunAbility railgun;

	private final List<Ability> all;

	public AbilityManager() {
		this.railgun = new RailgunAbility();
		this.railgun.registerAbility();

		this.all = List.of(railgun);
	}

	public void tick() {
		for (Ability a : all)
			a.onTick();
	}

	public void give(Player player) {
		final PlayerInfo pinfo = Main.getPlayerInfo(player);
		for (Ability ability : this.all) {
			Ability.giveAbility(player, ability, pinfo);
		}
	}

	public void remove(Player player) {
		final PlayerInfo pinfo = Main.getPlayerInfo(player);
		for (Ability ability : this.all) {
			Ability.removeAbility(player, ability, pinfo);
		}
	}

	public void unregisterAll() {
		this.railgun.unregisterAbility();
	}
}

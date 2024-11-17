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
public class CommonAbilityManager {
	public final RailgunAbility railgun;
	public final ExplosiveProjectilesAbility explosives;

	private final List<Ability> all;

	public CommonAbilityManager() {
		this.railgun = new RailgunAbility();
		this.railgun.registerAbility();
		this.explosives = new ExplosiveProjectilesAbility();
		this.explosives.registerAbility();

		this.all = List.of(railgun, explosives);
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
		for (Player p : Bukkit.getOnlinePlayers()) {
			this.remove(p);
		}

		for (Ability a : all)
			a.unregisterAbility();
	}
}

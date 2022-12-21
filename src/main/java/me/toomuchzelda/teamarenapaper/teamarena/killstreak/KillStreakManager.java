package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Class to manage TeamArena's killstreaks.
 *
 * @author toomuchzelda
 */
public class KillStreakManager
{
	private final Map<String, KillStreak> allKillstreaks;
	private final Map<Integer, List<KillStreak>> killstreaksByKills;

	public KillStreakManager() {
		this.allKillstreaks = new LinkedHashMap<>();
		this.killstreaksByKills = new HashMap<>();

		// TODO construst and put the killstreaks here
		addKillStreak("Compass", 2, new CompassKillStreak());


		// Register all killstreaks
		this.allKillstreaks.values().forEach(killStreak ->
				killStreak.getAbilities().forEach(Ability::registerAbility));
	}

	private void addKillStreak(String name, int killCount, KillStreak killStreak) {
		allKillstreaks.put(name, killStreak);
		List<KillStreak> list = killstreaksByKills.computeIfAbsent(killCount, integer -> new ArrayList<>(1));
		list.add(killStreak);
	}

	public void handleKill(Player killer, int newKills, PlayerInfo pinfo) {
		List<KillStreak> streaks = killstreaksByKills.get(newKills);
		if(streaks != null) {
			for (KillStreak streak : streaks) {
				killer.getInventory().addItem(streak.getItem());
				streak.getAbilities().forEach(ability -> {
					Ability.giveAbility(killer, ability, pinfo);
				});

				killer.sendMessage(Component.text()
						.append(Component.text(newKills + " Kill streak! Got ", NamedTextColor.RED))
						.append(Component.text(streak.getName(), streak.getTextColor()))
						.build()
				);
			}
		}
	}

	public void removeKillStreaks(Player player, PlayerInfo pinfo) {
		// Need to copy as Ability.removeAbility will modify during iteration
		Set<Ability> playersAbilities = new HashSet<>(pinfo.abilities);
		for(KillStreak streak : allKillstreaks.values()) {
			for(Ability streakAbility : streak.getAbilities()) {
				if(playersAbilities.contains(streakAbility)) {
					Ability.removeAbility(player, streakAbility, pinfo);
				}
			}
		}
	}

	public void tick() {
		for(KillStreak killStreak : allKillstreaks.values()) {
			killStreak.getAbilities().forEach(Ability::onTick);
		}
	}

	public void unregister() {
		for(KillStreak killStreak : allKillstreaks.values()) {
			killStreak.getAbilities().forEach(Ability::unregisterAbility);
		}

		allKillstreaks.clear();
		killstreaksByKills.clear();
	}
}

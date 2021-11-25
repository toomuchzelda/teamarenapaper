package me.toomuchzelda.teamarenapaper.teamarena;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.scores.Team;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class LegacyTeams
{
	//create the RWF teams from the config.yml within each SND map
	// "BLUE", "RED", etc
	public static TeamArenaTeam fromRWF(String configName) {
		TeamArenaTeam team = null;
		
		switch(configName) {
			case "BLUE":
				team = new TeamArenaTeam("Blue Team","Blue", TeamArenaTeam.convert(NamedTextColor.BLUE),
						null, DyeColor.BLUE);
				break;
			case "RED":
				team = new TeamArenaTeam("Red Team", "Red", TeamArenaTeam.convert(NamedTextColor.RED),
						null, DyeColor.RED);
				break;
			case "GREEN":
				team = new TeamArenaTeam("Green Team", "Green", TeamArenaTeam.convert(NamedTextColor.DARK_GREEN),
						null, DyeColor.GREEN);
				break;
			case "PURPLE":
				team = new TeamArenaTeam("Purple Team", "Purple", TeamArenaTeam.convert(NamedTextColor.DARK_PURPLE),
						null, DyeColor.PURPLE);
				break;
			case "YELLOW":
				team = new TeamArenaTeam("Yellow Team", "Yellow", TeamArenaTeam.convert(NamedTextColor.YELLOW),
						null, DyeColor.YELLOW);
				break;
		}
		return team;
	}
}

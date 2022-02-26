package me.toomuchzelda.teamarenapaper.teamarena;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

public class LegacyTeams
{
	//create the RWF teams from the config.yml within each SND map
	// "BLUE", "RED", etc
	// well... every team now
	public static TeamArenaTeam fromRWF(String configName) {
		TeamArenaTeam team = null;
		
		switch(configName) {
			case "BLUE":
				team = new TeamArenaTeam("Blue Team","Blue", TeamArenaTeam.convert(NamedTextColor.BLUE),
						null, DyeColor.BLUE, BossBar.Color.BLUE, Material.DEEPSLATE_DIAMOND_ORE);
				break;
			case "RED":
				team = new TeamArenaTeam("Red Team", "Red", TeamArenaTeam.convert(NamedTextColor.RED),
						null, DyeColor.RED, BossBar.Color.RED, Material.REDSTONE_BLOCK);
				break;
			case "GREEN":
				team = new TeamArenaTeam("Green Team", "Green", TeamArenaTeam.convert(NamedTextColor.GREEN),
						null, DyeColor.GREEN, BossBar.Color.GREEN, Material.MOSS_BLOCK);
				break;
			case "PURPLE": // DARK_PURPLE
				team = new TeamArenaTeam("Purple Team", "Purple", TeamArenaTeam.convert(NamedTextColor.DARK_PURPLE),
						null, DyeColor.PURPLE, BossBar.Color.PURPLE, Material.CRYING_OBSIDIAN);
				break;
			case "YELLOW":
				team = new TeamArenaTeam("Yellow Team", "Yellow", TeamArenaTeam.convert(NamedTextColor.YELLOW),
						null, DyeColor.YELLOW, BossBar.Color.YELLOW, Material.YELLOW_WOOL);
				break;
			case "PINK": //LIGHT_PURPLE
				team = new TeamArenaTeam("Pink Team", "Pink", TeamArenaTeam.convert(NamedTextColor.LIGHT_PURPLE),
						null, DyeColor.PINK, BossBar.Color.PINK, Material.BRAIN_CORAL_BLOCK);
				break;
			case "WHITE":
				team = new TeamArenaTeam("White Team", "White", TeamArenaTeam.convert(NamedTextColor.WHITE),
						null, DyeColor.WHITE, BossBar.Color.WHITE, Material.CHISELED_QUARTZ_BLOCK);
				break;
			case "BLACK":
				team = new TeamArenaTeam("Black Team", "Black", TeamArenaTeam.convert(NamedTextColor.BLACK),
						null, DyeColor.BLACK, BossBar.Color.PURPLE, Material.CHISELED_POLISHED_BLACKSTONE);
				break;
			case "DARK_BLUE":
				team = new TeamArenaTeam("Dark Blue Team", "Dark Blue", TeamArenaTeam.convert(NamedTextColor.DARK_BLUE),
						null, DyeColor.BLUE, BossBar.Color.BLUE, Material.BLUE_GLAZED_TERRACOTTA);
				break;
			case "DARK_GREEN":
				team = new TeamArenaTeam("Dark Green Team", "Dark Green", TeamArenaTeam.convert(NamedTextColor.DARK_GREEN),
						null, DyeColor.GREEN, BossBar.Color.GREEN, Material.DRIED_KELP_BLOCK);
				break;
			case "DARK_AQUA":
				team = new TeamArenaTeam("Dark Aqua Team", "Dark Aqua", TeamArenaTeam.convert(NamedTextColor.DARK_AQUA),
						null, DyeColor.CYAN, BossBar.Color.BLUE, Material.WARPED_PLANKS);
				break;
			case "DARK_RED":
				team = new TeamArenaTeam("Dark Red Team", "Dark Red", TeamArenaTeam.convert(NamedTextColor.DARK_RED),
						null, DyeColor.RED, BossBar.Color.RED, Material.NETHER_WART_BLOCK);
				break;
			case "GOLD":
				team = new TeamArenaTeam("Gold Team", "Gold", TeamArenaTeam.convert(NamedTextColor.GOLD),
						null, DyeColor.ORANGE, BossBar.Color.YELLOW, Material.RAW_GOLD_BLOCK);
				break;
			case "GRAY":
				team = new TeamArenaTeam("Gray Team", "Gray", TeamArenaTeam.convert(NamedTextColor.GRAY),
						null, DyeColor.LIGHT_GRAY, BossBar.Color.WHITE, Material.STONE_BRICKS);
				break;
			case "DARK_GRAY":
				team = new TeamArenaTeam("Dark Gray Team", "Dark Gray", TeamArenaTeam.convert(NamedTextColor.DARK_GRAY),
						null, DyeColor.GRAY, BossBar.Color.WHITE, Material.DEEPSLATE);
				break;
			case "AQUA":
				team = new TeamArenaTeam("Aqua Team", "Aqua", TeamArenaTeam.convert(NamedTextColor.AQUA),
						null, DyeColor.LIGHT_BLUE, BossBar.Color.BLUE, Material.DIAMOND_BLOCK);
				break;
		}
		return team;
	}
}

package me.toomuchzelda.teamarenapaper.teamarena;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;

public class LegacyTeams
{
	//create the RWF teams from the config.yml within each SND map
	// "BLUE", "RED", etc
	// well... every team now
	public static TeamArenaTeam fromRWF(String configName) {

		return switch (configName) {
			case "BLUE" -> new TeamArenaTeam("Blue Team", "Blue", TeamArenaTeam.convert(NamedTextColor.BLUE),
					null, DyeColor.BLUE, BossBar.Color.BLUE, Material.DEEPSLATE_DIAMOND_ORE);
			case "RED" -> new TeamArenaTeam("Red Team", "Red", TeamArenaTeam.convert(NamedTextColor.RED),
					null, DyeColor.RED, BossBar.Color.RED, Material.REDSTONE_BLOCK);
			case "GREEN" -> new TeamArenaTeam("Green Team", "Green", TeamArenaTeam.convert(NamedTextColor.GREEN),
					null, DyeColor.GREEN, BossBar.Color.GREEN, Material.MOSS_BLOCK);
			case "PURPLE" -> // DARK_PURPLE
					new TeamArenaTeam("Purple Team", "Purple", TeamArenaTeam.convert(NamedTextColor.DARK_PURPLE),
							null, DyeColor.PURPLE, BossBar.Color.PURPLE, Material.CRYING_OBSIDIAN);
			case "YELLOW" -> new TeamArenaTeam("Yellow Team", "Yellow", TeamArenaTeam.convert(NamedTextColor.YELLOW),
					null, DyeColor.YELLOW, BossBar.Color.YELLOW, Material.YELLOW_WOOL);
			case "PINK" -> //LIGHT_PURPLE
					new TeamArenaTeam("Pink Team", "Pink", TeamArenaTeam.convert(NamedTextColor.LIGHT_PURPLE),
							null, DyeColor.PINK, BossBar.Color.PINK, Material.BRAIN_CORAL_BLOCK);
			case "WHITE" -> new TeamArenaTeam("White Team", "White", TeamArenaTeam.convert(NamedTextColor.WHITE),
					null, DyeColor.WHITE, BossBar.Color.WHITE, Material.CHISELED_QUARTZ_BLOCK);
			case "BLACK" -> new TeamArenaTeam("Black Team", "Black", TeamArenaTeam.convert(NamedTextColor.BLACK),
					null, DyeColor.BLACK, BossBar.Color.PURPLE, Material.CHISELED_POLISHED_BLACKSTONE);
			case "DARK_BLUE" -> new TeamArenaTeam("Dark Blue Team", "Dark Blue", TeamArenaTeam.convert(NamedTextColor.DARK_BLUE),
					null, DyeColor.BLUE, BossBar.Color.BLUE, Material.BLUE_GLAZED_TERRACOTTA);
			case "DARK_GREEN" -> new TeamArenaTeam("Dark Green Team", "Dark Green", TeamArenaTeam.convert(NamedTextColor.DARK_GREEN),
					null, DyeColor.GREEN, BossBar.Color.GREEN, Material.DRIED_KELP_BLOCK);
			case "DARK_AQUA" -> new TeamArenaTeam("Dark Aqua Team", "Dark Aqua", TeamArenaTeam.convert(NamedTextColor.DARK_AQUA),
					null, DyeColor.CYAN, BossBar.Color.BLUE, Material.WARPED_PLANKS);
			case "DARK_RED" -> new TeamArenaTeam("Dark Red Team", "Dark Red", TeamArenaTeam.convert(NamedTextColor.DARK_RED),
					null, DyeColor.RED, BossBar.Color.RED, Material.NETHER_WART_BLOCK);
			case "GOLD" -> new TeamArenaTeam("Gold Team", "Gold", TeamArenaTeam.convert(NamedTextColor.GOLD),
					null, DyeColor.ORANGE, BossBar.Color.YELLOW, Material.RAW_GOLD_BLOCK);
			case "GRAY" -> new TeamArenaTeam("Gray Team", "Gray", TeamArenaTeam.convert(NamedTextColor.GRAY),
					null, DyeColor.LIGHT_GRAY, BossBar.Color.WHITE, Material.STONE_BRICKS);
			case "DARK_GRAY" -> new TeamArenaTeam("Dark Gray Team", "Dark Gray", TeamArenaTeam.convert(NamedTextColor.DARK_GRAY),
					null, DyeColor.GRAY, BossBar.Color.WHITE, Material.DEEPSLATE);
			case "AQUA" -> new TeamArenaTeam("Aqua Team", "Aqua", TeamArenaTeam.convert(NamedTextColor.AQUA),
					null, DyeColor.LIGHT_BLUE, BossBar.Color.BLUE, Material.DIAMOND_BLOCK);
			// For HNS
			case "SEEKERS" -> new TeamArenaTeam("Seekers", "Seekers", Color.fromRGB(0x9208FA),
				null, DyeColor.LIGHT_BLUE, BossBar.Color.BLUE, Material.TARGET);
			case "HIDERS" -> new TeamArenaTeam("Hiders", "Hiders", TeamArenaTeam.convert(NamedTextColor.GREEN),
				null, DyeColor.GREEN, BossBar.Color.GREEN, Material.HAY_BLOCK);
			default -> null;
		};
	}
}

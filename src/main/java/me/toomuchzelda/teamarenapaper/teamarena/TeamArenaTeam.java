package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class TeamArenaTeam
{
	private final String name;
	private final String simpleName;

	private final Color colour;
	//null if single flat colour
	private final Color secondColour;

	private final DyeColor dyeColour;
	private final TextColor RGBColour;
	
	//paper good spigot bad
	private Team paperTeam;
	
	private Location[] spawns;
	private Set<Entity> entityMembers = ConcurrentHashMap.newKeySet();
	
	//if someone needs to be booted out when a player leaves before game start
	//only used before teams decided
	public final Stack<Entity> lastIn = new Stack<>();
	
	//in the rare case a player joins during GAME_STARTING, need to find an unused spawn position
	// to teleport to
	public int spawnsIndex;
	
	//abstract score value, game-specific
	public int score;
	
	public TeamArenaTeam(String name, String simpleName, Color colour, Color secondColour, DyeColor dyeColor) {
		this.name = name;
		this.simpleName = simpleName;
		this.colour = colour;
		this.secondColour = secondColour;
		this.dyeColour = dyeColor;
		
		this.RGBColour = TextColor.color(colour.asRGB());
		
		spawns = null;
		score = 0;
		
		if(Bukkit.getScoreboardManager().getMainScoreboard().getTeam(name) != null)
			Bukkit.getScoreboardManager().getMainScoreboard().getTeam(name).unregister();
		
		paperTeam = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(name);
		paperTeam.displayName(Component.text(this.name).color(this.RGBColour));
		paperTeam.setAllowFriendlyFire(true);
		paperTeam.setCanSeeFriendlyInvisibles(true);
		paperTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		paperTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
		paperTeam.color(NamedTextColor.nearestTo(this.RGBColour));
	}
	
	public String getName() {
		return name;
	}
	
	public String getSimpleName() {
		return simpleName;
	}
	
	public Color getColour() {
		return colour;
	}

	public boolean isGradient() {
		return secondColour != null;
	}

	public Color getSecondColour() {
		return secondColour;
	}
	
	public DyeColor getDyeColour() {
		return dyeColour;
	}
	
	public TextColor getRGBTextColor() {
		return RGBColour;
	}
	
	public static Color convert(NamedTextColor textColor) {
		return Color.fromRGB(textColor.red(), textColor.green(), textColor.blue());
	}
	
	public Team getPaperTeam() {
		return paperTeam;
	}
	
	public Location[] getSpawns() {
		return spawns;
	}
	
	public void setSpawns(Location[] array) {
		this.spawns = array;
		this.spawnsIndex = 0;
	}
	
	public void addMembers(Entity... entities) {
		for (Entity entity : entities)
		{
			if (entity instanceof Player player)
			{
				//if they're already on a team
				// remove them from that team and update the reference in their own class
				TeamArenaTeam team = Main.getPlayerInfo(player).team;
				if (team != null)
				{
					team.removeMembers(player);
				}
				Main.getPlayerInfo(player).team = this;
				//change tab list name to colour for RGB colours
				player.playerListName(Component.text(player.getName()).color(this.getRGBTextColor()));
				paperTeam.addEntry(player.getName());
			}
			else
			{
				paperTeam.addEntry(entity.getUniqueId().toString());
			}
			entityMembers.add(entity);
			lastIn.push(entity);
		}
	}
	
	public void removeMembers(Entity... entities) {
		for (Entity entity : entities)
		{
			if (entity instanceof Player player)
			{
				paperTeam.removeEntry(player.getName());
				Main.getPlayerInfo(player).team = null;
				player.playerListName(Component.text(player.getName()).color(TeamArena.noTeamColour));
			}
			else
			{
				paperTeam.removeEntry(entity.getUniqueId().toString());
			}
			entityMembers.remove(entity);
			lastIn.remove(entity);
		}
		Main.getGame().lastHadLeft = this;
	}
	
	public void removeAllMembers() {
		removeMembers(entityMembers.toArray(new Entity[0]));
	}
	
	public Set<String> getStringMembers() {
		return paperTeam.getEntries();
	}
	
	public Set<Entity> getEntityMembers() {
		return entityMembers;
	}

	public static Color parseString(String string) {
		String[] strings = string.split(",");
		int[] ints = new int[3];
		for(int i = 0; i < strings.length; i++) {
			ints[i] = Integer.parseInt(strings[i]);
			if(i < 0 || i > 255) {
				throw new IllegalArgumentException("Bad colour info, must be between 0 and 255: " + i + " in " + string);
			}
		}
		return Color.fromRGB(ints[0], ints[1], ints[2]);
	}
}

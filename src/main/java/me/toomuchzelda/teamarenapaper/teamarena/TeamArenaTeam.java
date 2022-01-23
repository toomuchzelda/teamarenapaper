package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class TeamArenaTeam
{
	private final String name;
	private final String simpleName;

	private Component componentName;
	private Component componentSimpleName;

	private final Color colour;
	//null if single flat colour
	private final Color secondColour;

	private final DyeColor dyeColour;
	private final TextColor RGBColour;
	
	//BossBars are sent in prepLive() of each game class extending TeamArena
	private final BossBar.Color barColor;
	public final BossBar bossBar;

	//paper good spigot bad
	private Team paperTeam;

	private Location[] spawns;
	private final Set<Player> playerMembers = ConcurrentHashMap.newKeySet();

	//if someone needs to be booted out when a player leaves before game start
	//only used before teams decided
	public final Stack<Player> lastIn = new Stack<>();

	//next spawn position to use
	public int spawnsIndex;

	//abstract score values, game-specific
	public int score;
	public int score2;

	public TeamArenaTeam(String name, String simpleName, Color colour, Color secondColour, DyeColor dyeColor, BossBar.Color barColor) {
		this.name = name;
		this.simpleName = simpleName;
		this.colour = colour;
		this.secondColour = secondColour;
		this.dyeColour = dyeColor;
		
		this.RGBColour = TextColor.color(colour.asRGB());

		spawns = null;
		score = 0;
		score2 = 0;

		if(SidebarManager.SCOREBOARD.getTeam(name) != null)
			SidebarManager.SCOREBOARD.getTeam(name).unregister();

		this.componentName = colourWord(this.name);
		this.componentSimpleName = colourWord(this.simpleName);
		
		this.barColor = barColor;
		if(barColor != null) {
			bossBar = BossBar.bossBar(componentName, 1, barColor, BossBar.Overlay.NOTCHED_10);
		}
		else
			bossBar = null;

		paperTeam = SidebarManager.SCOREBOARD.registerNewTeam(name);
		paperTeam.displayName(componentName);
		//paperTeam.prefix(componentSimpleName);
		paperTeam.setAllowFriendlyFire(true);
		paperTeam.setCanSeeFriendlyInvisibles(true);
		paperTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		//previously was NEVER for RGB nametags that were armor stands
		//paperTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
		paperTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
		paperTeam.color(NamedTextColor.nearestTo(this.RGBColour));
	}

	public String getName() {
		return name;
	}

	public String getSimpleName() {
		return simpleName;
	}

	public Component getComponentName() {
		return componentName;
	}

	public Component getComponentSimpleName() {
		return componentSimpleName;
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
	
	public int getTotalScore() {
		return score + score2;
	}
	
	public BossBar.Color getBarColor() {
		return barColor;
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
				TeamArenaTeam team = Main.getPlayerInfo(player).team;

				//if they're already on this team
				// check both their reference and this Set as having the reference point here doesn't always mean
				// being in the team yet i.e when player logging in
				if(team == this && playerMembers.contains(player)) {
					updateNametag(player);
					continue;
				}

				//if (team != null)
				//{
					team.removeMembers(player);
				//}
				Main.getPlayerInfo(player).team = this;

				//change tab list name to colour for RGB colours
				// and armor stand nametag
				updateNametag(player);

				paperTeam.addEntry(player.getName());
				
				playerMembers.add(player);
				lastIn.push(player);
			}
			else
			{
				paperTeam.addEntry(entity.getUniqueId().toString());
			}
		}
	}

	public void removeMembers(Entity... entities) {
		for (Entity entity : entities)
		{
			if (entity instanceof Player player)
			{
				paperTeam.removeEntry(player.getName());
				Main.getPlayerInfo(player).team = null;
				//player.playerListName(Component.text(player.getName()).color(TeamArena.noTeamColour));
				// name colour should be handled by the team they're put on
			}
			else
			{
				paperTeam.removeEntry(entity.getUniqueId().toString());
			}
			playerMembers.remove(entity);
			lastIn.remove(entity);
		}
		Main.getGame().setLastHadLeft(this);
	}

	public void removeAllMembers() {
		//removeMembers(entityMembers.toArray(new Entity[0]));
		for (Player player : playerMembers)
		{
			//if (entity instanceof Player player)
			//{
				paperTeam.removeEntry(player.getName());
				Main.getPlayerInfo(player).team = null;
				//player.playerListName(Component.text(player.getName()).color(TeamArena.noTeamColour));
				// name colour should be handled by the team they're put on
			//}
			//else
			//{
			//	paperTeam.removeEntry(entity.getUniqueId().toString());
			//}
			playerMembers.remove(player);
			lastIn.remove(player);
		}
		
		//clear any non player entities in the paper team
		for(String entry : paperTeam.getEntries()) {
			paperTeam.removeEntry(entry);
		}
		Main.getGame().setLastHadLeft(this);
	}

	public Set<String> getStringMembers() {
		return paperTeam.getEntries();
	}

	public Set<Player> getPlayerMembers() {
		return playerMembers;
	}
	
	public boolean isAlive() {
		return playerMembers.size() > 0;
	}

	public void updateNametags() {
		for(Entity e : playerMembers) {
			if(e instanceof Player player) {
				updateNametag(player);
			}
		}
	}

	public void updateNametag(Player player) {
		Component component;
		if (Main.getGame().showTeamColours)
			component = colourWord(player.getName());
		else
			component = Main.getGame().noTeamTeam.colourWord(player.getName());

		//don't change name if it's not different
		// avoid sending packets and trouble
		if (!player.playerListName().contains(component)) {
			//Bukkit.broadcastMessage("Did not contain component");
			player.playerListName(component);
			//Main.getPlayerInfo(player).nametag.setText(component, true);
		}
	}

	public Location getNextSpawnpoint() {
		return spawns[spawnsIndex++ % spawns.length];
	}

	//for the spectator team only
	public void setNametagVisible() {
		paperTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);
	}

	//create gradient component word like player name or team name
	public Component colourWord(String str) {
		Component component = Component.empty();

		if(secondColour != null) {
			for (float i = 0; i < str.length(); i++) {
				//percentage of second colour to use, leftover is percentage of first colour
				// from 0 to 1
				float percentage = (i / (float) str.length());

				Vector colour1 = new Vector(colour.getRed(), colour.getGreen(), colour.getBlue());
				Vector colour2 = new Vector(secondColour.getRed(), secondColour.getGreen(), secondColour.getBlue());

				colour1.multiply(1 - percentage);
				colour2.multiply(percentage);

				TextColor result = TextColor.color((int) (colour1.getX() + colour2.getX()),
						(int) (colour1.getY() + colour2.getY()),
						(int) (colour1.getZ() + colour2.getZ()));


				component = component.append(Component.text(str.charAt((int) i)).color(result));
			}
		}
		else {
			component = Component.text(str).color(getRGBTextColor());
		}
		return component;
	}

	public void unregister() {
		paperTeam.unregister();
	}

	public static Color parseString(String string) {
		String[] strings = string.split(",");
		int[] ints = new int[3];
		for(int i = 0; i < strings.length; i++) {
			ints[i] = Integer.parseInt(strings[i]);
			if(ints[i] < 0 || ints[i] > 255) {
				throw new IllegalArgumentException("Bad colour info, must be between 0 and 255: " + i + " in " + string);
			}
		}
		return Color.fromRGB(ints[0], ints[1], ints[2]);
	}
	
	public static void playFireworks(TeamArenaTeam team) {
		for(Entity e : team.playerMembers) {
			Color colour1;
			Color colour2;
			if(team.secondColour != null) {
				if(MathUtils.random.nextBoolean()) {
					colour1 = team.secondColour;
					colour2 = team.colour;
				}
				else {
					colour1 = team.colour;
					colour2 = team.secondColour;
				}
			}
			else {
				colour1 = team.colour;
				colour2 = team.colour;
			}

			Firework firework = (Firework) e.getWorld().spawnEntity(e.getLocation(), EntityType.FIREWORK, CreatureSpawnEvent.SpawnReason.CUSTOM);
			FireworkMeta meta = firework.getFireworkMeta();
			meta.clearEffects();
			FireworkEffect.Type type = FireworkEffect.Type.values()[MathUtils.randomMax(FireworkEffect.Type.values().length - 1)];
			boolean flicker = MathUtils.random.nextBoolean();
			FireworkEffect effect = FireworkEffect.builder().trail(true).with(type).flicker(flicker).withColor(colour1)
					.withFade(colour2).build();
			
			meta.addEffect(effect);
			meta.setPower(1);
			firework.setFireworkMeta(meta);
			firework.setShotAtAngle(true);
			Vector velocity = new Vector(MathUtils.randomRange(-0.4, 0.4), 1, MathUtils.randomRange(-0.4, 0.4));
			firework.setVelocity(velocity);
		}
	}
}

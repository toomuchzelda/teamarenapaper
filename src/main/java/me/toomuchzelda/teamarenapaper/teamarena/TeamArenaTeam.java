package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.*;

public class TeamArenaTeam
{
	public static final Component SHOW_ALL_TEAMMATES = ItemUtils.noItalics(Component.text("Right click to see all your teammates", TextUtils.RIGHT_CLICK_TO));
	public static final List<Component> HOTBAR_ITEM_LORE = List.of(SHOW_ALL_TEAMMATES); //PING1);

	private final String name;
	private final String simpleName;

	private final Component componentName;
	private final Component componentSimpleName;

	private final Color colour;
	//null if single flat colour
	private final Color secondColour;

	private final DyeColor dyeColour;
	private final TextColor RGBColour;
	private final TextColor RGBSecondColor;

	//BossBars are sent in prepLive() of each game class extending TeamArena
	private final BossBar.Color barColor;
	public final BossBar bossBar;

	public final Material iconMaterial;
	private final ItemStack iconItem;

	//hotbar item players have during game
	private final ItemStack hotbarItem;

	//paper good spigot bad
	private final Team paperTeam;
	// Whether players are/should be on the paper team, which will send updates to client
	private boolean updatePaperTeam;

	private Location[] spawns;
	private final LinkedHashMap<Player, Void> playerMembers = new LinkedHashMap<>();

	//next spawn position to use
	private int spawnsIndex;

	//abstract score values, game-specific
	public int score;
	public int score2;

	public TeamArenaTeam(String name, String simpleName, Color colour, Color secondColour, DyeColor dyeColor,
						 BossBar.Color barColor, Material icon) {
		this.name = name;
		this.simpleName = simpleName;
		this.colour = colour;
		this.secondColour = secondColour;
		this.dyeColour = dyeColor;

		this.RGBColour = TextColor.color(colour.asRGB());
		if(secondColour != null)
			this.RGBSecondColor = TextColor.color(secondColour.asRGB());
		else
			this.RGBSecondColor = null;

		spawns = null;
		score = 0;
		score2 = 0;

		if(PlayerScoreboard.SCOREBOARD.getTeam(name) != null)
			PlayerScoreboard.SCOREBOARD.getTeam(name).unregister();

		this.componentName = colourWord(this.name);
		this.componentSimpleName = colourWord(this.simpleName);

		this.barColor = barColor;
		if(barColor != null) {
			bossBar = BossBar.bossBar(componentName, 1, barColor, BossBar.Overlay.NOTCHED_10);
		}
		else
			bossBar = null;

		this.iconMaterial = icon;
		this.iconItem = new ItemStack(iconMaterial);
		ItemMeta meta = iconItem.getItemMeta();
		meta.displayName(componentName.decoration(TextDecoration.ITALIC, false));
		iconItem.setItemMeta(meta);

		hotbarItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		LeatherArmorMeta leatherMeta = (LeatherArmorMeta) hotbarItem.getItemMeta();
		leatherMeta.displayName(ItemUtils.noItalics(Component.text("You are on ", NamedTextColor.GOLD).append(componentName)));
		leatherMeta.lore(HOTBAR_ITEM_LORE);
		leatherMeta.setColor(this.colour);
		hotbarItem.setItemMeta(leatherMeta);

		paperTeam = PlayerScoreboard.SCOREBOARD.registerNewTeam(name);
		paperTeam.displayName(componentName);
		//paperTeam.prefix(componentSimpleName.decorate(TextDecoration.BOLD));
		paperTeam.setAllowFriendlyFire(true);
		paperTeam.setCanSeeFriendlyInvisibles(true);
		paperTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		//previously was NEVER for RGB nametags that were armor stands
		//paperTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
		paperTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
		paperTeam.color(NamedTextColor.nearestTo(this.RGBColour));

		PlayerScoreboard.addGlobalTeam(paperTeam);
		this.updatePaperTeam = true;
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

	public TextColor getRGBSecondTextColor() {
		return RGBSecondColor;
	}

	public ItemStack getIconItem() {
		return this.iconItem.clone();
	}

	public ItemStack getHotbarItem() {
		return this.hotbarItem.clone();
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
		ArrayList<Entity> toPaper = new ArrayList<>(Arrays.asList(entities));
		toPaper.removeIf(entity -> {
			if (entity instanceof Player player)
			{
				PlayerInfo pinfo = Main.getPlayerInfo(player);
				TeamArenaTeam team = pinfo.team;

				//if they're already on this team
				// check both their reference and this Set as having the reference point to `this` doesn't always mean
				// being in the team yet i.e when player logging in
				if(team == this && playerMembers.containsKey(player)) {
					updateNametag(player);
					return true;
				}

				team.removeMembers(false, player);
				pinfo.team = this;

				//change tab list name to colour for RGB colours
				// and armor stand nametag
				updateNametag(player);

				playerMembers.put(player, null);

				Main.getGame().onTeamSwitch(player, team, this);

				return !this.updatePaperTeam;
			}
			else
				return false;
		});

		if (!toPaper.isEmpty()) {
			paperTeam.addEntities(toPaper);
			PlayerScoreboard.addMembersAll(paperTeam, toPaper);
		}
	}

	public void removeMembers(Entity... entities) {
		removeMembers(true, entities);
	}

	/**
	 * @param callEvent to avoid calling Main.getGame().onTeamSwitch() twice when adding a player to another team (
	 *                  and removing them from this one)
	 */
	private void removeMembers(boolean callEvent, Entity... entities) {
		List<Entity> toPaper = new ArrayList<>(Arrays.asList(entities));
		// Players should always be on a team anyway, which would implicitly do this removal
		toPaper.removeIf(entity -> entity instanceof Player);
		paperTeam.removeEntities(toPaper);
		PlayerScoreboard.removeMembersAll(paperTeam, toPaper);
		Main.getGame().setLastHadLeft(this);

		for (Entity entity : entities)
		{
			if (entity instanceof Player player)
			{
				Main.getPlayerInfo(player).team = null;
				playerMembers.remove(player);

				if(callEvent)
					Main.getGame().onTeamSwitch(player, this, null);
			}
		}
	}

	public void removeAllMembers() {
		//removeMembers(entityMembers.toArray(new Entity[0]));
		for (Player player : playerMembers.keySet())
		{
			Main.getPlayerInfo(player).team = null;
			playerMembers.remove(player);

			Main.getGame().onTeamSwitch(player, this, null);
		}

		//clear any non player entities in the paper team
		paperTeam.removeEntries(paperTeam.getEntries());
		PlayerScoreboard.removeEntriesAll(paperTeam, paperTeam.getEntries());
		Main.getGame().setLastHadLeft(this);
	}

	public void putOnMinecraftTeams(boolean put) {
		if (put != this.updatePaperTeam) {
			if(!this.updatePaperTeam) {
				// how do i do generics
				Collection<Entity> a = (Collection) this.playerMembers.keySet();
				this.paperTeam.addEntities(a);
				PlayerScoreboard.addMembersAll(this.paperTeam, a);
			}
			this.updatePaperTeam = put;
		}
	}

	public Set<String> getStringMembers() {
		return paperTeam.getEntries();
	}

	public Set<Player> getPlayerMembers() {
		return playerMembers.keySet();
	}

	public Player getRandomPlayer() {
		final int index = MathUtils.randomMax(this.playerMembers.size() - 1);
		int i = 0;
		for (Player p : this.playerMembers.keySet()) {
			if (i++ == index) return p;
		}

		return null;
	}

	public Player getLastJoinedPlayer() {
		Player last = null;
		//LinkedHashSet doesn't ahve a getter for last element
		for(Player p : playerMembers.keySet()) {
			last = p;
		}

		return last;
	}

	/** Returns true if the team has any members on it. Having members does NOT mean those members are alive in-game */
	public boolean isAlive() {
		return playerMembers.size() > 0;
	}

	/** Checks whether the team has players not considered permanently dead. */
	public boolean hasLivingOrRespawningMembers() {
		if (!isAlive())
			return false;
		TeamArena game = Main.getGame();
		for (Player player : getPlayerMembers()) {
			if (!game.isPermanentlyDead(player))
				return true;
		}
		return false;
	}

	public boolean hasMember(Player player) {
		return this.playerMembers.containsKey(player);
	}

	public boolean hasMember(Entity entity) {
		if(entity == null) return false;
		if(entity instanceof Player p) {
			return this.playerMembers.containsKey(p);
		}

		return this.paperTeam.hasEntity(entity);
	}

	public void updateNametags() {
		for(Player player : playerMembers.keySet()) {
			updateNametag(player);
		}
	}

	public void updateNametag(Player player) {
		Component component;
		//show spectators always
		if (Main.getGame().showTeamColours || this == Main.getGame().spectatorTeam)
			component = colourWord(player.getName());
		else
			component = Main.getGame().noTeamTeam.colourWord(player.getName());

		PlayerInfo pinfo = Main.getPlayerInfo(player);
		if (pinfo.displayPermissionLevel) {
			Component rank = pinfo.permissionLevel.tag;
			if (rank != null)
				component = rank.append(component);
		}

		//don't change name if it's not different
		// avoid sending packets and trouble
		if (!player.playerListName().equals(component)) {
			//Bukkit.broadcastMessage("Did not contain component");
			player.playerListName(component);
			//Main.getPlayerInfo(player).nametag.setText(component, true);
		}
	}

	public Location getNextSpawnpoint() {
		return spawns[spawnsIndex++ % spawns.length];
	}

	public void setNametagVisible(Team.OptionStatus visibility) {
		paperTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, visibility);
	}

	//create gradient component word like player name or team name
	public Component colourWord(String str) {
		Component component;
		if(secondColour != null) {
			component = TextUtils.getUselessRGBText(str, getRGBTextColor(), getRGBSecondTextColor());
		}
		else {
			component = Component.text(str, getRGBTextColor());
		}
		return component;
	}

	public void unregister() {
		PlayerScoreboard.removeGlobalTeam(paperTeam);
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
		for(Entity e : team.playerMembers.keySet()) {
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

			Firework firework = (Firework) e.getWorld().spawnEntity(e.getLocation(), EntityType.FIREWORK_ROCKET, CreatureSpawnEvent.SpawnReason.CUSTOM);
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

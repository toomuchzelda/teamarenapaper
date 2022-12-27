package me.toomuchzelda.teamarenapaper.teamarena;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandTeamChat;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.GraffitiManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.*;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.inventory.KitInventory;
import me.toomuchzelda.teamarenapaper.teamarena.inventory.SpectateInventory;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.KillStreakManager;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.WolvesKillStreak;
import me.toomuchzelda.teamarenapaper.teamarena.kits.*;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions.KitDemolitions;
import me.toomuchzelda.teamarenapaper.teamarena.kits.engineer.KitEngineer;
import me.toomuchzelda.teamarenapaper.teamarena.kits.explosive.KitExplosive;
import me.toomuchzelda.teamarenapaper.teamarena.kits.medic.KitMedic;
import me.toomuchzelda.teamarenapaper.teamarena.kits.trigger.KitTrigger;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * TeamArena game class. Handles the vast majority of the game's state and mechanics.
 *
 * @author toomuchzelda
 */
public abstract class TeamArena
{
	private final File tempWorldFile;
	public World gameWorld;
	private final TeamArenaMap gameMap;

	//ticks of wait time before teams are decided
	protected static final int PRE_TEAMS_TIME = 25 * 20;
	//ticks of wait time after teams chosen, before game starting phase
	protected static final int PRE_GAME_STARTING_TIME = 30 * 20;
	//ticks of game starting time
	protected static final int GAME_STARTING_TIME = 10 * 20;
	protected static final int TOTAL_WAITING_TIME = PRE_TEAMS_TIME + PRE_GAME_STARTING_TIME + GAME_STARTING_TIME;
	protected static final int END_GAME_TIME = 10 * 20;
	protected static final int MIN_PLAYERS_REQUIRED = 2;

	//init to this, don't want negative numbers when waitingSince is set to the past in the prepGamestate() methods
	protected static int gameTick = TOTAL_WAITING_TIME * 3;
	private int waitingSince;
	protected int gameLiveTime;
	protected GameState gameState;

	protected BoundingBox border;
	protected Location spawnPos;

	protected TeamArenaTeam[] teams;
	//to avoid having to construct a new List on every tabComplete
	protected ArrayList<String> tabTeamsList;
	protected TeamArenaTeam noTeamTeam;
	protected TeamArenaTeam winningTeam;
	//store the last team that a player has left from
	// to prevent players leaving -> rejoining before game start to try get on another team
	protected TeamArenaTeam lastHadLeft;
	//whether to show team colours in tab list + nametag yet
	protected boolean showTeamColours;

	protected TeamArenaTeam spectatorTeam;
	//use Bukkit.getOnlinePlayers() for all players
	protected Set<Player> players; // Players alive and in the game
	protected Set<Player> spectators; // Spectators including dead players
	/**
	 * for mid-game joiners with whatever amount of time to decide if they wanna join and dead players
	 * -1 value means ready to respawn when next liveTick runs (magic number moment)
	 */
	protected Map<Player, RespawnInfo> respawnTimers;
	protected static ItemStack respawnItem = ItemBuilder.of(Material.RED_DYE)
			.displayName(Component.text("Right click to respawn", NamedTextColor.RED))
			.build();
	public static final int RESPAWN_SECONDS = 5;

	protected final List<Kit> defaultKits;

	protected Map<String, Kit> kits = new LinkedHashMap<>();
	protected static ItemStack kitMenuItem = ItemBuilder.of(Material.FEATHER)
			.displayName(Component.text("Select a Kit", NamedTextColor.BLUE))
			.build();

	public static final Component OWN_TEAM_PREFIX = Component.text("▶ ");
	public static final Component OWN_TEAM_PREFIX_DANGER = OWN_TEAM_PREFIX.color(NamedTextColor.RED);

	protected Queue<DamageEvent> damageQueue;

	public final MiniMapManager miniMap;
	public final GraffitiManager graffiti;
	private final KillStreakManager killStreakManager;

	public TeamArena(TeamArenaMap map) {
		File worldFile = map.getFile();
		Main.logger().info("Loading world: " + map.getName() + ", file: " + worldFile.getAbsolutePath());
		//Main.logger().info("Reading info from " + getMapPath().getPath() + ':');

		//copy the map to another directory and load from there to avoid any accidental modifying of the original
		// map
		File dest = new File("temp_" + worldFile.getName().toLowerCase(Locale.ENGLISH) + "_" + System.currentTimeMillis());
		if (dest.mkdir()) {
			FileUtils.copyFolder(worldFile, dest);
			//delete the uid.dat
			for (File uid : dest.listFiles()) {
				if (uid.getName().equalsIgnoreCase("uid.dat")) {
					boolean b = uid.delete();
					Main.logger().info("Attempted delete of uid.dat in copy world, success: " + b);
					break;
				}
			}
		} else {
			//dae not bothered to try catch
			throw new IllegalArgumentException("Couldn't create new directory for temp map " + dest.getAbsolutePath());
		}
		dest.deleteOnExit();
		this.tempWorldFile = dest;
		WorldCreator worldCreator = new WorldCreator(dest.getName());
		//specify a ChunkGenerator that doesn't generate anything to ensure no new chunks are generated in game.
		worldCreator.generator(VoidChunkGenerator.INSTANCE);

		gameWorld = worldCreator.createWorld();

		//load the map config into real game stuff (teams, and sub-game things)
		this.gameMap = map;
		loadConfig(map);

		gameWorld.setSpawnLocation(spawnPos);
		gameWorld.setAutoSave(false);
		gameWorld.setGameRule(GameRule.DISABLE_RAIDS, true);
		gameWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS,false);
		gameWorld.setGameRule(GameRule.DO_INSOMNIA,	false);
		gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, map.isDoDaylightCycle());
		gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, map.isDoWeatherCycle());
		//undecided
		gameWorld.setGameRule(GameRule.DO_ENTITY_DROPS, false);
		gameWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
		//undecided
		gameWorld.setGameRule(GameRule.DO_MOB_LOOT, false);
		gameWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
		gameWorld.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
		gameWorld.setGameRule(GameRule.DO_TILE_DROPS, false);
		//handle ourselves
		gameWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
		gameWorld.setGameRule(GameRule.MAX_ENTITY_CRAMMING, 0);
		gameWorld.setGameRule(GameRule.MOB_GRIEFING, false);
		//handled ourselves
		gameWorld.setGameRule(GameRule.NATURAL_REGENERATION, false);
		gameWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
		gameWorld.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
		gameWorld.setDifficulty(Difficulty.NORMAL);

		//force disable relative projectile velocity (projectiles inheriting the velocity of their shooter)
		((CraftWorld) gameWorld).getHandle().paperConfig().misc.disableRelativeProjectileVelocity = true;

		waitingSince = gameTick;
		//gameState = GameState.PREGAME;
		setGameState(GameState.PREGAME);

		noTeamTeam = new TeamArenaTeam("No Team", "No Team", Color.YELLOW, Color.ORANGE, DyeColor.YELLOW, null, Material.GRASS_BLOCK);
		spectatorTeam = new TeamArenaTeam("Spectators", "Specs", TeamArenaTeam.convert(NamedTextColor.DARK_GRAY), null,
				null, null, Material.GLASS);
		winningTeam = null;
		lastHadLeft = null;

		//List of team names
		tabTeamsList = new ArrayList<>(teams.length);
		for(TeamArenaTeam team : teams) {
			tabTeamsList.add(team.getSimpleName());
		}

		players = new LinkedHashSet<>();
		spectators = new LinkedHashSet<>();
		respawnTimers = new LinkedHashMap<>();
		damageQueue = new LinkedList<>();

		PlayerListScoreManager.removeScores();

		miniMap = new MiniMapManager(this);
		graffiti = new GraffitiManager(this);
		killStreakManager = new KillStreakManager();

		this.defaultKits = List.of(new KitTrooper(), new KitArcher(), new KitGhost(), new KitDwarf(),
				new KitBurst(), new KitJuggernaut(), new KitNinja(), new KitPyro(), new KitSpy(), new KitDemolitions(),
				new KitNone(), new KitVenom(), new KitRewind(), new KitValkyrie(), new KitEngineer(), new KitExplosive(),
				new KitTrigger(), new KitMedic(this.killStreakManager));

		registerKits();

		DamageTimes.clear();

		StatusBarManager.StatusBarHologram.updatePregameText();

		//init all the players online at time of construction
		Kit fallbackKit = CommandDebug.filterKit(kits.values().iterator().next());
		for (var entry : Main.getPlayerInfoMap().entrySet()) {
			Player p = entry.getKey();
			PlayerInfo pinfo = entry.getValue();

			boolean tele = p.teleport(spawnPos);
			if(!tele) {
				Main.logger().severe("Could not teleport " + p.getName() + " to new game world " + gameWorld.getName());
			}
			players.add(p);

			pinfo.spawnPoint = spawnPos;
			pinfo.kit = findKit(pinfo.defaultKit);
			pinfo.team = noTeamTeam;
			pinfo.clearDamageReceivedLog();
			pinfo.getKillAssistTracker().clear();
			pinfo.kills = 0;
			noTeamTeam.addMembers(p);

			if(pinfo.kit == null)
				pinfo.kit = fallbackKit;

			PlayerUtils.resetState(p);
			p.setAllowFlight(true);

			p.getInventory().clear();
			giveLobbyItems(p);

			this.sendGameAndMapInfo(p);

			StatusBarManager.showStatusBar(p, pinfo);
		}
	}

	protected void registerKits() {
		defaultKits.forEach(this::registerKit);
	}

	protected void registerKit(Kit kit) {
		kits.put(kit.getName().toLowerCase(Locale.ENGLISH), kit);
		for (Ability ability : kit.getAbilities()) {
			ability.registerAbility();
		}
	}

	// player as in players in the players set
	public void givePlayerItems(Player player, PlayerInfo info, boolean clear) {
		player.sendMap(miniMap.view);
		PlayerInventory inventory = player.getInventory();
		if(clear)
			inventory.clear();

		inventory.setItem(8, miniMap.getMapItem(info.team));
		inventory.setItem(7, info.team.getHotbarItem());
		info.kit.giveKit(player, true, info);
	}

	public void cleanUp() {
		if (Main.getGame() != null) {
			//teleport everyone to new world before unloading: worlds must have no players in them to be unloaded
			for(Player player : gameWorld.getPlayers()) {
				if(!player.teleport(Main.getGame().spawnPos)) {
					player.kick(Component.text("Something went horribly wrong!!! Oh my god!!! OH MY GOOODDDD!!!!!!!!", NamedTextColor.YELLOW));
					Main.logger().severe("Teleporting " + player.getName() + " in cleanUp() fallback failed");
				}
			}
		}
		//no next game, plugin is (should be) disabling
		// kick everyone so the world can unload and the folder be deleted.
		else {
			if(Main.getPlugin().isEnabled()) {
				Main.logger().severe("Plugin is enabled but next game is null!");
			}

			for (Player player : Bukkit.getOnlinePlayers()) {
				player.kick(TextUtils.getRGBManiacComponent(
						Component.text("Row row, fight the power! (Server is closing (Merging with Mineplex (jk)))"),
						Style.empty(), 0));
			}
		}

		if (Bukkit.unloadWorld(gameWorld, false)) {
			FileUtils.delete(tempWorldFile);
		} else {
			Main.logger().severe("Failed to unload world " + gameWorld.getName());
		}
		gameWorld = null;
	}

	public final void tickSidebar() {
		boolean showGameSidebar = gameState != GameState.PREGAME && gameState != GameState.TEAMS_CHOSEN;
		boolean showTeamSize = gameState == GameState.TEAMS_CHOSEN;

		Collection<Component> sharedSidebar = showGameSidebar ? updateSharedSidebar() : null;

		for (var player : Bukkit.getOnlinePlayers()) {
			var sidebar = SidebarManager.getInstance(player);
			var style = Main.getPlayerInfo(player).getPreference(Preferences.SIDEBAR_STYLE);
			if (style == SidebarManager.Style.HIDDEN) {
				sidebar.clear(player);
				continue;
			}

			if (!showGameSidebar) {
				sidebar.setTitle(player, Component.text("Teams", NamedTextColor.GOLD));
				for (var team : getTeams()) {
					var builder = Component.text();
					if (team.getPlayerMembers().contains(player)) {
						builder.append(OWN_TEAM_PREFIX);
					}
					builder.append(team.getComponentName());
					if (showTeamSize) {
						builder.append(Component.text(": " + team.getPlayerMembers().size()));
					}
					sidebar.addEntry(builder.build());
				}
			} else {
				sharedSidebar.forEach(sidebar::addEntry);
				if (style == SidebarManager.Style.MODERN || style == SidebarManager.Style.RGB_MANIAC) {
					updateSidebar(player, sidebar);
				} else { // for conservatives like toomuchzelda
					updateLegacySidebar(player, sidebar);
				}
			}

			if (style == SidebarManager.Style.RGB_MANIAC || style == SidebarManager.Style.LEGACY_RGB_MANIAC) {
				double progress = (TeamArena.getGameTick() / 5 * 5) / 70d;
				for (var iterator = sidebar.getEntries().listIterator(); iterator.hasNext(); ) {
					var index = iterator.nextIndex();
					var entry = iterator.next();
					var component = TextUtils.getRGBManiacComponent(entry, Style.empty(), progress + index / 7d);
					sidebar.setEntry(index, component);
				}
			}

			sidebar.update(player);
		}
	}

	public Collection<Component> updateSharedSidebar() {
		return Collections.emptyList();
	}

	public abstract void updateSidebar(Player player, SidebarManager sidebar);

	public void updateLegacySidebar(Player player, SidebarManager sidebar) {
		//sidebar.addEntry(Component.text("Warning: legacy unsupported", NamedTextColor.YELLOW));
		updateSidebar(player, sidebar);
	}

	public void tick() {
		gameTick++;

		if(gameState.isPreGame())
		{
			preGameTick();
		}
		else if(gameState == GameState.LIVE)
		{
			liveTick();
		}
		else if(gameState == GameState.END)
		{
			endTick();
		}

		tickSidebar();
		graffiti.tick();
	}

	public void preGameTick() {
		//if countdown is ticking, do announcements
		if (CommandDebug.ignoreWinConditions || players.size() >= MIN_PLAYERS_REQUIRED) {
			//announce Game starting in:
			// and play sound
			sendCountdown(false);
			//teams decided time
			if(waitingSince + PRE_TEAMS_TIME == gameTick) {
				prepTeamsDecided();
			}
			//Game starting; teleport everyone to spawns and freeze them
			else if(waitingSince + PRE_TEAMS_TIME + PRE_GAME_STARTING_TIME == gameTick) {
				prepGameStarting();
			}
			//start game
			else if (waitingSince + TOTAL_WAITING_TIME == gameTick) {
				prepLive();
			}
		} else {
			waitingSince = gameTick;

			if (gameState == GameState.TEAMS_CHOSEN || gameState == GameState.GAME_STARTING) {
				//remove players from all teams
				/*for(TeamArenaTeam team : teams) {
					team.removeAllMembers();
				}*/

				//maybe band-aid, needed to set gamestate now for setSpectator() to work
				setGameState(GameState.PREGAME);
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (isSpectator(p))
						setSpectator(p, false, false);
					else
						noTeamTeam.addMembers(p);
				}
				showTeamColours = false;

				//announce game cancelled
				// spam sounds lol xddddddd
				for (Player player : Bukkit.getOnlinePlayers()) {
					for (int i = 0; i < 10; i++) {
						// peak humor
						Sound sound = MathUtils.random.nextBoolean() ? SoundUtils.getRandomObnoxiousSound() : SoundUtils.getRandomSound();
						player.playSound(border.getCenter().toLocation(gameWorld), sound,
								SoundCategory.AMBIENT, 99999f, (float) MathUtils.randomRange(0.5, 2));
					}
				}
				Bukkit.broadcast(Component.text("Not enough players to start the game, game cancelled!").color(MathUtils.randomTextColor()));
			}
			//setGameState(GameState.PREGAME);
		}

		StatusBarManager.StatusBarHologram.updatePregameText();
	}

	public void liveTick() {
		BuildingManager.tick();

		//checking team states (win/lose) done in liveTick() per-game

		SpectatorAngelManager.tick();
		//process players waiting to respawn if a respawning game
		if(isRespawningGame()) {
			respawnerTick();
		}

		//ability tick 'events'
		for (Kit kit : kits.values()) {
			for (Ability ability : kit.getAbilities()) {
				ability.onTick();
			}
		}

		// Killstreak tick events
		this.killStreakManager.tick();

		// Kit and killstreak player tick events
		for (var entry : Main.getPlayerInfoMap().entrySet()) {
			for(Ability a : entry.getValue().abilities) {
				a.onPlayerTick(entry.getKey());
			}
		}

		//handle and queue fire + poison damage events
		fireAndPoisonTick();

		//process damage events
		damageTick();

		regenTick();

		//end the game if there are no more players for some reason (everyone left or spectator)
		//also reveal bossbars for any teams that just joined
		byte aliveTeamCount = 0;
		TeamArenaTeam lastTeam = null;
		for(TeamArenaTeam team : teams) {
			if(team.isAlive()) {
				aliveTeamCount++;
				lastTeam = team;
			}
		}
		if (!CommandDebug.ignoreWinConditions && aliveTeamCount < 2) {
			if (lastTeam != null) {
				winningTeam = lastTeam;
				Bukkit.broadcast(lastTeam.getComponentName().append(Component.text(" is the last team standing so they win!!")));
			} else {
				Bukkit.broadcast(Component.text("Where'd everyone go?"));
			}
			prepEnd();
		}
	}

	public void respawnerTick() {
		Iterator<Map.Entry<Player, RespawnInfo>> respawnIter = respawnTimers.entrySet().iterator();
		while (respawnIter.hasNext()) {
			Map.Entry<Player, RespawnInfo> entry = respawnIter.next();
			Player p = entry.getKey();
			RespawnInfo rinfo = entry.getValue();

			//player interrupted respawning, ready to respawn
			// now handled with the interrupted boolean
			/*if(rinfo.deathTime == -1) {


				respawnPlayer(p);
				respawnIter.remove();
				continue;
			}*/

			//respawn after five seconds
			int ticksLeft = getGameTick() - rinfo.deathTime;
			if (ticksLeft >= RESPAWN_SECONDS * 20) {
				if(rinfo.interrupted) {
					if(ticksLeft % 20 == 0) {
						TextColor color;
						if (ticksLeft % 40 == 20)
							color = TextColor.color(52, 247, 140);
						else
							color = MathUtils.randomTextColor();

						p.sendActionBar(Component.text("Ready to respawn! Click the Red Dye or type /respawn")
								//.color(MathUtils.randomTextColor()));
								.color(color));
					}
				}
				else {
					respawnPlayer(p);
					respawnIter.remove();
					p.sendActionBar(Component.text("Respawned!").color(TextColor.color(0, 255, 0)));
				}
			}
			else {
				//tell them how long remaining to respawn
				int seconds = RESPAWN_SECONDS - (ticksLeft / 20);
				TextColor color;
				//flash green
				if (seconds % 2 == 0)
					color = TextColor.color(0, 255, 0);
				else
					color = TextColor.color(0, 190, 0);

				p.sendActionBar(Component.text("Respawning in " + seconds + " seconds").color(color));
			}
		}
	}

	public void handlePlayerJoinMidGame(Player player) {
		TeamArenaTeam team = addToLowestTeam(player, false);
		//if team was dead before, now becoming alive, show their bossbar
		// - not anymore
		/*if (!team.isAlive()) {
			for (Player viewer : Bukkit.getOnlinePlayers()) {
				viewer.showBossBar(team.bossBar);
			}
		}*/
		team.addMembers(player);

		informOfTeam(player);

		Bukkit.broadcast(player.playerListName().append(Component.text(" joined ", NamedTextColor.YELLOW)).append(team.getComponentName()));
	}

	public void damageTick() {
		Iterator<DamageEvent> iter = damageQueue.iterator();
		while(iter.hasNext()) {
			DamageEvent event = iter.next();
			iter.remove();

			onDamage(event);
			if(event.isCancelled())
				continue;

			//ability on confirmed attacks done in this.onConfirmedDamage() called by DamageEvent.executeAttack()
			if(event.getFinalAttacker() instanceof Player p && event.getVictim() instanceof Player p2) {
				if(!canAttack(p, p2))
					event.setCancelled(true);
			}

			//ability pre-attack events
			if(event.getFinalAttacker() instanceof Player p) {
				for(Ability ability : Kit.getAbilities(p)) {
					ability.onAttemptedAttack(event);
				}
			}
			if(event.getVictim() instanceof Player p) {
				for(Ability ability : Kit.getAbilities(p)) {
					ability.onAttemptedDamage(event);
				}
			}

			event.executeAttack();
		}

		/*var indiIter = activeDamageIndicators.iterator();
		while(indiIter.hasNext()) {
			DamageIndicatorHologram h = indiIter.next();
			if(h.age >= 300) {
				h.despawn();
				indiIter.remove();
			}
			else {
				h.tick();
			}
		}*/
	}

	public void onConfirmedDamage(DamageEvent event) {

		Player playerCause = null; //for hologram
		if(event.getFinalAttacker() instanceof Player p) {
			for(Ability ability : Kit.getAbilities(p)) {
				ability.onDealtAttack(event);
			}
			playerCause = p;
		}
		if(!event.isCancelled()) {
			if (event.getVictim() instanceof final Player p) {
				for (Ability ability : Kit.getAbilities(p)) {
					ability.onReceiveDamage(event);
				}

				if (!event.isCancelled() && event.getFinalDamage() > 0) {
					PlayerInfo pinfo = Main.getPlayerInfo(p);
					//spawn damage indicator hologram
					// divide by two to display as hearts
					Component damageText = Component.text(MathUtils.round(event.getFinalDamage() / 2, 2), NamedTextColor.YELLOW, TextDecoration.BOLD);
					Location spawnLoc = p.getLocation();
					spawnLoc.add(0, MathUtils.randomRange(1.4, 2), 0);
					new DamageIndicatorHologram(spawnLoc, PlayerUtils.getDamageIndicatorViewers(p, playerCause), damageText);

					//add to their damage log
					pinfo.logDamageReceived(p, event.getDamageType(), event.getFinalDamage(), event.getFinalAttacker(), gameTick);

					//give kill assist credit
					if (event.getFinalAttacker() instanceof Player attacker && p != attacker) {
						pinfo.getKillAssistTracker().addDamage(attacker, event.getFinalDamage());
					}
				}
			}
		}
	}

	/**
	 * also for overriding in subclass
	 */
	public void onDamage(DamageEvent event) {
		//multiple DamageEvents can exist in same tick for 1 victim, causes errors if a previous one killed them and
		// another runs
		if(isDead(event.getVictim())) {
			event.setCancelled(true);
			//don't bother passing it to event handlers?
			return;
		}

		if(this.killStreakManager.isCrateFirework(event.getFinalAttacker())) {
			event.setCancelled(true);
			return;
		}

		if(event.hasKnockback()) {
			//reduce knockback done by axes
			if (event.getDamageType().isMelee() && event.getFinalAttacker() instanceof LivingEntity living) {
				if (living.getEquipment() != null) {
					ItemStack weapon = living.getEquipment().getItemInMainHand();
					if (weapon.getType().toString().endsWith("AXE")) {
						event.getKnockback().multiply(0.8);
						//Bukkit.broadcastMessage("Reduced axe knockback");
					}
				}
			}
			//reduce knockback done by projectiles
			else if(event.getDamageType().isProjectile()) {
				if(event.getAttacker() instanceof Projectile) {
					event.getKnockback().multiply(0.8);
				}
			}
		}

		if(event.getVictim() instanceof Skeleton) {
			KitEngineer.EngineerAbility.handleSentryAttemptDamage(event);
		}
		else if(event.getVictim() instanceof Wolf) {
			WolvesKillStreak.WolvesAbility.handleWolfAttemptDamage(event);
		}
	}

	public boolean isDead(Entity victim) {
		if(victim instanceof Player p) {
			return !players.contains(p) || isSpectator(p);
		}

		return victim.isDead() || !victim.isValid();
	}

	public void onInteract(PlayerInteractEvent event) {
		if (respawnItem.isSimilar(event.getItem())) {
			event.setUseItemInHand(Event.Result.DENY);
			if (canRespawn(event.getPlayer()))
				setToRespawn(event.getPlayer());
			else
				event.getPlayer().sendMessage(Component.text("You can't respawn right now").color(NamedTextColor.RED));
		} else if (kitMenuItem.isSimilar(event.getItem())) {
			event.setUseItemInHand(Event.Result.DENY);
			Inventories.openInventory(event.getPlayer(), new KitInventory());
		}
		else if (gameState == GameState.LIVE){
			Player clicker = event.getPlayer();
			PlayerInfo pinfo = Main.getPlayerInfo(clicker);
			TeamArenaTeam team = pinfo.team;
			if (miniMap.isMapItem(event.getItem()) && event.useItemInHand() != Event.Result.DENY) {
				event.setUseItemInHand(Event.Result.DENY);
				event.setUseInteractedBlock(Event.Result.DENY);
				// TODO fix respawning players being able to see other teams
				TeamArenaTeam teamFilter = isSpectator(clicker) ? null : team;
				Inventories.openInventory(clicker, new SpectateInventory(teamFilter));
				return;
			}
			//right click to glow teammates
			if(team.getHotbarItem().isSimilar(event.getItem())) {
				Action action = event.getAction();
				if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
					event.setUseItemInHand(Event.Result.DENY);
					setViewingGlowingTeammates(pinfo, !pinfo.viewingGlowingTeammates, true);
				}
			}
			// Killstreak crate items
			else {
				this.killStreakManager.handleCrateItemUse(event);
			}
		}
	}

	public void setViewingGlowingTeammates(PlayerInfo pinfo, boolean glow, boolean message) {
		MetadataViewer meta = pinfo.getMetadataViewer();
		pinfo.viewingGlowingTeammates = glow;

		for (Player viewed : pinfo.team.getPlayerMembers()) {
			if(glow) {
				meta.updateBitfieldValue(viewed, MetaIndex.BASE_BITFIELD_IDX,
						MetaIndex.BASE_BITFIELD_GLOWING_IDX, glow);
			}
			else {
				meta.removeBitfieldValue(viewed, MetaIndex.BASE_BITFIELD_IDX,
						MetaIndex.BASE_BITFIELD_GLOWING_IDX);
			}

			meta.refreshViewer(viewed);
		}

		if(message) {
			Component text;
			if (glow)
				text = Component.text("Now seeing your teammates through walls", NamedTextColor.BLUE);
			else
				text = Component.text("Stopped seeing teammates through walls", NamedTextColor.BLUE);

			meta.getViewer().sendMessage(text);
		}
	}

	public void onInteractEntity(PlayerInteractEntityEvent event) {}

	public void onPlaceBlock(BlockPlaceEvent event) {}

	public void onChat(AsyncChatEvent event) {
		event.setCancelled(true);

		final Player chatter = event.getPlayer();
		final PlayerInfo pinfo = Main.getPlayerInfo(chatter);
		//if player is defaulting to team chat
		if(this.canTeamChatNow(chatter) && pinfo.getPreference(Preferences.DEFAULT_TEAM_CHAT)) {
			//put their message in team chat if teams have been decided
			CommandTeamChat.sendTeamMessage(pinfo.team, chatter, event.message());
		}
		else { //else global chat
			Bukkit.broadcast(constructChatMessage(chatter, event.message()));
		}
	}

	private static final Component COLON_SPACE = Component.text(": ");
	public Component constructChatMessage(Player sender, Component message) {
		return Component.text()
				.append(EntityUtils.getComponent(sender))
				.append(COLON_SPACE)
				.append(message)
				.build();
	}

	public void regenTick() {
		if(gameTick % 60 == 0) {
			Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
			while(iter.hasNext()) {
				Map.Entry<Player, PlayerInfo> entry = iter.next();

				Player p = entry.getKey();

				PlayerUtils.heal(p, 1, EntityRegainHealthEvent.RegainReason.SATIATED); // half a heart
			}
		}
	}

	/**
	 * Handle all entities on fire
	 */
	public void fireAndPoisonTick() {
		var iter = DamageTimes.getIterator();
		Map.Entry<LivingEntity, DamageTimes.DamageTime[]> entry;

		DamageTimes.DamageTime time;
		LivingEntity victim;
		PotionEffect poison;
		final int currentTick = getGameTick();
		int poisonRate;
		while(iter.hasNext()) {
			entry = iter.next();
			victim = entry.getKey();

			//FIRE
			time = entry.getValue()[DamageTimes.TrackedDamageTypes.FIRE.ordinal()];
			if(victim.getFireTicks() > 0) {
				if(time.getTimeGiven() == -1) {
					time.setTimeGiven(currentTick);
				}

				if ((currentTick - time.getTimeGiven()) % 20 == 0) {
					DamageEvent fireDEvent = DamageEvent.newDamageEvent(victim, 1, DamageType.FIRE_TICK, null, false);
					queueDamage(fireDEvent);
				}
			}
			//clear fire giver so they don't get credit if the person gets set on fire later
			else {
				time.extinguish();
			}

			//POISON
			time = entry.getValue()[DamageTimes.TrackedDamageTypes.POISON.ordinal()];
			poison = victim.getPotionEffect(PotionEffectType.POISON);
			if(poison != null) {
				if(time.getTimeGiven() == -1) {
					time.setTimeGiven(currentTick);
				}

				poisonRate = poison.getAmplifier() > 0 ? 12 : 25;
				if((currentTick - time.getTimeGiven()) % poisonRate == 0 && victim.getHealth() > 2d) { //must leave them at half a heart
					DamageEvent pEvent = DamageEvent.newDamageEvent(victim, 1d, DamageType.POISON, time.getGiver(), false);
					queueDamage(pEvent);
				}
			}
			else {
				time.extinguish();
			}
		}
	}

	public void endTick() {
		//fireworks
		if(winningTeam != null && gameTick % 40 == 0)
			TeamArenaTeam.playFireworks(winningTeam);

		if(gameTick - waitingSince >= END_GAME_TIME) {
			Bukkit.broadcastMessage("Prepping dead....");
			prepDead();
		}
	}

	public void prepTeamsDecided() {
		//set teams here
		showTeamColours = true;
		setupTeams();
		setGameState(GameState.TEAMS_CHOSEN);

		Bukkit.broadcast(Component.text("Teams have been decided!", NamedTextColor.RED));
		for (Player p : players) {
			informOfTeam(p);
			PlayerInfo pinfo = Main.getPlayerInfo(p);
			StatusBarManager.setBarText(pinfo, pinfo.kit.getDisplayName());
		}
		Main.logger().info("Decided Teams");

		//correct the timer
		waitingSince = gameTick - PRE_TEAMS_TIME;

		for(Player p : spectators) {
			makeSpectator(p);
		}

		sendCountdown(true);
	}

	public void prepGameStarting() {
		//teleport players to team spawns
		for(TeamArenaTeam team : teams) {
			for(Entity e : team.getPlayerMembers()) {
				if(e instanceof Player p)
					p.setAllowFlight(false);

				e.teleport(team.getNextSpawnpoint());
			}
		}

		//correct the timer
		waitingSince = gameTick - PRE_TEAMS_TIME - PRE_GAME_STARTING_TIME;
		//EventListeners.java should stop them from moving
		setGameState(GameState.GAME_STARTING);
	}


	public void prepLive() {
		setGameState(GameState.LIVE);
		gameLiveTime = gameTick;

		if(damageQueue.size() > 0) {
			Main.logger().warning("damage queue had events in it during prepLive()!");
			damageQueue.clear();
		}

		Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			final Player player = entry.getKey();

			PlayerUtils.resetState(player);
			player.setSaturatedRegenRate(0);
			PlayerListScoreManager.setKills(player, 0);

			givePlayerItems(player, entry.getValue(), true);

			player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.AMBIENT, 2, 1);
		}

		for(TeamArenaTeam team : teams) {
			//if(team.isAlive())
			//	player.showBossBar(team.bossBar);

			team.bossBar.progress(0); //init to 0, normally is 1
		}
	}

	public void prepEnd() {
		waitingSince = gameTick;

		if(winningTeam != null) {
			Component winText = winningTeam.getComponentName().append(Component.text(" wins!!").color(winningTeam.getRGBTextColor()));
			Bukkit.broadcast(winText);

			Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
			while(iter.hasNext()) {
				Map.Entry<Player, PlayerInfo> entry = iter.next();
				if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
					PlayerUtils.sendTitle(entry.getKey(), winText, Component.empty(), 10, 4 * 20, 10);
				}
				if(entry.getValue().team == winningTeam) {
					entry.getKey().playSound(entry.getKey().getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE,
							SoundCategory.AMBIENT, 2f, 1f);
				}
			}
		}
		else {
			Bukkit.broadcast(Component.text("DRAW!!!!!!").color(NamedTextColor.AQUA));
			Bukkit.broadcast(Component.text("DRAW!!!!!!").color(NamedTextColor.AQUA));
			Bukkit.broadcast(Component.text("DRAW!!!!!!").color(NamedTextColor.AQUA));
			Bukkit.broadcast(Component.text("DRAW!!!!!!").color(NamedTextColor.AQUA));

			Bukkit.getOnlinePlayers().forEach(player ->	player.playSound(player.getLocation(),
					Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.AMBIENT, 2, 0.5f));
		}


		//cleanup everything before dropping the reference to this for garbage collection
		// everything here may not need to be manually cleared, but better safe than sorry

		//reveal everyone to everyone just to be safe
		for(Player p : Bukkit.getOnlinePlayers()) {
			for(Player pp : Bukkit.getOnlinePlayers()) {
				p.showPlayer(Main.getPlugin(), pp);
			}
			p.setAllowFlight(true);

			PlayerInfo pinfo = Main.getPlayerInfo(p);
			if(pinfo.activeKit != null) { //!isSpectator(p)) {
				pinfo.activeKit.removeKit(p, pinfo);
			}
			pinfo.kit = null;
			//unglow before setting pinfo.team to null as it needs that.
			setViewingGlowingTeammates(pinfo, false, false);

			// Remove any corpse angels if they have one
			SpectatorAngelManager.removeAngel(p);

			this.killStreakManager.removeKillStreaks(p, pinfo);
		}

		for(Kit kit : kits.values()) {
			for(Ability ability : kit.getAbilities()) {
				ability.unregisterAbility();
			}
		}

		miniMap.cleanUp();
		graffiti.cleanUp();

		players.clear();
		spectators.clear();
		respawnTimers.clear();
		damageQueue.clear();

		setGameState(GameState.END);
		Bukkit.broadcastMessage("Game end");
	}


	public void prepDead() {
		for(var entry : Main.getPlayerInfoMap().entrySet()) {
			final Player p = entry.getKey();
			PlayerUtils.resetState(p);

			final PlayerInfo pinfo = entry.getValue();
			StatusBarManager.hideStatusBar(p, pinfo);
		}

		for (TeamArenaTeam team : teams) {
			//team.removeAllMembers();
			team.unregister();
		}
		//spectatorTeam.removeAllMembers();
		spectatorTeam.unregister();
		noTeamTeam.unregister();

		// remove map
		miniMap.removeMapView();

		this.killStreakManager.unregister();

		BuildingManager.cleanUp();

		setGameState(GameState.DEAD);
	}

	public void setupTeams() {
		//shuffle order of teams first so certain teams don't always get the odd player(s)
		TeamArenaTeam[] shuffledTeams = Arrays.copyOf(teams, teams.length);
		MathUtils.shuffleArray(shuffledTeams);

		//players that didn't choose a team yet
		ArrayList<Player> shuffledPlayers = new ArrayList<>();
		for(Player p : players) {
			if(/*p.getTeamArenaTeam() == null || */Main.getPlayerInfo(p).team == noTeamTeam)
				shuffledPlayers.add(p);
		}
		//if everyone is already on a team (there is noone without a team selected)
		if(shuffledPlayers.size() == 0)
			return;

		Collections.shuffle(shuffledPlayers, MathUtils.random);

		//not considering remainders/odd players
		int maxOnTeam = players.size() / teams.length;

		//theoretically playerIdx shouldn't become larger than the number of players so i don't need to modulus
		int playerIdx = 0;
		for(TeamArenaTeam team : shuffledTeams) {
			while(team.getPlayerMembers().size() < maxOnTeam) {
				team.addMembers(shuffledPlayers.get(playerIdx));
				playerIdx++;
			}
		}

		int numOfRemainders = players.size() % teams.length;
		if(numOfRemainders > 0) {
			for(int i = 0; i < numOfRemainders; i++) {
				shuffledTeams[i].addMembers(shuffledPlayers.get(playerIdx));
				playerIdx++;
			}
		}

		for(TeamArenaTeam team : teams) {
			team.updateNametags();
		}

		//also update name colours for spectators
		spectatorTeam.updateNametags();
	}

	public void balancePlayerLeave() {
		if(gameState == GameState.PREGAME) {
			int maxTeamSize = players.size() / teams.length;
			for (TeamArenaTeam team : teams)
			{
				if (team.getPlayerMembers().size() > maxTeamSize)
				{
					//peek not pop, since removeMembers will remove them from the Stack
					Player removed = team.getLastJoinedPlayer();
					//team.removeMembers(removed);
					noTeamTeam.addMembers(removed);
					removed.sendMessage(Component.text("A player left, so you were removed from your chosen team for balance. Sorry!").color(NamedTextColor.AQUA));
					removed.playSound(removed.getLocation(), Sound.ENTITY_CHICKEN_HURT, SoundCategory.AMBIENT, 2f, 1f);
				}
			}
		}
	}

	public void giveLobbyItems(Player player) {
		PlayerInventory inventory = player.getInventory();
		inventory.setItem(0, kitMenuItem.clone());
	}

	public Collection<Kit> getKits() {
		return kits.values();
	}

	public TeamArenaTeam[] getTeams() {
		return teams;
	}

	public Set<Player> getSpectators() {
		return spectators;
	}

	public KillStreakManager getKillStreakManager() {
		return this.killStreakManager;
	}

	public abstract boolean canSelectKitNow();

	public abstract boolean canSelectTeamNow();

	public abstract boolean canTeamChatNow(Player player);

	public void selectKit(@NotNull Player player, @NotNull Kit kit) {
		if (!canSelectKitNow()) {
			player.sendMessage(Component.text("You can't choose a kit right now").color(NamedTextColor.RED));
			return;
		}
		final PlayerInfo pinfo = Main.getPlayerInfo(player);
		pinfo.kit = kit;
		player.sendMessage(Component.text("Using kit " + kit.getName(), NamedTextColor.BLUE));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1f, 2f);

		// if TEAMS_DECIDED or GAME_STARTING gamestate update their status bar to teammates
		if(this.gameState.teamsChosen()) {
			StatusBarManager.setBarText(pinfo, kit.getDisplayName());
		}
	}

	@Nullable
	public Kit findKit(String name) {
		return kits.get(name.toLowerCase(Locale.ENGLISH));
	}

	public void selectTeam(Player player, String teamName) {
		//see if team by this name exists
		TeamArenaTeam requestedTeam = null;
		for (TeamArenaTeam team : teams) {
			if (team.getName().equalsIgnoreCase(teamName) || team.getSimpleName().equalsIgnoreCase(teamName)) {
				requestedTeam = team;
				break;
			}
		}
		//if team wasn't found
		if (requestedTeam == null) {
			player.sendMessage(Component.text("Could not find team: " + teamName).color(NamedTextColor.RED));
			return;
		}

		//figure out if the team can hold any more players
		int numPlayers = players.size();
		int maxOnTeam = numPlayers / teams.length;
		if (numPlayers % teams.length > 0)
			maxOnTeam++;

		if (requestedTeam.getPlayerMembers().size() >= maxOnTeam) {
			player.sendMessage(Component.text("This team is already full!").color(NamedTextColor.RED));
		} else {
			requestedTeam.addMembers(player);
			/*player.sendMessage(Component.text("You are now on ").color(NamedTextColor.GOLD)
					.append(requestedTeam.getComponentName()));
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 1f, 2f);*/
			informOfTeam(player);
		}
	}

	/**
	 * Called anytime a player changes teams.
	 */
	public void onTeamSwitch(Player player, @Nullable TeamArenaTeam oldTeam, @Nullable TeamArenaTeam newTeam) {
		for(Ability ability : Kit.getAbilities(player)) {
			ability.onTeamSwitch(player, oldTeam, newTeam);
		}

		KitDemolitions.DemolitionsAbility.teamSwitch(player, oldTeam, newTeam);
	}

	//switch a player between spectator and player
	// teams stuff only, practical changes happen in makeSpectator(Player)
	public void setSpectator(Player player, boolean spec, boolean shame) {
		if(spec) {
			players.remove(player);
			spectators.add(player);

			if (gameState == GameState.PREGAME) {
				final Component text = Component.text("You will spectate this game").color(NamedTextColor.GRAY);
				//player.showTitle(Title.title(Component.empty(), text));
				player.sendMessage(text);
			} else {
				//EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.VOID, 9999d);
				//DamageEvent dEvent = DamageEvent.createFromBukkitEvent(event, DamageType.SUICIDE);
				queueDamage(DamageEvent.newDamageEvent(player, 99999d, DamageType.SUICIDE, null, false));

				if(isRespawningGame()) {
					respawnTimers.remove(player); //if respawning game remove them from respawn queue
				}
				makeSpectator(player);

				if(shame) {
					Component text = player.displayName().append(Component.text(" has joined the spectators", NamedTextColor.GRAY));
					Bukkit.broadcast(text);
				}
			}
			//do after so it gets the correct displayName above
			spectatorTeam.addMembers(player);
		}
		//else they are (or set to be) a spectator
		// only re-set them as a player if the game hasn't started
		else {
			if(gameState == GameState.PREGAME) {
				spectators.remove(player);
				players.add(player);
				noTeamTeam.addMembers(player);
				final Component text = Component.text("No longer spectating this game").color(NamedTextColor.GRAY);
				player.sendMessage(text);
			}
			else {
				final Component text = Component.text("You can't rejoin after becoming a spectator").color(NamedTextColor.RED);
				player.sendMessage(text);
			}
		}
	}

	private void makeSpectator(Player player) {
		player.getInventory().clear();
		giveSpectatorItems(player);
		player.setAllowFlight(true);

		//hide all the spectators from everyone else
		for(Player p : Bukkit.getOnlinePlayers()) {
			p.hidePlayer(Main.getPlugin(), player);
		}
	}

	public void respawnPlayer(final Player player) {
		PlayerInfo pinfo = Main.getPlayerInfo(player);

		if(this.isRespawningGame()) {
			SpectatorAngelManager.removeAngel(player);
		}

		player.teleport(pinfo.team.getNextSpawnpoint());

		players.add(player);
		spectators.remove(player);

		player.setAllowFlight(false);
		PlayerUtils.resetState(player);

		givePlayerItems(player, pinfo, true);
		pinfo.kills = 0;
		PlayerListScoreManager.setKills(player, 0);

		StatusBarManager.showStatusBar(player, pinfo);

		//do this one (two?) tick later
		// when revealing first then teleporting, the clients interpolate the super fast teleport movement, so players
		// see them quickly zooming from wherever they were to their spawnpoint.
		// teleporting first in this method in the same tick creates this awful desync bug with positioning
		// so try teleport them, then reveal them 2 ticks later
		Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
			for(Player p : Bukkit.getOnlinePlayers()) {
				p.showPlayer(Main.getPlugin(), player);
			}
		}, 2);
	}

	public void handleDeath(DamageEvent event) {
		Component deathMessage = event.getDamageType().getDeathMessage(event.getVictim(), event.getFinalAttacker(), event.getDamageTypeCause());
		if(deathMessage != null) {
			Bukkit.broadcast(deathMessage);
		}
		Entity victim = event.getVictim();
		//if player make them a spectator and put them in queue to respawn if is a respawning game
		if(victim instanceof Player playerVictim) {
			PlayerUtils.sendTitle(playerVictim, Component.empty(), Component.text("You died!", TextColor.color(255, 0, 0)), 0, 30, 20);

			//Give out kill assists on the victim
			Entity killer = null;
			if(event.getFinalAttacker() instanceof Player finalAttacker) {
				killer = finalAttacker;
			}
			else {
				DamageTimes.DamageTime dTime = DamageTimes.getLastEntityDamageTime(playerVictim);
				if(dTime != null)
					killer = dTime.getGiver();
			}

			final PlayerInfo pinfo = Main.getPlayerInfo(playerVictim);
			//if not null and player
			if(killer instanceof Player playerKiller) {
				//killer's onKill ability
				for (Ability a : Kit.getAbilities(playerKiller)) {
					a.onKill(event);
				}
				attributeKillAndAssists(playerVictim, pinfo, playerKiller);
			}

			for(Ability a : Kit.getAbilities(playerVictim)) {
				a.onDeath(event);
			}
			pinfo.activeKit.removeKit(playerVictim, pinfo);

			this.killStreakManager.removeKillStreaks(playerVictim, pinfo);

			PlayerUtils.resetState(playerVictim);

			players.remove(playerVictim);
			spectators.add(playerVictim);

			makeSpectator(playerVictim);

			StatusBarManager.hideStatusBar(playerVictim, pinfo);

			DamageLogEntry.sendDamageLog(playerVictim);
			pinfo.clearDamageReceivedLog();

			//if they died in the void teleport them back to map
			// only for non-respawning games
			if(!this.isRespawningGame()) {
				if(playerVictim.getLocation().getY() <= border.getMinY()) {
					Location toTele;
					if (killer != null) {
						toTele = killer.getLocation();
					}
					else {
						toTele = spawnPos;
					}
					playerVictim.teleport(toTele);
				}
				SpectatorAngelManager.spawnAngel(playerVictim, false);
			}
			else {
				SpectatorAngelManager.spawnAngel(playerVictim, true);
			}

			//clear attack givers so they don't get falsely attributed on this next player's death
			DamageTimes.clearDamageTimes(playerVictim);

			if(this.isRespawningGame()) {
				respawnTimers.put(playerVictim, new RespawnInfo(gameTick));
				playerVictim.getInventory().addItem(kitMenuItem.clone());
			}
		}
		else if(victim instanceof Damageable dam) {
			dam.setHealth(0);
		}
	}

	private void attributeKillAndAssists(Player victim, PlayerInfo victimInfo, @Nullable Player finalDamager) {

		//the finalDamager always gets 1 kill no matter what
		if(finalDamager != null) {
			addKillAmount(finalDamager, 1, victim);
			victimInfo.getKillAssistTracker().removeAssist(finalDamager);
		}

		var iter = victimInfo.getKillAssistTracker().getIterator();
		while(iter.hasNext()) {
			Map.Entry<Player, Double> entry = iter.next();
			//convert the raw damage into decimal range 0 to 1
			// eg 10 damage (on player with 20 max health) = 0.5 kills
			double damageAmount = entry.getValue();
			damageAmount /= victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			addKillAmount(entry.getKey(), damageAmount, victim);
			iter.remove();
		}
	}

	/**
	 * give a player some kill assist amount or kill(s)
	 * relies on being called one at a time for each kill/death, and relies on amount not being greater than 1
	 */
	protected void addKillAmount(Player player, double amount, Player victim) {

		if(amount < 1)
			player.sendMessage(Component.text("Scored a kill assist of " + MathUtils.round(amount, 2) + "!", NamedTextColor.RED));

		PlayerInfo pinfo = Main.getPlayerInfo(player);
		int killsBefore = (int) pinfo.kills;
		pinfo.kills += amount;
		int killsAfter = (int) pinfo.kills;

		PlayerListScoreManager.setKills(player, killsAfter);

		//player kill Assist abilities
		for(Ability a : Kit.getAbilities(player)) {
			a.onAssist(player, amount, victim);
		}

		//if their number of kills increased to the next whole number
		// and if their kit gets killstreak bonuses by getting kills
		if(!pinfo.activeKit.handlesStreaksManually() && killsAfter != killsBefore) {
			this.killStreakManager.handleKill(player, killsAfter, pinfo);
		}
	}

	//todo: make a settable and changeable option (GameOption maybe)
	public abstract boolean isRespawningGame();

	public boolean canRespawn(Player player) {
		if(gameState != GameState.LIVE)
			return false;

		RespawnInfo rinfo = respawnTimers.get(player);
		if(rinfo == null)
			return false;

		int timeLeft = rinfo.deathTime;
		return gameTick - timeLeft >= RESPAWN_SECONDS * 20;
	}

	public void interruptRespawn(Player player) {
		if(gameState != GameState.LIVE)
			return;

		RespawnInfo rinfo = respawnTimers.get(player);
		if(rinfo != null && !rinfo.interrupted) {
			rinfo.interrupted = true;
			player.sendMessage(Component.text("Cancelled auto-respawn as you are choosing a new kit").color(TextColor.color(52, 247, 140)));
			player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 2f, 0.5f);
			player.getInventory().addItem(respawnItem);
		}
	}

	//set a player to respawn when their respawn time is up, only really needed if the respawn was interrupted
	public void setToRespawn(Player player) {
		RespawnInfo rinfo = respawnTimers.get(player);
		if(rinfo != null) {
			rinfo.interrupted = false;
			player.getInventory().removeItem(respawnItem);
		}
	}

	public boolean isSpectator(Player player) {
		//Main.logger().info("spectators contains player: " + spectators.contains(player));
		return spectators.contains(player);
	}

	public void giveSpectatorItems(Player player) {
		player.sendMap(miniMap.view);
		PlayerInventory inventory = player.getInventory();
		inventory.setItem(8, miniMap.getMapItem());
	}

	//process logging in player
	public void loggingInPlayer(Player player, PlayerInfo playerInfo) {
		Location toTeleport = spawnPos;
		if(gameState.isPreGame()) {
			if(gameState == GameState.TEAMS_CHOSEN || gameState == GameState.GAME_STARTING) {
				//cache the team and put them on it when they've joined
				TeamArenaTeam toJoin = addToLowestTeam(player, false);
				playerInfo.team = toJoin;
				if(gameState == GameState.GAME_STARTING) {
					//put them in next spawn point
					toTeleport = toJoin.getNextSpawnpoint();
				}
			}
			else if (gameState == GameState.PREGAME) {
				//noTeamTeam.addMembers(player);
				playerInfo.team = noTeamTeam;
			}
			players.add(player);
		}
		else {// if (gameState == GameState.LIVE){
			playerInfo.team = spectatorTeam; // necessary to initialize first so TeamArenaTeam#addMembers doesn't NPE.
			setSpectator(player, true, false);
		}

		if (playerInfo.kit == null) {
			playerInfo.kit = CommandDebug.filterKit(findKit(playerInfo.defaultKit));
			//default kit somehow invalid; maybe a kit was removed
			if (playerInfo.kit == null) {
				playerInfo.kit = CommandDebug.filterKit(kits.values().iterator().next());
				Main.logger().severe("PlayerInfo default kit somehow invalid in TeamArena#loggingInPlayer. Should" +
						" have been handled in EventListeners playerLogin.");
			}
		}

		//pass spawnpoint to the PlayerSpawnEvent
		playerInfo.spawnPoint = toTeleport;
	}

	public void joiningPlayer(Player player) {
		player.setGameMode(GameMode.SURVIVAL);
		player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(999999);
		this.sendGameAndMapInfo(player);
		if (gameState.isPreGame()) {
			//decided from loggingInPlayer(Player)
			final PlayerInfo pinfo = Main.getPlayerInfo(player);
			pinfo.team.addMembers(player);
			giveLobbyItems(player);
			StatusBarManager.showStatusBar(player, pinfo);
			if (gameState == GameState.TEAMS_CHOSEN || gameState == GameState.GAME_STARTING) {
				informOfTeam(player);
			}
			if (gameState == GameState.PREGAME || gameState == GameState.TEAMS_CHOSEN) {
				player.setAllowFlight(true);
			}
		} else if (gameState == GameState.LIVE) {
			//if it's a respawning game put them on a team and in the respawn queue
			if (this.isRespawningGame() && Main.getPlayerInfo(player).team == spectatorTeam) {
				handlePlayerJoinMidGame(player);
				respawnTimers.put(player, new RespawnInfo(gameTick));
				giveLobbyItems(player);
			}

			// Apply the spectator effects
			makeSpectator(player);

			/*for (TeamArenaTeam team : teams) {
				if (team.isAlive())
					player.showBossBar(team.bossBar);
			}*/
		}
	}

	public void leavingPlayer(Player player) {
		PlayerInfo pinfo = Main.getPlayerInfo(player);
		pinfo.team.removeMembers(player);
		// If they were a player and left during game then broadcast their quit.
		if(pinfo.activeKit != null) {
			pinfo.activeKit.removeKit(player, pinfo);
		}

		StatusBarManager.hideStatusBar(player, pinfo);

		players.remove(player);
		spectators.remove(player);
		SpectatorAngelManager.removeAngel(player);
		balancePlayerLeave();
		PlayerListScoreManager.removeScore(player);

		if(this.gameState == GameState.LIVE) {
			Bukkit.broadcast(player.playerListName().append(Component.text(" left the game", NamedTextColor.YELLOW)));
		}
	}

	public void informOfTeam(Player p) {
		TeamArenaTeam team = Main.getPlayerInfo(p).team;
		Component text = Component.text("You are on ", NamedTextColor.GOLD).append(team.getComponentName());
		PlayerUtils.sendTitle(p, Component.empty(), text, 10, 70, 20);
		if(gameState == GameState.TEAMS_CHOSEN) {
			final Component startConniving = Component.text("! Start scheming a game plan with /t!", NamedTextColor.GOLD);
			text = text.append(startConniving);
		}
		p.sendMessage(text);
		p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.AMBIENT, 2f, 0.5f);
	}

	//find an appropriate team to put player on at any point during game
	// boolean to actually put them on that team or just to get the team they would've been put on
	public TeamArenaTeam addToLowestTeam(Player player, boolean add) {
		int remainder = players.size() % teams.length;

		//find the lowest player count on any of the teams
		TeamArenaTeam lowestTeam = null;
		int count = Integer.MAX_VALUE;
		for(TeamArenaTeam team : teams) {
			if(team.getPlayerMembers().size() < count) {
				lowestTeam = team;
				count = team.getPlayerMembers().size();
			}
		}

		//if theres only 1 team that has 1 less player than the others
		// put them on that team
		// else, more than 1 team with the same low player count, judge them based on score if game is live
		//    else judge on lastLeft, or pick randomly if no lastLeft
		if(remainder != teams.length - 1)
		{

			//get all teams with that lowest player amount
			ArrayList<TeamArenaTeam> lowestTeams = new ArrayList<>(teams.length);
			for(TeamArenaTeam team : teams) {
				if(team.getPlayerMembers().size() == count) {
					lowestTeams.add(team);
				}
			}

			if(gameState == GameState.LIVE) {
				//shuffle them, and loop through and get the first one in the list that has the lowest score.
				Collections.shuffle(lowestTeams);
				int lowestScore = Integer.MAX_VALUE;
				for (TeamArenaTeam team : lowestTeams)
				{
					if (team.getTotalScore() < lowestScore)
					{
						lowestScore = team.getTotalScore();
						lowestTeam = team;
					}
				}
			}
			else if(lastHadLeft == null){
				lowestTeam = lowestTeams.get(MathUtils.randomMax(lowestTeams.size() - 1));
			}
			else {
				lowestTeam = lastHadLeft;
			}
		}

		if(add)
			lowestTeam.addMembers(player);

		return lowestTeam;
	}

	public void setLastHadLeft(TeamArenaTeam team) {
		if(team != noTeamTeam && team != spectatorTeam) {
			this.lastHadLeft = team;
		}
	}

	public void sendCountdown(boolean force) {
		if((gameTick - waitingSince) % 20 == 0 || force)
		{
			int timeLeft;
			//how long until teams are chosen
			if(gameState == GameState.PREGAME) {
				timeLeft = (waitingSince + PRE_TEAMS_TIME) - gameTick;
			}
			else {
				timeLeft = (waitingSince + TOTAL_WAITING_TIME) - gameTick;
			}
			timeLeft /= 20;
			//is a multiple of 30, is 15, is between 10 and 1 inclusive , AND is not 0
			// OR is just forced
			if(((timeLeft % 30 == 0 || timeLeft == 15 || timeLeft == 10 ||
					(timeLeft <= 5 && timeLeft >= 1 && gameState == GameState.GAME_STARTING)) && timeLeft != 0) || force)
			{
				String s;
				if(gameState == GameState.PREGAME)
					s = "Teams will be chosen in ";
				else
					s = "Game starting in ";

				Bukkit.broadcast(Component.text(s + timeLeft + 's').color(NamedTextColor.RED));

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(p.getLocation(), Sound.ENTITY_CREEPER_DEATH, SoundCategory.AMBIENT, 10, 0);
				}
			}
		}
	}

	public void sendGameAndMapInfo(Player player) {
		player.sendMessage(Component.textOfChildren(
				Component.text("GameType: ", NamedTextColor.GOLD),
				this.getGameName(),
				Component.newline(),
				this.gameMap.getMapInfoComponent()
		));
	}

	public void loadConfig(TeamArenaMap map) {
		Main.logger().info("Loading map config data");
		Main.logger().info(map.toString());

		//Map border
		// Only supports rectangular prism borders as of now
		Vector minCorner = map.getMinBorderCorner();
		Vector maxCorner = map.getMaxBorderCorner();
		border = BoundingBox.of(minCorner, maxCorner);

		//calculate spawnpoint based on map border
		Vector centre = border.getCenter();
		int y = gameWorld.getHighestBlockYAt(centre.getBlockX(), centre.getBlockZ());
		int worldSpawnY = gameWorld.getSpawnLocation().getBlockY();
		if(Math.abs(centre.getY() - y) < Math.abs(centre.getY() - worldSpawnY)) {
			centre.setY(y);
		}
		else {
			centre.setY(worldSpawnY);
		}
		spawnPos = centre.toLocation(gameWorld, 90, 0);
		spawnPos.setY(spawnPos.getY() + 2);

		//if both Y are 0 then have no ceiling
		// do this after spawnpoint calculation otherwise it's trouble
		if(!map.hasVerticalBorder()) {
			minCorner.setY(gameWorld.getMinHeight() - 20);
			maxCorner.setY(gameWorld.getMaxHeight() + 50);
			border = BoundingBox.of(minCorner, maxCorner);
		}

		//Create the teams
		int numOfTeams = map.getTeamSpawns().size();
		teams = new TeamArenaTeam[numOfTeams];
		int teamsArrIndex = 0;

		for (Map.Entry<String, Vector[]> entry : map.getTeamSpawns().entrySet()) {
			String teamName = entry.getKey();

			//if it's a legacy RWF team
			//TeamColours teamColour = TeamColours.valueOf(teamName);
			TeamArenaTeam teamArenaTeam = LegacyTeams.fromRWF(teamName);
			if (teamArenaTeam == null) {
				throw new IllegalArgumentException("Bad team name! Use RED, BLUE, DARK_GRAY, etc.");

				// RGB teams are finally dead
				//it's not a legacy rwf team

				/*String simpleName = teamName;
				if(spawnsYaml.containsKey("SimpleName")) {
					simpleName = spawnsYaml.get("SimpleName").get(0);
				}

				ArrayList<String> coloursInfo = spawnsYaml.get("Colour");
				boolean isGradient = coloursInfo.get(0).equals("GRADIENT");
				Color first = TeamArenaTeam.parseString(coloursInfo.get(1));
				Color second = null;
				if(isGradient) {
					second = TeamArenaTeam.parseString(coloursInfo.get(2));
				}

				//probably do later: seperate choosable name and simple names for non-legacy teams
				// and also choosable hats
				// and dye color
				teamArenaTeam = new TeamArenaTeam(teamName, simpleName, first, second, null);*/
			}

			Vector[] spawnVecs = entry.getValue();
			Location[] locArray = new Location[spawnVecs.length];

			int index = 0;
			for (Vector vec : spawnVecs) {
				Location location = vec.toLocation(gameWorld);
				Vector direction = centre.clone().setY(0).subtract(location.toVector().setY(0));

				direction.normalize();
				location.setDirection(direction);
				//in case location is same as map centre
				if (!Float.isFinite(location.getPitch())) {
					location.setPitch(90f);
				}
				if (!Float.isFinite(location.getYaw())) {
					location.setYaw(0f);
				}

				locArray[index] = location;
				index++;
			}
			teamArenaTeam.setSpawns(locArray);
			teams[teamsArrIndex] = teamArenaTeam;
			teamsArrIndex++;
		}
	}

	public void setGameState(GameState gameState) {
		this.gameState = gameState;
		Main.logger().info("GameState: " + gameState);
	}

	public boolean canAttack(Player one, Player two) {
		TeamArenaTeam team = Main.getPlayerInfo(one).team;
		//if two is on the same team as one
		if(team.getPlayerMembers().contains(two)) {
			return false;
		}
		return true;
	}

	public boolean canHeal(Player medic, LivingEntity target) {
		if(medic == target)
			return false;

		if (target instanceof Player pTarget && !Main.getPlayerInfo(pTarget).team.getPlayerMembers().contains(medic)) {
			return false;
		}

		return true;
	}

	public boolean canSeeStatusBar(Player player, Player viewer) {
		TeamArenaTeam viewersTeam = Main.getPlayerInfo(viewer).team;
		return viewersTeam == Main.getGame().spectatorTeam || viewersTeam.getPlayerMembers().contains(player);
	}

	public boolean isTeamHotbarItem(ItemStack item) {
		for(TeamArenaTeam team : teams) {
			if(team.getHotbarItem().isSimilar(item))
				return true;
		}

		return false;
	}

	public boolean isWearableArmorPiece(ItemStack item) {
		return !isTeamHotbarItem(item);
	}

	public void queueDamage(DamageEvent event) {
		if(this.gameState == GameState.LIVE && !isDead(event.getVictim()))
			damageQueue.add(event);
	}

	public void queueUnsafeDamage(DamageEvent event) {
		damageQueue.add(event);
	}

	public Set<Player> getPlayers() {
		return players;
	}

	public TeamArenaTeam getSpectatorTeam() {
		return spectatorTeam;
	}

	public ArrayList<String> getTabTeamsList() {
		return tabTeamsList;
	}

	public Collection<String> getTabKitList() {
		return kits.keySet();
	}

	public abstract Component getGameName();

	public File getMapPath() {
		return new File("Maps");
	}

	public World getWorld() {
		return gameWorld;
	}

	public BoundingBox getBorder() {
		return border;
	}

	public Location getSpawnPos() {
		return this.spawnPos != null ? this.spawnPos.clone() : null;
	}

	/**
	 * for use in configs
	 */
	protected TeamArenaTeam getTeamByName(String name) {
		for(TeamArenaTeam team : teams) {
			if(team.getName().equalsIgnoreCase(name)) {
				return team;
			}
		}

		return null;
	}

	//Get by config names like "RED", "BLUE" etc.
	protected TeamArenaTeam getTeamByLegacyConfigName(String name) {
		name = name.replace('_', ' ');
		for(TeamArenaTeam team : this.teams) {
			if(team.getSimpleName().equalsIgnoreCase(name))
				return team;
		}

		return null;
	}

	public GameState getGameState() {
		return gameState;
	}

	public static int getGameTick() {
		return gameTick;
	}
}

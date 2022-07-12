package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.inventory.KitInventory;
import me.toomuchzelda.teamarenapaper.inventory.SpectateInventory;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.damage.*;
import me.toomuchzelda.teamarenapaper.teamarena.kits.*;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions.KitDemolitions;
import me.toomuchzelda.teamarenapaper.teamarena.kits.engineer.KitEngineer;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
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
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//main game class
public abstract class TeamArena
{
	public static GameType nextGameType = GameType.CTF;
	@Nullable
	public static String nextMapName = null;

	private final File worldFile;
	public World gameWorld;

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
	protected Set<Player> players;
	protected Set<Player> spectators;
	/**
	 * for mid-game joiners with whatever amount of time to decide if they wanna join and dead players
	 * -1 value means ready to respawn when next liveTick runs (magic number moment)
	 */
	protected Map<Player, RespawnInfo> respawnTimers;
	protected static ItemStack respawnItem = ItemBuilder.of(Material.RED_DYE)
			.displayName(Component.text("Right click to respawn", NamedTextColor.RED))
			.build();
	public static final int RESPAWN_SECONDS = 5;
	public static final int MID_GAME_JOIN_SECONDS = 10;

	protected final List<Kit> defaultKits = List.of(new KitTrooper(), new KitArcher(), new KitGhost(), new KitDwarf(),
			new KitBurst(), new KitJuggernaut(), new KitNinja(), new KitPyro(), new KitSpy(), new KitDemolitions(),
			new KitNone(), new KitVenom(), new KitRewind(), new KitValkyrie(), new KitEngineer());

	protected Map<String, Kit> kits;
	protected static ItemStack kitMenuItem = ItemBuilder.of(Material.FEATHER)
			.displayName(Component.text("Select a Kit", NamedTextColor.BLUE))
			.build();

	public static final Component OWN_TEAM_PREFIX = Component.text("▶ ");
	public static final Component OWN_TEAM_PREFIX_DANGER = OWN_TEAM_PREFIX.color(NamedTextColor.RED);

	protected MapInfo mapInfo;

	protected Queue<DamageEvent> damageQueue;

	private final List<DamageIndicatorHologram> activeDamageIndicators = new LinkedList<>();

	public final MiniMapManager miniMap;

	public TeamArena() {
		Main.logger().info("Reading info from " + getMapPath().getPath() + ':');
		File[] maps = getMapPath().listFiles();
		if (maps == null || maps.length == 0) {
			throw new IllegalStateException(getMapPath().getAbsolutePath() + " is empty");
		}

		//copy the map to another directory and load from there to avoid any accidental modifying of the original
		// map
		File source;
		if (nextMapName != null) {
			source = new File(getMapPath(), nextMapName);
			nextMapName = null;
			if (!source.exists()) {
				throw new IllegalStateException("Map " + source.getName() + " does not exist!");
			}
		} else {
			source = maps[MathUtils.random.nextInt(maps.length)];
		}
		Main.logger().info("Loading map: " + source.getAbsolutePath());
		File dest = new File("temp_" + source.getName().toLowerCase(Locale.ENGLISH) + "_" + System.currentTimeMillis());
		if (dest.mkdir()) {
			FileUtils.copyFolder(source, dest);
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
		worldFile = dest;
		WorldCreator worldCreator = new WorldCreator(dest.getName());
		gameWorld = worldCreator.createWorld();

		//parse config before world gamerules to know world options
		File configFile = new File(source, "config.yml");
		Yaml yaml = new Yaml();
		Main.logger().info("Reading config YAML: " + configFile);


		try (var fileStream = new FileInputStream(configFile)) {
			Map<String, Object> map = yaml.load(fileStream);
			parseConfig(map);
		} catch (IOException e) {
			e.printStackTrace();
		}

		gameWorld.setSpawnLocation(spawnPos);
		gameWorld.setAutoSave(false);
		gameWorld.setGameRule(GameRule.DISABLE_RAIDS, true);
		gameWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS,false);
		gameWorld.setGameRule(GameRule.DO_INSOMNIA,	false);
		gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, mapInfo.doDaylightCycle);
		gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, mapInfo.doWeatherCycle);
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

		if (mapInfo.weatherType == 2)
			gameWorld.setThundering(true);
		else if (mapInfo.weatherType == 1)
			gameWorld.setStorm(true);
		else
			gameWorld.setClearWeatherDuration(6000); //5 minutes

		waitingSince = gameTick;
		//gameState = GameState.PREGAME;
		setGameState(GameState.PREGAME);

		noTeamTeam = new TeamArenaTeam("No Team", "No Team", Color.YELLOW, Color.ORANGE, DyeColor.YELLOW, null, Material.GRASS_BLOCK);
		spectatorTeam = new TeamArenaTeam("Spectators", "Specs", TeamArenaTeam.convert(NamedTextColor.DARK_GRAY), null,
				null, null, Material.GLASS);
		winningTeam = null;
		lastHadLeft = null;

		kits = new LinkedHashMap<>();
		registerKits();

		//List of team names
		tabTeamsList = new ArrayList<>(teams.length);
		for(TeamArenaTeam team : teams) {
			tabTeamsList.add(team.getSimpleName());
		}

		players = ConcurrentHashMap.newKeySet();
		spectators = ConcurrentHashMap.newKeySet();
		respawnTimers = new LinkedHashMap<>();
		damageQueue = new LinkedList<>();

		PlayerListScoreManager.removeScores();

		miniMap = new MiniMapManager(this);

		DamageTimes.clear();

		//init all the players online at time of construction
		for (var entry : Main.getPlayerInfoMap().entrySet()) {
			Player p = entry.getKey();
			PlayerInfo pinfo = entry.getValue();

			p.teleport(spawnPos);
			players.add(p);

			pinfo.spawnPoint = spawnPos;
			pinfo.kit = findKit(pinfo.defaultKit);
			pinfo.team = noTeamTeam;
			pinfo.clearDamageReceivedLog();
			pinfo.getKillAssistTracker().clear();
			pinfo.kills = 0;
			noTeamTeam.addMembers(p);

			if(pinfo.kit == null)
				pinfo.kit = kits.values().iterator().next();

			PlayerUtils.resetState(p);
			p.setAllowFlight(true);

			p.getInventory().clear();
			giveLobbyItems(p);

			mapInfo.sendMapInfo(p);
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
		for (Player player : gameWorld.getPlayers()) {
			player.kick(Component.text("You have been evacuated!", NamedTextColor.YELLOW));
		}
		if (Bukkit.unloadWorld(gameWorld, false)) {
			FileUtils.delete(worldFile);
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
	}

	public void liveTick() {
		BuildingManager.tick();

		//checking team states (win/lose) done in liveTick() per-game

		//process players waiting to respawn if a respawning game
		if(isRespawningGame()) {
			respawnerTick();
		}

		//ability tick 'events'
		for (Kit kit : kits.values()) {
			for (Ability ability : kit.getAbilities()) {
				ability.onTick();
			}

			for (Player p : kit.getActiveUsers()) {
				for (Ability a : kit.getAbilities()) {
					a.onPlayerTick(p);
				}
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
		if (!team.isAlive()) {
			for (Player viewer : Bukkit.getOnlinePlayers()) {
				viewer.showBossBar(team.bossBar);
			}
		}
		team.addMembers(player);

		informOfTeam(player);
	}

	public void damageTick() {
		Iterator<DamageEvent> iter = damageQueue.iterator();
		while(iter.hasNext()) {
			DamageEvent event = iter.next();
			iter.remove();

			onDamage(event);
			if(event.isCancelled())
				continue;

			//ability pre-attack events
			if(event.getFinalAttacker() instanceof Player p) {
				Ability[] abilities = Kit.getAbilities(p);
				for(Ability ability : abilities) {
					ability.onAttemptedAttack(event);
				}
			}
			if(event.getVictim() instanceof Player p) {
				Ability[] abilities = Kit.getAbilities(p);
				for(Ability ability : abilities) {
					ability.onAttemptedDamage(event);
				}
			}

			//ability on confirmed attacks done in this.onConfirmedDamage() called by DamageEvent.executeAttack()
			if(event.getFinalAttacker() instanceof Player p && event.getVictim() instanceof Player p2) {
				if(!canAttack(p, p2))
					continue;
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
			Ability[] abilities = Kit.getAbilities(p);
			for(Ability ability : abilities) {
				ability.onDealtAttack(event);
			}
			playerCause = p;
		}
		if(!event.isCancelled()) {
			if (event.getVictim() instanceof Player p) {
				Ability[] abilities = Kit.getAbilities(p);
				for (Ability ability : abilities) {
					ability.onReceiveDamage(event);
				}

				if (!event.isCancelled()) {
					PlayerInfo pinfo = Main.getPlayerInfo(p);
					//spawn damage indicator hologram
					// divide by two to display as hearts
					Component damageText = Component.text(MathUtils.round(event.getFinalDamage() / 2, 2), pinfo.team.getRGBTextColor(), TextDecoration.BOLD);
					Location spawnLoc = p.getLocation();
					spawnLoc.add(0, MathUtils.randomRange(1.4, 2), 0);
					DamageIndicatorHologram hologram = new DamageIndicatorHologram(spawnLoc, PlayerUtils.getDamageIndicatorViewers(p, playerCause), damageText);
					//activeDamageIndicators.add(hologram);

					//add to their damage log
					pinfo.logDamageReceived(p, event.getDamageType(), event.getFinalDamage(), event.getFinalAttacker(), gameTick);

					if (event.getFinalAttacker() instanceof Player attacker) {
						pinfo.getKillAssistTracker().addDamage(attacker, event.getFinalDamage());
					}
				}
			} else if(event.getVictim() instanceof Axolotl) {
				KitDemolitions.DemolitionsAbility.handleAxolotlDamage(event);
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

		if(event.getVictim() instanceof Axolotl) {
			KitDemolitions.DemolitionsAbility.handleAxolotlAttemptDamage(event);
		}

		if(event.getVictim() instanceof Skeleton) {
			KitEngineer.EngineerAbility.handleSentryAttemptDamage(event);
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
			if (miniMap.isMapItem(event.getItem())) {
				event.setUseItemInHand(Event.Result.DENY);
				Inventories.openInventory(clicker, new SpectateInventory(isSpectator(clicker) ? null : team));
				return;
			}
			//right click to glow teammates, left click to ping to nearby teammates
			if(team.getHotbarItem().isSimilar(event.getItem())) {
				Action action = event.getAction();
				if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
					event.setUseItemInHand(Event.Result.DENY);
					setViewingGlowingTeammates(pinfo, !pinfo.viewingGlowingTeammates, true);
				}
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

		Bukkit.broadcast(Component.text("Teams have been decided!").color(NamedTextColor.RED));
		for (Player p : players) {
			informOfTeam(p);
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
			int i = 0;
			Location[] spawns = team.getSpawns();
			for(Entity e : team.getPlayerMembers()) {
				if(e instanceof Player p)
					p.setAllowFlight(false);

				e.teleport(spawns[i % spawns.length]);
				team.spawnsIndex++;
				i++;
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

		Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			Player player = entry.getKey();

			PlayerUtils.resetState(player);
			player.setSaturatedRegenRate(0);
			PlayerListScoreManager.setKills(player, 0);

			givePlayerItems(player, entry.getValue(), true);

			for(TeamArenaTeam team : teams) {
				if(team.isAlive())
					player.showBossBar(team.bossBar);

				team.bossBar.progress(0); //init to 0, normally is 1
			}

			player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.AMBIENT, 2, 1);
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
			pinfo.team = null;
			pinfo.spawnPoint = null;
		}

		for(Kit kit : kits.values()) {
			for(Ability ability : kit.getAbilities()) {
				ability.unregisterAbility();
			}
		}

		miniMap.cleanUp();

		players.clear();
		spectators.clear();
		respawnTimers.clear();
		damageQueue.clear();

		setGameState(GameState.END);
		Bukkit.broadcastMessage("Game end");
	}


	public void prepDead() {
		for (TeamArenaTeam team : teams) {
			//team.removeAllMembers();
			team.unregister();
		}
		//spectatorTeam.removeAllMembers();
		spectatorTeam.unregister();
		noTeamTeam.unregister();

		for(Player p : Bukkit.getOnlinePlayers()) {
			PlayerUtils.resetState(p);
			for(TeamArenaTeam team : teams) {
				p.hideBossBar(team.bossBar);
			}
		}
		// remove map
		miniMap.removeMapView();

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
					Entity removed = team.lastIn.peek();
					//team.removeMembers(removed);
					noTeamTeam.addMembers(removed);
					if(removed instanceof Player p) {
						p.sendMessage(Component.text("A player left, so you were removed from your chosen team for balance. Sorry!").color(NamedTextColor.AQUA));
						p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_HURT, SoundCategory.AMBIENT, 2f, 1f);
					}
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

	public abstract boolean canSelectKitNow();

	public abstract boolean canSelectTeamNow();

	public abstract boolean canTeamChatNow(Player player);

	public void selectKit(@NotNull Player player, @NotNull Kit kit) {
		if (!canSelectKitNow()) {
			player.sendMessage(Component.text("You can't choose a kit right now").color(NamedTextColor.RED));
			return;
		}
		Main.getPlayerInfo(player).kit = kit;
		player.sendMessage(Component.text("Using kit " + kit.getName()).color(NamedTextColor.BLUE));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1f, 2f);
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
				//todo: kill the player here (remove from game)
				//EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.VOID, 9999d);
				//DamageEvent dEvent = DamageEvent.createFromBukkitEvent(event, DamageType.SUICIDE);
				queueDamage(DamageEvent.newDamageEvent(player, 99999d, DamageType.SUICIDE, null, false));

				if(isRespawningGame()) {
					respawnTimers.remove(player); //if respawning game remove them from respawn queue
					makeSpectator(player);
				}

				if(shame) {
					Component text;
					if(MathUtils.randomMax(128) == 128) {
						text = player.displayName().append(Component.text(" baby raged off the game").color(NamedTextColor.GRAY));
					}
					else {
						text = player.displayName().append(Component.text(" has joined the spectators").color(NamedTextColor.GRAY));
					}
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

	public void respawnPlayer(Player player) {
		PlayerInfo pinfo = Main.getPlayerInfo(player);
		player.teleport(pinfo.team.getNextSpawnpoint());

		players.add(player);
		spectators.remove(player);

		player.setAllowFlight(false);
		PlayerUtils.resetState(player);

		givePlayerItems(player, pinfo, true);
		pinfo.kills = 0;
		PlayerListScoreManager.setKills(player, 0);

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

			PlayerInfo pinfo = Main.getPlayerInfo(playerVictim);
			//if not null and player
			if(killer instanceof Player playerKiller) {
				//killer's onKill ability
				for (Ability a : Kit.getAbilities(playerKiller)) {
					a.onKill(event);
				}
				attributeKillAndAssists(playerVictim, pinfo, playerKiller);
			}

			for(Ability a : pinfo.activeKit.getAbilities()) {
				a.onDeath(event);
			}
			pinfo.activeKit.removeKit(playerVictim, pinfo);

			PlayerUtils.resetState(playerVictim);

			players.remove(playerVictim);
			spectators.add(playerVictim);

			makeSpectator(playerVictim);

			DamageLogEntry.sendDamageLog(playerVictim);
			pinfo.clearDamageReceivedLog();

			//if they died in the void teleport them back to map
			if(playerVictim.getLocation().getY() <= border.getMinY()) {
				Location toTele;
				if(killer != null) {
					toTele = killer.getLocation();
				}
				else {
					toTele = spawnPos;
				}
				playerVictim.teleport(toTele);
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

		if(amount != 1)
			player.sendMessage(Component.text("Scored a kill assist of " + MathUtils.round(amount, 2) + "!").color(NamedTextColor.RED));

		PlayerInfo pinfo = Main.getPlayerInfo(player);
		int killsBefore = (int) pinfo.kills;
		pinfo.kills += amount;
		int killsAfter = (int) pinfo.kills;

		PlayerListScoreManager.setKills(player, killsAfter);

		//player kill Assist abilities
		Ability[] abilities = Kit.getAbilities(player);
		for(Ability a : abilities) {
			a.onAssist(player, amount, victim);
		}

		if(killsAfter != killsBefore) { //if their number of kills increased to the next whole number
			//todo: check for and give killstreaks here
			final double someKillStreakAmount = 5;
			if(killsAfter == someKillStreakAmount) {
				player.sendMessage("5 killstreak");
				//give them killstreak item(s)
			}
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
		//todo: make async
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
			playerInfo.team = spectatorTeam;
		}

		if(playerInfo.kit == null) {
			playerInfo.kit = findKit(playerInfo.defaultKit);
			//default kit somehow invalid; maybe a kit was removed
			if(playerInfo.kit == null) {
				playerInfo.kit = kits.values().iterator().next();
			}
		}

		//pass spawnpoint to the PlayerSpawnEvent
		playerInfo.spawnPoint = toTeleport;
	}

	public void joiningPlayer(Player player) {
		player.setGameMode(GameMode.SURVIVAL);
		player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(999999);
		mapInfo.sendMapInfo(player);
		if (gameState.isPreGame()) {
			//decided from loggingInPlayer(Player)
			Main.getPlayerInfo(player).team.addMembers(player);
			giveLobbyItems(player);
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
			}

			//make sure to hide them as they are still a spectator
			for(Player p : Bukkit.getOnlinePlayers()) {
				p.hidePlayer(Main.getPlugin(), player);
			}

			for (TeamArenaTeam team : teams) {
				if (team.isAlive())
					player.showBossBar(team.bossBar);
			}
		}
	}

	public void leavingPlayer(Player player) {
		PlayerInfo pinfo = Main.getPlayerInfo(player);
		pinfo.team.removeMembers(player);
		if(pinfo.activeKit != null) {
			pinfo.activeKit.removeKit(player, pinfo);
		}
		players.remove(player);
		spectators.remove(player);
		balancePlayerLeave();
		PlayerListScoreManager.removeScore(player);
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
			long timeLeft;
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

	public void parseConfig(Map<String, Object> map) {
		//basic info
		mapInfo = new MapInfo();
		mapInfo.gameType = getGameName();
		mapInfo.name = (String) map.get("Name");
		mapInfo.author = (String) map.get("Author");
		mapInfo.description = (String) map.get("Description");

		try {
			mapInfo.doDaylightCycle = (boolean) map.get("DoDaylightCycle");
		}
		//the element doesn't exist, or spelled incorrectly and recognized by snakeyaml as a String instead of a boolean
		catch(NullPointerException | ClassCastException e) {
			mapInfo.doDaylightCycle = false;
			//e.printStackTrace();
		}

		try {
			String weather = (String) map.get("Weather");

			if(weather.equalsIgnoreCase("DOWNPOUR"))
				mapInfo.weatherType = 1;
			else if(weather.equalsIgnoreCase("THUNDER"))
				mapInfo.weatherType = 2;
			else
				mapInfo.weatherType = 0;

		}
		catch(NullPointerException | ClassCastException e) {
			mapInfo.weatherType = 0;
			//e.printStackTrace();
		}

		try {
			mapInfo.doWeatherCycle = (boolean) map.get("DoWeatherCycle");
		}
		catch(NullPointerException | ClassCastException e) {
			mapInfo.doWeatherCycle = false;
			//e.printStackTrace();
		}

		//Map border
		// Only supports rectangular prism borders as of now
		ArrayList<String> borders = (ArrayList<String>) map.get("Border");
		Vector vec1 = BlockUtils.parseCoordsToVec(borders.get(0), 0, 0, 0);
		Vector vec2 = BlockUtils.parseCoordsToVec(borders.get(1), 0, 0, 0);
		Vector corner1 = Vector.getMinimum(vec1, vec2);
		Vector corner2 = Vector.getMaximum(vec1, vec2).add(new Vector(1, 1, 1));
		border = BoundingBox.of(corner1, corner2);

		//calculate spawnpoint based on map border
		Vector centre = border.getCenter();
			/*Vec spawnpoint = BlockStuff.getFloor(centre, instance);
			//if not safe to spawn just spawn them in the sky
			if(spawnpoint == null) {
				spawnpoint = new Vec(centre.x(), 255, centre.z());
			}*/
		int y = gameWorld.getHighestBlockYAt(centre.getBlockX(), centre.getBlockZ());
		int worldSpawnY = gameWorld.getSpawnLocation().getBlockY();

		if(Math.abs(centre.getY() - y) < Math.abs(centre.getY() - worldSpawnY)) {
			centre.setY(y);
		}
		else {
			centre.setY(worldSpawnY);
		}

		//centre.setY(y);

		spawnPos = centre.toLocation(gameWorld, 90, 0);
		spawnPos.setY(spawnPos.getY() + 2);


		//if both Y are 0 then have no ceiling
		// do this after spawnpoint calculation otherwise it's trouble
		if(vec1.getY() == vec2.getY()) {
			corner1.setY(gameWorld.getMinHeight());
			corner2.setY(gameWorld.getMaxHeight());
			border = BoundingBox.of(corner1, corner2);
		}

		//Create the teams
		Map<String, Map<String, ArrayList<String>>> teamsMap =
				(Map<String, Map<String, ArrayList<String>>>) map.get("Teams");

		int numOfTeams = teamsMap.size();
		teams = new TeamArenaTeam[numOfTeams];
		int teamsArrIndex = 0;

		for (Map.Entry<String, Map<String, ArrayList<String>>> entry : teamsMap.entrySet()) {
			String teamName = entry.getKey();

			Map<String, ArrayList<String>> spawnsYaml = entry.getValue();

			//if it's a legacy RWF team
			//TeamColours teamColour = TeamColours.valueOf(teamName);
			TeamArenaTeam teamArenaTeam = LegacyTeams.fromRWF(teamName);
			if (teamArenaTeam == null) {
				Main.logger().severe("RGB teams are dead!");
				return;

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

			ArrayList<String> spawnsList = spawnsYaml.get("Spawns");
			Location[] locArray = new Location[spawnsList.size()];

			int index = 0;
			for (String loc : spawnsList) {
				Vector coords = BlockUtils.parseCoordsToVec(loc, 0.5, 0, 0.5);
				Location location = coords.toLocation(gameWorld);
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
		if(!isDead(event.getVictim()))
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

	public File getWorldFile() {
		return worldFile;
	}

	public GameState getGameState() {
		return gameState;
	}

	public static int getGameTick() {
		return gameTick;
	}
}

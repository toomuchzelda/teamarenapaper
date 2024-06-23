package me.toomuchzelda.teamarenapaper.teamarena.kingofthehill;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapPalette;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.*;

public class KingOfTheHill extends TeamArena
{
	public static final int ANTI_STALL_ROTATION_TIME = 5 * 60 * 20;
	public static final int ANTI_STALL_HALVE_TIME = 120 * 20;
	protected boolean randomHillOrder;
	protected Hill[] hills;
	protected Hill activeHill;
	protected Hill lastActiveHill; // for minimap
	protected int hillIndex;
	protected int lastHillChangeTime;

	protected HashMap<TeamArenaTeam, Float> hillCapProgresses;
	protected Map<TeamArenaTeam, Float> hillCapChange = new HashMap<>();
	protected TeamArenaTeam owningTeam;
	//teams can earn max 1 point per tick
	// for every team, field score is used for the score they've earned on the current hill,
	// and field score2 is used for their total score excluding current hill
	// every time the hill changes the amount of points they earned on that one is put onto their score2
	// and score is cleared
	public static final float INITIAL_CAP_TIME = 10 * 20;
	// Amount that owning players on hill reduces other team's cap progress from -1 to 0
	private static final float KING_REDUCE_RATIO = -0.4f;
	public float ticksAndPlayersToCaptureHill = INITIAL_CAP_TIME;
	//the total score u need to get to win
	public final int TICKS_TO_WIN;

	public static final Component GAME_NAME = Component.text("King of the Hill", NamedTextColor.YELLOW);
	public static final Component HOW_TO_PLAY = Component.text("Stand on the active hill to capture it! The first team to be King for enough time wins!", NamedTextColor.YELLOW);

	private final Map<TeamArenaTeam, SidebarManager.SidebarEntry> sidebarCache = new LinkedHashMap<>();

	public KingOfTheHill(TeamArenaMap map) {
		super(map);

		hillCapProgresses = new HashMap<>();
		hillIndex = 0;
		lastHillChangeTime = gameTick;

		int toWin = 0;
		for(Hill h : hills) {
			toWin += h.getTime() * 20;
		}
		TICKS_TO_WIN = toWin;
	}

	@Override
	public void preGameTick() {
		super.preGameTick();

		for(Hill hill : hills) {
			hill.playParticles(MathUtils.randomColor());
		}
	}

	@Override
	public void liveTick() {

		//count how many players of the hill owning team are on the hill to subtract against other capper's points
		float numOwningPlayers = 0;
		if(owningTeam != null) {
			for(Entity e : owningTeam.getPlayerMembers()) {
				if(e instanceof Player p && isSpectator(p))
					continue;

				if(activeHill.getBorder().contains(e.getBoundingBox())) {
					numOwningPlayers++;
					// Make players on the hill glow.
					//e.setGlowing(true);
					hillGlow(e, true);
				}
				else {
					//e.setGlowing(false);
					hillGlow(e, false);
				}
			}

			float max = (float) (owningTeam.getPlayerMembers().size() * 0.7);
			if(numOwningPlayers > max)
				numOwningPlayers = max;
		}

		//find how many team members of each team are on the active hill and add them to the cap progress
		// also see if a new team has taken over
		// also see which teams are on thing to determine which colours the active Hill should play
		LinkedList<Color> coloursList = new LinkedList<>();
		TeamArenaTeam newOwningTeam = null;
		//band-aid, store the rate of earning in here for each team to be accessed later in this liveTick method
		// to display the rate on th sidebar
		hillCapChange.clear();
		float newOwningTeamsPoints = 0;
		for(TeamArenaTeam team : teams) {
			if(team != owningTeam && team.isAlive()) {
				float numPlayers = 0;
				Float points = hillCapProgresses.get(team);
				if(points == null || points < 0f)
					points = 0f;

				for (Entity member : team.getPlayerMembers()) {
					if(member instanceof Player p && isSpectator(p))
						continue;

					if (activeHill.getBorder().contains(member.getBoundingBox())) {
						numPlayers++;
						//member.setGlowing(true);
						hillGlow(member, true);
					}
					else {
						//member.setGlowing(false);
						hillGlow(member, false);
					}
				}

				float toEarn;
				if(numPlayers > 0) {
					coloursList.add(team.getColour());
					if (team.getSecondColour() != null)
						coloursList.add(team.getSecondColour());
					//do it twice for correct ratio of colours
					else
						coloursList.add(team.getColour());

					//70% of the team be on Hill for max points, any more doesn't grant more
					toEarn = numPlayers / ((float) team.getPlayerMembers().size() * 0.7f);

					if(toEarn > 1f)
						toEarn = 1f;
				}
				//slowly decrement if no teammates on the hill
				else if(points > 0) {
					toEarn = -0.1f;
				}
				else
					toEarn = 0;

				//decrease gain speed for every owning team player on the hill concurrently
				toEarn += numOwningPlayers * KING_REDUCE_RATIO;
				hillCapChange.put(team, toEarn);
				points += toEarn;

                if(points < 0f)
                    points = 0f;

				//Bukkit.broadcastMessage(team.getName() + " points: " + points + ", toEarn: " + toEarn + "numPlayers: " + numPlayers + " numOwningPlayers: " + numOwningPlayers);
				//Bukkit.broadcastMessage("entityMembers size: " + team.getEntityMembers().size());

				//a team has capped
				// do comparisons in case two teams cap in one tick, do the one with more progress over the limit
				if(points >= ticksAndPlayersToCaptureHill && points > newOwningTeamsPoints) {
					newOwningTeam = team;
					newOwningTeamsPoints = points;
				}

				hillCapProgresses.put(team, points);
			}
		}

		if(newOwningTeam != null) {
			//change the owning team here
			Component capturedMsg = newOwningTeam.getComponentSimpleName()
					.append(Component.text(" has captured ").color(NamedTextColor.GOLD)
							.append(Component.text(activeHill.getName()).color(NamedTextColor.GOLD)
									.append(Component.text('!').color(NamedTextColor.GOLD))));
			Bukkit.broadcast(capturedMsg);

			Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
			while(iter.hasNext()) {
				Map.Entry<Player, PlayerInfo> entry = iter.next();
				Player p = entry.getKey();
				p.playSound(p.getLocation(), Sound.ENTITY_HORSE_DEATH, SoundCategory.AMBIENT, 9, 0.5f);
				if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
					PlayerUtils.sendTitle(p, Component.empty(), capturedMsg, 10, 25, 10);
				}
			}

			owningTeam = newOwningTeam;
			hillCapProgresses.clear();
			ticksAndPlayersToCaptureHill = INITIAL_CAP_TIME;
		}

		if(owningTeam != null) {
			owningTeam.score++;

			coloursList.add(owningTeam.getColour());
			if(owningTeam.getSecondColour() != null)
				coloursList.add(owningTeam.getSecondColour());
			else
				coloursList.add(owningTeam.getColour());
		}
		else {
			coloursList.add(Color.WHITE);
			//coloursList.add(Color.WHITE);
		}

		activeHill.playParticles(coloursList.toArray(new Color[0]));

		if (owningTeam == null) {
			//no team owns the hill; do anti-stalling mechanism
			//no hill caps for 5 minutes, end the game
			if (gameTick - lastHillChangeTime >= ANTI_STALL_ROTATION_TIME) {
				for (int i = 0; i < 5; i++) {
					Bukkit.broadcast(Component.text("Too slow! It's been 5 minutes!!", NamedTextColor.RED));
				}
				// disable anti-stall game end if debug
				nextHillOrEnd(!CommandDebug.ignoreWinConditions);
			}
			//every two minutes
			else if ((gameTick - lastHillChangeTime) % ANTI_STALL_HALVE_TIME == 0 && lastHillChangeTime != gameTick) {
				String s = "The time to capture the Hill has been halved";
				if (ticksAndPlayersToCaptureHill != INITIAL_CAP_TIME)
					s += " again";

				s += "! It will reset when one team captures the hill";
				Bukkit.broadcast(Component.text(s).color(TextColor.color(255, 10, 10)));

				ticksAndPlayersToCaptureHill /= 2;

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.AMBIENT, 99999, 0.6f);
				}
			}
		}
		//process hill change
		else if(owningTeam.score / 20 >= activeHill.getHillTime() || owningTeam.getTotalScore() >= TICKS_TO_WIN) {
			nextHillOrEnd(false);
		}

		for (TeamArenaTeam team : teams) {
			if (team.isAlive()) {
				team.bossBar.progress(Math.min(((float) team.getTotalScore() / TICKS_TO_WIN), 1f));
			}
		}
		super.liveTick();
	}

	@Override
	public Collection<Component> updateSharedSidebar() {
		sidebarCache.clear();
		record CaptureSummary(TeamArenaTeam team, float progress, float change) {}
		var teamSummary = Arrays.stream(teams)
				.filter(team -> CommandDebug.ignoreWinConditions || team.hasLivingOrRespawningMembers())
				.map(team -> new CaptureSummary(
						team,
						// max capture progress if king of the hill for sorting purposes
						team == owningTeam ? Float.MAX_VALUE : hillCapProgresses.getOrDefault(team, 0f),
						hillCapChange.getOrDefault(team, 0f))
				)
				.sorted(Comparator.comparingDouble(CaptureSummary::progress)
						.thenComparingDouble(summary -> summary.team.getTotalScore())
						.reversed())
				.toList();
		if (teamSummary.isEmpty())
			return List.of();

		double fastestGrowth = teamSummary.stream()
				.mapToDouble(CaptureSummary::change).max().orElse(1);

		for (var summary : teamSummary) {
			var builder = Component.text();
			builder.append(summary.team.getComponentSimpleName(), Component.text(": "));
			if (summary.team == owningTeam) {
				builder.append(Component.text("KING", NamedTextColor.GOLD, TextDecoration.BOLD));
			} else {
				// whatever the hell this means
				double hillPercentage = summary.progress / ticksAndPlayersToCaptureHill * 100;
				builder.append(Component.text((int) hillPercentage + "%"));
				if (summary.change > 0) {
					builder.append(Component.text(summary.change() == fastestGrowth && TeamArena.getGameTick() % 20 < 10 ?
							" ▲" : " ↑", NamedTextColor.GREEN));
				} else if (summary.change < 0) {
					builder.append(Component.text(" ↓", NamedTextColor.RED));
				}
			}

			int controlTime = summary.team.getTotalScore() / 20;
			sidebarCache.put(summary.team, new SidebarManager.SidebarEntry(builder.build(), Component.text(controlTime + "s", NamedTextColor.WHITE)));
		}

		// next anti-stall
		int antiStallTime = gameTick - lastHillChangeTime;
		int timeUntilRotation = ANTI_STALL_ROTATION_TIME - antiStallTime;
		int timeUntilHalve = ANTI_STALL_HALVE_TIME - antiStallTime % ANTI_STALL_HALVE_TIME;
		Component antiStallAction;
		int antiStallCountdown;

		if (timeUntilHalve < timeUntilRotation) {
			boolean again = ticksAndPlayersToCaptureHill != INITIAL_CAP_TIME;
			antiStallAction = Component.text("Capture " + (again ? "even faster" : "faster"), NamedTextColor.GREEN);
			antiStallCountdown = timeUntilHalve;
		} else {
			antiStallAction = Component.text("Hill rotates", NamedTextColor.DARK_AQUA);
			antiStallCountdown = timeUntilRotation;
		}

		return List.of(
			Component.text("First to " + TICKS_TO_WIN / 20 + "s as King", NamedTextColor.GRAY),
			Component.textOfChildren(antiStallAction, Component.text(" in "), TextUtils.formatDurationMmSs(Ticks.duration(antiStallCountdown)))
		);
	}

	@Override
	public void updateSidebar(Player player, SidebarManager sidebar) {
		TeamArenaTeam playerTeam = Main.getPlayerInfo(player).team;
		sidebar.setTitle(player, getGameName());

		int teamsShown = 0;

		for (var entry : sidebarCache.entrySet()) {
			TeamArenaTeam team = entry.getKey();
			var line = entry.getValue();

			if (teamsShown >= 4 && team != playerTeam)
				continue; // don't show
			teamsShown++;
			if (team == playerTeam) {
				sidebar.addEntry(Component.textOfChildren(OWN_TEAM_PREFIX, line.text()), line.numberFormat());
			} else {
				sidebar.addEntry(line);
			}
		}
		// unimportant teams
		if (sidebarCache.size() != teamsShown)
			sidebar.addEntry(Component.empty(), Component.text("+ " + (sidebarCache.size() - teamsShown) + " teams", NamedTextColor.GRAY));

	}

	@Override
	public void handleDeath(DamageEvent event) {
		super.handleDeath(event);

		this.hillGlow(event.getVictim(), false);
	}

	public void nextHill() {
		hillIndex++;
		//if all the hills have been played once and it's random hill order, shuffle the hills again
		if(hillIndex % hills.length == 0) {
			MathUtils.shuffleArray(hills);
		}
		Hill nextHill = hills[hillIndex % hills.length];

		Component hillChangeMsg = Component.text("The Hill has moved to " + nextHill.getName(), NamedTextColor.GOLD);
		Bukkit.broadcast(hillChangeMsg);

		Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
		Location soundLoc = nextHill.getBorder().getCenter().toLocation(gameWorld);
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(entry.getKey(), Component.empty(), hillChangeMsg, 10, 30, 10);
			}
			//and the sound, while we've got this iter anyway
			entry.getKey().playSound(soundLoc, Sound.ENTITY_PARROT_IMITATE_ENDER_DRAGON,SoundCategory.AMBIENT,
					9999, 0.5f);
		}

		activeHill = nextHill;
		lastHillChangeTime = gameTick;
		//clear the owningteam and clear cap progresses
		owningTeam = null;
		hillCapProgresses.clear();
		ticksAndPlayersToCaptureHill = INITIAL_CAP_TIME;
	}

	public void nextHillOrEnd(boolean forceEnd) {
		//add their current hill points to total
		for(TeamArenaTeam team : teams) {
			team.score2 += team.score;
			team.score = 0;
		}

		//no more hills, game is over
		//if(hillIndex == hills.length - 1) {
		//change to if no team has won yet, keep rotating forever
		if (!CommandDebug.ignoreWinConditions &&
				((owningTeam != null && owningTeam.getTotalScore() >= TICKS_TO_WIN) || forceEnd)) {

			TeamArenaTeam winner = null;
			int highestScore = 0;
			for(TeamArenaTeam team : teams) {
				if(team.score2 > highestScore) {
					winner = team;
					highestScore = team.score2;
				}
			}

			winningTeam = winner;

			prepEnd();
			//return;
		}
		else {
			nextHill();
		}
	}


	@Override
	public void prepLive() {
		super.prepLive();

		Component text = Component.text(activeHill.getName() + " is the active Hill!").color(NamedTextColor.GOLD);
		Bukkit.broadcast(text);

		Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();

			Player p = entry.getKey();
			/* moved to super.prepLive()
			for(TeamArenaTeam team : teams) {
				if(team.isAlive())
					p.showBossBar(team.bossBar);
			}*/

			if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(p, Component.empty(), text, 5, 40, 5);
			}
		}

		this.lastHillChangeTime = gameTick;
	}

	@Override
	public void setupMiniMap() {
		// register hill cursor
		for (Hill hill : hills) {
			miniMap.registerCursor((ignored, ignored1) -> {
				boolean active = hill == activeHill;
				Location center = hill.getBorder().getCenter().toLocation(gameWorld);
				Component currentHillText = Component.text(hill.getName(),
					active ?
						(owningTeam != null ? owningTeam.getRGBTextColor() : NamedTextColor.WHITE) :
						NamedTextColor.DARK_GRAY
				);
				MapCursor.Type icon = active ? MapCursor.Type.WHITE_CROSS : MapCursor.Type.SMALL_WHITE_CIRCLE;
				return new MiniMapManager.CursorInfo(center, false, icon, currentHillText);
			});
		}
		// fancy border for active hill
		lastActiveHill = activeHill;
		miniMap.registerCanvasOperation((player, info, canvas, renderer) -> {
			if (lastActiveHill != activeHill) {
				// clear old overlay
				BoundingBox oldBox = lastActiveHill.getBorder();
				renderer.drawRect(canvas, oldBox.getMin(), oldBox.getMax(),
					MiniMapManager.GameRenderer.TRANSPARENT, MiniMapManager.GameRenderer.TRANSPARENT);
				lastActiveHill = activeHill;
			}

			BoundingBox box = activeHill.getBorder();

			if (TeamArena.getGameTick() % 40 < 20) { // only render every other second
				var teamColor = owningTeam != null ? new java.awt.Color(owningTeam.getColour().asRGB(), false) : null;
				@SuppressWarnings("removal")
				byte color = teamColor != null ? MapPalette.matchColor(teamColor) : MapPalette.TRANSPARENT;
				byte borderColor = 29 * 4 + 3; // black
				renderer.drawRect(canvas, box.getMin(), box.getMax(), color, borderColor);
			} else {
				renderer.drawRect(canvas, box.getMin(), box.getMax(),
					MiniMapManager.GameRenderer.TRANSPARENT, MiniMapManager.GameRenderer.TRANSPARENT);
			}
		});
	}

	// Record hill glowing players here to avoid spamming packets unnecessarily.
	private final Set<Player> hillGlowingPlayers = Collections.newSetFromMap(new WeakHashMap<>());
	private void hillGlow(Entity entity, boolean glowing) {
		entity.setGlowing(glowing);

		if(entity instanceof Player player && (!glowing || Kit.getActiveKit(player).isInvisKit())) {
			if(glowing) {
				if(hillGlowingPlayers.add(player))
					PlayerUtils.sendMaxWarningPacket(player);
			}
			else {
				if(hillGlowingPlayers.remove(player))
					PlayerUtils.resetWarningDistance(player);
			}
		}
	}

	@Override
	public void prepEnd() {

		for(Hill h : hills) {
			h.getHologram().remove();
		}

		hillCapProgresses.clear();

		super.prepEnd();
	}

	@Override
	public void prepDead() {
		super.prepDead();
	}

	@Override
	public void loadConfig(TeamArenaMap map) {
		super.loadConfig(map);

		TeamArenaMap.KOTHInfo kothInfo = map.getKothInfo();
		if(kothInfo == null) {
			throw new IllegalArgumentException("Koth constructor has been called with a non-KOTH map");
		}

		this.randomHillOrder = kothInfo.randomOrder();

		List<TeamArenaMap.KothHill> hillConfigs = kothInfo.hills();

		Hill[] hills = new Hill[hillConfigs.size()];
		int index = 0;
		for (TeamArenaMap.KothHill hillInfo : hillConfigs) {
			String name = hillInfo.name();
			BoundingBox hillBox = BoundingBox.of(hillInfo.minCorner(), hillInfo.maxCorner());
			int time = hillInfo.time();

			hills[index] = new Hill(name, hillBox, time, gameWorld);
			index++;
		}
		this.hills = hills;

		if(randomHillOrder)
			MathUtils.shuffleArray(this.hills);

		activeHill = hills[0];
	}

	//respawning game, can change kit at any time (change takes effect on respawn doe)
	@Override
	public boolean canSelectKitNow() {
		return !gameState.isEndGame();
	}

	@Override
	public boolean canSelectTeamNow() {
		return gameState == GameState.PREGAME;
	}

	@Override
	public boolean isRespawningGame() {
		return true;
	}

	@Override
	public Component getGameName() {
		return GAME_NAME;
	}

	@Override
	public Component getHowToPlayBrief() {
		return HOW_TO_PLAY;
	}

	@Override
	public File getMapPath() {
		return new File(super.getMapPath(), "KOTH");
	}

	@Override
	public String getDebugAntiStall() {

		int antiStallTime = gameTick - lastHillChangeTime;
		int timeUntilRotation = ANTI_STALL_ROTATION_TIME - antiStallTime;
		int timeUntilHalve = ANTI_STALL_HALVE_TIME - antiStallTime % ANTI_STALL_HALVE_TIME;
		return """
   			lastHillChangeTime: %d
   			*antiStallTime: %d
   			timeUntilRotation: %d
   			timeUntilHalve: %d
   			ANTI_STALL_ROTATION_TIME: %d
   			ANTI_STALL_HALVE_TIME: %d""".formatted(lastHillChangeTime, antiStallTime, timeUntilRotation, timeUntilHalve,
			ANTI_STALL_ROTATION_TIME, ANTI_STALL_HALVE_TIME);
	}

	@Override
	public void setDebugAntiStall(int antiStallCountdown) {
		lastHillChangeTime = gameTick - antiStallCountdown;
	}
}

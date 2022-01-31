package me.toomuchzelda.teamarenapaper.teamarena.capturetheflag;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class CaptureTheFlag extends TeamArena
{
	public HashMap<TeamArenaTeam, Flag> teamToFlags; //initialized in parseConfig
	public HashMap<ArmorStand, Flag> flagStands; // this too
	public HashMap<Player, Flag> flagHolders = new HashMap<>();
	public int capsToWin;
	public static final int TAKEN_FLAG_RETURN_TIME = 3 * 60 * 20;
	public static final int DROPPED_TIME_PER_TICK = TAKEN_FLAG_RETURN_TIME / (5 * 20);
	public static final int DROPPED_PROGRESS_BAR_LENGTH = 10;
	public static final String DROPPED_PROGRESS_STRING;
	
	public static final Component PICK_UP_MESSAGE = Component.text("%holdingTeam% has picked up %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component DROP_MESSAGE = Component.text("%holdingTeam% has dropped %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component RETURNED_MESSAGE = Component.text("%team%'s flag has been returned to their base").color(NamedTextColor.GOLD);
	public static final Component CAPTURED_MESSAGE = Component.text("%holdingTeam% has captured %team%'s flag!").color(NamedTextColor.GOLD);;
	//shorter ones for titles
	public static final Component PICK_UP_TITLE = Component.text("%holdingTeam% took %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component DROP_TITLE = Component.text("%holdingTeam% dropped %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component RETURNED_TITLE = Component.text("%team%'s flag returned").color(NamedTextColor.GOLD);
	public static final Component CAPTURED_TITLE = Component.text("%holdingTeam% captured %team%'s flag!").color(NamedTextColor.GOLD);;
	
	public static final Component CANT_CAPTURE_YOUR_FLAG_NOT_AT_BASE = Component.text("You can't capture until your flag is safely at your base!").color(TextColor.color(255, 20, 20));
	public static final String CANT_CAPTURE_KEY = "yrflagnotatbase";
	
	public static final Component CANT_TELEPORT_HOLDING_FLAG_MESSAGE = Component.text("You can't teleport while holding the flag!").color(TextColor.color(255, 20, 20));
	public static final Component CANT_TELEPORT_HOLDING_FLAG_TITLE = Component.text("Can't teleport with the flag!").color(TextColor.color(255, 20, 20));
	
	static {
		StringBuilder builder = new StringBuilder(DROPPED_PROGRESS_BAR_LENGTH);
		for(int i = 0; i < DROPPED_PROGRESS_BAR_LENGTH; i++) {
			builder.append('█');
		}
		DROPPED_PROGRESS_STRING = builder.toString();
	}
	
	public CaptureTheFlag() {
		super();
		
		
	}
	
	@Override
	public void liveTick() {
		
		//check if dropped flags have been left long enough to be returned
		// use their nametag for time left to return
		for(Flag flag : teamToFlags.values()) {
			if(!flag.isAtBase) {// && !flag.isBeingCarried()) {
				int returnSpeed;
				if(flag.isBeingCarried())
					returnSpeed = 1;
				else
					returnSpeed = DROPPED_TIME_PER_TICK;
				
				flag.ticksUntilReturn = flag.ticksUntilReturn - returnSpeed;
				
				//int timePassed = TAKEN_FLAG_RETURN_TIME - flag.ticksUntilReturn;
				if(flag.ticksUntilReturn <= 0) {
					returnFlagToBase(flag);
				}
				else {
					float percentage = (float) flag.ticksUntilReturn / (float) TAKEN_FLAG_RETURN_TIME;
					percentage += 0.05;
					if(percentage > 1)
						percentage = 1;
					else if (percentage < 0)
						percentage = 0;
					
					int splitIndex = (int) ((float) DROPPED_PROGRESS_BAR_LENGTH * percentage);
					
					Component firstComponent = Component.text()
							.content(DROPPED_PROGRESS_STRING.substring(0, splitIndex))
							.color(flag.team.getRGBTextColor())
							.append(Component.text().content(DROPPED_PROGRESS_STRING.substring(splitIndex))
									.color(NamedTextColor.DARK_RED)
									.build()).build();
					
					flag.getArmorStand().customName(firstComponent);
				}
			}
		}
		
		updateLiveSidebar();
		
		super.liveTick();
	}
	
	public void updateLiveSidebar() {
		//update the sidebar every tick
		byte numLines;
		LinkedList<Flag> aliveFlags = new LinkedList<>();
		
		Component[] lines;
		for(Flag flag : teamToFlags.values()) {
			if(flag.team.isAlive())
				aliveFlags.add(flag);
		}
		
		Comparator<Flag> byScore = (teamArenaTeam, t1) -> (t1.team.getTotalScore()) - (teamArenaTeam.team.getTotalScore());
		aliveFlags.sort(byScore);
		
		if(aliveFlags.size() <= 7)
			numLines = 2;
		else
			numLines = 1;
		
		lines = new Component[numLines * aliveFlags.size()];
		
		int index = 0;
		for (Flag flag : aliveFlags) {
			Component first = flag.team.getComponentSimpleName();
			if(numLines == 2) {
				Component flagStatus = Component.text("Flag ").color(NamedTextColor.WHITE);
				if(flag.isAtBase)
					flagStatus = flagStatus.append(Component.text("Safe").color(NamedTextColor.GREEN));
				else if(flag.holdingTeam != null) {
					flagStatus = flagStatus.append(Component.text("Held by ")).append(flag.holdingTeam.getComponentSimpleName());
				}
				else {
					flagStatus = flagStatus.append(Component.text("Unsafe").color(TextColor.color(255, 85, 0)));
				}
				
				lines[index] = first.append(Component.text(": " + flag.team.getTotalScore()).color(NamedTextColor.WHITE));
				lines[index + 1] = flagStatus;
			}
			else {
				Component flagStatus;
				if(flag.isAtBase)
					flagStatus = Component.text("Safe").color(NamedTextColor.GREEN);
				else if(flag.holdingTeam != null) {
					flagStatus = Component.text("Held").color(flag.holdingTeam.getRGBTextColor());
				}
				else {
					flagStatus = Component.text("Unsafe").color(TextColor.color(255, 85, 0));
				}
				lines[index] = first.append(Component.text(": " + flag.team.getTotalScore() + ' ').color(NamedTextColor.WHITE).append(flagStatus));
			}
			
			index += numLines;
		}
		
		SidebarManager.setLines(lines);
	}
	
	@Override
	public void tick() {
		super.tick();
		
		//tick flags
		for(Map.Entry<ArmorStand, Flag> entry : flagStands.entrySet()) {
			ArmorStand stand = entry.getKey();
			Flag flag = entry.getValue();
			if(!gameState.isEndGame()) {
				if(!flag.isBeingCarried()) {
					//spin and bob around if not being carried
					flagPositionTick(flag);
					
					//check player get it
					if(gameState == GameState.LIVE) {
						for (Player p : players) {
							//if(isSpectator(p))
							//	continue;
							
							if(p.getBoundingBox().overlaps(stand.getBoundingBox())) {
								//return flag to base if teammate touches it and not at base
								if(flag.team.getPlayerMembers().contains(p)) {
									if(!flag.isAtBase) {
										returnFlagToBase(flag);
										break;
									}
								}
								//enemy picked up the flag
								else {
									pickUpFlag(p, flag);
									break;
								}
							}
						}
					}
				}
				//flag is being carried, check for capture
				else if(gameState == GameState.LIVE) {
					Player holder = flag.holder;
					//check if this player's team's flag is taken
					// if not taken, check if this player is touching their flag base thing and capture if they are
					TeamArenaTeam team = Main.getPlayerInfo(holder).team;
					Flag teamsFlag = teamToFlags.get(team);
					if(holder.getBoundingBox().overlaps(teamsFlag.baseBox)) {
						if(teamsFlag.isAtBase) //capture the flag!!
							captureTheFlag(holder, team, flag);
						else {
							PlayerInfo pinfo = Main.getPlayerInfo(holder);
							if(pinfo.messageHasCooldowned(CANT_CAPTURE_KEY, 3 * 20)) {
								holder.sendMessage(CANT_CAPTURE_YOUR_FLAG_NOT_AT_BASE);
								PlayerUtils.sendTitle(holder, Component.empty(), CANT_CAPTURE_YOUR_FLAG_NOT_AT_BASE,
										0, 25, 10);
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public void handleDeath(DamageEvent event) {
		if(event.getVictim() instanceof Player p) {
			dropFlag(p);
		}
		
		super.handleDeath(event);
	}

	public void pickUpFlag(Player player, Flag flag) {
		flagHolders.put(player, flag);
		if(flag.isAtBase)
			flag.ticksUntilReturn = TAKEN_FLAG_RETURN_TIME;
		
		flag.isAtBase = false;
		flag.holder = player;
		flag.holdingTeam = Main.getPlayerInfo(player).team;
		player.addPassenger(flag.getArmorStand());
		player.setGlowing(true);
		//send a metadata packet that has the marker armor stand option on so they can still interact with the outside
		// world
		//PlayerUtils.sendPacket(player, flag.markerMetadataPacket);
		PlayerUtils.sendPacket(player, flag.getRemovePacket());

		final TextReplacementConfig playerConfig = TextReplacementConfig.builder().match("%holdingTeam%")
				.replacement(player.playerListName()).build();
		final TextReplacementConfig flagTeamConfig = TextReplacementConfig.builder().match("%team%")
				.replacement(flag.team.getComponentSimpleName()).build();

		Component pickupChat = PICK_UP_MESSAGE.replaceText(playerConfig).replaceText(flagTeamConfig);
		Component pickupTitle = PICK_UP_TITLE.replaceText(playerConfig).replaceText(flagTeamConfig);

		Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			Player p = entry.getKey();
			if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(p, Component.empty(), pickupTitle, 7, 30, 7);
			}

			//todo maybe a preference for game sounds
			p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.AMBIENT, 2, 1f);
		}

		Bukkit.broadcast(pickupChat);
	}

	public void dropFlag(Player player) {
		Flag flag = flagHolders.remove(player);
		if(flag != null) {
			player.removePassenger(flag.getArmorStand());
			flag.currentLoc = player.getLocation();
			flag.holder = null;
			flag.holdingTeam = null;
			flagHolders.remove(player);
			player.setGlowing(false);
			flag.sendRecreatePackets(player);
			
			//if there's no floor to land on when it's dropped teleport it back to base
			if(BlockUtils.getFloor(flag.currentLoc) == null) {
				flag.teleportToBase();
			}
			
			Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
			
			final TextReplacementConfig playerConfig = TextReplacementConfig.builder().match("%holdingTeam%")
					.replacement(player.playerListName()).build();
			final TextReplacementConfig teamConfig = TextReplacementConfig.builder().match("%team%")
					.replacement(flag.team.getComponentSimpleName()).build();
			
			Component titleText = DROP_TITLE.replaceText(playerConfig).replaceText(teamConfig);
			Component chatText = DROP_MESSAGE.replaceText(playerConfig).replaceText(teamConfig);
			
			while(iter.hasNext()) {
				Map.Entry<Player, PlayerInfo> entry = iter.next();
				Player p = entry.getKey();
				// dae use unsafe type casts because the preference system is so bad
				if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
					PlayerUtils.sendTitle(p, Component.empty(), titleText, 7, 30, 7);
				}
				
				p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, SoundCategory.AMBIENT, 2, 1f);
			}
			Bukkit.broadcast(chatText);
		}
	}
	
	public void returnFlagToBase(Flag flag) {
		
		flag.teleportToBase();
		
		final TextReplacementConfig returnConfig = TextReplacementConfig.builder().match("%team%").replacement(flag.team.getComponentSimpleName()).build();
		
		Component chatText = RETURNED_MESSAGE.replaceText(returnConfig);
		Component titleText = RETURNED_TITLE.replaceText(returnConfig);
		
		var iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			Player p = entry.getKey();
			
			if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(p, Component.empty(), titleText, 7, 30, 7);
			}
			
			p.playSound(p.getLocation(), Sound.BLOCK_LARGE_AMETHYST_BUD_PLACE, SoundCategory.AMBIENT, 2, 1);
		}
		
		Bukkit.broadcast(chatText);
	}
	
	public void captureTheFlag(Player player, TeamArenaTeam capturingTeam, Flag capturedFlag) {
		
		capturedFlag.teleportToBase();
		capturingTeam.score++;
		player.setGlowing(false);
		flagHolders.remove(player);
		capturedFlag.sendRecreatePackets(player);
		
		updateBossBars();
		
		final TextReplacementConfig holdingConfig = TextReplacementConfig.builder().match("%holdingTeam%")
				.replacement(player.playerListName()).build();
		final TextReplacementConfig victimConfig = TextReplacementConfig.builder().match("%team%").replacement(capturedFlag.team.getComponentSimpleName()).build();
		
		Component chatText = CAPTURED_MESSAGE.replaceText(holdingConfig).replaceText(victimConfig);
		Component titleText = CAPTURED_TITLE.replaceText(holdingConfig).replaceText(victimConfig);
		
		var iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			Player p = entry.getKey();
			
			if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(p, Component.empty(), titleText, 7, 30, 7);
			}
			
			p.playSound(p.getLocation(), Sound.BLOCK_LARGE_AMETHYST_BUD_BREAK, SoundCategory.AMBIENT, 2f, 1f);
		}
		
		Bukkit.broadcast(chatText);
		
		//end the game if win
		if(capturingTeam.score >= capsToWin) {
			winningTeam = capturingTeam;
			prepEnd();
		}
	}
	
	public void updateBossBars() {
		for(TeamArenaTeam team : teams) {
			float progress = (float) team.score / (float) capsToWin;
			if(progress > 1)
				progress = 1; //floating point moment
			team.bossBar.progress(progress);
		}
	}
	
	public void flagPositionTick(Flag flag) {
		ArmorStand stand = flag.getArmorStand();
		Location loc;
		if(flag.isAtBase)
			loc = flag.baseLoc.clone();
		else {
			RayTraceResult result = flag.currentLoc.getWorld().rayTraceBlocks(flag.currentLoc,
					new Vector(0, -1, 0), 383, FluidCollisionMode.SOURCE_ONLY, true);
			
			if(result != null) {
				double distance = flag.currentLoc.toVector().distance(result.getHitPosition());
				//max fall of 0.1 blocks per tick
				if(distance > 0.5) {
					distance = 0.1;
					flag.currentLoc.subtract(0, distance, 0);
				}
			}
			else {
				Main.logger().warning("Flag has been dropped and left above void, should be impossible!");
				Thread.dumpStack();
				flag.teleportToBase();
				return;
			}
			loc = flag.currentLoc.clone();
		}
		loc.setY(loc.getY() + (Math.sin((double) System.currentTimeMillis() / 2) / 5));
		loc.setYaw(((stand.getLocation().getYaw() + 5f) % 360));//- 180f);
		stand.teleport(loc);
		//net.minecraft.world.entity.decoration.ArmorStand nmsStand = ((CraftArmorStand) stand).getHandle();
		//nmsStand.moveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
	}
	
	@Override
	public void onDamage(DamageEvent event) {
		super.onDamage(event);
		
		if(event.getVictim() instanceof ArmorStand stand && flagStands.containsKey(stand)) {
			event.setCancelled(true);
		}
	}
	
	@Override
	public void prepEnd() {
		super.prepEnd();
		
		Bukkit.getScheduler().runTaskLater(Main.getPlugin(), bukkitTask -> {
			
			for(Flag flag : flagStands.values()) {
				flag.getArmorStand().remove();
				flag.unregisterTeam();
			}
		
		}, END_GAME_TIME - 4); //one tick before the scheduled tasks in super
	}
	
	
	
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
	
	public boolean isFlagCarrier(Player p) {
		return flagHolders.containsKey(p);
	}
	
	@Override
	public void parseConfig(Map<String, Object> map) {
		super.parseConfig(map);
		
		flagStands = new HashMap<>();
		teamToFlags = new HashMap<>();
		
		Map<String, Object> customFlags = (Map<String, Object>) map.get("Custom");
		
		Main.logger().info("Custom Info: ");
		Main.logger().info(customFlags.toString());
		
		for (Map.Entry<String, Object> entry : customFlags.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("CapsToWin")) {
				try {
					capsToWin = (Integer) entry.getValue();//Integer.parseInt(entry.getValue());
				} catch (NullPointerException | ClassCastException e) {
					Main.logger().warning("Invalid CapsToWin! Must be an integer number (no decimals!). Defaulting to 3");
					e.printStackTrace();
					capsToWin = 3;
				}
			} else {
				TeamArenaTeam team = getTeamByName(entry.getKey());
				if (team == null) {
					throw new IllegalArgumentException("Unknown team " + entry.getKey() + "!!!! Use the team's full name i.e \"Red Team:\"");
				}
				
				Location teamsFlagLoc = BlockUtils.parseCoordsToVec((String) entry.getValue(), 0.5, -0.4, 0.5).toLocation(gameWorld);
				Flag flag = new Flag(this, team, teamsFlagLoc);
				
				teamToFlags.put(team, flag);
			}
		}
	}
	
	@Override
	public String mapPath() {
		return super.mapPath() + "CTF";
	}
}

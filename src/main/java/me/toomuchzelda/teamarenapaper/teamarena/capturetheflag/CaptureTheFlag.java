package me.toomuchzelda.teamarenapaper.teamarena.capturetheflag;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CaptureTheFlag extends TeamArena
{
	public HashMap<TeamArenaTeam, Flag> teamToFlags; //initialized in parseConfig
	public HashMap<ArmorStand, Flag> flagStands; // this too
	public HashMap<Player, Flag> flagHolders = new HashMap<>();
	public int capsToWin;
	
	public static final Component PICK_UP_MESSAGE = Component.text("%holdingTeam% has picked up %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component DROP_MESSAGE = Component.text("%holdingTeam% has dropped %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component RETURNED_MESSAGE = Component.text("%team%'s flag has been returned to their base").color(NamedTextColor.GOLD);
	public static final Component CAPTURED_MESSAGE = Component.text("%holdingTeam% has captured %team%'s flag!").color(NamedTextColor.GOLD);;
	//shorter ones for titles
	public static final Component PICK_UP_TITLE = Component.text("%holdingTeam% took %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component DROP_TITLE = Component.text("%holdingTeam% dropped %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component RETURNED_TITLE = Component.text("%team%'s flag returned").color(NamedTextColor.GOLD);
	public static final Component CAPTURED_TITLE = Component.text("%holdingTeam% captured %team%'s flag!").color(NamedTextColor.GOLD);;
	
	public CaptureTheFlag() {
		super();
		
		
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
							//TODO: send them message they can't cap if flag not at base
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
		flag.isAtBase = false;
		flag.holder = player;
		player.addPassenger(flag.getArmorStand());
		
		player.setGlowing(true);
		
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
			player.setGlowing(false);
			
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
			
			//TODO: play a sound probably
		}
		
		Bukkit.broadcast(chatText);
	}
	
	public void captureTheFlag(Player player, TeamArenaTeam capturingTeam, Flag capturedFlag) {
		
		capturedFlag.teleportToBase();
		capturingTeam.score++;
		player.setGlowing(false);
		
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
			
			//TODO: play a sound
		}
		
		Bukkit.broadcast(chatText);
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

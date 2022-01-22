package me.toomuchzelda.teamarenapaper.teamarena.capturetheflag;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.EnumPreference;
import net.kyori.adventure.text.Component;
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
	
	public CaptureTheFlag() {
		super();
		
		
	}
	
	@Override
	public void tick() {
		super.tick();
		
		//make them spin around and bob around
		for(Map.Entry<ArmorStand, Flag> entry : flagStands.entrySet()) {
			ArmorStand stand = entry.getKey();
			if(!stand.isInsideVehicle() && !gameState.isEndGame()) {
				Flag flag = entry.getValue();
				Location loc;
				if(flag.isAtBase)
					loc = flag.baseLoc.clone();
				else {
					RayTraceResult result = flag.currentLoc.getWorld().rayTraceBlocks(flag.currentLoc,
							new Vector(0, -1, 0), 383, FluidCollisionMode.SOURCE_ONLY, true);
					
					if(result != null) {
						double distance = flag.currentLoc.toVector().distance(result.getHitPosition());
						//max fall of 0.1 blocks per tick
						if(distance > 0.1) {
							distance = 0.1;
						}
						flag.currentLoc.subtract(0, distance, 0);
					}
					else {
						Main.logger().warning("Flag has been dropped and left above void, should be impossible!");
						Thread.dumpStack();
					}
					loc = flag.currentLoc;
				}
				loc.setY(loc.getY() + (Math.sin((double) System.currentTimeMillis() / 2) / 5));
				loc.setYaw(((stand.getLocation().getYaw() + 0.0001f) % 360) - 180f);
				stand.teleport(loc);
				
				//check player get it
				if(gameState == GameState.LIVE) {
					for (Player p : Bukkit.getOnlinePlayers()) {
						//skip if on same team
						if(entry.getValue().team.getEntityMembers().contains(p))
							continue;
						
						//picked up the flag
						if (p.getBoundingBox().overlaps(stand.getBoundingBox())) {
							pickUpFlag(p, entry.getValue());
							break;
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
	
	public void pickUpFlag(Player capper, Flag flag) {
		flagHolders.put(capper, flag);
		flag.isAtBase = false;
		capper.addPassenger(flag.getArmorStand());
		
		Component text = capper.playerListName().append(Component.text(" has picked up ").color(NamedTextColor.GOLD));
		Component endText = Component.text("'s flag!").color(NamedTextColor.GOLD);
		Component chatText = text.append(flag.team.getComponentName()).append(endText);
		//title uses simple name to make it a bit shorter
		Component titleText = text.append(flag.team.getComponentSimpleName()).append(endText);
		
		Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			Player p = entry.getKey();
			if((Boolean) entry.getValue().getPreference(EnumPreference.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(p, Component.empty(), titleText, 7, 15, 7);
			}
			
			//todo maybe a preference for game sounds
			p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.AMBIENT, 2, 1f);
		}
		
		Bukkit.broadcast(titleText);
	}
	
	public void dropFlag(Player p) {
		Flag flag = flagHolders.remove(p);
		if(flag != null) {
			p.removePassenger(flag.getArmorStand());
			flag.currentLoc = p.getLocation();
			//if there's no floor to land on when it's dropped teleport it back to base
			if(BlockUtils.getFloor(flag.currentLoc) == null) {
			}
			
			Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
			// dae use 3 variables because the component system is so bad
			Component text = p.playerListName();
			Component hasDropped = Component.text(" has dropped ").color(NamedTextColor.GOLD);
			Component flagText = Component.text(" flag!").color(NamedTextColor.GOLD);
			text = text.append(hasDropped).append(flag.team.getComponentSimpleName()).append(flagText);
			while(iter.hasNext()) {
				Map.Entry<Player, PlayerInfo> entry = iter.next();
				// dae use unsafe type casts because the preference system is so bad
				if((Boolean) entry.getValue().getPreference(EnumPreference.RECEIVE_GAME_TITLES)) {
					PlayerUtils.sendTitle(p, Component.empty(), text, 7, 15, 7);
				}
				
				p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, SoundCategory.AMBIENT, 2, 1f);
			}
			Bukkit.broadcast(text);
		}
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
		
		for(ArmorStand stand : flagStands.keySet()) {
			stand.remove();
		}
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

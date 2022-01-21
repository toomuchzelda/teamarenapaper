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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

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
			if(!stand.isInsideVehicle()) {
				Location loc = entry.getValue().baseLoc.clone();
				loc.setY(loc.getY() + (Math.sin((double) System.currentTimeMillis() / 2) / 5));
				loc.setYaw(((stand.getLocation().getYaw() + 1f) % 360) - 180f);
				stand.teleport(loc);
				
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (p.getBoundingBox().overlaps(stand.getBoundingBox())) {
						flagHolders.put(p, entry.getValue());
						p.addPassenger(stand);
						break;
					}
				}
			}
		}
	}
	
	@Override
	public void handleDeath(DamageEvent event) {
		super.handleDeath(event);
		
		if(event.getVictim() instanceof Player p) {
			Flag flag = flagHolders.get(p);
			if(flag != null) {
				p.removePassenger(flag.getArmorStand());
				flag.getArmorStand().teleport(flag.baseLoc);
				Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
				while(iter.hasNext()) {
					Map.Entry<Player, PlayerInfo> entry = iter.next();
					Component text = p.playerListName().append(Component.text(" has dropped " + flag.team.getSimpleName() + "'s Flag!"));
					// dae use unsafe type casts because the preference system is so bad
					if((Boolean) entry.getValue().getPreference(EnumPreference.RECEIVE_GAME_TITLES)) {
						PlayerUtils.sendTitle(p, Component.empty(), text, 7, 15, 7);
					}
				}
			}
			
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

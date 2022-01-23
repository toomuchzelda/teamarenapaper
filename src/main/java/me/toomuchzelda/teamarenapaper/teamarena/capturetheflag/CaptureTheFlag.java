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
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftArmorStand;
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
				
				//check player get it
				if(gameState == GameState.LIVE) {
					for (Player p : Bukkit.getOnlinePlayers()) {
						//skip if on same team
						if(entry.getValue().team.getPlayerMembers().contains(p))
							continue;
						
						//picked up the flag
						if (!isSpectator(p) && p.getBoundingBox().overlaps(stand.getBoundingBox())) {
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
	
	public void pickUpFlag(Player player, Flag flag) {
		flagHolders.put(player, flag);
		flag.isAtBase = false;
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
			if((Boolean) entry.getValue().getPreference(EnumPreference.RECEIVE_GAME_TITLES)) {
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
				if((Boolean) entry.getValue().getPreference(EnumPreference.RECEIVE_GAME_TITLES)) {
					PlayerUtils.sendTitle(p, Component.empty(), titleText, 7, 30, 7);
				}
				
				p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, SoundCategory.AMBIENT, 2, 1f);
			}
			Bukkit.broadcast(chatText);
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

package me.toomuchzelda.teamarenapaper;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import me.toomuchzelda.teamarenapaper.core.Hologram;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class EventListeners implements Listener
{
	public EventListeners(Plugin plugin) {
		Bukkit.getServer().getPluginManager().registerEvents(this,plugin);
	}

	//run the TeamArena tick
	//paper good spigot bad
	@EventHandler
	public void endTick(ServerTickEndEvent event) {
		Main.getGame().tick();
		
		//update nametag positions
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			if(pinfo.nametag != null) {
				pinfo.nametag.updatePosition();
			}
		}
	}

	//these three events are called in this order
	@EventHandler
	public void playerLogin(PlayerLoginEvent event) {
		//todo: read from MySQL server or something for stored player data.
		// or use persistent data containers, or option to use either
		// and also use the PreLoginEvent
		Main.addPlayerInfo(event.getPlayer(), new PlayerInfo());
		Main.getGame().loggingInPlayer(event.getPlayer());
		Main.playerIdLookup.put(event.getPlayer().getEntityId(), event.getPlayer());
	}
	
	@EventHandler
	public void playerSpawn(PlayerSpawnLocationEvent event) {
		event.setSpawnLocation(Main.getPlayerInfo(event.getPlayer()).spawnPoint);
	}
	
	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		//disable yellow "Player has joined the game" messages
		event.joinMessage(null);
		Main.getGame().joiningPlayer(event.getPlayer());
		new Hologram(event.getPlayer());
		//put them on team after their hologram made
		Main.getPlayerInfo(event.getPlayer()).team.addMembers(event.getPlayer());
	}
	
	@EventHandler
	public void playerQuit(PlayerQuitEvent event) {
		event.quitMessage(null);
		Main.getGame().leavingPlayer(event.getPlayer());
		Main.getPlayerInfo(event.getPlayer()).nametag.remove();
		Main.removePlayerInfo(event.getPlayer());
		Main.playerIdLookup.remove(event.getPlayer().getEntityId());
	}
	
	@EventHandler
	public void playerMove(PlayerMoveEvent event) {
		if(Main.getGame().getGameState() == GameState.GAME_STARTING) {
			Location prev = event.getFrom();
			Location next = event.getTo();
			
			if(prev.getX() != next.getX() || prev.getZ() != next.getZ()) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void entityPoseChange(EntityPoseChangeEvent event) {
		if(event.getEntity() instanceof  Player p) {
			Hologram hologram = Main.getPlayerInfo(p).nametag;
			if(hologram != null) {
				//need to update the position of the nametag when Pose changes
				// would do this within the event but we need to let the event pass to be able to get the
				// player's height (otherwise have to use hardcoded values)
				//just signal to update it at the end of the tick
				hologram.poseChanged = true;
			}
		}
	}
}
package me.toomuchzelda.teamarenapaper;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import me.toomuchzelda.teamarenapaper.core.Hologram;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.ArrowPierceManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageTimes;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import static me.toomuchzelda.teamarenapaper.teamarena.GameState.LIVE;

public class EventListeners implements Listener
{
	public EventListeners(Plugin plugin) {
		Bukkit.getServer().getPluginManager().registerEvents(this,plugin);
	}

	//run the TeamArena tick
	//paper good spigot bad
	@EventHandler
	public void endTick(ServerTickEndEvent event) {
		PacketListeners.cancelDamageSounds = false;
		
		try {
			Main.getGame().tick();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
			//update nametag positions
			for (PlayerInfo pinfo : Main.getPlayerInfos()) {
				if (pinfo.nametag != null) {
					pinfo.nametag.updatePosition();
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		//every 3 minutes
		if(event.getTickNumber() % 3 * 60 * 20 == 0) {
			DamageTimes.cleanup();
			ArrowPierceManager.cleanup();
		}

		PacketListeners.cancelDamageSounds = true;
	}

	//these three events are called in this order
	@EventHandler
	public void playerLogin(PlayerLoginEvent event) {
		//todo: read from MySQL server or something for stored player data.
		// or use persistent data containers, or option to use either
		// and also use the PreLoginEvent
		PlayerInfo playerInfo = new PlayerInfo();
		Main.addPlayerInfo(event.getPlayer(), playerInfo);
		Main.getGame().loggingInPlayer(event.getPlayer(), playerInfo);
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
		new Hologram(event.getPlayer());
		Main.getGame().joiningPlayer(event.getPlayer());
		//put them on team after their hologram made
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
		TeamArena game = Main.getGame();
		if(game.getGameState() == GameState.GAME_STARTING && !game.isSpectator(event.getPlayer())) {
			Location prev = event.getFrom();
			Location next = event.getTo();
			
			if(prev.getX() != next.getX() || prev.getZ() != next.getZ()) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void blockBreak(BlockBreakEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler
	public void entityPoseChange(EntityPoseChangeEvent event) {
		if(event.getEntity() instanceof  Player p) {
			Hologram hologram = Main.getPlayerInfo(p).nametag;
			if(hologram != null) {
				//need to update the position of the nametag when Pose changes
				// would do this within the event but we need to let the event pass to be able to get the
				// player's changed height (otherwise have to use hardcoded values)
				//just signal to update it at the end of the tick
				hologram.poseChanged = true;
			}
		}
	}

	//create and cache damage events
	@EventHandler
	public void entityDamage(EntityDamageEvent event) {
		event.setCancelled(true);
		//Bukkit.broadcast(Component.text("DamageCause: " + event.getCause()));
		
		//marker armorstands must never be damaged/killed
		if(event.getEntity() instanceof ArmorStand stand && stand.isMarker())
			return;

		if(Main.getGame().getGameState() == LIVE) {
			//Main.getGame().queueDamage(new DamageEvent(event));
			//will queue itself
			new DamageEvent(event);
		}
		
	}

	@EventHandler
	public void playerDeath(PlayerDeathEvent event) {
		//todo
		event.setCancelled(true);
	}

	//shouldn't run since we handle deaths on our own, but just in case
	// we want the server to avoid loading the default "world" world
	@EventHandler
	public void playerRespawn(PlayerRespawnEvent event) {
		event.setRespawnLocation(Main.getPlayerInfo(event.getPlayer()).spawnPoint);
	}

	@EventHandler
	public void entityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
		//don't get arrows still in motion
		if(event.getEntity() instanceof AbstractArrow aa && aa.isInBlock()) {
			ArrowPierceManager.piercedEntitiesMap.remove(event.getEntity());
		}
	}
}
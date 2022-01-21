package me.toomuchzelda.teamarenapaper;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.core.Hologram;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
import me.toomuchzelda.teamarenapaper.teamarena.damage.ArrowPierceManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageTimes;
import me.toomuchzelda.teamarenapaper.teamarena.kingofthehill.KingOfTheHill;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitGhost;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.EnumPreference;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.PreferenceManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static me.toomuchzelda.teamarenapaper.teamarena.GameState.DEAD;
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

		//Reset mobs' fire status if they've been extinguished
		Iterator<Map.Entry<LivingEntity, DamageTimes>> iter = DamageTimes.entityDamageTimes.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<LivingEntity, DamageTimes> entry = iter.next();
			if(entry.getKey().getFireTicks() <= 0) {
				entry.getValue().fireTimes.fireGiver = null;
				entry.getValue().fireTimes.fireType = null;
			}
		}

		//run this before the game tick so there is a whole tick after prepDead and construction of the next
		// TeamArena instance
		if(Main.getGame().getGameState() == DEAD) {
			
			//use this opportunity to cleanup
			Iterator<Map.Entry<Player, PlayerInfo>> pinfoIter = Main.getPlayersIter();
			while(pinfoIter.hasNext()) {
				if(!pinfoIter.next().getKey().isOnline()) {
					pinfoIter.remove();
				}
			}
			DamageTimes.cleanup();
			Main.playerIdLookup.entrySet().removeIf(idLookupEntry -> !idLookupEntry.getValue().isOnline());
			
			if(TeamArena.nextGameType == GameType.KOTH) {
				Main.setGame(new KingOfTheHill());
			}
			else if(TeamArena.nextGameType == GameType.CTF) {
				Main.setGame(new CaptureTheFlag());
			}
		}

		try {
			Main.getGame().tick();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		/*try {
			//update nametag positions
			for (PlayerInfo pinfo : Main.getPlayerInfos()) {
				if (pinfo.nametag != null) {
					pinfo.nametag.updatePosition();
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}*/

		//every 3 minutes
		if(event.getTickNumber() % (3 * 60 * 20) == 0) {
			ArrowPierceManager.cleanup();
		}

		PacketListeners.cancelDamageSounds = true;
	}

	//load the player's preferences from db and store temporarily until their PlayerInfo is created
	@EventHandler
	public void asyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		if(event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
			return;
		
		Object[] values = new Object[EnumPreference.SIZE];
		
		//todo: read from database or persistent storage
		EnumPreference[] arr = EnumPreference.values();
		for(int i = 0; i < arr.length; i++) {
			values[i] = arr[i].preference.getDefaultValue();
		}
		
		PreferenceManager.putData(event.getUniqueId(), values);
	}
	
	//these three events are called in this order
	@EventHandler
	public void playerLogin(PlayerLoginEvent event) {
		//todo: read from MySQL server or something for stored player data.
		// or use persistent data containers, or option to use either
		// and also use the PreLoginEvent
		PlayerInfo playerInfo;

		//todo: read perms from db or other
		if(event.getPlayer().getName().equalsIgnoreCase("toomuchzelda"))
			playerInfo = new PlayerInfo(CustomCommand.OWNER);
		else
			playerInfo = new PlayerInfo(CustomCommand.ALL);
		
		playerInfo.setPreferenceValues(PreferenceManager.getAndRemoveData(event.getPlayer().getUniqueId()));
		
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
		event.getPlayer().setScoreboard(SidebarManager.SCOREBOARD);
		//new Hologram(event.getPlayer());
		Main.getGame().joiningPlayer(event.getPlayer());
	}

	//don't show any commands the player doesn't have permission to use in the tab list
	@EventHandler
	public void playerCommandSend(PlayerCommandSendEvent event) {
		@NotNull Collection<String> commands = event.getCommands();

		Iterator<String> iter = commands.iterator();
		PlayerInfo pinfo = Main.getPlayerInfo(event.getPlayer());
		while(iter.hasNext()) {
			String strCommand = iter.next();
			Command command = Bukkit.getCommandMap().getCommand(strCommand);
			if(command != null) {
				//if it's custom command check for my own permission level otherwise use
				// bukkit ones or whatever
				if (command instanceof CustomCommand customCmd) {
					if (pinfo.permissionLevel < customCmd.permissionLevel) {
						iter.remove();
					}
				}
				else if (command.getPermission() != null){
					if (!event.getPlayer().hasPermission(command.getPermission())) {
						iter.remove();
					}
				}
			}
		}
	}
	
	@EventHandler
	public void playerQuit(PlayerQuitEvent event) {
		event.quitMessage(null);
		Main.getGame().leavingPlayer(event.getPlayer());
		//Main.getPlayerInfo(event.getPlayer()).nametag.remove();
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
		if(Main.getGame().getGameState() != LIVE)
			return;
		
		//marker armorstands must never be damaged/killed
		if(event.getEntity() instanceof ArmorStand stand && stand.isMarker())
			return;

		if(event.getEntity().getWorld() != Main.getGame().getWorld())
			return;

		//prevent spectators from getting hurt
		if(event.getEntity() instanceof Player p && Main.getGame().isSpectator(p))
			return;


		if(event instanceof EntityDamageByEntityEvent dEvent) {
			if(dEvent.getDamager() instanceof Player p && Main.getGame().isSpectator(p))
				return;
			else if (dEvent.getCause() == EntityDamageEvent.DamageCause.PROJECTILE && dEvent.getDamager() instanceof AbstractArrow aa) {
				//Bukkit.broadcastMessage("Critical arrow: " + aa.isCritical());
				//Bukkit.broadcastMessage("speed: " + aa.getVelocity().length());

				//fix arrow damage - no random crits
				//  arrow damage is the vanilla formula without the part
				double damage = Math.ceil(MathUtils.clamp(0, 2.147483647E9d, aa.getDamage() * aa.getVelocity().length()));
				//this also does all armor re-calculations and stuff
				dEvent.setDamage(damage);

				//stop arrows from bouncing off after this event is run
				//store info about how it's moving now, before the EntityDamageEvent ends and the cancellation
				// makes the arrow bounce off the damagee, so we can re-set the movement later
				ArrowPierceManager.addOrUpdateInfo(aa);

				//fix the movement after event is run
				Bukkit.getScheduler().runTaskLater(Main.getPlugin(), bukkitTask -> {
					if(aa.isValid())
						ArrowPierceManager.fixArrowMovement(aa);
				}, 0L);
			}
		}

		//Bukkit.broadcastMessage("EventFinalDamage: " + event.getFinalDamage());
		
		//Main.getGame().queueDamage(new DamageEvent(event));
		//will queue itself
		new DamageEvent(event);
	}

	@EventHandler
	public void playerDeath(PlayerDeathEvent event) {
		//todo
		event.setCancelled(true);
	}

	//shouldn't run since we handle deaths on our own, but just in case
	// try have the server avoid loading the default "world" world
	@EventHandler
	public void playerRespawn(PlayerRespawnEvent event) {
		event.setRespawnLocation(Main.getPlayerInfo(event.getPlayer()).spawnPoint);
	}

	//stop projectiles from inheriting thrower's velocity
	// like moving too up/down when player is jumping/falling/rising
	@EventHandler
	public void entityShootBow(EntityShootBowEvent event) {
		event.getProjectile().setVelocity(projectileLaunchVector(event.getEntity(), event.getProjectile().getVelocity()));
	}

	//^^
	@EventHandler
	public void playerLaunchProjectile(PlayerLaunchProjectileEvent event) {
		/*Bukkit.broadcastMessage(event.getItemStack().getType().toString());
		Bukkit.broadcastMessage(event.getProjectile().getVelocity().toString());*/

		event.getProjectile().setVelocity(projectileLaunchVector(event.getPlayer(), event.getProjectile().getVelocity()));
		
		if(Main.getGame() != null && Main.getGame().getGameState() == LIVE) {
			Ability[] abilites = Main.getPlayerInfo(event.getPlayer()).kit.getAbilities();
			for(Ability a : abilites) {
				a.onLaunchProjectile(event);
			}
		}
	}

	public static Vector projectileLaunchVector(Entity shooter, Vector original) {
		//slight randomness in direction
		double randX = MathUtils.random.nextGaussian() * 0.0075;
		double randY = MathUtils.random.nextGaussian() * 0.0075;
		double randZ = MathUtils.random.nextGaussian() * 0.0075;

		Vector direction = shooter.getLocation().getDirection();
		double power = original.subtract(shooter.getVelocity()).length();

		//probably add to each component?
		direction.setX(direction.getX() + randX);
		direction.setY(direction.getY() + randY);
		direction.setZ(direction.getZ() + randZ);

		direction.multiply(power);

		//Bukkit.broadcastMessage("velocity: " + direction.toString());

		return direction;
	}

	//stop projectiles from colliding with spectators
	@EventHandler
	public void projectileCollide(ProjectileCollideEvent event) {
		if(event.getCollidedWith() instanceof Player p && Main.getGame() != null && Main.getGame().isSpectator(p))
			event.setCancelled(true);
	}

	//stop hunger
	@EventHandler
	public void entityExhaustion(EntityExhaustionEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler
	public void arrowBodyCountChange(ArrowBodyCountChangeEvent event) {
		//not worth adding a new method to Ability.java for this one
		if(Main.getGame() != null && Main.getGame().getGameState() == LIVE) {
			if (event.getEntity() instanceof Player p) {
				Ability[] abilities = Main.getPlayerInfo(p).kit.getAbilities();
				for(int i = 0; i < abilities.length; i++) {
					if(abilities[i] instanceof KitGhost.GhostAbility ghosta) {
						ghosta.arrowCountDecrease(event);
						return;
					}
				}
			}
		}
	}

	@EventHandler
	public void playerItemCooldown(PlayerItemCooldownEvent event) {
		if(Main.getGame() != null && Main.getGame().getGameState() == LIVE) {
			Ability[] abilities = Main.getPlayerInfo(event.getPlayer()).kit.getAbilities();
			for(Ability a : abilities) {
				a.onItemCooldown(event);
			}
		}
	}

	//stop item damage/breaking
	@EventHandler
	public void entityDamageItem(EntityDamageItemEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public void playerItemDamage(PlayerItemDamageEvent event) {
		event.setCancelled(true);
	}

	/*
	@EventHandler
	public void entityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
		//don't get arrows still in motion
		if(event.getEntity() instanceof AbstractArrow aa && aa.isInBlock()) {
			ArrowPierceManager.piercedEntitiesMap.remove(event.getEntity());
		}
	}
	*/
}
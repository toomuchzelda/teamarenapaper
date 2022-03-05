package me.toomuchzelda.teamarenapaper;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
import me.toomuchzelda.teamarenapaper.teamarena.damage.ArrowPierceManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageTimes;
import me.toomuchzelda.teamarenapaper.teamarena.kingofthehill.KingOfTheHill;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitGhost;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitPyro;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitReach;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.PreferenceManager;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

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
				Entry<Player, PlayerInfo> entry = pinfoIter.next();
				
				if(!entry.getKey().isOnline()) {
					pinfoIter.remove();
				}
				else {
					entry.getValue().clearMessageCooldowns();
				}
			}
			DamageTimes.cleanup();
			Main.playerIdLookup.int2ObjectEntrySet().removeIf(idLookupEntry -> !idLookupEntry.getValue().isOnline());
			
			if(MathUtils.random.nextBoolean()) {//MathUtils.randomMax(3) < 3) {
				TeamArena.nextGameType = GameType.KOTH;
			}
			else
				TeamArena.nextGameType = GameType.CTF;
			
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
	
	private HashMap<UUID, CompletableFuture<Map<Preference<?>, ?>>> preferenceFutureMap = new HashMap<>();
	@EventHandler(priority = EventPriority.MONITOR)
	public void asynchronousPlayerPreLoginEventHandler(AsyncPlayerPreLoginEvent e) {
		if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
			return;
		synchronized (preferenceFutureMap) {
			preferenceFutureMap.put(e.getUniqueId(), PreferenceManager.fetchPreferences(e.getUniqueId()));
		}
	}
	
	
	//these three events are called in this order
	@EventHandler
	public void playerLogin(PlayerLoginEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		//todo: read from MySQL server or something for stored player data.
		// or use persistent data containers, or option to use either
		// and also use the PreLoginEvent
		PlayerInfo playerInfo;

		//todo: read perms from db or other
		String playerName = event.getPlayer().getName();
		if ("toomuchzelda".equalsIgnoreCase(playerName) || "jacky8399".equalsIgnoreCase(playerName))
			playerInfo = new PlayerInfo(CustomCommand.PermissionLevel.OWNER, event.getPlayer());
		else
			playerInfo = new PlayerInfo(CustomCommand.PermissionLevel.ALL, event.getPlayer());
		
		synchronized (preferenceFutureMap) {
			CompletableFuture<Map<Preference<?>, ?>> future = preferenceFutureMap.remove(uuid);
			if (future == null) {
				event.disallow(Result.KICK_OTHER, Component.text("Failed to load preferences!")
						.color(NamedTextColor.DARK_RED));
				return;
			}
			playerInfo.setPreferenceValues(future.join());
		}
		
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

		Player player = event.getPlayer();
		PlayerInfo pinfo = Main.getPlayerInfo(player);

		commands.removeIf(commandStr -> {
			Command command = Bukkit.getCommandMap().getCommand(commandStr);
			if (command == null) // command not found
				return true;
			if (command instanceof CustomCommand customCommand) {
				return pinfo.permissionLevel.compareTo(customCommand.permissionLevel) < 0;
			} else if (command.getPermission() != null) {
				return !player.hasPermission(command.getPermission());
			}
			return false;
		});
	}
	
	//public static int i = 0;
	
	//handle tab-completes asynchronously
	/*@EventHandler
	public void asyncTabComplete(AsyncTabCompleteEvent event) {
		//parse if it's a command first
		
		Bukkit.broadcastMessage(event.getBuffer());
		Bukkit.broadcastMessage("----------" + i++);
		/*for(AsyncTabCompleteEvent.Completion completion : event.completions()) {
			Bukkit.broadcast(Component.text(completion.suggestion() + " + ").append(completion.tooltip() == null ? Component.empty() : completion.tooltip()));
		}
		Bukkit.broadcastMessage("============");
		
		String typed = event.getBuffer();
		if(typed.startsWith("/")) {
			int firstSpaceIdx = typed.indexOf(' ');
			String commandName = typed.substring(1, firstSpaceIdx);
			CustomCommand typedCommand = CustomCommand.getFromName(commandName);
			//if it's null it may be a vanilla or other plugin command so just let the normal process happen for that
			if(typedCommand != null) {
				event.setHandled(true);
				String argsString = typed.substring(firstSpaceIdx + 1); //get all the arguments
				String[] args = argsString.split(" ");
				// if the typed args ends with " " we need to manually add the space to the args array
				if(argsString.endsWith(" ")) {
					args = Arrays.copyOf(args, args.length + 1);
					args[args.length - 1] = " ";
				}
				List<String> stringSuggestions = typedCommand.tabComplete(event.getSender(), commandName, args);
				LinkedList<AsyncTabCompleteEvent.Completion> completionSuggestions = new LinkedList<>();
				for(final String s : stringSuggestions) {
					AsyncTabCompleteEvent.Completion completion = AsyncTabCompleteEvent.Completion.completion(s);
					completionSuggestions.add(completion);
				}
				
				event.completions(completionSuggestions);
			}
		}
	}*/
	
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
		
		//prevent them from moving outside the game border
		if(!event.isCancelled()) {
			Vector to = event.getTo().toVector();
			if(!game.getBorder().contains(to)) {
				event.setCancelled(true);
				//if they're hitting the bottom border call it falling into the void
				if(to.getY() < game.getBorder().getMinY()) {
					if(game.getGameState() == LIVE) {
						event.setCancelled(false);
						EntityDamageEvent dEvent = new EntityDamageEvent(event.getPlayer(), EntityDamageEvent.DamageCause.VOID, 999);
						DamageEvent.createDamageEvent(dEvent);
					}
				}
			}
		}
	}

	@EventHandler
	public void blockBreak(BlockBreakEvent event) {
		if(event.getPlayer().getGameMode() != GameMode.CREATIVE)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void inventoryCreative(InventoryCreativeEvent event) {
		if(Main.getGame() != null && event.getWhoClicked() instanceof Player p) {
			Ability[] abilities = Kit.getAbilities(p);
			for(Ability a : abilities) {
				if(a instanceof KitReach.ReachAbility) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	/*@EventHandler
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
	}*/

	//create and cache damage events
	@EventHandler
	public void entityDamage(EntityDamageEvent event) {
		DamageEvent.createDamageEvent(event);
	}
	
	@EventHandler
	public void playerDeath(PlayerDeathEvent event) {
		Main.logger().warning("PlayerDeathEvent called! not good");
		Thread.dumpStack();
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
	//this event is fired for shooting bows including by players
	@EventHandler
	public void entityShootBow(EntityShootBowEvent event) {
		event.getProjectile().setVelocity(projectileLaunchVector(event.getEntity(), event.getProjectile().getVelocity()));
		
		if(event.getProjectile() instanceof AbstractArrow aa)
			aa.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
		
		if(event.getEntity() instanceof Player p) {
			Ability[] abilities = Kit.getAbilities(p);
			for(Ability a : abilities) {
				a.onShootBow(event);
			}
		}
	}
	
	@EventHandler
	public void entityLoadCrossbow(EntityLoadCrossbowEvent event) {
		if(event.getEntity() instanceof Player p) {
			Ability[] abilities = Kit.getAbilities(p);
			for(Ability a : abilities) {
				a.onLoadCrossbow(event);
			}
		}
	}

	//^^
	//this event fired by players throwing projectiles (not bows!!)
	@EventHandler
	public void playerLaunchProjectile(PlayerLaunchProjectileEvent event) {
		/*Bukkit.broadcastMessage(event.getItemStack().getType().toString());
		Bukkit.broadcastMessage(event.getProjectile().getVelocity().toString());*/

		event.getProjectile().setVelocity(projectileLaunchVector(event.getPlayer(), event.getProjectile().getVelocity()));
		
		if(Main.getGame() != null) {
			Player p = event.getPlayer();
			if(Main.getGame().getGameState() == LIVE) {
				if (Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(p)) {
					event.setCancelled(true);
					PlayerInfo pinfo = Main.getPlayerInfo(p);
					if (pinfo.getPreference(Preferences.RECEIVE_GAME_TITLES)) {
						PlayerUtils.sendTitle(p, Component.empty(), CaptureTheFlag.CANT_TELEPORT_HOLDING_FLAG_TITLE, 10, 25, 10);
					}
					p.sendMessage(CaptureTheFlag.CANT_TELEPORT_HOLDING_FLAG_MESSAGE);
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 2, 0.5f);
				}
			
				if(!event.isCancelled()) {
					Ability[] abilites = Kit.getAbilities(p);
					for (Ability a : abilites) {
						a.onLaunchProjectile(event);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void playerTeleport(PlayerTeleportEvent event) {
		if(event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND ||
			event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN ||
			event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN)
			return;
		
		if(Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(event.getPlayer())) {
			Player p = event.getPlayer();
			event.setCancelled(true);
			PlayerInfo pinfo = Main.getPlayerInfo(p);
			if(pinfo.getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(p, Component.empty(), CaptureTheFlag.CANT_TELEPORT_HOLDING_FLAG_TITLE, 10, 25, 10);
			}
			p.sendMessage(CaptureTheFlag.CANT_TELEPORT_HOLDING_FLAG_MESSAGE);
			p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 2, 0.5f);
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
	// and the flags in CTF
	@EventHandler
	public void projectileCollide(ProjectileCollideEvent event) {
		if(Main.getGame() != null) {
			if(event.getCollidedWith() instanceof Player p && Main.getGame().isSpectator(p))
				event.setCancelled(true);
			else if(event.getCollidedWith() instanceof ArmorStand stand && Main.getGame() instanceof CaptureTheFlag ctf 
					&& ctf.flagStands.containsKey(stand))
				event.setCancelled(true);
			
			if(!event.isCancelled() && event.getEntity().getShooter() instanceof Player p) {
				for(Ability a : Kit.getAbilities(p)) {
					a.projectileHitEntity(event);
				}
			}
		}
	}
	
	@EventHandler
	public void projectileHit(ProjectileHitEvent event) {
		if(Main.getGame() != null) {
			if(event.getHitBlock() != null && event.getEntity().getShooter() instanceof Player p) {
				Ability[] abilities = Kit.getAbilities(p);
				for(Ability a : abilities) {
					if(a instanceof KitPyro.PyroAbility pyroAbility) {
						pyroAbility.onProjectileHit(event);
						return;
					}
				}
			}
		}
	}

	//stop hunger
	@EventHandler
	public void entityExhaustion(EntityExhaustionEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public void entityRegainHealth(EntityRegainHealthEvent event) {
		if(event.getEntity() instanceof Player p) {
			Main.getPlayerInfo(p).getKillAssistTracker().heal(event.getAmount());
		}
	}

	@EventHandler
	public void arrowBodyCountChange(ArrowBodyCountChangeEvent event) {
		//not worth adding a new method to Ability.java for this one
		if(Main.getGame() != null && Main.getGame().getGameState() == LIVE) {
			if (event.getEntity() instanceof Player p) {
				Ability[] abilities = Kit.getAbilities(p);
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
			Ability[] abilities = Kit.getAbilities(event.getPlayer());
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

	@EventHandler
	public void playerDropItem(PlayerDropItemEvent event) {
		if(event.getPlayer().getGameMode() != GameMode.CREATIVE)
			event.setCancelled(true);
	}

	//stop items being moved from one inventory to another (chests etc)
	@EventHandler
	public void inventoryMoveItem(InventoryMoveItemEvent event) {
		event.setCancelled(true);
	}

	//stop players from messing with the armor of CTF Flags
	@EventHandler
	public void playerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
		if(Main.getGame() instanceof CaptureTheFlag ctf && ctf.flagStands.containsKey(event.getRightClicked())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerArmorChange(InventoryClickEvent e) {
		if (!(Main.getGame() instanceof CaptureTheFlag ctf))
			return;
		Player player = (Player) e.getWhoClicked();
//		player.sendMessage(MessageFormat.format("click: {0}, slot type: {1}, action: {2}", e.getClick(), e.getSlotType(), e.getAction()));
		ItemStack toCheck = null;
		InventoryAction action = e.getAction();
		// these two actions move the current item so check the current item
		if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.HOTBAR_SWAP) {
			toCheck = e.getCurrentItem();
		} else if (e.getSlotType() == InventoryType.SlotType.ARMOR) {
			toCheck = e.getCursor();
		}
//		player.sendMessage("ItemStack to check: " + toCheck);
		
		if (toCheck != null && ctf.isFlagItem(toCheck)) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerArmorDrag(InventoryDragEvent e) {
		if (!(Main.getGame() instanceof CaptureTheFlag ctf))
			return;
		
		if (ctf.isFlagItem(e.getOldCursor())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void playerInteract(PlayerInteractEvent event) {
		if(Main.getGame() != null) {
			Main.getGame().onInteract(event);

			Ability[] abilities = Kit.getAbilities(event.getPlayer());
			for (Ability a : abilities) {
				a.onInteract(event);
			}
		}
	}

	@EventHandler
	public void playerInteractEntity(PlayerInteractEntityEvent event) {
		if(Main.getGame() != null) {
			Main.getGame().onInteractEntity(event);

			Ability[] abilities = Kit.getAbilities(event.getPlayer());
			for(Ability a : abilities) {
				a.onInteractEntity(event);
			}
		}
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
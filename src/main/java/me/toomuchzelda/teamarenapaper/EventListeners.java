package me.toomuchzelda.teamarenapaper;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import me.toomuchzelda.teamarenapaper.explosions.EntityExplosionInfo;
import me.toomuchzelda.teamarenapaper.explosions.ExplosionManager;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitbox;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import me.toomuchzelda.teamarenapaper.sql.*;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
import me.toomuchzelda.teamarenapaper.teamarena.damage.ArrowPierceManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.GameScheduler;
import me.toomuchzelda.teamarenapaper.teamarena.kits.*;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.*;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorldBorder;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static me.toomuchzelda.teamarenapaper.teamarena.GameState.DEAD;
import static me.toomuchzelda.teamarenapaper.teamarena.GameState.LIVE;

public class EventListeners implements Listener
{

	public static final boolean[] BREAKABLE_BLOCKS;
	private final ConcurrentHashMap<UUID, Map<Preference<?>, ?>> preferenceFutureMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, String> defaultKitMap = new ConcurrentHashMap<>();

	static {
		BREAKABLE_BLOCKS = new boolean[Material.values().length];
		Arrays.fill(BREAKABLE_BLOCKS, false);

		for(Material mat : Material.values()) {
			if(mat.isBlock() && !mat.isCollidable() && !mat.name().endsWith("SIGN") && !mat.name().endsWith("TORCH")) {
				setBlockBreakable(mat);
			}
		}
	}

	private static void setBlockBreakable(Material mat) {
		BREAKABLE_BLOCKS[mat.ordinal()] = true;
	}

	private static boolean isBlockBreakable(Material mat) {
		return BREAKABLE_BLOCKS[mat.ordinal()];
	}

	public EventListeners(Plugin plugin) {
		Bukkit.getServer().getPluginManager().registerEvents(this,plugin);
	}

	//run the TeamArena tick
	@EventHandler
	public void endTick(ServerTickEndEvent event) {
		PacketListeners.cancelDamageSounds = false;

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

			Main.playerIdLookup.entrySet().removeIf(idLookupEntry -> !idLookupEntry.getValue().isOnline());

			PacketEntityManager.cleanUp();

			//might as well reset
			ItemUtils._uniqueName = 0;

			// initialize next game
			if (TeamArena.nextGameType == null) {
				TeamArena.nextGameType = GameType.values()[MathUtils.random.nextInt(GameType.values().length)];
			}

			//create the next game
			Main.setGame(GameScheduler.getNextGame());
		}

		try {
			Main.getGame().tick();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		try {
			PacketEntityManager.tick();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		try {
			FakeHitboxManager.tick();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		//every 3 minutes
		int count = event.getTickNumber() % (3 * 60  *20);
		if(count == 0) {
			ArrowPierceManager.cleanUp();
		}
		else if(count == 10) {
			for(PlayerInfo pinfo : Main.getPlayerInfos()) {
				pinfo.getMetadataViewer().cleanUp();
			}
		}
		else if(count == 20) {
			FakeHitboxManager.cleanUp();
		}

		PacketListeners.cancelDamageSounds = true;
	}

	@EventHandler
	public void asyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
			return;

		//if in offline mode, get UUID from stored playerinfo, by the player's name.
		// for players joining for the first time a temporary entry in the PlayerInfo table
		// is made for them and deleted afterwards.
		// why do i need offline mode? it is very good in testing with bots
		if(!DatabaseManager.ONLINE_MODE) {
			//don't need bukkit async scheduler as this event is called async
			DBGetUuidByName getUuidByName = new DBGetUuidByName(event.getName());
			try {
				UUID newUuid = getUuidByName.run();
				if(newUuid != null) {
					PlayerProfile old = event.getPlayerProfile();
					PlayerProfile newProfile = new CraftPlayerProfile(newUuid, old.getName());
					newProfile.setTextures(old.getTextures());
					newProfile.setProperties(old.getProperties());
					event.setPlayerProfile(newProfile);
				}
			}
			catch (SQLException | IllegalArgumentException exception) {
				//sql exception will already be printed
				if(exception instanceof IllegalArgumentException)
					exception.printStackTrace();

				event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
				event.kickMessage(Component.text("Database error"));
				return;
			}
		}

		final UUID uuid = event.getUniqueId();

		//update player info, cancel and return if fail
		DBSetPlayerInfo setPlayerInfo = new DBSetPlayerInfo(uuid, event.getName());
		try {
			setPlayerInfo.run();
		}
		catch(SQLException e) {
			event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
			event.kickMessage(TextUtils.getRGBManiacComponent(
					Component.text("Could not update DBPlayerInfo in AsyncPreLogin! Please tell an admin."),
					Style.empty(), 0d));
			return;
		}

		//load preferences from DB
		// cancel and return if fail
		DBGetPreferences getPreferences = new DBGetPreferences(uuid);
		Map<Preference<?>, ?> retrievedPrefs;
		try {
			retrievedPrefs = getPreferences.run();
			preferenceFutureMap.put(uuid, retrievedPrefs);
		}
		catch (SQLException e) {
			event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
			event.kickMessage(Component.text("Could not retrieve preferences! Run for your lives!!!", MathUtils.randomTextColor()));
			return;
		}

		//load default kit from DB
		// log error and just use default kit if fail
		DBGetDefaultKit getDefaultKit = new DBGetDefaultKit(uuid);
		String defaultKit;
		try {
			defaultKit = getDefaultKit.run();
		}
		catch(SQLException e) {
			defaultKit = getDefaultKit.getDefaultValue();
			Main.logger().severe("Could not load default kit for " + uuid.toString() + ", username " + event.getName());
		}
		defaultKitMap.put(uuid, defaultKit);
	}

	//these three events are called in this order
	@EventHandler
	public void playerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		PlayerInfo playerInfo;

		//todo: read perms from db or other
		if (player.isOp()) {
			playerInfo = new PlayerInfo(CustomCommand.PermissionLevel.OWNER, player);
			Bukkit.getScheduler().runTask(Main.getPlugin(),
					() -> player.sendMessage(Component.text("Your rank has been updated to OWNER", NamedTextColor.GREEN)));
		} else {
			playerInfo = new PlayerInfo(CustomCommand.PermissionLevel.ALL, player);
		}

		//remove the default kit now so if the preferences are null and the method returns here
		// there isn't a memory leak in the map.
		String defaultKit = defaultKitMap.remove(uuid);
		Map<Preference<?>, ?> prefMap = preferenceFutureMap.remove(uuid);
		if (prefMap == null) {
			event.disallow(Result.KICK_OTHER, Component.text("Failed to load preferences!")
					.color(TextColors.ERROR_RED));
			return;
		}
		//null values are inserted in the DBGetPreferences operation to signal that a previously
		// stored value is now invalid for some reason.
		// so notify the player here of that.
		for(var entry : prefMap.entrySet()) {
			Preference<?> pref = entry.getKey();
			if(entry.getValue() == null) {
				((Map.Entry<Preference<?>, Object> ) entry).setValue(pref.getDefaultValue());
				Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
					player.sendMessage(Component.text(
							"Your previous set value for preference " + pref.getName() +
									" is now invalid and has been reset to default: " + pref.getDefaultValue().toString() +
									". This may have happened because the preference itself was changed or perhaps due to " +
									"some extraneous shenanigans and perchance, a sizeable portion of tomfoolery.",
							TextColors.ERROR_RED));}
				);
			}
		}
		playerInfo.setPreferenceValues(prefMap);

		if(defaultKit == null)
			defaultKit = DBGetDefaultKit.DEFAULT_KIT;
		playerInfo.defaultKit = defaultKit;

		Main.addPlayerInfo(player, playerInfo);
		Main.playerIdLookup.put(player.getEntityId(), player);
		FakeHitboxManager.addFakeHitbox(player);
		Main.getGame().loggingInPlayer(player, playerInfo);
	}

	@EventHandler
	public void playerSpawn(PlayerSpawnLocationEvent event) {
		event.setSpawnLocation(Main.getPlayerInfo(event.getPlayer()).spawnPoint);
	}

	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		//disable yellow "Player has joined the game" messages
		event.joinMessage(null);
		Main.getPlayerInfo(player).getScoreboard().set();
		// send sidebar objectives
		SidebarManager.getInstance(player).registerObjectives(player);

		Main.getGame().joiningPlayer(player);
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

	@EventHandler
	public void asyncChat(AsyncChatEvent event) {
		if (Main.getPlayerInfo(event.getPlayer()).permissionLevel.compareTo(CustomCommand.PermissionLevel.MOD) >= 0) {
			event.message(MiniMessage.miniMessage().deserialize(PlainTextComponentSerializer.plainText().serialize(event.message())));
		}
	}

	@EventHandler
	public void playerQuit(PlayerQuitEvent event) {
		event.quitMessage(null);
		Player leaver = event.getPlayer();
		Main.getGame().leavingPlayer(leaver);
		//Main.getPlayerInfo(event.getPlayer()).nametag.remove();
		FakeHitboxManager.removeFakeHitbox(leaver);
		PlayerInfo pinfo = Main.removePlayerInfo(leaver);

		//save preferences when leaving
		DBSetPreferences.asyncSavePlayerPreferences(leaver, pinfo);

		Main.playerIdLookup.remove(leaver.getEntityId());
	}

	@EventHandler
	public void playerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		TeamArena game = Main.getGame();
		if (game.getGameState() == GameState.GAME_STARTING) {
			if(!game.isSpectator(player)) {
				Location prev = event.getFrom();
				Location next = event.getTo();

				if (prev.getX() != next.getX() || prev.getZ() != next.getZ()) {
					event.setCancelled(true);
					return;
				}
			}
		}
		else if(game.getGameState() == LIVE) {
			for(Ability a : Kit.getAbilities(event.getPlayer())) {
				a.onMove(event);
			}
		}

		Vector from = event.getFrom().toVector();
		Vector to = event.getTo().toVector();

		//prevent them from moving outside the game border
		if (player.getGameMode() != GameMode.SPECTATOR && !game.getBorder().contains(to)) {
			// if they're hitting the bottom border call it falling into the void
			if (to.getY() < game.getBorder().getMinY() && game.getGameState() == LIVE) {
				event.setCancelled(false);
				DamageEvent dEvent = DamageEvent.newDamageEvent(event.getPlayer(), 9999d, DamageType.VOID, null, false);
				Main.getGame().queueDamage(dEvent);
			} else {
				Location newTo = event.getTo().clone();
				newTo.set(from.getX(), from.getY(), from.getZ());
				event.setTo(newTo);
			}
		}

		// dynamic world border
		if (from.distanceSquared(to) != 0) {
			BoundingBox border = game.getBorder();
			// only display when distance to border <= 10 blocks
			if (MathUtils.distanceBetween(border, from, true) <= 10) {
				Vector closest = new Vector(
						from.getX() > border.getCenterX() ? border.getMaxX() : border.getMinX(),
						0,
						from.getZ() > border.getCenterZ() ? border.getMaxZ() : border.getMinZ()
				);

				WorldBorder worldBorder = player.getWorldBorder();
				// calculate the center of the world border
				// such that at size 512 the world border edge would line up with the closest point
				double centerX = closest.getX() + Math.signum(from.getX() - closest.getX()) * 256,
						centerZ = closest.getZ() + Math.signum(from.getZ() - closest.getZ()) * 256;
				if (worldBorder == null || // no border
						((CraftWorldBorder) worldBorder).getHandle().getLerpTarget() == 768 || // fading away
						Math.abs(worldBorder.getCenter().getX() - centerX) > Vector.getEpsilon() ||
						Math.abs(worldBorder.getCenter().getZ() - centerZ) > Vector.getEpsilon()) {
					// wrong center, replace border
					if (worldBorder == null) {
						worldBorder = Bukkit.createWorldBorder();
						worldBorder.setCenter(centerX, centerZ);
						worldBorder.setSize(768);
						worldBorder.setSize(512, 1);
						worldBorder.setWarningDistance(3);
						worldBorder.setWarningTime(0);
						player.setWorldBorder(worldBorder);
					} else {
						worldBorder.setCenter(centerX, centerZ);
						worldBorder.setSize(512, 1);
					}
				}
			} else {
				WorldBorder worldBorder = player.getWorldBorder();
				if (worldBorder != null && ((CraftWorldBorder) worldBorder).getHandle().getLerpRemainingTime() == 0) {
					worldBorder.setSize(768, 2);
					Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
						// check if the world border is the same and has faded out completely
						if (player.getWorldBorder() == worldBorder && worldBorder.getSize() == 768) {
							player.setWorldBorder(null);
						}
					}, 40);
				}
			}
		}

		if(!event.isCancelled() && FakeHitboxManager.ACTIVE) {
			//don't need to updateClients here, packets will be sent after event by server
			FakeHitboxManager.getFakeHitbox(player).updatePosition(event.getTo(), player.getPose(), false);
		}
	}

	@EventHandler
	public void blockBreak(BlockBreakEvent event) {
		//Handling breaking teleporter blocks
		if(Main.getGame() != null && Main.getGame().getGameState() == LIVE){
			BuildingManager.EventListener.onBlockBreak(event);
		}

		if(event.getPlayer().getGameMode() != GameMode.CREATIVE) {
			if(!isBlockBreakable(event.getBlock().getType()))
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void blockPlace(BlockPlaceEvent event) {
		if(event.getPlayer().getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
		}

		Main.getGame().onPlaceBlock(event);

		for(Ability ability : Kit.getAbilities(event.getPlayer())) {
			ability.onPlaceBlock(event);
		}
	}

	@EventHandler
	public void explosionPrime(ExplosionPrimeEvent event) {
		//if there is an EntityExplosionInfo use that to handle it
		EntityExplosionInfo exInfo = ExplosionManager.getEntityInfo(event.getEntity());
		if(exInfo != null) {
			exInfo.handleEvent(event);
		}
	}

	@EventHandler
	public void entityExplode(EntityExplodeEvent event) {
		EntityExplosionInfo exInfo = ExplosionManager.getEntityInfo(event.getEntity());
		if(exInfo != null) {
			exInfo.handleEvent(event);
		}
		else {
			event.blockList().clear();
		}
	}

	@EventHandler
	public void blockExplode(BlockExplodeEvent event) {
		event.blockList().clear();
	}

	@EventHandler
	public void tntPrime(TNTPrimeEvent event) {
		if(event.getReason() == TNTPrimeEvent.PrimeReason.PROJECTILE) {
			event.setCancelled(true);
		}
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
		DamageEvent.handleBukkitEvent(event);
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

	//this event is fired for shooting bows including by players
	@EventHandler
	public void entityShootBow(EntityShootBowEvent event) {
		//event.getProjectile().setVelocity(EntityUtils.projectileLaunchVector(event.getEntity(),
		//		event.getProjectile().getVelocity(), EntityUtils.VANILLA_PROJECTILE_SPRAY));

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
		if(Main.getGame().getGameState() == LIVE) {
			if (event.getEntity() instanceof Player p) {
				Ability[] abilities = Kit.getAbilities(p);
				for (Ability a : abilities) {
					a.onLoadCrossbow(event);
				}
			}
		}
	}

	//^^
	//this event fired by players throwing projectiles (not bows!!)
	@EventHandler
	public void playerLaunchProjectile(PlayerLaunchProjectileEvent event) {
		/*Bukkit.broadcastMessage(event.getItemStack().getType().toString());
		Bukkit.broadcastMessage(event.getProjectile().getVelocity().toString());*/

		//event.getProjectile().setVelocity(EntityUtils.projectileLaunchVector(event.getPlayer(),
		//		event.getProjectile().getVelocity(), EntityUtils.VANILLA_PROJECTILE_SPRAY));

		if(Main.getGame() != null) {
			Player p = event.getPlayer();
			if(Main.getGame().getGameState() == LIVE) {
				if (event.getProjectile() instanceof EnderPearl &&
						Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(p)) {
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
		PlayerTeleportEvent.TeleportCause cause = event.getCause();

		if(Main.getGame() != null) {
			//don't do any game logic if it's done by plugin or a command
			// unknown also tends to be things like server rubber-banding
			if(cause != PlayerTeleportEvent.TeleportCause.PLUGIN &&
					cause != PlayerTeleportEvent.TeleportCause.UNKNOWN &&
					cause != PlayerTeleportEvent.TeleportCause.COMMAND) {

				if(cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL ||
						cause == PlayerTeleportEvent.TeleportCause.END_PORTAL ||
						cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
					event.setCancelled(true);
				}
				else if(!Main.getGame().getBorder().contains(event.getTo().toVector())) {
					event.setCancelled(true);

					if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
						event.getPlayer().sendMessage(Component.text("One of your ender pearls landed outside the border. " +
								"Aim better!").color(TextColors.ERROR_RED));
					}
					else if(cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
						event.getPlayer().sendMessage(Component.text("This fruit tried to take you outside the border, " +
										"so now you just go nowhere because I have deemed finding a safe alternative position" +
										" to be too much trouble (i am lazy). Here's a free diamond! - toomuchzelda")
								.color(TextColors.ERROR_RED));
						event.getPlayer().getInventory().addItem(new ItemStack(Material.DIAMOND));
					}
				}

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
		}

		if(!event.isCancelled() && FakeHitboxManager.ACTIVE) {
			//don't need to updateClients here, packets will be sent after this event.
			FakeHitboxManager.getFakeHitbox(event.getPlayer()).updatePosition(event.getTo(), event.getPlayer().getPose(), false);
		}
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
					a.onProjectileHitEntity(event);
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
		if(event.getEntity() instanceof LivingEntity living) {
			if(KitVenom.POISONED_ENTITIES.containsKey(living)){
				event.setCancelled(true);
			}

			if(!event.isCancelled() && event.getEntity() instanceof Player p) {
				Main.getPlayerInfo(p).getKillAssistTracker().heal(event.getAmount());
			}
		}
	}

	@EventHandler
	public void entityPoseChange(EntityPoseChangeEvent event) {
		if(FakeHitboxManager.ACTIVE && event.getEntity() instanceof Player player) {
			FakeHitbox hitbox = FakeHitboxManager.getFakeHitbox(player);
			hitbox.handlePoseChange(event);
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
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
		}

		TeamArena game = Main.getGame();
		if (game != null) {
			if (game.miniMap.isMapItem(event.getItemDrop().getItemStack()) &&
				player.getCooldown(Material.FILLED_MAP) == 0 && player.isSneaking()) {
				player.setCooldown(Material.FILLED_MAP, 40 * 20);
				var playerInfo = Main.getPlayerInfo(player);
				// spawn a random owned graffiti
				var ownedGraffiti = game.graffiti.getAllGraffiti().stream()
					.filter(playerInfo::hasCosmeticItem)
					.toList();
				if (ownedGraffiti.size() != 0) {
					game.graffiti.spawnGraffiti(player, ownedGraffiti.get(MathUtils.random.nextInt(ownedGraffiti.size())));
					event.setCancelled(true);
					return;
				}
			}

			if (game.getGameState() == LIVE) {
				for (Ability a : Kit.getAbilities(player)) {
					a.onPlayerDropItem(event);
				}
			}
		}
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
		TeamArena game = Main.getGame();
		InventoryAction action = e.getAction();
		// these two actions move the current item so check the current item
		if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.HOTBAR_SWAP) {
			if (!game.isWearableArmorPiece(e.getCurrentItem()))
				e.setCancelled(true);
			else if (e.getHotbarButton() != -1 && !game.isWearableArmorPiece(e.getView().getBottomInventory().getItem(e.getHotbarButton())))
				e.setCancelled(true);
		} else if (e.getSlotType() == InventoryType.SlotType.ARMOR) {
			if (!game.isWearableArmorPiece(e.getCursor())) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerArmorDrag(InventoryDragEvent e) {
		ItemStack draggedItem = e.getOldCursor();

		boolean isDraggingOnArmorSlot = false;
		if(e.getInventory().getHolder() instanceof HumanEntity) {
			for(int i : e.getInventorySlots()) {
				if(ItemUtils.isArmorSlotIndex(i)) {
					isDraggingOnArmorSlot = true;
					break;
				}
			}
		}

		if(isDraggingOnArmorSlot && !Main.getGame().isWearableArmorPiece(draggedItem)) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void playerInteract(PlayerInteractEvent event) {
		if(Main.getGame() != null) {
			Main.getGame().onInteract(event);

			if(Main.getGame().getGameState() == LIVE) {
				Ability[] abilities = Kit.getAbilities(event.getPlayer());
				for (Ability a : abilities) {
					a.onInteract(event);
				}
			}
		}
	}

	@EventHandler
	public void playerInteractEntity(PlayerInteractEntityEvent event) {
		if(Main.getGame() != null) {
			Main.getGame().onInteractEntity(event);

			if(Main.getGame().getGameState() == LIVE) {
				Ability[] abilities = Kit.getAbilities(event.getPlayer());
				for (Ability a : abilities) {
					a.onInteractEntity(event);
				}
			}
		}
	}

	@EventHandler
	public void playerUseUnknownEntity(PlayerUseUnknownEntityEvent event) {
		//prevent right clicks being handled 4 times
		boolean handle = false;
		if(!event.isAttack()) {
			PlayerInfo pinfo = Main.getPlayerInfo(event.getPlayer());
			int currentTick = TeamArena.getGameTick();
			int idx = event.getHand().ordinal();
			if (pinfo.lastInteractUnknownEntityTimes[idx] != currentTick) {
				pinfo.lastInteractUnknownEntityTimes[idx] = currentTick;
				handle = true;
			}
		}
		else {
			handle = true;
		}

		if(handle) {
			boolean success = PacketEntityManager.handleInteract(event);

			if(!success) {
				Main.logger().warning(event.getPlayer().getName() + " used an Unknown Entity that is not a PacketEntity" +
						" or a FakeHitbox");
			}
		}
	}

	@EventHandler
	public void onRecipeUnlock(PlayerRecipeDiscoverEvent e) {
		e.setCancelled(true);
	}

	@EventHandler
	public void onEat(PlayerItemConsumeEvent event){
		Player player = event.getPlayer();
		if(KitVenom.POISONED_ENTITIES.containsKey((LivingEntity)player)){
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void playerAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
		event.setCancelled(true);
	}


	static final Component MOTD_SEPARATOR = Component.textOfChildren(Component.space(),
			Component.text("|", NamedTextColor.DARK_RED, TextDecoration.BOLD), Component.space());
	static final Component MOTD = Component.textOfChildren(
			Component.text("               "),
			TextUtils.getUselessRGBText("Blue Warfare", TextColor.color(0x060894), TextColor.color(0x1ad3f0)),
			Component.space(),
			TextUtils.getUselessRainbowText("[1.19]"),
			Component.newline(),
			TextUtils.getUselessRGBText("King of the Hill", TextColor.color(0x595959), TextColor.color(0xadadad)),
			MOTD_SEPARATOR,
			TextUtils.getUselessRGBText("Capture the Flag", TextColor.color(0x1d6e16), TextColor.color(0x00ff40)),
			MOTD_SEPARATOR,
			TextUtils.getUselessRGBText("UnbalancedBS", TextColor.color(0x631773), TextColor.color(0xff00f2))
	);
	@EventHandler
	public void onMotd(PaperServerListPingEvent e) {
		e.motd(MOTD);
	}

	@EventHandler
	public void onItemFrame(PlayerItemFrameChangeEvent e) {
		if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
			e.setCancelled(true);
	}

	@EventHandler
	public void onSwapMainHand(PlayerSwapHandItemsEvent e) {
		Main.getGame().graffiti.onSwapHandItems(e);
	}

}
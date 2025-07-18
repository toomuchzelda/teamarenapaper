package me.toomuchzelda.teamarenapaper;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import me.toomuchzelda.teamarenapaper.httpd.HttpDaemon;
import me.toomuchzelda.teamarenapaper.sql.*;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LoginHandler
{
	private record DBLoadedData(Map<Preference<?>, ?> preferenceMap, String defaultKit, PermissionLevel permissionLevel) {}

	private static final ConcurrentHashMap<UUID, DBLoadedData> loadedDbDataCache = new ConcurrentHashMap<>();

	static void handleAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
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
			if (retrievedPrefs == null) {
				throw new IllegalStateException("DBGetPreferences returned null pref map.");
			}
		}
		catch (SQLException | IllegalStateException | IllegalArgumentException e) {
			event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
			event.kickMessage(Component.text("Could not retrieve preferences! Run for your life!!!", MathUtils.randomTextColor()));
			if (e instanceof IllegalStateException) { // SQLException message and stack trace will be printed already by DBOperation
				Main.logger().severe("Could not load preferences for " + uuid + ". " + e.getMessage());
			}
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

		// Load CustomCommand permission level if any set.
		PermissionLevel permissionLevel;
		DBGetPermissionLevel getPermissionLevel = new DBGetPermissionLevel(uuid);
		try {
			permissionLevel = getPermissionLevel.run();
			if (permissionLevel == null) {
				permissionLevel = PermissionLevel.ALL;
			}
		}
		catch (SQLException e) {
			permissionLevel = PermissionLevel.ALL;
		}

		DBLoadedData data = new DBLoadedData(retrievedPrefs, defaultKit, permissionLevel);
		loadedDbDataCache.put(uuid, data);
	}

	static void asyncMonitor(AsyncPlayerPreLoginEvent event) {
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
			loadedDbDataCache.remove(event.getUniqueId());
		}
	}

	static void handlePlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final UUID uuid = player.getUniqueId();
		final PlayerInfo playerInfo;

		List<Component> messages = new ArrayList<>();

		Main.logger().info(player.getName() + " PlayerJoinEvent (pre)");

		// Remove before check if they are allowed to join to prevent memory leak
		final DBLoadedData loadedData = loadedDbDataCache.remove(uuid);

		if (PlayerUtils.getOpLevel(player) == 4) { // Being max level op on vanilla server overrides DB
			playerInfo = new PlayerInfo(PermissionLevel.OWNER, player);
		} else {
			playerInfo = new PlayerInfo(loadedData.permissionLevel(), player);
		}

		if (playerInfo.permissionLevel != PermissionLevel.ALL) {
			messages.add(Component.text("Your rank has been updated to " + playerInfo.permissionLevel.name(), NamedTextColor.GREEN));
		}

		Map<Preference<?>, ?> prefMap = loadedData.preferenceMap();
		if (prefMap == null) {
			Main.logger().severe("prefMap is null in PlayerJoinEvent. Should be impossible.");
			prefMap = new HashMap<>();
		}
		//null values are inserted in the DBGetPreferences operation to signal that a previously
		// stored value is now invalid for some reason.
		// so notify the player here of that.
		for(var entry : prefMap.entrySet()) {
			Preference<?> pref = entry.getKey();
			if(entry.getValue() == null) {
				((Map.Entry<Preference<?>, Object> ) entry).setValue(pref.getDefaultValue());
				messages.add(Component.text(
						"Your previous set value for preference " + pref.getName() +
							" is now invalid and has been reset to default: " + pref.getDefaultValue() +
							". This may have happened because the preference itself was changed or perhaps due to " +
							"some extraneous shenanigans and perchance, a sizeable portion of tomfoolery.",
						TextColors.ERROR_RED));
			}
		}
		playerInfo.setPreferenceValues(prefMap);

		String defaultKit = loadedData.defaultKit();
		if(defaultKit == null)
			defaultKit = DBGetDefaultKit.DEFAULT_KIT;
		playerInfo.defaultKit = defaultKit;

		Main.playerIdLookup.put(player.getEntityId(), player);

		HttpDaemon hd = Main.getHttpDaemon();
		if (hd != null)
			hd.onConnect(player, player.getAddress().getAddress());

		Main.getGame().loggingInPlayer(player, playerInfo);

		Main.logger().info(player.getName() + " PlayerJoinEvent");

		FakeHitboxManager.addFakeHitbox(player);

		//disable yellow "Player has joined the game" messages
		event.joinMessage(null);

		Main.addPlayerInfo(player, playerInfo);
		playerInfo.getScoreboard().set();
		// send sidebar objectives
		SidebarManager.getInstance(player).registerObjectives(player);

		Main.getGame().joiningPlayer(player, playerInfo);

		PacketPlayer.onJoin(event);
		DisguiseManager.applyViewedDisguises(player);

		messages.forEach(player::sendMessage);
		player.addCustomChatCompletions(List.of("<item>"));
	}
}

package me.toomuchzelda.teamarenapaper;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
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
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LoginHandler
{
	private record DBLoadedData(DBGetPreferences.Result preferenceMap, String defaultKit, PermissionLevel permissionLevel) {}

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
		DBGetPreferences.Result retrievedPrefs;
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

	static void validateLoginMonitor(PlayerConnectionValidateLoginEvent event) {
		if (event.isAllowed()) {
			HttpDaemon hd = Main.getHttpDaemon();
			if (hd == null) return;
			if (event.getConnection().getClientAddress().getAddress() instanceof InetAddress ia)
				hd.onConnect(ia);
			else {
				Main.logger().log(Level.WARNING, "Bad address in PCVLEvent", new RuntimeException());
			}
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

		DBGetPreferences.Result prefResult = loadedData.preferenceMap();
		Map<Preference<?>, ?> prefMap = prefResult.preferences();
		if (prefMap == null) {
			Main.logger().severe("prefMap is null in PlayerJoinEvent. Should be impossible.");
			prefMap = new HashMap<>();
		}

		if (!prefResult.preferenceMessages().isEmpty()) {
			messages.add(Component.text("Some preferences require your attention:", NamedTextColor.GOLD));
			for (Map.Entry<String, Component> entry : prefResult.preferenceMessages().entrySet()) {
				TextComponent.Builder builder = Component.text().append(Component.text("  "), entry.getValue());
				Preference<?> preference = Preference.getByName(entry.getKey());
				if (preference != null) {
					if (preference.getValues() != null)
						builder.clickEvent(ClickEvent.runCommand("/prefs gui " + entry.getKey()));
					else
						builder.clickEvent(ClickEvent.suggestCommand("/prefs change " + entry.getKey() + " <value>"));
					builder.hoverEvent(Component.text("Click to change the preference", NamedTextColor.BLUE));
				} else {
					builder.hoverEvent(Component.text("This preference does not exist.", TextColors.ERROR_RED));
				}
				messages.add(builder.build());
			}
			messages.add(Component.text("You can click on the message to change the preference, if possible.", NamedTextColor.BLUE));
		}

		playerInfo.setPreferenceValues(prefMap);

		String defaultKit = loadedData.defaultKit();
		if(defaultKit == null)
			defaultKit = DBGetDefaultKit.DEFAULT_KIT;
		playerInfo.defaultKit = defaultKit;

		Main.playerIdLookup.put(player.getEntityId(), player);

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

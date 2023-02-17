package me.toomuchzelda.teamarenapaper;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import me.toomuchzelda.teamarenapaper.sql.*;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticItem;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticsManager;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LoginHandler
{
	private record DBLoadedData(Map<Preference<?>, ?> preferenceMap, Map<CosmeticType, NamespacedKey> cosmeticMap,
								String defaultKit, CustomCommand.PermissionLevel permissionLevel) {}

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
		DBGetPreferences.LoadedPreferences retrievedPrefs;
		try {
			retrievedPrefs = getPreferences.run();
			if (retrievedPrefs == null) {
				throw new IllegalStateException("DBGetPreferences returned null pref map.");
			}
		}
		catch (SQLException | IllegalStateException e) {
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
		CustomCommand.PermissionLevel permissionLevel;
		DBGetPermissionLevel getPermissionLevel = new DBGetPermissionLevel(uuid);
		try {
			permissionLevel = getPermissionLevel.run();
			if (permissionLevel == null) {
				permissionLevel = CustomCommand.PermissionLevel.ALL;
			}
		}
		catch (SQLException e) {
			permissionLevel = CustomCommand.PermissionLevel.ALL;
		}

		DBLoadedData data = new DBLoadedData(retrievedPrefs.preferenceMap(), retrievedPrefs.cosmeticMap(), defaultKit, permissionLevel);
		loadedDbDataCache.put(uuid, data);
	}

	static void asyncMonitor(AsyncPlayerPreLoginEvent event) {
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
			loadedDbDataCache.remove(event.getUniqueId());
		}
	}

	static void handlePlayerLogin(PlayerLoginEvent event) {
		final Player player = event.getPlayer();
		final UUID uuid = player.getUniqueId();
		final PlayerInfo playerInfo;
		List<Component> motd = new ArrayList<>();

		// Remove before check if they are allowed to join to prevent memory leak
		final DBLoadedData loadedData = loadedDbDataCache.remove(uuid);
		if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
			return;
		}

		if (PlayerUtils.getOpLevel(player) == 4) { // Being max level op on vanilla server overrides DB
			playerInfo = new PlayerInfo(CustomCommand.PermissionLevel.OWNER, player);
		} else {
			playerInfo = new PlayerInfo(loadedData.permissionLevel(), player);
		}

		if (playerInfo.permissionLevel != CustomCommand.PermissionLevel.ALL) {
			motd.add(Component.text("Your rank has been updated to " + playerInfo.permissionLevel.name(), NamedTextColor.GREEN));
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
			@SuppressWarnings("rawtypes")
			Preference pref = entry.getKey();
			Object value = entry.getValue();
			if(value == null) {
				motd.add(Component.text("Your preference " + pref.getName() +
						" has been reset to default (" + pref.serialize(pref.getDefaultValue()) + ").",
					TextColors.ERROR_RED));
			} else {
				playerInfo.setPreference(pref, value);
			}
		}

		loadedData.cosmeticMap.forEach((cosmeticType, key) -> {
			if (playerInfo.hasCosmeticItem(key)) {
				playerInfo.setSelectedCosmetic(cosmeticType, key);
			} else {
				CosmeticItem item = CosmeticsManager.getCosmetic(cosmeticType, key);
				String name = item != null ? item.name : key.toString();
				motd.add(Component.text("Your equipped " + cosmeticType + " " + name + " has expired!", NamedTextColor.GOLD));
			}
		});

		String defaultKit = loadedData.defaultKit();
		if(defaultKit == null)
			defaultKit = DBGetDefaultKit.DEFAULT_KIT;
		playerInfo.defaultKit = defaultKit;

		Main.addPlayerInfo(player, playerInfo);
		Main.playerIdLookup.put(player.getEntityId(), player);
		FakeHitboxManager.addFakeHitbox(player);
		Main.getGame().loggingInPlayer(player, playerInfo);

		if (motd.size() != 0)
			Bukkit.getScheduler().runTask(Main.getPlugin(), () -> motd.forEach(player::sendMessage));
	}
}

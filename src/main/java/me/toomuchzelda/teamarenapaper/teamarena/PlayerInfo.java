package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageLogEntry;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.damage.KillAssistTracker;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

//container class to store per-player info
public class PlayerInfo
{
	public CustomCommand.PermissionLevel permissionLevel;
	public TeamArenaTeam team;
	public Location spawnPoint;
	public PacketHologram nametag;
	public Kit kit;
	public Kit activeKit; // kit they've selected vs the kit they're currently using
	//todo: read from DB or other persistent storage
	//todo: prob make a preference
	public String defaultKit;

	private Map<Preference<?>, Object> preferences = new HashMap<>();

	private Map<CosmeticType, NamespacedKey> selectedCosmetic = new EnumMap<>(CosmeticType.class);

	private final Map<String, Integer> messageCooldowns = new HashMap<>();
	private final LinkedList<DamageLogEntry> damageReceivedLog;
	private final KillAssistTracker killAssistTracker;
	private final PlayerScoreboard scoreboard; //scoreboard they view
	private final MetadataViewer metadataViewer; //custom entity metadata tracker

	public double kills;
	//for right clicking the leather chestplate
	public boolean viewingGlowingTeammates;

	/**
	 * PlayerUseUnknownEntityEvent is called 4 times for right clicks.
	 * So store the last tick they interacted here and don't handle the event more than once for each hand
	 * Index is ordinal of EquipmentSlot hand.
	 */
	public int[] lastInteractUnknownEntityTimes;

	public PlayerInfo(CustomCommand.PermissionLevel permissionLevel, Player player) {
		team = null;
		spawnPoint = null;
		nametag = null;
		kit = null;
		activeKit = null;
		//should now be initialised in EventListeners
		//defaultKit = "Trooper";

		this.permissionLevel = permissionLevel;
		damageReceivedLog = new LinkedList<>();

		killAssistTracker = new KillAssistTracker(player);
		kills = 0;
		viewingGlowingTeammates = false;
		lastInteractUnknownEntityTimes = new int[2];

		this.scoreboard = new PlayerScoreboard(player);
		this.metadataViewer = new MetadataViewer(player);
	}

	public void setPreferenceValues(Map<Preference<?>, ?> values) {
		preferences = new HashMap<>(values);
	}

	public boolean hasPreference(Preference<?> preference) {
		Object value = preferences.get(preference);
		return value != null && !preference.getDefaultValue().equals(value);
	}

	public <T> void resetPreference(Preference<T> preference) {
		preferences.remove(preference);
	}

	public <T> void setPreference(Preference<T> preference, T value) {
		preferences.put(preference, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T getPreference(Preference<T> preference) {
		return (T) preferences.getOrDefault(preference, preference.getDefaultValue());
	}

	public Map<Preference<?>, ?> getPreferences() {
		return Collections.unmodifiableMap(preferences);
	}

	public boolean hasCosmeticItem(NamespacedKey key) {
		return true;
	}

	public Set<NamespacedKey> getCosmeticItems(CosmeticType type) {
		return switch (type) {
			case GRAFFITI -> Main.getGame().graffiti.getAllGraffiti();
		};
	}

	public Optional<NamespacedKey> getSelectedCosmetic(CosmeticType type) {
		return Optional.ofNullable(selectedCosmetic.get(type));
	}

	public void setSelectedCosmetic(@NotNull CosmeticType type, @NotNull NamespacedKey key) {
		if (!type.checkKey(key)) {
			throw new IllegalArgumentException("key incompatible with type");
		}
		selectedCosmetic.put(type, key);
	}

	/**
	 * see if enough time has passed for a message to be sent to player (no spam)
	 * @param message message, or a key representing the message, to be sent
	 * @param cooldown number of ticks in between each message sending
	 * @return the meaning of the universe
	 */
	public boolean messageHasCooldowned(String message, int cooldown) {
		int currentTick = TeamArena.getGameTick();
		Integer time = messageCooldowns.get(message);
		if(time != null) {
			if(currentTick - time >= cooldown) {
				messageCooldowns.put(message, currentTick);
				return true;
			}
			else
				return false;
		}
		else {
			messageCooldowns.put(message, currentTick);
			return true;
		}
	}

	public void clearMessageCooldowns() {
		messageCooldowns.clear();
	}

	public void logDamageReceived(Damageable damaged, DamageType damageType, double damage, @Nullable Entity damager, int time) {
		Component damagerComponent = damager == null ? null : EntityUtils.getComponent(damager);
		damageReceivedLog.add(new DamageLogEntry(damageType, damage, damagerComponent, time));
	}

	public MetadataViewer getMetadataViewer() {
		return metadataViewer;
	}

	public PlayerScoreboard getScoreboard() {
		return scoreboard;
	}

	public void clearDamageReceivedLog() {
		damageReceivedLog.clear();
	}

	public KillAssistTracker getKillAssistTracker() {
		return killAssistTracker;
	}

	public List<DamageLogEntry> getDamageReceivedLog() {
		return damageReceivedLog;
	}
}

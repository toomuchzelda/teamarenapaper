package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.core.PacketHologram;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageLogEntry;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.damage.KillAssistTracker;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.*;
import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//container class to store per-player info
public class PlayerInfo
{
	public final byte permissionLevel;
	public TeamArenaTeam team;
	public Location spawnPoint;
	public PacketHologram nametag;
	public Kit kit;
	public Kit activeKit; // kit they've selected vs the kit they're currently using
	//todo: read from DB or other persistent storage
	public String defaultKit;

	//todo make array
	private HashMap<Preference<?>, Object> preferences = new HashMap<>(); //todo make array
	//todo make array
	//todo make array
	//todo make array
	//todo make array
	//todo make array
	//todo make array
	//todo make array
	//todo make array
	//todo make array
	//todo make array
	//todo make array
	//todo make array
	
	private final HashMap<String, Integer> messageCooldowns = new HashMap<String, Integer>(); 
	private final LinkedList<DamageLogEntry> damageReceivedLog;
	private final KillAssistTracker killAssistTracker;

	public double kills;

	public PlayerInfo(byte permissionLevel, Player player) {
		team = null;
		spawnPoint = null;
		nametag = null;
		kit = null;
		activeKit = null;
		defaultKit = "Trooper";

		this.permissionLevel = permissionLevel;
		damageReceivedLog = new LinkedList<>();

		killAssistTracker = new KillAssistTracker(player);

		kills = 0;
	}
	
	public void setPreferenceValues(Map<Preference<?>, ?> values) {
		preferences = new HashMap<>(values); // disgusting and slow
	}
	
	public <T> void setPreference(Preference<T> preference, T value) {
		preferences.put(preference, value);
	}

	@SuppressWarnings("unchecked") //bad cod emoment
	public <T> T getPreference(Preference<T> preference) {
		return (T) preferences.getOrDefault(preference, preference.getDefaultValue());
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
		DamageLogEntry dinfo = new DamageLogEntry(damageType, damage, damager, time);
		damageReceivedLog.add(dinfo);
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

package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.core.Hologram;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.*;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

//container class to store per-player info
public class PlayerInfo
{
	public final byte permissionLevel;
	public TeamArenaTeam team;
	public Location spawnPoint;
	public Hologram nametag;
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

	// cringe
	//from 1-10. number of ticks in between particle play
	//public byte kothHillParticles;
	//public final PreferenceKothHillParticles kothParticles;
	//whether the player wants to see titles during/regarding gameplay (they will also get a chat message regardless)
	//public boolean receiveGameTitles;
	//public final PreferenceReceiveGameTitles receiveGameTitles;
	//sound played when hit a bow shot
	//public Sound bowShotHitSound;
	//public final PreferenceBowHitSound bowHitSound;
	//whether the screen should tilt when taking damage
	//public boolean damageTilt;
	//public final PreferenceDamageTilt damageTilt;
	
	//for kit related messages; play in chat, action bar, or both
	//public boolean kitActionBarMessages;
	//public final PreferenceKitActionBar kitActionBar;
	//public boolean kitChatMessages;
	//public final PreferenceKitChatMessages kitChatMessages;

	public PlayerInfo(byte permissionLevel) {
		team = null;
		spawnPoint = null;
		nametag = null;
		kit = null;
		activeKit = null;
		defaultKit = "Trooper";
		//kothHillParticles = 1;
		//kothParticles = new PreferenceKothHillParticles();
		//receiveGameTitles = true;
		//receiveGameTitles = new PreferenceReceiveGameTitles();
		//damageTilt = true;
		//damageTilt = new PreferenceDamageTilt();
		
		this.permissionLevel = permissionLevel;
		//bowShotHitSound = Sound.ENTITY_ARROW_HIT_PLAYER;
		//bowHitSound = new PreferenceBowHitSound();
		
		//kitActionBarMessages = true;
		//kitActionBar = new PreferenceKitActionBar();
		//kitChatMessages = true;
		//kitChatMessages = new PreferenceKitChatMessages();
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
}

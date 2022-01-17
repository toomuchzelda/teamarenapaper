package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.core.Hologram;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.*;
import org.bukkit.Location;

//container class to store per-player info
public class PlayerInfo
{
	public final byte permissionLevel;
	public TeamArenaTeam team;
	public Location spawnPoint;
	public Hologram nametag;
	public Kit kit;
	//todo: read from DB or other persistent storage
	public String defaultKit;
	
	
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
	
	private Object[] preferenceValues;

	public PlayerInfo(byte permissionLevel) {
		team = null;
		spawnPoint = null;
		nametag = null;
		kit = null;
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
	
	public void setPreferenceValues(Object[] values) {
		this.preferenceValues = values;
	}
	
	public Object getPreference(EnumPreference preference) {
		return preferenceValues[preference.ordinal()];
	}
}

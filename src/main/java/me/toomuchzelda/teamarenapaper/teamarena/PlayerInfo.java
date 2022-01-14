package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.core.Hologram;
import me.toomuchzelda.teamarenapaper.core.WrappedBoolean;
import me.toomuchzelda.teamarenapaper.core.WrappedByte;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.*;
import org.bukkit.Location;
import org.bukkit.Sound;

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
	public final PreferenceKothParticles kothParticles;
	//whether the player wants to see titles during/regarding gameplay (they will also get a chat message regardless)
	//public boolean receiveGameTitles;
	public final PreferenceReceiveGameTitles receiveGameTitles;
	//sound played when hit a bow shot
	//public Sound bowShotHitSound;
	public final PreferenceBowHitSound bowHitSound;
	//whether the screen should tilt when taking damage
	//public boolean damageTilt;
	public final PreferenceDamageTilt damageTilt;
	
	//for kit related messages; play in chat, action bar, or both
	//public boolean kitActionBarMessages;
	public final PreferenceKitActionBar kitActionBar;
	//public boolean kitChatMessages;
	public final PreferenceKitChatMessages kitChatMessages;

	public PlayerInfo(byte permissionLevel) {
		team = null;
		spawnPoint = null;
		nametag = null;
		kit = null;
		defaultKit = "Trooper";
		//kothHillParticles = 1;
		kothParticles = new PreferenceKothParticles(new WrappedByte((byte) 1));
		//receiveGameTitles = true;
		receiveGameTitles = new PreferenceReceiveGameTitles(new WrappedBoolean(true));
		//damageTilt = true;
		damageTilt = new PreferenceDamageTilt(new WrappedBoolean(true));
		
		this.permissionLevel = permissionLevel;
		//bowShotHitSound = Sound.ENTITY_ARROW_HIT_PLAYER;
		bowHitSound = new PreferenceBowHitSound(Sound.ENTITY_ARROW_HIT_PLAYER);
		
		//kitActionBarMessages = true;
		kitActionBar = new PreferenceKitActionBar(new WrappedBoolean(true));
		//kitChatMessages = true;
		kitChatMessages = new PreferenceKitChatMessages(new WrappedBoolean(true));
	}
}

package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.core.Hologram;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import org.bukkit.Location;

//container class to store per-player info
public class PlayerInfo
{
	public static final byte lowestKothParticles = 10;

	public byte permissionLevel;
	public TeamArenaTeam team;
	public Location spawnPoint;
	public Hologram nametag;
	public Kit kit;
	//todo: read from DB or other persistent storage
	public String defaultKit;
	//from 1-10. number of ticks in between particle play
	public byte kothHillParticles;

	public PlayerInfo() {
		team = null;
		spawnPoint = null;
		nametag = null;
		kit = null;
		defaultKit = "Trooper";
		kothHillParticles = 1;

		permissionLevel = CustomCommand.ALL;
	}
}

package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.core.Hologram;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

public class PlayerInfo
{
	public TeamArenaTeam team;
	public Location spawnPoint;
	public Hologram nametag;
	public Kit kit;
	//todo: read from DB or other persistent storage
	public String defaultKit;
	//tick they last received damage
	public long lastHurt;
	//last amount of damage received from 1 damage source within the current (if any) invulnerability period
	public double lastDamage;

	public PlayerInfo() {
		team = null;
		spawnPoint = null;
		nametag = null;
		kit = null;
		defaultKit = "Trooper";
		lastHurt = -1;
		lastDamage = 0;
	}
}

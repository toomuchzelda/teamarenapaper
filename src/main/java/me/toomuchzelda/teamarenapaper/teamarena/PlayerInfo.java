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

	public PlayerInfo() {
		team = null;
		spawnPoint = null;
		nametag = null;
		kit = null;
	}
}

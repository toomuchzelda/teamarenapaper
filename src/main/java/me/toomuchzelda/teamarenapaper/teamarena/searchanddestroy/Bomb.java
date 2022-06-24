package me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class Bomb
{
	private final TeamArenaTeam owningTeam;
	private final Location spawnLoc;
	private RealHologram hologram;

	public Bomb(TeamArenaTeam team, Location spawnLoc) {
		this.owningTeam = team;
		this.spawnLoc = spawnLoc;
	}

	public void init() {
		spawnLoc.getBlock().setType(Material.TNT);
		Component bombName = owningTeam.getComponentName().append(Component.text("'s Bomb")).decoration(TextDecoration.BOLD, true);
		this.hologram = new RealHologram(spawnLoc.clone().add(0d, 1d, 0d), bombName);
	}

	public void kill() {
		this.hologram.remove(); //shouldn't be null if init'd
	}
}

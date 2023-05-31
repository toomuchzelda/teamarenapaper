package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class LifeOre
{
	final TeamArenaTeam owningTeam;
	final Location coordsAsLoc;

	private final double protectionRadius;
	final double protectionRadiusSqr;

	private final RealHologram hologram;

	LifeOre(TeamArenaTeam owningTeam, Material mat, BlockCoords coords, double protectionRadius, World world) {
		this.owningTeam = owningTeam;
		this.coordsAsLoc = coords.toLocation(world);

		this.protectionRadius = protectionRadius;
		this.protectionRadiusSqr = protectionRadius * protectionRadius;

		world.setBlockData(coords.x(), coords.y(), coords.z(), mat.createBlockData());

		this.hologram = new RealHologram(coords.toLocation(world).add(0.5d, 1d, 0.5d),
			RealHologram.Alignment.TOP, owningTeam.getComponentName().append(Component.text("'s Life Ore", owningTeam.getRGBTextColor())));
	}
}

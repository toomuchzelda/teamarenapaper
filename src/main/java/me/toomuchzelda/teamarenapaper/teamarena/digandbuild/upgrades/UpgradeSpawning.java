package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades;

import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuildInfo;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.ConfigOptional;
import me.toomuchzelda.teamarenapaper.utils.ConfigPath;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.List;

public record UpgradeSpawning(
	@ConfigOptional List<Vector> holograms,
	@ConfigPath("as") BlockData spawnAs,
	@ConfigOptional @ConfigPath("every") Integer spawnInterval,
	@ConfigPath("at") List<BlockCoords> spawnAt
) {
	public UpgradeSpawning withDefaultSpawnInterval(int defaultSpawnInterval) {
		if (spawnInterval == null)
			return new UpgradeSpawning(holograms, spawnAs, defaultSpawnInterval, spawnAt);
		return this;
	}
}

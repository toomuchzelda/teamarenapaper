package me.toomuchzelda.teamarenapaper.teamarena;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * This is a ChunkGenerator that does not generate anything. This is supplied when loading maps to avoid any costly
 * generation of new chunks.
 *
 * @author toomuchzelda
 */
public class VoidChunkGenerator extends ChunkGenerator
{
	public static final VoidChunkGenerator INSTANCE = new VoidChunkGenerator();

	private static final SnowyBiomeProvider biomeProvider = new SnowyBiomeProvider();

	public VoidChunkGenerator() {
		super();
	}

	@Override
	public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {}

	@Override
	public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {}

	@Override
	public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {}

	@Override
	public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {}

	@Override
	public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
		return biomeProvider;
	}

	@Override
	public int getBaseHeight(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull HeightMap heightMap) {
		return 1;
	}

	@Override
	public boolean canSpawn(@NotNull World world, int x, int z) {
		return false;
	}

	@Override
	public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
		return Collections.emptyList();
	}

	@Override
	public @Nullable Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
		return null;
	}

	@Override
	public boolean isParallelCapable() {
		return true;
	}

	@Override
	public boolean shouldGenerateNoise() {
		return false;
	}

	@Override
	public boolean shouldGenerateSurface() {
		return false;
	}

	@Override
	public boolean shouldGenerateBedrock() {
		return false;
	}

	@Override
	public boolean shouldGenerateCaves() {
		return false;
	}

	@Override
	public boolean shouldGenerateDecorations() {
		return false;
	}

	@Override
	public boolean shouldGenerateMobs() {
		return false;
	}

	@Override
	public boolean shouldGenerateStructures() {
		return false;
	}

	private static class SnowyBiomeProvider extends BiomeProvider {
		private static final List<Biome> list = List.of(Biome.SNOWY_TAIGA);

		@Override
		public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
			return Biome.SNOWY_TAIGA;
		}

		@Override
		public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
			return list;
		}
	}
}

package me.toomuchzelda.teamarenapaper.utils;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class BlockUtils
{
	//parse coords from map config.yml
	// String format: x,y,z
	// example: 34,0,40.5
	public static double[] parseCoords(String string, double xOffset, double yOffset, double zOffset) {
		String[] split = string.split(",");
		double[] coords = new double[3];
		coords[0] = Double.parseDouble(split[0]) + xOffset;
		coords[1] = Double.parseDouble(split[1]) + yOffset;
		coords[2] = Double.parseDouble(split[2]) + zOffset;
		return coords;
	}

	public static int getBlockColor(Block block) {
		//MaterialColor color = ((CraftBlock) block).getNMS().getMaterial().getColor();
		return ((CraftBlock) block).getNMS().getBlock().defaultMapColor().col;
	}

	public static Color getBlockBukkitColor(Block block) {
		int col = getBlockColor(block);
		return Color.fromRGB(col);
	}

	//get the highest point of a block from it's base, also considering fancy block shapes
	public static double getBlockHeight(Block block) {
		Collection<BoundingBox> list = block.getCollisionShape().getBoundingBoxes();
		double highest = 0;
		for (BoundingBox box : list)
		{
			if (box.getMaxY() > highest)
				highest = box.getMaxY();
		}
		return highest;
	}

	public static Vector parseCoordsToVec(String string, double xOffset, double yOffset, double zOffset) {
		String[] split = string.split(",");
		double x = Double.parseDouble(split[0]) + xOffset;
		double y = Double.parseDouble(split[1]) + yOffset;
		double z = Double.parseDouble(split[2]) + zOffset;
		return new Vector(x, y, z);
	}

	public static BlockCoords parseCoordsToBlockCoords(String string) {
		String[] split = string.split(",");
		int x = Integer.parseInt(split[0]);
		int y = Integer.parseInt(split[1]);
		int z = Integer.parseInt(split[2]);
		return new BlockCoords(x, y, z);
	}

	//find the first non-air block below any coordinate
	// returns null if none
	public static Location getFloor(Location pos) {
		int min = pos.getWorld().getMinHeight();
		for(int i = (int) pos.getY(); i >= min; i--) {
			if(pos.getWorld().getBlockAt(pos.getBlockX(), i, pos.getBlockZ()).isSolid()) {
				Location loc = pos.clone(); loc.setY(i);
				return loc;
			}
		}

		return null;
	}

	/* For hide and seek */
	public static ArrayList<BlockCoords> getAllBlocksSlow(Set<Material> mats, TeamArena game) {
		final ArrayList<BlockCoords> list = new ArrayList<>(512);

		final int minHeight = game.getWorld().getMinHeight();
		final int maxHeight = game.getWorld().getMaxHeight();
		game.forEachGameChunk((chunk, border) -> {
			for (int x = 0; x < 16; x++) {
				for (int y = minHeight; y < maxHeight; y++) {
					for (int z = 0; z < 16; z++) {
						Block block = chunk.getBlock(x, y, z);
						if (border.contains(block.getLocation().toVector())) {
							if (mats.contains(block.getType())) {
								list.add(new BlockCoords(block.getX(), block.getY(), block.getZ()));
							}
						}
					}
				}
			}
		});

		list.trimToSize();
		return list;
	}

	public static List<BlockCoords> getAllBlocks(Set<Material> mats, TeamArena game) {
		Set<BlockState> nmsBlockStateSet = mats.stream()
			.map(CraftBlockType::bukkitToMinecraft)
			.flatMap(block -> block.getStateDefinition().getPossibleStates().stream())
			.collect(ObjectOpenHashSet.toSet());
		Predicate<BlockState> predicate = nmsBlockStateSet::contains;
		ArrayList<BlockCoords> list = new ArrayList<>(512);

		// access the internal paletted container of each chunk section for much more efficient BlockState comparisons
		BoundingBox border = game.getBorder();
		for (Chunk chunk : game.gameWorld.getIntersectingChunks(border)) {
			ChunkAccess nmsChunk = ((CraftChunk) chunk).getHandle(ChunkStatus.FULL);
			int xOffset = nmsChunk.locX << 4, zOffset = nmsChunk.locZ << 4;

			LevelChunkSection[] sections = nmsChunk.getSections();
			for (int sectionIdx = Math.max(0, SectionPos.blockToSectionCoord(border.getMinY())),
				 sectionEnd = Math.min(sections.length, SectionPos.blockToSectionCoord(border.getMaxY()));
				 sectionIdx < sectionEnd; sectionIdx++) {
				LevelChunkSection section = sections[sectionIdx];
				if (section.hasOnlyAir())
					continue;
				PalettedContainer<BlockState> palettedContainer = section.getStates();
				if (!palettedContainer.maybeHas(predicate))
					continue;
				int yOffset = sectionIdx << 4;
				// block states are internally stored in yzx order
				for (int j = 0; j < 16; j++) {
					int y = yOffset + j;
					for (int k = 0; k < 16; k++) {
						int z = zOffset + k;
						for (int i = 0; i < 16; i++) {
							int x = xOffset + i;
							if (!border.contains(x, y, z))
								continue;
							BlockState blockState = palettedContainer.get(i, j, k);
							if (nmsBlockStateSet.contains(blockState)) {
								list.add(new BlockCoords(x, y, z));
							}
						}
					}
				}
			}
		}
		return List.copyOf(list);
	}

	public static boolean isAirToTheNakedEye(Material mat) {
		return mat.isAir() || mat == Material.LIGHT;
	}
}

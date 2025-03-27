package me.toomuchzelda.teamarenapaper.teamarena;

import org.bukkit.Material;

import java.util.Arrays;

/**
 * Class to manage which blocks are breakable by all players in all gamemodes.
 *
 * These blocks are mostly instant-breakable like tall grass, and other decor that might get in the way.
 */
public class BreakableBlocks
{
	private static final boolean[] BREAKABLE_BLOCKS;

	static {
		BREAKABLE_BLOCKS = new boolean[Material.values().length];
		Arrays.fill(BREAKABLE_BLOCKS, false);

		for(Material mat : Material.values()) {
			if(mat.isBlock() && !mat.isCollidable() && !mat.name().endsWith("SIGN") && !mat.name().endsWith("TORCH") &&
				!mat.name().endsWith("BANNER")) {
				setBlockBreakable(mat, true);
			}

			//don't break big dripleaf as may be part of map path
			setBlockBreakable(Material.BIG_DRIPLEAF_STEM, false);
			setBlockBreakable(Material.BIG_DRIPLEAF, false);
			setBlockBreakable(Material.SCAFFOLDING, false);
		}
	}

	private static void setBlockBreakable(Material mat, boolean breakable) {
		BREAKABLE_BLOCKS[mat.ordinal()] = breakable;
	}

	public static boolean isBlockBreakable(Material mat) {
		return BREAKABLE_BLOCKS[mat.ordinal()];
	}
}

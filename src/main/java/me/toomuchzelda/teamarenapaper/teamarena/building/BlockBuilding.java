package me.toomuchzelda.teamarenapaper.teamarena.building;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a block-based building.
 */
public abstract non-sealed class BlockBuilding extends Building {
	public BlockBuilding(Player player, Location loc) {
		super(player, loc);
	}

	private final List<Block> managedBlocks = new ArrayList<>();
	protected void registerBlocks(Block... blocks) {
		Block baseBlock = location.getBlock();
		for (Block block : blocks) {
			managedBlocks.add(block);
			BuildingListeners.registerBlock(baseBlock, block);
		}
	}

	public Block getBlock() {
		return location.getBlock();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		managedBlocks.forEach(BuildingListeners::unregisterBlock);
		managedBlocks.clear();
	}

	public boolean onBreak(BlockBreakEvent e) {
		return false; // not handled
	}

	public void onInteract(PlayerInteractEvent e) {

	}
}

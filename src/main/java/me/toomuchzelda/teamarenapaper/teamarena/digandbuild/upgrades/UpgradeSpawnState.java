package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public class UpgradeSpawnState {
	ItemStack itemGiven;
	int interval;
	BlockData spawnData;
	BlockData depletedData;
	Map<Block, Integer> blocksLastChange;

	public UpgradeSpawnState(World world, ItemStack itemGiven, UpgradeSpawning spawning) {
		this.itemGiven = itemGiven.clone();
		this.interval = spawning.spawnInterval();
		this.spawnData = spawning.spawnAs();
		this.depletedData = Material.COBBLESTONE.createBlockData();
		this.blocksLastChange = new LinkedHashMap<>();
		for (BlockCoords coords : spawning.spawnAt()) {
			blocksLastChange.put(coords.toBlock(world), -interval);
		}
	}

	public void tick() {
		int now = TeamArena.getGameTick();
		for (Map.Entry<Block, Integer> entry : blocksLastChange.entrySet()) {
			Block block = entry.getKey();
			int lastRegen = entry.getValue();
			if (now - lastRegen >= interval) {
				block.setBlockData(spawnData);
				entry.setValue(now);
			}
		}
	}

	public boolean isOreBlock(Block block) {
		return blocksLastChange.containsKey(block);
	}

	public boolean onBreak(Block block, Player breaker) {
		if (!blocksLastChange.containsKey(block))
			return false;
		BlockData blockData = block.getBlockData();
		if (blockData.matches(spawnData)) {
			breaker.getInventory().addItem(itemGiven);
			block.setBlockData(depletedData);
			blocksLastChange.put(block, TeamArena.getGameTick());
		}
		return true;
	}
}

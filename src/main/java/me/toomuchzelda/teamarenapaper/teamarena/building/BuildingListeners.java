package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;

public class BuildingListeners {
	private BuildingListeners() {

	}

	private static final Map<Block, Block> extraBlockLinks = new HashMap<>();
	private static final Map<Entity, Block> extraEntityLinks = new HashMap<>();

	public static void unregisterBlock(Block block) {
		extraBlockLinks.remove(block);
	}

	public static void registerBlock(Block baseBlock, Block block) {
		extraBlockLinks.put(block, baseBlock);
	}

	public static void unregisterEntity(Entity entity) {
		extraEntityLinks.remove(entity);
	}

	public static void registerEntity(Block baseBlock, Entity entity) {
		extraEntityLinks.put(entity, baseBlock);
	}
	public static boolean onBlockBroken(BlockBreakEvent e) {
		if (e.isCancelled())
			return false;

		Block block = e.getBlock();
		Block actualBlock = extraBlockLinks.getOrDefault(block, block);
		Building building = BuildingManager.getBuildingAt(actualBlock);
		if (!(building instanceof BlockBuilding blockBuilding))
			return false;
		return blockBuilding.onBreak(e);
	}

	public static boolean onEntityAttack(DamageEvent e) {
		if (e.isCancelled())
			return false;

		Entity victim = e.getVictim();
		Block block = extraEntityLinks.get(victim);
		if (block == null) {
			block = victim.getLocation().getBlock();
		}
		Building building = BuildingManager.getBuildingAt(block);
		if (!(building instanceof EntityBuilding entityBuilding))
			return false;
		return entityBuilding.onDamage(e);
	}
}

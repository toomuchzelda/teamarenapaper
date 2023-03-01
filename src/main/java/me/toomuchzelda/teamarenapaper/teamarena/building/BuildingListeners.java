package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

public class BuildingListeners {
	private BuildingListeners() {

	}

	private static final Map<Block, Block> extraBlockLinks = new HashMap<>();
//	private static final Map<Entity, Block> extraEntityLinks = new HashMap<>();

	public static void unregisterBlock(Block block) {
		extraBlockLinks.remove(block);
	}

	public static void registerBlock(Block baseBlock, Block block) {
		extraBlockLinks.put(block, baseBlock);
	}

	public static void unregisterEntity(Entity entity) {
//		extraEntityLinks.remove(entity);
	}

	public static void registerEntity(Block baseBlock, Entity entity) {
//		extraEntityLinks.put(entity, baseBlock);
	}

	public static void onPlayerQuit(PlayerQuitEvent event) {
		BuildingManager.getAllPlayerBuildings(event.getPlayer()).forEach(BuildingManager::destroyBuilding);
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

	public static boolean onInteract(PlayerInteractEvent e) {
		if (e.useInteractedBlock() == Event.Result.DENY)
			return false;

		Block block = e.getClickedBlock();
		Block actualBlock = extraBlockLinks.getOrDefault(block, block);
		Building building = BuildingManager.getBuildingAt(actualBlock);
		if (!(building instanceof BlockBuilding blockBuilding))
			return false;
		blockBuilding.onInteract(e);
		return true;
	}

	public static boolean onEntityAttack(DamageEvent e) {
		if (e.isCancelled())
			return false;

		Entity victim = e.getVictim();
		EntityBuilding building = BuildingManager.getBuilding(victim);
		if (building == null)
			return false;
		return building.onDamage(e);
	}

	public static boolean onEntityInteract(PlayerInteractEntityEvent e) {
		if (e.isCancelled())
			return false;

		Entity victim = e.getRightClicked();
		EntityBuilding building = BuildingManager.getBuilding(victim);
		if (building == null)
			return false;
		building.onInteract(e);
		return true;
	}

}

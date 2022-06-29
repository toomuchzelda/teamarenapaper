package me.toomuchzelda.teamarenapaper.teamarena;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Class to exempt certain explosions from not doing anything in EventListeners handlers.
 *
 * TODO: Block explosion handling when I need it.
 */
public class ExplosionManager {

	public static final byte DEFAULT_FIRE = -1;
	public static final byte YES_FIRE = 1;
	public static final byte NO_FIRE = 0;

	public static final float DEFAULT_FLOAT_VALUE = -1;

	/**
	 * boolean cancel: cancel the event. No other parameters will be considered if true
	 *
	 * breakBlocks boolean decides if blocks in the explosion should be destroyed. Blocks in the exemptions Set are
	 * treated the opposite of what the breakBlocks boolean says, and can be null for no exemptions.
	 *
	 * fire, radius, and yield can be set to the global constants in this class for default values (values that are
	 * given in the Event Handler)
	 */
	public record EntityExplosionInfo(boolean cancel, byte fire, float radius, float yield, boolean breakBlocks,
									  @Nullable HashSet<Block> exemptions) {}

	private static final Map<Entity, EntityExplosionInfo> ENTITY_EXPLOSION_MAP = new WeakHashMap<>();

	public static void setEntityInfo(Entity entity, EntityExplosionInfo info) {
		ENTITY_EXPLOSION_MAP.put(entity, info);
	}

	public static EntityExplosionInfo getEntityInfo(Entity entity) {
		return ENTITY_EXPLOSION_MAP.get(entity);
	}

	public static EntityExplosionInfo getAndRemoveEntityInfo(Entity entity) {
		return ENTITY_EXPLOSION_MAP.remove(entity);
	}
}

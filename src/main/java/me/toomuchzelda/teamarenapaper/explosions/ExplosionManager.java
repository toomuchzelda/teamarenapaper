package me.toomuchzelda.teamarenapaper.explosions;

import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Class to exempt certain explosions from not doing anything in EventListeners handlers.
 * If an EntityExplosionInfo exists for an Entity then a CustomExplosion will not be automatically created for it.
 * Vanilla explosion will be used instead.
 *
 * TODO: Block explosion handling when I need it.
 */
public class ExplosionManager {

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

package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftArrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.*;

//maybe not needed, uhhhhhh
public class ArrowPierceManager {

    private static final Map<AbstractArrow, ArrowInfo> PIERCED_ENTITIES_MAP = new WeakHashMap<>();

    public static void addOrUpdateInfo(AbstractArrow arrow) {
        ArrowInfo info = PIERCED_ENTITIES_MAP.get(arrow);
        if(info == null) {
            info = new ArrowInfo();
            info.piercedEntities = new ArrayList<>(Math.min(arrow.getPierceLevel(), 20));
            info.velocity = arrow.getVelocity();
            info.pitch = arrow.getLocation().getPitch();
            info.yaw = arrow.getLocation().getYaw();

            info.lastUpdated = TeamArena.getGameTick();

            //Bukkit.broadcastMessage("added new arrowInfo to map");
            PIERCED_ENTITIES_MAP.put(arrow, info);
        }
        //slightly unelegant, but sometimes an arrow may collide with two entities in one tick
        // this fires two EntityDamageByEntityEvents, which means the first one will go through and update the
        // velocity and pitch/yaw just fine, but then it will move on and the event will be cancelled and the
        // arrow will get bounced back. which is normally fine, but there's two EntityDamageByEntityEvents and the second
        // one will update these fields with the arrow's bounced back movement + direction which is undesirable.
        // so just check to make sure these aren't updated twice in the same tick
        else if(info.lastUpdated != TeamArena.getGameTick()) {
            info.velocity = arrow.getVelocity();
            info.pitch = arrow.getLocation().getPitch();
            info.yaw = arrow.getLocation().getYaw();
            info.lastUpdated = TeamArena.getGameTick();
        }
    }

    public static void fixArrowMovement(AbstractArrow arrow) {
        ArrowInfo info = PIERCED_ENTITIES_MAP.get(arrow);

        net.minecraft.world.entity.projectile.AbstractArrow nmsArrow = ((CraftArrow) arrow).getHandle();

        nmsArrow.setXRot(info.pitch);
        nmsArrow.setYRot(info.yaw);
        arrow.setVelocity(info.velocity);
    }


    public static PierceType canPierce(AbstractArrow arrow, Entity piercedEntity) {
        ArrowInfo info = PIERCED_ENTITIES_MAP.get(arrow);

        List<Entity> hitList = info.piercedEntities;

        //Bukkit.broadcastMessage(hitList.toString());

        if(hitList.contains(piercedEntity))
            return PierceType.ALREADY_HIT;

        hitList.add(piercedEntity);

        //Bukkit.broadcastMessage("hitList: " + hitList.size() + ", arrowPierce: " + arrow.getPierceLevel());

        if(hitList.size() > arrow.getPierceLevel()) {
            //piercedEntitiesMap.remove(arrow);
            //garbage collection done in EventListeners.entityRemoveFromWorld or EventListeners.endTick by cleanup
            return PierceType.REMOVE_ARROW;
        }
        else
            return PierceType.PIERCE;
    }

	public static void removeInfo(AbstractArrow arrow) {
		PIERCED_ENTITIES_MAP.remove(arrow);
	}

    public enum PierceType
    {
        ALREADY_HIT,
        PIERCE,
        REMOVE_ARROW
    }

    private static class ArrowInfo
    {
        public ArrayList<Entity> piercedEntities;
        public Vector velocity;
        public float yaw;
        public float pitch;
        public int lastUpdated;

        public ArrowInfo() {
        }
    }
}

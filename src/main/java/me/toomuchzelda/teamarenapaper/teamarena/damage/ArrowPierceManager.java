package me.toomuchzelda.teamarenapaper.teamarena.damage;

import org.bukkit.craftbukkit.v1_17_R1.entity.CraftArrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

//maybe not needed, uhhhhhh
public class ArrowPierceManager {

    public static final HashMap<AbstractArrow, ArrowInfo> piercedEntitiesMap = new HashMap<>();


    public static void addInfo(AbstractArrow arrow) {
        if(!piercedEntitiesMap.containsKey(arrow)) {
            ArrowInfo info = new ArrowInfo();
            info.piercedEntities = new LinkedList<>();
            info.velocity = arrow.getVelocity();
            info.pitch = arrow.getLocation().getPitch();
            info.yaw = arrow.getLocation().getYaw();

            piercedEntitiesMap.put(arrow, info);
        }
    }

    public static void fixArrowMovement(AbstractArrow arrow) {
        //shouldn't be needed after this
        ArrowInfo info = piercedEntitiesMap.remove(arrow);

        net.minecraft.world.entity.projectile.AbstractArrow nmsArrow = ((CraftArrow) arrow).getHandle();

        nmsArrow.setXRot(info.pitch);
        nmsArrow.setYRot(info.yaw);
        arrow.setVelocity(info.velocity);
    }


    public static PierceType canPierce(AbstractArrow arrow, Entity piercedEntity) {
        ArrowInfo info = piercedEntitiesMap.get(arrow);

        LinkedList<Entity> hitList = info.piercedEntities;

        if(hitList.contains(piercedEntity))
            return PierceType.ALREADY_HIT;

        hitList.add(piercedEntity);

        if(hitList.size() > arrow.getPierceLevel()) {
            //piercedEntitiesMap.remove(arrow);
            //garbage collection done in EventListeners.entityRemoveFromWorld or EventListeners.endTick by cleanup
            return PierceType.REMOVE_ARROW;
        }
        else
            return PierceType.PIERCE;
    }

    public enum PierceType
    {
        ALREADY_HIT,
        PIERCE,
        REMOVE_ARROW;
    }

    private static class ArrowInfo
    {
        public LinkedList<Entity> piercedEntities;
        public Vector velocity;
        public float yaw;
        public float pitch;

        public ArrowInfo() {
        }
    }

    //just in case
    public static void cleanup() {
        Iterator<Map.Entry<AbstractArrow, ArrowInfo>> iter = piercedEntitiesMap.entrySet().iterator();

        while(iter.hasNext()) {
            Map.Entry<AbstractArrow, ArrowInfo> entry = iter.next();
            if(!entry.getKey().isValid()) {
                iter.remove();
            }
        }
    }
}

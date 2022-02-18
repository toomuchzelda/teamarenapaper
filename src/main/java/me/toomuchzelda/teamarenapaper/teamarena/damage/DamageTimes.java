package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//class for recording when damage of different types has been taken by LivingEntity
public class DamageTimes {

    public static final HashMap<LivingEntity, DamageTimes> entityDamageTimes = new HashMap<>();

    //tick they last received damage from direct "Fighting" sources i.e melee / projectile
    public int lastAttackTime;
    //reference to the last direct attack event
    public DamageEvent lastAttackEvent;

    public FireTimes fireTimes;
    //for all damagetypes not explicitly covered
    public int lastMiscDamageTime;

    public Entity lastDamager;

    public DamageTimes(LivingEntity living) {

        fireTimes = new FireTimes();
        fireTimes.lastFireTime = 0;
        fireTimes.fireGiver = null;
        fireTimes.fireType = null;

        lastAttackTime = 0;
        lastDamager = null;


        lastMiscDamageTime = 0;

        entityDamageTimes.put(living, this);
    }
    
    public void clearAttackers() {
        lastDamager = null;
        lastAttackEvent = null;
        fireTimes.fireGiver = null;
        fireTimes.fireType = null;
    }

    public static DamageTimes getDamageTimes(LivingEntity living) {
        DamageTimes times = entityDamageTimes.get(living);
        if(times == null)
            times = new DamageTimes(living);

        return times;
    }

    public static class FireTimes
    {
        public int lastFireTime;
        public DamageType fireType;
        //if fire was caused by an entity, the entity that caused it
        // reset when the fire stops in EventListeners.endTick()
        public Entity fireGiver;

        public FireTimes() {

        }
    }

    public static void cleanup() {
        Iterator<Map.Entry<LivingEntity, DamageTimes>> iter = entityDamageTimes.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<LivingEntity, DamageTimes> entry = iter.next();

            LivingEntity living = entry.getKey();

            if(living instanceof Player p && !p.isOnline()) {
                iter.remove();
            }
            else if(!living.isValid()) {
                iter.remove();
            }
            Main.logger().info(living.getName() + " has been DamageTime cleaned up");
        }
    }
}

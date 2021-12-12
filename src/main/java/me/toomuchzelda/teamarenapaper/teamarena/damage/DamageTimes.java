package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//class for recording when damage of different types has been taken by LivingEntity
public class DamageTimes {

    public static final HashMap<LivingEntity, DamageTimes> entityDamageTimes = new HashMap<>();

    //tick they last received damage from direct "Fighting" sources i.e melee / projectile
    public long lastAttackTime;

    public long lastFireTime;
    //public long lastPoisonTime;
    //for all damagetypes not covered
    public long lastMiscDamageTime;

    public Entity lastDamager;

    public DamageTimes(LivingEntity living) {
        //long time = TeamArena.getGameTick();
        lastAttackTime = 0;
        lastFireTime = 0;
        //lastPoisonTime = time;
        lastMiscDamageTime = 0;
        lastDamager = null;

        entityDamageTimes.put(living, this);
    }

    public static DamageTimes getDamageTimes(LivingEntity living) {
        DamageTimes times = entityDamageTimes.get(living);
        if(times == null)
            times = new DamageTimes(living);

        return times;
    }

    public static void cleanup() {
        Iterator<Map.Entry<LivingEntity, DamageTimes>> iter = entityDamageTimes.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<LivingEntity, DamageTimes> entry = iter.next();

            LivingEntity living = entry.getKey();

            if(!living.isValid()) {
                Main.logger().info(living.getName() + " has been DamageTime cleaned up");
                iter.remove();
            }
        }
    }
}

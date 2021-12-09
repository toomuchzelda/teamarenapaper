package me.toomuchzelda.teamarenapaper.teamarena.damage;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

//a custom damage event; custom calculations for damage, knockback etc. good to enable things spigot/paper event can't
public class DamageEvent {

    private Entity hurt;
    //damage before damage-reduction calculations
    private double rawDamage;
    //damage after damage-reduction calculations
    private double finalDamage;
    private EntityDamageEvent.DamageCause damageCause;

    //null implies no knockback
    // dont use 0,0,0 vector as that'll stop the player moving for a split moment
    private Vector knockback;

    private Entity attacker;
    //shooter of arrow, snowball etc where attacker would be the projectile
    private Entity realAttacker;
    private boolean isCritical;

    public DamageEvent(EntityDamageEvent event) {
        hurt = event.getEntity();
        rawDamage = event.getDamage();
        finalDamage = event.getFinalDamage();
        damageCause = event.getCause();

        if(event instanceof EntityDamageByEntityEvent dEvent) {
            if(dEvent.getDamager() instanceof Projectile projectile &&
                    projectile.getShooter() instanceof LivingEntity living) {
                realAttacker = living;
                attacker = projectile;
            }
            else {
                attacker = dEvent.getDamager();
                realAttacker = null;
            }
            isCritical = dEvent.isCritical();
        }
    }

    public boolean isKnockback(EntityDamageEvent.DamageCause cause) {
        boolean knockback = false;
        //brug
        switch(cause) {
            case CONTACT:
            case CRAMMING:
            case CUSTOM:
            case DRAGON_BREATH:
            case DROWNING:
            case DRYOUT:
            case FALL:
            case FALLING_BLOCK:
            case FIRE:
            case FIRE_TICK:
            case FLY_INTO_WALL:
        }
    }
}

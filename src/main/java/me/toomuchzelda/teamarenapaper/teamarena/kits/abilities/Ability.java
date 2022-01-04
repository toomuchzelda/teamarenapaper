package me.toomuchzelda.teamarenapaper.teamarena.kits.abilities;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import org.bukkit.entity.Player;

//methods aren't abstract as kit abilities may not need to override them
public abstract class Ability {

    protected final TeamArena teamArena;
    
    protected Ability() {
        teamArena = Main.getGame();
    }
    
    //register all one-time-registered things for this ability
    public void registerAbility() {
    }

    public void unregisterAbility() {
    }

    //'give' this ability to one player
    // whatever that means for a specific ability
    public void giveAbility(Player player) {
    }

    public void removeAbility(Player player) {
    }
    
    /**
     * when an attack is *attempted* on the ability user
     * may or may not be cancelled at this point
     */
    public void onAttemptedDamage(DamageEvent event) {
    
    }
    
    /**
     * when the ability user actually takes damage
     */
    public void onReceiveDamage(DamageEvent event) {}
    
    /**
     * when the user *attempts* to attack another entity
     */
    public void onAttemptedAttack(DamageEvent event) {}
    
    /**
     * when the user successfully deals damage to another entity
     */
    public void onDealtAttack(DamageEvent event) {
    
    }
}
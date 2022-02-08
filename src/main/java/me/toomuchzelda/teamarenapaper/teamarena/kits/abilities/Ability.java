package me.toomuchzelda.teamarenapaper.teamarena.kits.abilities;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

//methods aren't abstract as kit abilities may not need to override them
public abstract class Ability {

    protected Ability() {
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
    
    /**
     * when the user launches a projectile (not a bow)
     * @param event
     */
    public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
    }
    
    /**
     * when user shoots a bow
     * @param event
     */
    public void onShootBow(EntityShootBowEvent event) {
    }
    
    /**
     * when user loads a crossbow
     */
    public void onLoadCrossbow(EntityLoadCrossbowEvent event) {
    }
    
    /**
     * run every tick
     */
    public void onTick(Player player) {
    }

    /**
     * when the player receieves a cooldown on any of their items (e.g enderpearl after throwing)
     */
    public void onItemCooldown(PlayerItemCooldownEvent event) {

    }
    
    public void onInteract(PlayerInteractEvent event) {
    }

    public void onInteractEntity(PlayerInteractEntityEvent event) {
    }
}
package me.toomuchzelda.teamarenapaper.teamarena.kits.abilities;

import org.bukkit.entity.Player;

//methods aren't abstract as kit abilities may not need to override them
public abstract class Ability {

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
}
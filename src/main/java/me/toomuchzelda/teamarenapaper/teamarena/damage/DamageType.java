package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.core.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

//partially from RedWarfare's AttackType class - credit libraryaddict
// https://github.com/libraryaddict/RedWarfare/blob/master/redwarfare-core/src/me/libraryaddict/core/damage/AttackType.java
public class DamageType {

    public static DamageType CACTUS = new DamageType("Cactus", "%Killed% licked a cactus").setNoKnockback();

    public static DamageType CUSTOM = new DamageType("Custom", "%Killed% fell over and died").setNoKnockback();

    public static DamageType DAMAGE_POTION = new DamageType("Damage Potion",
            "%Killed% died to %Killer%'s damage potion which isn't part of the game?").setNoKnockback();

    public static DamageType DRAGON_BREATH = new DamageType("Dragon Breath", "%Killed% sucked a lungfull of dragon's breath")
            .setNoKnockback();

    public static DamageType DROWNED = new DamageType("Drowned", "%Killed% is swimming with the fishes").setIgnoreArmor()
            .setNoKnockback();

    public static DamageType EXPLOSION = new DamageType("Explosion", "%Killed% was caught in an explosion");

    public static DamageType FALL = new DamageType("Fall", "%Killed% fell to their death").setFall().setNoKnockback();

    public static DamageType FALL_PUSHED = new DamageType("Pushed Fall", "%Killed% was pushed to their death by %Killer%")
            .setFall().setNoKnockback();

    public static DamageType FALL_SHOT = new DamageType("Shot Fall", "%Killed% was shot by %Killer% and fell to their death")
            .setFall().setNoKnockback();

    public static DamageType FALLING_BLOCK = new DamageType("Falling Block", "%Killed% was crushed beneath a falling block");

    /**
     * Direct exposure to fire
     */
    public static DamageType FIRE = new DamageType("Direct Fire", "%Killed% stood inside flames and laughed").setNoKnockback()
            .setBurn();

    public static DamageType FIRE_ASPECT = new DamageType("Fire Aspect", "%Killed% was charred to a crisp by %Killer%").setFire()
            .setNoKnockback().setIgnoreArmor();

    public static DamageType FIRE_BOW = new DamageType("Fire Bow", "%Killed% charred to a crisp by %Killer%'s bow").setFire()
            .setNoKnockback().setIgnoreArmor();

    /**
     * Fire on the entity itself
     */
    public static DamageType FIRE_TICK = new DamageType("Fire Tick", "%Killed% was burned alive").setNoKnockback().setFire()
            .setIgnoreArmor();

    public static DamageType FISHING_HOOK = new DamageType("Fishing Hook", "%Killed% was killed by %Killer%'s... Fishing rod?");

    public static DamageType FLY_INTO_WALL = new DamageType("Flew into Wall", "%Killed% still hadn't gotten the hang of flying")
            .setNoKnockback();

    public static DamageType LAVA = new DamageType("Lava", "%Killed% tried to swim in lava").setNoKnockback().setBurn();

    public static DamageType LIGHTNING = new DamageType("Lightning", "%Killed% was electrified by lightning").setNoKnockback();

    public static DamageType MAGMA = new DamageType("Magma", "%Killed% took a rest on some hot magma").setNoKnockback().setBurn();

    public static DamageType MELEE = new DamageType("Melee", "%Killed% was murdered by %Killer%").setMelee();

    public static DamageType MELTING = new DamageType("Melting", "%Killed% melted in the hot sun").setNoKnockback();

    public static DamageType POISON = new DamageType("Poison", "%Killed% drank a flask of poison").setIgnoreArmor()
            .setNoKnockback();

    public static DamageType PROJECTILE = new DamageType("Projectile", "%Killed% was shot by %Killer%").setProjectile();

    public static DamageType QUIT = new DamageType("Quit", "%Killed% has left the game").setInstantDeath().setNoKnockback();

    public static DamageType STARVATION = new DamageType("Starvation", "%Killed% died from starvation").setIgnoreArmor()
            .setNoKnockback();

    public static DamageType SUFFOCATION = new DamageType("Suffocation", "%Killed% choked on block").setNoKnockback();

    public static DamageType SUICIDE = new DamageType("Suicide", "%Killed% commited suicide").setInstantDeath().setNoKnockback();

    public static DamageType SUICIDE_ASSISTED = new DamageType("Assisted Suicide", "%Killed% was assisted on the path to suicide")
            .setInstantDeath().setNoKnockback();

    public static DamageType THORNS = new DamageType("Thorns", "%Killed% found out how the thorns enchantment works")
            .setNoKnockback();

    public static DamageType UNKNOWN = new DamageType("Unknown", "%Killed% died from unknown causes").setNoKnockback();

    public static DamageType VOID = new DamageType("Void", "%Killed% fell into the void").setIgnoreArmor().setNoKnockback()
            .setIgnoreRate();

    public static DamageType VOID_PUSHED = new DamageType("Void Pushed", "%Killed% was knocked into the void by %Killer%")
            .setIgnoreArmor().setNoKnockback().setIgnoreRate();

    public static DamageType VOID_SHOT = new DamageType("Void Shot", "%Killed% was shot into the void by %Killer%")
            .setIgnoreArmor().setNoKnockback().setIgnoreRate();

    public static DamageType WITHER_POISON = new DamageType("Wither Poison", "%Killed% drank a vial of wither poison")
            .setNoKnockback();
    private boolean _burn;
    private final String[] _deathMessages;
    private boolean _explosion;
    private boolean _fall;
    private boolean _fire;
    private boolean _ignoreArmor;
    private boolean _ignoreRate;
    private boolean _instantDeath;
    private boolean _isntKnockback;
    private boolean _melee;
    private final String _name;
    private boolean _projectile;

    public DamageType(String name, String... deathMessages) {
        _name = name;
        _deathMessages = deathMessages;
    }

    public static DamageType getAttack(EntityDamageEvent.DamageCause cause) {
        switch (cause) {
            case CONTACT:
                return CACTUS;
            case ENTITY_ATTACK:
                return MELEE;
            case PROJECTILE:
                return PROJECTILE;
            case SUFFOCATION:
                return SUFFOCATION;
            case FALL:
                return FALL;
            case FIRE:
                return FIRE;
            case FIRE_TICK:
                return FIRE_TICK;
            case MELTING:
                return MELTING;
            case LAVA:
                return LAVA;
            case DROWNING:
                return DROWNED;
            case BLOCK_EXPLOSION:
                return EXPLOSION;
            case ENTITY_EXPLOSION:
                return EXPLOSION;
            case VOID:
                return VOID;
            case LIGHTNING:
                return LIGHTNING;
            case SUICIDE:
                return SUICIDE;
            case STARVATION:
                return STARVATION;
            case POISON:
                return POISON;
            case MAGIC:
                return DAMAGE_POTION;
            case WITHER:
                return WITHER_POISON;
            case FALLING_BLOCK:
                return FALLING_BLOCK;
            case THORNS:
                return THORNS;
            case DRAGON_BREATH:
                return DRAGON_BREATH;
            case CUSTOM:
                return CUSTOM;
            case FLY_INTO_WALL:
                return FLY_INTO_WALL;
            case HOT_FLOOR:
                return MAGMA;
            default:
                return UNKNOWN;
        }
    }

    public String getDeathMessage() {
        return _deathMessages[MathUtils.randomMax(_deathMessages.length - 1)];
    }

    /*public String getDeathMessage(String color, Entity victim, Entity killer, Entity cause) {
        String victimName;
        if(victim == null)
            victimName = "Unknown";

        return getDeathMessage(color, UtilEnt.getName(victim), UtilEnt.getName(killer), UtilEnt.getName(cause));
    }*/

    public String getDeathMessage(TextColor color, Component victim, Component killer, Component cause) {
        //String message = color + getDeathMessage();
        Component message = Component.text(getDeathMessage());

        
        message = replace(message, "%Killed%", victim);
        message = replace(message, "%Killed%", victim);
        message = replace(message, "%Cause%", cause);
        message = replace(message, "%Cause%", cause);
        message = replace(message, "%Killer%", killer);
        message = replace(message, "%Killer%", killer);

        return message;
    }

    public String getName() {
        return _name;
    }

    public boolean isBurn() {
        return _burn;
    }

    public boolean isExplosion() {
        return _explosion;
    }

    public boolean isFall() {
        return _fall;
    }

    public boolean isFire() {
        return _fire;
    }

    public boolean isIgnoreArmor() {
        return _ignoreArmor;
    }

    public boolean isIgnoreRate() {
        return _ignoreRate;
    }

    public boolean isInstantDeath() {
        return _instantDeath;
    }

    public boolean isKnockback() {
        return !_isntKnockback;
    }

    public boolean isMelee() {
        return _melee;
    }

    public boolean isProjectile() {
        return _projectile;
    }

    private String replace(String message, String find, String replace) {
        while (message.contains(find)) {
            message = message.replaceFirst(find, replace + ChatColor.getLastColors(message.substring(0, message.indexOf(find))));
        }

        return message;
    }

    public DamageType setBurn() {
        _burn = true;

        return this;
    }

    public DamageType setExplosion() {
        _explosion = true;

        return this;
    }

    public DamageType setFall() {
        _fall = true;
        setIgnoreArmor();
        setIgnoreRate();

        return this;
    }

    public DamageType setFire() {
        setBurn();

        _fire = true;

        return this;
    }

    public DamageType setIgnoreArmor() {
        _ignoreArmor = true;

        return this;
    }

    public DamageType setIgnoreRate() {
        _ignoreRate = true;

        return this;
    }

    public DamageType setInstantDeath() {
        _instantDeath = true;

        return this;
    }

    public DamageType setMelee() {
        _melee = true;

        return this;
    }

    public DamageType setNoKnockback() {
        _isntKnockback = true;

        return this;
    }

    public DamageType setProjectile() {
        _projectile = true;

        return this;
    }

    public String toString() {
        return "DamageType[" + getName() + "]";
    }
}
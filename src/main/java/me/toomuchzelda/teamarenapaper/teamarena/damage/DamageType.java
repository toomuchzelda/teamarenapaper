package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.regex.Pattern;

//partially from RedWarfare's AttackType class - credit libraryaddict
// https://github.com/libraryaddict/RedWarfare/blob/master/redwarfare-core/src/me/libraryaddict/core/damage/AttackType.java
public class DamageType {
    
    /******************************************************************************************
     *                                  GENERAL DAMAGETYPES
     ******************************************************************************************/
    
    public static final DamageType BERRY_BUSH = new DamageType(DamageTypeEnum.BERRY_BUSH, "Berry Bush", "%Killed% ate one too many sweet berries").setNoKnockback()
            .setDamageSource(DamageSource.SWEET_BERRY_BUSH);

    public static final DamageType CACTUS = new DamageType(DamageTypeEnum.CACTUS, "Cactus", "%Killed% hugged a cactus").setNoKnockback()
            .setDamageSource(DamageSource.CACTUS);

    public static final DamageType CRAMMING = new DamageType(DamageTypeEnum.CRAMMING, "Cramming", "%Killer% was crammed to death").setIgnoreArmor()
            .setNoKnockback().setDamageSource(DamageSource.CRAMMING);

    public static final DamageType CUSTOM = new DamageType(DamageTypeEnum.CUSTOM, "Custom", "%Killed% fell over and died").setNoKnockback()
            .setDamageSource(DamageSource.GENERIC);

    public static final DamageType DAMAGE_POTION = new DamageType(DamageTypeEnum.DAMAGE_POTION, "Damage Potion",
            "%Killed% died to %Killer%'s damage potion which isn't part of the game?").setNoKnockback()
            .setDamageSource(DamageSource.MAGIC);

    public static final DamageType DRAGON_BREATH = new DamageType(DamageTypeEnum.DRAGON_BREATH, "Dragon Breath", "%Killed% sucked a lungful of dragon's breath")
            .setNoKnockback().setDamageSource(DamageSource.DRAGON_BREATH);

    public static final DamageType DROWNED = new DamageType(DamageTypeEnum.DROWNED, "Drowned", "%Killed% is swimming with the fishes").setIgnoreArmor()
            .setNoKnockback().setDamageSource(DamageSource.DROWN);

    public static final DamageType DRYOUT = new DamageType(DamageTypeEnum.DRYOUT, "Dryout", "%Killed% died of dehydration")
            .setNoKnockback().setDamageSource(DamageSource.DRY_OUT);

    public static final DamageType EXPLOSION = new DamageType(DamageTypeEnum.EXPLOSION, "Explosion", "%Killed% was caught in an explosion")
            .setDamageSource(DamageSource.explosion((net.minecraft.world.entity.LivingEntity) null));

    public static final DamageType FALL = new DamageType(DamageTypeEnum.FALL, "Fall", "%Killed% fell to their death").setFall().setNoKnockback()
            .setDamageSource(DamageSource.FALL);

    public static final DamageType FALL_PUSHED = new DamageType(DamageTypeEnum.FALL_PUSHED, "Pushed Fall", "%Killed% was pushed to their death by %Killer%")
            .setFall().setNoKnockback().setDamageSource(DamageSource.FALL);

    public static final DamageType FALL_SHOT = new DamageType(DamageTypeEnum.FALL_SHOT, "Shot Fall", "%Killed% was shot by %Killer% and fell to their death")
            .setFall().setNoKnockback().setDamageSource(DamageSource.FALL);

    public static final DamageType FALLING_BLOCK = new DamageType(DamageTypeEnum.FALLING_BLOCK, "Falling Block", "%Killed% was crushed beneath a falling block")
            .setNoKnockback().setDamageSource(DamageSource.FALLING_BLOCK);

    public static final DamageType FALLING_STALACTITE = new DamageType(DamageTypeEnum.FALLING_STALACTITE, "Falling Stalactite", "%Killed% was impaled by a " +
            "falling stalactite").setNoKnockback().setDamageSource(DamageSource.FALLING_STALACTITE);

    /**
     * Direct exposure to fire
     */
    public static final DamageType FIRE = new DamageType(DamageTypeEnum.FIRE, "Direct Fire", "%Killed% stood inside flames and laughed").setNoKnockback()
            .setBurn().setDamageSource(DamageSource.IN_FIRE);

    public static final DamageType FIRE_ASPECT = new DamageType(DamageTypeEnum.FIRE_ASPECT, "Fire Aspect", "%Killed% was charred to a crisp by %Killer%").setFire()
            .setNoKnockback().setIgnoreArmor().setDamageSource(DamageSource.ON_FIRE);

    public static final DamageType FIRE_BOW = new DamageType(DamageTypeEnum.FIRE_BOW, "Fire Bow", "%Killed% charred to a crisp by %Killer%'s bow").setFire()
            .setNoKnockback().setIgnoreArmor().setDamageSource(DamageSource.ON_FIRE);

    /**
     * Fire on the entity itself
     */
    public static final DamageType FIRE_TICK = new DamageType(DamageTypeEnum.FIRE_TICK, "Fire Tick", "%Killed% was burned alive").setNoKnockback().setFire()
            .setIgnoreArmor().setDamageSource(DamageSource.ON_FIRE);

    public static final DamageType FISHING_HOOK = new DamageType(DamageTypeEnum.FISHING_HOOK, "Fishing Hook", "%Killed% was killed by %Killer%'s... Fishing rod?");

    public static final DamageType FLY_INTO_WALL = new DamageType(DamageTypeEnum.FLY_INTO_WALL, "Flew into Wall", "%Killed% still hadn't gotten the hang of flying")
            .setNoKnockback().setDamageSource(DamageSource.FLY_INTO_WALL);

    public static final DamageType FREEZE = new DamageType(DamageTypeEnum.FREEZE, "Freeze", "%Killed% froze to death")
            .setIgnoreArmor().setDamageSource(DamageSource.FREEZE);

    public static final DamageType LAVA = new DamageType(DamageTypeEnum.LAVA, "Lava", "%Killed% tried to swim in lava").setNoKnockback().setBurn()
            .setDamageSource(DamageSource.LAVA);

    public static final DamageType LIGHTNING = new DamageType(DamageTypeEnum.LIGHTNING, "Lightning", "%Killed% was electrified by lightning").setNoKnockback()
            .setDamageSource(DamageSource.LIGHTNING_BOLT);

    public static final DamageType MAGMA = new DamageType(DamageTypeEnum.MAGMA, "Magma", "%Killed% took a rest on some hot magma").setNoKnockback().setBurn()
            .setDamageSource(DamageSource.HOT_FLOOR);

    public static final DamageType MELEE = new DamageType(DamageTypeEnum.MELEE, "Melee", "%Killed% was murdered by %Killer%").setMelee();

    public static final DamageType MELTING = new DamageType(DamageTypeEnum.MELTING, "Melting", "%Killed% melted in the hot sun").setNoKnockback();

    public static final DamageType POISON = new DamageType(DamageTypeEnum.POISON, "Poison", "%Killed% drank a flask of poison").setIgnoreArmor()
            .setNoKnockback();

    public static final DamageType PROJECTILE = new DamageType(DamageTypeEnum.PROJECTILE, "Projectile", "%Killed% was shot by %Killer%").setProjectile();

    public static final DamageType QUIT = new DamageType(DamageTypeEnum.QUIT, "Quit", "%Killed% has left the game").setInstantDeath().setNoKnockback();

    public static final DamageType STARVATION = new DamageType(DamageTypeEnum.STARVATION, "Starvation", "%Killed% died from starvation").setIgnoreArmor()
            .setNoKnockback().setDamageSource(DamageSource.STARVE);

    public static final DamageType SUFFOCATION = new DamageType(DamageTypeEnum.SUFFOCATION, "Suffocation", "%Killed% choked on block").setNoKnockback()
            .setDamageSource(DamageSource.IN_WALL);

    public static final DamageType SUICIDE = new DamageType(DamageTypeEnum.SUICIDE, "Suicide", "%Killed% died").setInstantDeath().setNoKnockback();

    public static final DamageType SUICIDE_ASSISTED = new DamageType(DamageTypeEnum.SUICIDE_ASSISTED, "Assisted Suicide",
            "%Killed% saw T_0_E_D's face and died", "%Killed% caught a whiff " +
            "of their own body odour", "%Killed% thought Mineplex was better than Red Warfare", "%Killed% kicked a stray " +
            "cat because they thought it was funny", "%Killed% had negative social credit score", "%Killed% played " +
            "russian roulette, and lost", "%Killed% lost against themselves in a 1v1").setInstantDeath().setNoKnockback();
    
    public static final DamageType SWEEP_ATTACK = new DamageType(DamageTypeEnum.SWEEP_ATTACK, "Sweep Attack", "%Killed% was killed by %Killer%'s sweeping attack")
            .setMelee();

    public static final DamageType THORNS = new DamageType(DamageTypeEnum.THORNS, "Thorns", "%Killed% found out how the thorns enchantment works")
            .setNoKnockback();//TODO: construct with entity

    public static final DamageType UNKNOWN = new DamageType(DamageTypeEnum.UNKNOWN, "Unknown", "%Killed% died from unknown causes").setNoKnockback();

    public static final DamageType VOID = new DamageType(DamageTypeEnum.VOID, "Void", "%Killed% fell into the void").setIgnoreArmor().setNoKnockback()
            .setIgnoreRate().setDamageSource(DamageSource.OUT_OF_WORLD);

    public static final DamageType VOID_PUSHED = new DamageType(DamageTypeEnum.VOID_PUSHED, "Void Pushed", "%Killed% was knocked into the void by %Killer%")
            .setIgnoreArmor().setNoKnockback().setIgnoreRate().setDamageSource(DamageSource.OUT_OF_WORLD);

    public static final DamageType VOID_SHOT = new DamageType(DamageTypeEnum.VOID_SHOT, "Void Shot", "%Killed% was shot into the void by %Killer%")
            .setIgnoreArmor().setNoKnockback().setIgnoreRate().setDamageSource(DamageSource.OUT_OF_WORLD);

    public static final DamageType WITHER_POISON = new DamageType(DamageTypeEnum.WITHER_POISON, "Wither Poison", "%Killed% drank a vial of wither poison")
            .setNoKnockback().setDamageSource(DamageSource.WITHER);
    
    /******************************************************************************************
     *                                  KIT DAMAGETYPES
     ******************************************************************************************/
    
    public static final DamageType PYRO_MOLOTOV = new DamageType(DamageTypeEnum.PYRO_MOLOTOV, "Pyro Incendiary", "%Killed% was burned to death by %Killer%'s incendiary")
            .setFire().setIgnoreArmor().setNoKnockback().setDamageSource(DamageSource.ON_FIRE);

    //a constant identifier for same types, to compare for same types across separate instances of this class
    // without evaluating a String
    private final DamageTypeEnum id;
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
    //mainly for getting correct damage sound from nms in EntityUtils.playHurtAnimation
    private DamageSource nmsDamageSource;

    public DamageType(DamageTypeEnum id, String name, String... deathMessages) {
        this.id = id;
        _name = name;
        _deathMessages = deathMessages;
    }

    public DamageType(DamageType copyOf) {
        id = copyOf.id;
        _burn = copyOf._burn;
        _deathMessages = copyOf._deathMessages;
        _explosion = copyOf._explosion;
        _fall = copyOf._fall;
        _fire = copyOf._fire;
        _ignoreArmor = copyOf._ignoreArmor;
        _ignoreRate = copyOf._ignoreRate;
        _instantDeath = copyOf._instantDeath;
        _isntKnockback = copyOf._isntKnockback;
        _melee = copyOf._melee;
        _name = copyOf._name;
        _projectile = copyOf._projectile;
        nmsDamageSource = copyOf.nmsDamageSource;
    }

    public static DamageType getAttack(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();

        switch (cause) {
            case CONTACT:
                if(event instanceof EntityDamageByBlockEvent blockEvent) {
                    if(blockEvent.getDamager().getBlockData().getMaterial() == Material.SWEET_BERRY_BUSH)
                        return BERRY_BUSH;
                }
                return CACTUS;
            case ENTITY_ATTACK:
                return getMelee(event);
            case ENTITY_SWEEP_ATTACK:
                return getSweeping(event);
            case PROJECTILE:
                return getProjectile(event);
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
            case CRAMMING:
                return CRAMMING;
            case DRYOUT:
                return DRYOUT;
            case FREEZE:
                return FREEZE;
            default:
                return UNKNOWN;
        }
    }

    public String getDeathMessage() {
        return _deathMessages[MathUtils.randomMax(_deathMessages.length - 1)];
    }

    public Component getDeathMessage(TextColor color, Entity victim, Entity killer, Entity cause) {
        return getDeathMessage(color, EntityUtils.getName(victim), EntityUtils.getName(killer), EntityUtils.getName(cause));
    }

    // precompile regular expressions
    public static final Pattern KILLED_REGEX = Pattern.compile("%Killed%");
    public static final Pattern KILLER_REGEX = Pattern.compile("%Killer%");
    public static final Pattern CAUSE_REGEX = Pattern.compile("%Cause%");
    public Component getDeathMessage(TextColor color, Component victim, Component killer, Component cause) {
        String message = getDeathMessage();

        Component component = Component.text(message).color(color);

        if(victim != null) {
            component = component.replaceText(TextReplacementConfig.builder().match(KILLED_REGEX).replacement(victim).build());
        }
        //%Cause% is never used, but eh
        if(cause != null) {
            component = component.replaceText(TextReplacementConfig.builder().match(CAUSE_REGEX).replacement(cause).build());
        }
        if(killer != null) {
            component = component.replaceText(TextReplacementConfig.builder().match(KILLER_REGEX).replacement(killer).build());
        }

        return component;
    }

    public static DamageType getMelee(EntityDamageEvent event) {
        if(event instanceof EntityDamageByEntityEvent dEvent && dEvent.getDamager() instanceof org.bukkit.entity.LivingEntity living) {
            return new DamageType(MELEE).setDamageSource(DamageSourceCreator.getMelee(living));
        }
        else {
            return MELEE;
        }
    }
    
    public static DamageType getSweeping(EntityDamageEvent event) {
        if(event instanceof EntityDamageByEntityEvent dEvent && dEvent.getDamager() instanceof LivingEntity living) {
            return getSweeping(living);
        }
        else {
            return SWEEP_ATTACK;
        }
    }
    
    public static DamageType getSweeping(LivingEntity living) {
        return new DamageType(SWEEP_ATTACK).setDamageSource(DamageSourceCreator.getMelee(living).sweep());
    }

    public static DamageType getProjectile(EntityDamageEvent event) {
        if(event instanceof EntityDamageByEntityEvent dEvent) {
            Entity damager = dEvent.getDamager();
            LivingEntity shooter = null;
            if(damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity livingShooter) {
                shooter = livingShooter;
            }

            return new DamageType(PROJECTILE).setDamageSource(DamageSourceCreator.getProjectile(damager, shooter));
        }

        return PROJECTILE;
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
    
    //if two damagetypes are the same, used for melee/projectile/sweeping where a new DamageType instance
    // is constructed.
    public boolean is(DamageType damageType) {
        return this.id == damageType.id;
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

    public DamageType setDamageSource(DamageSource source) {
        this.nmsDamageSource = source;
        return this;
    }

    public DamageSource getDamageSource() {
        return this.nmsDamageSource != null ? this.nmsDamageSource : DamageSource.GENERIC;
    }

    public String toString() {
        return "DamageType[" + getName() + "]";
    }

    public static void checkDamageTypes() {
        //Entity for the sake of constructing damage events
        Pig entity = (Pig) Main.getGame().getWorld().spawnEntity(Main.getGame().getWorld().getSpawnLocation(), EntityType.PIG);

        for (EntityDamageEvent.DamageCause cause : EntityDamageEvent.DamageCause.values()) {
            EntityDamageEvent event = new EntityDamageEvent(entity, cause, 1);
            DamageType attack = DamageType.getAttack(event);

            if (attack == null || attack == DamageType.UNKNOWN) {
                Main.logger().warning("The DamageCause '" + cause.name() +
                        "' has not been registered as a DamageType");
            }
        }

        entity.remove();
    }
    
    private enum DamageTypeEnum {
        BERRY_BUSH,
        CACTUS,
        CRAMMING,
        CUSTOM,
        DAMAGE_POTION,
        DRAGON_BREATH,
        DROWNED,
        DRYOUT,
        EXPLOSION,
        FALL,
        FALL_PUSHED,
        FALL_SHOT,
        FALLING_BLOCK,
        FALLING_STALACTITE,
        FIRE,
        FIRE_ASPECT,
        FIRE_BOW,
        FIRE_TICK,
        FISHING_HOOK,
        FLY_INTO_WALL,
        FREEZE,
        LAVA,
        LIGHTNING,
        MAGMA,
        MELEE,
        MELTING,
        POISON,
        PROJECTILE,
        QUIT,
        STARVATION,
        SUFFOCATION,
        SUICIDE,
        SUICIDE_ASSISTED,
        SWEEP_ATTACK,
        THORNS,
        UNKNOWN,
        VOID,
        VOID_PUSHED,
        VOID_SHOT,
        WITHER_POISON,

        //Kit Stuff
        PYRO_MOLOTOV
    }
}
package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSniper;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.level.Level;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

//partially from RedWarfare's AttackType class - credit libraryaddict
// https://github.com/libraryaddict/RedWarfare/blob/master/redwarfare-core/src/me/libraryaddict/core/damage/AttackType.java
public class DamageType {

	/******************************************************************************************
     *                                  GENERAL DAMAGETYPES
     ******************************************************************************************/

    public static final DamageType BERRY_BUSH = new DamageType("Berry Bush", "%Killed% ate one too many sweet berries").setNoKnockback();

	public static final DamageType BORDER = new DamageType("Out of Bounds", "%Killed% went out of bounds")
		.setIgnoreArmor(true).setIgnoreRate();

    public static final DamageType CACTUS = new DamageType("Cactus", "%Killed% hugged a cactus").setNoKnockback();

    public static final DamageType CRAMMING = new DamageType("Cramming", "%Killer% was crammed to death")
            .setNoKnockback();

    public static final DamageType CUSTOM = new DamageType("Custom", "%Killed% fell over and died").setNoKnockback();

    public static final DamageType DAMAGE_POTION = new DamageType("Damage Potion",
            "%Killed% died to %Killer%'s damage potion which isn't part of the game?").setNoKnockback();

    public static final DamageType DRAGON_BREATH = new DamageType("Dragon Breath", "%Killed% sucked a lungful of dragon's breath")
            .setNoKnockback();

    public static final DamageType DROWNED = new DamageType("Drowned", "%Killed% is swimming with the fishes")
		.setIgnoreArmor(true).setNoKnockback();

    public static final DamageType DRYOUT = new DamageType("Dryout", "%Killed% died of dehydration")
		.setIgnoreArmor(true).setNoKnockback();

    public static final DamageType EXPLOSION = new DamageType("Explosion", "%Killed% was caught in an explosion")
			.setExplosion();

    public static final DamageType FALL = new DamageType("Fall", "%Killed% fell to their death").setFall().setNoKnockback();

    public static final DamageType FALL_PUSHED = new DamageType("Pushed Fall", "%Killed% was pushed to their death by %Killer%")
            .setFall().setNoKnockback();

    public static final DamageType FALL_SHOT = new DamageType("Shot Fall", "%Killed% was shot by %Killer% and fell to their death")
            .setFall().setNoKnockback();

    public static final DamageType FALLING_BLOCK = new DamageType("Falling Block", "%Killed% was crushed beneath a falling block")
            .setNoKnockback();// DamageSource assigned in getAttack() //setDamageSource(DamageSource.fallingBlock(null));

    public static final DamageType FALLING_STALACTITE = new DamageType("Falling Stalactite", "%Killed% was impaled by a " +
            "falling stalactite").setNoKnockback();// DamageSource in getAttack() //.setDamageSource(DamageSource.FALLING_STALACTITE);

    /**
     * Direct exposure to fire
     */
    public static final DamageType FIRE = new DamageType("Direct Fire", "%Killed% stood inside flames and laughed").setNoKnockback()
            .setBurn();

    public static final DamageType FIRE_ASPECT = new DamageType("Fire Aspect", "%Killed% was charred to a crisp by %Killer%")
		.setFire().setNoKnockback().setIgnoreArmor(false);

    public static final DamageType FIRE_BOW = new DamageType("Fire Bow", "%Killed% charred to a crisp by %Killer%'s bow")
		.setFire().setNoKnockback().setIgnoreArmor(false);

    /**
     * Fire on the entity itself
     */
    public static final DamageType FIRE_TICK = new DamageType("Fire Tick", "%Killed% was burned alive")
		.setNoKnockback().setFire().setIgnoreArmor(false);

	public static final DamageType CAMPFIRE = new DamageType("Campfire", "%Killed% got too cozy at a campfire")
		.setNoKnockback().setBurn();

    public static final DamageType FISHING_HOOK = new DamageType("Fishing Hook", "%Killed% was killed by %Killer%'s... Fishing rod?");

    public static final DamageType FLY_INTO_WALL = new DamageType("Flew into Wall", "%Killed% still hadn't gotten the hang of flying")
            .setNoKnockback();

    public static final DamageType FREEZE = new DamageType("Freeze", "%Killed% froze to death")
            .setIgnoreArmor(false);

    public static final DamageType LAVA = new DamageType("Lava", "%Killed% tried to swim in lava").setNoKnockback().setBurn();

    public static final DamageType LIGHTNING = new DamageType("Lightning", "%Killed% was electrified by lightning").setNoKnockback();

    public static final DamageType MAGMA = new DamageType("Magma", "%Killed% took a rest on some hot magma").setNoKnockback().setBurn();

    public static final DamageType MELEE = new DamageType("Melee", "%Killed% was murdered by %Killer%").setMelee();

    public static final DamageType MELTING = new DamageType("Melting", "%Killed% melted in the hot sun").setNoKnockback();

    public static final DamageType POISON = new DamageType("Poison", "%Killed% drank a flask of poison").setIgnoreArmor(true)
            .setNoKnockback().setTrackedDamageType(DamageTimes.TrackedDamageTypes.POISON);

    public static final DamageType PROJECTILE = new DamageType("Projectile", "%Killed% was shot by %Killer%").setProjectile();

    public static final DamageType QUIT = new DamageType("Quit"/*, "%Killed% has left the game"*/)
		.setInstantDeath().setNoKnockback().setIgnoreRate();

	public static final DamageType SONIC_BOOM = new DamageType("Sonic Boom", "%Killed% was killed by %Killer%'s sonic boom")
			.setIgnoreArmor(true);

    public static final DamageType STARVATION = new DamageType("Starvation", "%Killed% died from starvation")
		.setIgnoreArmor(true).setNoKnockback();

    public static final DamageType SUFFOCATION = new DamageType("Suffocation", "%Killed% choked on block").setNoKnockback();

    public static final DamageType SUICIDE = new DamageType("Suicide", "%Killed% died")
		.setInstantDeath().setNoKnockback().setIgnoreRate();

	public static final DamageType SPECTATE = new DamageType("Suicide", "%Killed% died").setInstantDeath().setNoKnockback();

    public static final DamageType KILL_ADMIN = new DamageType("Admin",
            "%Killed% saw T_0_E_D's face and died", "%Killed% caught a whiff " +
            "of their own body odour", "%Killed% thought Mineplex was better than Red Warfare", "%Killed% kicked a stray " +
            "cat and thought it was funny", "%Killed% had negative social credit score", "%Killed% played " +
            "russian roulette, and lost", "%Killed% lost against themselves in a 1v1")
		.setInstantDeath().setNoKnockback().setIgnoreRate();

    public static final DamageType SWEEP_ATTACK = new DamageType("Sweep Attack", "%Killed% was killed by %Killer%'s sweeping attack")
            .setMelee();

    public static final DamageType THORNS = new DamageType("Thorns", "%Killed% found out how the thorns enchantment works")
            .setNoKnockback();

    public static final DamageType UNKNOWN = new DamageType("Unknown", "%Killed% died from unknown causes").setNoKnockback();

    public static final DamageType VOID = new DamageType("Void", "%Killed% fell into the void").setIgnoreArmor(true).setNoKnockback()
            .setIgnoreRate();

    public static final DamageType VOID_PUSHED = new DamageType("Void Pushed", "%Killed% was knocked into the void by %Killer%")
            .setIgnoreArmor(true).setNoKnockback().setIgnoreRate();

	public static final DamageType VOID_PUSHED_SELF = new DamageType(VOID_PUSHED, "%Killed% pushed themselves into the void");

    public static final DamageType VOID_SHOT = new DamageType("Void Shot", "%Killed% was shot into the void by %Killer%")
            .setIgnoreArmor(true).setNoKnockback().setIgnoreRate();

    public static final DamageType WITHER_POISON = new DamageType("Wither Poison", "%Killed% withered to death")
            .setNoKnockback();

    /******************************************************************************************
     *                                  KIT BASE DAMAGETYPES
     ******************************************************************************************/

	public static final DamageType NONE_MELEE = new DamageType(MELEE, "%Killed% was punched to death by %Killer% using %Weapon%",
		"%Killed% was beaten to death by %Killer% using %Weapon%", "%Killed% was no match against %Killer%'s %Weapon%",
		"%Killed% was bludgeoned to death by %Killer% using %Weapon%");

    public static final DamageType PYRO_MOLOTOV = new DamageType("Pyro Incendiary", "%Killed% was burned to death by %Killer%'s incendiary")
            .setFire().setIgnoreArmor(false).setNoKnockback();

	public static final DamageType SNIPER_GRENADE_FAIL = new DamageType("Grenade Fail", "%Killed% forgot they pulled the pin.")
			.setInstantDeath().setNoKnockback();

	/**
	 * for sniper kills, damage types with additional death messages are supplied by the kit itself
	 * @see KitSniper.SniperDamageTypes
 	 */
	@ApiStatus.Internal
	public static final DamageType SNIPER_SHOT = new DamageType("Sniper Shot", "%Killed% was sniped by %Killer%")
		.setProjectile();

	@ApiStatus.Internal
	public static final DamageType SNIPER_HEADSHOT = new DamageType("Sniper Headshot", "%Killed% was headshot by %Killer%")
		.setProjectile();

	public static final DamageType SNIPER_MELEE = new DamageType(MELEE, "%Killed% was stabbed by %Killer% using %Weapon%");

    public static final DamageType DEMO_TNTMINE = new DamageType("Demolitions TNT Mine",
            "%Killed% stepped on %Killer%'s TNT Mine and blew up")
            .setIgnoreRate().setExplosion();

	public static final DamageType TOXIC_LEAP = new DamageType("Venom Leap", "%Killed% was killed by %Killer%'s Toxic Leap");

	public static final DamageType DEMO_PUSHMINE = new DamageType("Demolitions Push Mine",
			"%Killed% was somehow directly killed by %Killer%'s Push Mine. (If you see this please tell how you did it" +
					" to a mod. Thanks)").setTrackedDamageType(DamageTimes.TrackedDamageTypes.ATTACK).setIgnoreRate();

	public static final DamageType BURST_ROCKET = new DamageType("Burst Rocket", "%Killed% was blown up by %Killer%'s rocket")
			.setExplosion();

	public static final DamageType BURST_FIREWORK = new DamageType("Burst Firework", "%Killed% was blown to shimmering, shining bits by %Killer%'s firework")
			.setExplosion();

	public static final DamageType BURST_FIREWORK_REFLECTED = new DamageType("Reflected Firework", "%Killed% had %Cause%'s firework reflected at them by %Killer%")
			.setExplosion();

	public static final DamageType BURST_FIREWORK_SELF = new DamageType("Badly Aimed Firework", "%Killed% became a part of their firework show")
			.setIgnoreRate().setExplosion();

	public static final DamageType BURST_SHOTGUN = new DamageType("Burst Blast", "%Killed% was killed by %Killer%'s firework shrapnel")
			.setIgnoreRate().setProjectile().setNoKnockback();

	public static final DamageType BURST_SHOTGUN_SELF = new DamageType("Burst Blast Self Harm", "%Killed% went trigger happy and blew their fingers off")
			.setIgnoreRate().setIgnoreArmor(true).setExplosion();

	public static final DamageType EXPLOSIVE_RPG = new DamageType("Explosive RPG", "%Killed% was caught in %Killer%'s RPG")
			.setExplosion();

	public static final DamageType EXPLOSIVE_RPG_REFLECTED = new DamageType("Reflected RPG", "%Killed% died to %Killer% reflecting %Cause%'s RPG")
			.setExplosion();

	public static final DamageType EXPLOSIVE_RPG_SELF = new DamageType("Explosive Self RPG", "%Killed% shot their RPG a bit too close to themselves")
			.setExplosion();

	public static final DamageType EXPLOSIVE_GRENADE = new DamageType("Explosive Grenade", "%Killed% was blown up by %Killer%'s grenades")
			.setExplosion();

	public static final DamageType TRIGGER_UNSTABLE_EXPLODE = new DamageType("Trigger Unstable Explosion", "%Killed% tried to hug %Killer%, who exploded from instability")
			.setExplosion().setIgnoreRate();

	public static final DamageType TRIGGER_UNSTABLE_SELF_KILL = new DamageType("Trigger Instability", "%Killed% went emotionally unstable and blew up")
			.setInstantDeath().setIgnoreRate();

	public static final DamageType TRIGGER_BOOM_SELF = new DamageType("Trigger self-destruct", "%Killed% blew themselves up")
		.setInstantDeath().setIgnoreRate();

	public static final DamageType TRIGGER_BOOM = new DamageType("Trigger BOOM", "%Killed% was caught in %Killer%'s explosion of madness")
			.setExplosion().setIgnoreRate();

	public static final DamageType ENGINEER_SENTRY = new DamageType("Sentry arrow", "%Killed% was shot by %Killer%'s sentry")
			.setProjectile().setNoKnockback();

	public static final DamageType ENGINEER_SENTRY_REFLECTED = new DamageType("Reflected Sentry Arrow", "%Killed% <-- %Killer% reflect <-- %Cause%'s sentry")
			.setProjectile().setNoKnockback();

	public static final DamageType BEEKEEPER_BEE = new DamageType("Beekeeper's bee", "%Killed% was stung by %Killer%'s bee");

	public static final DamageType RATIO_CRIT = new DamageType("Splitter critical hit", "%Killed% was split in half by %Killer%").setMelee();
	public static final DamageType REFLECTED_RATIO_CRIT = new DamageType("Reflected splitter critical hit", "%Killed% had their splitter bounce off %Killer%").setMelee();

	/*******************************************************************************************
	 * 									GAMEMODE DAMAGETYPES
	 ******************************************************************************************/

	public static final DamageType BOMB_EXPLODED = new DamageType("Team Bomb").setInstantDeath().setIgnoreRate();

	public static final DamageType END_GAME_LIGHTNING = new DamageType("Herobrine", "%Killed% was killed by Herobrine")
			.setIgnoreArmor(true).setNoKnockback().setIgnoreRate();

	/*******************************************************************************************
	 * 									KILLSTREAK DAMAGETYPES
	 ******************************************************************************************/
	public static final DamageType WOLF_KILL = new DamageType("Attack Wolves", "%Killed% was mauled by %Killer%'s %Cause%")
			.setMelee();

	public static final DamageType IRON_GOLEM_KILL = new DamageType("Iron Golem", "%Killed% was hydraulically pressed by %Killer%'s %Cause%")
			.setMelee();

	public static final DamageType HARBINGER = new DamageType("Harbinger", "%Killed% was obliterated by %Killer%'s Harbinger")
			.setIgnoreArmor(true).setIgnoreRate();

	/*********************************************************************************************
	 * 									ITEM DAMAGETYPES
	 *********************************************************************************************/

	public static final DamageType RAILGUN = new DamageType("Railgun", "%Killed% was perforated by %Killer%'s Railgun")
		.setIgnoreRate().setNoKnockback().setProjectile().setIgnoreArmor(true);

	public static final DamageType TRIDENT_PROJECTILE = new DamageType("Trident", "%Killed% was impaled by %Killer%")
		.setProjectile();

	public static final DamageType TRIDENT_MELEE = new DamageType(MELEE, "%Killed% was impaled by %Killer%")
		.setMelee();

	private static int idCounter = 0;
	static DamageSources nmsDamageSources;

    //a constant identifier for same types, to compare for same types across separate instances of this class
    // without evaluating a String
	private final int id;
	private final DeathMessage[] _deathMessages;
	private final List<Enchantment> applicableEnchantments;
    private boolean _burn;
    private boolean _explosion;
    private boolean _fall;
    private boolean _fire;
    private boolean _ignoreArmor;
    private boolean _ignoreRate;
    private boolean _instantDeath;
    private boolean _isntKnockback;
    private boolean _melee;
	private boolean _projectile;
    private final String _name;
    //for getting correct damage sound from nms in EntityUtils.playHurtAnimation
    private DamageSource nmsDamageSource;
	private DamageTimes.TrackedDamageTypes trackedType;

    private DamageType(String name, String... deathMessages) {
		this.id = nextId();
        _name = name;
        _deathMessages = DeathMessage.parseArray(deathMessages);
		applicableEnchantments = new ArrayList<>(2);
		applicableEnchantments.add(Enchantment.PROTECTION);
		trackedType = DamageTimes.TrackedDamageTypes.OTHER;
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
		applicableEnchantments = new ArrayList<>(copyOf.applicableEnchantments);
		trackedType = copyOf.trackedType;
    }

    public DamageType(DamageType copyOf, String... deathMessages) {
        id = copyOf.id;
        _burn = copyOf._burn;
        _deathMessages = DeathMessage.parseArray(deathMessages);
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
		applicableEnchantments = copyOf.applicableEnchantments;
		trackedType = copyOf.trackedType;
    }

	private static int nextId() {
		return idCounter++;
	}

	public static void updateDamageSources(TeamArena game) {
		Level nmsWorld = ((CraftWorld) game.getWorld()).getHandle();
		nmsDamageSources = nmsWorld.damageSources();

		BERRY_BUSH.setDamageSource(nmsDamageSources.sweetBerryBush());
		BORDER.setDamageSource(nmsDamageSources.outOfBorder());
		CACTUS.setDamageSource(nmsDamageSources.cactus());
		CRAMMING.setDamageSource(nmsDamageSources.cramming());
		CUSTOM.setDamageSource(nmsDamageSources.generic());
		DAMAGE_POTION.setDamageSource(nmsDamageSources.magic());
		DRAGON_BREATH.setDamageSource(nmsDamageSources.dragonBreath());
		DROWNED.setDamageSource(nmsDamageSources.drown());
		DRYOUT.setDamageSource(nmsDamageSources.dryOut());
		EXPLOSION.setDamageSource(nmsDamageSources.explosion(null, null)); // Should be handled per-event anyway
		FALL.setDamageSource(nmsDamageSources.fall());
		FALL_PUSHED.setDamageSource(nmsDamageSources.fall());
		FALL_SHOT.setDamageSource(nmsDamageSources.fall());
		//FALLING_BLOCK
		//FALLING_STALACTITE Both handled from getAttack()
		FIRE.setDamageSource(nmsDamageSources.inFire());
		FIRE_ASPECT.setDamageSource(nmsDamageSources.onFire());
		FIRE_BOW.setDamageSource(nmsDamageSources.onFire());
		FIRE_TICK.setDamageSource(nmsDamageSources.onFire());
		CAMPFIRE.setDamageSource(nmsDamageSources.campfire());
		FLY_INTO_WALL.setDamageSource(nmsDamageSources.flyIntoWall());
		FREEZE.setDamageSource(nmsDamageSources.freeze());
		LAVA.setDamageSource(nmsDamageSources.lava());
		LIGHTNING.setDamageSource(nmsDamageSources.lightningBolt());
		MAGMA.setDamageSource(nmsDamageSources.hotFloor());
		// MELEE Handled in getAttack
		// These two taken from what CraftBukkit does
		MELTING.setDamageSource(nmsDamageSources.onFire().knownCause(EntityDamageEvent.DamageCause.MELTING));
		POISON.setDamageSource(nmsDamageSources.magic().knownCause(EntityDamageEvent.DamageCause.POISON));
		// PROJECTILE handled in getAttack
		// SONIC_BOOM
		STARVATION.setDamageSource(nmsDamageSources.starve());
		SUFFOCATION.setDamageSource(nmsDamageSources.inWall());
		SUICIDE.setDamageSource(nmsDamageSources.genericKill());
		// SWEEP ATTACK
		// THORNS
		VOID.setDamageSource(nmsDamageSources.fellOutOfWorld());
		VOID_PUSHED.setDamageSource(nmsDamageSources.fellOutOfWorld());
		VOID_SHOT.setDamageSource(nmsDamageSources.fellOutOfWorld());
		WITHER_POISON.setDamageSource(nmsDamageSources.wither());

		/* KITS */
		PYRO_MOLOTOV.setDamageSource(nmsDamageSources.onFire());
		SNIPER_GRENADE_FAIL.setDamageSource(nmsDamageSources.explosion(null, null)); // TODO handle source and attacker
		// SNIPER_HEADSHOT TODO handle
		DEMO_TNTMINE.setDamageSource(nmsDamageSources.explosion(null, null));
		TOXIC_LEAP.setDamageSource(nmsDamageSources.magic().knownCause(EntityDamageEvent.DamageCause.POISON));
		// BURST_FIREWORK TODO nmsD.firework( ,)
		// BURST_FIREWORK_SELF.setDamageSource()
		// BURST_SHOTGUN_SELF

	}

    public static DamageType getAttack(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();

        switch (cause) {
            case CONTACT:
                if(event instanceof EntityDamageByBlockEvent blockEvent) {
                    if(blockEvent.getDamager() != null && blockEvent.getDamager().getType() == Material.SWEET_BERRY_BUSH)
                        return BERRY_BUSH;
                }
                return CACTUS;
            case ENTITY_ATTACK:
                return getMelee(event);
            case ENTITY_SWEEP_ATTACK:
                return getSweeping(event);
            case PROJECTILE:
                return getProjectile(event);
			case BLOCK_EXPLOSION:
			case ENTITY_EXPLOSION:
				return getExplosion(EXPLOSION, event);
            case SUFFOCATION:
                return SUFFOCATION;
            case FALL:
                return FALL;
            case FIRE:
                return FIRE;
			case CAMPFIRE:
				return CAMPFIRE;
            case FIRE_TICK:
                return FIRE_TICK;
            case MELTING:
                return MELTING;
            case LAVA:
                return LAVA;
            case DROWNING:
                return DROWNED;
            case VOID:
                return VOID;
            case LIGHTNING:
                return LIGHTNING;
            case SUICIDE:
			case KILL:
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
                return getFallingBlock(event);
            case THORNS:
                return getThorns(event);
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
			case SONIC_BOOM:
				return getSonicBoom(event);
			case WORLD_BORDER:
				return BORDER;
            default:
				Main.logger().warning("DamageType UNKNOWN returned for " + event.getEventName() + ", " + event.getCause() +
					", " + event.getEntity());
                return UNKNOWN;
        }
    }

	@Nullable
    public DeathMessage getDeathMessage() {
		if(hasDeathMessages()) {
			return MathUtils.randomElement(_deathMessages);
		}

		return null;
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
        if(event instanceof EntityDamageByEntityEvent dEvent && dEvent.getDamager() instanceof Player p) {
			return getSweeping(p);
        }
        else {
            return SWEEP_ATTACK;
        }
    }

	public static DamageType getSweeping(Player p) {
		return new DamageType(SWEEP_ATTACK).setDamageSource(DamageSourceCreator.getSweeping(p));
	}

	public static DamageType getSonicBoom(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent) {
			return new DamageType(SONIC_BOOM).setDamageSource(
					nmsDamageSources.sonicBoom(((CraftEntity) entityDamageByEntityEvent.getDamager()).getHandle())
			);
		}
		return SONIC_BOOM;
	}

	private static DamageType getExplosion(DamageType type, EntityDamageEvent event) {
		// TODO bukkit broadcast
		if(event instanceof EntityDamageByEntityEvent dEvent) {
			DamageSource source;
			if(dEvent.getDamager() instanceof TNTPrimed tnt)
				source = DamageSourceCreator.getExplosion(tnt, tnt.getSource());
			else
				source = DamageSourceCreator.getExplosion(dEvent.getDamager(), null);

			return new DamageType(type).setDamageSource(source);
		}
		else {
			return type;
		}
	}

	private static DamageType getFallingBlock(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent dEvent) {
			return new DamageType(FALLING_BLOCK).setDamageSource(DamageSourceCreator.getFallingBlock(dEvent.getDamager()));
		}

		return FALLING_BLOCK;
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

	private static DamageType getThorns(EntityDamageEvent event) {
		if(event instanceof EntityDamageByEntityEvent dEvent) {
			return new DamageType(THORNS).setDamageSource(nmsDamageSources.thorns(((CraftEntity) dEvent.getDamager()).getHandle()));
		}

		return THORNS;
	}

    public String getName() {
        return _name;
    }

	public DamageTimes.TrackedDamageTypes getTrackedType() {
		return this.trackedType;
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

	public boolean isVoid() {
		return this.is(VOID) || this.is(VOID_PUSHED) || this.is(VOID_SHOT);
	}

	public boolean hasDeathMessages() {
		return this._deathMessages.length > 0;
	}

	public boolean hasApplicableEnchants() {
		return this.applicableEnchantments.size() > 0;
	}

    //if two damagetypes are the same, used for melee/projectile/sweeping where a new DamageType instance
    // is constructed.
    public boolean is(DamageType damageType) {
        return this.id == damageType.id;
    }

	/**
	 * When need to use a DamageType and copy the DamageSource from another.
	 */
	public DamageType withDamageSource(DamageType other) {
		return new DamageType(this).setDamageSource(other.nmsDamageSource);
	}

    public DamageType setBurn() {
        _burn = true;

		addApplicableEnchant(Enchantment.FIRE_PROTECTION);
		setTrackedDamageType(DamageTimes.TrackedDamageTypes.FIRE);

        return this;
    }

    public DamageType setExplosion() {
        _explosion = true;

		addApplicableEnchant(Enchantment.BLAST_PROTECTION);
		setTrackedDamageType(DamageTimes.TrackedDamageTypes.ATTACK);

        return this;
    }

    public DamageType setFall() {
        _fall = true;
        setIgnoreArmor(false);
        setIgnoreRate();
		addApplicableEnchant(Enchantment.FEATHER_FALLING);
		setTrackedDamageType(DamageTimes.TrackedDamageTypes.OTHER);

        return this;
    }

    public DamageType setFire() {
        setBurn();

        _fire = true;
		removeApplicableEnchant(Enchantment.PROTECTION);
		setTrackedDamageType(DamageTimes.TrackedDamageTypes.FIRE);

        return this;
    }

    public DamageType setIgnoreArmor(boolean ignoreEnchants) {
        _ignoreArmor = true;

		if (ignoreEnchants)
			this.setIgnoreArmorEnchants();

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

		setTrackedDamageType(DamageTimes.TrackedDamageTypes.ATTACK);

        return this;
    }

    public DamageType setNoKnockback() {
        _isntKnockback = true;

        return this;
    }

    public DamageType setProjectile() {
        _projectile = true;

		addApplicableEnchant(Enchantment.PROJECTILE_PROTECTION);
		setTrackedDamageType(DamageTimes.TrackedDamageTypes.ATTACK);

		return this;
    }

	public DamageType setIgnoreArmorEnchants() {
		applicableEnchantments.clear();

		return this;
	}

	public DamageType setTrackedDamageType(DamageTimes.TrackedDamageTypes type) {
		this.trackedType = type;

		return this;
	}

	public DamageType addApplicableEnchant(Enchantment enchantment) {
		if(!applicableEnchantments.contains(enchantment)) {
			applicableEnchantments.add(enchantment);
		}

		return this;
	}

	public DamageType removeApplicableEnchant(Enchantment enchantment) {
		applicableEnchantments.remove(enchantment);

		return this;
	}

	/**
	 * shouldn't contain duplicates
	 */
	public List<Enchantment> getApplicableEnchantments() {
		return applicableEnchantments;
	}

    public DamageType setDamageSource(DamageSource source) {
        this.nmsDamageSource = source;
        return this;
    }

    public DamageSource getDamageSource() {
        return this.nmsDamageSource != null ? this.nmsDamageSource : nmsDamageSources.generic();
    }

    public String toString() {
        return "DamageType[" + getName() + "]";
    }

    @SuppressWarnings("UnstableApiUsage")
	public static void checkDamageTypes() {
        //Entity for the sake of constructing damage events
        Pig entity = Main.getGame().getWorld().spawn(Main.getGame().getWorld().getSpawnLocation(), Pig.class);

		org.bukkit.damage.DamageSource source = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.GENERIC).build();
        for (EntityDamageEvent.DamageCause cause : EntityDamageEvent.DamageCause.values()) {
			EntityDamageEvent event = new EntityDamageEvent(entity, cause, source, 1d);
            DamageType attack = DamageType.getAttack(event);

            if (attack == null || attack == DamageType.UNKNOWN) {
                Main.logger().warning("The DamageCause '" + cause.name() + "' has not been registered as a DamageType");
            }
        }

        entity.remove();
    }

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DamageType other &&
				id == other.id;
	}

	/**
	 * A death message with certain placeholders
	 * @param fragments The component fragments
	 * @param placeholders The placeholders
	 */
	public record DeathMessage(Component[] fragments, Placeholder[] placeholders) {
		private static final TextColor MESSAGE_COLOR = NamedTextColor.YELLOW;
		private static final TextColor MESSAGE_COLOR_DARKENED = TextUtils.darken(NamedTextColor.YELLOW);

		public enum Placeholder {
			VICTIM, ATTACKER, CAUSE, WEAPON
		}

		private static final Pattern LEGACY_REGEX = Pattern.compile("%(?:Kille[dr]|Cause|Weapon)%");
		public static DeathMessage parse(String string) {
			List<Component> fragments = new ArrayList<>();
			List<Placeholder> placeholders = new ArrayList<>();
			int index = 0;
			var matcher = LEGACY_REGEX.matcher(string);
			while (matcher.find()) {
				fragments.add(Component.text(string.substring(index, matcher.start())));
				placeholders.add(switch (string.substring(matcher.start() + 1, matcher.end() - 1)) {
					case "Killed" -> Placeholder.VICTIM;
					case "Killer" -> Placeholder.ATTACKER;
					case "Cause" -> Placeholder.CAUSE;
					case "Weapon" -> Placeholder.WEAPON;
					default -> throw new Error();
				});
				index = matcher.end();
			}
			// append tail
			fragments.add(Component.text(string.substring(index)));

			return new DeathMessage(fragments.toArray(new Component[0]), placeholders.toArray(new Placeholder[0]));
		}

		public static DeathMessage[] parseArray(String... strings) {
			DeathMessage[] deathMessages = new DeathMessage[strings.length];
			for (int i = 0; i < strings.length; i++) {
				deathMessages[i] = parse(strings[i]);
			}
			return deathMessages;
		}

		public Component render(@Nullable Entity victim, @Nullable Entity attacker, @Nullable Entity cause) {
			return render(false, victim, attacker, cause);
		}

		public Component renderDarkened(@Nullable Entity victim, @Nullable Entity attacker, @Nullable Entity cause) {
			return render(true, victim, attacker, cause);
		}

		private Component render(boolean darken, @Nullable Entity victim, @Nullable Entity attacker, @Nullable Entity cause) {
			var builder = Component.text().color(darken ? MESSAGE_COLOR_DARKENED : MESSAGE_COLOR);
			for (int i = 0; i < placeholders.length; i++) {
				builder.append(fragments[i]);
				Component value = switch (placeholders[i]) {
					case VICTIM -> EntityUtils.getComponent(victim);
					case ATTACKER -> EntityUtils.getComponent(attacker);
					case CAUSE -> EntityUtils.getComponent(cause);
					case WEAPON -> {
						ItemStack stack;
						if (attacker instanceof LivingEntity livingEntity &&
							livingEntity.getEquipment() instanceof EntityEquipment ee && // I LOVE NULLS
							!(stack = ee.getItemInMainHand()).isEmpty()) {
							yield stack.displayName();
						} else {
							yield Component.empty();
						}
					}
				};
				builder.append(darken ? TextUtils.darken(value) : value);
			}
			// append tail
			builder.append(fragments[fragments.length - 1]);
			return builder.build();
		}
	}
}
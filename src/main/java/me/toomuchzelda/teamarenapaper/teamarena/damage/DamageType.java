package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

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

    public static final DamageType BERRY_BUSH = new DamageType("Berry Bush", "%Killed% ate one too many sweet berries").setNoKnockback()
            .setDamageSource(DamageSource.SWEET_BERRY_BUSH);

    public static final DamageType CACTUS = new DamageType("Cactus", "%Killed% hugged a cactus").setNoKnockback()
            .setDamageSource(DamageSource.CACTUS);

    public static final DamageType CRAMMING = new DamageType("Cramming", "%Killer% was crammed to death").setIgnoreArmor()
            .setNoKnockback().setDamageSource(DamageSource.CRAMMING);

    public static final DamageType CUSTOM = new DamageType("Custom", "%Killed% fell over and died").setNoKnockback()
            .setDamageSource(DamageSource.GENERIC);

    public static final DamageType DAMAGE_POTION = new DamageType("Damage Potion",
            "%Killed% died to %Killer%'s damage potion which isn't part of the game?").setNoKnockback()
            .setDamageSource(DamageSource.MAGIC);

    public static final DamageType DRAGON_BREATH = new DamageType("Dragon Breath", "%Killed% sucked a lungful of dragon's breath")
            .setNoKnockback().setDamageSource(DamageSource.DRAGON_BREATH);

    public static final DamageType DROWNED = new DamageType("Drowned", "%Killed% is swimming with the fishes").setIgnoreArmor()
            .setNoKnockback().setDamageSource(DamageSource.DROWN);

    public static final DamageType DRYOUT = new DamageType("Dryout", "%Killed% died of dehydration")
            .setNoKnockback().setDamageSource(DamageSource.DRY_OUT);

    public static final DamageType EXPLOSION = new DamageType("Explosion", "%Killed% was caught in an explosion")
			.setExplosion().setDamageSource(DamageSource.explosion((net.minecraft.world.entity.LivingEntity) null));

    public static final DamageType FALL = new DamageType("Fall", "%Killed% fell to their death").setFall().setNoKnockback()
            .setDamageSource(DamageSource.FALL);

    public static final DamageType FALL_PUSHED = new DamageType("Pushed Fall", "%Killed% was pushed to their death by %Killer%")
            .setFall().setNoKnockback().setDamageSource(DamageSource.FALL);

    public static final DamageType FALL_SHOT = new DamageType("Shot Fall", "%Killed% was shot by %Killer% and fell to their death")
            .setFall().setNoKnockback().setDamageSource(DamageSource.FALL);

    public static final DamageType FALLING_BLOCK = new DamageType("Falling Block", "%Killed% was crushed beneath a falling block")
            .setNoKnockback().setDamageSource(DamageSource.FALLING_BLOCK);

    public static final DamageType FALLING_STALACTITE = new DamageType("Falling Stalactite", "%Killed% was impaled by a " +
            "falling stalactite").setNoKnockback().setDamageSource(DamageSource.FALLING_STALACTITE);

    /**
     * Direct exposure to fire
     */
    public static final DamageType FIRE = new DamageType("Direct Fire", "%Killed% stood inside flames and laughed").setNoKnockback()
            .setBurn().setDamageSource(DamageSource.IN_FIRE);

    public static final DamageType FIRE_ASPECT = new DamageType("Fire Aspect", "%Killed% was charred to a crisp by %Killer%").setFire()
            .setNoKnockback().setIgnoreArmor().setDamageSource(DamageSource.ON_FIRE);

    public static final DamageType FIRE_BOW = new DamageType("Fire Bow", "%Killed% charred to a crisp by %Killer%'s bow").setFire()
            .setNoKnockback().setIgnoreArmor().setDamageSource(DamageSource.ON_FIRE);

    /**
     * Fire on the entity itself
     */
    public static final DamageType FIRE_TICK = new DamageType("Fire Tick", "%Killed% was burned alive").setNoKnockback().setFire()
            .setIgnoreArmor().setDamageSource(DamageSource.ON_FIRE);

    public static final DamageType FISHING_HOOK = new DamageType("Fishing Hook", "%Killed% was killed by %Killer%'s... Fishing rod?");

    public static final DamageType FLY_INTO_WALL = new DamageType("Flew into Wall", "%Killed% still hadn't gotten the hang of flying")
            .setNoKnockback().setDamageSource(DamageSource.FLY_INTO_WALL);

    public static final DamageType FREEZE = new DamageType("Freeze", "%Killed% froze to death")
            .setIgnoreArmor().setDamageSource(DamageSource.FREEZE);

    public static final DamageType LAVA = new DamageType("Lava", "%Killed% tried to swim in lava").setNoKnockback().setBurn()
            .setDamageSource(DamageSource.LAVA);

    public static final DamageType LIGHTNING = new DamageType("Lightning", "%Killed% was electrified by lightning").setNoKnockback()
            .setDamageSource(DamageSource.LIGHTNING_BOLT);

    public static final DamageType MAGMA = new DamageType("Magma", "%Killed% took a rest on some hot magma").setNoKnockback().setBurn()
            .setDamageSource(DamageSource.HOT_FLOOR);

    public static final DamageType MELEE = new DamageType("Melee", "%Killed% was murdered by %Killer%").setMelee();

    public static final DamageType MELTING = new DamageType("Melting", "%Killed% melted in the hot sun").setNoKnockback();

    public static final DamageType POISON = new DamageType("Poison", "%Killed% drank a flask of poison").setIgnoreArmor()
            .setNoKnockback().setTrackedDamageType(DamageTimes.TrackedDamageTypes.POISON);

    public static final DamageType PROJECTILE = new DamageType("Projectile", "%Killed% was shot by %Killer%").setProjectile();

    public static final DamageType QUIT = new DamageType("Quit", "%Killed% has left the game").setInstantDeath().setNoKnockback();

	public static final DamageType SONIC_BOOM = new DamageType("Sonic Boom", "%Killed% was killed by %Killer%'s sonic boom")
			.setIgnoreArmor().setIgnoreArmorEnchants();

    public static final DamageType STARVATION = new DamageType("Starvation", "%Killed% died from starvation").setIgnoreArmor()
            .setNoKnockback().setDamageSource(DamageSource.STARVE);

    public static final DamageType SUFFOCATION = new DamageType("Suffocation", "%Killed% choked on block").setNoKnockback()
            .setDamageSource(DamageSource.IN_WALL);

    public static final DamageType SUICIDE = new DamageType("Suicide", "%Killed% died").setInstantDeath().setNoKnockback();

    public static final DamageType SUICIDE_ASSISTED = new DamageType("Assisted Suicide",
            "%Killed% saw T_0_E_D's face and died", "%Killed% caught a whiff " +
            "of their own body odour", "%Killed% thought Mineplex was better than Red Warfare", "%Killed% kicked a stray " +
            "cat and thought it was funny", "%Killed% had negative social credit score", "%Killed% played " +
            "russian roulette, and lost", "%Killed% lost against themselves in a 1v1").setInstantDeath().setNoKnockback();

    public static final DamageType SWEEP_ATTACK = new DamageType("Sweep Attack", "%Killed% was killed by %Killer%'s sweeping attack")
            .setMelee();

    public static final DamageType THORNS = new DamageType("Thorns", "%Killed% found out how the thorns enchantment works")
            .setNoKnockback();//TODO: construct with entity

    public static final DamageType UNKNOWN = new DamageType("Unknown", "%Killed% died from unknown causes").setNoKnockback();

    public static final DamageType VOID = new DamageType("Void", "%Killed% fell into the void").setIgnoreArmor().setNoKnockback()
            .setIgnoreRate().setDamageSource(DamageSource.OUT_OF_WORLD);

    public static final DamageType VOID_PUSHED = new DamageType("Void Pushed", "%Killed% was knocked into the void by %Killer%")
            .setIgnoreArmor().setNoKnockback().setIgnoreRate().setDamageSource(DamageSource.OUT_OF_WORLD);

    public static final DamageType VOID_SHOT = new DamageType("Void Shot", "%Killed% was shot into the void by %Killer%")
            .setIgnoreArmor().setNoKnockback().setIgnoreRate().setDamageSource(DamageSource.OUT_OF_WORLD);

    public static final DamageType WITHER_POISON = new DamageType("Wither Poison", "%Killed% drank a vial of wither poison")
            .setNoKnockback().setDamageSource(DamageSource.WITHER);

    /******************************************************************************************
     *                                  KIT DAMAGETYPES
     ******************************************************************************************/

    public static final DamageType PYRO_MOLOTOV = new DamageType("Pyro Incendiary", "%Killed% was burned to death by %Killer%'s incendiary")
            .setFire().setIgnoreArmor().setNoKnockback().setDamageSource(DamageSource.ON_FIRE);

	public static final DamageType SNIPER_GRENADE_FAIL = new DamageType("Grenade Fail", "%Killed% forgot they pulled the pin.")
			.setInstantDeath().setNoKnockback().setDamageSource(DamageSource.explosion((net.minecraft.world.entity.LivingEntity) null));

	public static final DamageType SNIPER_HEADSHOT = new DamageType("Headshot", "%Killed% was headshot by %Killer%")
			.setProjectile();

    public static final DamageType DEMO_TNTMINE = new DamageType("Demolitions TNT Mine",
            "%Killed% stepped on %Killer%'s TNT Mine and blew up")
            .setDamageSource(DamageSource.explosion((net.minecraft.world.entity.LivingEntity) null))
            .setIgnoreRate().setExplosion();

	public static final DamageType TOXIC_LEAP = new DamageType("Venom Leap", "%Killed% was killed by %Killer%'s Toxic Leap");

	public static final DamageType DEMO_PUSHMINE = new DamageType("Demolitions Push Mine",
			"%Killed% was somehow directly killed by %Killer%'s Push Mine. (If you see this please tell how you did it" +
					" to a mod. Thanks)").setTrackedDamageType(DamageTimes.TrackedDamageTypes.ATTACK).setIgnoreRate();

	/*******************************************************************************************
	 * 									GAMEMODE DAMAGETYPES
	 ******************************************************************************************/

	public static final DamageType BOMB_EXPLODED = new DamageType("Team Bomb" ).setInstantDeath().setIgnoreRate();

	public static final DamageType END_GAME_LIGHTNING = new DamageType("Herobrine", "%Killed% was killed by Herobrine")
			.setIgnoreArmor().setIgnoreArmorEnchants().setNoKnockback().setIgnoreRate().setDamageSource(DamageSource.LIGHTNING_BOLT);

	private static int idCounter = 0;

    //a constant identifier for same types, to compare for same types across separate instances of this class
    // without evaluating a String
	private final int id;
	private final String[] _deathMessages;
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
    //mainly for getting correct damage sound from nms in EntityUtils.playHurtAnimation
    private DamageSource nmsDamageSource;
	private DamageTimes.TrackedDamageTypes trackedType;

    private DamageType(String name, String... deathMessages) {
		this.id = nextId();
        _name = name;
        _deathMessages = deathMessages;
		applicableEnchantments = new ArrayList<>(2);
		applicableEnchantments.add(Enchantment.PROTECTION_ENVIRONMENTAL);
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
		applicableEnchantments = copyOf.applicableEnchantments;
		trackedType = copyOf.trackedType;
    }

    public DamageType(DamageType copyOf, String... deathMessages) {
        id = copyOf.id;
        _burn = copyOf._burn;
        _deathMessages = deathMessages;
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
			case SONIC_BOOM:
				return getSonicBoom(event);
            default:
                return UNKNOWN;
        }
    }

    public String getRawDeathMessage() {
        return _deathMessages[MathUtils.random.nextInt(_deathMessages.length)];
    }

	@Nullable
    public Component getDeathMessage(@Nullable Entity victim, @Nullable Entity killer, @Nullable Entity cause) {
		if(hasDeathMessages()) {
			return doPlaceholders(getRawDeathMessage(),
					EntityUtils.getComponent(victim),
					EntityUtils.getComponent(killer),
					cause != null ? EntityUtils.getComponent(cause) : Component.empty()
			);
		}

		return null;
    }

	private static final Pattern LEGACY_REGEX = Pattern.compile("%(Kille[dr]|Cause)%");
	private static final TextColor MESSAGE_COLOR = NamedTextColor.YELLOW;
	private static Component doPlaceholders(String message, Component victim, Component killer, Component cause) {
		if (message.contains("%")) {
			// old message format
			return Component.text(message, MESSAGE_COLOR)
					.replaceText(TextReplacementConfig.builder()
							.match(LEGACY_REGEX)
							.replacement((result, builder) -> {
								String arg = result.group(1);
								return switch (arg) {
									case "Killed" -> victim;
									case "Killer" -> killer;
									case "Cause" -> cause;
									default -> throw new Error();
								};
							})
							.build());
		} else {
			return MiniMessage.miniMessage().deserialize(message,
					Placeholder.component("victim", victim),
					Placeholder.component("killer", killer),
					Placeholder.component("cause", cause)
			);
		}
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

	public static DamageType getSonicBoom(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent) {
			return new DamageType(SONIC_BOOM).setDamageSource(
					DamageSource.sonicBoom(((CraftEntity) entityDamageByEntityEvent.getDamager()).getHandle())
			);
		}
		return SONIC_BOOM;
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

    public DamageType setBurn() {
        _burn = true;

		addApplicableEnchant(Enchantment.PROTECTION_FIRE);
		setTrackedDamageType(DamageTimes.TrackedDamageTypes.FIRE);

        return this;
    }

    public DamageType setExplosion() {
        _explosion = true;

		addApplicableEnchant(Enchantment.PROTECTION_EXPLOSIONS);
		setTrackedDamageType(DamageTimes.TrackedDamageTypes.ATTACK);

        return this;
    }

    public DamageType setFall() {
        _fall = true;
        setIgnoreArmor();
        setIgnoreRate();
		addApplicableEnchant(Enchantment.PROTECTION_FALL);
		setTrackedDamageType(DamageTimes.TrackedDamageTypes.OTHER);

        return this;
    }

    public DamageType setFire() {
        setBurn();

        _fire = true;
		removeApplicableEnchant(Enchantment.PROTECTION_ENVIRONMENTAL);
		setTrackedDamageType(DamageTimes.TrackedDamageTypes.FIRE);

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

		setTrackedDamageType(DamageTimes.TrackedDamageTypes.ATTACK);

        return this;
    }

    public DamageType setNoKnockback() {
        _isntKnockback = true;

        return this;
    }

    public DamageType setProjectile() {
        _projectile = true;

		addApplicableEnchant(Enchantment.PROTECTION_PROJECTILE);
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
        return this.nmsDamageSource != null ? this.nmsDamageSource : DamageSource.GENERIC;
    }

    public String toString() {
        return "DamageType[" + getName() + "]";
    }

    public static void checkDamageTypes() {
        //Entity for the sake of constructing damage events
        Pig entity = Main.getGame().getWorld().spawn(Main.getGame().getWorld().getSpawnLocation(), Pig.class);

        for (EntityDamageEvent.DamageCause cause : EntityDamageEvent.DamageCause.values()) {
			EntityDamageEvent event = new EntityDamageEvent(entity, cause, 1);
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
}
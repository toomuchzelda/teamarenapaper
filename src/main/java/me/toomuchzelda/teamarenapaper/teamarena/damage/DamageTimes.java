package me.toomuchzelda.teamarenapaper.teamarena.damage;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * class for recording when damage of different types has been taken by LivingEntity
 *
 * There is an individual 'ticker' or 'tracker' for:
 * - Direct player attacks like melee and projectiles
 * - Fire
 * - Poison
 * - Everything else
 *
 * @author toomuchzelda
 */
public class DamageTimes {

	private static final Map<LivingEntity, DamageTime[]> ENTITY_DAMAGE_TIMES = new LinkedHashMap<>();

	//supposedly this array is a new allocation on every .values() call, so cache here
	private static final TrackedDamageTypes[] TRACKED_DAMAGE_TYPES_ARR = TrackedDamageTypes.values();

	private static DamageTime[] newTimesArray(LivingEntity victim) {
		DamageTime[] arr = new DamageTime[TRACKED_DAMAGE_TYPES_ARR.length];

		for(TrackedDamageTypes type : TRACKED_DAMAGE_TYPES_ARR) {
			arr[type.ordinal()] = new DamageTime();
		}

		return arr;
	}

	public static DamageTime getDamageTime(LivingEntity victim, TrackedDamageTypes type) {
		DamageTime[] arr = ENTITY_DAMAGE_TIMES.computeIfAbsent(victim, living -> newTimesArray(victim));
		return arr[type.ordinal()];
	}

	/**
	 * Get most recently occuring DamageTime thing
	 */
	public static DamageTime getLastDamageTime(LivingEntity victim) {
		DamageTime[] arr = ENTITY_DAMAGE_TIMES.computeIfAbsent(victim, living -> newTimesArray(victim));

		DamageTime mostRecent = arr[0];
		for(DamageTime time : arr) {
			if(time.getLastTimeDamaged() > mostRecent.getLastTimeDamaged()) {
				mostRecent = time;
			}
		}

		return mostRecent;
	}

	public static void clearDamageTimes(LivingEntity victim) {
		DamageTime[] arr = ENTITY_DAMAGE_TIMES.get(victim);
		if(arr != null) {
			for (DamageTime time : arr) {
				time.clear();
			}
		}
	}

	public static void clear() {
		ENTITY_DAMAGE_TIMES.clear();
	}

	public static Iterator<Map.Entry<LivingEntity, DamageTime[]>> getIterator() {
		return ENTITY_DAMAGE_TIMES.entrySet().iterator();
	}

	public static class DamageTime
	{
		private Entity giver;
		/**
		 * for status effect aka FIRE and POISON. for ATTACK and OTHER this is ignored
		 * record the time the initial fire was given, so that if their fire is 're-given' meaning
		 * the time given is reset, it won't interrupt their current fire damage rythm
		 * eg if we did:
		 *
		 * if((TeamArena.getGameTick() - lastTimeDamaged) % 20 == 0) {
		 * 		hurtPlayer();
		 * }
		 *
		 * Then when lastTimeDamaged is reset, say by being attacked by another player's fire aspect sword,
		 * if they were on already fire they would have to wait another second before being hurt again, interrupting
		 * their fire damage rhythm. So use variable timeGiven instead.
		 *
		 * Set to -1 when they are not on fire, so the rhythm doesn't carry over in between set-on-fire events
		 */
		private int timeGiven;
		private int lastTimeDamaged;
		private double damage;
		private DamageType damageType;

		private DamageTime() {
			this.giver = null;
			this.lastTimeDamaged = 0;
			this.damage = 0;
			this.damageType = null;
		}

		public DamageType getDamageType() {
			return damageType;
		}

		public int getTimeGiven() {
			return timeGiven;
		}

		public void setTimeGiven(int timeGiven) {
			this.timeGiven = timeGiven;
		}

		/**
		 * The time (tick) this damage was dealt to the livingentity
		 */
		public int getLastTimeDamaged() {
			return lastTimeDamaged;
		}

		public double getDamage() {
			return damage;
		}

		public Entity getGiver() {
			return giver;
		}

		public void extinguish() {
			this.giver = null;
			this.timeGiven = -1;
		}

		public void update(Entity giver, int lastTimeDamaged, double damage, DamageType damageType) {
			update(giver, lastTimeDamaged, lastTimeDamaged, damage, damageType);
		}

		public void update(Entity giver, int timeGiven, int lastTimeDamaged, double damage, DamageType damageType) {
			this.giver = giver;
			this.timeGiven = timeGiven;
			this.lastTimeDamaged = lastTimeDamaged;
			this.damage = damage;
			this.damageType = damageType;
		}

		public void clear() {
			this.giver = null;
			this.timeGiven = 0;
			this.lastTimeDamaged = 0;
			this.damage = 0d;
			this.damageType = null;
		}
	}

	//DamageTypes as in type of damage, and not DamageType.class
	public enum TrackedDamageTypes {
		ATTACK,
		FIRE,
		POISON,
		OTHER
	}
}

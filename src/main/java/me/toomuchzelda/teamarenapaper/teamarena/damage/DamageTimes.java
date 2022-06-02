package me.toomuchzelda.teamarenapaper.teamarena.damage;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

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

	public static void setDamageTime(LivingEntity victim, TrackedDamageTypes type, Entity giver,
									 DamageType damageType, int timeStamp, double damage) {
		DamageTime[] arr = ENTITY_DAMAGE_TIMES.computeIfAbsent(victim, living -> newTimesArray(victim));
		DamageTime entry = arr[type.ordinal()];

		entry.update(giver, timeStamp, damage, damageType);
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
			if(time.getTimeGiven() > mostRecent.getTimeGiven()) {
				mostRecent = time;
			}
		}

		return mostRecent;
	}

	public static void clearDamageTimes(LivingEntity victim) {
		DamageTime[] arr = ENTITY_DAMAGE_TIMES.computeIfAbsent(victim, living -> newTimesArray(victim));

		for(DamageTime time : arr) {
			time.clear();
		}
	}

	public static class DamageTime
	{
		private Entity giver;
		private int timeGiven;
		private double damage;
		private DamageType damageType;

		private DamageTime() {
			this.giver = null;
			this.timeGiven = 0;
			this.damage = 0;
			this.damageType = null;
		}

		public DamageType getDamageType() {
			return damageType;
		}

		/**
		 * The time (tick) this damage was dealt to the livingentity
		 */
		public int getTimeGiven() {
			return timeGiven;
		}

		public double getDamage() {
			return damage;
		}

		public Entity getGiver() {
			return giver;
		}

		public void update(Entity giver, int timeGiven, double damage, DamageType damageType) {
			this.giver = giver;
			this.timeGiven = timeGiven;
			this.damage = damage;
			this.damageType = damageType;
		}

		public void clear() {
			this.giver = null;
			this.timeGiven = 0;
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

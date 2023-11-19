package me.toomuchzelda.teamarenapaper.potioneffects;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * This class and package is to allow easy stacking of multiple potion effects atop one another.
 * <p>
 * Effects are organized by types. A single effect exists under a type and string key.
 * Given various levels of potion effect of one type, all lower levels will be paused and the highest
 * level instance(s) will take effect until they tick away.
 * Then the lower ones are run in descending level order.
 *
 * @author toomuchzelda
 */
public class PotionEffectManager
{
	/** Container class */
	private static final class StackedEffect {
		private final LivingEntity entity;
		// Not an effect applied by plugin, but the one applied to the entity and outwardly visible
		private PotionEffect visibleEffect;
		private int visibleEffectApplied; // tick timestamp
		private StatusEffect toTick;
		private final Map<String, StatusEffect> effects;

		private StackedEffect(LivingEntity entity) {
			this.entity = entity;
			this.effects = new HashMap<>();
		}

		/**
		 * Determine the Bukkit PotionEffect that should be applied to the entity.
		 * This is the combined duration of all the effects in the this.effects with the highest and equal effect level.
		 * If none remain then remove the effect. */
		private void recalcVisibleEffect() {
			if (this.effects.size() > 0) {
				PotionEffect newEffect = null;

				StatusEffect highestEffect = null;
				long duration = 0; // Use long to avoid integer overflow
				for (StatusEffect effect : this.effects.values()) {
					if (highestEffect == null || effect.level > highestEffect.level) {
						highestEffect = effect;
						duration = effect.getTicksLeft();
					}
					else if (effect.level == highestEffect.level) {
						duration += effect.getTicksLeft();
						if (effect.getTicksLeft() > highestEffect.getTicksLeft()) {
							highestEffect = effect;
						}
					}
				}

				int iDuration = (int) MathUtils.clamp(0, Integer.MAX_VALUE, duration);

				// Only apply new effect if differs from current visible effect
				if (!highestEffect.compare(this.visibleEffect, this.visibleEffectApplied, TeamArena.getGameTick())) {
					newEffect = highestEffect.toPotionEffect(iDuration);
					//Bukkit.broadcastMessage("compare false for " + highestEffect.type);
				}
				else {
					//Bukkit.broadcastMessage("compare true for " + highestEffect.type);
				}
				this.toTick = highestEffect;

				if (newEffect != null) {
					this.visibleEffect = newEffect;
					blockEvent = true;
					this.entity.removePotionEffect(this.visibleEffect.getType());
					this.entity.addPotionEffect(this.visibleEffect);
					blockEvent = false;
					this.visibleEffectApplied = TeamArena.getGameTick();
				}
			}
			else {
				if (this.visibleEffect != null) {
					blockEvent = true;
					this.entity.removePotionEffect(this.visibleEffect.getType());
					blockEvent = false;
					this.visibleEffect = null;
					this.toTick = null;
				}
				else {
					Main.logger().warning("recalcVisibleEffect called with empty effects list and no visible" +
						"Effect, probably removeEffect() called before addEffect()");
					Thread.dumpStack();
				}
			}
		}

		/* 'tick' the highest StatusEffect instance and remove + recalc if it's done */
		private void tick() {
			if (this.visibleEffect != null) {
				assert this.toTick != null;
				if (!this.toTick.tick()) { // recalc once effect has run out
					this.effects.remove(this.toTick.key);
					//Bukkit.broadcastMessage("Removed " + this.toTick.key);
					this.recalcVisibleEffect();
				}
			}
		}

		private void addEffect(String key, PotionEffect effect) {
			this.effects.put(key, new StatusEffect(key, effect));
			this.recalcVisibleEffect();
		}

		private void removeEffect(String key) {
			if (this.effects.remove(key) != null) {
				this.recalcVisibleEffect();
			}
		}

		private void removeAll() {
			this.effects.clear();
			this.recalcVisibleEffect();
		}

		@Override
		public String toString() {
			return "visibleEffect: " + this.visibleEffect +
				", effects: " + this.effects;
		}
	}

	// prevent stack overflow from called event -> recalcVisibleEffect -> addPotionEffect -> called event
	private static boolean blockEvent = false;

	private static final Map<LivingEntity, Map<PotionEffectType, StackedEffect>> ALL_EFFECTS = new HashMap<>();
	private static final String VANILLA_KEY = "vnll";

	private static Map<PotionEffectType, StackedEffect> getEffects(LivingEntity living) {
		return ALL_EFFECTS.computeIfAbsent(living, livingEntity -> new HashMap<>());
	}

	public static void addEffect(LivingEntity entity, String key, PotionEffect effect) {
		Map<PotionEffectType, StackedEffect> effects = getEffects(entity);
		StackedEffect stackedEffect = effects.computeIfAbsent(effect.getType(), potionEffectType -> new StackedEffect(entity));
		stackedEffect.addEffect(key, effect);
		// Bukkit.broadcastMessage("Added key " + key + " type " + effect.getType());
	}

	public static void removeEffect(LivingEntity entity, PotionEffectType type, String key) {
		Map<PotionEffectType, StackedEffect> effects = ALL_EFFECTS.get(entity);
		if (effects != null) {
			StackedEffect stackedEffect = effects.get(type);
			if (stackedEffect != null) {
				stackedEffect.removeEffect(key);
			}
		}
	}

	public static boolean hasEffect(LivingEntity entity, PotionEffectType type, String key) {
		Map<PotionEffectType, StackedEffect> effects = ALL_EFFECTS.get(entity);
		if (effects == null) return false;

		StackedEffect stackedEffect = effects.get(type);
		if (stackedEffect == null) return false;

		if (stackedEffect.effects.containsKey(key))
			return true;

		return false;
	}

	public static void removeAll(LivingEntity entity) {
		Map<PotionEffectType, StackedEffect> effectsMap = ALL_EFFECTS.remove(entity);
		if (effectsMap != null) {
			for (var stackedEffects : effectsMap.values()) {
				stackedEffects.removeAll();
			}
		}
	}

	public static void onEntityPotionEffect(EntityPotionEffectEvent event) {
		if (blockEvent)
			return;

		blockEvent = true;

		event.setCancelled(true); // Should cancel ALL shenanigans done by the game.

		assert event.getEntity() instanceof LivingEntity;
		final LivingEntity livent = (LivingEntity) event.getEntity();
		final EntityPotionEffectEvent.Action action = event.getAction();

		if (action == EntityPotionEffectEvent.Action.REMOVED || event.getNewEffect() == null) {
			removeEffect(livent, event.getModifiedType(), VANILLA_KEY);
		}
		else if (action == EntityPotionEffectEvent.Action.ADDED || action == EntityPotionEffectEvent.Action.CHANGED) {
			assert event.getNewEffect() != null;
			addEffect(livent, VANILLA_KEY, event.getNewEffect());
		}
		else { // CLEARED
			removeAll(livent);
		}

		blockEvent = false;
	}

	public static void tick() {
		if (TeamArena.getGameTick() % (2 * 20 * 60) == 11) {
			ALL_EFFECTS.entrySet().removeIf(entry -> !entry.getKey().isValid());
		}

		for (var entry : ALL_EFFECTS.entrySet()) {
			//sometimes removed effects don't trigger the event, so detect them here and remove


			entry.getValue().forEach((potionEffectType, stackedEffect) -> {
				stackedEffect.tick();
			});
		}
	}

	// These methods primarily for CommandPotion.java
	public static String getPlayerData(Player player) {
		Map<PotionEffectType, StackedEffect> map = ALL_EFFECTS.get(player);

		if (map == null)
			return "no effects";

		StringBuilder s = new StringBuilder();
		for (var entry : map.entrySet()) {
			s.append(entry.getValue().toString());
			s.append('\n');
		}

		return s.toString();
	}

	public static void debugAddEffect(Player player, String key, PotionEffectType type, int level) {
		PotionEffect effect = new PotionEffect(type, 10 * 20, level);

		addEffect(player, key, effect);
	}
}

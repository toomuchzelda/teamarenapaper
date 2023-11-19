package me.toomuchzelda.teamarenapaper.potioneffects;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class StatusEffect
{
	final String key;
	final PotionEffectType type;
	final int level;
	final boolean ambient;
	final boolean particles;
	final boolean icon;

	private int ticksLeft;

	StatusEffect(String key, PotionEffect effect) {
		this.key = key;
		this.type = effect.getType();
		this.level = effect.getAmplifier();
		this.ambient = effect.isAmbient();
		this.particles = effect.hasParticles();
		this.icon = effect.hasIcon();

		this.ticksLeft = effect.getDuration();
		if (this.ticksLeft == PotionEffect.INFINITE_DURATION) {
			this.ticksLeft = Integer.MAX_VALUE;
		}
	}

	StatusEffect(String key, PotionEffectType type, int level, boolean ambient, boolean particles, boolean icon, int duration) {
		this.key = key;
		this.type = type;
		this.level = level;
		this.ambient = ambient;
		this.particles = particles;
		this.icon = icon;

		this.ticksLeft = duration;
	}

	boolean tick() {
		this.ticksLeft--;

		return this.ticksLeft > 0;
	}

	int getTicksLeft() {
		return this.ticksLeft;
	}

	public boolean compare(PotionEffect effect, int applied, int now) {
		return effect != null &&
			this.type == effect.getType() &&
			this.level == effect.getAmplifier() &&
			this.ambient == effect.isAmbient() &&
			this.particles == effect.hasParticles() &&
			this.icon == effect.hasIcon() &&
			applied + effect.getDuration() == now + this.ticksLeft;
	}

	public PotionEffect toPotionEffect() {
		return new PotionEffect(this.type, this.getTicksLeft(), this.level, this.ambient, this.particles, this.icon);
	}

	public PotionEffect toPotionEffect(int duration) {
		return new PotionEffect(this.type, duration, this.level, this.ambient, this.particles, this.icon);
	}

	@Override
	public String toString() {
		return "{type:" + this.type.getName() + ",level:" + this.level + "}";
	}
}

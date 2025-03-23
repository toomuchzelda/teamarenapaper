package me.toomuchzelda.teamarenapaper.teamarena.abilities.centurion;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public record ShieldConfig(double health, double maxHealth, int duration, @Nullable Location anchor) {
	public static final double DEFAULT_MAX_HEALTH = 100;
	public static final double DEFAULT_HEALTH = DEFAULT_MAX_HEALTH;

	private static final int SHIELD_PERMANENT = -1;
	public static ShieldConfig DEFAULT = new ShieldConfig(DEFAULT_HEALTH, DEFAULT_MAX_HEALTH, SHIELD_PERMANENT, null);

	public ShieldConfig {
		if (maxHealth < health) throw new IllegalArgumentException("maxHealth");
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.abilities;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitPorcupine;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

public class ProjectileReflectEvent {
	public Player reflector;
	public Projectile projectile;
	public ProjectileSource shooter;

	public boolean cancelled = false;

	// To be initialized by the consumer of this event, if needed.
	// Function is responsible for removing the projectile(s) if desired
	public KitPorcupine.CleanupFunc cleanupFunc = null;
	public KitPorcupine.OnHitFunc hitFunc = null;
	public KitPorcupine.OnAttackFunc attackFunc = null;

	public ProjectileReflectEvent(Player reflector, Projectile projectile, ProjectileSource shooter) {
		this.reflector = reflector;
		this.projectile = projectile;
		this.shooter = shooter;
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.abilities;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

import java.util.function.BiConsumer;

public class ProjectileReflectEvent {
	public Player reflector;
	public Projectile projectile;
	public ProjectileSource shooter;

	public boolean cancelled = false;
	public boolean overrideShooter = true;

	// To be initialized by the consumer of this event, if needed.
	public BiConsumer<Player, Projectile> cleanupFunc = null;

	public ProjectileReflectEvent(Player reflector, Projectile projectile, ProjectileSource shooter) {
		this.reflector = reflector;
		this.projectile = projectile;
		this.shooter = shooter;
	}
}

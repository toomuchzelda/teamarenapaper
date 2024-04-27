package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate.CratePayload;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate.FallingCrate;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

/**
 * Class to store data for a killstreak crate.
 * A crate that is summoned with a firework and falls from the sky to a destination and then does its thing upon landing.
 *
 * @author toomuchzelda
 */
public class Crate
{
	// Amount of time after spawning the firework the crate block entity spawns and starts falling
	private static final int CRATE_DELAY_TIME = 50;
	private static final int CRATE_SPAWN_HEIGHT = 88;
	private static final double SLOW_FALL_VELOCITY = -0.25;
	private static final double FALL_VELOCITY = -2;

	public final Player owner;
	private final CratedKillStreak killStreak; // Killstreak given to the player that opens it.
	private final boolean fragile;
	private final Location destination; // Destination it needs to fall to.

	private Firework firework; // The firework used to summon this crate
	private Parrot parrot; // Parrot riding the firework
	private FallingCrate fallingCrate;

	private final int spawnTime;
	private int crateFallTime;

	private boolean done;

	public Crate(Player owner, Location destination, CratedKillStreak cratedKillStreak) {
		this.owner = owner;
		this.destination = destination;

		this.killStreak = cratedKillStreak;
		this.fragile = killStreak.isPayloadFragile(owner, destination);

		this.spawnTime = TeamArena.getGameTick();

		TeamArenaTeam team = Main.getPlayerInfo(owner).team;

		this.firework = owner.getWorld().spawn(this.destination, Firework.class, firework1 -> {
			FireworkMeta meta = firework1.getFireworkMeta();
			meta.clearEffects();
			meta.addEffect(FireworkEffect.builder().trail(true).withColor(team.getColour()).build());
			meta.setPower(127);
			firework1.setFireworkMeta(meta);
		});

		Location lookUp = destination.clone().setDirection(new Vector(0d, 1d, 0d));
		this.parrot = owner.getWorld().spawn(lookUp, Parrot.class, parrot -> {
			parrot.setInvulnerable(true);
			parrot.setAI(false);

			Parrot.Variant[] variants = Parrot.Variant.values();
			parrot.setVariant(variants[MathUtils.random.nextInt(variants.length)]);
		});

		this.firework.addPassenger(parrot);

		done = false;

		Main.getGame().getKillStreakManager().crateFireworks.add(this.firework);

		killStreak.onCratePlace(owner, destination);
	}

	void tick() {
		final int currentTick = TeamArena.getGameTick();

		final int diff = currentTick - this.spawnTime;
		if(diff >= CRATE_DELAY_TIME) {
			if(this.fallingCrate == null) {
				this.parrot.setHealth(0);
				this.firework.detonate();

				killStreak.onFireworkFinish(owner, destination, this);

				this.parrot = null;
				this.firework = null;

				if(!this.isDone()) { // May be marked done by the above event call
					CratePayload payload = killStreak.getPayload(owner, destination);

					this.fallingCrate = new FallingCrate(this.destination.clone().add(0, CRATE_SPAWN_HEIGHT, 0),
						Main.getPlayerInfo(owner).team.getDyeColour(), payload);

					this.fallingCrate.spawn();

					crateFallTime = currentTick;
				}
			}

			if(!this.isDone()) {
				if (fallingCrate.getY() < destination.getY() - 0.5) {
					fallingCrate.despawn();
					fallingCrate = null;

					killStreak.onCrateLand(owner, destination);

					done = true;
				} else {
					Vector velocity;
					if (fragile && fallingCrate.getY() < destination.getY() + 16) {
						velocity = new Vector(0, SLOW_FALL_VELOCITY, 0);
						fallingCrate.spawnParachute();
					} else {
						velocity = new Vector(0, FALL_VELOCITY, 0); // fall faster
					}

					fallingCrate.move(velocity);
					killStreak.onCrateTick(owner, destination, currentTick - crateFallTime);
				}
			}
		}
	}

	void remove() {
		this.killStreak.onCrateRemove(this);
		if (this.parrot != null)
			this.parrot.remove();

		if (this.firework != null)
			this.firework.remove();

		if (this.fallingCrate != null)
			this.fallingCrate.despawn();
	}

	boolean isDone() {
		return done;
	}
}

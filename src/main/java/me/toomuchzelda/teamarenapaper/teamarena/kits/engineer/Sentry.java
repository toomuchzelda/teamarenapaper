package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.building.Building;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;

public class Sentry extends Building {
	enum State {
		STARTUP,
		NEUTRAL,
		LOCKED,
		WRANGLED
	}

	State currState;
	float initYaw;
	Mob sentry;
	int initTick;
	int creationTick;
	ItemStack[] armor;
	public static final int SENTRY_LIFETIME = KitEngineer.EngineerAbility.SENTRY_CD;
	public static final int SENTRY_STARTUP_TIME = 40;
	public static final int SENTRY_CYCLE_TIME = 120;
	public static final int SENTRY_SIGHT_RANGE = 15;
	//degree rotation = how much the sentry will rotate
	//yaw and pitch view = the sentry's cone of vision
	public static final int SENTRY_DEGREE_ROTATION = 90;
	public static final double SENTRY_YAW_VIEW = 15.0;
	public static final double SENTRY_PITCH_VIEW = 70.0;
	//Fire every SENTRY_FIRE_RATE ticks
	public static final int SENTRY_FIRE_RATE = 12;

	private static final ItemStack ICON = new ItemStack(Material.BOW);

	public Sentry(Player player, Mob sentry) {
		super(player, sentry.getLocation());
		setName("Sentry");
		setIcon(ICON);
		this.currState = State.STARTUP;
		this.sentry = sentry;
		this.initYaw = this.location.getYaw();
		this.initTick = TeamArena.getGameTick();
		this.creationTick = TeamArena.getGameTick() + SENTRY_STARTUP_TIME;

		var playerTeam = Main.getPlayerInfo(player).team;
		Color teamColor = playerTeam.getColour();
		this.setText(playerTeam.colourWord(player.getName() + "'s Sentry"));

		//Changing properties from Projection state to Active state

		sentry.setInvisible(false);
		sentry.setInvulnerable(false);
		sentry.setCollidable(true);
		sentry.setSilent(false);
		this.armor = new ItemStack[4];
		armor[3] = new ItemStack(Material.LEATHER_HELMET);
		armor[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
		armor[1] = new ItemStack(Material.LEATHER_LEGGINGS);
		armor[0] = new ItemStack(Material.LEATHER_BOOTS);
		ItemUtils.colourLeatherArmor(teamColor, armor[3]);
		ItemUtils.colourLeatherArmor(teamColor, armor[2]);
		ItemUtils.colourLeatherArmor(teamColor, armor[1]);
		ItemUtils.colourLeatherArmor(teamColor, armor[0]);

		ItemStack sentryBow = new ItemStack(Material.BOW);
		sentry.getEquipment().setItemInMainHand(sentryBow, true);

		//player.getWorld().playSound(sentry, Sound.BLOCK_ANVIL_USE, 1.0f, 0.8f);
	}

	@Override
	protected Location getHologramLocation() {
		return getLocation().add(0, 2, 0);
	}

	public boolean isExpired() {
		return TeamArena.getGameTick() - creationTick > SENTRY_LIFETIME;
	}

	public boolean sentryCanSee(LivingEntity sentry, Player target) {
		Location sentryLoc = sentry.getEyeLocation();
		Vector sentryToTarget = target.getEyeLocation().clone().subtract(sentry.getEyeLocation()).clone().toVector();
		double distance = sentryToTarget.length();
		sentryToTarget.normalize();
		Location relAngle = sentry.getEyeLocation().clone();
		relAngle.setDirection(sentryToTarget);

		double yawAngle = relAngle.getYaw();
		double currYaw = sentry.getEyeLocation().getYaw();
		double pitchAngle = relAngle.getPitch();
		double currPitch = sentry.getEyeLocation().getPitch();

		RayTraceResult rayTrace = sentry.getWorld().rayTraceBlocks(sentryLoc, sentryToTarget, distance,
				FluidCollisionMode.NEVER, true);

		return (Math.abs(yawAngle - currYaw) <= SENTRY_YAW_VIEW &&
				Math.abs(pitchAngle - currPitch) <= SENTRY_PITCH_VIEW) &&
				rayTrace == null;
	}

	//Handling idle rotation of Sentry in NEUTRAL state
	public void setIdleRotation() {
		//Elapsed Tick defines the tick within the whole rotation cycle
		//Cycle Tick defines the tick for a stage in the whole rotation cycle
		int elapsedTick = (TeamArena.getGameTick() - creationTick) % SENTRY_CYCLE_TIME;
		int cycleTick = (int) ((TeamArena.getGameTick() - creationTick) % (SENTRY_CYCLE_TIME / 6.0));
		double degInc = ((SENTRY_DEGREE_ROTATION / 2.0) / (SENTRY_CYCLE_TIME / 6.0));
		Location sentryLoc = sentry.getLocation().clone();
		//Rotate from center to left side
		if (elapsedTick < SENTRY_CYCLE_TIME / 6) {
			sentryLoc.setYaw((float) (initYaw - cycleTick * degInc));
		}
		//Pause at Left Side
		else if (elapsedTick < 2 * SENTRY_CYCLE_TIME / 6) {

		}
		//Rotate from Left to Center
		else if (elapsedTick < 3 * SENTRY_CYCLE_TIME / 6) {
			sentryLoc.setYaw((float) (initYaw - (SENTRY_CYCLE_TIME / 6 * degInc)
					+ (cycleTick * degInc)));
		}
		//Rotate from Center to Right
		else if (elapsedTick < 4 * SENTRY_CYCLE_TIME / 6) {
			sentryLoc.setYaw((float) (initYaw + (cycleTick * degInc)));
		}
		//Pause at Right Side
		else if (elapsedTick < 5 * SENTRY_CYCLE_TIME / 6) {

		}
		//Return back to center position
		else {
			sentryLoc.setYaw((float) (initYaw + (SENTRY_CYCLE_TIME / 6 * degInc)
					- (cycleTick * degInc)));
		}

		//Handling Pitch, reverting to pitch = 0 when returning to neutral state after locking on
		if (sentryLoc.getPitch() != 0) {
			sentryLoc.setPitch((float) Math.max(0, sentryLoc.getPitch() - 10.0));
		}
		sentry.teleport(sentryLoc);
	}

	//Checks if the sentry's current yaw is within the sentry's idle potential idle rotation angles
	public boolean isIdle() {
		double degInc = ((SENTRY_DEGREE_ROTATION / 2.0) / (SENTRY_CYCLE_TIME / 6.0));
		return Math.abs(sentry.getLocation().getYaw() - initYaw) <= SENTRY_CYCLE_TIME / 6.0 * degInc;
	}

	//Checks within sight range for possible targets, then locks on the closest target
	public void findTarget() {
		TeamArenaTeam ownerTeam = Main.getPlayerInfo(owner).team;
		Location sentryLoc = sentry.getLocation().clone();
		Collection<Player> nearbyTargets = sentryLoc.getNearbyPlayers(SENTRY_SIGHT_RANGE);
		Player nearestPlayer = nearbyTargets.stream()
				//Always ignore teammates and players who are invisible or are obstructed from view
				.filter(player -> !player.isInvisible()
						&& sentryCanSee(sentry, player)
						&& Main.getGame().canAttack(player, owner))
				//Ignore spies who are disguised as allies
				.filter(enemy -> !PlayerUtils.isDisguisedAsAlly(owner, enemy))
				.reduce(null, (currClosest, currPlayer) -> {
					if (currClosest == null) {
						return currPlayer;
					}

					if (sentryLoc.distanceSquared(currPlayer.getLocation()) <
							sentryLoc.distanceSquared(currClosest.getLocation())) {
						//If currPlayer is closer than currMax, it is the new closest
						return currPlayer;
					} else {
						return currClosest;
					}
				});

		if (nearestPlayer != null) {
			if (sentry.getTarget() == null) {
				//Only play Lock-on Sound once for a given target
				//sentry.getWorld().playSound(sentry, Sound.BLOCK_LANTERN_BREAK, 1.0f, 1.8f);
			}
			//Success: Player is found, so lock on
			sentry.setTarget(nearestPlayer);
			this.currState = State.LOCKED;
		} else {
			//Failure: Player not found, return/stay in neutral state
			sentry.setTarget(null);
			this.currState = State.NEUTRAL;
		}
	}

	//Makes sentry look at its current target
	public void lockOn() {
		if (sentry.getTarget() != null) {
			Mob sentryCasted = sentry;
			Player target = (Player) sentryCasted.getTarget();
			Location sentryLoc = sentry.getLocation();
			Vector diff = target.getEyeLocation().toVector().subtract(sentryCasted.getEyeLocation().toVector());
			diff.normalize();
			sentryLoc.setDirection(diff);
			sentry.teleport(sentryLoc);
		}
	}

	//Animation during STARTUP
	public void construction() {
		int elapsedTick = TeamArena.getGameTick() - this.initTick;
		long percCD = Math.round(100 * (double) (elapsedTick) / SENTRY_STARTUP_TIME);
		EntityEquipment equipment = sentry.getEquipment();
		this.setText(Component.text("Building... " + percCD + "%"));

		if (elapsedTick < SENTRY_STARTUP_TIME / 4.0) {
			if (equipment.getBoots().getType() == Material.AIR) {
				equipment.setBoots(this.armor[0]);
			}
		} else if (elapsedTick < 2.0 * SENTRY_STARTUP_TIME / 4.0) {
			if (equipment.getLeggings().getType() == Material.AIR) {
				equipment.setLeggings(this.armor[1]);
			}
		} else if (elapsedTick < 3.0 * SENTRY_STARTUP_TIME / 4.0) {
			if (equipment.getChestplate().getType() == Material.AIR) {
				equipment.setChestplate(this.armor[2]);
			}
		} else if (elapsedTick < 4.0 * SENTRY_STARTUP_TIME / 4.0) {
			if (equipment.getHelmet().getType() == Material.AIR) {
				equipment.setHelmet(this.armor[3]);
			}
		} else {
			this.currState = State.NEUTRAL;
			var ownerTeam = Main.getPlayerInfo(owner).team;
			this.setText(ownerTeam.colourWord(owner.getName() + "'s Sentry"));
		}
	}

	public void shoot(Vector direction) {
		AbstractArrow sentryFire = sentry.launchProjectile(Arrow.class);
		sentryFire.setVelocity(direction.multiply(2.5));
		sentryFire.setDamage(1.0);
		sentryFire.setGravity(false);
		sentryFire.setKnockbackStrength(0);
		sentryFire.setShooter(owner);
		sentryFire.setCritical(false);
		sentryFire.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
	}

	public void forceTarget(Location location) {
		//sentryLoc is for calculating the beam, sentryPosLoc is for altering pitch + yaw of sentry itself
		Location sentryLoc = sentry.getEyeLocation();
		Location sentryPosLoc = sentry.getLocation();
		Vector sentryToTermDir = location.clone().subtract(sentryLoc).toVector().normalize();
		sentryPosLoc.setDirection(sentryToTermDir);
		sentry.teleport(sentryPosLoc);

		var data = new Particle.DustOptions(Main.getPlayerInfo(owner).team.getColour(), 0.7f);
		//Creating particle beam
		Vector inc = sentryToTermDir.multiply(0.5);

		for (int i = 0, length = (int) (sentryLoc.distance(location)) * 2; i < length; i++) {
			sentryLoc.add(inc);
			sentry.getWorld().spawnParticle(Particle.REDSTONE, sentryLoc, 1, 0, 0, 0, data);
		}
	}

	@Override
	public void onTick() {
		if (sentry.isDead() || isExpired()) {
			markInvalid();
			return;
		}

		if (this.currState == State.STARTUP) {
			construction();
		} else if (this.currState == State.NEUTRAL) {
			//Handles idle rotation animation of sentry
			setIdleRotation();
			//Only find target when sentry is in its normal neutral state
			if (isIdle()) {
				findTarget();
			}
		} else if (this.currState == State.LOCKED) {
			//Sentry periodically "refreshes" to find the closest target
			if (TeamArena.getGameTick() % 12 == 0) {
				findTarget();
			}
			//Sentry remains locked on until the target becomes obstructed from view OR target dies
			if ((sentry.getTarget() != null && sentryCanSee(sentry, (Player) sentry.getTarget()))
					&& Main.getPlayerInfo((Player) sentry.getTarget()).activeKit != null) {
				if (TeamArena.getGameTick() % SENTRY_FIRE_RATE == 0) {
					Vector direction = sentry.getTarget().getLocation().subtract(sentry.getLocation()).toVector().normalize();
					shoot(direction);
				}
				lockOn();
			}
			//View is now obstructed, return to NEUTRAL state
			else {
				sentry.setTarget(null);
				this.currState = State.NEUTRAL;
			}
		} else if (this.currState == State.WRANGLED) {
			if (!PlayerUtils.isHolding(owner, KitEngineer.WRANGLER) || owner.hasCooldown(Material.STICK)) {
				this.currState = State.NEUTRAL;
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sentry.remove();
	}
}

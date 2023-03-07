package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.building.EntityBuilding;
import me.toomuchzelda.teamarenapaper.teamarena.building.PreviewableBuilding;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class Sentry extends EntityBuilding implements PreviewableBuilding {
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
	public static final int SENTRY_SIGHT_RANGE = 20;
	//degree rotation = how much the sentry will rotate
	//yaw and pitch view = the sentry's cone of vision
	public static final int SENTRY_DEGREE_ROTATION = 90;
	public static final double SENTRY_YAW_VIEW = 15.0;
	public static final double SENTRY_PITCH_VIEW = 70.0;
	//Fire every SENTRY_FIRE_RATE ticks
	public static final int SENTRY_FIRE_RATE = 12;

	private static final ItemStack ICON = new ItemStack(Material.BOW);

	public Sentry(Player player, Location sentryLocation) {
		super(player, sentryLocation);
		setName("Sentry");
		setIcon(ICON);
		this.currState = State.STARTUP;
		this.initYaw = this.location.getYaw();
		this.creationTick = TeamArena.getGameTick() + SENTRY_STARTUP_TIME;

		var playerTeam = Main.getPlayerInfo(player).team;
		Color teamColor = playerTeam.getColour();
		// Set the skeleton's custom name instead.
		//this.setText(playerTeam.colourWord(player.getName() + "'s Sentry"));

		//Changing properties from Projection state to Active state


		this.armor = new ItemStack[4];
		armor[3] = new ItemStack(Material.LEATHER_HELMET);
		armor[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
		armor[1] = new ItemStack(Material.IRON_LEGGINGS);
		armor[0] = new ItemStack(Material.IRON_BOOTS);
		ItemUtils.colourLeatherArmor(teamColor, armor[3]);
		ItemUtils.colourLeatherArmor(teamColor, armor[2]);
	}

	@Override
	public @NotNull Collection<? extends Entity> getEntities() {
		return List.of(sentry);
	}

	@Override
	public @NotNull Collection<? extends PacketEntity> getPacketEntities() {
		return List.of();
	}

	@Override
	protected Location getHologramLocation() {
		return getLocation().add(0, 2, 0);
	}

	public boolean isExpired() {
		return TeamArena.getGameTick() - initTick > SENTRY_LIFETIME;
	}

	public boolean sentryCanSee(LivingEntity sentry, Player target) {
		Location sentryLoc = sentry.getEyeLocation();
		Vector sentryToTarget = target.getEyeLocation().clone().subtract(sentry.getEyeLocation()).clone().toVector();
		double distance = sentryToTarget.length();
		sentryToTarget.normalize();
		Location relAngle = sentry.getEyeLocation().clone();
		relAngle.setDirection(sentryToTarget);

		double yawAngle = Location.normalizeYaw(relAngle.getYaw());
		double currYaw = Location.normalizeYaw(sentryLoc.getYaw());
		double pitchAngle = Location.normalizePitch(relAngle.getPitch());
		double currPitch = Location.normalizePitch(sentryLoc.getPitch());

		RayTraceResult rayTrace = sentry.getWorld().rayTraceBlocks(sentryLoc, sentryToTarget, distance,
				FluidCollisionMode.NEVER, true);

		//Bukkit.broadcast(Component.text("YAW DIFF: " + Math.abs(yawAngle - currYaw)));
		//Bukkit.broadcast(Component.text("PITCH DIFF: " + Math.abs(pitchAngle - currPitch)));

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
		//Return to center position
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
		Location sentryLoc = sentry.getLocation().clone();
		Collection<Player> nearbyTargets = sentryLoc.getNearbyPlayers(SENTRY_SIGHT_RANGE);
		Player nearestPlayer = nearbyTargets.stream()
				//Always ignore teammates and players who are invisible or are obstructed from view
				.filter(player -> !player.isInvisible()
						&& sentryCanSee(sentry, player)
						&& Main.getGame().canAttack(player, owner)
						&& sentry.getLocation().distanceSquared(player.getLocation())
						<= Math.pow(SENTRY_SIGHT_RANGE, 2))
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

		//Bukkit.broadcast(Component.text("nearest player: " + nearestPlayer));

		if (nearestPlayer != null) {
			//if (sentry.getTarget() == null) {
			//Only play Lock-on Sound once for a given target
			//sentry.getWorld().playSound(sentry, Sound.BLOCK_LANTERN_BREAK, 1.0f, 1.8f);
			//}
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
			Location sentryLoc = sentry.getLocation().clone();
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
		sentry.launchProjectile(Arrow.class, direction.multiply(4.1), arrow -> {
			arrow.setDamage(1.0);
			arrow.setShooter(owner);
			arrow.setCritical(false);
			arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		});

		sentry.getWorld().playSound(sentry, Sound.ENTITY_FISHING_BOBBER_THROW, 1.5f, 0.8f);
	}

	//Manual fire which is available when sentry is WRANGLED
	public void forceFire() {
		Vector direction = owner.getLocation().getDirection();
		shoot(direction);
	}

	//Fires a particle beam to show direction of fire when sentry is WRANGLED
	public void wranglerParticleBeam(){
		Vector direction = owner.getLocation().getDirection();
		Location sentryLoc = sentry.getEyeLocation();
		var data = new Particle.DustOptions(Main.getPlayerInfo(owner).team.getColour(), 0.7f);
		//Creating particle beam
		Vector inc = direction.multiply(0.5);
		for (int i = 0; i < 6; i++) {
			sentryLoc.add(inc);
			sentry.getWorld().spawnParticle(Particle.REDSTONE, sentryLoc, 1, 0, 0, 0, data);
		}
	}

	@Override
	public void onPlace() {
		super.onPlace();
		this.sentry = location.getWorld().spawn(location, Skeleton.class, skeleton -> {
			skeleton.setAI(false);
			skeleton.setRemoveWhenFarAway(false);
			skeleton.setShouldBurnInDay(false);
			skeleton.getEquipment().clear();
			skeleton.setCanPickupItems(false);
			skeleton.setInvisible(false);
			skeleton.setCollidable(true);
			skeleton.setSilent(false);
			skeleton.customName(owner.playerListName().append(Component.text("'s Sentry", owner.playerListName().style())));
			ItemStack sentryBow = new ItemStack(Material.BOW);
			skeleton.getEquipment().setItemInMainHand(sentryBow, true);
		});
		this.currState = State.STARTUP;
		this.initTick = TeamArena.getGameTick();
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
			wranglerParticleBeam();
			Location ownerLoc = owner.getLocation();
			sentry.setRotation(ownerLoc.getYaw(), ownerLoc.getPitch());
			//If engineer leaves mount, return to NEUTRAL
			if(sentry.getPassengers().isEmpty()){
				this.currState = State.NEUTRAL;
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sentry.remove();
	}

	@Override
	public boolean onDamage(DamageEvent e) {
		if (e.getAttacker() instanceof Player attacker && !Main.getGame().canAttack(attacker, owner))
			e.setCancelled(true); // ally damage
		return false; // continue handling
	}

	@Override
	public void onInteract(PlayerInteractEntityEvent e) {
		Player rider = e.getPlayer();
		if (owner.equals(rider) && currState != Sentry.State.STARTUP) {
			sentry.addPassenger(rider);
			currState = State.WRANGLED;
		}
	}


	public static final int SENTRY_PLACEMENT_RANGE = 3;
	@Override
	public @Nullable PreviewResult doRayTrace() {
		Location eyeLocation = owner.getEyeLocation().clone().add(0, -0.8, 0);
		Vector direction = eyeLocation.getDirection();
		var result = owner.getWorld().rayTraceBlocks(eyeLocation, direction, SENTRY_PLACEMENT_RANGE,
			FluidCollisionMode.NEVER, true);
		Location blockLoc;
		if (result != null) {
			BlockFace face = result.getHitBlockFace();
			blockLoc = result.getHitPosition().toLocation(owner.getWorld());
			blockLoc.setYaw(eyeLocation.getYaw());
			blockLoc.setPitch(eyeLocation.getPitch());
			if (face != null && face != BlockFace.UP) {
				blockLoc.add(face.getModX() * 0.5, face.getModY() * 2, face.getModZ() * 0.5);
			}
		} else {
			blockLoc = eyeLocation.add(direction.multiply(SENTRY_PLACEMENT_RANGE));
		}
		var gravityResult = owner.getWorld().rayTraceBlocks(blockLoc, new Vector(0, -1, 0), 1);
		if (gravityResult != null) {
			blockLoc.setY(gravityResult.getHitPosition().getY());
			return PreviewResult.validate(blockLoc, Sentry::isValidLocation);
		} else {
			return PreviewResult.deny(blockLoc);
		}
	}

	private static boolean isValidLocation(Location location) {
		BoundingBox aabb = BoundingBox.of(location.clone().add(0, 1.05, 0), 0.3, 1, 0.3);
		return !location.getWorld().hasCollisionsIn(aabb);
	}

	private static PacketEntity PREVIEW;
	@Override
	public @Nullable PacketEntity getPreviewEntity(Location location) {
		if (PREVIEW == null)
			PREVIEW = new PacketEntity(PacketEntity.NEW_ID, EntityType.SKELETON, location, List.of(), null);
		return PREVIEW;
	}

	@Override
	public void setLocation(Location newLoc) {
		super.setLocation(newLoc);
		if (sentry != null)
			sentry.teleport(newLoc);
	}
}

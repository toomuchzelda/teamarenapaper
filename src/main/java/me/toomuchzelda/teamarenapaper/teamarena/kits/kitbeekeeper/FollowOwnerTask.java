package me.toomuchzelda.teamarenapaper.teamarena.kits.kitbeekeeper;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftBee;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class FollowOwnerTask extends BeeTask
{
	public FollowOwnerTask(Bee beeEntity, int spawnTime, int beeNum, Player owner) {
		super(beeEntity, new FollowOwnerGoal(owner, beeEntity, spawnTime, beeNum));
	}

	/**
	 * Method to calculate position of bee for initial spawn and during FOLLOW_OWNER task
	 *
	 * @param beeOwner Boundingbox to surround
	 * @param beeNum   index of bee out of all the bees (0, 1, 2 etc)
	 * @return Position as Location
	 */
	static Location calculateBeeLocation(Entity beeOwner, int beeNum, int spawnTime) {
		//yaw : -180 to 180
		float offset = (float) (TeamArena.getGameTick() - spawnTime);
		offset *= 2;
		offset %= 360f;
		offset -= 180f; // get it between -180 and 180

		float part = (float) beeNum / (float) KitBeekeeper.BeekeeperAbility.MAX_BEES;
		offset += part * 360;

		Location loc = beeOwner.getLocation().add(0, beeOwner.getHeight(), 0);
		//loc.setPitch(0f);
		loc.setYaw(offset);
		Vector direction = loc.getDirection();
		direction.setY(-direction.getY());
		direction.multiply(beeOwner.getWidth() * 1.41);

		loc.add(direction);

		if (!loc.getBlock().getType().isAir() || !loc.getBlock().getRelative(BlockFace.UP).getType().isAir()) {
			loc = beeOwner.getLocation();
		}

		return loc; // TODO improve positioning for visual effect
	}

	private static class FollowOwnerGoal implements Goal<Bee>
	{
		private static final EnumSet<GoalType> GOAL_TYPES = EnumSet.of(GoalType.MOVE);
		private static final GoalKey<Bee> KEY = GoalKey.of(Bee.class, new NamespacedKey(Main.getPlugin(), "beekeeper_follow_owner"));;
		private static final double SPEED_UP_DIST_SQR = 15 * 15;
		private static final double TELEPORT_DIST_SQR = 30 * 30;
		private static final double SPEEDUP_MULT = 0.25d;
		private static final int SPEEDUP_COOLDOWN = 20;

		private final Player owner;
		private final Bee bee;
		private final int spawnTime;
		private final int beeNum;

		private int lastAttackTime;
		private int lastSpeedPushTime;

		private static final int ATTACK_SPEED = 20; // once every second.

		private FollowOwnerGoal(Player owner, Bee bee, int spawnTime, int beeNum) {
			this.owner = owner;
			this.bee = bee;
			this.spawnTime = spawnTime;
			this.beeNum = beeNum;

			this.lastAttackTime = 0;
			this.lastSpeedPushTime = 0;
		}

		@Override
		public boolean shouldActivate() {
			return true; // Should be active for lifetime of instance. Will be removed from Bee when no longer needed.
		}

		@Override
		public boolean shouldStayActive() {
			return this.shouldActivate();
		}

		@Override
		public void start() {
			moveToPosition();
		}

		@Override
		public void stop() {
			this.bee.getPathfinder().stopPathfinding();
		}

		@Override
		public void tick() {
			final int currentTick = TeamArena.getGameTick();
			if (currentTick - lastAttackTime >= ATTACK_SPEED) {
				final TeamArena game = Main.getGame();
				for (Player potentialVictim : game.getPlayers()) {
					// Attack enemies that are not invisible kits if their boundingbox is touching the bee.
					if (game.canAttack(this.owner, potentialVictim) && !Kit.getActiveKit(potentialVictim).isInvisKit()) {
						if (potentialVictim.getBoundingBox().overlaps(this.bee.getBoundingBox())) {
							lastAttackTime = currentTick;
							this.bee.attack(potentialVictim);
							this.bee.setHasStung(false);

							// Play rolling animation
							this.bee.setRollingOverride(TriState.TRUE);
							Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
								this.bee.setRollingOverride(TriState.NOT_SET);
							}, 7);
						}
					}
				}
			}

			moveToPosition();
		}

		private void moveToPosition() {
			final Location destination = calculateBeeLocation(owner, this.beeNum, this.spawnTime);
			final Location beeLoc = this.bee.getLocation();
			final double distSqr = beeLoc.distanceSquared(this.owner.getLocation());
			// If too far teleport the bee or speed them up
			if (distSqr >= TELEPORT_DIST_SQR) {
				this.bee.teleport(destination);
			}
			else {
				this.bee.getPathfinder().moveTo(destination, 1);

				final int currentTick = TeamArena.getGameTick();
				if (currentTick - this.lastSpeedPushTime >= SPEEDUP_COOLDOWN) {
					if (distSqr >= SPEED_UP_DIST_SQR) { // give a small speedup
						// speed = 2; I've tried upping the speed argument on pathfinder.moveTo() but it doesn't seem
						// to do anything for the bees so i'll try to do this manually.

						// Get the next point in their pathfinder path and move them to it instantly.
						Pathfinder.PathResult currentPath = this.bee.getPathfinder().getCurrentPath();
						if (currentPath != null) {
							Location nextStep = currentPath.getNextPoint();
							if (nextStep != null) {
								this.bee.teleport(nextStep); // TODO test
								Vector vec = nextStep.subtract(beeLoc).toVector();
								vec.multiply(SPEEDUP_MULT);
								this.bee.setVelocity(vec);

								this.lastSpeedPushTime = currentTick;
							}
						}
					}
				}
			}
		}

		@Override
		public @NotNull GoalKey<Bee> getKey() {
			return KEY;
		}

		@Override
		public @NotNull EnumSet<GoalType> getTypes() {
			return GOAL_TYPES;
		}
	}
}

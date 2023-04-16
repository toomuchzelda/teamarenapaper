package me.toomuchzelda.teamarenapaper.teamarena.kits.kitbeekeeper;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftBee;
import org.bukkit.entity.Bee;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class DeliverHoneyTask extends BeeTask
{
	private static final Component ACTIONBAR_HEAL = Component.text("Heal", NamedTextColor.LIGHT_PURPLE);

	public DeliverHoneyTask(Bee beeEntity, LivingEntity target) {
		super(beeEntity, new DeliverHoneyGoal(beeEntity, target));
	}

	@Override
	Component getActionBarPart() {
		return ACTIONBAR_HEAL;
	}

	static class DeliverHoneyGoal implements Goal<Bee> {
		private static final EnumSet<GoalType> GOAL_TYPES = EnumSet.of(GoalType.MOVE);
		private static final GoalKey<Bee> KEY = GoalKey.of(Bee.class, new NamespacedKey(Main.getPlugin(), "beekeeper_deliver_honey"));;

		private static final float DELIVER_FLY_SPEED = 0.02f;
		// Max distance from the target the bee can be to complete its delivery.
		private static final double DIST_SQR_TO_TARGET = 0.9d * 0.9d;
		private static final int WAIT_AFTER_DONE_TIME = 3 * 20;
		private static final int NOT_DONE = -1;

		private final Bee bee;
		private final LivingEntity target;
		private final float originalFlyingSpeed;
		private int doneTime; // Wait 2 seconds after delivering before stopping this Goal.

		public DeliverHoneyGoal(Bee bee, LivingEntity target) {
			this.bee = bee;
			this.target = target;

			this.originalFlyingSpeed = ((CraftBee) bee).getHandle().flyingSpeed;
			this.doneTime = NOT_DONE;
		}


		@Override
		public boolean shouldActivate() {
			return this.doneTime == NOT_DONE || TeamArena.getGameTick() - this.doneTime < WAIT_AFTER_DONE_TIME;
		}

		@Override
		public boolean shouldStayActive() {
			return this.shouldActivate();
		}

		@Override
		public void start() {
			((CraftBee) bee).getHandle().flyingSpeed = DELIVER_FLY_SPEED;
			this.bee.getPathfinder().moveTo(this.target);
		}

		@Override
		public void stop() {
			this.bee.getPathfinder().stopPathfinding();
			((CraftBee) bee).getHandle().flyingSpeed = this.originalFlyingSpeed;
		}

		@Override
		public void tick() {
			if (this.doneTime == NOT_DONE) {
				if (EntityUtils.distanceSqr(this.bee, this.target) <= DIST_SQR_TO_TARGET) {
					// Has reached the target successfully, give the honey and return
					this.doneTime = TeamArena.getGameTick();
					KitBeekeeper.BeekeeperAbility.beeHeal(this.bee, this.target);
				}
				else {
					this.bee.getPathfinder().moveTo(this.target);
				}
			}
			// else wait for WAIT_AFTER_DONE_TIME ticks
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

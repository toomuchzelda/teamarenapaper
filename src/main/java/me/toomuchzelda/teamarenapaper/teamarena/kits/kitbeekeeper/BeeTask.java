package me.toomuchzelda.teamarenapaper.teamarena.kits.kitbeekeeper;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Class containing logic of the different BeeTasks.
 * @author toomuchzelda
 */
public abstract class BeeTask
{
	private final Bee beeEntity;
	private final Goal<Bee> behaviour;

	public BeeTask(Bee beeEntity, Goal<Bee> beeAi) {
		this.beeEntity = beeEntity;
		this.behaviour = beeAi;
	}

	static class FollowOwner extends BeeTask {
		public FollowOwner(Bee beeEntity, Player owner) {
			super(beeEntity, new FollowOwnerGoal(owner));
		}
	}

	static class DefendPoint extends BeeTask { //TODO
		public DefendPoint(Bee beeEntity, Goal<Bee> beeAi) {
			super(beeEntity, beeAi);
		}
	}

	static class DeliverHoney extends BeeTask { // TODO
		public DeliverHoney(Bee beeEntity, Goal<Bee> beeAi) {
			super(beeEntity, beeAi);
		}
	}

	static class PursueEnemy extends BeeTask { // TODO
		public PursueEnemy(Bee beeEntity, Goal<Bee> beeAi) {
			super(beeEntity, beeAi);
		}
	}

	private static class FollowOwnerGoal implements Goal<Bee>
	{
		private final Player owner;
		private FollowOwnerGoal(Player owner) {
			this.owner = owner;
		}

		@Override
		public boolean shouldActivate() {
			return false;
		}

		@Override
		public boolean shouldStayActive() {
			return Goal.super.shouldStayActive();
		}

		@Override
		public void start() {
			Goal.super.start();
		}

		@Override
		public void stop() {
			Goal.super.stop();
		}

		@Override
		public void tick() {
			Goal.super.tick();
		}

		@Override
		public @NotNull GoalKey<Bee> getKey() {
			return null;
		}

		@Override
		public @NotNull EnumSet<GoalType> getTypes() {
			return null;
		}
	}
}

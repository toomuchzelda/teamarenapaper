package me.toomuchzelda.teamarenapaper.teamarena.kits.kitbeekeeper;

import com.destroystokyo.paper.entity.ai.Goal;
import org.bukkit.entity.Bee;

import java.util.Arrays;
import java.util.List;

/**
 * Class containing logic of the different BeeTasks.
 * @author toomuchzelda
 */
public abstract class BeeTask
{
	private final Bee beeEntity;
	private final List<Goal<Bee>> mobGoals;

	public BeeTask(Bee beeEntity, Goal<Bee>... beeAi) {
		this.beeEntity = beeEntity;
		this.mobGoals = Arrays.asList(beeAi);
	}

	public List<Goal<Bee>> getMobGoals() {
		return this.mobGoals;
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
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.kitbeekeeper;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Bee;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class PursueEnemyTask extends BeeTask {
	public PursueEnemyTask(Bee beeEntity, LivingEntity target) {
		super(beeEntity, new PursueEnemyGoal(beeEntity, target));
	}

	static class PursueEnemyGoal implements Goal<Bee>
	{
		private static final EnumSet<GoalType> GOAL_TYPES = EnumSet.of(GoalType.TARGET);
		private static final GoalKey<Bee> KEY = GoalKey.of(Bee.class, new NamespacedKey(Main.getPlugin(), "beekeeper_pursue_enemy"));

		private final Bee bee;
		private final LivingEntity target;

		public PursueEnemyGoal(Bee bee, LivingEntity target) {
			this.bee = bee;
			this.target = target;
		}

		@Override
		public boolean shouldActivate() {
			return !Main.getGame().isDead(this.target);
		}

		@Override
		public boolean shouldStayActive() {
			return this.shouldActivate();
		}

		@Override
		public void start() {
			bee.setTarget(target);
		}

		@Override
		public void stop() {
			bee.setTarget(null);
		}

		@Override
		public void tick() {}

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

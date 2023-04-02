package me.toomuchzelda.teamarenapaper.teamarena.kits.kitbeekeeper;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Bee;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class PursueEnemyTask extends BeeTask {
	private final PursueEnemyGoal goal;

	private PursueEnemyTask(Bee beeEntity, PursueEnemyGoal goal) {
		super(beeEntity, goal);
		this.goal = goal;
	}

	// Wrap the constructor so I can keep a reference to the PursueEnemyGoal here.
	public static PursueEnemyTask newInstance(Bee beeEntity, LivingEntity target) {
		PursueEnemyGoal goal = new PursueEnemyGoal(beeEntity, target);
		return new PursueEnemyTask(beeEntity, goal);
	}

	@Override
	Component getActionBarPart() {
		final int secondsLeft = (PursueEnemyGoal.PURSUE_TIME - (TeamArena.getGameTick() - this.goal.startTime)) / 20;
		return Component.text("Pursue " + secondsLeft + "s", NamedTextColor.RED);
	}

	static class PursueEnemyGoal implements Goal<Bee>
	{
		private static final EnumSet<GoalType> GOAL_TYPES = EnumSet.of(GoalType.TARGET);
		private static final GoalKey<Bee> KEY = GoalKey.of(Bee.class, new NamespacedKey(Main.getPlugin(), "beekeeper_pursue_enemy"));
		/** Amount of time the bee should pursue the target */
		private static final int PURSUE_TIME = 15 * 20;
		/** Will only pursue ghosts for 2 seconds */
		private static final int INVIS_PURSUE_TIME = 2 * 20;

		private final Bee bee;
		private final LivingEntity target;
		private final int startTime;
		private final int pursueTime;

		public PursueEnemyGoal(Bee bee, LivingEntity target) {
			this.bee = bee;
			this.target = target;
			this.startTime = TeamArena.getGameTick();

			// Temp variable: use INVIS_PURSUE_TIME if target is invis kit.
			int pTime = PURSUE_TIME;
			if (this.target instanceof Player playerTarget && !Main.getGame().isDead(this.target)) {
				Kit targetKit = Kit.getActiveKit(playerTarget);
				if (targetKit.isInvisKit()) {
					pTime = INVIS_PURSUE_TIME;
				}
			}
			this.pursueTime = pTime;
		}

		@Override
		public boolean shouldActivate() {
			return TeamArena.getGameTick() - this.startTime < this.pursueTime && !Main.getGame().isDead(this.target);
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
		public void tick() {
			bee.setTarget(this.target);
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

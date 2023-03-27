package me.toomuchzelda.teamarenapaper.teamarena.kits.kitbeekeeper;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.trigger.KitTrigger;
import me.toomuchzelda.teamarenapaper.teamarena.mobgoals.TargetEnemiesAtPointGoal;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class DefendPointTask extends BeeTask
{
	private static final int SWITCH_TARGET_CD = 15;
	private static final double MAX_DISTANCE_SQR = 15 * 15;
	private static final double MIN_DISTANCE_SQR = 0.5d * 0.5d;


	public DefendPointTask(Bee beeEntity, Player owner, Location loc) {
		super(beeEntity, new DefendPointGoal(Main.getPlugin(), owner, loc, beeEntity, SWITCH_TARGET_CD, MAX_DISTANCE_SQR, MIN_DISTANCE_SQR));
	}

	private static class DefendPointGoal extends TargetEnemiesAtPointGoal {
		private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey(Main.getPlugin(), "bee_defend_point"));
		private static final EnumSet<GoalType> GOAL_TYPES = EnumSet.of(GoalType.MOVE, GoalType.TARGET);

		public DefendPointGoal(Plugin plugin, Player owner, Location point, Mob mob, int changeTargetCooldown, double maxDistFromLocSqr, double minDistFromLocSqr) {
			super(plugin, owner, point, mob, changeTargetCooldown, maxDistFromLocSqr, minDistFromLocSqr);
		}

		@Override
		protected boolean isValidEnemy(Mob mob, Player candidateEnemy) {
			TeamArena game = Main.getGame();
			if (game.getGameState() != GameState.LIVE) return false;
			if (game.isDead(candidateEnemy)) return false;

			TeamArenaTeam team = Main.getPlayerInfo(this.owner).team;
			if(team.getPlayerMembers().contains(candidateEnemy)) return false;

			Kit kit = Kit.getActiveKit(candidateEnemy);
			if (kit.isInvisKit()) return false;

			return true;
		}

		@Override
		public boolean shouldStayActive() {
			super.shouldStayActive();
			return true;
		}

		@Override
		public boolean shouldActivate() {
			super.shouldActivate();
			return true;
		}

		@Override
		public @NotNull GoalKey<Mob> getKey() {
			return KEY;
		}

		@Override
		public @NotNull EnumSet<GoalType> getTypes() {
			return GOAL_TYPES;
		}
	}
}

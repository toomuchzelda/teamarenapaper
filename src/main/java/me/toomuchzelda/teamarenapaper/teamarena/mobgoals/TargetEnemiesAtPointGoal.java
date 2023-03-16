package me.toomuchzelda.teamarenapaper.teamarena.mobgoals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.trigger.KitTrigger;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * https://pastebin.com/QE4qEC1s
 * Mob goal to target any enemies that come close to a point. If no enemies, return to the point and remain there
 * until an enemy comes within maxDistFromLocSqr
 */
public class TargetEnemiesAtPointGoal<T> implements Goal
{
	private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey(Main.getPlugin(), "target_enemies_at_point"));

	private final int changeTargetTime;
	private final double maxDistFromLocSqr;
	private final double minDistFromLocSqr;

	private final Location defendLoc;
	protected final Player owner;
	private final Mob golem;

	private boolean walkToLoc;
	private Player closestPlayer;
	private int switchTime;

	/**
	 * @param point The location to remain at and defend.
	 * @param mob The mob doing the defending.
	 * @param changeTargetCooldown The minimum amount of time the mob must wait after selecting a target to choose another target
	 * @param maxDistFromLocSqr Maximum distance the mob will stray from the point while chasing enemies.
	 * @param minDistFromLocSqr Minimum distance from the point the mob may idle.
	 */
	public TargetEnemiesAtPointGoal(Plugin plugin, Player owner, Location point, Mob mob,
									int changeTargetCooldown, double maxDistFromLocSqr, double minDistFromLocSqr) {

		this.changeTargetTime = changeTargetCooldown;
		this.maxDistFromLocSqr = maxDistFromLocSqr;
		this.minDistFromLocSqr = minDistFromLocSqr;

		this.owner = owner;
		this.defendLoc = point.clone();
		this.golem = mob;

		this.switchTime = 0;
		this.walkToLoc = false;
	}

	@Override
	public boolean shouldActivate() {
		Player closestCandidate = getClosestPlayer();
		if(closestCandidate != this.closestPlayer) {
			this.switchTime = TeamArena.getGameTick();
		}

		this.closestPlayer = closestCandidate;

		if(this.closestPlayer == null) { // No target found - move to defend loc if too far
			double distSqr = this.golem.getLocation().distanceSquared(this.defendLoc);
			this.walkToLoc = distSqr >= this.minDistFromLocSqr;
		}
		else {
			this.walkToLoc = false;
		}

		return closestPlayer != null || walkToLoc;
	}

	@Override
	public boolean shouldStayActive() {
		//Bukkit.broadcastMessage("shouldStayActive: " + shouldContinue);
		return shouldActivate();
	}

	@Override
	public void start() {
		if(this.closestPlayer != null)
			golem.setTarget(closestPlayer);
		else if(this.walkToLoc)
			golem.getPathfinder().moveTo(this.defendLoc, 0.6d);
	}

	@Override
	public void stop() {
		golem.setTarget(null);
		golem.getPathfinder().stopPathfinding();
	}

	@Override
	public void tick() {
		// Allow some buffer period so don't switch between many targets too quickly
		if(this.switchTime != 0 && TeamArena.getGameTick() - this.switchTime >= this.changeTargetTime) {
			this.switchTime = 0;

			this.golem.setTarget(this.closestPlayer);
		}

		if(walkToLoc && TeamArena.getGameTick() % 5 == 0) {
			this.golem.getPathfinder().moveTo(this.defendLoc);
		}
	}

	@Override
	public @NotNull GoalKey<Mob> getKey() {
		return KEY;
	}

	@Override
	public @NotNull EnumSet<GoalType> getTypes() {
		return EnumSet.of(GoalType.TARGET, GoalType.MOVE);
	}

	private Player getClosestPlayer() {
		double closestDistanceSqr = this.maxDistFromLocSqr;

		// Only find a player if golem is in range of centre
		if(this.golem.getLocation().distanceSquared(this.defendLoc) >= (this.maxDistFromLocSqr))
			return null;

		// Find player closest to the centre
		Player closestPlayer = null;
		for(Player candidate : Main.getGame().getPlayers()) {
			double distSqr = candidate.getLocation().distanceSquared(this.defendLoc);
			if(distSqr <= closestDistanceSqr && isValidEnemy(golem, candidate)) {
				closestDistanceSqr = distSqr;
				closestPlayer = candidate;
			}
		}

		return closestPlayer;
	}

	protected boolean isValidEnemy(Mob mob, Player candidateEnemy) {
		TeamArena game = Main.getGame();
		if (game.getGameState() != GameState.LIVE) return false;
		if (game.isDead(candidateEnemy)) return false;

		TeamArenaTeam team = Main.getPlayerInfo(this.owner).team;
		if(team.getPlayerMembers().contains(candidateEnemy)) return false;

		Kit kit = Kit.getActiveKit(candidateEnemy);

		if(kit instanceof KitTrigger) return false;

		if(kit.isInvisKit()) {
			if(!candidateEnemy.isSprinting() && candidateEnemy.getArrowsInBody() == 0 && !ItemUtils.isHoldingItem(candidateEnemy))
				return false;
		}

		if(!mob.hasLineOfSight(candidateEnemy)) return false;

		return true;
	}
}

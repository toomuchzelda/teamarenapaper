package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.destroystokyo.paper.entity.ai.MobGoals;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.trigger.KitTrigger;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class WolvesKillStreak extends KillStreak
{
	private static final TextColor color = TextColor.color(216, 212, 213);

	WolvesKillStreak() {
		super("Attack wolves", "A pack of wolves will follow or sit at your command and chew up enemies", color, null,
				new WolvesAbility());

		this.crateItemType = Material.WOLF_SPAWN_EGG;
		this.crateBlockType = Material.WHITE_WOOL;
	}

	@Override
	boolean isDeliveredByCrate() {
		return true;
	}

	// Band aid - pass the crate location to the WolvesAbility#giveAbility()
	private static final HashMap<Player, Location> crateLocs = new HashMap<>();

	@Override
	public void onCrateLand(Player player, Location destination) {
		crateLocs.put(player, destination);
		this.giveStreak(player, Main.getPlayerInfo(player));
	}

	private static class WolvesAbility extends Ability {

		private record WolfInfo(Set<Wolf> wolves) {}
		private static final Map<Player, WolfInfo> WOLF_MASTERS = new HashMap<>();
		private static final Map<Wolf, Player> WOLF_LOOKUP = new HashMap<>();

		@Override
		public void giveAbility(Player player) {
			Location crateLoc = crateLocs.remove(player);
			DyeColor dyeColor = Main.getPlayerInfo(player).team.getDyeColour();
			Wolf wolf = player.getWorld().spawn(crateLoc, Wolf.class, wolf1 -> {
				wolf1.setOwner(player);
				wolf1.setAgeLock(true);
				wolf1.setSitting(false);
				wolf1.setCollarColor(dyeColor);
			});

			MobGoals manager = Bukkit.getMobGoals();
			manager.removeAllGoals(wolf, GoalType.TARGET); // Remove pre-existing target goals and add our own.
			manager.addGoal(wolf, 2, new TargetEnemiesGoal(Main.getPlugin(), player, wolf));


		}
	}

	/**
	 * https://pastebin.com/QE4qEC1s
	 * Wolf's mob goal to target enemies and attack them.
	 */
	public static class TargetEnemiesGoal implements Goal<Wolf> {
		private static final int CHANGE_TARGET_TIME = 20;

		private final GoalKey<Wolf> key;
		private final Player owner;
		private final Wolf wolf;
		private Player closestPlayer;
		private int switchTime;

		public TargetEnemiesGoal(Plugin plugin, Player owner, Wolf mob) {
			this.key = GoalKey.of(Wolf.class, new NamespacedKey(plugin, "target_enemies"));
			this.owner = owner;
			this.wolf = mob;

			this.switchTime = 0;
		}

		@Override
		public boolean shouldActivate() {
			Player closestCandidate = getClosestPlayer();
			if(closestCandidate != this.closestPlayer) {
				this.switchTime = TeamArena.getGameTick();
			}

			closestPlayer = closestCandidate;

			return closestPlayer != null;
		}

		@Override
		public boolean shouldStayActive() {
			boolean shouldContinue = shouldActivate();
			//Bukkit.broadcastMessage("shouldStayActive: " + shouldContinue);
			return shouldContinue;
		}

		@Override
		public void start() {
			wolf.setTarget(closestPlayer);
		}

		@Override
		public void stop() {
			wolf.setTarget(null);
		}

		@Override
		public void tick() {
			// Allow some buffer period so don't switch between many targets too quickly
			if(this.switchTime != 0 && TeamArena.getGameTick() - this.switchTime >= CHANGE_TARGET_TIME) {
				this.switchTime = 0;

				this.wolf.setTarget(this.closestPlayer);
			}
		}

		@Override
		public GoalKey<Wolf> getKey() {
			return key;
		}

		@Override
		public EnumSet<GoalType> getTypes() {
			return EnumSet.of(GoalType.TARGET);
		}

		private Player getClosestPlayer() {
			final double radius = 14d;

			double closestDistanceSqr = radius * radius;
			Player closestPlayer = null;
			Location wolfLoc = this.wolf.getLocation();
			for(Player candidate : Main.getGame().getPlayers()) {
				double distSqr = candidate.getLocation().distanceSquared(wolfLoc);
				if(distSqr <= closestDistanceSqr && isValidEnemy(wolf, candidate)) {
					closestDistanceSqr = distSqr;
					closestPlayer = candidate;
				}
			}

			return closestPlayer;
		}

		private boolean isValidEnemy(Wolf wolf, Player candidateEnemy) {
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

			if(!wolf.hasLineOfSight(candidateEnemy)) return false;

			return true;
		}
	}
}

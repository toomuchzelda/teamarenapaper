package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import com.destroystokyo.paper.entity.ai.*;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate.CratePayload;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.trigger.KitTrigger;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class IronGolemKillStreak extends CratedKillStreak
{
	private static final TextColor color = TextColor.color(206, 184, 167);
	private static final List<String> GOLEM_NAMES = List.of(
			"Big Man", "BigDoggie", "Will crush you for iron", "Pandu_Destroyer", "Stoic Sigma", "Ben", "Muhammad",
			"The Armored Titan", "Steph", "(●'◡'●)", "(^///^)", "OwO", "(✿◡‿◡)", "( •̀ ω •́ )✧", "d=====(￣▽￣*)b",
			"private static final Map<Player, Set<IronGolem>> GOLEM_OWNERS = new HashMap<>();",
			"float Q_rsqrt( float number ){ long i; float x2, y; const float threehalfs = 1.5F; x2 = number * 0.5F; y = number; i = * ( long * ) &y; /*evil floating point bit level hacking*/ i = 0x5f3759df - ( i >> 1 ); /* what the fuck?*/ y  = * ( float * ) &i;\n" + "\ty  = y * ( threehalfs - ( x2 * y * y ) ); /* 1st iteration*/ " + "/* y  = y * ( threehalfs - ( x2 * y * y ) );*/ /* 2nd iteration, this can be removed*/ return y; }",
			"Hello!", "Bond", "Reg", "libraryaddict", "n00b d35tr0y3r", "Gamer", "I love Team Arena", "I love toomuchzelda!",
			"Metal man", "Lil Iron"
	);

	IronGolemKillStreak() {
		super("Iron Golem", "An Iron Golem that will stay at the position where you summoned it and defend it with its life"
				, color, null, Material.IRON_BLOCK, new GolemAbility());
	}


	private static final CratePayload GOLEM_PAYLOAD = new CratePayload.Group(new Vector(0, 3, 0),
		Map.of(
			new Vector(0, 2, 0), new CratePayload.SimpleBlock(Material.CARVED_PUMPKIN.createBlockData()),
			new Vector(0, 1, 0), new CratePayload.SimpleBlock(Material.IRON_BLOCK.createBlockData()),
			new Vector(1, 1, 0), new CratePayload.SimpleBlock(Material.IRON_BLOCK.createBlockData()),
			new Vector(-1, 1, 0), new CratePayload.SimpleBlock(Material.IRON_BLOCK.createBlockData()),
			new Vector(0, 0, 0), new CratePayload.SimpleBlock(Material.IRON_BLOCK.createBlockData())
		));
	@Override
	public @NotNull CratePayload getPayload(Player player, Location destination) {
		return GOLEM_PAYLOAD;
	}

	// Band aid - pass the crate location to the WolvesAbility#giveAbility()
	private static final HashMap<Player, Location> crateLocs = new HashMap<>();

	@Override
	public void onCrateLand(Player player, Location destination) {
		destination.getWorld().playSound(destination, Sound.BLOCK_ANVIL_LAND, 1f, 2f);

		crateLocs.put(player, destination);
		this.giveStreak(player, Main.getPlayerInfo(player));
	}

	public static class GolemAbility extends Ability {

		private static final Map<Player, Set<IronGolem>> GOLEM_OWNERS = new HashMap<>();
		private static final Map<IronGolem, Player> GOLEM_LOOKUP = new HashMap<>();

		@Override
		public void giveAbility(Player player) {
			Location crateLoc = crateLocs.remove(player);
			if(crateLoc == null)
				crateLoc = player.getLocation();

			TeamArenaTeam team = Main.getPlayerInfo(player).team;

			Set<IronGolem> set = GOLEM_OWNERS.computeIfAbsent(player, player1 -> new HashSet<>());
			IronGolem golem = player.getWorld().spawn(crateLoc, IronGolem.class, golem1 -> {
				golem1.setPlayerCreated(false);

				String name = "(Iron Golem) " + GOLEM_NAMES.get(MathUtils.randomMax(GOLEM_NAMES.size() - 1));
				golem1.customName(Component.text(name, team.getRGBTextColor()));
				golem1.setCustomNameVisible(true);
			});

			MobGoals manager = Bukkit.getMobGoals();
			manager.removeAllGoals(golem, GoalType.TARGET); // Remove pre-existing target goals and add our own.
			manager.removeGoal(golem, VanillaGoal.MOVE_BACK_TO_VILLAGE);
			manager.addGoal(golem, 2, new TargetEnemiesGoalAtPointGoal(Main.getPlugin(), player, crateLoc, golem));

			team.addMembers(golem);
			set.add(golem);
			GOLEM_LOOKUP.put(golem, player);
		}

		@Override
		public void removeAbility(Player player) {
			Set<IronGolem> wolves = GOLEM_OWNERS.remove(player);
			if(wolves == null) throw new IllegalStateException("WolvesAbility#removeAbility()");

			for(IronGolem wolf : wolves) {
				wolf.remove();
				GOLEM_LOOKUP.remove(wolf);
			}
			wolves.clear();
		}

		@Override
		public void unregisterAbility() {
			for(Set<IronGolem> wolves : GOLEM_OWNERS.values()) {
				for (IronGolem wolf : wolves) {
					wolf.remove();
					GOLEM_LOOKUP.remove(wolf);
				}
				wolves.clear();
			}
			GOLEM_OWNERS.clear();
		}

		public static boolean isKillStreakGolem(IronGolem golem) {
			return GOLEM_LOOKUP.containsKey(golem);
		}

		public static void handleIronGolemAttemptDamage(DamageEvent event) {
			Player owner = GOLEM_LOOKUP.get(event.getVictim());
			if(owner == null) return;
			if(Main.getPlayerInfo(owner).team.hasMember(event.getFinalAttacker())) {
				event.setCancelled(true);
			}
		}

		public static void handleIronGolemAttemptAttack(DamageEvent event) {
			IronGolem golem = (IronGolem) event.getFinalAttacker();
			Player owner = GOLEM_LOOKUP.get(golem);
			if(owner == null) return;

			if(event.getDamageType().isMelee()) {
				event.setFinalAttacker(owner);
				event.setDamageType(DamageType.IRON_GOLEM_KILL.withDamageSource(event.getDamageType()));
				event.setDamageTypeCause(golem);
			}
		}
	}

	/**
	 * https://pastebin.com/QE4qEC1s
	 * Golem's mob goal to target enemies and attack them.
	 */
	public static class TargetEnemiesGoalAtPointGoal implements Goal<IronGolem>
	{
		private static final int CHANGE_TARGET_TIME = 20;
		private static final double MAX_DIST_FROM_LOC_SQR = 20 * 20d;
		private static final double MIN_DIST_FROM_LOC_SQR = 3d * 3d;

		private final GoalKey<IronGolem> key;
		private final Location defendLoc;
		private boolean walkToLoc;
		private final Player owner;
		private final IronGolem golem;
		private Player closestPlayer;
		private int switchTime;

		public TargetEnemiesGoalAtPointGoal(Plugin plugin, Player owner, Location loc, IronGolem mob) {
			this.key = GoalKey.of(IronGolem.class, new NamespacedKey(plugin, "target_enemies"));
			this.owner = owner;
			this.defendLoc = loc.clone();
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
				this.walkToLoc = distSqr >= MIN_DIST_FROM_LOC_SQR;
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
			if(this.switchTime != 0 && TeamArena.getGameTick() - this.switchTime >= CHANGE_TARGET_TIME) {
				this.switchTime = 0;

				this.golem.setTarget(this.closestPlayer);
			}

			if(walkToLoc && TeamArena.getGameTick() % 5 == 0) {
				this.golem.getPathfinder().moveTo(this.defendLoc);
			}
		}

		@Override
		public @NotNull GoalKey<IronGolem> getKey() {
			return key;
		}

		@Override
		public @NotNull EnumSet<GoalType> getTypes() {
			return EnumSet.of(GoalType.TARGET, GoalType.MOVE);
		}

		private Player getClosestPlayer() {
			double closestDistanceSqr = MAX_DIST_FROM_LOC_SQR;

			// Only find a player if golem is in range of centre
			if(this.golem.getLocation().distanceSquared(this.defendLoc) >= (MAX_DIST_FROM_LOC_SQR))
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

		private boolean isValidEnemy(IronGolem wolf, Player candidateEnemy) {
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

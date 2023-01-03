package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.destroystokyo.paper.entity.ai.MobGoals;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate.CratePayload;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.trigger.KitTrigger;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class WolvesKillStreak extends CratedKillStreak
{
	private static final TextColor color = TextColor.color(216, 212, 213);
	private static final int WOLF_COUNT = 3;

	private static final List<String> WOLF_NAMES = List.of(
			"Dorothy", "Santa", "Quote", "Curly", "Toroko", "Totoro", "Elitemaster5", "I love my owner", "I hate my owner",
			"Yato", "Yukine", "Hiyori", "Eren", "Mikasa", "Armin", "Wolverine's final form", "Doggy", "Cutie pie",
			"Milo", "Lilipup", "Olivia", "Noah", "Oliver", "Harper", "Archer", "Obama", "Ugly unrecognisable thing",
			"<3", "</3", "Naruto", "Ichigo", "Kruger", "Loid", "Anya", "Weather Forecast", "Pucci", "DIO", "Jojo"
	);

	WolvesKillStreak() {
		super("Attack wolves", "A pack of wolves that will follow at your command and chew up enemies", color, null,
				new WolvesAbility());
	}

	@Override
	public @NotNull ItemStack createCrateItem(Player player) {
		return createSimpleCrateItem(Material.WOLF_SPAWN_EGG);
	}

	@Override
	public @NotNull CratePayload getPayload(Player player, Location destination) {
		return new CratePayload.SimpleEntity(EntityType.WOLF);
	}

	// Band aid - pass the crate location to the WolvesAbility#giveAbility()
	private static final HashMap<Player, Location> crateLocs = new HashMap<>();

	@Override
	public void onCrateLand(Player player, Location destination) {
		super.onCrateLand(player, destination);
		crateLocs.put(player, destination);
		this.giveStreak(player, Main.getPlayerInfo(player));
	}

	public static class WolvesAbility extends Ability {

		private static final Map<Player, Set<Wolf>> WOLF_MASTERS = new HashMap<>();
		private static final Map<Wolf, Player> WOLF_LOOKUP = new HashMap<>();

		@Override
		public void giveAbility(Player player) {
			Location crateLoc = crateLocs.remove(player);
			if(crateLoc == null)
				crateLoc = player.getLocation();

			TeamArenaTeam team = Main.getPlayerInfo(player).team;
			DyeColor dyeColor = team.getDyeColour();

			Set<Wolf> set = WOLF_MASTERS.computeIfAbsent(player, player1 -> new HashSet<>());
			for(int i = 0; i < WOLF_COUNT; i++) {
				Wolf wolf = player.getWorld().spawn(crateLoc, Wolf.class, wolf1 -> {
					wolf1.setOwner(player);
					wolf1.setAgeLock(true);
					wolf1.setSitting(false);
					wolf1.setCollarColor(dyeColor);
					wolf1.setBreed(false);

					String name = "(Wolf) " + WOLF_NAMES.get(MathUtils.randomMax(WOLF_NAMES.size() - 1));
					wolf1.customName(Component.text(name, team.getRGBTextColor()));
					wolf1.setCustomNameVisible(true);
				});

				MobGoals manager = Bukkit.getMobGoals();
				manager.removeAllGoals(wolf, GoalType.TARGET); // Remove pre-existing target goals and add our own.
				manager.addGoal(wolf, 2, new TargetEnemiesGoal(Main.getPlugin(), player, wolf));

				team.addMembers(wolf);
				set.add(wolf);
				WOLF_LOOKUP.put(wolf, player);
			}
		}

		@Override
		public void removeAbility(Player player) {
			Set<Wolf> wolves = WOLF_MASTERS.remove(player);
			if(wolves == null) throw new IllegalStateException("WolvesAbility#removeAbility()");

			for(Wolf wolf : wolves) {
				wolf.remove();
				WOLF_LOOKUP.remove(wolf);
			}
			wolves.clear();
		}

		@Override
		public void unregisterAbility() {
			for(Set<Wolf> wolves : WOLF_MASTERS.values()) {
				for (Wolf wolf : wolves) {
					wolf.remove();
					WOLF_LOOKUP.remove(wolf);
				}
				wolves.clear();
			}
			WOLF_MASTERS.clear();
		}

		/**
		 * Cancel damage done to wolf by same team
		 */
		public static void handleWolfAttemptDamage(DamageEvent event) {
			Player master = WOLF_LOOKUP.get(event.getVictim());
			if(master == null) return;
			if(Main.getPlayerInfo(master).team.hasMember(event.getFinalAttacker())) {
				event.setCancelled(true);
			}
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			// The wolf attacked someone
			if(event.getDamageType().isMelee() && event.getAttacker() instanceof Wolf wolf) {
				event.setDamageType(DamageType.WOLF_KILL.withDamageSource(event.getDamageType()));
				event.setDamageTypeCause(wolf);
			}
		}

		private static final TextColor color = TextColor.color(99, 125, 5);
		private static final List<Component> fleshLore = List.of(ItemUtils.noItalics(
				Component.text("Your wolves fetched this for you. Feed it to them!", color)));
		@Override
		public void onKill(DamageEvent event) {
			// If a dog got the kill, give the owner rotten flesh
			if(event.getDamageType().is(DamageType.WOLF_KILL)) {
				ItemStack flesh = ItemBuilder.of(Material.ROTTEN_FLESH)
						.displayName(EntityUtils.getComponent(event.getVictim()).append(Component.text("'s chewed up corpse", color)))
						.lore(fleshLore)
						.build();

				((Player) event.getFinalAttacker()).getInventory().addItem(flesh);
			}
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
		public @NotNull GoalKey<Wolf> getKey() {
			return key;
		}

		@Override
		public @NotNull EnumSet<GoalType> getTypes() {
			return EnumSet.of(GoalType.TARGET);
		}

		private Player getClosestPlayer() {
			final double radius = 14d;
			double closestDistanceSqr = radius * radius;

			// Only find a player if dog is in range of owner
			if(this.wolf.getLocation().distanceSquared(this.owner.getLocation()) >= (20d * 20d))
				return null;

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

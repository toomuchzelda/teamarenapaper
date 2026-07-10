package me.toomuchzelda.teamarenapaper.teamarena.oneagainstall;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.damage.KillAssistTracker;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.KillStreakManager;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitOne;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterAction;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterRule;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class OneAgainstAll extends TeamArena {
	private static final Component GAME_NAME = Component.text("One Against All", GameType.OAA.shortName.color());
	private static final Component HOW_TO_PLAY = Component.textOfChildren(
		Component.text("The Chosen One must kill everyone before time's up.", GameType.OAA.shortName.color()),
		Component.newline(),
		Component.text("Everyone else must kill them or survive.", GameType.OAA.shortName.color()),
		Component.newline(),
		Component.text("The Chosen One does not regen health.", GameType.OAA.shortName.color())
	);

	private static final FilterRule ONE_RULE = new FilterRule("oaa/one_team", "One team restrictions", FilterAction.allow(KitOne.KEY));
	private static final FilterRule ALL_RULE = new FilterRule("oaa/all_team", "All team restrictions", FilterAction.block(KitOne.KEY));

	private final Kit kitOne;

	private TeamArenaTeam oneTeam;
	private TeamArenaTeam allTeam;

	private double onePlayerMaxHealth;
	private AttributeModifier onePlayerHealthModifier;

	private Player onePlayer; // null until teams decided unless admin set
	private double oneKills = 0d;
	private Entity oneKiller; // player (or not) who kills the one
	private final Map<Player, Double> damagers = new HashMap<>();
	private int allTeamSize; // teams decided

	private int ticksLeft;

	public OneAgainstAll(TeamArenaMap map) {
		super(map);

		this.kitOne = this.kits.get(KitOne.KEY);
		this.ticksLeft = 20 * 60 * 5; // 5 minutes
	}

	@Override
	protected void loadConfig(TeamArenaMap map) {
		super.loadConfig(map);

		assert this.teams.length == 2;
		this.oneTeam = this.teams[0];
		this.allTeam = this.teams[1];

		final TeamArenaMap.SNDInfo sndInfo = map.getSndInfo();
		if (sndInfo != null) {
			for (final List<BlockVector> list : sndInfo.teamBombs().values()) {
				list.forEach(bombPos -> {
					final Block block = this.gameWorld.getBlockAt(
						bombPos.getBlockX(),
						bombPos.getBlockY(),
						bombPos.getBlockZ()
					);
					if (block.getType() == Material.TNT)
						block.setType(Material.AIR);
				});
			}
		}
	}

	@Override
	public void prepTeamsDecided() {
		super.prepTeamsDecided();

		assert this.oneTeam.getPlayerMembers().size() == 1;
		assert this.onePlayer == this.oneTeam.getRandomPlayer();
		Main.getPlayerInfo(this.onePlayer).kit = this.kitOne;

		this.allTeamSize = this.allTeam.getPlayerMembers().size();

		// hacky, but this.allAgainstPlayer would've been assigned by this.informOfTeam, called by super.prepTeamsDecided
		// unless there's only 1 player in the game...
		if (this.allAgainstPlayer != null)
			Bukkit.broadcast(this.allAgainstPlayer);

		this.updateKitFilters();
		KitFilter.updateKitsFor(this, this.players);
	}

	private static final WeakHashMap<Player, Void> alreadyChosen = new WeakHashMap<>();
	@Override
	public void setupTeams() {
		// assign all players to a team before super
		assert !this.players.isEmpty();

		final List<Player> shuffled = new ArrayList<>(this.players);
		shuffled.removeIf(player -> Main.getPlayerInfo(player).team != this.noTeamTeam);
		Collections.shuffle(shuffled, MathUtils.random);

		if (this.onePlayer == null) { // if not already set by external forces
			final List<Player> oneCandidates = new ArrayList<>(shuffled);
			oneCandidates.removeIf(alreadyChosen::containsKey);
			if (oneCandidates.isEmpty()) {
				alreadyChosen.clear();
				oneCandidates.add(MathUtils.randomElement(shuffled));
			}
			this.onePlayer = oneCandidates.getFirst();
		}

		this.oneTeam.addMembers(this.onePlayer);
		alreadyChosen.put(this.onePlayer, null);
		shuffled.remove(this.onePlayer);
		shuffled.forEach(player -> this.allTeam.addMembers(player));

		super.setupTeams();
	}

	@Override
	public TeamArenaTeam addToLowestTeam(Player player, boolean add) {
		final TeamArenaTeam chosen;
		if (this.oneTeam.getPlayerMembers().isEmpty()) {
			chosen = this.oneTeam;
			Main.logger().log(Level.WARNING, "Should be unreachable", new RuntimeException());
		}
		else
			chosen = this.allTeam;

		if (add) {
			chosen.addMembers(player);
			if (chosen == this.oneTeam) {
				this.onePlayer = player;
			}
		}

		return chosen;
	}

	private Component allAgainstPlayer = null;
	@Override
	public void informOfTeam(Player p, Component title) {
		if (p == this.onePlayer) {
			final Component allAgainstYou = Component.text("All against ", NamedTextColor.DARK_RED).append(Component.text("YOU", NamedTextColor.DARK_RED, TextDecoration.UNDERLINED));
			super.informOfTeam(p, allAgainstYou);
		}
		else {
			if (this.allAgainstPlayer == null) {
				this.allAgainstPlayer = Component.text("All against ", NamedTextColor.GOLD)
					.append(this.onePlayer.playerListName())
					.append(Component.text("!", NamedTextColor.GOLD));
			}
			super.informOfTeam(p, this.allAgainstPlayer);
		}
	}

	@Override
	public void prepLive() {
		super.prepLive();

		// extra 10 hearts for every enemy
		final double extra = this.allTeam.getPlayerMembers().size() * 10;
		this.onePlayerHealthModifier = new AttributeModifier(
			new NamespacedKey(Main.getPlugin(), "oaa/one_max_health_modifier"),
			extra,
			AttributeModifier.Operation.ADD_NUMBER
		);

		final AttributeInstance attribute = this.onePlayer.getAttribute(Attribute.MAX_HEALTH);
		assert attribute != null;
		attribute.addModifier(onePlayerHealthModifier);
		this.onePlayer.setHealth(attribute.getValue());
		this.onePlayerMaxHealth = attribute.getValue();

		// extra items
		final PlayerInfo pinfo = Main.getPlayerInfo(this.onePlayer);
		final KillStreakManager streakManager = this.getKillStreakManager();
		streakManager.giveKillStreak(this.onePlayer, pinfo, KillStreakManager.KillStreakID.COMPASS);
		streakManager.giveKillStreak(this.onePlayer, pinfo, KillStreakManager.KillStreakID.WOLVES);
		streakManager.giveKillStreak(this.onePlayer, pinfo, KillStreakManager.KillStreakID.IRON_GOLEM);

		this.onePlayer.setGlowing(true);
	}

	public void checkWinner() {
		if (CommandDebug.ignoreWinConditions) return;

		if (this.isDead(this.onePlayer)) {
			this.winningTeam = this.allTeam;
		}
		else {
			int alivePlayerCount = 0;
			for (Player player : this.allTeam.getPlayerMembers()) {
				if (!this.isDead(player))
					alivePlayerCount++;
			}
			if (alivePlayerCount == 0) {
				this.winningTeam = this.oneTeam;
			}
		}
	}

	@Override
	public void liveTick() {
		this.ticksLeft--;
		if (this.ticksLeft <= 0) {
			if (this.ticksLeft == 0)
				Bukkit.broadcast(Component.text("Time's up!", NamedTextColor.RED));

			if (!this.isDead(this.onePlayer) && this.ticksLeft % 40 == 0)
				this.gameWorld.strikeLightning(this.onePlayer.getLocation());
		}

		super.liveTick();

		if (this.winningTeam != null) {
			this.prepEnd();
		}
		else {
			this.checkWinner();
			if (this.winningTeam != null)
				this.prepEnd();
		}
	}

	// Hook into just before the killassist tracker is read and cleared
	@Override
	protected void attributeKillAndAssists(Player victim, PlayerInfo victimInfo, @Nullable Player finalDamager) {
		if (victim == this.onePlayer) {
			if (finalDamager != null)
				this.oneKiller = finalDamager;

			final KillAssistTracker tracker = victimInfo.getKillAssistTracker();

			for (final var iter = tracker.getIterator(); iter.hasNext();) {
				final var entry = iter.next();

				final Player damager = entry.getKey();
				final Double amount = entry.getValue();
				assert amount != null;
				if (this.damagers.put(damager, amount) != null) {
					Main.logger().warning(victim.getName() + " was killed more than once by " + damager.getName() + " for amount " + amount);
				}
			}
		}

		super.attributeKillAndAssists(victim, victimInfo, finalDamager);
	}

	@Override
	public void addKillAmount(Player player, double amount, Player victim) {
		super.addKillAmount(player, amount, victim);

		if (player == this.onePlayer) {
			this.oneKills += amount;
		}
	}

	@Override
	public void prepEnd() {
		super.prepEnd();

		final var builder = Component.text();

		Player maxKiller = null;
		double killAmount = 0d;
		for (var entry : this.damagers.entrySet()) {
			final double k = entry.getValue();
			if (k > killAmount) {
				maxKiller = entry.getKey();
				killAmount = k;
			}
		}
		if (maxKiller != null) {
			final Component msg = Component.textOfChildren(
				maxKiller.playerListName(),
				Component.text(" dealt the most damage at " + TextUtils.formatNumber(killAmount / 2d, 2) + "/" + (this.onePlayerMaxHealth / 2d), NamedTextColor.GOLD),
				TextColors.HEART,
				Component.newline()
			);
			builder.append(msg);
		}

		if (this.winningTeam == this.allTeam) {
			if (this.oneKiller != null) {
				final Component msg = Component.textOfChildren(
					EntityUtils.getComponent(this.oneKiller),
					Component.text(" dealt the final blow", NamedTextColor.GOLD),
					Component.newline()
				);
				builder.append(msg);
			}

			builder.append(Component.textOfChildren(
				EntityUtils.getComponent(this.onePlayer),
				Component.text(" killed " + TextUtils.formatNumber(this.oneKills, 2) + "/" + this.allTeamSize + " ", NamedTextColor.GOLD),
				this.allTeam.getComponentName(),
				Component.text(" players", NamedTextColor.GOLD)
			));
		}
		else if (this.winningTeam == this.oneTeam) {
			final double health = this.onePlayer.getHealth();
			final Component msg = Component.textOfChildren(
				EntityUtils.getComponent(this.onePlayer),
				Component.text(" had " + TextUtils.formatNumber(health / 2d, 2) + "/" + (this.onePlayerMaxHealth / 2d), NamedTextColor.GOLD),
				TextColors.HEART,
				Component.text(" left", NamedTextColor.GOLD)
			);
			builder.append(msg);
		}

		Bukkit.broadcast(builder.build());
	}

	@Override
	protected boolean shouldRegen(Player p) {
		if (p == this.onePlayer) return false;
		return super.shouldRegen(p);
	}

	@Override
	public void onDamage(DamageEvent event) {
		super.onDamage(event);

		if (!event.isCancelled() && event.getDamageType().is(DamageType.LIGHTNING) && event.getVictim() == this.onePlayer) {
			event.setRawDamage(event.getRawDamage() * 3d);
		}
	}

	@Override
	protected void applyKitFilters() {
		KitFilter.addGlobalRule(TeamArena.NO_HNS);
		KitFilter.addGlobalRule(ALL_RULE); // Noone can select super trooper until teams decided
	}

	private void updateKitFilters() {
		KitFilter.removeGlobalRule(ALL_RULE.key());
		KitFilter.addTeamRule(this.oneTeam.getSimpleName(), ONE_RULE);
		KitFilter.addTeamRule(this.allTeam.getSimpleName(), ALL_RULE);
	}

	@Override
	protected void removeKitFilters() {
		KitFilter.removeGlobalRule(TeamArena.NO_HNS.key());
		KitFilter.removeGlobalRule(ALL_RULE.key());
		KitFilter.removeTeamRule(this.oneTeam.getSimpleName(), ONE_RULE.key());
		KitFilter.removeTeamRule(this.allTeam.getSimpleName(), ALL_RULE.key());
	}

	@Override
	public Collection<Component> updateSharedSidebar() {
		final List<Component> list = new ArrayList<>(4);
		list.add(Component.text("Target:", NamedTextColor.GRAY));
		list.add(this.onePlayer.playerListName());

		if (this.gameState == GameState.LIVE) {
			final int health;
			if (this.isDead(this.onePlayer))
				health = 0;
			else
				health = (int) ((this.onePlayer.getHealth() / 2d) + 0.5d);

			list.add(Component.textOfChildren(
				Component.text(health + "/" + ((int) (this.onePlayerMaxHealth / 2d))),
				TextColors.HEART
			));
		}

		if (this.ticksLeft > 0)
			list.add(TextUtils.formatDurationMmSs(Ticks.duration(this.ticksLeft)).append(Component.text(" left")));
		else
			list.add(Component.text("Time's up!", NamedTextColor.YELLOW));

		return list;
	}

	public void setOne(Player one) {
		if (this.gameState != GameState.PREGAME)
			throw new IllegalStateException("Too late!");

		this.onePlayer = one;
	}

	// debug
	public static Set<Player> getAlreadyChosen() {
		return alreadyChosen.keySet();
	}

	@Override
	public void updateSidebar(Player player, SidebarManager sidebar) {

	}

	@Override
	public boolean canSelectKitNow(Player player) {
		return this.gameState.isPreGame();
	}

	@Override
	public boolean canSelectTeamNow() {
		return false;
	}

	@Override
	public boolean isRespawningGame() {
		return false;
	}

	@Override
	public Component getGameName() {
		return GAME_NAME;
	}

	@Override
	public Component getHowToPlayBrief() {
		return HOW_TO_PLAY;
	}

	@Override
	public String getDebugAntiStall() {
		return "";
	}

	@Override
	public void setDebugAntiStall(int antiStallCountdown) {

	}
}

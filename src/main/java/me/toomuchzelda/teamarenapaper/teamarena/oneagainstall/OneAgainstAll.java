package me.toomuchzelda.teamarenapaper.teamarena.oneagainstall;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitOne;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterAction;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterRule;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class OneAgainstAll extends TeamArena {
	private static final Component GAME_NAME = Component.text("One Against All", GameType.OAA.shortName.color());

	private static final FilterRule ONE_RULE = new FilterRule("oaa/one_team", "One team restrictions", FilterAction.allow(KitOne.KEY));
	private static final FilterRule ALL_RULE = new FilterRule("oaa/all_team", "All team restrictions", FilterAction.block(KitOne.KEY));

	private final Kit kitOne;

	private TeamArenaTeam oneTeam;
	private TeamArenaTeam allTeam;

	private double onePlayerHealth; // max health determined by amount of players
	private AttributeModifier onePlayerHealthModifier;

	private Player onePlayer; // null until teams decided
	private double oneKills = 0d;
	private Entity oneKiller; // player (or not) who kills the one
	private final Map<Player, Double> damagers = new HashMap<>();

	public OneAgainstAll(TeamArenaMap map) {
		super(map);

		this.kitOne = this.kits.get(KitOne.KEY);
	}

	@Override
	protected void loadConfig(TeamArenaMap map) {
		super.loadConfig(map);

		assert this.teams.length == 2;
		this.oneTeam = this.teams[0];
		this.allTeam = this.teams[1];
	}

	@Override
	public void prepTeamsDecided() {
		super.prepTeamsDecided();

		assert this.oneTeam.getPlayerMembers().size() == 1;
		this.onePlayer = this.oneTeam.getRandomPlayer();

		// announce stuff
		final Component allAgainstPlayer = Component.text("All against ", NamedTextColor.GOLD)
			.append(this.onePlayer.playerListName())
			.append(Component.text("!", NamedTextColor.GOLD));
		final Component allAgainstYou = Component.text("All against ", NamedTextColor.DARK_RED).append(Component.text("YOU", NamedTextColor.DARK_RED, TextDecoration.UNDERLINED));

		for (Player player : this.getPlayers()) {
			if (player == this.onePlayer) {
				Main.getPlayerInfo(player).kit = this.kitOne;
				this.informOfTeam(player, allAgainstYou);
			}
			else
				this.informOfTeam(player, allAgainstPlayer);
		}
		Bukkit.broadcast(allAgainstPlayer);
	}

	@Override
	public TeamArenaTeam addToLowestTeam(Player player, boolean add) {
		final TeamArenaTeam chosen;
		if (this.oneTeam.getPlayerMembers().isEmpty())
			chosen = this.oneTeam;
		else
			chosen = this.allTeam;

		if (add)
			chosen.addMembers(player);

		return chosen;
	}

	@Override
	public void prepLive() {
		super.prepLive();

		// extra 10 hearts for every enemy
		final double extra = this.allTeam.getPlayerMembers().size() * 20;
		this.onePlayerHealth = 20d + extra;
		this.onePlayerHealthModifier = new AttributeModifier(
			new NamespacedKey(Main.getPlugin(), "oaa/one_max_health_modifier"),
			extra,
			AttributeModifier.Operation.ADD_NUMBER
		);

		final AttributeInstance attribute = this.onePlayer.getAttribute(Attribute.MAX_HEALTH);
		assert attribute != null;
		attribute.addModifier(onePlayerHealthModifier);
		this.onePlayer.setHealth(attribute.getValue());
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

	@Override
	public void addKillAmount(Player player, double amount, Player victim) {
		super.addKillAmount(player, amount, victim);

		if (victim == this.onePlayer) {
			if (amount == 1.0d)
				this.oneKiller = player;

			if (this.damagers.put(player, amount) != null) {
				Main.logger().warning(victim.getName() + " was killed more than once by " + player.getName() + " for amount " + amount);
			}
		}
		else if (player == this.onePlayer) {
			this.oneKills += amount;
		}
	}

	@Override
	public void prepEnd() {
		super.prepEnd();

		if (this.winningTeam == this.allTeam) {
			final var builder = Component.text();

			if (this.oneKiller != null) {
				final Component msg = Component.textOfChildren(
					EntityUtils.getComponent(this.oneKiller),
					Component.text(" dealt the final blow"),
					Component.newline()
				);
				builder.append(msg);
			}

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
				final double damage = (killAmount * this.onePlayerHealth) / 2d;
				final Component msg = Component.textOfChildren(
					maxKiller.playerListName(),
					Component.text(" dealt the most damage at " ),
					Component.text(TextUtils.formatNumber(damage, 2)),
					TextColors.HEART,
					Component.newline()
				);
				builder.append(msg);
			}

			builder.append(Component.textOfChildren(
				EntityUtils.getComponent(this.onePlayer),
				Component.text(" killed " + TextUtils.formatNumber(this.oneKills, 2) + " "),
				this.allTeam.getComponentName(),
				Component.text(" players")
			));

			Bukkit.broadcast(builder.build());
		}
		else if (this.winningTeam == this.oneTeam) {
			final double health = this.onePlayer.getHealth();
			final Component msg = Component.textOfChildren(
				EntityUtils.getComponent(this.onePlayer),
				Component.text(" had " + TextUtils.formatNumber(health / 2d, 2)),
				TextColors.HEART,
				Component.text(" left")
			);
			Bukkit.broadcast(msg);
		}
	}

	@Override
	protected void applyKitFilters() {
		KitFilter.addGlobalRule(TeamArena.NO_HNS);
		KitFilter.addTeamRule(this.oneTeam.getSimpleName(), ONE_RULE);
		KitFilter.addTeamRule(allTeam.getSimpleName(), ALL_RULE);
	}

	@Override
	protected void removeKitFilters() {
		KitFilter.removeGlobalRule(TeamArena.NO_HNS.key());
		KitFilter.removeTeamRule(this.oneTeam.getSimpleName(), ONE_RULE.key());
		KitFilter.removeTeamRule(allTeam.getSimpleName(), ALL_RULE.key());
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
		return this.gameState == GameState.PREGAME;
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
		return Component.text("TODO");
	}

	@Override
	public String getDebugAntiStall() {
		return "";
	}

	@Override
	public void setDebugAntiStall(int antiStallCountdown) {

	}
}

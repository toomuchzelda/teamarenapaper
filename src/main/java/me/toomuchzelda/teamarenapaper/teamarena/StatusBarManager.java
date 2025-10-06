package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Methods for handling the Status Bars in Team Arena
 *
 * @author toomuchzelda
 */
public class StatusBarManager
{
	/**
	 * Show the player's in-game status bar
	 * Teams must have been decided at this point (TeamArena gamestate must be TEAMS_DECIDED or later)
	 */
	static void initStatusBar(Player player, PlayerInfo pinfo) {
		StatusBarHologram hologram = pinfo.statusBar;
		if(hologram != null) {
			hologram.remove();
		}
		hologram = new StatusBarHologram(player);
		hologram.respawn();

		pinfo.statusBar = hologram;
	}

	static void removeStatusBar(Player player, PlayerInfo pinfo) {
		if(pinfo.statusBar != null) {
			pinfo.statusBar.remove();
			pinfo.statusBar = null;
		}
	}

	static void setBarText(PlayerInfo pinfo, Component text) {
		if(pinfo.statusBar != null) {
			pinfo.statusBar.setText(text, true);
		}
	}

	static class StatusBarHologram extends AttachedPacketHologram
	{
		private static final Component PREGAME_TEXT = Component.text("I love Team Arena!");
		/** Cached RGB text */
		private static final List<Component> PREGAME_TEXT_RGB = IntStream.range(0, 20)
			.mapToObj(i -> TextUtils.getRGBManiacComponent(PREGAME_TEXT, Style.empty(), i / 20d))
			.toList();

		// RGB effect uses a ton of CPU time, so limit who gets it.
		private final boolean preGameRgb;

		public StatusBarHologram(Player player) {
			super(Main.getPlayerInfo(player).statusIndicatorId, player, null,
				viewer -> Main.getGame().canSeeStatusBar(player, viewer), PREGAME_TEXT, false);

			this.setText(PREGAME_TEXT, false);
			PlayerInfo pinfo = Main.getPlayerInfo(player);
			// If they're mod or higher and have their tag displayed, else random 5% chance.
			this.preGameRgb = (pinfo.displayPermissionLevel && pinfo.hasPermission(PermissionLevel.MOD)) ||
				MathUtils.random.nextDouble() >= 0.95d;
		}

		@Override
		public void tick() {
			final int gameTick = TeamArena.getGameTick();

			final GameState gameState = Main.getGame().getGameState();
			if(gameState == GameState.PREGAME) {
				if (this.preGameRgb)
					this.setText(PREGAME_TEXT_RGB.get(gameTick % 20), true);
			}
			// Show health and other during the game
			else if(gameState == GameState.LIVE) {
				if(gameTick % 5 == 0) {
					final Player player = (Player) this.entity;
					final Component heartChar;
					final double absorp = player.getAbsorptionAmount();
					if(absorp > 0) {
						heartChar = TextColors.YELLOW_HEART;
					}
					else {
						heartChar = TextColors.HEART; // red heart
					}

					Component health = Component.text(MathUtils.round((player.getHealth() + absorp) / 2d, 2) + " ").append(heartChar);
					this.setText(health, true);
				}
			}
			// Selected kit during TEAMS_DECIDED and GAME_STARTING is done in TeamArena#selectKit()
			// and prepTeamsDecided()

			super.tick();
		}

		@Override
		public void setText(Component component, boolean sendPacket) {
			if (component != null)
				super.setText(component, sendPacket);
			else
				super.setText(PREGAME_TEXT, sendPacket);
		}
	}
}

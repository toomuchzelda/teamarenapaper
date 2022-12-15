package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.entity.Player;

import java.util.Objects;

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
	static void showStatusBar(Player player, PlayerInfo pinfo) {
		StatusBarHologram hologram = pinfo.statusBar;
		if(hologram != null) {
			hologram.remove();
		}
		hologram = new StatusBarHologram(player);
		hologram.respawn();

		pinfo.statusBar = hologram;
	}

	static void hideStatusBar(Player player, PlayerInfo pinfo) {
		if(pinfo.statusBar != null) {
			pinfo.statusBar.remove();
			pinfo.statusBar = null;
		}
	}

	static class StatusBarHologram extends AttachedPacketHologram
	{
		private static final Component PREGAME_TEXT = Component.text("I love Team Arena!");
		private static Component pregameComp;
		private Component currentText;

		// Called by TeamArena
		static void updatePregameText() {
			final double offset = (((double) TeamArena.getGameTick()) / 20d) % 1;
			pregameComp = TextUtils.getRGBManiacComponent(PREGAME_TEXT, Style.empty(), offset);
		}


		public StatusBarHologram(Player player) {
			super(player, null, viewer -> Main.getGame().canSeeStatusBar(player, viewer),
					PREGAME_TEXT);

			this.currentText = PREGAME_TEXT;
		}

		@Override
		public void tick() {
			final int gameTick = TeamArena.getGameTick();

			final GameState gameState = Main.getGame().getGameState();
			if(gameState == GameState.PREGAME) {
				this.setText(pregameComp, true);
			}
			// Show health and other during the game
			else if(gameState == GameState.LIVE) {
				if(gameTick % 5 == 0) {
					// TODO Show absorption hearts and use a yellow heart colour for them
					final double hearts = MathUtils.round(this.player.getHealth() / 2d, 2);
					Component health = Component.text(hearts + " ").append(TextColors.HEART);
					this.setText(health, true);
				}
			}
			// TODO Selected kit during TEAMS_DECIDED is done in TeamArena#selectKit()

			super.tick();
		}

		@Override
		public void setText(Component component, boolean sendPacket) {
			if(component != null && !Objects.equals(this.currentText, component)) {
				this.currentText = component;
				super.setText(this.currentText, sendPacket);
			}
		}
	}
}

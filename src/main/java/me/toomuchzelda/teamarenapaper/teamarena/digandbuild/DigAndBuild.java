package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class DigAndBuild extends TeamArena
{
	private static final Component GAME_NAME = Component.text("Dig and Build", NamedTextColor.DARK_GREEN);
	private static final Component HOW_TO_PLAY = Component.text("Dig your way around the map and break the enemies' " +
		"Life Ore!!", NamedTextColor.DARK_GREEN);
	public DigAndBuild(TeamArenaMap map) {
		super(map);
	}

	@Override
	public void updateSidebar(Player player, SidebarManager sidebar) {

	}

	@Override
	protected void loadConfig(TeamArenaMap map) {
		super.loadConfig(map);


	}

	@Override
	public boolean canSelectKitNow() {
		return !this.gameState.isEndGame();
	}

	@Override
	public boolean canSelectTeamNow() {
		return gameState == GameState.PREGAME;
	}

	@Override
	public boolean canTeamChatNow(Player player) {
		return gameState != GameState.PREGAME && gameState != GameState.DEAD;
	}

	@Override
	public boolean isRespawningGame() {
		return true;
	}

	@Override
	public Component getGameName() {
		return GAME_NAME;
	}

	@Override
	public Component getHowToPlayBrief() {
		return HOW_TO_PLAY;
	}
}

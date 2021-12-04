package me.toomuchzelda.teamarenapaper.teamarena;

public class KingOfTheHill extends TeamArena
{
	
	public KingOfTheHill() {
		super();
	}

	@Override
	public boolean canSelectKitNow() {
		return !gameState.isEndGame();
	}

	@Override
	public boolean canSelectTeamNow() {
		return gameState == GameState.PREGAME;
	}

	@Override
	public String mapPath() {
		return super.mapPath() + "KOTH";
	}
}

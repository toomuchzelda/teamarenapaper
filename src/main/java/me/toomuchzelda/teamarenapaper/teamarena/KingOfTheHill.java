package me.toomuchzelda.teamarenapaper.teamarena;

public class KingOfTheHill extends TeamArena
{
	
	public KingOfTheHill() {
		super();
	}

	//respawning game, can change kit at any time (change takes effect on respawn doe)
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

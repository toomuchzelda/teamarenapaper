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
	public String mapPath() {
		return super.mapPath() + "KOTH";
	}
}

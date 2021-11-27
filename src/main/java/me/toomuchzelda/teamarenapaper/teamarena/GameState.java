package me.toomuchzelda.teamarenapaper.teamarena;

public enum GameState
{
	PREGAME, TEAMS_CHOSEN, GAME_STARTING, LIVE, END, DEAD;
	
	public boolean isPreGame()
	{
		return this == PREGAME || this == TEAMS_CHOSEN || this == GAME_STARTING;
	}
	
	public boolean teamsChosen() {
		return this == TEAMS_CHOSEN || this == GAME_STARTING;
	}
	
	public boolean isEndGame()
	{
		return this == END || this == DEAD;
	}
}

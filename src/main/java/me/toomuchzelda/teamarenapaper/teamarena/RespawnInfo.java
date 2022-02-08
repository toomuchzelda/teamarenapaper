package me.toomuchzelda.teamarenapaper.teamarena;

public class RespawnInfo
{
	public int deathTime;
	public boolean interrupted;
	
	public RespawnInfo(int time) {
		deathTime = time;
		interrupted = false;
	}
}

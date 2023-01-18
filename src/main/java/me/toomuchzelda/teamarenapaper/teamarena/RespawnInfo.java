package me.toomuchzelda.teamarenapaper.teamarena;

public class RespawnInfo
{
	public int deathTime;
	public final int selectedSlot;
	public boolean interrupted;

	public RespawnInfo(int time) {
		this(time, -1);
	}

	public RespawnInfo(int time, int selectedSlot) {
		deathTime = time;
		this.selectedSlot = selectedSlot;
		interrupted = false;
	}
}

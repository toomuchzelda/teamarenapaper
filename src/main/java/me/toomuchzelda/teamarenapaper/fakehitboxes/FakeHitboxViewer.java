package me.toomuchzelda.teamarenapaper.fakehitboxes;

import org.bukkit.entity.Player;

public class FakeHitboxViewer
{
	//if the viewer's client has the real player spawned
	boolean isSeeingRealPlayer;
	//if the viewer is in range to see and is seeing the fake hitboxes
	boolean isSeeingHitboxes;

	FakeHitboxViewer() {
		this.isSeeingRealPlayer = false;
		this.isSeeingHitboxes = false;
	}

	public void setSeeingRealPlayer(boolean seeing) {
		this.isSeeingRealPlayer = seeing;
	}

	public boolean isSeeingHitboxes() {
		return isSeeingHitboxes;
	}

	public void setSeeingHitboxes(boolean seeing) {
		this.isSeeingHitboxes = seeing;
	}
}

package me.toomuchzelda.teamarenapaper.fakehitboxes;

public class FakeHitboxViewer
{
	//if the viewer's client has the real player spawned
	boolean isSeeingRealPlayer;
	//if the viewer is in range to see and is seeing the fake hitboxes
	boolean isSeeingHitboxes;
	int hitboxSpawnTime;

	FakeHitboxViewer() {
		this.isSeeingRealPlayer = false;
		this.isSeeingHitboxes = false;
		this.hitboxSpawnTime = 0;
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

	public int getHitboxSpawnTime() {
		return this.hitboxSpawnTime;
	}

	public void setHitboxSpawnTime(int time) {
		this.hitboxSpawnTime = time;
	}
}

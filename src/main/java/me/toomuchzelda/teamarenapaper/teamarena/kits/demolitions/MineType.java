package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

public enum MineType
{
	TNTMINE("TNT Mine", 3, 20, 40),
	PUSHMINE("Push Mine", 3, 20, 40);

	final String name;
	final int damageToKill;
	final int timeToDetonate;
	final int timeToDetonateRemote;

	private MineType(String name, int dmg, int timeToDetonate, int timeToDetonateRemote) {
		this.name = name;
		this.damageToKill = dmg;
		this.timeToDetonate = timeToDetonate;
		this.timeToDetonateRemote = timeToDetonateRemote;
	}
}

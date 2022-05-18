package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

public enum MineType
{
	TNTMINE("TNT Mine", 3, 20, 30, 30 * 20),
	PUSHMINE("Push Mine", 3, 20, 30, 30 * 20);

	final String name;
	final int damageToKill;
	final int timeToDetonate;
	final int timeToDetonateRemote;
	final int timeToRegen;

	private MineType(String name, int dmg, int timeToDetonate, int timeToDetonateRemote, int timeToRegen) {
		this.name = name;
		this.damageToKill = dmg;
		this.timeToDetonate = timeToDetonate;
		this.timeToDetonateRemote = timeToDetonateRemote;
		this.timeToRegen = timeToRegen;
	}
}

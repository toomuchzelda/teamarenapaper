package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

public enum MineType
{
	TNTMINE("TNT Mine", 3),
	PUSHMINE("Push Mine", 3);
	
	final String name;
	final int damageToKill;
	
	private MineType(String name, int dmg) {
		this.name = name;
		this.damageToKill = dmg;
	}
}

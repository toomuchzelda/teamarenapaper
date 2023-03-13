package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

public enum MineType
{
	TNTMINE("TNT Mine", TNTMine::new, 3, 12, 25, 30 * 20),
	PUSHMINE("Push Mine", PushMine::new, 3, 12, 25, 30 * 20);

	final String name;
	final BiFunction<Player, Block, ? extends DemoMine> constructor;
	final int damageToKill;
	final int timeToDetonate;
	final int timeToDetonateRemote;
	final int timeToRegen;

	MineType(String name, BiFunction<Player, Block, ? extends DemoMine> constructor, int dmg, int timeToDetonate, int timeToDetonateRemote, int timeToRegen) {
		this.name = name;
		this.constructor = constructor;
		this.damageToKill = dmg;
		this.timeToDetonate = timeToDetonate;
		this.timeToDetonateRemote = timeToDetonateRemote;
		this.timeToRegen = timeToRegen;
	}
}

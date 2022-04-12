package me.toomuchzelda.teamarenapaper.utils;

import org.bukkit.Sound;

public class SoundUtils
{
	public static Sound getRandomSound() {
		return Sound.values()[MathUtils.random.nextInt(Sound.values().length)];
	}
}

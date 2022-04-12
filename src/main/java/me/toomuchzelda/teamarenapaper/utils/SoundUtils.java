package me.toomuchzelda.teamarenapaper.utils;

import org.bukkit.Sound;

import java.util.ArrayList;

public class SoundUtils {
	public static Sound getRandomSound() {
		return Sound.values()[MathUtils.random.nextInt(Sound.values().length)];
	}

	private static final ArrayList<Sound> OBNOXIOUS_SOUNDS = new ArrayList<>();
	static {
		for (Sound sound : Sound.values()) {
			var name = sound.name();
			if (name.startsWith("BLOCK_ANVIL")) {
				OBNOXIOUS_SOUNDS.add(sound);
			} else if (name.startsWith("MUSIC") || name.startsWith("UI")) {
				OBNOXIOUS_SOUNDS.add(sound);
			} else if (name.startsWith("ENTITY_GHAST") || name.startsWith("ENTITY_GOAT") || name.startsWith("ENTITY_HORSE")) {
				OBNOXIOUS_SOUNDS.add(sound);
			} else if (name.contains("EXPLODE") || name.contains("DEATH")) {
				OBNOXIOUS_SOUNDS.add(sound);
			}
		}
	}

	// very funny - toomuchzelda
	public static Sound getRandomObnoxiousSound() {
		return OBNOXIOUS_SOUNDS.get(MathUtils.random.nextInt(OBNOXIOUS_SOUNDS.size()));
	}
}

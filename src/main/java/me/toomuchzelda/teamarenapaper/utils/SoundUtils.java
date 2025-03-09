package me.toomuchzelda.teamarenapaper.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.ArrayList;

public class SoundUtils {
    public static Sound getRandomSound() {
        return Sound.values()[MathUtils.random.nextInt(Sound.values().length)];
    }

    private static final ArrayList<Sound> OBNOXIOUS_SOUNDS = new ArrayList<>();
    static {
        for (Sound sound : Registry.SOUND_EVENT) {
			NamespacedKey key = Registry.SOUND_EVENT.getKey(sound);
			if (key == null)
				continue;
			var name = key.getKey();
            if (name.startsWith("block_anvil")) {
                OBNOXIOUS_SOUNDS.add(sound);
            } else if (name.startsWith("music") || name.startsWith("ui")) {
                OBNOXIOUS_SOUNDS.add(sound);
            } else if (name.startsWith("entity_ghast") || name.startsWith("entity_goat") || name.startsWith("entity_horse")) {
                OBNOXIOUS_SOUNDS.add(sound);
            } else if (name.contains("explode") || name.contains("death") || name.contains("horn")) {
                OBNOXIOUS_SOUNDS.add(sound);
            }
        }
		OBNOXIOUS_SOUNDS.trimToSize();
    }

    // very funny - toomuchzelda
    public static Sound getRandomObnoxiousSound() {
        return OBNOXIOUS_SOUNDS.get(MathUtils.random.nextInt(OBNOXIOUS_SOUNDS.size()));
	}
}

package me.toomuchzelda.teamarenapaper.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class SoundUtils {
    public static Sound getRandomSound() {
		net.minecraft.core.Registry<SoundEvent> registry = ((CraftServer) Bukkit.getServer()).getHandle().getServer()
			.registryAccess().lookupOrThrow(Registries.SOUND_EVENT);
		Holder.Reference<SoundEvent> randomSound = registry.getRandom(RandomSource.create()).orElseThrow();
		return CraftSound.minecraftHolderToBukkit(randomSound);
    }

    private static final ArrayList<Sound> OBNOXIOUS_SOUNDS = new ArrayList<>();
    static {
        for (Sound sound : Registry.SOUND_EVENT) {
			NamespacedKey key = Registry.SOUND_EVENT.getKey(sound);
			if (key == null)
				continue;
			var name = key.getKey();
            if (name.startsWith("block.anvil")) {
                OBNOXIOUS_SOUNDS.add(sound);
            } else if (name.startsWith("music") || name.startsWith("ui")) {
                OBNOXIOUS_SOUNDS.add(sound);
            } else if (name.startsWith("entity.ghast") || name.startsWith("entity.goat") || name.startsWith("entity.horse")) {
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

	/**
	 * Plays a sound with a minimum volume similar to /playsound.
	 */
	public static void playSoundWithMinVolume(Player player, Location location, Sound sound, SoundCategory category, float volume, float pitch, long seed, float minVolume) {
		// attenuation distance for all sounds not from vanilla gameplay is 16
		Location playerLocation = player.getLocation();
		if (minVolume > 0 && playerLocation.distanceSquared(location) > 16 * 16) {
			location = location.clone().subtract(playerLocation);
			location.multiply(2 / location.length()).add(playerLocation);
			volume = minVolume;
		}
		player.playSound(location, sound, category, volume, pitch, seed);
	}
}

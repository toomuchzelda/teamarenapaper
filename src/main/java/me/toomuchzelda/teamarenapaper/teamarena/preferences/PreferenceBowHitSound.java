package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import org.bukkit.Sound;

import java.util.LinkedList;

public class PreferenceBowHitSound extends Preference<Sound>
{
	public PreferenceBowHitSound() {
		super("BowHitSound", "Sound that plays when you hit a bow shot on an entity", Sound.ENTITY_ARROW_HIT_PLAYER, new LinkedList<>());
		for(Sound s : Sound.values()) {
			tabSuggestions.add(s.name());
		}
	}
	
	@Override
	public Sound validateArgument(String arg) throws IllegalArgumentException {
		try {
			return Sound.valueOf(arg.toUpperCase());
		}
		catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("Sound " + arg + " doesn't exist");
		}
	}
}

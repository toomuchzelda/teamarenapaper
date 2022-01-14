package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import org.bukkit.Sound;

import java.util.LinkedList;

public class PreferenceBowHitSound extends Preference<Sound>
{
	public static final String NAME = "Bow shot hit sound";
	public static final String DESCRIPTION = "The sound you hear when you hit a bow shot. Check out https://papermc.io/javadocs/paper/1.18/org/bukkit/Sound.html for all of them, or use the autocomplete";
	public static final LinkedList<String> TAB_SUGGESTIONS;
	
	static {
		TAB_SUGGESTIONS = new LinkedList<>();
		for(Sound sound : Sound.values()) {
			TAB_SUGGESTIONS.add(sound.name());
		}
	}
	
	public PreferenceBowHitSound(Sound value) {
		super(value);
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public String getDescription() {
		return DESCRIPTION;
	}
	
	@Override
	public LinkedList<String> tabCompleteList() {
		return TAB_SUGGESTIONS;
	}
}

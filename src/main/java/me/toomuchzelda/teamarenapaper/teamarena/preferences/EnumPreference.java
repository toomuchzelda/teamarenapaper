package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import java.util.ArrayList;

public enum EnumPreference
{
	BOW_HIT_SOUND(new PreferenceBowHitSound()),
	HEARTS_FLASH_DAMAGE(new PreferenceHeartsFlashDamage()),
	HEARTS_FLASH_REGEN(new PreferenceHeartsFlashRegen()),
	DAMAGE_TILT(new PreferenceDamageTilt()),
	KIT_ACTION_BAR(new PreferenceKitActionBar()),
	KIT_CHAT_MESSAGES(new PreferenceKitChatMessages()),
	KOTH_HILL_PARTICLES(new PreferenceKothHillParticles()),
	RECEIVE_GAME_TITLES(new PreferenceReceiveGameTitles());
	
	public static final int SIZE = values().length;
	public static final ArrayList<String> TAB_SUGGESTIONS;
	
	static {
		TAB_SUGGESTIONS = new ArrayList<>(SIZE);
		for(EnumPreference pref : values()) {
			TAB_SUGGESTIONS.add(pref.name());
		}
	}
	
	public final Preference<?> preference;
	
	private EnumPreference(Preference<?> pref) {
		this.preference = pref;
	}
}

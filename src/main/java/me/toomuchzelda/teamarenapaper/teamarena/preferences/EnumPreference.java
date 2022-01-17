package me.toomuchzelda.teamarenapaper.teamarena.preferences;

public enum EnumPreference
{
	BOW_HIT_SOUND(new PreferenceBowHitSound()),
	DAMAGE_TILT(new PreferenceDamageTilt()),
	KIT_ACTION_BAR(new PreferenceKitActionBar()),
	KIT_CHAT_MESSAGES(new PreferenceKitChatMessages()),
	KOTH_HILL_PARTICLES(new PreferenceKothHillParticles()),
	RECEIVE_GAME_TITLES(new PreferenceReceiveGameTitles());
	
	public static final int SIZE = values().length;
	
	public final Preference<?> preference;
	
	private EnumPreference(Preference<?> pref) {
		this.preference = pref;
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.preferences;

public class PreferenceHeartsFlashDamage extends Preference<Boolean>
{
	public PreferenceHeartsFlashDamage() {
		super(EnumPreference.getId(), "HeartsFlashDamage", "If your hearts should flash when taking damage", true, BOOLEAN_SUGGESTIONS);
	}
}

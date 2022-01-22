package me.toomuchzelda.teamarenapaper.teamarena.preferences;

public class PreferenceHeartsFlashRegen extends Preference<Boolean>
{
	public PreferenceHeartsFlashRegen() {
		super("HeartsFlashRegen", "If your hearts should flash while regenerating", true, BOOLEAN_SUGGESTIONS);
	}
}

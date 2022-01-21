package me.toomuchzelda.teamarenapaper.teamarena.preferences;

public class PreferenceReceiveGameTitles extends Preference<Boolean>
{
	public PreferenceReceiveGameTitles() {
		super(EnumPreference.getId(), "GameplayTitles", "Receive titles that cover up part of your screen during gameplay (you will also get a chat message regardless)", true, BOOLEAN_SUGGESTIONS);
	}
}

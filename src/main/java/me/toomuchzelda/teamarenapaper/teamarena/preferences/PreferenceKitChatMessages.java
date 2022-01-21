package me.toomuchzelda.teamarenapaper.teamarena.preferences;

public class PreferenceKitChatMessages extends Preference<Boolean>
{
	public PreferenceKitChatMessages() {
		super(EnumPreference.getId(), "Kit chat messages", "Receive kit-related messages in chat", true, BOOLEAN_SUGGESTIONS);
	}
}

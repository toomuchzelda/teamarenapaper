package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import me.toomuchzelda.teamarenapaper.core.WrappedBoolean;

public class PreferenceKitChatMessages extends Preference<WrappedBoolean>
{
	
	public PreferenceKitChatMessages(WrappedBoolean value) {
		super(value);
	}
	
	@Override
	public String getName() {
		return "Kit chat messages";
	}
	
	@Override
	public String getDescription() {
		return "Receive kit-related messages in chat";
	}
}

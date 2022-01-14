package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import me.toomuchzelda.teamarenapaper.core.WrappedBoolean;

public class PreferenceKitActionBar extends Preference<WrappedBoolean>
{
	
	public PreferenceKitActionBar(WrappedBoolean value) {
		super(value);
	}
	
	@Override
	public String getName() {
		return "Kit Action bar messages";
	}
	
	@Override
	public String getDescription() {
		return "Receive kit-related messages in the action bar slot (The little text space above your hotbar)";
	}
}

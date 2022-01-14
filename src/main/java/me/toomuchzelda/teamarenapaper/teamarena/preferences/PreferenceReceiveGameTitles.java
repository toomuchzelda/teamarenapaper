package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import me.toomuchzelda.teamarenapaper.core.WrappedBoolean;

public class PreferenceReceiveGameTitles extends Preference<WrappedBoolean>
{
	public static final String NAME = "Receive Titles in gameplay";
	public static final String DESCRIPTION = "If you want to receive titles that cover up part of your screen during gameplay (you will also get a chat message regardless)";
	
	public PreferenceReceiveGameTitles(WrappedBoolean value) {
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
}

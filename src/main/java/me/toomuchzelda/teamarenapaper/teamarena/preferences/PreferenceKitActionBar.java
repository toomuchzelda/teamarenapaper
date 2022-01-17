package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import java.util.LinkedList;

public class PreferenceKitActionBar extends Preference<Boolean>
{
	public PreferenceKitActionBar() {
		super("KitActionBar", "Receive kit-related messages in the Action bar slot (the little text space above your hotbar)", true, BOOLEAN_SUGGESTIONS);
	}
}

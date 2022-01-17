package me.toomuchzelda.teamarenapaper.teamarena.preferences;


import java.util.LinkedList;

public class PreferenceDamageTilt extends Preference<Boolean>
{
	public PreferenceDamageTilt() {
		super("DamageTilt", "Whether your screen should tilt in pain when taking damage", true, Preference.BOOLEAN_SUGGESTIONS);
	}
}

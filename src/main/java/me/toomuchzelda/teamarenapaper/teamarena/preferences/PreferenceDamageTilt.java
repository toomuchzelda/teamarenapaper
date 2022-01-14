package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import me.toomuchzelda.teamarenapaper.core.WrappedBoolean;

public class PreferenceDamageTilt extends Preference<WrappedBoolean>
{
	public PreferenceDamageTilt(WrappedBoolean value) {
		super(value);
	}
	
	@Override
	public String getName() {
		return "Damage Tilts screen";
	}
	
	@Override
	public String getDescription() {
		return "Whether your screen should tilt slightly when taking damage (does not affect the sound)";
	}
}

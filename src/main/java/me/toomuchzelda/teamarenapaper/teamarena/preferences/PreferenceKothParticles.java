package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import me.toomuchzelda.teamarenapaper.core.WrappedByte;

public class PreferenceKothParticles extends Preference<WrappedByte>
{
	public static final byte LOWEST_KOTH_PARTICLES = 10;
	public static final String NAME = "KoTH Hill Particles";
	public static final String DESCRIPTION = "The number of ticks in between each spawning of particles along the Hill border in King of the Hill";
	
	public PreferenceKothParticles(WrappedByte value) {
		super(value);
	}
	
	@Override
	public void setValue(WrappedByte value) {
		//just set the value, don't throw an exception in case LOWEST_KOTH_PARTICLES changes in the future
		// it would invalidate many database entries
		if(value.value > LOWEST_KOTH_PARTICLES)
			value.value = LOWEST_KOTH_PARTICLES;
		else if(value.value < 1)
			value.value = 1;
		
		super.setValue(value);
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
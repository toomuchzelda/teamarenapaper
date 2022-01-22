package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import java.util.LinkedList;

public class PreferenceKothHillParticles extends Preference<Byte>
{
	public static final byte LOWEST_KOTH_PARTICLES = 10;
	
	public PreferenceKothHillParticles() {
		super("KoTHHillParticles", "The number of ticks in between each spawning of particles along the Hill border in King of the Hill. Minimum 1, Max " + LOWEST_KOTH_PARTICLES, (byte) 1, new LinkedList<>());
		tabSuggestions.add("1 - " + LOWEST_KOTH_PARTICLES + " inclusive");
	}
	
	@Override
	public Byte validateArgument(String arg) throws IllegalArgumentException {
		Byte b = super.validateArgument(arg);
		if(b > LOWEST_KOTH_PARTICLES || b < 1) {
			throw new IllegalArgumentException("Must be between 1 and " + LOWEST_KOTH_PARTICLES + " inclusive");
		}
		
		return b;
	}
}
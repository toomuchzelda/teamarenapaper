package me.toomuchzelda.teamarenapaper.teamarena.kits.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class KitOptions
{
	public static boolean kitSniper = false;
	public static boolean sniperAccuracy = false;

	// Keys must not have spaces and be type-able with a keyboard.
	private static final Map<String, Runnable> OPTION_TOGGLE_FUNCS;

	static {
		OPTION_TOGGLE_FUNCS = new HashMap<>();

		OPTION_TOGGLE_FUNCS.put("kitSniper", () -> kitSniper = !kitSniper);
		OPTION_TOGGLE_FUNCS.put("sniperAccuracy", () -> sniperAccuracy = !sniperAccuracy);
	}

	/** @return true if toggling was successful. */
	public static boolean toggleOption(String option) {
		Runnable r = OPTION_TOGGLE_FUNCS.get(option);
		if (r == null)
			return false;

		r.run();
		return true;
	}

	public static Collection<String> getOptions() {
		return OPTION_TOGGLE_FUNCS.keySet();
	}
}

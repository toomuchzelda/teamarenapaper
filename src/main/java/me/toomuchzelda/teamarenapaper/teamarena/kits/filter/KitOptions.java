package me.toomuchzelda.teamarenapaper.teamarena.kits.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class KitOptions
{
	public static boolean kitSniper;
	public static boolean sniperAccuracy;

	// These flags used to change behaviour of kits to be more old style
	public static boolean rwfKits;

	public static boolean ghostAetherial;
	public static boolean burstShowArrows;
	public static boolean ninjaFastAttack;
	public static boolean pyroMolotov;

	private static void resetToDefault() {
		kitSniper = false; sniperAccuracy = false;

		rwfKits = true;

		ghostAetherial = false;
		burstShowArrows = false;
		ninjaFastAttack = false;
		pyroMolotov = false;
	}

	// Keys must not have spaces and be type-able with a keyboard.
	private static final Map<String, Runnable> OPTION_TOGGLE_FUNCS;

	static {
		resetToDefault();

		OPTION_TOGGLE_FUNCS = new HashMap<>();

		OPTION_TOGGLE_FUNCS.put("kitSniper", () -> kitSniper = !kitSniper);
		OPTION_TOGGLE_FUNCS.put("sniperAccuracy", () -> sniperAccuracy = !sniperAccuracy);

		OPTION_TOGGLE_FUNCS.put("reset", KitOptions::resetToDefault);

		OPTION_TOGGLE_FUNCS.put("teamArenaKits", () -> {
			rwfKits = false; ghostAetherial = true; burstShowArrows = false; ninjaFastAttack = true;
			pyroMolotov = true;
		});

		OPTION_TOGGLE_FUNCS.put("rwfKits", () -> rwfKits = !rwfKits);
		OPTION_TOGGLE_FUNCS.put("ghostAetherial", () -> ghostAetherial = !ghostAetherial);
		OPTION_TOGGLE_FUNCS.put("burstShowArrows", () -> burstShowArrows = !burstShowArrows);
		OPTION_TOGGLE_FUNCS.put("ninjaFastAttack", () -> ninjaFastAttack = !ninjaFastAttack);
		OPTION_TOGGLE_FUNCS.put("pyroMolotov", () -> pyroMolotov = !pyroMolotov);
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

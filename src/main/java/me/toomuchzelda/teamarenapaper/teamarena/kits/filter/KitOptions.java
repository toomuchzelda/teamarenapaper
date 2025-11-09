package me.toomuchzelda.teamarenapaper.teamarena.kits.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class KitOptions
{
	public static boolean sniperCooldown;
	public static boolean sniperAccuracy;

	public static boolean ghostAetherial;
	public static boolean burstShowArrows;
	public static boolean ninjaFastAttack;
	public static boolean ninjaSlowFall;
	public static boolean pyroMolotov;
	public static boolean rewindClockPhases;
	public static boolean rewindStasis;
	public static boolean splitterVisible;

	private static void resetToDefault() {
		sniperCooldown = true;
		sniperAccuracy = true;

		ghostAetherial = false;
		burstShowArrows = false;
		ninjaFastAttack = false;
		ninjaSlowFall = true;
		pyroMolotov = false;
		rewindClockPhases = false;
		rewindStasis = false;
		splitterVisible = false;
	}

	// Keys must not have spaces and be type-able with a keyboard.
	private static final Map<String, Runnable> OPTION_TOGGLE_FUNCS;

	static {
		resetToDefault();

		OPTION_TOGGLE_FUNCS = new HashMap<>();

		OPTION_TOGGLE_FUNCS.put("sniperCooldown", () -> sniperCooldown = !sniperCooldown);
		OPTION_TOGGLE_FUNCS.put("sniperAccuracy", () -> sniperAccuracy = !sniperAccuracy);

		OPTION_TOGGLE_FUNCS.put("resetToDefault", KitOptions::resetToDefault);

		OPTION_TOGGLE_FUNCS.put("teamArenaKits", () -> {
			ghostAetherial = true; burstShowArrows = false; ninjaFastAttack = true;
			pyroMolotov = true; rewindClockPhases = true; rewindStasis = true;
		});

		OPTION_TOGGLE_FUNCS.put("rwfKits", KitOptions::resetToDefault);
		OPTION_TOGGLE_FUNCS.put("ghostAetherial", () -> ghostAetherial = !ghostAetherial);
		OPTION_TOGGLE_FUNCS.put("burstShowArrows", () -> burstShowArrows = !burstShowArrows);
		OPTION_TOGGLE_FUNCS.put("ninjaFastAttack", () -> ninjaFastAttack = !ninjaFastAttack);
		OPTION_TOGGLE_FUNCS.put("ninjaSlowFall", () -> ninjaSlowFall = !ninjaSlowFall);
		OPTION_TOGGLE_FUNCS.put("pyroMolotov", () -> pyroMolotov = !pyroMolotov);
		OPTION_TOGGLE_FUNCS.put("rewindClockPhases", () -> rewindClockPhases = !rewindClockPhases);
		OPTION_TOGGLE_FUNCS.put("rewindStasis", () -> rewindStasis = !rewindStasis);
		OPTION_TOGGLE_FUNCS.put("splitterVisible", () -> splitterVisible = !splitterVisible);
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

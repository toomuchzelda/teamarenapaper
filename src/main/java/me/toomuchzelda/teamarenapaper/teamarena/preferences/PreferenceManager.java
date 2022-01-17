package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import java.util.HashMap;
import java.util.UUID;

public class PreferenceManager
{
	private static final HashMap<UUID, ValuesAndTime> PLAYER_PREFVALUE_MAP = new HashMap<>();
	
	public record ValuesAndTime(Object[] values, long time) {}
	
	
	public static void putData(UUID uuid, Object[] values) {
		PLAYER_PREFVALUE_MAP.put(uuid, new ValuesAndTime(values, System.currentTimeMillis()));
	}
	
	public static Object[] getAndRemoveData(UUID uuid) {
		return PLAYER_PREFVALUE_MAP.remove(uuid).values();
	}
}

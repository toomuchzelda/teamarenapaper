package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PreferenceManager
{
	private static final ConcurrentHashMap<UUID, ValuesAndTime> PLAYER_PREFVALUE_MAP = new ConcurrentHashMap<>();
	
	public record ValuesAndTime(Object[] values, long time) {}
	
	
	public static void putData(UUID uuid, Object[] values) {
		PLAYER_PREFVALUE_MAP.put(uuid, new ValuesAndTime(values, System.currentTimeMillis()));
	}
	
	public static Object[] getAndRemoveData(UUID uuid) {
		return PLAYER_PREFVALUE_MAP.remove(uuid).values();
	}
}

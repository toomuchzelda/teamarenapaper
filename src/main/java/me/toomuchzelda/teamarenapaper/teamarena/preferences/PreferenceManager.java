package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class PreferenceManager {
	// TODO load data from database whatever
	public static CompletableFuture<Map<Preference<?>, ?>> fetchPreferences(UUID uuid) {
		/*return CompletableFuture.supplyAsync(() -> {
			// do database query and get SQL injection query
		});*/
		// empty map to indicate that no preferences have been overwritten
		return CompletableFuture.completedFuture(Collections.emptyMap());
	}
}

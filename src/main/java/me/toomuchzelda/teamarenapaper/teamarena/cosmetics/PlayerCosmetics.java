package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.sql.DBGetSelectedCosmetics;
import me.toomuchzelda.teamarenapaper.sql.DBSetSelectedCosmetics;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;

public class PlayerCosmetics implements CosmeticsProvider {
	private final UUID uuid;
	public PlayerCosmetics(UUID uuid) {
		this.uuid = uuid;
	}

	private final Map<CosmeticType, Set<NamespacedKey>> ownedCosmetics = Collections.synchronizedMap(new EnumMap<>(CosmeticType.class));
	private final Map<CosmeticType, Set<NamespacedKey>> selectedCosmetics = Collections.synchronizedMap(new EnumMap<>(CosmeticType.class));

	public void fetch() {
		selectedCosmetics.clear();
		for (CosmeticType type : CosmeticType.values()) {
			var query = new DBGetSelectedCosmetics(uuid, type);
			try {
				var selected = query.run();
				selectedCosmetics.put(type, new HashSet<>(selected));
			} catch (SQLException ex) {
				Main.logger().warning("Failed to fetch %s's selected %ss!".formatted(uuid, type));
			}
		}
	}

	public void save() {
		for (CosmeticType type : CosmeticType.values()) {
			var selected = selectedCosmetics.get(type);
			var query = new DBSetSelectedCosmetics(uuid, type, selected);
			try {
				query.run();
			} catch (SQLException ex) {
				Main.logger().warning("Failed to save %s's selected %ss!".formatted(uuid, type));
			}
		}
	}

	@Override
	public @Nullable CosmeticsProvider getParent() {
		return CosmeticsProvider.DEFAULT;
	}

	@Override
	public boolean hasCosmeticItem(CosmeticType type, NamespacedKey key) {
		return ownedCosmetics.getOrDefault(type, Set.of()).contains(key);
	}

	@Override
	public Set<NamespacedKey> getCosmeticItems(CosmeticType type) {
		return ownedCosmetics.getOrDefault(type, Set.of());
	}

	@Override
	public @Nullable Collection<NamespacedKey> getSelectedCosmetic(CosmeticType type) {
		return selectedCosmetics.get(type);
	}
	@Override
	public void setSelectedCosmetic(@NotNull CosmeticType type, @Nullable Set<NamespacedKey> key) {
		if (key != null)
			selectedCosmetics.put(type, new HashSet<>(key));
		else
			selectedCosmetics.remove(type);
	}

	public void addSelectedCosmetic(@NotNull CosmeticType type, NamespacedKey key) {
		selectedCosmetics.computeIfAbsent(type, ignored -> new HashSet<>()).add(key);
	}

	public void removeSelectedCosmetic(@NotNull CosmeticType type, NamespacedKey key) {
		var selected = selectedCosmetics.get(type);
		if (selected != null)
			selected.remove(key);
	}
}

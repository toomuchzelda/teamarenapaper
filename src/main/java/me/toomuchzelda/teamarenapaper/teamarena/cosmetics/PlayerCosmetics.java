package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.sql.DBGetSelectedCosmetics;
import me.toomuchzelda.teamarenapaper.sql.DBSetSelectedCosmetics;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;

public class PlayerCosmetics implements CosmeticsProvider {
	private final UUID uuid;
	private final boolean isOp;

	private boolean graffitiReminder;
	private final IntSet viewedMapIds = new IntOpenHashSet();

	public PlayerCosmetics(UUID uuid, CustomCommand.PermissionLevel permissionLevel) {
		this.uuid = uuid;
		this.isOp = permissionLevel == CustomCommand.PermissionLevel.OWNER;
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
			save(type);
		}
	}

	public void save(CosmeticType type) {
		var selected = selectedCosmetics.get(type);
		var query = new DBSetSelectedCosmetics(uuid, type, selected);
		try {
			query.run();
		} catch (SQLException ex) {
			Main.logger().warning("Failed to save %s's selected %ss!".formatted(uuid, type));
		}
	}

	public void scheduleSave(CosmeticType type) {
		Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> save(type));
	}

	// Graffiti utility methods
	private Player getPlayer() {
		return Objects.requireNonNull(Bukkit.getPlayer(uuid), "Player " + uuid + " is offline?");
	}

	public void remindGraffiti() {
		if (graffitiReminder)
			return;
		graffitiReminder = true;
		getPlayer().sendMessage(Component.textOfChildren(
			Component.text("You can pick a graffiti you like "),
			Component.text("here", Style.style(TextDecoration.UNDERLINED))
				.clickEvent(ClickEvent.runCommand("/cosmetics gui"))
				.hoverEvent(Component.text("Click to run /cosmetics gui", NamedTextColor.YELLOW).asHoverEvent()),
			Component.text("!")
		).color(NamedTextColor.AQUA));
	}

	public void sendMapView(MapView view) {
		if (viewedMapIds.add(view.getId())) {
			getPlayer().sendMap(view);
		}
	}

	@Override
	public @Nullable CosmeticsProvider getParent() {
		return isOp ? ALL : DEFAULT;
	}

	@Override
	public boolean hasCosmeticItem(CosmeticType type, NamespacedKey key) {
		return ownedCosmetics.getOrDefault(type, Set.of()).contains(key);
	}

	@Override
	public Set<NamespacedKey> getCosmeticItems(CosmeticType type) {
		return Collections.unmodifiableSet(ownedCosmetics.getOrDefault(type, Set.of()));
	}

	@Override
	public @Nullable Set<NamespacedKey> getSelectedCosmetic(CosmeticType type) {
		var selected = selectedCosmetics.get(type);
		return selected != null ? Collections.unmodifiableSet(selected) : null;
	}
	@Override
	public void setSelectedCosmetic(@NotNull CosmeticType type, @Nullable Collection<NamespacedKey> key) {
		if (key != null)
			selectedCosmetics.put(type, new HashSet<>(key));
		else
			selectedCosmetics.remove(type);
		scheduleSave(type);
	}

	public void selectCosmetic(@NotNull CosmeticType type, NamespacedKey key) {
		if (type.multiselect) {
			selectedCosmetics.computeIfAbsent(type, ignored -> new HashSet<>()).add(key);
		} else {
			selectedCosmetics.put(type, Set.of(key));
		}
		scheduleSave(type);
	}

	public void unselectCosmetic(@NotNull CosmeticType type, NamespacedKey key) {
		var selected = selectedCosmetics.get(type);
		if (type.multiselect) {
			if (selected != null)
				selected.remove(key);
		} else {
			if (selected.contains(key))
				selectedCosmetics.put(type, Set.of());
		}
		scheduleSave(type);
	}
}

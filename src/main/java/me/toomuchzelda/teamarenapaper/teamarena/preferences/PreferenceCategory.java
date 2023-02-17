package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public enum PreferenceCategory {
	ANNOUNCER("Announcer", ItemBuilder.of(Material.GOAT_HORN)
		.displayName(text("Announcer", RED))
		.lore(text("Announcer-related preferences", GRAY))
		.build()),
	CLIENTSIDE("Visual Effects", ItemBuilder.of(Material.SPYGLASS)
		.displayName(text("Visual Effects", YELLOW))
		.lore(text("Visual effects that don't affect gameplay", GRAY))
		.build()),
	GAMEPLAY("Gameplay", ItemBuilder.of(Material.COMMAND_BLOCK)
		.displayName(text("Gameplay", GREEN))
		.lore(text("Gameplay and gamemode specific settings", GRAY))
		.build()),
	;

	public final String name;
	private final ItemStack display;

	PreferenceCategory(String name, ItemStack display) {
		this.name = name;
		this.display = display;
	}

	public ItemStack display() {
		return display.clone();
	}
}

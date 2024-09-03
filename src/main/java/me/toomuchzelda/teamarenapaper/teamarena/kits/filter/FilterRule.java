package me.toomuchzelda.teamarenapaper.teamarena.kits.filter;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;

public record FilterRule(NamespacedKey key, String description, FilterAction action) {
	public FilterRule(String key, String description, FilterAction action) {
		this(new NamespacedKey(Main.getPlugin(), key), description, action);
	}

	Component toComponent() {
		return Component.textOfChildren(
			Component.text()
				.content(description)
				.color(NamedTextColor.GRAY)
				.hoverEvent(Component.text(key.toString()))
				.clickEvent(ClickEvent.copyToClipboard(key.toString())),
			Component.text(": "),
			action.toComponent()
		);
	}
}

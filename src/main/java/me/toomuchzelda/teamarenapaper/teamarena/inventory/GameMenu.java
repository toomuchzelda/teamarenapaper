package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class GameMenu implements InventoryProvider {
	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text("Game menu");
	}

	@Override
	public int getRows() {
		return 3;
	}

	private static Consumer<InventoryClickEvent> openInventory(Supplier<? extends InventoryProvider> constructor) {
		return e -> Inventories.openInventory((Player) e.getWhoClicked(), constructor.get());
	}

	public static final ClickableItem PREFERENCES_ITEM = ItemBuilder.from(PreferencesInventory.PREFERENCE.clone())
		.displayName(Component.text("Manage preferences", NamedTextColor.WHITE))
		.toClickableItem(openInventory(PreferencesInventory::new));

	public static final ClickableItem COSMETICS_ITEM = ItemBuilder.of(Material.ARMOR_STAND)
		.displayName(Component.text("Manage cosmetics", NamedTextColor.LIGHT_PURPLE))
		.toClickableItem(openInventory(() -> new CosmeticsInventory(CosmeticType.GRAFFITI)));

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		inventory.fill(MenuItems.BORDER);
		inventory.set(9 + 2, PREFERENCES_ITEM);
		inventory.set(9 + 4, COSMETICS_ITEM);
		// show name of latest update
		var builder = ItemBuilder.of(Material.LECTERN)
			.displayName(Component.text("See what's new", NamedTextColor.GREEN));
		if (ChangelogMenu.changelogs != null && ChangelogMenu.changelogs.size() != 0) {
			ChangelogMenu.Changelog latestUpdate = ChangelogMenu.changelogs.get(0);
			builder.lore(Component.textOfChildren(
				Component.text("Latest update: ", NamedTextColor.GRAY),
				Component.text(latestUpdate.title(), NamedTextColor.GOLD)
			));
		}
		inventory.set(9 + 6, builder.toClickableItem(openInventory(ChangelogMenu::new)));
	}
}

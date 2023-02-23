package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.inventory.ClickableItem;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.InventoryProvider;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BuildingInventory implements InventoryProvider {

	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text("Click Building to Destroy");
	}

	@Override
	public int getRows() {
		return 1;
	}

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		buildPDA(player, inventory);
	}

	@Override
	public void update(Player player, InventoryAccessor inventory) {
		if (TeamArena.getGameTick() % 10 == 0) {
			inventory.invalidate();
		}
	}

	public void buildPDA(Player player, InventoryAccessor inventory) {
		var playerLocation = player.getLocation();
		int slot = 0;
		for (Building building : BuildingManager.getAllPlayerBuildings(player)) {
			double distance = playerLocation.distance(building.getLocation());
			var itemStack = ItemBuilder.from(building.getIcon())
					.displayName(Component.text(building.name, NamedTextColor.BLUE))
					.lore(Component.text(
							TextUtils.formatNumber(distance) + " blocks away", NamedTextColor.WHITE
					))
					.build();

			inventory.set(slot++, ClickableItem.of(itemStack, e -> {
				BuildingManager.destroyBuilding(building);
				Inventories.closeInventory(player);
			}));
		}
	}
}

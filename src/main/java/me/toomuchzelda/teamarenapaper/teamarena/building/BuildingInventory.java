package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.inventory.ClickableItem;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.InventoryProvider;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.engineer.Sentry;
import me.toomuchzelda.teamarenapaper.teamarena.kits.engineer.Teleporter;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BuildingInventory implements InventoryProvider {

	@Override
	public Component getTitle(Player player) {
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
			var material = Material.BARRIER;
			if (building instanceof Sentry) {
				material = Material.BOW;
			} else if (building instanceof Teleporter) {
				material = Material.HONEYCOMB_BLOCK;
			}

			double distance = playerLocation.distance(building.getLocation());
			var itemStack = ItemBuilder.of(material)
					.displayName(Component.text(building.name, NamedTextColor.BLUE))
					.lore(Component.text(
							TextUtils.TWO_DECIMAL_POINT.format(distance) + " blocks away", NamedTextColor.WHITE
					))
					.build();

			inventory.set(slot++, ClickableItem.of(itemStack, e -> {
				BuildingManager.destroyBuilding(building);
				Inventories.closeInventory(player);
			}));
		}
	}
}

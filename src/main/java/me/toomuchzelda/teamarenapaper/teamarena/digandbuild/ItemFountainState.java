package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;

public class ItemFountainState {
	private final World world;
	private final Location location;
	private final int interval;
	private final int maxItems;
	private final List<ItemStack> sequence;
	private final Component name;
	private final PointMarker hologram;

	private int elapsed;
	private int index;
	public ItemFountainState(DigAndBuild game, World gameWorld, DigAndBuildInfo.ItemFountain itemFountain) {
		world = gameWorld;
		location = itemFountain.at().toLocation(gameWorld);
		interval = itemFountain.interval();
		maxItems = itemFountain.maxItems();
		sequence = itemFountain.sequence().stream()
			.map(ref -> ref.resolve(game))
			.toList();

		if (itemFountain.customName() != null) {
			name = itemFountain.customName();

			Location hologramLocation = itemFountain.hologram() != null ?
				itemFountain.hologram().toLocation(world) :
				location.clone().add(0, 2.5, 0);

			hologram = new PointMarker(hologramLocation, itemFountain.customName(),
				itemFountain.customName().color() != null ? Color.fromRGB(itemFountain.customName().color().value()) : Color.WHITE,
				sequence.getFirst().getType());
		} else {
			name = null;
			hologram = null;
		}
	}

	public void tick() {
		if (elapsed++ == interval) {
			elapsed = 0;
			ItemStack toDrop = sequence.get(index);
			// check dropped items
			Collection<Item> items = location.getNearbyEntitiesByType(Item.class, 5);
			int count = 0;
			for (Item item : items) {
				ItemStack stack = item.getItemStack();
				if (stack.isSimilar(toDrop)) {
					count += stack.getAmount();
				}
			}
			if (count < maxItems) {
				world.dropItem(location, toDrop);
			}
			index = (index + 1) % sequence.size();
		}
		if (hologram != null) {
			hologram.setText(Component.textOfChildren(
				name,
				Component.newline(),
				Component.text("Next item in " + (interval - elapsed) / 20 + " seconds")
			));
		}
	}
}

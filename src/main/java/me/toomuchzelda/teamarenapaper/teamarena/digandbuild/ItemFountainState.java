package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemFountainState {
	private final World world;
	private final Location location;
	private final int interval;
	private final List<ItemStack> sequence;

	private int elapsed;
	private int index;
	public ItemFountainState(DigAndBuild game, World gameWorld, DigAndBuildInfo.ItemFountain itemFountain) {
		world = gameWorld;
		location = itemFountain.at().toLocation(gameWorld);
		interval = itemFountain.interval();
		sequence = itemFountain.sequence().stream()
			.map(ref -> ref.resolve(game))
			.toList();
	}

	public void tick() {
		if (elapsed++ == interval) {
			elapsed = 0;
			world.dropItem(location, sequence.get(index));
			index = (index + 1) % sequence.size();
		}
	}
}

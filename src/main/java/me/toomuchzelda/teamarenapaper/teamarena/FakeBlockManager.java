package me.toomuchzelda.teamarenapaper.teamarena;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.math.BlockPosition;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;

public class FakeBlockManager {

	private final TeamArena game;

	public static final long INVALID_KEY = 0;
	private static long keyCtr = 0; // unique key for each fakeblock register-er
	private static final class FakeBlock {
		private final BlockData blockData;
		private final Predicate<Player> viewerRule;
		private final long key;

		private FakeBlock(BlockData blockData, Predicate<Player> viewerRule) {
			this.blockData = blockData;
			this.viewerRule = viewerRule;
			this.key = ++keyCtr;
		}

		@Override
		public String toString() { return "FakeBlock[nmsState=" + blockData + "]"; }
	}

	private final Map<BlockCoords, List<FakeBlock>> fakeBlocks;

	public FakeBlockManager(TeamArena game) {
		this.game = game;
		this.fakeBlocks = new HashMap<>();
	}

	private record PosAndBlockData(BlockCoords coords, BlockData data) {}

	/** Add a fake block */
	public long setFakeBlock(BlockCoords coords, BlockData nmsBlockState, Predicate<Player> viewerRule) {
		FakeBlock fakeBlock = new FakeBlock(nmsBlockState, viewerRule);

		List<FakeBlock> list = fakeBlocks.computeIfAbsent(coords, c -> new ArrayList<>(2));
		list.add(fakeBlock);

		for (Player viewer : Bukkit.getOnlinePlayers()) {
			if (!coords.hasLoaded(viewer)) continue;

			ArrayList<PosAndBlockData> toSend = new ArrayList<>(2);
			for (FakeBlock fb : list) {
				if (fb.viewerRule.test(viewer)) {
					toSend.add(new PosAndBlockData(coords, fb.blockData));
				}
			}

			if (toSend.size() > 1) {
				Main.logger().severe("> 1 FakeBlock registered for " + viewer + " at coords " + coords +
					". List: " + toSend);
				while (toSend.size() > 1) {
					toSend.removeLast();
				}
			}

			sendBlockChanges(this.game, viewer, toSend);
		}

		return fakeBlock.key;
	}

	public void injectFakeBlocks(PlayerChunkLoadEvent event) {
		final Chunk chunk = event.getChunk();
		final Player viewer = event.getPlayer();

 		ArrayList<PosAndBlockData> list = null; // lazy init
		for (var entry : this.fakeBlocks.entrySet()) {
			BlockCoords bc = entry.getKey();
			for (FakeBlock fb : entry.getValue()) {
				if (bc.isInChunk(chunk) && fb.viewerRule.test(viewer)) {
					if (list == null)
						list = new ArrayList<>(2);

					list.add(new PosAndBlockData(bc, fb.blockData));
				}
			}
		}

		if (list != null) {
			sendBlockChanges(this.game, viewer, list);
		}
	}

	/** Try to send block changes efficiently */
	private static void sendBlockChanges(TeamArena game, Player viewer,
										 List<PosAndBlockData> posAndBlockData) {
		if (!posAndBlockData.isEmpty()) {
			if (posAndBlockData.size() == 1) {
				PosAndBlockData pair = posAndBlockData.getFirst();
				viewer.sendBlockChange(pair.coords.toLocation(game.getWorld()), pair.data);
			}
			else {
				Map<BlockPosition, BlockData> map = HashMap.newHashMap(posAndBlockData.size());
				for (var pair : posAndBlockData) {
					BlockData previous = map.put(pair.coords.toPaperBlockPos(), pair.data);
					if (previous != null) {
						Main.logger().warning("Multiple Fakeblocks in same coordinates registered for " + viewer +
							", pair: " + pair + ". Was previously " + previous);
					}
				}

				viewer.sendMultiBlockChange(map);
			}
		}
	}

	/** @return true if a FakeBlock was removed */
	public boolean removeFakeBlock(final BlockCoords coords, long key) {
		if (key == INVALID_KEY) {
			Main.logger().severe("Invalid key provided!");
			Thread.dumpStack();
			return false;
		}

		List<FakeBlock> fakeBlocks = this.fakeBlocks.get(coords);
		boolean found = false;

		if (fakeBlocks != null) {
			var iter = fakeBlocks.iterator();
			while (iter.hasNext()) {
				FakeBlock fb = iter.next();

				if (fb.key == key) {
					iter.remove();

					Block block = coords.toBlock(this.game.getWorld());
					Location loc = block.getLocation();
					for (Player viewer : Bukkit.getOnlinePlayers()) {
						if (coords.hasLoaded(viewer) && fb.viewerRule.test(viewer)) {
							viewer.sendBlockChange(loc, block.getBlockData());
						}
					}

					found = true;
					break;
				}
			}

			if (fakeBlocks.isEmpty()) {
				this.fakeBlocks.remove(coords);
			}
		}

		return found;
	}

	public void removeAll(boolean send) {
		if (!send) {
			this.fakeBlocks.clear();
			return;
		}

		var fakeBlocksIter = this.fakeBlocks.entrySet().iterator();
		while (fakeBlocksIter.hasNext()) {
			var fakeBlockEntry = fakeBlocksIter.next();
			fakeBlocksIter.remove();
			final BlockCoords coords = fakeBlockEntry.getKey();
			final List<FakeBlock> fakeBlocks = fakeBlockEntry.getValue();

			var iter = fakeBlocks.iterator();
			while (iter.hasNext()) {
				FakeBlock fb = iter.next();
				iter.remove();

				Block block = coords.toBlock(this.game.getWorld());
				Location loc = block.getLocation();
				for (Player viewer : Bukkit.getOnlinePlayers()) {
					if (coords.hasLoaded(viewer) && fb.viewerRule.test(viewer)) {
						viewer.sendBlockChange(loc, block.getBlockData());
					}
				}
			}
		}
	}
}

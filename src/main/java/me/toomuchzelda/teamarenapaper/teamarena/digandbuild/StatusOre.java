package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public class StatusOre
{
	private static final BlockData MINED_DATA = Material.COBBLESTONE.createBlockData();

	private final StatusOreType type;
	private final World world;
	private final BlockCoords position;
	private int lastBreakTime;
	private boolean ready;

	private final BlockData originalBlock;

	public StatusOre(StatusOreType type, BlockCoords position, World world) {
		this.type = type;
		this.position = position;
		this.world = world;
		this.lastBreakTime = 0;
		this.ready = true;

		this.originalBlock = this.position.toBlock(this.world).getBlockData();
	}

	public StatusOreType getType() {
		return this.type;
	}

	/** @return true if was successfully broken and the breaker should be rewarded the ore */
	public boolean onBreak(Player breaker) {
		if (Main.getGame().isDead(breaker)) return false;
		if (!this.ready) return false;

		final Block block = this.position.toBlock(this.world);
		if (block.isPreferredTool(breaker.getEquipment().getItemInMainHand())) {
			this.lastBreakTime = TeamArena.getGameTick();
			this.ready = false;

			Bukkit.getOnlinePlayers().forEach(viewer -> {
				if (viewer != breaker) ParticleUtils.blockBreakEffect(viewer, block);
			});

			block.setBlockData(MINED_DATA);

			return true;
		}

		return false;
	}

	/**
	 * After regen time has passed reset the block to original state.
	 * Use this.ready so we're not setting the block data every tick.
	 */
	public void tick() {
		final int currentTick = TeamArena.getGameTick();
		if (!this.ready && currentTick - this.lastBreakTime >= this.type.regenTime) {
			assert CompileAsserts.OMIT ||
				currentTick - this.lastBreakTime == this.type.regenTime: "Shouldn't be > this.type.regenTime. " +
				"Means this wasn't called for some reason in a previous tick.";

			this.position.toBlock(this.world).setBlockData(this.originalBlock);
			this.ready = true;
		}
	}
}

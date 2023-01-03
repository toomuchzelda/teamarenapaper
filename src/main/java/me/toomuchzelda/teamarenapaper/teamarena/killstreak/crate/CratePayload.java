package me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents the payload of a {@link FallingCrate}
 */
public sealed interface CratePayload {

	sealed interface SimplePayload extends CratePayload permits SimpleBlock, SimpleEntity {}

	record SimpleEntity(@NotNull EntityType entityType) implements SimplePayload {}

	record SimpleBlock(@NotNull BlockData blockData) implements SimplePayload {}

	record Group(@NotNull Vector anchorOffset, @NotNull Map<Vector, ? extends SimplePayload> children) implements CratePayload {
		public Group {
			children = Map.copyOf(children);
		}
	}

}

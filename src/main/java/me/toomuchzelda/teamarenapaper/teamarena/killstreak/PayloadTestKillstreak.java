package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate.CratePayload;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class PayloadTestKillstreak extends CratedKillStreak {
	PayloadTestKillstreak() {
		super("Amogus", "amogus", TextColor.color(216, 212, 213), null);
	}

	private static final BlockData RED = Material.RED_CONCRETE.createBlockData();
	private static final BlockData BLUE = Material.LIGHT_BLUE_CONCRETE.createBlockData();
	private static final String ART = """
		 ...
		..xx
		....
		....
		 . .""";

	private static final CratePayload CRATE_PAYLOAD = createPayloadFromString(ART, Map.of('.', RED, 'x', BLUE));
	@Override
	public @NotNull CratePayload getPayload(Player player, Location destination) {
		return CRATE_PAYLOAD;
	}

	@Override
	public @NotNull ItemStack createCrateItem(Player player) {
		return createSimpleCrateItem(Material.BOOKSHELF);
	}

	public static CratePayload createPayloadFromString(String string, Map<Character, BlockData> mappings) {
		Map<Vector, CratePayload.SimpleBlock> blocks = new LinkedHashMap<>();
		int width = string.length() - string.lastIndexOf('\n') - 1;
		double xOffset = width / 2d - 0.5;
		String[] lines = string.lines().toArray(String[]::new);
		for (int i = 0; i < lines.length; i++) {
			int y = lines.length - i - 1;
			String line = lines[i];
			for (int j = 0; j < line.length(); j++) {
				var mapping = mappings.get(line.charAt(j));
				if (mapping == null)
					continue;
				Vector offset = new Vector(j - xOffset, y, 0);
				blocks.put(offset, new CratePayload.SimpleBlock(mapping));
			}
		}
		return new CratePayload.Group(new Vector(0, lines.length, 0), blocks);
	}
}

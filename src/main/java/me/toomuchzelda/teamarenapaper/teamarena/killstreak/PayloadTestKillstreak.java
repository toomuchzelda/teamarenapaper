package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate.CratePayload;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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

	record Note(int delay, int uses, Instrument instrument) {}
	private static Note note(int delay, int uses) {
		return new Note(delay, uses, Instrument.PIANO);
	}
	private static final List<Note> SONG = List.of(
			new Note(0, 6, Instrument.BASS_GUITAR),
			note(6, 6),
			note(6, 9),
			note(6, 11),
			note(6, 12),
			note(6, 11),
			note(6, 9),
			note(6, 6),
			note(18, 4),
			note(3, 8),
			note(3, 6),
			new Note(12, 1, Instrument.BASS_GUITAR),
			new Note(12, 6, Instrument.BASS_GUITAR),
			note(12, 6),
			note(6, 9),
			note(6, 11),
			note(6, 12),
			note(6, 11),
			note(6, 9),
			note(6, 12),
			note(24, 12),
			note(3, 11),
			note(3, 9),
			note(3, 12),
			note(3, 11),
			note(3, 9)
	);

	@Override
	public void onCrateLand(Player player, Location destination) {
		World world = destination.getWorld();
		Iterator<Note> iterator = SONG.iterator();
		new BukkitRunnable() {
			Note nextNote;
			int ticksElapsed;
			@Override
			public void run() {
				if (nextNote == null) {
					if (!iterator.hasNext()) {
						cancel();
						return;
					}
					nextNote = iterator.next();
				}
				if (ticksElapsed >= nextNote.delay) {
					Sound sound = nextNote.instrument() == Instrument.BASS_GUITAR ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.BLOCK_NOTE_BLOCK_HARP;
					double pitch = Math.pow(2, (-12 + nextNote.uses) / 12d);
					world.playSound(destination, sound, 1f, (float) pitch);

					ticksElapsed = 1;
					nextNote = null;
				} else {
					ticksElapsed++;
				}
			}
		}.runTaskTimer(Main.getPlugin(), 0, 1);
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

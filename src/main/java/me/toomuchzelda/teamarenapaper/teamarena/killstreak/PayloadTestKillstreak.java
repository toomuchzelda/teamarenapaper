package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import com.google.common.io.LittleEndianDataInputStream;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate.CratePayload;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;

public class PayloadTestKillstreak extends CratedKillStreak {
	PayloadTestKillstreak() {
		super(KillStreakManager.KillStreakID.PAYLOAD_TEST, "Amogus", "amogus", TextColor.color(216, 212, 213), null, Material.BOOKSHELF);
	}

	private static final BlockData RED = Material.RED_CONCRETE.createBlockData();
	private static final BlockData BLUE = Material.LIGHT_BLUE_CONCRETE.createBlockData();
	private static final String ART = """
		 ...
		..xx
		....
		....
		 . .""";

	private static final CratePayload.Group CRATE_PAYLOAD = createPayloadFromString(ART, Map.of('.', RED, 'x', BLUE));
	@Override
	public @NotNull CratePayload getPayload(Player player, Location destination) {
		return CRATE_PAYLOAD;
	}

	@Override
	public void onCrateLand(Player player, Location destination) {
		World world = destination.getWorld();

		List<BlockState> modifiedBlocks = new ArrayList<>();
		CRATE_PAYLOAD.children().forEach((offset, blockPayload) -> {
			Location blockLocation = destination.clone().add(offset);
			Block block = blockLocation.getBlock();
			modifiedBlocks.add(block.getState());
			block.setBlockData(((CratePayload.SimpleBlock) blockPayload).blockData(), false);
		});

		playAmogus(destination, world);
	}

	public static void playAmogus(Location destination, World world) {
		try {
			TreeMap<Integer, List<NbsNote>> song = loadSong(Main.getPlugin().getResource("sus.nbs"));
			BukkitScheduler scheduler = Bukkit.getScheduler();
			song.forEach((ticks, notes) -> {
				scheduler.runTaskLater(Main.getPlugin(), () -> {
					for (NbsNote note : notes) {
						note.play(world, destination);
					}
				}, ticks);
			});
		} catch (IOException ex) {
			Main.logger().log(Level.SEVERE, "Failed to load amogus song :(", ex);
		}
	}

	public record NbsNote(byte instrument, byte key, byte velocity) {
		private static final Instrument[] INSTRUMENT_MAP = {
			Instrument.PIANO,
			Instrument.BASS_GUITAR,
			Instrument.BASS_DRUM,
			Instrument.SNARE_DRUM,
			Instrument.STICKS,
			Instrument.GUITAR,
			Instrument.FLUTE,
			Instrument.BELL,
			Instrument.CHIME,
			Instrument.XYLOPHONE,
			Instrument.IRON_XYLOPHONE,
			Instrument.COW_BELL,
			Instrument.DIDGERIDOO,
			Instrument.BIT,
			Instrument.BANJO,
			Instrument.PLING
		};
		public Instrument getInstrument() {
			return INSTRUMENT_MAP[instrument % 16];
		}

		public float getPitch() {
			return (float) Math.pow(2, (key - 45) / 12f);
		}

		public void play(World world, Location location) {
			world.playSound(location, getInstrument().getSound(), SoundCategory.PLAYERS, 1, getPitch());
		}
	}

	/**
	 * Simple .nbs loader
	 * @param inputStream The input stream
	 * @return A map of delay in ticks to a list of notes to play in that tick
	 * @author jacky
	 */
	public static TreeMap<Integer, List<NbsNote>> loadSong(InputStream inputStream) throws IOException {
		TreeMap<Integer, List<NbsNote>> map = new TreeMap<>();
		try (var is = new LittleEndianDataInputStream(inputStream)) {
			// part 1: header
			is.skipNBytes(8);
			// name, author, original author, description
			for (int i = 0; i < 4; i++) {
				int length = is.readInt();
				is.skipNBytes(length);
			}
			short tempo = is.readShort();
			double tickMultiplier = 20d / (tempo / 100d);
			is.skipNBytes(3 + 20);
			int length = is.readInt();
			is.skipNBytes(length);
			is.skipNBytes(4);
			// part 2: note blocks
			int ticks = -1;
			while (true) {
				short nextTick = is.readShort();
				if (nextTick == 0)
					break;
				ticks += nextTick;
				int actualTicks = (int) Math.round(ticks * tickMultiplier);

				while (true) {
					short nextLayer = is.readShort();
					if (nextLayer == 0)
						break;
					map.computeIfAbsent(actualTicks, ignored -> new ArrayList<>()).add(
						new NbsNote(is.readByte(), is.readByte(), is.readByte())
					);
					is.skipNBytes(3);
				}
			}
		}
		return map;
	}

	public static CratePayload.Group createPayloadFromString(String string, Map<Character, BlockData> mappings) {
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

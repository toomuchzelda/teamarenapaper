package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import com.google.common.io.LittleEndianDataInputStream;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate.CratePayload;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
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

		playAmogus(world, destination);
	}

	public static void playAmogus(World world, Location location) {
		try {
			var song = loadSong(Main.getPlugin().getResource("sus.nbs"));
			new NbsSongPlayer(song, location).schedule();
		} catch (IOException ex) {
			Main.logger().log(Level.SEVERE, "Failed to load amogus song :(", ex);
		}
	}

	public static void playAmogus(World world, Entity entity) {
		try {
			var song = loadSong(Main.getPlugin().getResource("sus.nbs"));
			new NbsSongPlayer(song, entity).schedule();
		} catch (IOException ex) {
			Main.logger().log(Level.SEVERE, "Failed to load amogus song :(", ex);
		}
	}

	public record NbsSong(double length, String name, String author, String originalAuthor, String description, String midiName, List<NbsTick> ticks) {
		@Override
		public String toString() {
			return "NbsSong{length=%.2f seconds, name=%s, author=%s, originalAuthor=%s, description=%s, midiName=%s, ticks=%d".formatted(
				length, name, author, originalAuthor, description, midiName, ticks.size()
			);
		}
	}

	public record NbsTick(int atTick, List<NbsNote> notes) {}

	public record NbsNote(byte instrument, byte key, byte velocity) {
		private static final Instrument[] INSTRUMENT_MAP = {Instrument.PIANO, Instrument.BASS_GUITAR, Instrument.BASS_DRUM,
			Instrument.SNARE_DRUM, Instrument.STICKS, Instrument.GUITAR, Instrument.FLUTE, Instrument.BELL, Instrument.CHIME,
			Instrument.XYLOPHONE, Instrument.IRON_XYLOPHONE, Instrument.COW_BELL, Instrument.DIDGERIDOO, Instrument.BIT,
			Instrument.BANJO, Instrument.PLING};
		public Instrument getInstrument() {
			return INSTRUMENT_MAP[instrument % 16];
		}

		public float getPitch() {
			return (float) Math.pow(2, (key - 45) / 12f);
		}

		public void play(World world, Location location) {
			world.playSound(location, Objects.requireNonNull(getInstrument().getSound()), SoundCategory.PLAYERS, 1, getPitch());
		}

		public void play(World world, Entity entity) {
			world.playSound(entity, Objects.requireNonNull(getInstrument().getSound()), SoundCategory.PLAYERS, 1, getPitch());
		}
	}

	/**
	 * Simple .nbs loader
	 * @param inputStream The input stream
	 * @return A map of delay in ticks to a list of notes to play in that tick
	 * @author jacky
	 */
	public static NbsSong loadSong(InputStream inputStream) throws IOException {
		String name, author, originalAuthor, description, midiName;
		double lengthInSeconds;
		List<NbsTick> song = new ArrayList<>();
		try (var is = new LittleEndianDataInputStream(inputStream)) {
			// part 1: header
			is.skipNBytes(4);
			short songLength = is.readShort();
			is.skipNBytes(2);
			name = readNbsString(is);
			author = readNbsString(is);
			originalAuthor = readNbsString(is);
			description = readNbsString(is);
			short tempo = is.readShort();
			lengthInSeconds = songLength / (tempo / 100d);
			double tickMultiplier = 20d / (tempo / 100d);
			is.skipNBytes(3 + 20);
			midiName = readNbsString(is);
			is.skipNBytes(4);
			// part 2: note blocks
			int ticks = -1;
			while (true) {
				short nextTick = is.readShort();
				if (nextTick == 0)
					break;
				ticks += nextTick;
				int actualTicks = (int) Math.round(ticks * tickMultiplier); // adjust the tick to the tempo

				var notes = new ArrayList<NbsNote>();
				while (true) {
					short nextLayer = is.readShort();
					if (nextLayer == 0)
						break;
					notes.add(new NbsNote(is.readByte(), is.readByte(), is.readByte()));
					is.skipNBytes(3);
				}

				song.add(new NbsTick(actualTicks, List.copyOf(notes)));
			}
			return new NbsSong(lengthInSeconds, name, author, originalAuthor, description, midiName, List.copyOf(song));
		}
	}

	private static String readNbsString(DataInput dis) throws IOException {
		int length = dis.readInt();
		char[] arr = new char[length];
		for (int i = 0; i < length; i++) {
			arr[i] = (char) dis.readUnsignedByte();
		}
		return new String(arr);
	}

	public static class NbsSongPlayer extends BukkitRunnable {
		private final NbsSong song;
		private final Iterator<NbsTick> iterator;
		private final World world;
		private final Entity entity;
		private final Location location;
		int elapsed;
		NbsTick tick;

		public NbsSongPlayer(NbsSong song, Location location) {
			this.song = song;
			this.iterator = song.ticks.iterator();
			this.world = location.getWorld();
			this.location = location;
			this.entity = null;
			elapsed = 0;
			Main.logger().info("Song " + song + " initialized");
		}

		public NbsSongPlayer(NbsSong song, Entity entity) {
			this.song = song;
			this.iterator = song.ticks.iterator();
			this.world = entity.getWorld();
			this.location = null;
			this.entity = entity;
			elapsed = 0;
			Main.logger().info("Song " + song + " initialized");
		}

		public void schedule() {
			runTaskTimer(Main.getPlugin(), 0, 1);
		}

		@Override
		public void run() {
			if (tick == null) {
				if (!iterator.hasNext()) {
					Main.logger().info("Song " + song + " ended");
					cancel();
					return;
				}
				tick = iterator.next();
			}
			if (tick.atTick <= elapsed) {
				for (NbsNote note : tick.notes) {
					if (entity != null)
						note.play(world, entity);
					else
						note.play(world, location);
				}
				tick = null;
			}
			elapsed++;
		}
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

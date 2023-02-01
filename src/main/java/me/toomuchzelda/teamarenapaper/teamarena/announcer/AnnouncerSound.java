package me.toomuchzelda.teamarenapaper.teamarena.announcer;

import me.toomuchzelda.teamarenapaper.Main;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;

/**
 * Class to represent a sound as declared in the resource pack's sounds.json
 * <p>
 * The sound names are declared as "type.soundname" in the sounds.json.
 * This is represented here as the Type enum and the name as a String.
 *
 * @author toomuchzelda
 */
public class AnnouncerSound {
	public static final String NAMESPACE = "tmaannouncer";
	static final List<AnnouncerSound> ALL_CHAT_SOUNDS = new ArrayList<>();
	// the typeAndName as the key
	private static final Map<String, AnnouncerSound> BY_TYPED_NAME = new LinkedHashMap<>();

	// game event sounds
	public static final AnnouncerSound GAME_A_WINNER_IS_YOU = new AnnouncerSound(Type.GAME, "awinnerisyou", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_BOMB_ARMED = new AnnouncerSound(Type.GAME, "bombarmed", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_BOMB_DETONATED = new AnnouncerSound(Type.GAME, "bombdetonated", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_BOMB_DISARMED = new AnnouncerSound(Type.GAME, "bombdisarmed", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_FLAG_CAPTURED = new AnnouncerSound(Type.GAME, "flagcaptured", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_FLAG_DROPPED = new AnnouncerSound(Type.GAME, "flagdropped", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_FLAG_RECOVERED = new AnnouncerSound(Type.GAME, "flagrecovered", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_FLAG_STOLEN = new AnnouncerSound(Type.GAME, "flagstolen", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_FLAG_TAKEN = new AnnouncerSound(Type.GAME, "flagtaken", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_FLAG_YOU_GOT_THE = new AnnouncerSound(Type.GAME, "flagyougotthe", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_FLAG_YOU_LOST_THE = new AnnouncerSound(Type.GAME, "flagyoulostthe", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_LAST_MAN_STANDING = new AnnouncerSound(Type.GAME, "lastmanstanding", MatchCriteria.NEVER_MATCH, false);

	public static final AnnouncerSound GAME_DOUBLE_KILL = new AnnouncerSound(Type.GAME, "doublekill", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_TRIPLE_KILL = new AnnouncerSound(Type.GAME, "triplekill", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_QUAD_KILL = new AnnouncerSound(Type.GAME, "quadkill", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound GAME_UNSTOPPABLE = new AnnouncerSound(Type.GAME, "unstoppable", MatchCriteria.NEVER_MATCH, false);

	public static final AnnouncerSound CHAT_DISGRACEFUL = new AnnouncerSound(Type.CHAT, "disgraceful", MatchCriteria.NEVER_MATCH, false);
	public static final AnnouncerSound CHAT_MY_GOD = new AnnouncerSound(Type.CHAT, "mygod", MatchCriteria.NEVER_MATCH, false, "my god", "omg");

	public final Type type;
	private final String typeAndName;
	private final String namespacedName;
	private final MatchCriteria matchCriteria;
	public final boolean isSwear;
	private final List<String> phraseNames;

	/**
	 * @param type The enum equivalent of the type in the sounds.json. E.g "chat.objection" is Type.CHAT
	 * @param name The name as defined in the sounds.json, excluding the type. E.g for "chat.objection", name = "objection"
	 * @param phraseNames Optional list of phrases that match to this AnnouncerSound when detected in a chat message.
	 *                    If none are specified, the name is used, replacing all underscores with spaces.
	 */
	private AnnouncerSound(Type type, String name, MatchCriteria matchCriteria, boolean swear, String... phraseNames) {
		this.type = type;

		this.typeAndName = this.type.asString + "." + name;
		this.namespacedName = NAMESPACE + ":" + this.typeAndName;

		this.matchCriteria = matchCriteria;
		this.isSwear = swear;
		if (phraseNames.length > 0) {
			this.phraseNames = List.of(phraseNames);
		}
		else {
			this.phraseNames = List.of(name.replace('_', ' '));
		}

		BY_TYPED_NAME.put(this.typeAndName, this);
		if (this.type.isChattable()) {
			ALL_CHAT_SOUNDS.add(this);
		}
	}

	private static String chopOffYml(String filename) {
		int index = filename.indexOf(".yml");
		return filename.substring(0, index);
	}

	private static String chopOffTypePrefix(String typeAndName) {
		return typeAndName.substring(0, typeAndName.indexOf('.') + 1);
	}

	// "type.name"
	// e.g: chat.objection
	public String getTypeAndName() {
		return this.typeAndName;
	}

	// full name
	// e.g: tmaannouncer:chat.objection
	public String getNamespacedName() {
		return this.namespacedName;
	}

	// string to check for in chat messages
	// e.g: name = "my_body_is_ready", phraseName = "my body is ready"
	// OR as custom defined in the constructor
	public List<String> getPhraseNames() {
		return this.phraseNames;
	}

	/**
	 * If a given string contains any of this AnnouncerSound's phraseNames
	 */
	public boolean stringMatchesPhrases(String string) {
		return this.matchCriteria.matchingFunction.test(this, string);
	}

	public static AnnouncerSound getByTypedName(String namespacedName) {
		return BY_TYPED_NAME.get(namespacedName);
	}

	public static Collection<String> getTypedNames() {
		return BY_TYPED_NAME.keySet();
	}

	public enum Type {
		GAME("game", 1f, false),
		CHAT("chat", 0.9f, true), // Chat lines slightly quieter than Game announcements
		NAME("chat", 0.8f, true);

		private final String asString;
		public final float volumeMult;
		private final boolean chat;
		private Type(String type, float volumeMult, boolean chat) {
			this.asString = type;
			this.volumeMult = volumeMult;
			this.chat = chat;
		}

		public boolean isChattable() {
			return chat;
		}
	}

	/**
	 * Criteria for matching phrases in messages
	 */
	private enum MatchCriteria {
		NEVER_MATCH((announcerSound, s) -> false),
		// Present anywhere in the string
		CONTAINS((announcerSound, string) -> {
			string = string.toLowerCase(Locale.ENGLISH);
			for (String phrase : announcerSound.phraseNames) {
				if (string.contains(phrase)) {
					return true;
				}
			}

			return false;
		}),
		// Present in the string and surrounded by non-alphabetic chars.
		// E.g: "objection"
		// "I have an objection." matches
		// "I have anobjection." doesn't.
		WHOLE_WORD((announcerSound, string) -> {
			string = string.toLowerCase(Locale.ENGLISH);
			for (String phrase : announcerSound.phraseNames) {
				final int index = string.indexOf(phrase);
				if (index != -1) { // if string contains phrase in whole then do char checks
					final int endIndex = index + phrase.length();
					// char at indexes are valid if out of bounds, or in bounds and not alphabetic
					if ((index - 1 < 0 || !Character.isAlphabetic(string.charAt(index - 1))) &&
						(endIndex >= string.length() || !Character.isAlphabetic(string.charAt(endIndex)))) {
						return true;
					}
				}
			}

			return false;
		}),
		// The phrase equals the whole message
		WHOLE_MESSAGE((announcerSound, string) -> {
			for (String phrase : announcerSound.phraseNames) {
				if (phrase.equalsIgnoreCase(string)) {
					return true;
				}
			}

			return false;
		});

		private final BiPredicate<AnnouncerSound, String> matchingFunction;
		MatchCriteria(BiPredicate<AnnouncerSound, String> matchingFunction) {
			this.matchingFunction = matchingFunction;
		}
	}

	static void init() {
		File[] folders = new File[]{new File("VoiceLines"), new File("VoiceLines/Names")};
		Type[] types = new Type[]{Type.CHAT, Type.NAME};

		for (int i = 0; i < folders.length; i++) {
			final File folder = folders[i];
			final Type type = types[i];
			File[] voices = folder.listFiles();

			if (voices == null || voices.length == 0) {
				Main.logger().info("No chat voice lines defined in " + folder.getName());
				return;
			}

			for (File voiceFile : voices) {
				if (voiceFile.isDirectory())
					continue;

				Yaml yaml = new Yaml();
				try (FileInputStream voiceFileReader = new FileInputStream(voiceFile)) {
					Map<String, Object> parsedMap = yaml.load(voiceFileReader);

					final String name = chopOffYml(voiceFile.getName());

					//Main.logger().info("name: " + name);

					MatchCriteria criteria;
					try {
						criteria = MatchCriteria.valueOf(((String) parsedMap.get("Criteria")).toUpperCase(Locale.ENGLISH));
					}
					catch (NullPointerException | ClassCastException e) {
						criteria = MatchCriteria.WHOLE_WORD;
					}

					boolean swear;
					try {
						swear = (Boolean) parsedMap.get("IsSwear");
					}
					catch (NullPointerException | ClassCastException e) {
						swear = false;
					}

					String[] phraseNames;
					try {
						List<String> list = (List<String>) parsedMap.get("Phrases");
						phraseNames = list.toArray(new String[0]);
					}
					catch (NullPointerException | ClassCastException e) {
						phraseNames = new String[]{name.replace('_', ' ')};
					}

					// Puts itself into static collections in constructor
					new AnnouncerSound(type, name, criteria, swear, phraseNames);
				}
				catch (IOException e) {
					Main.logger().warning("Bad voice config file " + voiceFile.getName() + " " + e.getMessage());
				}
			}
		}
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.announcer;

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
public enum AnnouncerSound {

	// game event sounds
	GAME_A_WINNER_IS_YOU(Type.GAME, "awinnerisyou", MatchCriteria.NEVER_MATCH, false),
	GAME_BOMB_ARMED(Type.GAME, "bombarmed", MatchCriteria.NEVER_MATCH, false),
	GAME_BOMB_DETONATED(Type.GAME, "bombdetonated", MatchCriteria.NEVER_MATCH, false),
	GAME_BOMB_DISARMED(Type.GAME, "bombdisarmed", MatchCriteria.NEVER_MATCH, false),
	GAME_FLAG_CAPTURED(Type.GAME, "flagcaptured", MatchCriteria.NEVER_MATCH, false),
	GAME_FLAG_DROPPED(Type.GAME, "flagdropped", MatchCriteria.NEVER_MATCH, false),
	GAME_FLAG_RECOVERED(Type.GAME, "flagrecovered", MatchCriteria.NEVER_MATCH, false),
	GAME_FLAG_STOLEN(Type.GAME, "flagstolen", MatchCriteria.NEVER_MATCH, false),
	GAME_FLAG_TAKEN(Type.GAME, "flagtaken", MatchCriteria.NEVER_MATCH, false),
	GAME_FLAG_YOU_GOT_THE(Type.GAME, "flagyougotthe", MatchCriteria.NEVER_MATCH, false),
	GAME_FLAG_YOU_LOST_THE(Type.GAME, "flagyoulostthe", MatchCriteria.NEVER_MATCH, false),
	GAME_LAST_MAN_STANDING(Type.GAME, "lastmanstanding", MatchCriteria.NEVER_MATCH, false),


	// chat word sounds
	CHAT_OBJECTION(Type.CHAT, "objection", MatchCriteria.CONTAINS, false),
	CHAT_LEEROY_JENKINS(Type.CHAT, "leeroy", MatchCriteria.CONTAINS, false),
	CHAT_MY_BODY_IS_READY(Type.CHAT, "mybodyisready", MatchCriteria.CONTAINS, false, "my body is ready", "reggie"),
	CHAT_AMAZING(Type.CHAT, "amazing", MatchCriteria.WHOLE_MESSAGE, false, "amazing!"),
	CHAT_BATTLE_OF_THE_CENTURY(Type.CHAT, "battleofthecentury", MatchCriteria.CONTAINS, false, "battle of the century"),
	CHAT_WHO_WILL_WIN(Type.CHAT, "whowillwin", MatchCriteria.CONTAINS, false, "who will win"),
	CHAT_WELCOME(Type.CHAT, "welcome", MatchCriteria.WHOLE_WORD, false),
	CHAT_UNTOUCHABLE(Type.CHAT, "untouchable", MatchCriteria.WHOLE_WORD, false),
	CHAT_MY_GOD(Type.CHAT, "mygod", MatchCriteria.CONTAINS, false, "my god", "omg"),
	CHAT_GODLIKE(Type.CHAT, "godlike", MatchCriteria.CONTAINS, false, "godlike", "god like", "god-like", "god tier", "god-tier", "godtier"),
	CHAT_EXCELLENT(Type.CHAT, "excellent", MatchCriteria.WHOLE_WORD, false),
	CHAT_HMMM(Type.CHAT, "hmmm", MatchCriteria.WHOLE_WORD, false),
	CHAT_DESPERATE(Type.CHAT, "desperate", MatchCriteria.CONTAINS, false),
	CHAT_DISGRACEFUL(Type.CHAT, "disgraceful", MatchCriteria.CONTAINS, false),
	CHAT_FUCK_YOU(Type.CHAT, "fuckyou", MatchCriteria.CONTAINS, true, "fuck you", "fuckyou"),
	CHAT_NASTY(Type.CHAT, "nasty", MatchCriteria.CONTAINS, false),
	CHAT_USELESS_PLAYER(Type.CHAT, "uselessplayer", MatchCriteria.CONTAINS, false, "toed", "selda"),
	CHAT_YOU_COWARD(Type.CHAT, "youcoward", MatchCriteria.CONTAINS, false, "you coward"),
	CHAT_YOURE_GARBAGE(Type.CHAT, "youregarbage", MatchCriteria.CONTAINS, false, "you're garbage", "youre garbage", "ur garbage")
	;

	public static final String NAMESPACE = "tmaannouncer";
	// the typeAndName as the key
	private static final Map<String, AnnouncerSound> BY_TYPED_NAME = new LinkedHashMap<>();

	static {
		for (AnnouncerSound sound : values()) {
			BY_TYPED_NAME.put(sound.getTypeAndName(), sound);
		}
	}

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
		GAME("game", 1f),
		CHAT("chat", 0.9f); // Chat lines slightly quieter than Game announcements

		private final String asString;
		public final float volumeMult;
		private Type(String type, float volumeMult) {
			this.asString = type;
			this.volumeMult = volumeMult;
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
}

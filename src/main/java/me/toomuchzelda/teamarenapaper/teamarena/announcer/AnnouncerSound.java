package me.toomuchzelda.teamarenapaper.teamarena.announcer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

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
	GAME_A_WINNER_IS_YOU(Type.GAME, "awinnerisyou"),

	// chat word sounds
	CHAT_OBJECTION(Type.CHAT, "objection"),
	CHAT_LEEROY_JENKINS(Type.CHAT, "leeroy"),
	CHAT_MY_BODY_IS_READY(Type.CHAT, "my_body_is_ready");

	public static final String NAMESPACE = "tmaannouncer";
	// the typeAndName as the key
	private static final Map<String, AnnouncerSound> BY_TYPED_NAME = new LinkedHashMap<>();

	static {
		for (AnnouncerSound sound : values()) {
			BY_TYPED_NAME.put(sound.getTypeAndName(), sound);
		}
	}

	public final Type type;
	public final String name;
	private final String typeAndName;
	private final String namespacedName;
	private final String phraseName; // The string the check for in chat messages

	private AnnouncerSound(Type type, String name) {
		this.type = type;
		this.name = name;

		this.typeAndName = this.type.asString + "." + this.name;
		this.namespacedName = NAMESPACE + ":" + this.typeAndName;

		this.phraseName = this.name.replace('_', ' ');
	}

	// type.name
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
	public String getPhraseName() {
		return this.phraseName;
	}

	public static AnnouncerSound getByTypedName(String namespacedName) {
		return BY_TYPED_NAME.get(namespacedName);
	}

	public static Collection<String> getTypedNames() {
		return BY_TYPED_NAME.keySet();
	}

	public enum Type {
		GAME("game"),
		CHAT("chat");

		private final String asString;
		private Type(String type) {
			this.asString = type;
		}
	}
}

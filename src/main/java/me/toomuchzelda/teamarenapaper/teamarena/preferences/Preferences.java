package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageLogEntry;
import org.bukkit.Sound;

public class Preferences {

	public static void registerPreferences() {
		// static initializer
	}

	public static final Preference<Sound> BOW_HIT_SOUND = SimplePreference.of("bow_hit_sound",
			"Sound that plays when you hit a bow shot on an entity", Sound.class, Sound.ENTITY_ARROW_HIT_PLAYER);
	public static final Preference<Boolean> HEARTS_FLASH_DAMAGE = SimplePreference.of("hearts_flash_damage",
			"If your hearts should flash when taking damage", true);
	public static final Preference<Boolean> HEARTS_FLASH_REGEN = SimplePreference.of("hearts_flash_regen",
			"If your hearts should flash while regenerating", true);
	public static final Preference<Boolean> DAMAGE_TILT = SimplePreference.of("damage_tilt",
			"Whether your screen should tilt in pain when taking damage. Due to a limitation this will only work if HEARTS_FLASH_DAMAGE is also set to false.", true);
	public static final Preference<Boolean> KIT_ACTION_BAR = SimplePreference.of("kit_action_bar",
			"Receive kit-related messages in the Action bar slot (the little text space above your hotbar)", true);
	public static final Preference<Boolean> KIT_CHAT_MESSAGES = SimplePreference.of("kit_chat_messages",
			"Receive kit-related messages in chat", true);
	public static final Preference<Integer> KOTH_HILL_PARTICLES = SimplePreference.of("koth_hill_particles",
			"The number of ticks in between each spawning of particles along the Hill border in King of the Hill. (0 to 10)", Integer.class, 5,
			value -> value >= 0 && value <= 10);

	public static final Preference<Boolean> RECEIVE_GAME_TITLES = SimplePreference.of("receive_game_titles",
			"Receive titles that cover up part of your screen during gameplay (you will also get a chat message regardless)", true);

	public static final Preference<Boolean> VIEW_OWN_DAMAGE_DISPLAYERS = SimplePreference.of("damage_hologram_own",
			"See damage displayers for damage that was caused by you", true);

	public static final Preference<Boolean> VIEW_OTHER_DAMAGE_DISPLAYERS = SimplePreference.of("damage_hologram_others",
			"See damage displayers for damage caused by other players", true);

	public static final Preference<DamageLogEntry.Style> RECEIVE_DAMAGE_RECEIVED_LIST = SimplePreference.of("damage_received_list",
			"""
					Format of the list of damage you received when you die.
					NONE = Do not list damage received.
					COMPACT = List total damage received, grouped by type.
					FULL = List all damage received.
					""", DamageLogEntry.Style.class, DamageLogEntry.Style.COMPACT);

	public static final Preference<SidebarManager.Style> SIDEBAR_STYLE = SimplePreference.of("sidebar_style",
			"""
					Appearance of the sidebar.
					HIDDEN = The sidebar will remain hidden.
					MODERN = The default sidebar appearance.
					LEGACY = The legacy sidebar appearance, if supported.
					""", SidebarManager.Style.class, SidebarManager.Style.MODERN);

	public static final Preference<Boolean> TEAM_CHAT_SOUND = SimplePreference.of("team_chat_sound",
			"Hear a sound when receiving a team chat message", true);

	public static final Preference<Float> EXPLOSION_VOLUME_MULTIPLIER = SimplePreference.of("explosion_volume",
			"Change the volume of explosions. 1 = normal, 0.5 = half, 0 = silent, etc.", Float.class, 1f);
}

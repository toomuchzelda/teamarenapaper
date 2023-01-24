package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageLogEntry;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Arrays;

public class Preferences {

	public static void registerPreferences() {
		// static initializer
	}

	public static final Preference<Sound> BOW_HIT_SOUND = SimplePreference.ofKeyed("bow_hit_sound",
			"Sound that plays when you hit a bow shot on an entity", Sound.class, Sound.ENTITY_ARROW_HIT_PLAYER,
			Arrays.asList(Sound.values()), Registry.SOUNDS::get)
		.setIcon(Material.BOW);

	public static final Preference<Boolean> HEARTS_FLASH_DAMAGE = SimplePreference.of("hearts_flash_damage",
			"If your hearts should flash when taking damage", true)
		// https://minecraft-heads.com/custom-heads/miscellaneous/34659-damage-particle
		.setIcon(ItemUtils.createPlayerHead("5ee118eddaee0dfb2cbc2c3d59c13a41a7d68cce945e42167aa1dcb8d0670517"));

	public static final Preference<Boolean> HEARTS_FLASH_REGEN = SimplePreference.of("hearts_flash_regen",
			"If your hearts should flash while regenerating", true)
		// https://minecraft-heads.com/custom-heads/miscellaneous/34655-heart-particle
		.setIcon(ItemUtils.createPlayerHead("f1266b748242115b303708d59ce9d5523b7d79c13f6db4ebc91dd47209eb759c"));

	public static final Preference<Boolean> DAMAGE_TILT = SimplePreference.of("damage_tilt",
			"Whether your screen should tilt in pain when taking damage. Due to a limitation this will only work if HEARTS_FLASH_DAMAGE is also set to false.", true)
		.setIcon(ItemBuilder.of(Material.POTION)
			.meta(PotionMeta.class, potionMeta -> potionMeta.setBasePotionData(new PotionData(PotionType.INSTANT_DAMAGE)))
			.build());

	public static final Preference<Boolean> KIT_ACTION_BAR = SimplePreference.of("kit_action_bar",
			"Receive kit-related messages in the Action bar slot (the little text space above your hotbar)", true)
		.setIcon(Material.BIRCH_SIGN);

	public static final Preference<Boolean> KIT_CHAT_MESSAGES = SimplePreference.of("kit_chat_messages",
			"Receive kit-related messages in chat", true)
		// https://minecraft-heads.com/custom-heads/miscellaneous/28599-speech-bubble-chat
		.setIcon(ItemUtils.createPlayerHead("b02af3ca2d5a160ca1114048b7947594269afe2b1b5ec255ee72b683b60b99b9"));

	public static final Preference<Integer> KOTH_HILL_PARTICLES = SimplePreference.of("koth_hill_particles",
			"The number of ticks in between each spawning of particles along the Hill border in King of the Hill. (0 to 10)", Integer.class, 5,
			value -> value >= 0 && value <= 10)
		// https://minecraft-heads.com/custom-heads/miscellaneous/34656-glint-particle
		.setIcon(ItemUtils.createPlayerHead("9f84735fc9c760e95eaf10cec4f10edb5f3822a5ff9551eeb5095135d1ffa302"));

	public static final Preference<Boolean> RECEIVE_GAME_TITLES = SimplePreference.of("receive_game_titles",
			"Receive titles that cover up part of your screen during gameplay (you will also get a chat message regardless)", true)
		.setIcon(Material.COMMAND_BLOCK);

	public static final Preference<Boolean> VIEW_OWN_DAMAGE_DISPLAYERS = SimplePreference.of("damage_hologram_own",
			"See damage displayers for damage that was caused by you", true)
		.setIcon(Material.IRON_CHESTPLATE);

	public static final Preference<Boolean> VIEW_OTHER_DAMAGE_DISPLAYERS = SimplePreference.of("damage_hologram_others",
			"See damage displayers for damage caused by other players", true)
		.setIcon(Material.IRON_SWORD);

	public static final Preference<DamageLogEntry.Style> RECEIVE_DAMAGE_RECEIVED_LIST = SimplePreference.of("damage_received_list",
			"""
					Format of the list of damage you received when you die.
					NONE = Do not list damage received.
					COMPACT = List total damage received, grouped by type.
					FULL = List all damage received.
					""", DamageLogEntry.Style.class, DamageLogEntry.Style.NONE)
		.setIcon(Material.BOOK);

	public static final Preference<SidebarManager.Style> SIDEBAR_STYLE = SimplePreference.of("sidebar_style",
			"""
					Appearance of the sidebar.
					HIDDEN = The sidebar will remain hidden.
					MODERN = The default sidebar appearance.
					LEGACY = The legacy sidebar appearance, if supported.
					""", SidebarManager.Style.class, SidebarManager.Style.MODERN)
		// https://minecraft-heads.com/custom-heads/alphabet/24498-information
		.setIcon(ItemUtils.createPlayerHead("d01afe973c5482fdc71e6aa10698833c79c437f21308ea9a1a095746ec274a0f"));

	public static final Preference<Boolean> TEAM_CHAT_SOUND = SimplePreference.of("team_chat_sound",
			"Hear a sound when receiving a team chat message", true)
		.setIcon(Material.BELL);

	public static final Preference<Float> EXPLOSION_VOLUME_MULTIPLIER = SimplePreference.of("explosion_volume",
					"Change the volume of explosions. 1 = normal, 0.5 = half, 0 = silent, etc.", Float.class, 1f,
					aFloat -> aFloat > 0f && aFloat <= 100f)
			.setIcon(Material.TNT);

	public static final Preference<Boolean> DEFAULT_TEAM_CHAT = SimplePreference.of("default_team_chat",
			"Whether your chat messages should go to team chat instead of all chat by default. (Use /t to talk in the opposite chat)",
			false).setIcon(Material.GOAT_HORN);

	public static final Preference<Boolean> VIEW_HARBINGER_PARTICLES = SimplePreference.of("harbinger_view_particles", "See the laggy smoke particles of the Harbinger or not", true)
			.setIcon(Material.NETHERRACK);

	public static final Preference<Boolean> KILL_SOUND = SimplePreference.of("kill_sound", "Hear the ding sound when you kill a player", true)
			.setIcon(Material.GOLDEN_SWORD);

	public static final Preference<Boolean> ANNOUNCER_GAME = SimplePreference.of("announce_game_events",
		"Whether you hear the announcer say things that happen in the game. For example, a Flag being captured.", true)
		.setIcon(Material.GOAT_HORN);

	public static final Preference<Boolean> ANNOUNCER_CHAT = SimplePreference.of("announce_chat_phrases",
		"Whether you hear the announcer speak out select phrases that appear in chat", true)
		.setIcon(Material.GOAT_HORN);

	public static final Preference<Boolean> ANNOUNCER_SWEAR = SimplePreference.of("announce_swear", "If you want to " +
		"hear the announcer say swear words", false).setIcon(Material.GOAT_HORN);
}

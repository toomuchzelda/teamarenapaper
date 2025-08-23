package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageLogEntry;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSniper;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.packetentities.SpeechBubbleHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Map;

public class Preferences {

	public static void registerPreferences() {
		// static initializer
	}

	private static final Component REMOVED_TEXT = Component.text("Removed. ", TextColors.ERROR_RED);
	public static final Map<String, Component> REMOVED_PREFERENCES = Map.of(
		"koth_hill_particles", REMOVED_TEXT.append(Component.text("Please use the client settings (Video -> Particles)", NamedTextColor.GREEN)),
		"hearts_flash_damage", REMOVED_TEXT.append(Component.text("No longer supported by Minecraft", NamedTextColor.YELLOW)),
		"damage_tilt", REMOVED_TEXT.append(Component.text("Please use client settings (Accessibility -> Damage Tilt)", NamedTextColor.GREEN))
	);

	// Clientside
	public static final Preference<Sound> BOW_HIT_SOUND = SimplePreference.ofKeyed("bow_hit_sound",
			"Sound that plays when you hit a bow shot on an entity", Sound.class, Sound.ENTITY_ARROW_HIT_PLAYER,
			Registry.SOUNDS.keyStream().sorted().map(Registry.SOUNDS::get).toList(),
			Registry.SOUNDS::get)
		.setIcon(Material.BOW)
		.setCategory(PreferenceCategory.VISUAL_EFFECTS);

	public static final Preference<Boolean> HEARTS_FLASH_REGEN = SimplePreference.ofBoolean("hearts_flash_regen",
			"If your hearts should flash while regenerating", true)
		// https://minecraft-heads.com/custom-heads/miscellaneous/34655-heart-particle
		.setIcon(ItemUtils.createPlayerHead("f1266b748242115b303708d59ce9d5523b7d79c13f6db4ebc91dd47209eb759c"))
		.setCategory(PreferenceCategory.VISUAL_EFFECTS);

	public static final Preference<DamageEvent.DamageTiltType> DIRECTIONAL_DAMAGE_TILT = SimplePreference.ofEnum(
		"damage_view_bobbing", "Preference the new or older view-bobbing when taking damage",
		DamageEvent.DamageTiltType.class, DamageEvent.DamageTiltType.DIRECTED)
		.setIcon(Material.RED_STAINED_GLASS_PANE)
		.setValueDescriptionStrings(Map.of(
			DamageEvent.DamageTiltType.ALL, "New style bobbing for all damage",
			DamageEvent.DamageTiltType.DIRECTED, "New style bobbing for directional attacks only i.e Melee attacks",
			DamageEvent.DamageTiltType.NONE, "Only old style bobbing"
		))
		.setCategory(PreferenceCategory.VISUAL_EFFECTS);

	public static final Preference<Boolean> VIEW_HARBINGER_PARTICLES = SimplePreference.ofBoolean("harbinger_view_particles",
			"See the laggy smoke particles of the Harbinger or not", true)
		.setIcon(Material.NETHERRACK)
		.setCategory(PreferenceCategory.VISUAL_EFFECTS);

	public static final Preference<Boolean> KILL_SOUND = SimplePreference.ofBoolean("kill_sound",
			"Hear the ding sound when you kill a player", true)
		.setIcon(Material.GOLDEN_SWORD)
		.setCategory(PreferenceCategory.SOUND_EFFECTS);

	public static final Preference<Boolean> VIEW_OWN_DAMAGE_DISPLAYERS = SimplePreference.ofBoolean("damage_hologram_own",
			"See damage holograms for damage that was caused by you", true)
		.setDisplayName("See self damage holograms")
		.setIcon(Material.IRON_CHESTPLATE)
		.setCategory(PreferenceCategory.VISUAL_EFFECTS);

	public static final Preference<Boolean> VIEW_OTHER_DAMAGE_DISPLAYERS = SimplePreference.ofBoolean("damage_hologram_others",
			"See damage holograms for damage caused by other players", true)
		.setDisplayName("See damage holograms")
		.setIcon(Material.IRON_SWORD)
		.setCategory(PreferenceCategory.VISUAL_EFFECTS);

	public static final Preference<KitSniper.ScopeGlint.Display> SNIPER_SCOPE_GLINT = SimplePreference.ofEnum("sniper_scope_glint",
		"See a glint when a sniper aims near you", KitSniper.ScopeGlint.Display.class, KitSniper.ScopeGlint.Display.WHITE)
		.setDisplayName("Sniper scope glint")
		.setIcon(Material.SPYGLASS)
		.setCategory(PreferenceCategory.VISUAL_EFFECTS);

	public static final Preference<Boolean> TEAM_CHAT_SOUND = SimplePreference.ofBoolean("team_chat_sound",
			"Hear a sound when receiving a team chat message", true)
		.setIcon(Material.BELL)
		.setCategory(PreferenceCategory.SOUND_EFFECTS);

	public static final Preference<Float> EXPLOSION_VOLUME_MULTIPLIER = SimplePreference.ofNumber("explosion_volume",
			"Change the volume of explosions. 1 = normal, 0.5 = half, 0 = silent, etc.", Float.class, 1f,
			aFloat -> aFloat > 0f && aFloat <= 100f)
		.setIcon(Material.TNT)
		.setCategory(PreferenceCategory.SOUND_EFFECTS);

	// Gameplay
	public static final Preference<Boolean> KIT_ACTION_BAR = SimplePreference.ofBoolean("kit_action_bar",
			"Receive kit-related messages in the Action bar slot (the little text space above your hotbar)", true)
		.setIcon(Material.BIRCH_SIGN)
		.setCategory(PreferenceCategory.GAMEPLAY);

	public static final Preference<Boolean> KIT_CHAT_MESSAGES = SimplePreference.ofBoolean("kit_chat_messages",
			"Receive kit-related messages in chat", true)
		// https://minecraft-heads.com/custom-heads/miscellaneous/28599-speech-bubble-chat
		.setIcon(ItemUtils.createPlayerHead("b02af3ca2d5a160ca1114048b7947594269afe2b1b5ec255ee72b683b60b99b9"))
		.setCategory(PreferenceCategory.GAMEPLAY);

	public static final Preference<Boolean> RECEIVE_GAME_TITLES = SimplePreference.ofBoolean("receive_game_titles",
			"Receive titles that cover up part of your screen during gameplay\n" +
				"(you will also get a chat message regardless)", true)
		.setIcon(Material.COMMAND_BLOCK)
		.setCategory(PreferenceCategory.GAMEPLAY);

	public static final Preference<DamageLogEntry.Style> RECEIVE_DAMAGE_RECEIVED_LIST = SimplePreference.ofEnum("damage_received_list",
			"Receive a list of damage when you die.", DamageLogEntry.Style.class, DamageLogEntry.Style.NONE)
		.setDisplayName("See damage on death")
		.setIcon(Material.BOOK)
		.setValueDescriptionStrings(Map.of(
			DamageLogEntry.Style.NONE, "Do not list damage received.",
			DamageLogEntry.Style.COMPACT, "List damage received, group by damage type.",
			DamageLogEntry.Style.FULL, "List all damage received."
		))
		.setCategory(PreferenceCategory.GAMEPLAY);

	public static final Preference<SidebarManager.Style> SIDEBAR_STYLE = SimplePreference.ofEnum("sidebar_style",
			"Appearance of the sidebar.", SidebarManager.Style.class, SidebarManager.Style.MODERN)
		// https://minecraft-heads.com/custom-heads/alphabet/24498-information
		.setIcon(ItemUtils.createPlayerHead("d01afe973c5482fdc71e6aa10698833c79c437f21308ea9a1a095746ec274a0f"))
		.setValueDescriptionStrings(Map.of(
			SidebarManager.Style.HIDDEN, "The sidebar will be hidden.",
			SidebarManager.Style.MODERN, "The default sidebar appearance.",
			SidebarManager.Style.RGB_MANIAC, "For maniacs that think RGB will improve their GAMING™ performance."
		))
		.setCategory(PreferenceCategory.GAMEPLAY)
		.setMigrationFunction(input -> input.contains("LEGACY") ? SidebarManager.Style.MODERN : null);

	public static final Preference<Boolean> DEFAULT_TEAM_CHAT = SimplePreference.ofBoolean("default_team_chat",
			"Whether your chat messages should go to team chat instead of all chat by default.\n" +
				"(Use /t to talk in the opposite chat)", false)
		.setIcon(Material.CALIBRATED_SCULK_SENSOR)
		.setCategory(PreferenceCategory.GAMEPLAY);

	public static final Preference<Boolean> DEFAULT_TEAMMATE_OUTLINE = SimplePreference.ofBoolean("default_teammate_outline",
			"Whether your teammates should be highlighted by default. You can always change this in-game with your team item.",
			false)
		.setIcon(Material.MUSIC_DISC_5)
		.setCategory(PreferenceCategory.GAMEPLAY);

	public static final Preference<BuildingManager.AllyVisibility> ALLY_BUILDING_OUTLINE = SimplePreference.ofEnum("ally_building_outline",
		"Appearance of ally buildings.", BuildingManager.AllyVisibility.class, BuildingManager.AllyVisibility.ALWAYS)
		.setIcon(Material.SMITHING_TABLE)
		.setValueDescriptionStrings(Map.of(
			BuildingManager.AllyVisibility.ALWAYS, "Always show ally building outlines.",
			BuildingManager.AllyVisibility.NEARBY, "Show outlines of ally buildings if nearby.",
			BuildingManager.AllyVisibility.NEVER, "Never see ally building outlines."
		))
		.setCategory(PreferenceCategory.GAMEPLAY);

	// Announcer
	public static final Preference<Boolean> ANNOUNCER_GAME = SimplePreference.ofBoolean("announce_game_events",
		"Whether the announcer will announce gameplay events. For example, a Flag being captured.", true)
		.setIcon(Material.GOAT_HORN)
		.setCategory(PreferenceCategory.ANNOUNCER);

	public static final Preference<Boolean> ANNOUNCER_CHAT = SimplePreference.ofBoolean("announce_chat_phrases",
		"Whether you hear the announcer speak out select phrases that appear in chat", true)
		.setIcon(Material.GOAT_HORN)
		.setCategory(PreferenceCategory.ANNOUNCER);

	public static final Preference<Boolean> ANNOUNCER_SWEAR = SimplePreference.ofBoolean("announce_swear",
		"If you want to hear the announcer say swear words", false)
		.setIcon(Material.GOAT_HORN)
		.setCategory(PreferenceCategory.ANNOUNCER);

	public static final Preference<SpeechBubbleHologram.ViewOptions> SEE_SPEECH_POPUPS = SimplePreference.ofEnum("see_speech_popups",
		"See players speech bubbles for things like thanking medics", SpeechBubbleHologram.ViewOptions.class, SpeechBubbleHologram.ViewOptions.ALL)
		.setIcon(Material.FLOWERING_AZALEA_LEAVES)
		.setCategory(PreferenceCategory.VISUAL_EFFECTS);

	public static final Preference<DamageEvent.DeathMessageGrayOption> DARKEN_DEATH_MESSAGES = SimplePreference.ofEnum("death_messages_darken", "Darken selected death messages to make chat easier to read",
		DamageEvent.DeathMessageGrayOption.class, DamageEvent.DeathMessageGrayOption.ALL)
		.setIcon(Material.DARK_OAK_SIGN)
		.setCategory(PreferenceCategory.VISUAL_EFFECTS);
}

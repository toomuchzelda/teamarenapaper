package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import org.bukkit.Sound;

public class Preferences {
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
            "The number of ticks in between each spawning of particles along the Hill border in King of the Hill. Minimum 1, Max 10", Integer.class, 1,
            value -> value >= 1 && value <= 10);

    public static final Preference<Boolean> RECEIVE_GAME_TITLES = SimplePreference.of("receive_game_titles",
            "Receive titles that cover up part of your screen during gameplay (you will also get a chat message regardless)", true);

}

package me.toomuchzelda.teamarenapaper.teamarena.announcer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/**
 * Class to manage players with/without the announcer resource pack
 *
 * @author toomuchzelda
 */
public class AnnouncerManager
{
	private static final Component HELP_COMMAND = Component.text("If you're having trouble with the voice announcer, " +
		"type /announcer", NamedTextColor.LIGHT_PURPLE);

	private static boolean isInit = false;
	public static void init() {
		if (!isInit) {
			isInit = true;
			AnnouncerSound.init();
		}
	}

	public static boolean hasResourcePack(Player player) {
		return player.hasResourcePack();
	}

	public static void handleEvent(PlayerResourcePackStatusEvent event) {
		if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
			event.getPlayer().sendMessage(HELP_COMMAND);
		}
	}

	public static void broadcastSound(AnnouncerSound sound) {
		if (sound != null)
			Bukkit.getOnlinePlayers().forEach(player -> playSound(player, sound));
	}

	public static void playSound(Player player, AnnouncerSound sound) {
		if (sound == null) return;

		if (hasResourcePack(player)) {
			PlayerInfo pinfo = Main.getPlayerInfo(player);
			if((sound.type == AnnouncerSound.Type.GAME && pinfo.getPreference(Preferences.ANNOUNCER_GAME)) ||
				(sound.type.isChattable() && pinfo.getPreference(Preferences.ANNOUNCER_CHAT))
			) {
				if (!sound.isSwear || pinfo.getPreference(Preferences.ANNOUNCER_SWEAR)) {
					player.playSound(player.getEyeLocation(), sound.getNamespacedName(), SoundCategory.VOICE,
						sound.type.volumeMult, 1f);
				}
			}
		}
	}
}

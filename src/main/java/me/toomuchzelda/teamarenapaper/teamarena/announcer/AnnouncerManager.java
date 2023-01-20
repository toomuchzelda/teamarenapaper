package me.toomuchzelda.teamarenapaper.teamarena.announcer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
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
	// Needs manual updating when resource pack changes
	private static final String URL = "https://www.dropbox.com/s/yqhxzu21bv0dvgr/tmarescpack.zip?dl=1";
	private static final String HASH = "d2ff457e8ac98684bfc84576a31db2ebe352b14c";
	private static final Component PROMPT = Component.text("Team Arena has a voice announcer that announces in-game events " +
		"as well as read out specific phrases when they are mentioned in chat. This resource pack is needed for that, but " +
			"it is ")
		.append(Component.text("entirely optional", Style.style(TextDecoration.UNDERLINED, TextDecoration.BOLD)));

	private static final Component HELP_COMMAND = Component.text("If you're having trouble with the voice announcer, " +
		"type /announcer", NamedTextColor.LIGHT_PURPLE);

	public static void sendResourcePack(Player player) {
		player.setResourcePack(URL, HASH, false, PROMPT);
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
		Bukkit.getOnlinePlayers().forEach(player -> playSound(player, sound));
	}

	public static void playSound(Player player, AnnouncerSound sound) {
		if (hasResourcePack(player)) {
			PlayerInfo pinfo = Main.getPlayerInfo(player);
			if((sound.type == AnnouncerSound.Type.GAME && pinfo.getPreference(Preferences.ANNOUNCER_GAME)) ||
				(sound.type == AnnouncerSound.Type.CHAT && pinfo.getPreference(Preferences.ANNOUNCER_CHAT))
			) {
				player.playSound(player.getEyeLocation(), sound.getNamespacedName(), SoundCategory.VOICE, 1f, 1f);
			}
		}
	}
}

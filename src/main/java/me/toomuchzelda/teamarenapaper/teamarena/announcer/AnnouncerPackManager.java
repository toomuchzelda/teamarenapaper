package me.toomuchzelda.teamarenapaper.teamarena.announcer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/**
 * Class to manage players with/without the announcer resource pack
 *
 * @author toomuchzelda
 */
public class AnnouncerPackManager
{
	// Needs manual updating when resource pack changes
	private static final String URL = "https://www.dropbox.com/s/yqhxzu21bv0dvgr/tmarescpack.zip?dl=1";
	private static final String HASH = "d2ff457e8ac98684bfc84576a31db2ebe352b14c";
	private static final Component PROMPT = Component.text("Team Arena has a voice announcer that announces in-game events " +
		"as well as read out specific phrases when they are mentioned in chat. This resource pack is needed for that, but " +
			"it is ")
		.append(Component.text("entirely optional", Style.style(TextDecoration.UNDERLINED, TextDecoration.BOLD)));

	public static void sendResourcePack(Player player) {
		player.setResourcePack(URL, HASH, false, PROMPT);
	}

	public static boolean hasResourcePack(Player player) {
		return player.hasResourcePack();
	}

	public static void broadcastSound(AnnouncerSound sound) {
		Bukkit.getOnlinePlayers().forEach(player -> playSound(player, sound));
	}

	public static void playSound(Player player, AnnouncerSound sound) {
		if (hasResourcePack(player)) {
			player.playSound(player.getLocation(), sound.getNamespacedName(), SoundCategory.VOICE, 999f, 1f);
		}
	}
}

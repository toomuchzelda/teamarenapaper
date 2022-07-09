package me.toomuchzelda.teamarenapaper.teamarena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

//basic config info for maps
public class MapInfo {
    public String name;
    public String author;
    public String description;

    public boolean doDaylightCycle;
    //0 for clear, 1 for downpour/rain/snow, 2 for thunder
    public int weatherType;
    public boolean doWeatherCycle;

	public Component gameType;
    /**
     * Component that says the map name, author etc. to send to every player
     */
    public Component infoComponent = null;

    public MapInfo() {

    }

    public void sendMapInfo(Player player) {
        if(infoComponent == null) {
			infoComponent = Component.text()
					.append(Component.text("GameType: ", NamedTextColor.GOLD), gameType, Component.newline(),
							Component.text("Map Name: " , NamedTextColor.GOLD), Component.text(name, NamedTextColor.YELLOW), Component.newline(),
							Component.text("Author(s): ", NamedTextColor.GOLD), Component.text(author, NamedTextColor.YELLOW), Component.newline(),
							Component.text("Description: ", NamedTextColor.GOLD), Component.text(description, NamedTextColor.YELLOW))
					.build();
        }

        player.sendMessage(infoComponent);
    }
}

package me.toomuchzelda.teamarenapaper.teamarena;

import net.kyori.adventure.text.Component;
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
    
    /**
     * Component that says the map name, author etc. to send to every player
     */
    public Component infoComponent = null;

    public MapInfo() {
    
    }
    
    public void sendMapInfo(Player player) {
        if(infoComponent == null) {
            infoComponent = Component.text("Map Name: ").color(NamedTextColor.GOLD);
            Component nameComponent = Component.text(name).color(NamedTextColor.YELLOW);
            infoComponent = infoComponent.append(nameComponent);
            infoComponent = infoComponent.append(Component.newline());
            
            Component authorComp = Component.text("Author(s): ").color(NamedTextColor.GOLD);
            authorComp = authorComp.append(Component.text(author).color(NamedTextColor.YELLOW));
            authorComp = authorComp.append(Component.newline());
            infoComponent = infoComponent.append(authorComp);
            
            Component descComp = Component.text("Description: ").color(NamedTextColor.GOLD);
            descComp = descComp.append(Component.text(description).color(NamedTextColor.YELLOW));
            infoComponent = infoComponent.append(descComp);
        }
        
        player.sendMessage(infoComponent);
    }
}

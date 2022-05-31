package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class Building {
    Player player;
    Location loc;
    RealHologram holo;
    Block prevBlock;
    TextColor teamColor;
    Component holoText;

    public Building(Player player, Location loc){
        this.loc = loc.add(0, 1, 0);
        this.player = player;
        this.prevBlock = loc.getBlock();
        this.teamColor = Main.getPlayerInfo(player).team.getRGBTextColor();
        this.holo = new RealHologram(this.loc, this.holoText);

        loc.getBlock().setType(Material.HONEYCOMB);
    }

    public Location getLoc(){
        return this.loc;
    }

    public void setLoc(Location newLoc){
        this.loc = newLoc;
    }

    public void setText(Component newText){
        this.holoText = newText.color(teamColor);
    }

    public void destroy(){
        holo.remove();
    }
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class Building {
    Player owner;
    Location loc;
    RealHologram holo;
    BlockData prevBlockData;
    TextColor teamColor;
    Component holoText;
	BuildingType type;
	String name;

	enum BuildingType{
		SENTRY,
		TELEPORTER
	}

    public Building(Player player, Location loc){
        this.loc = loc.clone();
        this.owner = player;
        this.teamColor = Main.getPlayerInfo(player).team.getRGBTextColor();
    }

    public Location getLoc(){
        return this.loc;
    }

    public void setLoc(Location newLoc){
        this.loc = newLoc;
    }

    public void destroy(){
		holo.remove();
	}
}

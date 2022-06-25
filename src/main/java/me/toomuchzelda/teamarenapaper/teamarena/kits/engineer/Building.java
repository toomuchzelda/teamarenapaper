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
    public Player owner;
    Location loc;
    RealHologram holo;
    TextColor teamColorText;
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
        this.teamColorText = Main.getPlayerInfo(player).team.getRGBTextColor();
    }

    public Location getLoc(){
        return this.loc;
    }

    public void setLoc(Location newLoc){
        this.loc = newLoc;
    }

	public void setText(Component newText){
		this.holoText = newText.color(teamColorText);
		this.holo.setText(this.holoText);
	}

    public void destroy(){
		holo.remove();
	}
}

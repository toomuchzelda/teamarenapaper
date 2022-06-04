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
    Player player;
    Location loc;
    RealHologram holo;
    BlockData prevBlockData;
    TextColor teamColor;
    Component holoText;

    public Building(Player player, Location loc){
		this.prevBlockData = loc.clone().getBlock().getBlockData().clone();
        this.loc = loc.clone();
        this.player = player;
        this.teamColor = Main.getPlayerInfo(player).team.getRGBTextColor();
        this.holo = new RealHologram(this.loc.clone().toCenterLocation().add(0,1.5,0), this.holoText);
    }

    public Location getLoc(){
        return this.loc;
    }

	public BlockData getPrevBlockData() {
		return this.prevBlockData;
	}

    public void setLoc(Location newLoc){
        this.loc = newLoc;
    }

    public void setText(Component newText){
        this.holoText = newText.color(teamColor);
		this.holo.setText(this.holoText);
    }

    public void destroy(){
        holo.remove();
    }
}

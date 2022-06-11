package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;

public class Teleporter extends Building{
    int lastUsedTick;
	Location teleLoc;
	BlockData prevBlockData;
	RealHologram holo;

    public Teleporter(Player player, Location loc){
        super(player, loc);
		this.prevBlockData = loc.clone().getBlock().getBlockData().clone();
        this.lastUsedTick = TeamArena.getGameTick();
		this.teleLoc = loc.clone().toCenterLocation().add(0,0.5,0);
		this.loc.getBlock().setType(Material.HONEYCOMB_BLOCK);
		this.holo = new RealHologram(this.loc.clone().toCenterLocation().add(0,1.5,0), this.holoText);
    }
	public void setText(Component newText){
		this.holoText = newText.color(teamColor);
		this.holo.setText(this.holoText);
	}

	public BlockData getPrevBlockData() {
		return this.prevBlockData;
	}
    public int getLastUsedTick(){
        return lastUsedTick;
    }

    public void setLastUsedTick(int newTick){
        this.lastUsedTick = newTick;
    }

	public Location getTPLoc(){
		return this.teleLoc;
	}

	public int getRemainingCD(){
		return TeamArena.getGameTick() - lastUsedTick;
	}

	public boolean hasCD(){
		return !(TeamArena.getGameTick() - lastUsedTick > KitEngineer.EngineerAbility.TP_CD);
	}

    @Override
    public void destroy(){
        this.loc.getBlock().setBlockData(this.getPrevBlockData());
        holo.remove();
    }
}

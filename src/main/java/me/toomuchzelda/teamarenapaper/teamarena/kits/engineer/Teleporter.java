package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;

public class Teleporter extends Building{
    int lastUsedTick;
	Location teleLoc;

    public Teleporter(Player player, Location loc){
        super(player, loc);
        this.lastUsedTick = TeamArena.getGameTick();
		this.teleLoc = loc.clone().toCenterLocation().add(0,0.5,0);
		this.loc.getBlock().setType(Material.HONEYCOMB_BLOCK);
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

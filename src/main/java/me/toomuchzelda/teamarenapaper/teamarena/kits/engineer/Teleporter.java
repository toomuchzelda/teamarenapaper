package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;

public class Teleporter extends Building{
    int lastUsedTick;

    public Teleporter(Player player, Location loc){
        super(player, loc);
        this.lastUsedTick = TeamArena.getGameTick();
    }

    public int getLastUsedTick(){
        return lastUsedTick;
    }

    public void setLastUsedTick(int newTick){
        this.lastUsedTick = newTick;
    }

    @Override
    public void destroy(){
        loc.getBlock().setBlockData(prevBlock.getBlockData());
        holo.remove();
    }
}

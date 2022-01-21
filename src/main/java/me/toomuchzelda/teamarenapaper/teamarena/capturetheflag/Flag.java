package me.toomuchzelda.teamarenapaper.teamarena.capturetheflag;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class Flag
{
	public final TeamArenaTeam team;
	public final Location baseLoc; // where it spawns/returns to (team's base usually)
	
	private ArmorStand stand;
	public Player holder;
	
	public Flag(CaptureTheFlag game, TeamArenaTeam team, Location baseLoc) {
		this.team = team;
		this.baseLoc = baseLoc;
		
		stand = (ArmorStand) baseLoc.getWorld().spawnEntity(baseLoc, EntityType.ARMOR_STAND);
		//stand.setMarker(true);
		stand.setInvisible(true);
		stand.setBasePlate(false);
		
		//set the armor stand's armor (team coloured chest and head piece)
		ItemStack[] items = new ItemStack[]{new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE)};
		for(ItemStack item : items) {
			LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
			meta.setColor(team.getColour());
			item.setItemMeta(meta);
		}
		
		stand.getEquipment().setHelmet(items[0]);
		stand.getEquipment().setChestplate(items[1]);
		
		stand.setCanTick(false);
		stand.setInvulnerable(true);
		
		game.flagStands.put(stand, this);
	}
	
	public ArmorStand getArmorStand() {
		return stand;
	}
}

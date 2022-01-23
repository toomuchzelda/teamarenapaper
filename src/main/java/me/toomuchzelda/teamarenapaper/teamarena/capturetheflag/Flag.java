package me.toomuchzelda.teamarenapaper.teamarena.capturetheflag;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.EulerAngle;

public class Flag
{
	public final TeamArenaTeam team;
	public final Location baseLoc; // where it spawns/returns to (team's base usually)
	public Location currentLoc;
	
	private ArmorStand stand;
	public Player holder;
	public boolean isAtBase;
	
	public static final EulerAngle LEG_ANGLE = new EulerAngle(Math.PI, 0, 0);
	
	public Flag(CaptureTheFlag game, TeamArenaTeam team, Location baseLoc) {
		this.team = team;
		this.baseLoc = baseLoc;
		this.currentLoc = baseLoc.clone();
		
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
		stand.setGlowing(true);
		stand.setLeftLegPose(LEG_ANGLE);
		stand.setRightLegPose(LEG_ANGLE);
		
		isAtBase = true;
		
		game.flagStands.put(stand, this);
		
		//add to bukkit team so when set glowing it shows the correct color
		team.addMembers(stand);
	}
	
	public ArmorStand getArmorStand() {
		return stand;
	}
	
	public void teleportToBase() {
		stand.teleport(baseLoc);
		currentLoc = baseLoc.clone();
		isAtBase = true;
	}
}

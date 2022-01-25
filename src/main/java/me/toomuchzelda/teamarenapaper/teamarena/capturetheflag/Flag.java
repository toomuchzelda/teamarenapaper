package me.toomuchzelda.teamarenapaper.teamarena.capturetheflag;

import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.EulerAngle;

public class Flag
{
	public final TeamArenaTeam team;
	public final Location baseLoc; // where it spawns/returns to (team's base usually)
	public final BoundingBox baseBox; // boundingbox at the team's base a player has to touch to capture an enemy's flag 
	public Location currentLoc;
	
	/**
	 * have a seperate bukkit team to put on.
	 * needs to be on a team to have the correct colour glowing effect,
	 * but if it's on the same bukkit team as the viewing player then the armor stand's body parts
	 * will be slightly visible (like when viewing invis teammates) and will hide the leather armor
	 * and make it transparent in some places
	 */
	private final Team bukkitTeam;
	
	private ArmorStand stand;
	public Player holder;
	public TeamArenaTeam holdingTeam;
	public boolean isAtBase;
	public int timeSinceDropped;
	
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
		
		stand.customName(team.getComponentName().append(Component.text("'s Flag")));
		stand.setCustomNameVisible(true);
		this.baseBox = stand.getBoundingBox().clone();
		
		isAtBase = true;
		timeSinceDropped = TeamArena.getGameTick();
		
		game.flagStands.put(stand, this);
		
		if(SidebarManager.SCOREBOARD.getTeam(team.getName() + "Flag") != null)
			SidebarManager.SCOREBOARD.getTeam(team.getName() + "Flag").unregister();
		
		bukkitTeam = SidebarManager.SCOREBOARD.registerNewTeam(team.getName() + "Flag");
		bukkitTeam.color(NamedTextColor.nearestTo(team.getRGBTextColor()));
		bukkitTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		bukkitTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
		
		bukkitTeam.addEntity(stand);
	}
	
	public ArmorStand getArmorStand() {
		return stand;
	}
	
	public boolean isBeingCarried() {
		return stand.isInsideVehicle();
	}
	
	public void teleportToBase() {
		stand.teleport(baseLoc);
		currentLoc = baseLoc.clone();
		isAtBase = true;
		holder = null;
		holdingTeam = null;
		stand.customName(team.getComponentName().append(Component.text("'s Flag")));
		stand.setMarker(false);
	}
	
	public void unregisterTeam() {
		bukkitTeam.unregister();
	}
}

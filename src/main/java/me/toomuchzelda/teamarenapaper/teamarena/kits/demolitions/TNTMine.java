package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public class TNTMine extends DemoMine
{
	TNTPrimed tnt;
	
	public TNTMine(Player demo, Block block) {
		super(demo, block);
		
		this.type = MineType.TNTMINE;
		
		Location spawnLoc1 = baseLoc.clone().add(0, 0, 0.5d);
		//put slightly lower to try prevent graphics plane fighting
		Location spawnLoc2 = baseLoc.clone().add(0, -0.005, -0.5d);
		spawnLoc2.setYaw(180f);
		
		World world = baseLoc.getWorld();
		stands = new ArmorStand[2];
		stands[0] = (ArmorStand) world.spawnEntity(spawnLoc1, EntityType.ARMOR_STAND);
		stands[1] = (ArmorStand) world.spawnEntity(spawnLoc2, EntityType.ARMOR_STAND);
		
		this.armorSlot = EquipmentSlot.FEET;
		ItemStack leatherBoots = new ItemStack(Material.LEATHER_BOOTS);
		ItemUtils.colourLeatherArmor(color, leatherBoots);
		
		for (ArmorStand stand : stands) {
			//make sure it's in hashmap first as the packet listener for glowing will fire on the following
			// methods
			ARMOR_STAND_ID_TO_DEMO_MINE.put(stand.getEntityId(), this);
			//glowTeam.addEntity(stand);
			
			stand.setGlowing(false);
			stand.setSilent(true);
			stand.setMarker(true);
			stand.setCanMove(false);
			stand.setCanTick(false);
			stand.setInvulnerable(true);
			stand.setBasePlate(false);
			stand.setInvisible(true);
			stand.setLeftLegPose(LEG_ANGLE);
			stand.setRightLegPose(LEG_ANGLE);
			stand.getEquipment().setBoots(leatherBoots, true);
		}
		
		glowingTeam.addEntities(stands);
		PlayerScoreboard.addMembersAll(glowingTeam, stands);
	}
	
	@Override
	public void trigger(Player triggerer) {
		super.trigger(triggerer);
		
		TNTPrimed tnt = (TNTPrimed) baseLoc.getWorld().spawnEntity(axolotl.getLocation(), EntityType.PRIMED_TNT);
		tnt.setFuseTicks(TNT_TIME_TO_DETONATE);
		tnt.setSource(this.owner);
		tnt.setVelocity(new Vector(0, 0.45d, 0));
		this.tnt = tnt;
	}
	
	@Override
	boolean isDone() {
		return this.type == MineType.TNTMINE && this.tnt != null && !this.tnt.isValid();
	}
	
	public static DemoMine getByTNT(Player player, TNTPrimed tnt) {
		List<DemoMine> list = KitDemolitions.DemolitionsAbility.PLAYER_MINES.get(player);
		TNTMine lex_mine = null;
		if(list != null) {
			for(DemoMine mine : list) {
				if(mine instanceof TNTMine tntmine) {
					if (tntmine.tnt == tnt) {
						lex_mine = tntmine;
						break;
					}
				}
			}
		}
		return lex_mine;
	}
}

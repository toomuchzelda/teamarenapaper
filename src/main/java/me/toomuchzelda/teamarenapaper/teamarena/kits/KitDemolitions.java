package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.BlockUtils;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.Arrays;


public class KitDemolitions extends Kit
{
	public KitDemolitions() {
		super("Demolitions", "mines", Material.STONE_PRESSURE_PLATE);
		
		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);
		
		ItemStack minePlacer = new ItemStack(Material.STICK);
		
		setItems(sword, minePlacer);
		
		this.setAbilities(new DemolitionsAbility());
	}
	
	public static class DemolitionsAbility extends Ability
	{
		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(event.getMaterial() == Material.STICK) {
				// create the armor stand mine
				
				Block block = event.getClickedBlock();
				if(block != null) {
					//summon armorstand with pants to be landmine
					new DemoMine(event.getPlayer(), block);
				}
				
			}
		}
	}
	
	public static class DemoMine
	{
		public static final EulerAngle LEG_ANGLE = new EulerAngle(1.5708d, 0 ,0); //angle for legs so boots r horizontal
		
		private ArmorStand[] stands;
		
		public DemoMine(Player demo, Block block) {
			Color blockColor = BlockUtils.getBlockBukkitColor(block);
			ItemStack leatherBoots = new ItemStack(Material.LEATHER_BOOTS);
			ItemUtils.colourLeatherArmor(blockColor, leatherBoots);
			
			World world = block.getWorld();
			double topOfBlock = BlockUtils.getBlockHeight(block);
			//put downwards slightly so rotated legs lay flat on ground and boots partially in ground
			Location baseLoc = block.getLocation().add(0.5d, topOfBlock - 0.85d, 0.5d);
			
			Location spawnLoc1 = baseLoc.clone().add(0, 0, 0.5d);
			//put slightly lower to try prevent graphics plane fighting
			Location spawnLoc2 = baseLoc.clone().add(0, -0.005, -0.5d);
			spawnLoc2.setYaw(180f);
			
			stands = new ArmorStand[2];
			stands[0] = (ArmorStand) world.spawnEntity(spawnLoc1, EntityType.ARMOR_STAND);
			stands[1] = (ArmorStand) world.spawnEntity(spawnLoc2, EntityType.ARMOR_STAND);
			
			for (ArmorStand stand : stands) {
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
		}
		
		public void remove() {
			for (ArmorStand stand : stands) {
				stand.remove();
			}
		}
	}
}

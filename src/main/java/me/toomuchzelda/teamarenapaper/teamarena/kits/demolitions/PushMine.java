package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class PushMine extends DemoMine
{
	//time in ticks
	public static final int TIME_TO_DETONATE = 20;
	public static final double BLAST_RADIUS = 4.5;
	public static final double BLAST_RADIUS_SQRD = BLAST_RADIUS * BLAST_RADIUS;
	public static final double BLAST_STRENGTH = 0.4;
	
	private int triggerTime;
	
	public PushMine(Player demo, Block block) {
		super(demo, block);
		
		this.type = MineType.PUSHMINE;
		
		Location spawnLoc = baseLoc.clone().add(0, -1.8, 0);
		
		World world = baseLoc.getWorld();
		this.armorSlot = EquipmentSlot.HEAD;
		ItemStack leatherHelmet = new ItemStack(Material.LEATHER_HELMET);
		ItemUtils.colourLeatherArmor(color, leatherHelmet);
		stands = new ArmorStand[1];
		org.bukkit.util.Consumer<ArmorStand> propApplier = stand -> {
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
			stand.getEquipment().setHelmet(leatherHelmet, true);
			KitDemolitions.DemolitionsAbility.ARMOR_STAND_ID_TO_DEMO_MINE.put(stand.getEntityId(), this);
		};
		stands[0] = world.spawn(spawnLoc, ArmorStand.class, propApplier);
		
		glowingTeam.addEntities(stands);
		PlayerScoreboard.addMembersAll(glowingTeam, stands);
	}
	
	@Override
	public void trigger(Player triggerer) {
		super.trigger(triggerer);
		this.triggerTime = TeamArena.getGameTick();
		
		this.stands[0].teleport(stands[0].getLocation().add(0, 0.6, 0));
	}
	
	@Override
	void tick() {
		if(isTriggered()) {
			if(TeamArena.getGameTick() - triggerTime >= TIME_TO_DETONATE) {
				World world = baseLoc.getWorld();
				Location explodeLoc = baseLoc.clone().add(0, 0.1, 0);
				world.spawnParticle(Particle.EXPLOSION_NORMAL, explodeLoc, 1);
				world.playSound(explodeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);
				
				//create a sort of explosion that pushes everyone away
				Vector explodeLocVec = explodeLoc.toVector();
				RayTraceResult result;
				for(Player p : Main.getGame().getPlayers()) {
					//they're within blast radius and has a line of sight
					
					//add half of height so aim for middle of body not feet
					Vector vector = p.getLocation().add(0, p.getHeight() / 2, 0).toVector().subtract(explodeLocVec);
					
					result = world.rayTrace(explodeLoc, vector, BLAST_RADIUS, FluidCollisionMode.SOURCE_ONLY, true, 0,
							e -> e == p);
					//Bukkit.broadcastMessage(result.toString());
					if(result != null && result.getHitEntity() == p) {
						double lengthSqrd = vector.lengthSquared();
						if (lengthSqrd <= BLAST_RADIUS_SQRD) {
							//weaker knockback the further they are from mine base
							double power = Math.sqrt(BLAST_RADIUS_SQRD - lengthSqrd);
							vector.normalize();
							vector.multiply(power * BLAST_STRENGTH);
							
							PlayerUtils.sendVelocity(p, vector);
						}
					}
				}
				
				this.removeEntities();
			}
		}
	}
	
	@Override
	boolean isDone() {
		return !this.stands[0].isValid();
	}
}

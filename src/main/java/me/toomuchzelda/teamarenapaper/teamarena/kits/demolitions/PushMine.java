package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
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
	public static final double BLAST_RADIUS = 4.5;
	public static final double BLAST_RADIUS_SQRD = BLAST_RADIUS * BLAST_RADIUS;
	public static final double BLAST_STRENGTH = 0.35d;

	private int triggerTime;

	public PushMine(Player demo, Block block) {
		super(demo, block);

		this.type = MineType.PUSHMINE;

		//put downwards slightly so rotated legs lay flat on ground and boots partially in ground
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
			stand.getEquipment().setHelmet(leatherHelmet, true);

			for(Player viewer : this.team.getPlayerMembers()) {
				MetadataViewer metaViewer = Main.getPlayerInfo(viewer).getMetadataViewer();
				metaViewer.setViewedValue(MetaIndex.BASE_ENTITY_META,
						MetaIndex.GLOWING_METADATA, stand.getEntityId(), stand);

				//Don't need to refresh metaViewer as this has been put in before the metadata packet is sent
			}
		};
		stands[0] = world.spawn(spawnLoc, ArmorStand.class, propApplier);

		this.glowingTeam = DARK_GREEN_GLOWING_TEAM;
		this.ownerGlowingTeam = GREEN_GLOWING_TEAM;
		glowingTeam.addEntities(stands);
		PlayerScoreboard.addMembersAll(glowingTeam, stands);

		//owner demo should see it as lighter colour
		Main.getPlayerInfo(owner).getScoreboard().addMembers(GREEN_GLOWING_TEAM, stands);
	}

	@Override
	public void trigger(Player triggerer) {
		super.trigger(triggerer);
		this.triggerTime = TeamArena.getGameTick();

		Location loc = stands[0].getLocation();
		loc.add(0, 0.18d, 0);
		loc.setYaw(loc.getYaw() - 180f);
		this.stands[0].teleport(loc);
	}

	@Override
	void tick() {
		if(isTriggered()) {
			if(TeamArena.getGameTick() - triggerTime == this.timeToDetonate) {
				World world = baseLoc.getWorld();
				Location explodeLoc = baseLoc.clone().add(0, 0.1, 0);
				world.spawnParticle(Particle.EXPLOSION_NORMAL, explodeLoc, 15);

				world.playSound(explodeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);

				//create a sort of explosion that pushes everyone away
				Vector explodeLocVec = explodeLoc.toVector();
				RayTraceResult result;
				for(Player p : Main.getGame().getPlayers()) {
					if(this.team.getPlayerMembers().contains(p))
						continue;

					//add half of height so aim for middle of body not feet
					Vector vector = p.getLocation().add(0, p.getHeight() / 2, 0).toVector().subtract(explodeLocVec);

					result = world.rayTrace(explodeLoc, vector, BLAST_RADIUS, FluidCollisionMode.SOURCE_ONLY, true, 0,
							e -> e == p);
					//Bukkit.broadcastMessage(result.toString());
					double lengthSqrd = vector.lengthSquared();
					boolean affect = false;
					if(result != null && result.getHitEntity() == p) {
						affect = true;
					}
					//even if raytrace didn't hit, if they are within 1.1 block count it anyway
					else if(lengthSqrd <= 1.21d){
						affect = true;
					}

					if (affect) {
						//weaker knockback the further they are from mine base
						double power = Math.sqrt(BLAST_RADIUS_SQRD - lengthSqrd);
						vector.normalize();
						vector.add(p.getVelocity().multiply(0.4));
						vector.multiply(power * BLAST_STRENGTH);

						EntityUtils.setVelocity(p, PlayerUtils.noNonFinites(vector));
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

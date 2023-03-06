package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.explosions.CustomExplosionInfo;
import me.toomuchzelda.teamarenapaper.explosions.ExplosionManager;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class TNTMine extends DemoMine
{
	public static final EulerAngle LEG_ANGLE = new EulerAngle(1.5708d, 0 ,0); //angle for legs so boots r horizontal

	TNTPrimed tnt;

	public TNTMine(Player demo, Block block) {
		super(demo, block);

		this.type = MineType.TNTMINE;
		setName(type.name);
	}

	@Override
	public void onPlace() {
		super.onPlace();
		Location standBaseLoc = baseLoc.clone().add(0d, -0.85d, 0d);

		Location spawnLoc1 = standBaseLoc.clone().add(0, 0, 0.5d);
		//put slightly lower to try prevent graphics plane fighting
		Location spawnLoc2 = standBaseLoc.clone().add(0, -0.005, -0.5d);
		spawnLoc2.setYaw(180f);

		World world = baseLoc.getWorld();
		this.armorSlot = EquipmentSlot.FEET;
		ItemStack leatherBoots = new ItemStack(Material.LEATHER_BOOTS);
		ItemUtils.colourLeatherArmor(color, leatherBoots);
		stands = new ArmorStand[2];
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
			stand.getEquipment().setBoots(leatherBoots, true);

			for(Player viewer : this.team.getPlayerMembers()) {
				MetadataViewer metaViewer = Main.getPlayerInfo(viewer).getMetadataViewer();
				metaViewer.setViewedValue(MetaIndex.BASE_BITFIELD_IDX,
					MetaIndex.GLOWING_METADATA_VALUE, stand.getEntityId(), stand);

				//Don't need to refresh metaViewer as this has been put in before the metadata packet is sent
			}
		};
		stands[0] = world.spawn(spawnLoc1, ArmorStand.class, propApplier);
		stands[1] = world.spawn(spawnLoc2, ArmorStand.class, propApplier);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

	@Override
	public @NotNull Collection<? extends Entity> getEntities() {
		return List.of(stands);
	}

	@Override
	public void trigger(Player triggerer) {
		super.trigger(triggerer);

		removeEntities(); //won't remove the tnt as it's still null as of now

		TNTPrimed tnt = (TNTPrimed) baseLoc.getWorld().spawnEntity(hitboxEntity.getLocation().subtract(0d, 0.35d, 0d),
				EntityType.PRIMED_TNT);
		tnt.setFuseTicks(this.timeToDetonate);
		tnt.setSource(this.owner);
		tnt.setVelocity(new Vector(0, 0.45d, 0));

		TeamArenaExplosion explosion = new TeamArenaExplosion(null, 7d, 0.5d, 35d, 3d, 0.1d, DamageType.DEMO_TNTMINE, tnt);
		CustomExplosionInfo cinfo = new CustomExplosionInfo(explosion, true);
		ExplosionManager.setEntityInfo(tnt, cinfo);

		this.tnt = tnt;
	}

	@Override
	boolean isDone() {
		return this.type == MineType.TNTMINE && this.tnt != null && !this.tnt.isValid();
	}

	@Override
	void removeEntities() {
		super.removeEntities();
		if(this.tnt != null)
			tnt.remove();
	}

	public static TNTMine getByTNT(Player player, TNTPrimed tnt) {
		List<TNTMine> list = BuildingManager.getPlayerBuildings(player, TNTMine.class);
		for (TNTMine mine : list) {
			if (mine.tnt == tnt) {
				return mine;
			}
		}
		return null;
	}
}

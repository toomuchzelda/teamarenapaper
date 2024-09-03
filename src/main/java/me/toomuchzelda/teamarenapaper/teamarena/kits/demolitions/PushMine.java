package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class PushMine extends DemoMine
{
	//time in ticks
	public static final double BLAST_RADIUS = 4.5;
	public static final double BLAST_STRENGTH = 1.575d;

	private int triggerTime;

	/**
	 * Creates a new push mine
	 * @param player The demolition player
	 * @param block The block the mine is sitting on
	 */
	public PushMine(Player player, Block block) {
		super(player, block);

		this.type = MineType.PUSHMINE;
		setName(type.name);
		setOutlineColor(NamedTextColor.DARK_GREEN);
	}

	@Override
	public void onPlace() {
		super.onPlace();

		//put downwards slightly so rotated legs lay flat on ground and boots partially in ground
		Location spawnLoc = baseLoc.clone().add(0, -1.8, 0);

		World world = baseLoc.getWorld();
		ItemStack leatherHelmet = new ItemStack(Material.LEATHER_HELMET);
		ItemUtils.colourLeatherArmor(color, leatherHelmet);

		Consumer<ArmorStand> propApplier = stand -> {
			stand.setGlowing(false);
			stand.setSilent(true);
			stand.setMarker(true);
			stand.setCanMove(false);
			stand.setCanTick(false);
			stand.setInvulnerable(true);
			stand.setBasePlate(false);
			stand.setInvisible(true);
			stand.getEquipment().setHelmet(leatherHelmet, true);
		};
		stands = new ArmorStand[] {world.spawn(spawnLoc, ArmorStand.class, propApplier)};
	}

	@Override
	public @NotNull Collection<? extends Entity> getEntities() {
		return List.of(stands);
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
	public void onTick() {
		super.onTick();
		if(isTriggered()) {
			if(TeamArena.getGameTick() - triggerTime == this.timeToDetonate) {
				Location explodeLoc = baseLoc.clone().add(0, 0.1, 0);

				//custom damageType, should not do direct damage but attribute the resulting fall to this mine owner
				PushMineExplosion explosion = new PushMineExplosion(explodeLoc, BLAST_RADIUS, 1.1d, 0d, 0d,
						BLAST_STRENGTH, DamageType.DEMO_PUSHMINE, this.owner);
				explosion.explode();

				markInvalid();
			}
		}
	}

	@Override
	boolean isDone() {
		return !this.stands[0].isValid();
	}

	private static List<PreviewEntity> PREVIEW;
	@Override
	public @NotNull List<PreviewEntity> getPreviewEntity(Location location) {
		if (PREVIEW == null) {
			var outline = new PacketEntity(PacketEntity.NEW_ID, EntityType.ARMOR_STAND, location, List.of(), null);
			outline.setEquipment(EquipmentSlot.HEAD, new ItemStack(Material.LEATHER_HELMET));
			outline.setMetadata(MetaIndex.ARMOR_STAND_BITFIELD_OBJ, MetaIndex.ARMOR_STAND_MARKER_MASK);
			outline.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
			PREVIEW = List.of(new PreviewEntity(outline, new Vector(0, -1.8, 0)));
		}
		return PREVIEW;
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.minecraft.world.phys.AABB;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;

public class PacketMineHitbox extends PacketEntity
{
	private final BoundingBox hitbox;
	public int lastHurtTime;

	public PacketMineHitbox(Location location) {
		super(PacketEntity.NEW_ID, EntityType.AXOLOTL, location, null, PacketEntity.VISIBLE_TO_ALL);

		this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
		this.updateMetadataPacket();

		AABB bb = net.minecraft.world.entity.EntityType.AXOLOTL.getDimensions().makeBoundingBox(
				location.getX(), location.getY(), location.getZ());
		this.hitbox = new BoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
		lastHurtTime = 0;
	}

	//Axolotol hitbox shouldn't move, no need to adjust for that
	public BoundingBox getBoundingBox() {
		return hitbox;
	}

	@Override
	public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
		if(attack) {
			KitDemolitions.DemolitionsAbility.handleHitboxPunch(this, player);
		}
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collections;

public class SentryProjection extends PacketEntity
{

	public SentryProjection(Location location, Player owner) {
		super(PacketEntity.NEW_ID, EntityType.SKELETON, location, Collections.singleton(owner), null);

		byte glowInvis = MetaIndex.BASE_BITFIELD_INVIS_MASK | MetaIndex.BASE_BITFIELD_GLOWING_MASK;
		this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, glowInvis);
		this.updateMetadataPacket();
	}
}

package me.toomuchzelda.teamarenapaper.utils.packetentities;

import java.util.LinkedList;
import java.util.List;

public class PacketEntityManager
{

	private static final List<PacketEntity> ALL_PACKET_ENTITIES = new LinkedList<>();

	static void addPacketEntity(PacketEntity packetEntity) {
		ALL_PACKET_ENTITIES.add(packetEntity);
	}

	public static void tick() {
		var iter = ALL_PACKET_ENTITIES.iterator();
		while(iter.hasNext()) {
			PacketEntity pEntity = iter.next();

			if(pEntity.isRemoved()) {
				iter.remove();
			}
			else if(pEntity.isAlive()) {
				pEntity.globalTick();
			}
		}
	}
}

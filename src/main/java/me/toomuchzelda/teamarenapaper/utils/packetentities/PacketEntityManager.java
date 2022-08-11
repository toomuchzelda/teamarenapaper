package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;

import java.util.LinkedHashMap;

public class PacketEntityManager
{

	private static final LinkedHashMap<Integer, PacketEntity> ALL_PACKET_ENTITIES = new LinkedHashMap<>();

	static void addPacketEntity(PacketEntity packetEntity) {
		ALL_PACKET_ENTITIES.put(packetEntity.getId(), packetEntity);
	}

	public static void tick() {
		var iter = ALL_PACKET_ENTITIES.values().iterator();
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

	public static void cleanUp() {
		var iter = ALL_PACKET_ENTITIES.values().iterator();
		while(iter.hasNext()) {
			iter.next().remove();
			iter.remove();
		}
	}

	public static boolean handleInteract(PlayerUseUnknownEntityEvent event) {
		PacketEntity pEnt = ALL_PACKET_ENTITIES.get(event.getEntityId());
		if(pEnt != null) {
			pEnt.onInteract(event.getPlayer(), event.getHand(), event.isAttack());
			return true;
		}

		return false;
	}
}

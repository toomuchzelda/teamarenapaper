package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import me.toomuchzelda.teamarenapaper.Main;

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

	public static void handleInteract(PlayerUseUnknownEntityEvent event) {
		PacketEntity pEnt = ALL_PACKET_ENTITIES.get(event.getEntityId());
		if(pEnt != null) {
			pEnt.onInteract(event.getPlayer(), event.getHand(), event.isAttack());
		}
		else {
			Main.logger().warning(event.getPlayer().getName() + " interacted on an Unknown Entity that is not a " +
					"PacketEntity");
		}
	}
}

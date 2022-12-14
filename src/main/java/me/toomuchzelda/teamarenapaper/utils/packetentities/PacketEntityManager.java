package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import me.toomuchzelda.teamarenapaper.Main;

import java.util.*;

public class PacketEntityManager
{
	private static final Map<Integer, PacketEntity> ALL_PACKET_ENTITIES = new LinkedHashMap<>();
	static final Map<Integer, Set<AttachedPacketEntity>> ATTACHED_PACKET_ENTITIES_LOOKUP = new LinkedHashMap<>();

	static void addPacketEntity(PacketEntity packetEntity) {
		ALL_PACKET_ENTITIES.put(packetEntity.getId(), packetEntity);
	}

	static void addAttachedEntity(AttachedPacketEntity entity) {
		Set<AttachedPacketEntity> set = ATTACHED_PACKET_ENTITIES_LOOKUP.computeIfAbsent(entity.player.getEntityId(), id -> new LinkedHashSet<>());
		set.add(entity);
	}

	public static Set<AttachedPacketEntity> lookupAttachedEntities(int id) {
		return ATTACHED_PACKET_ENTITIES_LOOKUP.get(id);
	}

	public static void tick() {
		var iter = ALL_PACKET_ENTITIES.values().iterator();
		while(iter.hasNext()) {
			PacketEntity pEntity = iter.next();

			if(pEntity.isRemoved()) {
				iter.remove();
				if(pEntity instanceof AttachedPacketEntity attachedE) {
					final int id = attachedE.player.getEntityId();
					Set<AttachedPacketEntity> set = ATTACHED_PACKET_ENTITIES_LOOKUP.get(id);
					if(set.size() == 1) {
						ATTACHED_PACKET_ENTITIES_LOOKUP.remove(id);
					}
					else {
						set.remove(attachedE);
					}
				}
			}
			else if(pEntity.isAlive()) {
				pEntity.globalTick();
			}
		}
	}

	public static void cleanUp() {
		{
			var iter = ALL_PACKET_ENTITIES.values().iterator();
			while (iter.hasNext()) {
				iter.next().remove();
				iter.remove();
			}

			// TODO: debug
			if (ALL_PACKET_ENTITIES.size() > 0) {
				Main.logger().severe("PacketEntityManager.ALL_PACKET_ENTITIES contains entries after cleanUp()!");
			}
		}

		{
			var iter = ATTACHED_PACKET_ENTITIES_LOOKUP.values().iterator();
			while (iter.hasNext()) {
				iter.next().forEach(PacketEntity::remove);
				iter.remove();
			}

			if (ATTACHED_PACKET_ENTITIES_LOOKUP.size() > 0) {
				Main.logger().severe("PacketEntityManager.ALL_PACKET_ENTITIES contains entries after cleanUp()!");
			}
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

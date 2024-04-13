package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.EventListeners;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.metadata.SimpleMetadataValue;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketHologram;
import net.kyori.adventure.text.Component;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftArrow;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftVector;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ArrowManager {

	// https://mappings.cephx.dev/1.19.4/net/minecraft/world/entity/projectile/AbstractArrow.html
	// Don't want NMS managing pierced entities
	private static final Method NMS_AA_RESET_PIERCED_ENTITIES;
	// Manually handle entity+block collisions
	public static final Method NMS_AA_ON_HIT_BLOCK;

	public static boolean spawnArrowMarkers = false;

	static {
		Method temp;
		try {
			temp = net.minecraft.world.entity.projectile.AbstractArrow.class
				.getDeclaredMethod("D");
			temp.setAccessible(true);
		}
		catch (NoSuchMethodException e) {
			temp = null;
			Main.logger().severe("Could not get reset pierced entities method from NMS AbstractArrow");
			e.printStackTrace();
		}
		NMS_AA_RESET_PIERCED_ENTITIES = temp;

		try {
			temp = net.minecraft.world.entity.projectile.AbstractArrow.class.getDeclaredMethod("a", BlockHitResult.class);
			temp.setAccessible(true);
		}
		catch (NoSuchMethodException e) {
			temp = null;
			Main.logger().severe("Could not get onHitBlock method of NMS AbstractArrow");
			e.printStackTrace();
		}
		NMS_AA_ON_HIT_BLOCK = temp;
	}

	private static class ArrowInfo {
		private final List<Integer> hitEntities;
		private boolean hitOccured = true;
		private BlockHitResult hitBlock = null;

		public ArrowInfo(AbstractArrow aa) {
			this.hitEntities = new ArrayList<>(aa.getPierceLevel());
		}

		public boolean hasHit(Entity victim) {
			return hitEntities.contains(victim.getEntityId());
		}

		public void hit(Entity victim) {
			this.hitEntities.add(victim.getEntityId());
		}

		public void clearHit() { this.hitEntities.clear(); }

		public int count() {
			return this.hitEntities.size();
		}
	}

    private static final Map<AbstractArrow, ArrowInfo> PIERCED_ENTITIES_MAP = new WeakHashMap<>();

    public static void handleArrowEntityCollision(ProjectileHitEvent event) {
		event.setCancelled(true);

		final AbstractArrow arrow = (AbstractArrow) event.getEntity();
		final ArrowInfo ainfo = PIERCED_ENTITIES_MAP.computeIfAbsent(arrow, ArrowInfo::new);
		final Entity hitEntity = event.getHitEntity();
		if (ainfo.hasHit(hitEntity)) {
			return;
		}

		ainfo.hit(hitEntity);
		ainfo.hitOccured = true;

		final int currentTick = TeamArena.getGameTick();
		// Call damage events until its piercing has run out
		// Arrow removal done in tick()
		if (ainfo.count() <= arrow.getPierceLevel() + 1) {
			//Bukkit.broadcastMessage("Would call damage for tick " + currentTick);
		}
    }

	// Called in end tick
	public static void tick() {
		EventListeners.avoidCME = true;
		try {
			for (Iterator<Map.Entry<AbstractArrow, ArrowInfo>> iterator = PIERCED_ENTITIES_MAP.entrySet().iterator(); iterator.hasNext(); ) {
				Map.Entry<AbstractArrow, ArrowInfo> entry = iterator.next();
				final ArrowInfo ainfo = entry.getValue();
				final AbstractArrow arrow = entry.getKey();

				if (!arrow.isValid()) {
					iterator.remove();
					continue;
				}

				// Order of these ops is important as onHitBlock changes pierce level of arrow
				final net.minecraft.world.entity.projectile.AbstractArrow nmsAa = ((CraftArrow) arrow).getHandle();
				if (ainfo.hitBlock != null) { // Assigned by handleBlockCollision
					ainfo.clearHit();
					Location loc = arrow.getLocation();
					arrow.teleport(CraftVector.toBukkit(ainfo.hitBlock.getLocation()).toLocation(loc.getWorld(), loc.getYaw(), loc.getPitch()));
					try { NMS_AA_ON_HIT_BLOCK.invoke(nmsAa, ainfo.hitBlock); }
					catch (IllegalAccessException | InvocationTargetException e) {
						e.printStackTrace();
					}
					finally {
						ainfo.hitBlock = null;
					}
				}
				if (ainfo.hitOccured) {
					ainfo.hitOccured = false;
					if (NMS_AA_RESET_PIERCED_ENTITIES != null) {
						try {
							NMS_AA_RESET_PIERCED_ENTITIES.invoke(nmsAa);
						}
						catch (IllegalAccessException | InvocationTargetException e) {
							e.printStackTrace();
						}
					}

					final int pierceLevel = arrow.getPierceLevel();
					if (ainfo.count() >= pierceLevel + 1) {
						arrow.remove();
						iterator.remove();
					}
				}
			}
		}
		finally {
			EventListeners.avoidCME = false;
		}
	}

	public static void remove(AbstractArrow aa) {
		PIERCED_ENTITIES_MAP.remove(aa);
	}

	private static int markerCtr = 0;
	/** Fix bug when arrow collides with entity and block in same tick, it goes through the block.
	 *  NMS bug */
	public static void handleBlockCollision(ProjectileHitEvent event) {
		if (event.getEntity() instanceof AbstractArrow aa) {
			if (spawnArrowMarkers) {
				PacketHologram hologram = new PacketHologram(aa.getLocation(), null, viewer -> true, Component.text("" + markerCtr++));
				hologram.respawn();
			}

			BlockHitResult hitBlock = EntityUtils.getHitBlock(event);
			if (hitBlock != null) {
				ArrowInfo ainfo = PIERCED_ENTITIES_MAP.computeIfAbsent(aa, ArrowInfo::new);
				ainfo.hitBlock = hitBlock;
			}
		}
	}

	/** Prevent clients predicting arrows disappearing when they hit a player
	 *  by sending them high pierce level metadata.
	 *  TODO replace this with code in packet listener to do it for all arrows */
	public static void addArrowMetaFilter(EntitySpawnEvent event) {
		final Entity spawnedEntity = event.getEntity();
		if (spawnedEntity instanceof AbstractArrow arrow) {
			var iter = Main.getPlayersIter();
			while (iter.hasNext()) {
				var entry = iter.next();
				MetadataViewer metadataViewer = entry.getValue().getMetadataViewer();
				metadataViewer.setViewedValue(MetaIndex.ABSTRACT_ARROW_PIERCING_LEVEL_IDX,
					new SimpleMetadataValue<>((byte) Byte.MAX_VALUE), arrow);
			}
		}
	}
}

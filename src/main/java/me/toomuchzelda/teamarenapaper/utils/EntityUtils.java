package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EntityUtils {
	private static final MethodHandle PLAY_BLOCK_FALL_SOUND;

	public static void cacheReflection() {
		// static initializer
	}

	static {
		try {
			PLAY_BLOCK_FALL_SOUND = MethodHandles.privateLookupIn(net.minecraft.world.entity.LivingEntity.class, MethodHandles.lookup())
				.findVirtual(net.minecraft.world.entity.LivingEntity.class, "playBlockFallSound", MethodType.methodType(void.class));
		} catch (NoSuchMethodException | IllegalAccessException ex) {
			throw new Error("Could not reflectively access method playBlockFallSound", ex);
		}
	}

	// From nmsLiving.causeFallDamage()
	public static void playFallDamageSound(LivingEntity living, double damage) {
		try {
			net.minecraft.world.entity.LivingEntity nmsLiving = ((CraftLivingEntity) living).getHandle();
			nmsLiving.playSound(nmsLiving.getFallDamageSound((int) damage), 1, 1);
			PLAY_BLOCK_FALL_SOUND.invokeExact((net.minecraft.world.entity.LivingEntity) nmsLiving);
		} catch (Throwable ignored) {}
	}

	@NotNull
    public static Component getComponent(@Nullable Entity entity) {
        if (entity == null)
            return Component.text("Unknown", TextColors.ERROR_RED);

        if (entity instanceof Player player) {
			var uselessRGBName = player.playerListName();
			var entityInfo = entity.asHoverEvent().value();
			entityInfo.name(uselessRGBName);
			return uselessRGBName.hoverEvent(HoverEvent.showEntity(entityInfo));
		} else {
			return entity.name();
		}
    }

	public static double getScale(Entity e) {
		double scale = 1d;
		if (e instanceof LivingEntity living) {
			AttributeInstance instance = living.getAttribute(Attribute.SCALE);
			if (instance != null)
				scale = instance.getValue();
		}

		return scale;
	}

	// For getting the hit block ProjectileHitEvents where only the hit entity is given
	public static BlockHitResult getHitBlock(ProjectileHitEvent event) {
		final Entity proj = event.getEntity();
		final net.minecraft.world.entity.Entity nmsProj = ((CraftEntity) proj).getHandle();

		final Vec3 position = nmsProj.position();
		final Vec3 vel = nmsProj.getDeltaMovement();
		final Vec3 nextPos = position.add(vel);

		BlockHitResult blockHitResult = nmsProj.level().clip(new ClipContext(position, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, nmsProj));
		if (blockHitResult.getType() != HitResult.Type.MISS) {
			return blockHitResult;
		}

		return null;
	}

	// Get the actual hit point of a ProjectileHitEvent that hit an entity
	public static RayTraceResult getEntityHitPoint(ProjectileHitEvent event) {
		final Entity hitEntity = event.getHitEntity();
		if (hitEntity == null) return null;

		final Projectile proj = event.getEntity();

		final Location loc = proj.getLocation();
		final Vector dir = proj.getVelocity();

		RayTraceResult result = hitEntity.getBoundingBox().expand(0.3).rayTrace(loc.toVector(), dir, dir.length() + 1);
		if (true)
			return result;
		// Raysize comes from ProjectileUtil
		return rayTraceEntities(loc.getWorld(), loc, dir, dir.length(), 1.0f, 0.3f,
			entity -> entity == hitEntity);
	}

	// Copied from CraftWorld, patched to allow custom scaling of ray's box
	public static RayTraceResult rayTraceEntities(World world, Location start, Vector direction, double maxDistance, double boxExpand,
												  double raySize, Predicate<Entity> filter) {
		if (maxDistance < 0.0D) {
			return null;
		}

		Vector startPos = start.toVector();
		Vector dir = direction.clone().normalize().multiply(maxDistance);
		BoundingBox aabb = BoundingBox.of(startPos, startPos).expandDirectional(dir).expand(boxExpand);
		Collection<Entity> entities = world.getNearbyEntities(aabb, filter);

		Entity nearestHitEntity = null;
		RayTraceResult nearestHitResult = null;
		double nearestDistanceSq = Double.MAX_VALUE;

		for (Entity entity : entities) {
			BoundingBox boundingBox = entity.getBoundingBox().expand(raySize);
			RayTraceResult hitResult = boundingBox.rayTrace(startPos, direction, maxDistance);

			if (hitResult != null) {
				double distanceSq = startPos.distanceSquared(hitResult.getHitPosition());

				if (distanceSq < nearestDistanceSq) {
					nearestHitEntity = entity;
					nearestHitResult = hitResult;
					nearestDistanceSq = distanceSq;
				}
			}
		}

		return (nearestHitEntity == null) ? null : new RayTraceResult(nearestHitResult.getHitPosition(), nearestHitEntity, nearestHitResult.getHitBlockFace());
	}

    /**
     * play critical hit animation on entity
     *
     * @param entity Entity playing the effect on
     */
    public static void playCritEffect(Entity entity) {
		playEffect(entity, ClientboundAnimatePacket.CRITICAL_HIT);
    }

	public static void playMagicCritEffect(Entity entity) {
		playEffect(entity, ClientboundAnimatePacket.MAGIC_CRITICAL_HIT);
	}

	/**
	 * @param effect A ClientboundAnimatePacket effect.
	 */
	public static void playEffect(Entity entity, int effect) {
		PacketContainer packet = animatePacket(entity.getEntityId(), effect);

		//if a player, send packet to self
		if (entity instanceof Player p) {
			PlayerUtils.sendPacket(p, packet);
		}

		//send to all viewers
		/*for (Player p : entity.getTrackedPlayers()) {
			PlayerUtils.sendPacket(p, packet);
		}*/
		for (ServerPlayerConnection connection : getTrackedPlayers0(entity)) {
			PlayerUtils.sendPacket(connection.getPlayer().getBukkitEntity(), packet);
		}
	}

	public static PacketContainer animatePacket(int entityId, int animation) {
		PacketContainer p = new PacketContainer(PacketType.Play.Server.ANIMATION);
		p.getIntegers().write(0, entityId);
		p.getIntegers().write(1, animation);

		return p;
	}

	public static ClientboundRemoveEntitiesPacket getRemoveEntitiesPacket(Entity... entities) {
		int[] ints = new int[entities.length];
		for (int i = 0; i < entities.length; i++) {
			ints[i] = entities[i].getEntityId();
		}

		return new ClientboundRemoveEntitiesPacket(ints);
	}

	public static Entity spawnCustomEntity(World world, Location loc, net.minecraft.world.entity.Entity nmsEntity) {
		Entity bukkitEnitity = ((CraftWorld) world).addEntity(nmsEntity, CreatureSpawnEvent.SpawnReason.CUSTOM);
		bukkitEnitity.teleport(loc);
		return bukkitEnitity;
	}

	/**
	 * set velocity fields and send the packet immediately instead of waiting for next tick if it's a player
	 */
    public static void setVelocity(Entity entity, Vector velocity) {
        entity.setVelocity(velocity);

        if(entity instanceof Player player) {
            ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
            //do not do stuff next tick
            nmsPlayer.hurtMarked = false;

            //send a packet NOW
            // can avoid protocollib since the nms constructor is public and modular
            Vec3 vec = CraftVector.toVec3(velocity);
            ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(player.getEntityId(), vec);
            nmsPlayer.connection.send(packet);
        }
    }

	/**
	 * Set entity's max health attribute and also set their current health manually to prevent a bug when only
	 * setting their max health attribute.
	 */
	public static void setMaxHealth(LivingEntity entity, double newHealth) {
		double oldHealth = entity.getHealth();

		entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newHealth);

		if(oldHealth > newHealth)
			entity.setHealth(newHealth);
	}

	public static void addAttribute(AttributeInstance instance, AttributeModifier modifier) {
		if (instance.getModifier(modifier.getKey()) == null) {
			instance.addModifier(modifier);
		}
	}

	public static void removeAllModifiers(LivingEntity living) {
		for(Attribute attribute : Registry.ATTRIBUTE) {
			AttributeInstance instance = living.getAttribute(attribute);
			if(instance != null) {
				removeAllModifiers(instance);
			}
		}
	}

	public static void removeAllModifiers(AttributeInstance attributeInstance) {
		for (AttributeModifier modifier : attributeInstance.getModifiers()) {
			attributeInstance.removeModifier(modifier);
		}
	}

	private static final double MAX_RELATIVE_DELTA = Short.MAX_VALUE / 4096d;
	private static final double MIN_RELATIVE_DELTA = Short.MIN_VALUE / 4096d;

	public static ClientboundTeleportEntityPacket createTeleportPacket(int id, double x, double y, double z,
																	   double yaw, double pitch, boolean onGround) {

		byte newYaw = (byte) (yaw * 256d / 360d);
		byte newPitch = (byte) (pitch * 256d / 360d);
		var friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
		friendlyByteBuf.writeVarInt(id);
		friendlyByteBuf.writeDouble(x);
		friendlyByteBuf.writeDouble(y);
		friendlyByteBuf.writeDouble(z);
		friendlyByteBuf.writeByte(newYaw);
		friendlyByteBuf.writeByte(newPitch);
		friendlyByteBuf.writeBoolean(onGround);

		return ClientboundTeleportEntityPacket.STREAM_CODEC.decode(friendlyByteBuf);
	}

	public static Packet<?> createMovePacket(int id, Location location, double xDelta, double yDelta, double zDelta,
											 double yawDelta, double pitchDelta, boolean onGround) {
		byte newYaw = (byte) ((location.getYaw() + yawDelta) * 256d / 360d);
		byte newPitch = (byte) ((location.getPitch() + pitchDelta) * 256d / 360d);

		if (xDelta >= MAX_RELATIVE_DELTA || yDelta >= MAX_RELATIVE_DELTA || zDelta >= MAX_RELATIVE_DELTA ||
			xDelta <= MIN_RELATIVE_DELTA || yDelta <= MIN_RELATIVE_DELTA || zDelta <= MIN_RELATIVE_DELTA) {
			var friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
			friendlyByteBuf.writeVarInt(id);
			friendlyByteBuf.writeDouble(location.getX() + xDelta);
			friendlyByteBuf.writeDouble(location.getY() + yDelta);
			friendlyByteBuf.writeDouble(location.getZ() + zDelta);
			friendlyByteBuf.writeByte(newYaw);
			friendlyByteBuf.writeByte(newPitch);
			friendlyByteBuf.writeBoolean(onGround);

			return ClientboundTeleportEntityPacket.STREAM_CODEC.decode(friendlyByteBuf);
		} else {
			short deltaX = (short) (xDelta * 4096d);
			short deltaY = (short) (yDelta * 4096d);
			short deltaZ = (short) (zDelta * 4096d);

			if (yawDelta == 0 && pitchDelta == 0) {
				return new ClientboundMoveEntityPacket.Pos(id, deltaX, deltaY, deltaZ, onGround);
			} else if (deltaX == 0 && deltaY == 0 && deltaZ == 0) {
				return new ClientboundMoveEntityPacket.Rot(id, newYaw, newPitch, onGround);
			} else {
				return new ClientboundMoveEntityPacket.PosRot(id, deltaX, deltaY, deltaZ, newYaw, newPitch, onGround);
			}
		}
	}

	public static Packet<?> createMovePacket(Entity entity, double xDelta, double yDelta, double zDelta,
											 double yawDelta, double pitchDelta, boolean onGround) {
		return createMovePacket(entity.getEntityId(), entity.getLocation(), xDelta, yDelta, zDelta, yawDelta, pitchDelta, onGround);
	}

	public static Packet<?> createMovePacket(PacketEntity entity, double xDelta, double yDelta, double zDelta,
											 double yawDelta, double pitchDelta, boolean onGround) {
		return createMovePacket(entity.getId(), entity.getLocation(), xDelta, yDelta, zDelta, yawDelta, pitchDelta, onGround);
	}

	public static PacketContainer getMountPacket(int vehicle, int... riders) {
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.MOUNT);
		packet.getIntegers().write(0, vehicle);
		packet.getIntegerArrays().write(0, riders);

		return packet;
	}

	// Following 3 methods exist because paper Entity.getTrackedPlayers() is really slow
	@Deprecated
	public static Set<ServerPlayerConnection> getTrackedPlayers0(Entity viewedEntity) {
		ServerLevel nmsWorld = ((CraftWorld) viewedEntity.getWorld()).getHandle();
		ChunkMap.TrackedEntity tracker = nmsWorld.getChunkSource().chunkMap.entityMap.get(viewedEntity.getEntityId());
		if (tracker == null) {
			return Collections.emptySet();
		}

		return tracker.seenBy;
	}

	public static boolean isTrackingEntity(Player viewer, Entity viewedEntity) {
		return getTrackedPlayers0(viewedEntity).contains(((CraftPlayer) viewer).getHandle().connection);
	}

	public static void forEachTrackedPlayer(Entity viewedEntity, Consumer<Player> func) {
		Set<ServerPlayerConnection> trackers = getTrackedPlayers0(viewedEntity);
		for (ServerPlayerConnection connection : trackers) {
			Player player = connection.getPlayer().getBukkitEntity().getPlayer();
			if (player != null)
				func.accept(player);
		}
	}

	public static void forEachTrackedPlayerAndSelf(Entity viewedEntity, Consumer<Player> func) {
		forEachTrackedPlayer(viewedEntity, func);
		if (viewedEntity instanceof Player p) {
			func.accept(p);
		}
	}

	public static ArrayList<Player> getTrackedPlayers(Entity viewedEntity) {
		Set<ServerPlayerConnection> connections = getTrackedPlayers0(viewedEntity);
		ArrayList<Player> list = new ArrayList<>(connections.size());
		for (ServerPlayerConnection connection : connections) {
			Player player = connection.getPlayer().getBukkitEntity().getPlayer();
			if (player != null) // CraftEntity.getPlayer() returns null for OfflinePlayers
				list.add(player);
		}

		return list;
	}

	/**
	 * Get the distance squared between two entities.
	 * In addition to convenience, this method doesn't allocate two Location objects.
	 */
	public static double distanceSqr(Entity one, Entity two) {
		Vec3 posOne = ((CraftEntity) one).getHandle().position();
		Vec3 posTwo = ((CraftEntity) two).getHandle().position();

		return posOne.distanceToSqr(posTwo);
	}

	public static double distanceSqr(Entity entity, Location loc) {
		Vec3 posOne = ((CraftEntity) entity).getHandle().position();

		double x = posOne.x() - loc.getX();
		double y = posOne.y() - loc.getY();
		double z = posOne.z() - loc.getZ();
		return (x * x) + (y * y) + (z * z);
	}

	public static List<Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>>
	getNMSEquipmentList(Map<EquipmentSlot, ItemStack> equipment) {
		if (equipment.size() == 0)
			throw new IllegalArgumentException("equipment cannot be empty");
		// truly one of the class names of all time
		List<Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>(equipment.size());
		for (var entry : equipment.entrySet()) {
			list.add(Pair.of(CraftEquipmentSlot.getNMS(entry.getKey()), CraftItemStack.asNMSCopy(entry.getValue())));
		}
		return list;
	}

	public static boolean isOnGround(Player player) {
		var nmsPlayer = ((CraftPlayer) player).getHandle();
		var boundingBox = nmsPlayer.getBoundingBox().move(0, -0.1, 0);
		return !nmsPlayer.level().noCollision(nmsPlayer, boundingBox);
	}

	// For debugging only
	@Deprecated
	public static Entity getById(int id) {
		for (World world : Bukkit.getWorlds()) {
			for (Entity entity : world.getEntities()) {
				if (entity.getEntityId() == id)
					return entity;
			}
		}

		return null;
	}

	private static final List<EquipmentSlot> playerSlots = List.of(
		EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
	);
	public static List<EquipmentSlot> getEquipmentSlots(LivingEntity livent) {
		if (livent instanceof Wolf || livent instanceof Horse) // May be more entities
			return List.of(EquipmentSlot.values());
		else
			return playerSlots;
	}
}

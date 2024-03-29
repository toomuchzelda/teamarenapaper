package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_19_R3.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftVector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityUtils {
    public static void cacheReflection() {
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

    public static Vector projectileLaunchVector(Entity shooter, Vector original, double spray) {
        //slight randomness in direction
        double randX = MathUtils.random.nextGaussian() * spray;
        double randY = MathUtils.random.nextGaussian() * spray;
        double randZ = MathUtils.random.nextGaussian() * spray;

        Vector direction = shooter.getLocation().getDirection();
        double power = original.subtract(shooter.getVelocity()).length();

        //probably add to each component?
        direction.setX(direction.getX() + randX);
        direction.setY(direction.getY() + randY);
        direction.setZ(direction.getZ() + randZ);

        direction.multiply(power);

        //Bukkit.broadcastMessage("velocity: " + direction.toString());

        return direction;
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
		net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();
		ClientboundAnimatePacket packet = new ClientboundAnimatePacket(nmsEntity, effect);

		//if a player, send packet to self
		if (entity instanceof Player p) {
			PlayerUtils.sendPacket(p, packet);
		}

		//send to all viewers
		/*for (Player p : entity.getTrackedPlayers()) {
			PlayerUtils.sendPacket(p, packet);
		}*/
		for (ServerPlayerConnection connection : getTrackedPlayers0(entity)) {
			connection.send(packet);
		}
	}

	/**
	 * @param effect A ClientboundAnimatePacket effect.
	 */
	public static void playEffect(PacketEntity packetEntity, int effect) {
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);

		StructureModifier<Integer> ints = packet.getIntegers();
		ints.write(0, packetEntity.getId());
		ints.write(1, effect);

		packetEntity.getRealViewers().forEach(player -> PlayerUtils.sendPacket(player, packet));
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
            Vec3 vec = CraftVector.toNMS(velocity);
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

		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newHealth);

		if(oldHealth > newHealth)
			entity.setHealth(newHealth);
	}

	public static void removeAllModifiers(LivingEntity living) {
		for(Attribute attribute : Attribute.values()) {
			AttributeInstance instance = living.getAttribute(attribute);
			if(instance != null) {
				removeAllModifiers(instance);
			}
		}
	}

	public static void removeAllModifiers(AttributeInstance attributeInstance) {
		Iterator<AttributeModifier> iter = attributeInstance.getModifiers().iterator();
		while(iter.hasNext()) {
			iter.next();
			iter.remove();
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

		return new ClientboundTeleportEntityPacket(friendlyByteBuf);
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

			return new ClientboundTeleportEntityPacket(friendlyByteBuf);
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

	// Following 2 methods exist because paper Entity.getTrackedPlayers() is really slow
	@Deprecated
	public static Set<ServerPlayerConnection> getTrackedPlayers0(Entity viewedEntity) {
		net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) viewedEntity).getHandle();
		if (nmsEntity.tracker == null) {
			return Collections.emptySet();
		}

		return nmsEntity.tracker.seenBy;
	}

	public static boolean isTrackingEntity(Player viewer, Entity viewedEntity) {
		return getTrackedPlayers0(viewedEntity).contains(((CraftPlayer) viewer).getHandle().connection);
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
		return !nmsPlayer.level.noCollision(nmsPlayer, boundingBox);
	}
}

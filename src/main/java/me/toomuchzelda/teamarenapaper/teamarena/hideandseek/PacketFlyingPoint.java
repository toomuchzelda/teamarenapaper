package me.toomuchzelda.teamarenapaper.teamarena.hideandseek;

import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftVector;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PacketFlyingPoint extends PacketEntity {

	public static boolean VISIBLE = false;

	private final LivingEntity toFollow;
	private final double distance;
	private final double radius;

	private final Vector acceleration;
	private final Vector velocity;
	private final double accelLimit;
	private final double friction;
	private final double yMult;

	private final int changeRate;
	private final int timerOffset;

	public PacketFlyingPoint(LivingEntity toFollow, double distance, double radius,
							 double speed, double friction, double yMult,
							 int changeRate) {
		super(PacketEntity.NEW_ID, EntityType.ALLAY, getCentrePoint(toFollow, distance).toLocation(toFollow.getWorld()),
			null, viewer -> VISIBLE);

		this.toFollow = toFollow;
		this.distance = distance;
		this.radius = radius;
		this.accelLimit = speed;
		this.yMult = yMult;

		double x = MathUtils.randomRange(-accelLimit, accelLimit);
		double y = MathUtils.randomRange(-accelLimit, accelLimit);
		double z = MathUtils.randomRange(-accelLimit, accelLimit);
		this.acceleration = new Vector(x, y, z);
		this.velocity = this.acceleration.clone();
		this.friction = friction;
		assert CompileAsserts.OMIT || changeRate > 0;
		this.changeRate = changeRate;

		this.timerOffset = MathUtils.randomMax(this.changeRate - 1);
	}

	private static Vector getCentrePoint(LivingEntity looker, double dist) {
		Location eyeLocation = looker.getEyeLocation();
		return eyeLocation.toVector().add(eyeLocation.getDirection().setY(0).multiply(dist));
	}

	@Override
	public void tick() {
		if (!this.toFollow.isValid()) {
			this.remove();
			return;
		}

		if (TeamArena.getGameTick() % this.changeRate == this.timerOffset) {
			double x = MathUtils.randomRange(-accelLimit, accelLimit);
			double y = MathUtils.randomRange(-accelLimit, accelLimit);
			double z = MathUtils.randomRange(-accelLimit, accelLimit);
			acceleration.setX(x);
			acceleration.setY(y);
			acceleration.setZ(z);

			this.velocity.add(acceleration);
		}

		this.velocity.multiply(this.friction);

		Location thisLoc = this.getLocation();
		Vector newPos = thisLoc.toVector().add(this.velocity);

		Vector centrePoint = getCentrePoint(this.toFollow, this.distance);

		Vector diffFromCentre = newPos.clone().subtract(centrePoint);
		// Transform into squished y space
		diffFromCentre.setY(diffFromCentre.getY() * (1d / this.yMult));
		if (diffFromCentre.length() - this.radius > 0d) {
			diffFromCentre.normalize().multiply(this.radius);
			// transform back
			diffFromCentre.setY(diffFromCentre.getY() * this.yMult);
			newPos = centrePoint.add(diffFromCentre);
		}

		// Reuse the object
		thisLoc.setX(newPos.getX());
		thisLoc.setY(newPos.getY());
		thisLoc.setZ(newPos.getZ());
		this.move(thisLoc);

		if (!(this.toFollow instanceof Player)) {
			net.minecraft.world.entity.LivingEntity nmsLiving = ((CraftLivingEntity) this.toFollow).getHandle();
			nmsLiving.lookAt(EntityAnchorArgument.Anchor.EYES, CraftVector.toNMS(newPos));

			//Location loc = this.toFollow.getLocation();
			//loc.setDirection(diffFromCentre);
			//this.toFollow.teleport(loc, TeleportFlag.Relative.X, TeleportFlag.Relative.Y, TeleportFlag.Relative.Z);
		}
	}
}

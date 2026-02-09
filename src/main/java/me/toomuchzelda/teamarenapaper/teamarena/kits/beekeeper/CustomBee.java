package me.toomuchzelda.teamarenapaper.teamarena.kits.beekeeper;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.bee.Bee;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftBee;

/**
 * Extend Bee class to be able to set our own flying speed.
 */
public class CustomBee extends net.minecraft.world.entity.animal.bee.Bee
{
	float flyingSpeed;

	public CustomBee(EntityType<? extends Bee> type, World world) {
		super(type, ((CraftWorld) world).getHandle());

		this.flyingSpeed = 0.02f;
	}

	@Override
	protected float getFlyingSpeed() {
		return this.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player ? super.getFlyingSpeed() : this.flyingSpeed;
	}

	public static void setCustomBeeSpeed(org.bukkit.entity.Bee bee, float speed) {
		CustomBee customBee = (CustomBee) ((CraftBee) bee).getHandle();
		customBee.flyingSpeed = speed;
	}

	public static float getCustomBeeSpeed(org.bukkit.entity.Bee bee) {
		CustomBee customBee = (CustomBee) ((CraftBee) bee).getHandle();
		return customBee.flyingSpeed;
	}
}

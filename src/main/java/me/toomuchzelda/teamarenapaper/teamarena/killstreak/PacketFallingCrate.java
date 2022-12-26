package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

public class PacketFallingCrate extends PacketEntity
{
	public PacketFallingCrate(Location location) {
		super(PacketEntity.NEW_ID, EntityType.FALLING_BLOCK, location, null, PacketEntity.VISIBLE_TO_ALL);
	}

	@Override
	protected void move(Location newLocation, boolean force) {
		Location cur = this.getLocation();
		if(cur.equals(newLocation)) {
			return;
		}

		cur = newLocation.clone().subtract(cur);

		super.move(newLocation, force);

		Vec3 vec = new Vec3(cur.getX(), cur.getY(), cur.getZ());
		ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(this.getId(), vec);
		// free(vec);

		this.getRealViewers().forEach(player -> PlayerUtils.sendPacket(player, packet));
	}
}

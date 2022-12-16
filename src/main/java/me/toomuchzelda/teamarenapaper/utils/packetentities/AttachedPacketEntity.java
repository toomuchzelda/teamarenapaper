package me.toomuchzelda.teamarenapaper.utils.packetentities;

import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * A PacketEntity that is attached to a Player, meaning it follows the player at a fixed offset (Y offset implemented only).
 *
 * @author toomuchzelda
 */
public class AttachedPacketEntity extends PacketEntity
{
	protected final Entity entity;
	private Pose oldPose;
	public final boolean sendHeadRotPackets;
	public final boolean selfSee;
	/**
	 * @param entity The entity to follow
	 * @param selfSee If the entity is a player, if they should be able to see it too
	 * @param sendHeadRotPackets if head rotation packets should be sent (not needed for all use cases)
	 */
	public AttachedPacketEntity(int id, EntityType entityType, Entity entity, @Nullable Collection<? extends Player> viewers,
								@Nullable Predicate<Player> viewerRule, boolean selfSee, boolean sendHeadRotPackets) {
		super(id, entityType, entity.getLocation().add(0, entity.getHeight(), 0), viewers, null);

		if(viewerRule != null) {
			Predicate<Player> viewingPlayer;
			if(selfSee)
				viewingPlayer = viewer -> viewer == entity || entity.getTrackedPlayers().contains(viewer);
			else
				viewingPlayer = viewer -> entity.getTrackedPlayers().contains(viewer);

			this.viewerRule = viewingPlayer.and(viewerRule);
		}
		this.selfSee = selfSee;
		this.reEvaluateViewers(false);

		this.entity = entity;
		this.sendHeadRotPackets = sendHeadRotPackets;

		PacketEntityManager.addAttachedEntity(this);
	}

	public double getYOffset() {
		return this.entity.getHeight() + 0.2d;
	}

	/**
	 * Override this to not send movement packets, since we are doing that in a packet listener for all players
	 * who are not the followed player.
	 * Do it in the packet listener so that the packetentity and player movement won't be weirdly de-synced to viewers
	 */
	@Override
	protected void move(Location newLocation, boolean force) {
		if(this.location.equals(newLocation))
			return;

		newLocation = newLocation.clone();
		if(this.isAlive() && this.selfSee && this.entity instanceof Player player) {
			if(this.sendHeadRotPackets)
				this.updateRotateHeadPacket(newLocation.getYaw());

			ClientboundMoveEntityPacket movePacket = getRelativePosPacket(this.location, newLocation);
			if(movePacket == null || (
					this.dirtyRelativePacketTime != HASNT_MOVED &&
					TeamArena.getGameTick() - this.dirtyRelativePacketTime >= PacketEntity.TICKS_PER_TELEPORT_UPDATE)) {

				updateTeleportPacket(newLocation);
				PlayerUtils.sendPacket(player, this.getTeleportPacket(), this.getRotateHeadPacket());
			}
			else {
				PlayerUtils.sendPacket(player, movePacket);
				if (this.sendHeadRotPackets)
					PlayerUtils.sendPacket(player, this.getRotateHeadPacket());

				if (this.dirtyRelativePacketTime == HASNT_MOVED) {
					this.dirtyRelativePacketTime = TeamArena.getGameTick();
				}
			}
		}

		this.updateSpawnPacket(newLocation);
		this.location = newLocation;
	}

	@Override
	public void tick() {
		//need to update sneaking metadata to hide the name tag behind blocks
		Pose entitysPose = entity.getPose();

		Location loc = entity.getLocation().add(0, this.getYOffset(), 0);
		this.move(loc);

		if(entitysPose != this.oldPose) { // If pose changed handle offset change too.
			if (this.data.hasIndex(MetaIndex.CUSTOM_NAME_IDX)) {
				Byte byteObj = (Byte) this.getMetadata(MetaIndex.BASE_BITFIELD_OBJ);
				byte bitfield;
				if(byteObj == null)
					bitfield = 0;
				else
					bitfield = byteObj;

				if (entitysPose == Pose.SNEAKING) // If the entity is sneaking then make the entity sneak too to hide the nametag
					bitfield |= MetaIndex.BASE_BITFIELD_SNEAKING_MASK;
				else
					bitfield &= ~(MetaIndex.BASE_BITFIELD_SNEAKING_MASK);
				this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, bitfield);
				this.refreshViewerMetadata();
			}

			//if pose changed, re-send precise loc packet to move the entity up/down
			this.updateTeleportPacket(loc);
			for(Player viewer : this.getRealViewers()) {
				PlayerUtils.sendPacket(viewer, this.getTeleportPacket());
			}
			this.oldPose = entitysPose;
		}
	}
}

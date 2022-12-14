package me.toomuchzelda.teamarenapaper.utils.packetentities;

import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import org.bukkit.Location;
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
	protected final Player player;
	private Pose oldPose;
	public final boolean sendHeadRotPackets;
	/**
	 * @param player The player to follow
	 */
	public AttachedPacketEntity(int id, EntityType entityType, Player player, @Nullable Collection<? extends Player> viewers,
								@Nullable Predicate<Player> viewerRule, boolean sendHeadRotPackets) {
		super(id, entityType, player.getLocation().add(0, player.getHeight(), 0), viewers, null);

		if(viewerRule != null) {
			Predicate<Player> viewingPlayer = viewer -> player.getTrackedPlayers().contains(viewer);
			this.viewerRule = viewingPlayer.and(viewerRule);
		}
		this.reEvaluateViewers(false);

		this.player = player;
		this.sendHeadRotPackets = sendHeadRotPackets;

		PacketEntityManager.addAttachedEntity(this);
	}

	public double getYOffset() {
		return this.player.getHeight() + 0.2d;
	}

	/**
	 * Override this to not send movement packets, since we are doing that in a packet listener
	 * Do it in the packet listener so that the packetentity and player movement won't be weirdly de-synced to viewers
	 */
	@Override
	public void move(Location newLocation) {
		if(this.location.equals(newLocation))
			return;

		newLocation = newLocation.clone();

		this.updateSpawnPacket(newLocation);
		this.location = newLocation;
	}

	@Override
	public void tick() {
		//need to update sneaking metadata to hide the name tag behind blocks
		Pose playersPose = player.getPose();

		Location loc = player.getLocation().add(0, this.getYOffset(), 0);
		this.move(loc);

		if(playersPose != this.oldPose) { // If pose changed handle offset change too.
			if (this.data.hasIndex(MetaIndex.CUSTOM_NAME_IDX)) {
				Byte byteObj = (Byte) this.getMetadata(MetaIndex.BASE_BITFIELD_OBJ);
				byte bitfield;
				if(byteObj == null)
					bitfield = 0;
				else
					bitfield = byteObj;

				if (playersPose == Pose.SNEAKING) // If the player is sneaking then make the entity sneak too to hide the nametag
					bitfield |= MetaIndex.BASE_BITFIELD_INVIS_MASK;
				else
					bitfield &= ~(MetaIndex.BASE_BITFIELD_INVIS_MASK);
				this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, bitfield);
				this.refreshViewerMetadata();
			}

			//if pose changed, re-send precise loc packet to move the entity up/down
			this.updateTeleportPacket(loc);
			for(Player viewer : this.getRealViewers()) {
				PlayerUtils.sendPacket(viewer, this.getTeleportPacket());
			}
			this.oldPose = playersPose;
		}
	}
}

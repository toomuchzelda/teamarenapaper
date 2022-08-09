package me.toomuchzelda.teamarenapaper.fakehitboxes;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.mojang.authlib.GameProfile;
import io.papermc.paper.adventure.PaperAdventure;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;

public class FakeHitbox
{
	//index is the fake player number 0-3
	private static final Vector[] OFFSETS;
	//metadata to make them invisible
	private static final List<WrappedWatchableObject> METADATA;
	public static final String USERNAME = "zzzzzzzzzzzzzzzz";
	public static final double VIEWING_RADIUS = 17d;
	public static final double VIEWING_RADIUS_SQR = VIEWING_RADIUS * VIEWING_RADIUS;

	static {
		OFFSETS = new Vector[4];
		int i = 0;
		for (int x = -1; x <= 1; x += 2) {
			for (int z = -1; z <= 1; z += 2) {
				Vector vec = new Vector(x * 0.15, 0d, z * 0.15);
				OFFSETS[i++] = vec;
			}
		}

		WrappedDataWatcher watcher = new WrappedDataWatcher();
		watcher.setObject(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
		METADATA = watcher.getWatchableObjects();
	}

	private final List<ClientboundPlayerInfoPacket.PlayerUpdate> playerInfoEntries;
	private final PacketContainer[] spawnPlayerPackets;
	private final PacketContainer[] teleportPackets;
	private final PacketContainer[] metadataPackets;
	private final PacketContainer[] spawnAndMetaPackets;
	private final PacketContainer removeEntitiesPacket;
	private final int[] fakePlayerIds;

	private final Player owner;
	private final Map<Player, FakeHitboxViewer> viewers;
	//[fakePlayerIndex][x/y/z]
	private final Vector[] coordinates;
	private int lastPoseChangeTime;

	public FakeHitbox(Player owner) {
		this.owner = owner;
		viewers = new LinkedHashMap<>();
		coordinates = new Vector[4];
		spawnPlayerPackets = new PacketContainer[4];
		teleportPackets = new PacketContainer[4];
		metadataPackets = new PacketContainer[4];
		spawnAndMetaPackets = new PacketContainer[8];

		removeEntitiesPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		fakePlayerIds = new int[4];

		List<ClientboundPlayerInfoPacket.PlayerUpdate> playerUpdates = new ArrayList<>(4);

		Location loc = owner.getLocation();
		for(int i = 0; i < 4; i++) {
			FakePlayer fPlayer = new FakePlayer();

			GameProfile authLibProfile = new GameProfile(fPlayer.uuid, USERNAME);

			//net.minecraft.network.chat.Component nmsComponent = PaperAdventure.asVanilla(Component.text(" "));
			Component displayNameComp;
			if(MathUtils.randomMax(10) == 10) {
				displayNameComp = Component.text("TEAM ARENA", MathUtils.randomTextColor(), TextDecoration.BOLD);
			}
			else {
				displayNameComp = Component.text(" ");
			}

			net.minecraft.network.chat.Component nmsComponent = PaperAdventure.asVanilla(displayNameComp);
			ClientboundPlayerInfoPacket.PlayerUpdate update = new ClientboundPlayerInfoPacket.PlayerUpdate(authLibProfile, 1,
					GameType.SURVIVAL, nmsComponent, null);

			playerUpdates.add(update);

			PacketContainer spawnPlayerPacket = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
			spawnPlayerPacket.getIntegers().write(0, fPlayer.entityId);
			spawnPlayerPacket.getUUIDs().write(0, fPlayer.uuid);
			//positions modified in updatePosition()
			spawnPlayerPackets[i] = spawnPlayerPacket;
			spawnAndMetaPackets[i] = spawnPlayerPacket;

			PacketContainer teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
			teleportPacket.getIntegers().write(0, fPlayer.entityId);
			teleportPackets[i] = teleportPacket;


			PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
			metadataPacket.getIntegers().write(0, fPlayer.entityId);
			metadataPacket.getWatchableCollectionModifier().write(0, METADATA);
			metadataPackets[i] = metadataPacket;
			spawnAndMetaPackets[i + 4] = metadataPacket;

			fakePlayerIds[i] = fPlayer.entityId;
			coordinates[i] = loc.toVector();

			FakeHitboxManager.addFakeLookupEntry(fPlayer.entityId, owner);
		}

		List<Integer> list = new ArrayList<>(4);
		for(int id : fakePlayerIds)
			list.add(id);

		removeEntitiesPacket.getIntLists().write(0, list);

		this.playerInfoEntries = playerUpdates;

		this.updatePosition(loc, owner.getPose(), false);
		this.lastPoseChangeTime = 0;
	}

	void tick() {
		var iter = this.viewers.entrySet().iterator();
		Location ownerLoc = this.owner.getLocation();
		final int currentTick = TeamArena.getGameTick();
		while(iter.hasNext()) {
			var entry = iter.next();
			Player playerViewer = entry.getKey();

			if(!playerViewer.isOnline()) {
				iter.remove();
				PlayerUtils.sendPacket(playerViewer, getRemoveEntitiesPacket());
				continue;
			}

			FakeHitboxViewer fakeHitboxViewer = entry.getValue();
			if (fakeHitboxViewer.isSeeingRealPlayer) {
				double distSqr = playerViewer.getLocation().distanceSquared(ownerLoc);
				boolean nowInRange = distSqr <= VIEWING_RADIUS_SQR;

				if(fakeHitboxViewer.isSeeingHitboxes != nowInRange) {
					fakeHitboxViewer.isSeeingHitboxes = nowInRange;
					//need to spawn / remove the hitboxes for viewer
					if (nowInRange) {
						PlayerUtils.sendPacket(playerViewer, getSpawnAndMetadataPackets());
						fakeHitboxViewer.hitboxSpawnTime = currentTick;
						//Bukkit.broadcastMessage("sent spawn packets from " + this.owner.getName() + " to " + playerViewer.getName());
					}
					else {
						PlayerUtils.sendPacket(playerViewer, getRemoveEntitiesPacket());
					}
				}
			}
		}
	}

	/**
	 * Update position in packets and calculate position for swimming/riptide pose if needed.
	 */
	public void updatePosition(Location newPosition, org.bukkit.entity.Pose pose, boolean updateClients) {
		HitboxPose boxPose = HitboxPose.getFromBukkit(pose);

		Vector direction = newPosition.getDirection();
		Vector newPos = new Vector();
		for(int i = 0; i < 4; i++) {
			getBoxPosition(newPos, boxPose, newPosition, direction, i);
			MathUtils.copyVector(coordinates[i], newPos);
		}

		//update the packets
		for(int i = 0; i < 4; i++) {
			writeDoubles(spawnPlayerPackets[i], coordinates[i]);
			writeDoubles(teleportPackets[i], coordinates[i]);
		}

		//update positions on client if needed
		if(updateClients) {
			for(var entry : this.viewers.entrySet()) {
				if(entry.getValue().isSeeingHitboxes) {
					PlayerUtils.sendPacket(entry.getKey(), teleportPackets);
				}
			}
		}
	}

	/**
	 * Calcualte new box number position. Take in Vectors from caller instead of constructing new ones to
	 * reduce Object allocation.
	 */
	private static void getBoxPosition(Vector container, HitboxPose pose, Location ownerLoc, Vector ownerDir, int boxNum) {
		if(pose == HitboxPose.OTHER) {
			Vector offset = OFFSETS[boxNum];

			container.setX(ownerLoc.getX() + offset.getX());
			container.setY(ownerLoc.getY());
			container.setZ(ownerLoc.getZ() + offset.getZ());
		}
		else {
			MathUtils.copyVector(container, ownerDir);

			//align the boxes along the length of the body
			// the original hitbox is at the player's feet (riptiding) or centre (swimming), so have these extend from there
			double mult;
			if(pose == HitboxPose.SWIMMING) {
				// skip one boxNum iteration as the player's real hitbox already covers the centre
				if(boxNum >= 2)
					boxNum++;

				mult = ((double) boxNum - 2) * 0.36d;
			}
			else { //riptiding
				//player's real hitbox is at their feet.
				mult = ((double) boxNum + 1) * 0.36d;
			}

			container.multiply(mult);
			container.setX(container.getX() + ownerLoc.getX());
			container.setY(container.getY() + ownerLoc.getY());
			container.setZ(container.getZ() + ownerLoc.getZ());
		}
	}

	public void handlePoseChange(EntityPoseChangeEvent event) {
		HitboxPose prevPose = HitboxPose.getFromBukkit(event.getEntity().getPose());
		HitboxPose newPose = HitboxPose.getFromBukkit(event.getPose());

		if(newPose != prevPose) {
			this.updatePosition(event.getEntity().getLocation(), event.getPose(), true);
			this.lastPoseChangeTime = TeamArena.getGameTick();
		}
	}

	public FakeHitboxViewer getFakeViewer(Player viewer) {
		return this.viewers.computeIfAbsent(viewer, player1 -> new FakeHitboxViewer());
	}

	public List<ClientboundPlayerInfoPacket.PlayerUpdate> getPlayerInfoEntries() {
		return this.playerInfoEntries;
	}

	public PacketContainer[] getSpawnAndMetadataPackets() {
		//return spawnAndMetaPackets;
		return spawnPlayerPackets;
	}

	public PacketContainer[] getTeleportPackets() {
		return this.teleportPackets;
	}

	public @Nullable PacketContainer[] createRelMovePackets(PacketContainer movePacket) {
		PacketContainer[] packets = null;

		//hard to create new rel move packets for fancy poses, just precise teleport to correct position
		// also, use teleport if it's immediately after a pose change, as those tend to create desyncs.
		final HitboxPose pose = HitboxPose.getFromBukkit(this.owner.getPose());
		final int currentTick = TeamArena.getGameTick();
		if (pose != HitboxPose.OTHER ||
				this.lastPoseChangeTime == currentTick - 1 ||
				this.lastPoseChangeTime == currentTick - 2) {

			packets = new PacketContainer[4];
			System.arraycopy(this.teleportPackets, 0, packets, 0, 4);
		}
		//ignore look-only packets as the direction hitboxes face is not important
		else if(movePacket.getType() == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK ||
				movePacket.getType() == PacketType.Play.Server.REL_ENTITY_MOVE) {
			packets = new PacketContainer[4];
			for(int i = 0; i < 4; i++) {
				packets[i] = new PacketContainer(PacketType.Play.Server.REL_ENTITY_MOVE);
				//replace the entity id
				packets[i].getIntegers().write(0, this.fakePlayerIds[i]);
				StructureModifier<Short> movePacketShorts = movePacket.getShorts();
				StructureModifier<Short> newPacketShorts = packets[i].getShorts();

				for (int idx = 0; idx < 3; idx++) {
					newPacketShorts.write(idx, movePacketShorts.read(idx));
				}
			}
		}

		return packets;
	}

	private static void writeDoubles(PacketContainer packet, Vector coords) {
		StructureModifier<Double> doubles = packet.getDoubles();
		doubles.write(0, coords.getX());
		doubles.write(1, coords.getY());
		doubles.write(2, coords.getZ());
	}

	public @Nullable PacketContainer[] createPoseMetadataPackets(PacketContainer packet) {
		List<WrappedWatchableObject> objects = packet.getWatchableCollectionModifier().read(0);

		PacketContainer[] newPackets = null;
		for (WrappedWatchableObject obj : objects) {
			if (obj.getIndex() == MetaIndex.POSE_IDX) { //if the metadata has pose
				Pose pose = (Pose) obj.getValue();

				newPackets = new PacketContainer[4];

				WrappedDataWatcher watcher = new WrappedDataWatcher();
				watcher.setObject(MetaIndex.POSE_OBJ, pose);
				List<WrappedWatchableObject> newObjects = watcher.getWatchableObjects();

				for(int i = 0; i < 4; i++) {
					PacketContainer newPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
					newPacket.getIntegers().write(0, this.fakePlayerIds[i]);
					newPacket.getWatchableCollectionModifier().write(0, newObjects);
					newPackets[i] = newPacket;
				}

				break;
			}
		}

		return newPackets;
	}

	public PacketContainer getRemoveEntitiesPacket() {
		return this.removeEntitiesPacket;
	}

	public int[] getFakePlayerIds() {
		return this.fakePlayerIds;
	}

	private static class FakePlayer {
		public final int entityId;
		public final UUID uuid;

		public FakePlayer() {
			this.entityId = Bukkit.getUnsafe().nextEntityId();
			this.uuid = UUID.randomUUID();
		}
	}

	private enum HitboxPose {
		SWIMMING,
		RIPTIDING,
		OTHER;

		private static final HitboxPose[] TABLE;

		static {
			TABLE = new HitboxPose[org.bukkit.entity.Pose.values().length];

			for(org.bukkit.entity.Pose pose : org.bukkit.entity.Pose.values()) {
				switch (pose) {
					case SWIMMING -> TABLE[pose.ordinal()] = SWIMMING;
					case FALL_FLYING, SPIN_ATTACK -> TABLE[pose.ordinal()] = RIPTIDING;
					default -> TABLE[pose.ordinal()] = OTHER;
				}
			}
		}

		static HitboxPose getFromBukkit(org.bukkit.entity.Pose pose) {
			return TABLE[pose.ordinal()];
		}
	}
}

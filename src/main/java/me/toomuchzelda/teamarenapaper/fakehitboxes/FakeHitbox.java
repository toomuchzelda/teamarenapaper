package me.toomuchzelda.teamarenapaper.fakehitboxes;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.mojang.authlib.GameProfile;
import io.papermc.paper.adventure.PaperAdventure;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
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
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;

public class FakeHitbox
{
	//index is the fake player number
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

		this.updatePosition(loc);
	}

	void tick() {
		var iter = this.viewers.entrySet().iterator();
		Location ownerLoc = this.owner.getLocation();
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
						//Bukkit.broadcastMessage("sent spawn packets from " + this.owner.getName() + " to " + playerViewer.getName());
					}
					else {
						PlayerUtils.sendPacket(playerViewer, getRemoveEntitiesPacket());
					}
				}

				//TODO: adjust coords on swimming and trident
			}
		}
	}

	/**
	 * Update position in packets and calculate position for swimming/riptide pose if needed.
	 */
	public void updatePosition(Location newPosition) {
		org.bukkit.entity.Pose pose = this.owner.getPose();
		HitboxPose boxPose = HitboxPose.getFromBukkit(pose);

		if(boxPose == HitboxPose.SWIMMING) {
			//todo: swimming positions here
			for(int i = 0; i < 4; i++) {
				coordinates[i].setY(coordinates[i].getY() + 4);
			}
		}
		else if(boxPose == HitboxPose.RIPTIDING) {
			for(int i = 0; i < 4; i++) {
				coordinates[i].setY(coordinates[i].getY() + 2);
			}
		}
		else {
			for(int i = 0; i < 4; i++) {
				Vector offset = OFFSETS[i];

				coordinates[i].setX(newPosition.getX() + offset.getX());
				coordinates[i].setY(newPosition.getY());
				coordinates[i].setZ(newPosition.getZ() + offset.getZ());
			}
		}

		//update the packets
		for(int i = 0; i < 4; i++) {
			writeDoubles(spawnPlayerPackets[i], coordinates[i]);
			writeDoubles(teleportPackets[i], coordinates[i]);
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

	public PacketContainer[] createRelMovePackets(PacketContainer movePacket) {
		PacketContainer[] packets = new PacketContainer[4];
		for(int i = 0; i < 4; i++) {
			packets[i] = movePacket.shallowClone();
			//just change entity id
			packets[i].getIntegers().write(0, this.fakePlayerIds[i]);
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

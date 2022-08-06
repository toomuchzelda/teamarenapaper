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
	private final PacketContainer[] metadataPackets;
	private final PacketContainer[] spawnAndMetaPackets;
	private final PacketContainer removeEntitiesPacket;
	private final int[] fakePlayerIds;

	private final Player owner;
	private final Map<Player, FakeHitboxViewer> viewers;

	public FakeHitbox(Player owner) {
		this.owner = owner;
		viewers = new LinkedHashMap<>();
		spawnPlayerPackets = new PacketContainer[4];
		metadataPackets = new PacketContainer[4];
		spawnAndMetaPackets = new PacketContainer[8];

		removeEntitiesPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		fakePlayerIds = new int[4];

		List<ClientboundPlayerInfoPacket.PlayerUpdate> playerUpdates = new ArrayList<>(4);

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
			//spawn position modified in getter
			spawnPlayerPackets[i] = spawnPlayerPacket;
			spawnAndMetaPackets[i] = spawnPlayerPacket;

			PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
			metadataPacket.getIntegers().write(0, fPlayer.entityId);
			metadataPacket.getWatchableCollectionModifier().write(0, METADATA);
			metadataPackets[i] = metadataPacket;
			spawnAndMetaPackets[i + 4] = metadataPacket;

			fakePlayerIds[i] = fPlayer.entityId;

			FakeHitboxManager.addFakeLookupEntry(fPlayer.entityId, owner);
		}

		List<Integer> list = new ArrayList<>(4);
		for(int id : fakePlayerIds)
			list.add(id);

		removeEntitiesPacket.getIntLists().write(0, list);

		this.playerInfoEntries = playerUpdates;
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
						PlayerUtils.sendPacket(playerViewer, getSpawnAndMetadataPackets(ownerLoc.getX(), ownerLoc.getY(), ownerLoc.getZ()));
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

	public FakeHitboxViewer getFakeViewer(Player viewer) {
		return this.viewers.computeIfAbsent(viewer, player1 -> new FakeHitboxViewer());
	}

	public List<ClientboundPlayerInfoPacket.PlayerUpdate> getPlayerInfoEntries() {
		return this.playerInfoEntries;
	}

	public PacketContainer[] getSpawnAndMetadataPackets(double x, double y, double z) {
		for(int i = 0; i < 4; i++) {
			writeDoubles(spawnPlayerPackets[i], x, y, z, i);
		}

		return spawnAndMetaPackets;
		//return spawnPlayerPackets;
	}

	public PacketContainer[] createTeleportPackets(PacketContainer teleportPacket) {
		StructureModifier<Double> coords = teleportPacket.getDoubles();
		double x = coords.read(0);
		double y = coords.read(1);
		double z = coords.read(2);

		PacketContainer[] packets = new PacketContainer[4];
		for(int i = 0; i < 4; i++) {
			PacketContainer packet = teleportPacket.shallowClone();
			packet.getIntegers().write(0, this.fakePlayerIds[i]);
			writeDoubles(packet, x, y, z, i);
			packets[i] = packet;
		}

		return packets;
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

	private static void writeDoubles(PacketContainer packet, double x, double y, double z, int index) {
		Vector offset = OFFSETS[index];
		StructureModifier<Double> doubles = packet.getDoubles();
		doubles.write(0, x + offset.getX());
		doubles.write(1, y);
		doubles.write(2, z + offset.getZ());
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
}

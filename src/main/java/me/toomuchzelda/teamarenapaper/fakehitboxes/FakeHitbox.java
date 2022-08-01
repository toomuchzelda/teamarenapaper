package me.toomuchzelda.teamarenapaper.fakehitboxes;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.mojang.authlib.GameProfile;
import io.papermc.paper.adventure.PaperAdventure;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakeHitbox
{
	//index is the fake player number
	private static final Vector[] OFFSETS;
	//metadata to make them invisible
	private static final List<WrappedWatchableObject> METADATA;

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

	public FakeHitbox(Player player) {

		spawnPlayerPackets = new PacketContainer[4];
		metadataPackets = new PacketContainer[4];
		spawnAndMetaPackets = new PacketContainer[8];

		removeEntitiesPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		fakePlayerIds = new int[4];

		List<ClientboundPlayerInfoPacket.PlayerUpdate> playerUpdates = new ArrayList<>(4);

		for(int i = 0; i < 4; i++) {
			FakePlayer fPlayer = new FakePlayer();

			/*String name = player.getName();
			if(name.length() == 16) {
				name = name.substring(0, 16);
			}
			name += i;*/

			//use this name so appears at bottom of tab list
			GameProfile authLibProfile = new GameProfile(fPlayer.uuid, "zzzzzzzzzzzzzzzz");

			net.minecraft.network.chat.Component nmsComponent = PaperAdventure.asVanilla(Component.text(" "));
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

			FakeHitboxManager.addFakeLookupEntry(fPlayer.entityId, player);
		}

		List<Integer> list = new ArrayList<>(4);
		for(int id : fakePlayerIds)
			list.add(id);

		removeEntitiesPacket.getIntLists().write(0, list);

		this.playerInfoEntries = playerUpdates;
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

	public @Nullable PacketContainer[] getPoseMetadataPackets(PacketContainer packet) {
		List<WrappedWatchableObject> objects = packet.getWatchableCollectionModifier().read(0);

		PacketContainer[] newPackets = null;
		for (WrappedWatchableObject obj : objects) {
			if (obj.getIndex() == MetaIndex.POSE_IDX) {
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
		public int entityId;
		public UUID uuid;

		public FakePlayer() {
			this.entityId = Bukkit.getUnsafe().nextEntityId();
			this.uuid = UUID.randomUUID();
		}
	}
}

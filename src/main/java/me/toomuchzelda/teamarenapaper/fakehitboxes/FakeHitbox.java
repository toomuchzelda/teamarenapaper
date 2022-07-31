package me.toomuchzelda.teamarenapaper.fakehitboxes;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.mojang.authlib.GameProfile;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakeHitbox
{
	//index is the fake player number
	private static final Vector[] OFFSETS;

	static {
		OFFSETS = new Vector[4];
		int i = 0;
		for (int x = -1; x <= 1; x += 2) {
			for (int z = -1; z <= 1; z += 2) {
				Vector vec = new Vector(x * 0.15, 0d, z * 0.15);
				OFFSETS[i++] = vec;
			}
		}
	}

	private final FakePlayer[] fakePlayers;
	private final PacketContainer addPlayerInfoPacket;
	private final List<ClientboundPlayerInfoPacket.PlayerUpdate> playerInfoEntries;
	private final PacketContainer removePlayerInfoPacket;

	private final PacketContainer[] spawnPlayerPackets;
	private final PacketContainer removeEntitiesPacket;
	private final int[] fakePlayerIds;

	public FakeHitbox(Player player) {
		fakePlayers = new FakePlayer[4];

		addPlayerInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
		addPlayerInfoPacket.getModifier().write(0, ClientboundPlayerInfoPacket.Action.ADD_PLAYER);

		removePlayerInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
		addPlayerInfoPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

		spawnPlayerPackets = new PacketContainer[4];

		removeEntitiesPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		fakePlayerIds = new int[4];

		List<ClientboundPlayerInfoPacket.PlayerUpdate> playerUpdates = new ArrayList<>(4);

		for(int i = 0; i < 4; i++) {
			FakePlayer fPlayer = new FakePlayer();
			fakePlayers[i] = fPlayer;

			String name = player.getName();
			if(name.length() == 16) {
				name = name.substring(0, 16);
			}
			name += i;

			GameProfile authLibProfile = new GameProfile(fPlayer.uuid, name);

			net.minecraft.network.chat.Component nmsComponent = PaperAdventure.asVanilla(Component.text(name));
			ClientboundPlayerInfoPacket.PlayerUpdate update = new ClientboundPlayerInfoPacket.PlayerUpdate(authLibProfile, 1,
					GameType.SURVIVAL, nmsComponent, null);

			playerUpdates.add(update);

			PacketContainer spawnPlayerPacket = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
			spawnPlayerPacket.getIntegers().write(0, fPlayer.entityId);
			spawnPlayerPacket.getUUIDs().write(0, fPlayer.uuid);
			//spawn position modified in getter
			spawnPlayerPackets[i] = spawnPlayerPacket;

			fakePlayerIds[i] = fPlayer.entityId;

			FakeHitboxManager.addFakeLookupEntry(fPlayer.entityId, player);
		}

		List<Integer> list = new ArrayList<>(4);
		for(int id : fakePlayerIds)
			list.add(id);

		removeEntitiesPacket.getIntLists().write(0, list);

		this.playerInfoEntries = playerUpdates;
		addPlayerInfoPacket.getModifier().write(1, playerUpdates);
		removePlayerInfoPacket.getModifier().write(1, playerUpdates);
	}

	public PacketContainer getAddPlayerInfoPacket() {
		return this.addPlayerInfoPacket;
	}

	public List<ClientboundPlayerInfoPacket.PlayerUpdate> getPlayerInfoEntries() {
		return this.playerInfoEntries;
	}

	public PacketContainer getRemovePlayerInfoPacket() {
		return this.removePlayerInfoPacket;
	}

	public PacketContainer[] getSpawnPlayerPackets(double x, double y, double z) {
		for(int i = 0; i < 4; i++) {
			writeDoubles(spawnPlayerPackets[i], x, y, z, i);
		}

		return spawnPlayerPackets;
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

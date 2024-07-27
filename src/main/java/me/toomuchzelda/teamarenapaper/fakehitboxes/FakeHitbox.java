package me.toomuchzelda.teamarenapaper.fakehitboxes;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.*;
import com.mojang.authlib.GameProfile;
import io.papermc.paper.adventure.PaperAdventure;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
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
	private static final WrappedDataValue INVIS_META;
	public static final String USERNAME = "zzzzzz";
	public static final Component DONT_MIND_ME = Component.text("don't mind me");
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

		INVIS_META = MetaIndex.newValue(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
	}

	private final List<PlayerInfoData> playerInfoEntries;
	// index 0 = invis bitfield, 1 = pose
	private final ArrayList<WrappedDataValue> metadataList;
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
	//if each of these fake players is colliding with a wall
	private final boolean[] collidings;
	private int lastPoseChangeTime;
	private boolean hidden = false;

	public FakeHitbox(Player owner) {
		this.owner = owner;
		viewers = new LinkedHashMap<>();
		coordinates = new Vector[4];
		collidings = new boolean[4];

		metadataList = new ArrayList<>(2);
		metadataList.add(INVIS_META);
		metadataList.add(MetaIndex.newValue(MetaIndex.POSE_OBJ, MetaIndex.getNmsPose(owner.getPose())));

		spawnPlayerPackets = new PacketContainer[4];
		teleportPackets = new PacketContainer[4];
		metadataPackets = new PacketContainer[4];
		spawnAndMetaPackets = new PacketContainer[8];

		removeEntitiesPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		fakePlayerIds = new int[4];

		List<PlayerInfoData> playerUpdates = new ArrayList<>(4);

		Location loc = owner.getLocation();
		for(int i = 0; i < 4; i++) {
			FakePlayer fPlayer = new FakePlayer();

			GameProfile authLibProfile = new GameProfile(fPlayer.uuid, USERNAME);
			//Component displayNameComp = getDisplayNameComponent();
			Component displayNameComp = Component.text(owner.getName() + i);
			net.minecraft.network.chat.Component nmsComponent = PaperAdventure.asVanilla(displayNameComp);

			// Unlisted playerinfo entry.
			PlayerInfoData wrappedEntry = new PlayerInfoData(fPlayer.uuid, 1, false, EnumWrappers.NativeGameMode.SURVIVAL,
				WrappedGameProfile.fromHandle(authLibProfile), WrappedChatComponent.fromHandle(nmsComponent),
				(WrappedProfilePublicKey.WrappedProfileKeyData) null);

			playerUpdates.add(wrappedEntry);

			PacketContainer spawnPlayerPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
			spawnPlayerPacket.getIntegers().write(0, fPlayer.entityId);
			spawnPlayerPacket.getUUIDs().write(0, fPlayer.uuid);
			spawnPlayerPacket.getEntityTypeModifier().write(0, EntityType.PLAYER);
			//positions modified in updatePosition()
			spawnPlayerPackets[i] = spawnPlayerPacket;
			spawnAndMetaPackets[i] = spawnPlayerPacket;

			PacketContainer teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
			teleportPacket.getIntegers().write(0, fPlayer.entityId);
			teleportPackets[i] = teleportPacket;


			PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
			metadataPacket.getIntegers().write(0, fPlayer.entityId);
			//metadataPacket.getWatchableCollectionModifier().write(0, METADATA);
			metadataPacket.getDataValueCollectionModifier().write(0, this.metadataList);
			metadataPackets[i] = metadataPacket;
			spawnAndMetaPackets[i + 4] = metadataPacket;

			fakePlayerIds[i] = fPlayer.entityId;
			coordinates[i] = loc.toVector();
			collidings[i] = false;

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
			FakeHitboxViewer fakeHitboxViewer = entry.getValue();

			if(!playerViewer.isOnline()) {
				if(fakeHitboxViewer.isSeeingHitboxes)
					PlayerUtils.sendPacket(playerViewer, getRemoveEntitiesPacket());

				iter.remove();
				continue;
			}

			if (fakeHitboxViewer.isSeeingRealPlayer) {
				boolean seeHitboxes;
				if (this.hidden) {
					seeHitboxes = false;
				}
				else if (this.owner.getVehicle() != null)
					seeHitboxes = false;
				else {
					double distSqr = playerViewer.getLocation().distanceSquared(ownerLoc);
					seeHitboxes = distSqr <= VIEWING_RADIUS_SQR;
				}

				if(fakeHitboxViewer.isSeeingHitboxes != seeHitboxes) {
					fakeHitboxViewer.isSeeingHitboxes = seeHitboxes;
					//need to spawn / remove the hitboxes for viewer
					if (seeHitboxes) {
						fakeHitboxViewer.hitboxSpawnTime = currentTick;
						PlayerUtils.PacketCache cache = new PlayerUtils.PacketCache();
						PlayerUtils.sendPacket(playerViewer, cache, getSpawnAndMetadataPackets());
						cache.flush();
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
	public void updatePosition(Location newLocation, org.bukkit.entity.Pose bukkitPose, final boolean updateClients) {
		HitboxPose boxPose = HitboxPose.getFromBukkit(bukkitPose);

		Vector direction = newLocation.getDirection();
		Vector newPos = new Vector();
		Vector currentPos = newLocation.toVector();
		//get bounding box dimensions based on new, not current pose
		EntityDimensions nmsDimensions = ((CraftPlayer) this.owner).getHandle().getDimensions(Pose.values()[bukkitPose.ordinal()]);
		final double scale = getScale(this.owner);
		boolean[] updateThisBox = new boolean[4];
		boolean updateAny = updateClients;
		int currentTick = TeamArena.getGameTick();
		for(int i = 0; i < 4; i++) {
			updateThisBox[i] = updateClients;
			calcBoxPosition(newPos, boxPose, newLocation, direction, scale, i);

			//check for collision with blocks. put in same position as real player if there is a collision
			// don't want fake hitboxes to peek through walls and such
			//following code taken from bukkit CraftRegionAccessor#hasCollisionsIn(BoundingBox);
			AABB nmsBoundingBox = nmsDimensions.makeBoundingBox(newPos.getX(), newPos.getY(), newPos.getZ());
			boolean noCollision = ((CraftWorld) newLocation.getWorld()).getHandle().noCollision(nmsBoundingBox);
			if(noCollision) {
				MathUtils.copyVector(coordinates[i], newPos);
				//was previously colliding and stopped, update precise pos
				if(this.collidings[i]) {
					this.collidings[i] = false;
					updateThisBox[i] = true;
					updateAny = true;
				}
			}
			else {
				MathUtils.copyVector(coordinates[i], currentPos);
				updateThisBox[i] = true;
				updateAny = true;
				//consider it a pose change so precise teleport will be used in following rel move packet(s)
				this.lastPoseChangeTime = currentTick;
				this.collidings[i] = true;
			}

			//update the packets
			writeDoubles(spawnPlayerPackets[i], coordinates[i]);
			writeDoubles(teleportPackets[i], coordinates[i]);
		}

		//update positions on client if needed
		if(updateAny) {
			for (var entry : this.viewers.entrySet()) {
				if (entry.getValue().isSeeingHitboxes) {
					for (int i = 0; i < 4; i++) {
						if (updateThisBox[i])
							PlayerUtils.sendPacket(entry.getKey(), teleportPackets);
					}
				}
			}
		}
	}

	/**
	 * Calculate new box number position. Take in Vectors from caller instead of constructing new ones to
	 * reduce Object allocation.
	 */
	private static void calcBoxPosition(Vector container, HitboxPose pose, Location ownerLoc, Vector ownerDir, double scale,
										int boxNum) {
		if(pose == HitboxPose.OTHER) {
			Vector offset = OFFSETS[boxNum];

			container.setX(ownerLoc.getX() + (offset.getX() * scale));
			container.setY(ownerLoc.getY());
			container.setZ(ownerLoc.getZ() + (offset.getZ() * scale));
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

				mult = ((double) boxNum - 2) * (0.36d * scale);
			}
			else { //riptiding
				//player's real hitbox is at their feet.
				mult = ((double) boxNum + 1) * (0.36d * scale);
			}

			container.multiply(mult);
			container.setX(container.getX() + ownerLoc.getX());
			container.setY(container.getY() + ownerLoc.getY());
			container.setZ(container.getZ() + ownerLoc.getZ());

			//don't go down/upwards into blocks
			Location heightLoc = ownerLoc.clone();
			heightLoc.setY(container.getY());
			if(heightLoc.getBlock().isSolid()) {
				container.setY(ownerLoc.getY());
			}
		}
	}

	public void handlePoseChange(EntityPoseChangeEvent event) {
		final org.bukkit.entity.Pose bukkitPose = event.getPose();
		this.metadataList.get(1).setRawValue(MetaIndex.getNmsPose(bukkitPose));

		HitboxPose prevPose = HitboxPose.getFromBukkit(event.getEntity().getPose());
		HitboxPose newPose = HitboxPose.getFromBukkit(bukkitPose);

		if(newPose != prevPose) {
			this.updatePosition(event.getEntity().getLocation(), bukkitPose, true);
			this.updateMetadataPackets();
			this.lastPoseChangeTime = TeamArena.getGameTick();
		}
	}

	private void updateMetadataPackets() {
		for (PacketContainer packet : this.metadataPackets) {
			packet.getDataValueCollectionModifier().write(0, this.metadataList);
		}
	}

	// Depends on not being called subsequently with different args
	void setVisible(boolean visible) {
		if (visible) {
			this.metadataList.get(0).setRawValue((byte) 0);
		}
		else {
			this.metadataList.get(0).setRawValue(MetaIndex.BASE_BITFIELD_INVIS_MASK);
		}
		this.updateMetadataPackets();
	}

	void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	/**
	 * Invalidate all player's viewing hitbox status so they will be refreshed on next tick call.
	 */
	public void invalidateViewers() {
		var iter = this.viewers.entrySet().iterator();
		while(iter.hasNext()) {
			var entry = iter.next();
			entry.getValue().isSeeingHitboxes = !entry.getValue().isSeeingHitboxes;
		}
	}

	public FakeHitboxViewer getFakeViewer(Player viewer) {
		return this.viewers.computeIfAbsent(viewer, player1 -> new FakeHitboxViewer());
	}

	public List<PlayerInfoData> getPlayerInfoEntries() {
		return this.playerInfoEntries;
	}

	public PacketContainer[] getSpawnAndMetadataPackets() {
		return this.spawnAndMetaPackets;
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
				(this.lastPoseChangeTime >= currentTick - 3 && this.lastPoseChangeTime <= currentTick)) {

			packets = this.teleportPackets;
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

	// Make new packets that only have the pose change, instead of using this.metadataPackets, to reduce
	// network use. Also, this handles poses like sneaking that aren't handled by handlePostChange
	public @Nullable PacketContainer[] createPoseMetadataPackets(PacketContainer packet) {
		PacketContainer[] newPackets = null;
		for (WrappedDataValue dataValue : packet.getDataValueCollectionModifier().read(0)) {
			if (dataValue.getIndex() == MetaIndex.POSE_IDX) { //if the metadata has pose
				//Pose pose = (Pose) dataValue.getValue();
				newPackets = new PacketContainer[4];
				List<WrappedDataValue> hitboxValueList = List.of(dataValue);
				for(int i = 0; i < 4; i++) {
					PacketContainer newPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
					newPacket.getIntegers().write(0, this.fakePlayerIds[i]);
					newPacket.getDataValueCollectionModifier().write(0, hitboxValueList);
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

	private static Component getDisplayNameComponent() {
		final int rand = MathUtils.randomMax(100);
		if (rand <= 10) {
			return Component.text("TEAM ARENA", MathUtils.randomTextColor(), TextDecoration.BOLD);
		}
		else if(rand == 100) {
			return DONT_MIND_ME;
		}
		else {
			return Component.space();
		}
	}

	private static double getScale(Player player) {
		double scale = 1d;
		AttributeInstance instance = player.getAttribute(Attribute.GENERIC_SCALE);
		if (instance != null)
			scale = instance.getValue();

		return scale;
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

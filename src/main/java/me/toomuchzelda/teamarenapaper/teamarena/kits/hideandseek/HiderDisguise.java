package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.FakeBlockManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.building.BlockBuilding;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.hideandseek.HideAndSeek;
import me.toomuchzelda.teamarenapaper.utils.*;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

public class HiderDisguise {

	private static final int BLOCK_SOLIDIFY_TICKS = 40;

	private final TeamArena game;

	private final Player hider;
	final int timerSeed;

	private final AttachedHiderEntity hitbox;
	private AttachedHiderEntity disguise;

	private final BossBar bossbar;
	private static final Component SOLIDIFYING_COMP = Component.text("Solidifying...", NamedTextColor.AQUA);
	private static final BossBar.Color SOLIDIFYING_COLOUR = BossBar.Color.BLUE;
	private static final Component SOLID_COMP = Component.text("Solid", TextColor.color(255, 132, 0));
	private static final BossBar.Color SOLID_COLOUR = BossBar.Color.YELLOW;
	private static final Component CANT_SOLID_HERE = Component.text("Can't solidify here", TextColors.ERROR_RED);
	private static final BossBar.Color CANT_SOLID_HERE_COLOUR = BossBar.Color.RED;


	// Used if disguised as a block
	private BlockData blockData;
	private static final BlockData airBlockData = Material.AIR.createBlockData();
	private BlockCoords occupiedBlock;
	private int blockChangeTick;
	private BlockBuilding building; // Register to prevent overlaps
	private long fbManagerKey;

	HiderDisguise(final Player hider, TeamArena game) {
		this.hitbox = new AttachedHiderEntity(EntityType.INTERACTION, hider, false);
		this.hitbox.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
		// Slightly over 1.0 to wrap blocks
		this.hitbox.setMetadata(MetaIndex.INTERACTION_WIDTH_OBJ, 1.05f);
		this.hitbox.setMetadata(MetaIndex.INTERACTION_HEIGHT_OBJ, 1.05f);
		this.hitbox.updateMetadataPacket();
		this.hitbox.respawn();

		bossbar = BossBar.bossBar(SOLIDIFYING_COMP, 0f, SOLIDIFYING_COLOUR,
			BossBar.Overlay.PROGRESS);

		this.game = game;
		this.hider = hider;
		this.timerSeed = TeamArena.getGameTick() - MathUtils.randomMax(4);
		this.fbManagerKey = FakeBlockManager.INVALID_KEY;
	}

	// To try work with paved grass and odd blocks
	private BlockCoords getCoords() { return new BlockCoords(this.hider.getLocation().add(0, 0.1, 0)); }

	// Is this the NMS type?
	private static final Vector3f defaultTranslateVec = new Vector3f(-0.5f, -0.5f, -0.5f);
	void disguise(Block clicked) {
		if (this.blockData != null) {
			assert CompileAsserts.OMIT || this.occupiedBlock != null;
			this.breakExistingSolid();
		}

		this.blockData = clicked.getBlockData();

		boolean respawn = false; // need to refresh meta manually if already existed
		if (this.disguise == null || this.disguise.getEntityType() != EntityType.BLOCK_DISPLAY) {
			if (this.disguise != null) {
				this.disguise.remove();
			}

			respawn = true;
			this.disguise = new AttachedHiderEntity(EntityType.BLOCK_DISPLAY, this.hider, true);

			// Invis makes clients not render the blue line when viewing hitboxes
			this.disguise.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);

			this.disguise.setMetadata(MetaIndex.DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ, 2);
			this.disguise.setMetadata(MetaIndex.DISPLAY_TRANSLATION_OBJ, defaultTranslateVec);
		}

		this.disguise.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, ((CraftBlockData) blockData).getState());
		this.disguise.updateMetadataPacket();

		if (respawn)
			this.disguise.respawn();
		else
			this.disguise.refreshViewerMetadata();

		BlockCoords coords = this.getCoords();
		this.occupiedBlock = coords;
		this.blockChangeTick = TeamArena.getGameTick();
		this.resetBlockTimer(coords);

		PlayerUtils.setInvisible(this.hider, true);
	}

	void disguise(LivingEntity clicked) {
		if (this.blockData != null)
			this.resetBlockTimer(null);

		this.blockData = null;

		if (this.disguise != null) {
			this.disguise.remove();
		}

		this.disguise = new AttachedHiderEntity(clicked.getType(), this.hider, true);
		for (WrappedWatchableObject obj : WrappedDataWatcher.getEntityWatcher(clicked).getWatchableObjects()) {
			if (obj.getIndex() == MetaIndex.BASE_BITFIELD_IDX) continue;
			this.disguise.setMetadata(obj.getWatcherObject(), obj.getValue());
		}
		this.disguise.updateMetadataPacket();
		this.disguise.respawn();

		PlayerUtils.setInvisible(this.hider, true);
	}

	void undisguise() {
		this.disguise.remove();
		this.disguise = null;

		if (this.blockData != null)
			this.resetBlockTimer(null);
		this.blockData = null;

		PlayerUtils.setInvisible(this.hider, false);
	}

	private void resetBlockTimer(BlockCoords newCoords) {
		breakExistingSolid();

		this.occupiedBlock = newCoords;
		this.blockChangeTick = TeamArena.getGameTick();
		if (newCoords != null) {
			this.bossbar.progress(0f);
			this.bossbar.name(SOLIDIFYING_COMP);
			this.bossbar.color(SOLIDIFYING_COLOUR);

			this.hider.showBossBar(this.bossbar);
		}
		else {
			this.hider.hideBossBar(this.bossbar);
		}
	}

	private void breakExistingSolid() {
		assert CompileAsserts.OMIT || // both are initialised or null
			((this.building == null) == (this.fbManagerKey == FakeBlockManager.INVALID_KEY));

		if (this.building != null) {
			BuildingManager.destroyBuilding(this.building);
			this.building = null;
		}
		this.setBlockDisplayData(false, this.occupiedBlock, true);

		//this.hider.getWorld().setBlockData(this.occupiedBlock.x(), this.occupiedBlock.y(), this.occupiedBlock.z(),
		//	Material.AIR.createBlockData());
		if (fbManagerKey != FakeBlockManager.INVALID_KEY) {
			this.game.getFakeBlockManager().removeFakeBlock(this.occupiedBlock, this.fbManagerKey);
			this.fbManagerKey = FakeBlockManager.INVALID_KEY;
			this.visualBlockEffect();
		}
	}

	private void setBlockDisplayData(boolean solid, BlockCoords coords, boolean refresh) {
		if (solid) { // Hider doesn't see their own solid block, so we use this display instead
			//this.disguise.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, nmsAirBlockState);
			this.disguise.setViewerRule(viewer -> viewer == this.hider);
			final Location loc = coords.toLocation(this.game.getWorld());
			this.hitbox.setSolid(loc.add(0.5, 0d, 0.5));
			this.disguise.setSolid(loc.add(0d, 0.5, 0d));
		}
		else {
			//this.disguise.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, nmsBlockState);
			this.disguise.setViewerRule(PacketEntity.VISIBLE_TO_ALL);
			this.hitbox.setSolid(null);
			this.disguise.setSolid(null);
		}

		if (refresh)
			this.disguise.refreshViewerMetadata();
		else
			this.disguise.updateMetadataPacket();
	}

	private void placeSolid() {
		if (this.building != null) {
			Main.logger().severe("");
			Thread.dumpStack();
			BuildingManager.destroyBuilding(this.building);
		}
		this.building = new BlockBuilding(this.hider, this.occupiedBlock.toLocation(this.hider.getWorld())) {};

		Block toReplace = this.occupiedBlock.toBlock(this.hider.getWorld());
		if (BuildingManager.canPlaceAt(toReplace)) {
			BuildingManager.placeBuilding(this.building);
		}
		else {
			assert CompileAsserts.OMIT || false : "placeSolid(): BuildingManager.canPlaceAt returned false";
		}

		// Avoid causing block updates (like wheat on stone is invalid)
		/*org.bukkit.block.BlockState bukkitState = toReplace.getState();
		bukkitState.setBlockData(this.nmsBlockState.createCraftBlockData());
		bukkitState.update(true, false);*/
		this.fbManagerKey = this.game.getFakeBlockManager()
			.setFakeBlock(this.occupiedBlock, this.blockData, viewer -> viewer != this.hider);

		this.setBlockDisplayData(true, this.occupiedBlock, true);

		this.hider.playSound(this.hider, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 2f, 1.5f);
		visualBlockEffect();
	}

	void tick() {
		final int currentTick = TeamArena.getGameTick();
		// action bar message on held item
		if (this.disguise != null && (currentTick - this.timerSeed) % 2 == 0 &&
			EntityUtils.isHoldingItem(this.hider)) {
			PlayerUtils.sendKitMessage(this.hider, null, HideAndSeek.HELD_ITEMS_VISIBLE);
		}

		if (this.blockData != null) {
			BlockCoords currentCoords = this.getCoords();
			// Use BuildingManager to prevent 2 solidifications in 1 block
			if (!currentCoords.equals(this.occupiedBlock)) {
				this.resetBlockTimer(currentCoords);
			}
			else {
				assert CompileAsserts.OMIT || currentTick >= this.blockChangeTick;

				if (currentTick <= this.blockChangeTick + BLOCK_SOLIDIFY_TICKS &&
					!BuildingManager.canPlaceAt(currentCoords.toBlock(this.hider.getWorld()))) {

					assert CompileAsserts.OMIT || (this.building == null);

					this.blockChangeTick = currentTick;

					this.bossbar.name(CANT_SOLID_HERE);
					this.bossbar.color(CANT_SOLID_HERE_COLOUR);
					this.bossbar.progress(0f);
				}
				else if (currentTick < this.blockChangeTick + BLOCK_SOLIDIFY_TICKS) {
					if (!this.occupiedBlock.toBlock(this.hider.getWorld()).getType().isAir()) {
						this.blockChangeTick = currentTick;

						this.bossbar.name(CANT_SOLID_HERE);
						this.bossbar.color(CANT_SOLID_HERE_COLOUR);
						this.bossbar.progress(0f);
					} else {
						this.bossbar.name(SOLIDIFYING_COMP);
						this.bossbar.color(SOLIDIFYING_COLOUR);

						float progress = (float) (currentTick - this.blockChangeTick);
						progress /= (float) BLOCK_SOLIDIFY_TICKS;
						progress = MathUtils.clamp(0f, 1f, progress);
						this.bossbar.progress(progress);
					}
				}
				else if (currentTick == this.blockChangeTick + BLOCK_SOLIDIFY_TICKS) {
					this.bossbar.name(SOLID_COMP);
					this.bossbar.color(SOLID_COLOUR);
					this.bossbar.progress(1f);

					this.placeSolid();
				}
			}
		}
	}

	private void visualBlockEffect() {
		ParticleUtils.blockBreakEffect(this.hider, this.blockData, this.hider.getLocation());
	}

	void remove() {
		if (this.blockData != null) {
			this.resetBlockTimer(null);
		}

		this.hitbox.remove();
		if (this.disguise != null)
			this.disguise.remove();

		this.hider.hideBossBar(this.bossbar);

		PlayerUtils.setInvisible(this.hider, false);
	}

	private static class AttachedHiderEntity extends AttachedPacketEntity {
		private final int hiderId;
		private boolean solid;

		public AttachedHiderEntity(EntityType type, Player hider, boolean selfSee) {
			super(PacketEntity.NEW_ID, type, hider, null, PacketEntity.VISIBLE_TO_ALL, selfSee,
				type != EntityType.BLOCK_DISPLAY);

			this.hiderId = hider.getEntityId();
			this.solid = false;
		}

		/*@Override
		public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
			if (player.getEntityId() == this.hiderId) return; // Don't interact with self!
			// Just redirect the interaction to the hider
			PacketContainer packet = PlayerUtils.createUseEntityPacket(player, hiderId, hand, attack);
			ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet, false);
		}*/

		@Override
		public double getYOffset() {
			return this.getEntityType() == EntityType.BLOCK_DISPLAY ? 0.5d : 0d;
		}

		private static final Vector3f nmsSmallScaleVec = new Vector3f(0.4f);
		private static final Vector3f nmsFullScaleVec = new Vector3f(1f);
		public void setSolid(Location loc) {
			if (loc != null) {
				this.move(loc, true);

				// TODO re-add these after 1.20.6 scale
				//this.setMetadata(MetaIndex.DISPLAY_SCALE_OBJ, nmsSmallScaleVec);
				//this.setMetadata(MetaIndex.DISPLAY_TRANSLATION_OBJ, new Vector3f(-0.2f));
				this.solid = true;
			}
			else {
				//this.setMetadata(MetaIndex.DISPLAY_SCALE_OBJ, nmsFullScaleVec);
				//this.setMetadata(MetaIndex.DISPLAY_TRANSLATION_OBJ, defaultTranslateVec);
				this.solid = false;
				this.move(this.getLocation(), true); // Hack
			}
		}

		@Override
		public void move(Location newLocation, boolean force) {
			if (!this.solid)
				super.move(newLocation, force);
		}

		@Override
		public boolean shouldRecreatePacket(PacketType packetType, Player viewer) {
			// no packet out when solid
			return !solid && super.shouldRecreatePacket(packetType, viewer);
		}

		@Override
		public void tick() {
			if (!solid)
				super.tick();
		}
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.building.BlockBuilding;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.utils.*;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.joml.Vector3f;

public class HiderDisguise {

	public static final int BLOCK_SOLIDIFY_TICKS = 40;

	private final Player hider;
	final int timerSeed;

	private AttachedPacketEntity hitbox;
	private AttachedPacketEntity disguise;

	private final BossBar bossbar;
	private static final Component SOLIDIFYING_COMP = Component.text("Solidifying...", NamedTextColor.AQUA);
	private static final BossBar.Color SOLIDIFYING_COLOUR = BossBar.Color.BLUE;
	private static final Component SOLID_COMP = Component.text("Solid", TextColor.color(255, 132, 0));
	private static final BossBar.Color SOLID_COLOUR = BossBar.Color.YELLOW;
	private static final Component CANT_SOLID_HERE = Component.text("Can't solidify here", TextColors.ERROR_RED);
	private static final BossBar.Color CANT_SOLID_HERE_COLOUR = BossBar.Color.RED;


	// Used if disguised as a block
	private BlockState nmsBlockState;
	private static final BlockState nmsAirBlockState = ((CraftBlockState) Material.AIR.createBlockData().createBlockState()).getHandle();
	private BlockCoords occupiedBlock;
	private int blockChangeTick;
	private BlockBuilding building; // Register to prevent overlaps

	HiderDisguise(final Player hider) {
		this.hitbox = new AttachedHiderEntity(EntityType.INTERACTION, hider);
		// TODO scale larger


		bossbar = BossBar.bossBar(SOLIDIFYING_COMP, 0f, SOLIDIFYING_COLOUR,
			BossBar.Overlay.PROGRESS);

		this.hider = hider;
		this.timerSeed = TeamArena.getGameTick() - MathUtils.randomMax(4);
	}

	// To try work with paved grass and odd blocks
	private BlockCoords getCoords() { return new BlockCoords(this.hider.getLocation().add(0, 0.1, 0)); }

	// Is this the NMS type?
	private static final Vector3f nmsVector = new Vector3f(-0.5f, -0.5f, -0.5f);
	void disguise(Block clicked) {
		if (this.nmsBlockState != null) {
			assert CompileAsserts.OMIT || this.occupiedBlock != null;
			this.breakExistingSolid();
		}

		this.nmsBlockState = ((CraftBlockState) clicked.getState()).getHandle();

		boolean respawn = false; // need to refresh meta manually if already existed
		if (this.disguise == null || this.disguise.getEntityType() != EntityType.BLOCK_DISPLAY) {
			if (this.disguise != null) {
				this.disguise.remove();
			}

			respawn = true;
			this.disguise = new AttachedHiderEntity(EntityType.BLOCK_DISPLAY, this.hider);

			// Invis makes clients not render the blue line when viewing hitboxes
			this.disguise.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);

			this.disguise.setMetadata(MetaIndex.DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ, 2);
			this.disguise.setMetadata(MetaIndex.DISPLAY_TRANSLATION_OBJ, nmsVector);
		}

		this.disguise.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, nmsBlockState);
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
		if (this.nmsBlockState != null)
			this.resetBlockTimer(null);

		this.nmsBlockState = null;

		if (this.disguise != null) {
			this.disguise.remove();
		}

		this.disguise = new AttachedHiderEntity(clicked.getType(), this.hider);
		for (WrappedWatchableObject obj : WrappedDataWatcher.getEntityWatcher(clicked).getWatchableObjects()) {
			this.disguise.setMetadata(obj.getWatcherObject(), obj.getValue());
		}
		this.disguise.updateMetadataPacket();
		this.disguise.respawn();

		PlayerUtils.setInvisible(this.hider, true);
	}

	void undisguise() {
		this.disguise.remove();
		this.disguise = null;

		if (this.nmsBlockState != null)
			this.resetBlockTimer(null);
		this.nmsBlockState = null;

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
		if (TeamArena.getGameTick() >= this.blockChangeTick + BLOCK_SOLIDIFY_TICKS) {
			if (!CompileAsserts.OMIT &&
				this.occupiedBlock.toBlock(this.hider.getWorld()).getType() != this.nmsBlockState.getBukkitMaterial()) {
				Main.logger().severe("Hider block and actual block didn't match. " +
					this.nmsBlockState.toString() + ", " + this.occupiedBlock.toBlock(this.hider.getWorld()));
				Thread.dumpStack();
			}

			if (this.building == null) {
				Main.logger().severe("breakExistingSolid() building was null");
				Thread.dumpStack();
			}
			else {
				BuildingManager.destroyBuilding(this.building);
				this.building = null;
			}
			this.setBlockDisplayData(false, true);

			this.hider.getWorld().setBlockData(this.occupiedBlock.x(), this.occupiedBlock.y(), this.occupiedBlock.z(),
				Material.AIR.createBlockData());
			this.visualBlockEffect();
		}
	}

	private void setBlockDisplayData(boolean air, boolean refresh) {
		if (air)
			this.disguise.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, nmsAirBlockState);
		else
			this.disguise.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, nmsBlockState);

		if (refresh)
			this.disguise.refreshViewerMetadata();
		else
			this.disguise.updateMetadataPacket();
	}

	private void placeSolid() {
		this.nmsBlockState.createCraftBlockData().createBlockState().update(false, false);
		Block toReplace = this.occupiedBlock.toBlock(this.hider.getWorld());

		if (this.building != null) {
			Main.logger().severe("");
			Thread.dumpStack();
			BuildingManager.destroyBuilding(this.building);
		}
		this.building = new BlockBuilding(this.hider, this.occupiedBlock.toLocation(this.hider.getWorld())) {};
		if (BuildingManager.canPlaceAt(toReplace)) {
			BuildingManager.placeBuilding(this.building);
		}
		else {
			assert CompileAsserts.OMIT || false : "placeSolid(): BuildingManager.canPlaceAt returned false";
		}

		// Avoid causing block updates (like wheat on stone is invalid)
		org.bukkit.block.BlockState bukkitState = toReplace.getState();
		bukkitState.setBlockData(this.nmsBlockState.createCraftBlockData());
		bukkitState.update(true, false);

		this.setBlockDisplayData(true, true);

		this.hider.playSound(this.hider, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 2f, 1.5f);
		visualBlockEffect();
	}

	void tick() {
		if (this.nmsBlockState != null) {
			BlockCoords currentCoords = this.getCoords();
			// Use BuildingManager to prevent 2 solidifications in 1 block
			if (!currentCoords.equals(this.occupiedBlock)) {
				this.resetBlockTimer(currentCoords);
			}
			else {
				final int currentTick = TeamArena.getGameTick();
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
		ParticleUtils.blockBreakEffect(this.hider, this.nmsBlockState.getBukkitMaterial(), this.hider.getLocation());
	}

	void remove() {
		if (this.nmsBlockState != null) {
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

		public AttachedHiderEntity(EntityType type, Player hider) {
			super(PacketEntity.NEW_ID, type, hider, null, PacketEntity.VISIBLE_TO_ALL, true,
				type != EntityType.BLOCK_DISPLAY);

			this.hiderId = hider.getEntityId();
		}

		@Override
		public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
			if (player.getEntityId() == this.hiderId) return; // Don't interact with self!
			// Just redirect the interaction to the hider
			PacketContainer packet = PlayerUtils.createUseEntityPacket(player, hiderId, hand, attack);
			ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet, false);
		}

		@Override
		public double getYOffset() {
			return this.getEntityType() == EntityType.BLOCK_DISPLAY ? 0.5d : 0d;
		}
	}
}

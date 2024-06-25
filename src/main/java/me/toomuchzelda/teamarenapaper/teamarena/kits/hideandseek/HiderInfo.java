package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.joml.Vector3f;

public class HiderInfo {

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
	private BlockCoords occupiedBlock;
	private int blockChangeTick;

	HiderInfo(final Player hider) {
		final int hiderId = hider.getEntityId();

		this.hitbox = new AttachedHiderEntity(EntityType.INTERACTION, hider, hiderId);
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

		boolean wasNull = false; // need to refresh meta manually if already existed
		if (this.disguise == null) {
			wasNull = true;
			this.disguise = new AttachedHiderEntity(EntityType.BLOCK_DISPLAY, this.hider,
				this.hider.getEntityId());

			this.disguise.setMetadata(MetaIndex.DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ, 2);
			this.disguise.setMetadata(MetaIndex.DISPLAY_TRANSLATION_OBJ, nmsVector);
		}

		this.disguise.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, nmsBlockState);
		this.disguise.updateMetadataPacket();

		this.disguise.respawn();
		if (!wasNull)
			this.disguise.refreshViewerMetadata();

		BlockCoords coords = this.getCoords();
		this.occupiedBlock = coords;
		this.blockChangeTick = TeamArena.getGameTick();
		this.resetBlockTimer(coords);
	}

	void disguise(LivingEntity clicked) {

	}

	void undisguise() {
		this.disguise.remove();
		this.disguise = null;

		this.resetBlockTimer(null);
		this.nmsBlockState = null;
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
			assert CompileAsserts.OMIT || this.occupiedBlock.toBlock(this.hider.getWorld()).getType() == this.nmsBlockState.getBukkitMaterial();
			this.hider.getWorld().setBlockData(this.occupiedBlock.x(), this.occupiedBlock.y(), this.occupiedBlock.z(),
				Material.AIR.createBlockData());
			this.visualBlockEffect();
		}
	}

	void tick() {
		if (this.nmsBlockState != null) {
			BlockCoords currentCoords = this.getCoords();
			if (!currentCoords.equals(this.occupiedBlock)) {
				this.resetBlockTimer(currentCoords);
			}
			else {
				final int currentTick = TeamArena.getGameTick();
				assert CompileAsserts.OMIT || currentTick >= this.blockChangeTick;
				if (currentTick < this.blockChangeTick + BLOCK_SOLIDIFY_TICKS) {
					if (!this.occupiedBlock.toBlock(this.hider.getWorld()).getType().isAir()) {
						this.blockChangeTick = currentTick;

						this.bossbar.name(CANT_SOLID_HERE);
						this.bossbar.color(CANT_SOLID_HERE_COLOUR);
						this.bossbar.progress(0f);
					}
					else {
						this.bossbar.name(SOLIDIFYING_COMP);
						this.bossbar.color(SOLIDIFYING_COLOUR);

						float progress = (float) (currentTick - this.blockChangeTick);
						progress /= (float) BLOCK_SOLIDIFY_TICKS;
						progress = MathUtils.clamp(0f, 1f, progress);
						this.bossbar.progress(progress);
					}
				} else if (currentTick == this.blockChangeTick + BLOCK_SOLIDIFY_TICKS) {
					this.bossbar.name(SOLID_COMP);
					this.bossbar.color(SOLID_COLOUR);
					this.bossbar.progress(1f);

					// TODO set the physical block
					hider.getWorld().setBlockData(this.occupiedBlock.x(), this.occupiedBlock.y(), this.occupiedBlock.z(),
						this.nmsBlockState.createCraftBlockData());

					this.hider.playSound(this.hider, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 2f, 1.5f);
					visualBlockEffect();
				}
			}
		}
	}

	private void visualBlockEffect() {
		ParticleUtils.blockBreakEffect(this.hider, this.nmsBlockState.getBukkitMaterial(), this.hider.getLocation());
	}

	void remove() {
		this.hitbox.remove();
		if (this.disguise != null)
			this.disguise.remove();

		this.hider.hideBossBar(this.bossbar);
	}

	private static class AttachedHiderEntity extends AttachedPacketEntity {
		private final int hiderId;

		public AttachedHiderEntity(EntityType type, Player hider, int hiderId) {
			super(PacketEntity.NEW_ID, type, hider, null, PacketEntity.VISIBLE_TO_ALL, true, false);
			this.hiderId = hiderId;
		}

		@Override
		public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
			// Just redirect the interaction to the hider
			PacketContainer packet = PlayerUtils.createUseEntityPacket(player, hiderId, hand, attack);
			ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet, false);
		}

		@Override
		public double getYOffset() {
			return 0.5d;
		}
	}
}

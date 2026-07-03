package me.toomuchzelda.teamarenapaper.teamarena.abilities;

import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketDisplay;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class GasterBlasterAbility extends Ability {
	private static final NamespacedKey KEY = new NamespacedKey(Main.getPlugin(), "sansblaster");
	public static final ItemStack ITEM = ItemBuilder.of(Material.BONE)
		.displayName(Component.text("Gaster Blaster", NamedTextColor.WHITE))
		.setPDCFlag(KEY)
		.build();

	private static final BlockData SKULL_BLOCK_DATA = Material.SKELETON_SKULL.createBlockData(); // cache
	private static final BlockData BEAM_BLOCK_DATA = Material.WHITE_CONCRETE.createBlockData(); // cache
	private static final int SKULL_ROTATION_TICKS = 8;
	private static final int SKULL_BEAM_RESIDUAL_TICKS = SKULL_ROTATION_TICKS + 15;
	private static final float BEAM_LENGTH = 30f;

	private static class BlasterInfo {
		private final Player shooter;
		private final int shootTime;
		private final Location spawnLoc;
		private PacketDisplay skull;
		private PacketDisplay beam;

		public BlasterInfo(Player shooter, int shootTime, Location spawnLoc) {
			this.shooter = shooter;
			this.shootTime = shootTime;
			this.spawnLoc = spawnLoc;
		}
	}

	private final TeamArena game;
	private final List<BlasterInfo> blasters = new ArrayList<>();

	public GasterBlasterAbility(TeamArena game) {
		this.game = game;
	}

	@Override
	protected void removeAbility(Player player) {
		blasters.removeIf(binfo -> {
				if (binfo.shooter.equals(player.getUniqueId())) {
					if (binfo.skull != null) binfo.skull.remove();
					if (binfo.beam != null) binfo.beam.remove();
					return true;
				}
				else return false;
			}
		);
	}

	@Override
	public void unregisterAbility() {
		blasters.forEach(binfo -> {
			if (binfo.skull != null) binfo.skull.remove();
			if (binfo.beam != null) binfo.beam.remove();
		});
		blasters.clear();
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (event.getAction() != Action.PHYSICAL && ITEM.isSimilar(event.getItem()) && !player.hasCooldown(event.getItem())) {
			this.blasters.add(new BlasterInfo(
				player, TeamArena.getGameTick(),
				player.getEyeLocation()
			));
			player.setCooldown(ITEM, SKULL_ROTATION_TICKS + SKULL_BEAM_RESIDUAL_TICKS + 5);
		}
	}

	@Override
	public void onTick() {
		final List<BlasterInfo> toRemove = new ArrayList<>(this.blasters.size() / 5);
		final int currentTick = TeamArena.getGameTick();
		for (final BlasterInfo binfo : this.blasters) {
			final int timeDiff = currentTick - binfo.shootTime;
			if (timeDiff < SKULL_ROTATION_TICKS) {
				final Location animLoc = binfo.spawnLoc.clone();
				animLoc.setDirection(animLoc.getDirection().multiply(-1d));
				if (timeDiff == 0) { // spawn
					assert binfo.skull == null;
					binfo.skull = new PacketDisplay(PacketEntity.NEW_ID, EntityType.BLOCK_DISPLAY, animLoc, null, PacketEntity.VISIBLE_TO_ALL);
					binfo.skull.setBlockData(SKULL_BLOCK_DATA);
					binfo.skull.setTeleportDuration(1);
					binfo.skull.translate(new Vector(-0.5d, -0.25d, -0.5d));
					binfo.skull.updateMetadataPacket();
					binfo.skull.respawn();
				}
				else { // spin
					final float diff = (float) timeDiff / (float) SKULL_ROTATION_TICKS;
					final float degrees = 180f * (diff * diff);
					animLoc.setRotation(
						animLoc.getYaw() + degrees, animLoc.getPitch()
					);
					animLoc.add(binfo.spawnLoc.getDirection().multiply(diff));
					binfo.skull.move(animLoc);
				}
			}
			else if (timeDiff == SKULL_ROTATION_TICKS) { // shoot
				final Location shootLoc = binfo.spawnLoc.clone().add(binfo.spawnLoc.getDirection());
				binfo.skull.move(shootLoc);

				assert binfo.beam == null;
				shootLoc.add(shootLoc.getDirection().multiply(1d));
				binfo.beam = new PacketDisplay(PacketEntity.NEW_ID, EntityType.BLOCK_DISPLAY, shootLoc, null, PacketEntity.VISIBLE_TO_ALL);
				binfo.beam.setBlockData(BEAM_BLOCK_DATA);
				binfo.beam.translate(new Vector(-0.4d, -0.4d, -0.4d));
				binfo.beam.setScale(new Vector(0.8f, 0.8f, BEAM_LENGTH));
				binfo.beam.updateMetadataPacket();
				binfo.beam.setBrightnessOverride(new Display.Brightness(15, 15));
				binfo.beam.respawn();
			}
			else if (timeDiff < SKULL_BEAM_RESIDUAL_TICKS) {
				this.beamDamage(binfo.shooter, binfo);
			}
			else if (timeDiff == SKULL_BEAM_RESIDUAL_TICKS) {
				final List<PacketContainer> packets = new ArrayList<>((int) BEAM_LENGTH + 1);
				final Location loc = binfo.skull.getLocation();
				final Vector dir = loc.getDirection();
				final Vector vec = loc.toVector();
				for (int i = 0; i < (int) BEAM_LENGTH + 1; i++) {
					packets.add(ParticleUtils.batchParticles(
						Particle.CLOUD, null,
						vec.getX(),
						vec.getY(),
						vec.getZ(),
						2,
						0f, 0f, 0f,
						0.1f, false
					));

					vec.add(dir);
				}

				PlayerUtils.sendPacket(binfo.skull.getRealViewers(), packets); // batch

				binfo.skull.remove();
				binfo.beam.remove();

				toRemove.add(binfo);
			}
			else {
				toRemove.add(binfo);
			}
		}

		this.blasters.removeAll(toRemove);
	}

	private void beamDamage(Player shooter, BlasterInfo binfo) {
		final World world = binfo.beam.getWorld();

		final List<Entity> hitList = new ArrayList<>();
		final Location loc = binfo.beam.getLocationMut();
		for (int i = 0; i < 20; i++) { // hit max 20 victims
			final RayTraceResult raytrace = world.rayTraceEntities(
				loc, loc.getDirection(), BEAM_LENGTH, 0.4d,
				victim -> victim instanceof LivingEntity livingVictim &&
					!hitList.contains(victim) &&
					!this.game.isDead(livingVictim) &&
					this.game.canAttack(shooter, livingVictim)
			);

			if (raytrace != null) {
				assert raytrace.getHitEntity() != null;
				final Entity hitEntity = raytrace.getHitEntity();
				if (hitEntity != null) {
					hitList.add(hitEntity);

					final DamageEvent dEvent = DamageEvent.newDamageEvent(hitEntity, 1d,
						DamageType.SANS_BEAM,shooter, false);
					this.game.queueDamage(dEvent);
				}
			}
		}
	}
}

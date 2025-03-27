package me.toomuchzelda.teamarenapaper.teamarena.abilities;

import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.PacketListeners;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitbox;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketHologram;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketDisplay;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * An ability that
 */
public class CritAbility extends Ability {

	private static class CritHitbox {
		private static final Component CUSTOM_NAME = Component.text("You shouldn't see this");
		private static final float BOX_HEIGHT = 0.3f;

		private final LivingEntity followed;
		// Interaction entities aren't interpolated, so we use this as a vehicle
		// to have it ride. Then the interaction should perfectly match the position
		// of the player on the clientside.
		private final PacketEntity interaction;
		private final PacketDisplay flash; // For flash vfx
		private final AttachedPacketHologram vehicle;

		private float width;
		private final PacketContainer mountPacket;

		public CritHitbox(LivingEntity followed, CritAbility critAbility) {
			this.followed = followed;

			final Predicate<Player> viewerRule =
				viewer -> viewer != followed && viewer.canSee(followed);

			this.vehicle = new AttachedPacketHologram(PacketEntity.NEW_ID, this.followed, null,
				viewerRule, CUSTOM_NAME, false) {
				@Override
				public double getYOffset() {
					return getBoxYOffset(entity);
				}
			};
			this.vehicle.setCustomNameVisible(false);
			this.vehicle.updateMetadataPacket();

			interaction = new PacketEntity(PacketEntity.NEW_ID, EntityType.INTERACTION, followed.getLocation(), null,
				viewer -> this.getVehicle().getRealViewers().contains(viewer) &&
					Ability.getAbility(viewer, CritAbility.class) != null) {
				@Override
				public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
					if (player == followed) return;
					if (attack)
						critAbility.crit(player, followed);
					else { // just pass the interaction to the entity
						super.onInteract(player, hand, attack);
					}
				}

				// Always mount the vehicle. The vehicle should always be alive when this is called
				// since it was registed before this
				@Override
				public void respawn() {
					super.respawn();
					this.broadcastPacket(CritHitbox.this.mountPacket);
				}
			};

			this.width = getWidth(followed);
			if (!KitOptions.splitterVisible)
				this.interaction.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);

			final Location flashLoc = this.vehicle.getLocation();
			flashLoc.setPitch(0f); flashLoc.setYaw(0f);
			this.flash = new PacketDisplay(PacketEntity.NEW_ID, EntityType.BLOCK_DISPLAY, flashLoc,
				null, viewer -> this.getVehicle().getRealViewers().contains(viewer)) {
				@Override
				public void respawn() {
					super.respawn();
					this.broadcastPacket(CritHitbox.this.mountPacket);
				}
			};
			this.flash.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, (byte) MetaIndex.BASE_BITFIELD_INVIS_MASK);
			this.flash.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, ((CraftBlockData) Material.AIR.createBlockData()).getState());

			this.updateScale(this.width);

			this.mountPacket = EntityUtils.getMountPacket(this.vehicle.getId(), this.interaction.getId(), this.flash.getId());
		}

		public void spawn() {
			this.vehicle.respawn();
			this.interaction.respawn();
			this.flash.respawn();
		}

		public void remove() {
			this.flash.remove();
			this.interaction.remove();
			this.vehicle.remove();
		}

		private PacketEntity getVehicle() {
			return this.vehicle;
		}

		private PacketEntity getInteraction() { return this.interaction; }

		private void tick() {
			float currentWidth = getWidth(followed);
			if (currentWidth != this.width) {
				this.width = currentWidth;

				// update vehicle position
				this.vehicle.move();
				// resize interaction and block display
				this.updateScale(currentWidth);
			}
		}

		private void updateScale(float width) {
			this.interaction.setMetadata(MetaIndex.INTERACTION_HEIGHT_OBJ, BOX_HEIGHT);
			this.interaction.setMetadata(MetaIndex.INTERACTION_WIDTH_OBJ, width);
			this.interaction.refreshViewerMetadata();

			this.flash.translate(new Vector(-0.5d, 0d, -0.5d));
			this.flash.setScale(new Vector3f(width, BOX_HEIGHT, width));
			this.flash.refreshViewerMetadata();
		}

		public void flash() {
			final byte glowOn = (byte) (MetaIndex.BASE_BITFIELD_GLOWING_MASK | MetaIndex.BASE_BITFIELD_INVIS_MASK);
			this.flash.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, glowOn);
			this.flash.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ,
				((CraftBlockData) Material.RED_STAINED_GLASS.createBlockData()).getState());
			this.flash.refreshViewerMetadata();

			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
				this.flash.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
				this.flash.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ,
					((CraftBlockData) Material.AIR.createBlockData()).getState());
				this.flash.refreshViewerMetadata();
			}, 1L);
		}

		private static float getWidth(Entity e) {
			double base = e.getWidth();
			if (e instanceof Player p && FakeHitboxManager.ACTIVE) {
				final double scale = EntityUtils.getScale(e);
				base += (FakeHitbox.OFFSET_DEFAULT * scale * 2d);
			}

			return (float) (base + 0.07f);
		}

		public static double getBoxYOffset(Entity e) {
			return (e.getHeight() / 2d) - (BOX_HEIGHT / 4d);
		}
	}

	private static final double DAMAGE_MULT = 2d;

	private final TeamArena game;
	// Every player online has 1
	private final Map<LivingEntity, CritHitbox> critHitboxes = new HashMap<>();

	public CritAbility(TeamArena game) {
		this.game = game;
	}

	@Override
	public void registerAbility() {
		//Bukkit.broadcastMessage("registered ability");
		addHitboxes();
	}

	private boolean isValidCritCandidate(LivingEntity living) {
		if (living instanceof ArmorStand stand && stand.isMarker()) return false;
		if (this.game.isDead(living)) return false;
		if (this.game instanceof CaptureTheFlag ctf && ctf.isFlagEntity(living)) return false;

		return true;
	}

	private void addHitboxes() {
		this.game.getWorld().getLivingEntities().forEach(living -> {
				if (this.isValidCritCandidate(living)) {
					critHitboxes.computeIfAbsent(living,
							lv -> new CritHitbox(living, this)
					).spawn();
				}
			}
		);
	}

	@Override
	public void unregisterAbility() {
		critHitboxes.forEach((livingEntity, critHitbox) -> critHitbox.remove());
		critHitboxes.clear();
	}

	// Add / remove missing players
	@Override
	public void onTick() {
		var iter = critHitboxes.entrySet().iterator();
		while (iter.hasNext()) {
			var entry = iter.next();
			if (!this.isValidCritCandidate(entry.getKey())) {
				entry.getValue().remove();
				iter.remove();
			}
		}
		addHitboxes();
		this.critHitboxes.values().forEach(CritHitbox::tick);
	}

	@Override
	public void onAttemptedAttack(DamageEvent event) {
		if (event.getDamageType().is(DamageType.RATIO_CRIT)) {
			event.setRawDamage(event.getRawDamage() * DAMAGE_MULT);
		}
	}

	@Override
	public void onDealtAttack(DamageEvent event) {
		if (event.getDamageType().is(DamageType.RATIO_CRIT)) {
			Entity attacker = event.getAttacker();
			assert CompileAsserts.OMIT || attacker != null;
			// splitterEffect(attacker, event.getVictim()); Done by TeamArena
		}
	}

	/** Play effects for anyone who got critted */
	public void onSuccessfulCrit(LivingEntity attacker, LivingEntity victim) {
		World world = attacker.getWorld();
		world.playSound(attacker, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.f, 0.9f);
		for (long i = 0; i < 3; i++) {
			final float volume = 1.f - (((float) i) / 3f);
			playCritSound(world, attacker, i * 2, volume, 1.1f + (((float) i) / 3f));
			playCritSound(world, attacker, (i * 2) + 1, volume, 1.3f + (((float) i) / 3f));
		}

		ParticleUtils.bloodEffect(victim);

		// Make the vulnerable part flash
		CritHitbox critHitbox = this.critHitboxes.get(victim);
		critHitbox.flash();
	}

	private static void playCritSound(World world, Entity attacker, long delay, float volume, float pitch) {
		Bukkit.getScheduler().runTaskLater(Main.getPlugin(),
			() -> {
				boolean orig = PacketListeners.cancelDamageSounds;
				PacketListeners.cancelDamageSounds = false;
				world.playSound(attacker, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, volume, pitch);
				PacketListeners.cancelDamageSounds = orig;
			}, delay);
	}

	private void crit(Player attacker, LivingEntity victim) {
		DamageEvent ratio = DamageEvent.newDamageEvent(victim, 1d, DamageType.RATIO_CRIT, attacker, false);
		this.game.queueDamage(ratio);
	}
}

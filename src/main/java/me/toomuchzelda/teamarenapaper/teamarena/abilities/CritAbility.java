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
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;

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
		private final AttachedPacketHologram vehicle;

		private float width;
		private float vehicleWidth; // not the vehicle's width

		public CritHitbox(LivingEntity followed, CritAbility critAbility) {
			this.followed = followed;

			final Predicate<Player> viewerRule =
				viewer -> viewer != followed && Ability.getAbility(viewer, CritAbility.class) != null;

			this.vehicle = new AttachedPacketHologram(PacketEntity.NEW_ID, this.followed, null,
				viewerRule, CUSTOM_NAME, false) {
				@Override
				public double getYOffset() {
					return getBoxYOffset(entity);
				}

				@Override
				public void tick() {
					super.tick();
					float currentWidth = getWidth(followed);
					if (currentWidth != CritHitbox.this.vehicleWidth) {
						CritHitbox.this.vehicleWidth = currentWidth;
						this.updateTeleportPacket();
						this.broadcastPacket(this.getTeleportPacket());
					}
				}
			};
			this.vehicle.setCustomNameVisible(false);
			this.vehicle.updateMetadataPacket();

			interaction = new PacketEntity(PacketEntity.NEW_ID, EntityType.INTERACTION, followed.getLocation(), null,
				viewerRule) {
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
				// since it was registed before this, and the viewerrules are identical.
				@Override
				public void respawn() {
					super.respawn();
					PacketContainer mountPacket = EntityUtils.getMountPacket(CritHitbox.this.getVehicle().getId(), this.getId());
					this.broadcastPacket(mountPacket);
				}

				@Override
				public void tick() {
					super.tick();
					float currentWidth = getWidth(followed);
					if (currentWidth != CritHitbox.this.width) {
						CritHitbox.this.width = currentWidth;
						this.setMetadata(MetaIndex.INTERACTION_WIDTH_OBJ, currentWidth);
						this.refreshViewerMetadata();
					}
				}
			};

			this.width = getWidth(followed);
			this.vehicleWidth = width;
			if (!KitOptions.trooperRatioVisible)
				this.interaction.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
			this.interaction.setMetadata(MetaIndex.INTERACTION_WIDTH_OBJ, this.width);
			this.interaction.setMetadata(MetaIndex.INTERACTION_HEIGHT_OBJ, BOX_HEIGHT);
			this.interaction.updateMetadataPacket();
		}

		public void spawn() {
			this.vehicle.respawn();
			this.interaction.respawn();
		}

		public void remove() {
			this.interaction.remove();
			this.vehicle.remove();
		}

		private PacketEntity getVehicle() {
			return this.vehicle;
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

	public static void splitterEffect(Entity attacker, Entity victim) {
		World world = attacker.getWorld();
		world.playSound(attacker, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.f, 0.9f);
		for (long i = 0; i < 3; i++) {
			final float volume = 1.f - (((float) i) / 3f);
			playCritSound(world, attacker, i * 2, volume, 1.1f + (((float) i) / 3f));
			playCritSound(world, attacker, (i * 2) + 1, volume, 1.3f + (((float) i) / 3f));
		}

		ParticleUtils.bloodEffect(victim);
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

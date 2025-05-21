package me.toomuchzelda.teamarenapaper.teamarena.damage;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static me.toomuchzelda.teamarenapaper.teamarena.GameState.LIVE;

//a custom damage event for custom knockback and other customisability bukkit/spigot/paper can't provide
public class DamageEvent {

	private final Entity victim;
	//damage before damage-reduction calculations
	private double rawDamage; // full raw damage
	private double baseDamage; //base damage if any
	private double enchantDamage; //enchantment damage from the item if any

	//damage after damage-reduction calculations
	private double finalDamage;
	private DamageType damageType;
	private Entity damageTypeCause; // for %Cause% in DamageType deathmessages
	@Nullable
	private DamageType.DeathMessage deathMessageOverride;
	private boolean broadcastsDeathMessage;

	//null implies no knockback
	// dont use 0,0,0 vector as that'll stop the player moving for a split moment
	private Vector knockback;
	private double knockbackLevels;
	//from 0 to 1, 0 = no knockback received on XZ, 1 = all knockback received on XZ
	private double knockbackResistance;

	private Entity attacker;
	//shooter of arrow, snowball etc where attacker would be the projectile
	private Entity realAttacker;
	private boolean isCritical;
	//if the attacker was sprinting, only applicable if the attacker is a Player
	private boolean wasSprinting;
	private boolean sweepEffect = false;
	private boolean ignoreInvulnerability = false;
	private ItemStack meleeWeapon; // melee attacks only

	private boolean cancelled;

	//for knockback
	public static final double yVal = 0.4;
	public static final double xzVal = 0.4;
	public static final double yMult = 0.1;
	public static final double xzMult = 1;

	public static @Nullable DamageEvent handleBukkitEvent(EntityDamageEvent event) {
		event.setCancelled(true);

		// Always play fall damage sound
		if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
			if (event.getEntity() instanceof LivingEntity living)
				EntityUtils.playFallDamageSound(living, event.getDamage());
		}

		if(Main.getGame().getGameState() != LIVE)
			return null;

		final EntityDamageEvent.DamageCause damageCause = event.getCause();

		//Handle entity fire ourselves
		if(damageCause == EntityDamageEvent.DamageCause.FIRE_TICK)
			return null;

		//handle poison ourselves
		if(damageCause == EntityDamageEvent.DamageCause.POISON)
			return null;

		//marker armorstands must never be damaged/killed
		if(event.getEntity() instanceof ArmorStand stand && stand.isMarker())
			return null;

		if (event.getEntity() instanceof Painting)
			return null;

		//damage only occur in game world
		if(event.getEntity().getWorld() != Main.getGame().getWorld())
			return null;

		//prevent spectators from getting hurt
		if(event.getEntity() instanceof Player p && Main.getGame().isSpectator(p))
			return null;


		if(event instanceof EntityDamageByEntityEvent dEvent) {
			if(dEvent.getDamager() instanceof Player p && Main.getGame().isSpectator(p))
				return null;
			else if (damageCause == EntityDamageEvent.DamageCause.PROJECTILE && dEvent.getDamager() instanceof AbstractArrow aa) {
				//fix arrow damage - no random crits
				//  arrow damage is the vanilla formula without the random crit part
				double damage = DamageNumbers.calcArrowDamage(aa, aa.getVelocity().length());
				dEvent.setDamage(damage);
			}
			// Stop ender-pearls from doing fall damage
			else if (damageCause == EntityDamageEvent.DamageCause.FALL && dEvent.getDamager() instanceof EnderPearl)
				return null;
		}

		//Bukkit.broadcastMessage("EDEvent raw damage: " + event.getDamage());
		//Bukkit.broadcastMessage("EDEvent final damage: " + event.getFinalDamage());

		DamageEvent dEvent = fromEDE(event);
		Main.getGame().queueDamage(dEvent);
		return dEvent;
	}

	private DamageEvent(Entity victim, double damage) {
		this.victim = victim;
		this.rawDamage = damage;
		this.cancelled = false;
		this.broadcastsDeathMessage = true;
	}

	public static DamageEvent newDamageEvent(@NotNull Entity victim, double rawDamage, @NotNull DamageType damageType,
											 @Nullable Entity attacker, boolean critical) {

		DamageEvent damageEvent = new DamageEvent(victim, rawDamage);

		double finalDamage = rawDamage;

		//if it's fire caused by a non-specified attacker, set the damager from the cached DamageTimes
		// sort of re-construct this DamageEvent so it's accurate
		// Note: burn damagetypes are NOT inherently fire
		if(damageType.isFire() && attacker == null) {
			if(victim instanceof LivingEntity living) {
				DamageTimes.DamageTime dTime = DamageTimes.getDamageTime(living, DamageTimes.TrackedDamageTypes.FIRE);
				if(dTime.getGiver() != null && dTime.getDamageType() != null) {
					attacker = dTime.getGiver();
					damageType = dTime.getDamageType();
				}
			}
		}
		// also attribute fall damage if they were pushed
		else if((damageType.isFall() || damageType.is(DamageType.VOID)) && victim instanceof LivingEntity living) {
			DamageTimes.DamageTime times = DamageTimes.getDamageTime(living, DamageTimes.TrackedDamageTypes.ATTACK);

			if(times.getGiver() != null && TeamArena.getGameTick() - times.getLastTimeDamaged() < 10 * 20) { // 10 seconds since last attacked
				if(times.getDamageType().is(DamageType.PROJECTILE)) {
					if(damageType.isFall())
						damageType = DamageType.FALL_SHOT;
					else
						damageType = DamageType.VOID_SHOT;
				}
				else {
					if(damageType.isFall())
						damageType = DamageType.FALL_PUSHED;
					else
						damageType = DamageType.VOID_PUSHED;
				}

				attacker = times.getGiver();
			}
		}

		boolean doBaseKB = true;
		double knockbackLevels = 0;
		double knockbackResistance = 1;

		boolean alreadyCalcedArmor = false;
		Entity realAttacker = null;

		ItemStack weapon = null;

		if(attacker != null) {
			//damageEvent.isCritical = dEvent.isCritical();
			if(attacker instanceof Projectile projectile) {
				if(attacker instanceof AbstractArrow aa) {
					weapon = aa.getWeapon();
					if (weapon != null) {
						knockbackLevels += weapon.getEnchantmentLevel(Enchantment.PUNCH);
					}
				}

				if (projectile.getShooter() instanceof LivingEntity living) {
					realAttacker = living;
				}
			}
			else if(attacker instanceof TNTPrimed tnt) {
				Entity tntSource = tnt.getSource();
				if(tntSource != null) {
					realAttacker = tntSource;
				}
			}
			else if(attacker instanceof Item item) {
				UUID throwerUuid = item.getThrower();
				if(throwerUuid != null) {
					Player thrower = Bukkit.getPlayer(throwerUuid);
					if(thrower != null)
						realAttacker = thrower;
				}
			}
			else if(attacker instanceof LivingEntity living) {
				if(damageType.isMelee() && living.getEquipment() != null) {
					//item used during the attack, if applicable
					weapon = living.getEquipment().getItemInMainHand();

					double[] damages = DamageCalculator.calcItemDamageOnEntity(living, weapon, damageType, critical, victim);
					damageEvent.baseDamage = damages[0];
					damageEvent.enchantDamage = damages[1];
					damageEvent.rawDamage = damages[0] + damages[1];
					finalDamage = damages[2];

					alreadyCalcedArmor = true;

					//halve the strength of knockback enchantments
					knockbackLevels += ((float) weapon.getEnchantmentLevel(Enchantment.KNOCKBACK)) / 2;
					//knockbackMults.add(level);

					//cancelled bukkit event doesn't do sweeping attacks, re-do them here
					if(living instanceof Player p && damageType.is(DamageType.MELEE)) {
						//Bukkit.broadcastMessage("DamageType is melee: line 99");
						//same as nmsP.getAttackStrengthCooldown(0.5f);
						boolean isChargedWeapon = p.getAttackCooldown() > 0.9f;

						boolean isChargedSprintAttack = isChargedWeapon && p.isSprinting();

						net.minecraft.world.entity.player.Player nmsPlayer = ((CraftPlayer) p).getHandle();
						double walkedDist = nmsPlayer.getKnownMovement().horizontalDistanceSqr();
						boolean notExceedingWalkSpeed = walkedDist < MathUtils.square(p.getAttribute(Attribute.MOVEMENT_SPEED).getValue() * 2.5d);
						boolean sweep = false;

						if(isChargedWeapon && !isChargedSprintAttack && !critical &&
								notExceedingWalkSpeed) {
							if(ItemUtils.isSword(weapon))
								sweep = true;
						}

						if(sweep) {
							float sweepingEdgeDmg = (float) (1f + p.getAttribute(Attribute.SWEEPING_DAMAGE_RATIO).getValue() * finalDamage);

							List<LivingEntity> list = p.getWorld().getLivingEntities();
							Iterator<LivingEntity> iter = list.iterator();
							//only mobs colliding with this bounding box
							BoundingBox box = victim.getBoundingBox().expand(1, 0.25, 1);
							while(iter.hasNext()) {
								LivingEntity livingEntity = iter.next();
								net.minecraft.world.entity.LivingEntity nmsLivingEntity = ((CraftLivingEntity) livingEntity).getHandle();
								//disqualifying conditions
								if(livingEntity == p || livingEntity == victim)
									continue;

								if(livingEntity instanceof ArmorStand stand && stand.isMarker())
									continue;

								if(nmsPlayer.distanceToSqr(nmsLivingEntity) < 9 && livingEntity.getBoundingBox().overlaps(box)) {
									//does the damage/armor calculations and calls the EntityDamageEvent,
									// which will create another one of these and queue it
									nmsLivingEntity.hurt(DamageType.getSweeping(p).getDamageSource(), sweepingEdgeDmg);
									damageEvent.sweepEffect = true; // Only sweep attack effect if actually sweep attack'd
								}
							}
						}
					}
					else if(damageType.is(DamageType.SWEEP_ATTACK)) {
						//sweep attacks no do base kb
						// use de kb level based on attacker looking direction instead
						doBaseKB = false;
						knockbackLevels += 1;
					}
				}

				if(living instanceof Player p && damageType.isMelee()) {
					damageEvent.wasSprinting = p.isSprinting();
					if(damageEvent.wasSprinting)
						knockbackLevels += 1;
				}
				else if(living instanceof Tameable tameable && damageType.isMelee()) {
					UUID ownerUuid = tameable.getOwnerUniqueId();
					if(ownerUuid != null) {
						// Hopefully this method doesn't return OfflinePlayers
						realAttacker = Bukkit.getPlayer(ownerUuid);
					}
				}
			}
		}

		if(victim instanceof LivingEntity living) {
			knockbackResistance = 1d - living.getAttribute(Attribute.KNOCKBACK_RESISTANCE).getValue();
			if (!alreadyCalcedArmor) {
				finalDamage = DamageCalculator.calcArmorReducedDamage(damageType, rawDamage, living);
			}
		}

		if(damageType.isKnockback()) {
			damageEvent.knockback = calculateKnockback(victim, damageType, attacker, doBaseKB, knockbackLevels,
					knockbackResistance);
		}

		damageEvent.finalDamage = finalDamage;
		damageEvent.damageType = damageType;
		damageEvent.isCritical = critical;
		damageEvent.attacker = attacker;
		damageEvent.realAttacker = realAttacker;
		damageEvent.knockbackLevels = knockbackLevels;
		damageEvent.knockbackResistance = knockbackResistance;
		damageEvent.meleeWeapon = weapon;

		return damageEvent;

	}

	private static DamageEvent fromEDE(EntityDamageEvent event) {
		DamageType damageType = DamageType.getAttack(event);
		if(event instanceof EntityDamageByEntityEvent dEvent) {
			return newDamageEvent(dEvent.getEntity(), dEvent.getDamage(), damageType,
					dEvent.getDamager(), dEvent.isCritical());
		}

		return newDamageEvent(event.getEntity(), event.getDamage(), damageType, null, false);
	}

    /*public DamageEvent(Entity damagee, double rawDamage, double finalDamage, DamageType damageType, Entity damager,
                       Entity realDamager) {
        this.damagee = damagee;
        this.rawDamage = rawDamage;
        this.finalDamage = finalDamage;
        this.damageType = damageType;
        this.damager = damager;
        this.realDamager = realDamager;
    }*/

	public void executeAttack() {
		if(cancelled)
			return;

		//non-livingentitys dont have NDT or health, can't do much
		if(!(victim instanceof LivingEntity)) {
			//TODO: handle non-living entities
		}
		else
		{
			LivingEntity living = (LivingEntity) victim;

			net.minecraft.world.entity.LivingEntity nmsLiving = ((CraftLivingEntity) living).getHandle();
			nmsLiving.walkAnimation.setSpeed(1.5f);

			DamageTimes.TrackedDamageTypes trackedType = damageType.getTrackedType();
			DamageTimes.DamageTime dTimes = DamageTimes.getDamageTime(living, trackedType);

			int ndt;

			boolean doHurtEffect = true;
			final double originalDamage = this.finalDamage; // for NDT record-keeping
			if(!damageType.isIgnoreRate() && !ignoreInvulnerability) {
				ndt = TeamArena.getGameTick() - dTimes.getLastTimeDamaged();

				//they are still in no-damage-time
				// if they were hit with a stronger attack from a different entity, only apply
				// strength of new attack - strength of last attack, as if they were only hit
				// by the source of greater damage
				// also do not extra knockback, and don't play the hurt effect again
				// else if the new attack isn't stronger than the previous attack with this invulnerability period
				// then don't take damage
				// Does not apply to non ATTACK tracked types, they can only hit twice per second max
				if (ndt < living.getMaximumNoDamageTicks() / 2) {
					if (trackedType.considersDamage() &&
							getFinalAttacker() != dTimes.getGiver() && finalDamage > dTimes.getDamage()) {
						this.setNoKnockback();
						this.finalDamage = this.finalDamage - dTimes.getDamage();
						doHurtEffect = false;
					}
					else {
						return;
					}
				}
			}

			if(damageType.isInstantDeath()) {
				this.finalDamage = 9999999d;
			}

			//run modifications done by confirmed damage ability "Event Handlers"
			Main.getGame().onConfirmedDamage(this);

			if(cancelled)
				return;

			updateNDT(dTimes, originalDamage,
				doHurtEffect ? TeamArena.getGameTick() : dTimes.getLastTimeDamaged()); // don't update time if this is a "stronger hit in 1 ndt period" hit

			//knockback
			if(knockback != null && damageType.isKnockback()) {
				if (victim instanceof Player player) {
					//send knockback packet without modifying player's velocity
					Vec3 vec = CraftVector.toNMS(knockback);
					ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(player.getEntityId(), vec);
					PlayerUtils.sendPacket(player, new PacketContainer(PacketType.Play.Server.ENTITY_VELOCITY, packet));
				} else {
					victim.setVelocity(knockback);
				}
			}

			//this should be impossible normally but can happen in some circumstances
			if(finalDamage < 0) {
				StringBuilder error = new StringBuilder();
				error.append(getFinalAttacker().getName()).append(" is doing ").append(finalDamage)
						.append(" damage to ").append(victim.getName()).append(" DamageType: ")
						.append(damageType.toString()).append(" attacker: ")
						.append(attacker != null ? attacker.getName() : "null").append("\n");
				if(Main.getGame().getGameState() == LIVE) {
					if (getFinalAttacker() instanceof Player p && Main.getPlayerInfo(p).activeKit != null) {
						error.append("attacker kit: ").append(Main.getPlayerInfo(p).activeKit.getName()).append("\n");
					}
					if (victim instanceof Player p && Main.getPlayerInfo(p).activeKit != null) {
						error.append("victim kit: ").append(Main.getPlayerInfo(p).activeKit.getName()).append("\n");
					}
				}

				String errString = error.toString();
				Main.logger().warning(errString);

				finalDamage = 0;
			}

			//damage
			boolean isDeath = false;
			if(finalDamage > 0) {
				double absorp = living.getAbsorptionAmount();
				double newHealth = living.getHealth();
				//they still got absorption hearts
				if(absorp > 0) {
					if(finalDamage >= absorp) {
						living.setAbsorptionAmount(0);
						newHealth -= finalDamage - absorp;
					}
					else {
						living.setAbsorptionAmount(absorp - finalDamage);
					}
				}
				else {
					newHealth -= finalDamage;
				}

				if (newHealth <= 0) {
					//Bukkit.broadcast(Component.text(living.getName() + " has died"));
					newHealth = living.getAttribute(Attribute.MAX_HEALTH).getValue();
					isDeath = true;
				}

				living.setHealth(newHealth);
				living.setLastDamage(finalDamage);

				if(doHurtEffect) {
					this.playHurtAnimation(isDeath);
					// Additional custom death sound for players
					// This should be in TeamArena.
					if (isDeath && living instanceof Player) {
						living.getWorld().playSound(living.getLocation(), Sound.ENTITY_RABBIT_DEATH, 0.9f, 1f);
					}
				}

				if(isCritical)
					EntityUtils.playCritEffect(living);

				if(enchantDamage > 0d)
					EntityUtils.playMagicCritEffect(living);
			}

			if(isDeath)
				Main.getGame().handleDeath(this); // run this after to ensure the animations are seen by viewers
			else if(attacker instanceof AbstractArrow aa && aa.getPierceLevel() == 0 &&
					damageType.isProjectile() && ArrowImpaleStatus.isImpaling(aa)) {
				living.setArrowsInBody(living.getArrowsInBody() + 1);
			}
			else if (attacker instanceof Bee && damageType.isMelee()) {
				living.setBeeStingersInBody(living.getBeeStingersInBody() + 1);
			}
		}
		net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) victim).getHandle();
		nmsEntity.hasImpulse = true;

		//damager stuff
		if (this.attacker instanceof SpectralArrow sa && this.damageType.isProjectile()) {
			if (this.victim instanceof LivingEntity livingVictim) {
				livingVictim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, sa.getGlowingTicks(), 0));
			}
		}
		// If melee, always use the direct attacker, else use direct or real attacker
		LivingEntity livingDamager = null;
		if(damageType.isMelee() && this.attacker instanceof LivingEntity)
			livingDamager = (LivingEntity) attacker;
		else {
			Entity finalE = this.getFinalAttacker();
			if(finalE instanceof LivingEntity living)
				livingDamager = living;
		}

		if(livingDamager != null) {
			if(livingDamager.getEquipment() != null) {
				int fireTicks = 0;
				DamageType type = DamageType.FIRE_TICK;
				if(damageType.isMelee()) {
					ItemStack weapon = this.getMeleeWeapon();
					fireTicks = weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
					fireTicks *= 80; //80 ticks of fire per level
					type = DamageType.FIRE_ASPECT;
				}
				else if(damageType.isProjectile() && attacker instanceof Projectile proj) {
					if(proj.getFireTicks() > 0) {
						fireTicks = 5 * 20;
						type = DamageType.FIRE_BOW;
					}
					//Bukkit.broadcastMessage("isProjectile, fireTicks: " + fireTicks);
				}

				if(fireTicks > victim.getFireTicks()) {
					if (victim instanceof LivingEntity living) {
						DamageTimes.DamageTime time = DamageTimes.getDamageTime(living, DamageTimes.TrackedDamageTypes.FIRE);

						int timeGiven;
						if(victim.getFireTicks() <= 0)
							timeGiven = TeamArena.getGameTick();
						else
							timeGiven = time.getTimeGiven();

						//Just update:
						//- person who gave the fire
						//- if they weren't already on fire, the time that the fire started
						//- damageType of the fire
						time.update(getFinalAttacker(), timeGiven, time.getLastTimeDamaged(), time.getDamage(), type);
					}

					victim.setFireTicks(fireTicks);
				}
			}

			if (livingDamager instanceof Player p) {
				//melee attack sound
				if(damageType.is(DamageType.MELEE)) {
					net.minecraft.world.entity.player.Player nmsPlayer = ((CraftPlayer) p).getHandle();
					if (wasSprinting) {
						nmsPlayer.setDeltaMovement(nmsPlayer.getDeltaMovement().multiply(0.6, 1, 0.6));
						//3 for sprinting flag
						// dont use setSprinting as it sends an attribute packet that may stop the client from sprinting
						// server-client desync good? it's 1.8 behaviour anyway, may change later
						nmsPlayer.setSharedFlag(3, false);
					}

					if (isCritical) {
						p.getWorld().playSound(p, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1f);
					}
					else if (sweepEffect) {
						p.getWorld().playSound(p, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 1f);
						nmsPlayer.sweepAttack();
					}
					else if (wasSprinting) {
						p.getWorld().playSound(p, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1f, 1f);
					}
					else {
						Sound sound;
						if (p.getAttackCooldown() > 0.9f)
							sound = Sound.ENTITY_PLAYER_ATTACK_STRONG;
						else
							sound = Sound.ENTITY_PLAYER_ATTACK_WEAK;

						p.getWorld().playSound(p, sound, SoundCategory.PLAYERS, 1f, 1f);
					}

					//reset their attack cooldown
					p.resetCooldown();
				}
				else if(damageType.isProjectile()) {
					Sound sound = Main.getPlayerInfo(p).getPreference(Preferences.BOW_HIT_SOUND);
					p.playSound(p, sound, SoundCategory.PLAYERS, 2f, 1f);
				}
			}
		}
	}

	private void updateNDT(DamageTimes.DamageTime dTime, double damage, int hitTime) {
		dTime.update(getFinalAttacker(), dTime.getTimeGiven(), hitTime, damage, this.damageType);
	}

	/**
	 * Play entity hurt animation and sound.
	 */
	private void playHurtAnimation(final boolean isDeath) {
		if (!(this.victim instanceof LivingEntity livingVictim))
			return;

		net.minecraft.world.entity.LivingEntity nmsVictim = ((CraftLivingEntity) this.victim).getHandle();
		DamageSource source = this.damageType.getDamageSource();
		ClientboundDamageEventPacket damageEventPacket = new ClientboundDamageEventPacket(nmsVictim, source);
		PacketContainer pLibPacket = new PacketContainer(PacketType.Play.Server.DAMAGE_EVENT, damageEventPacket);

		Sound sound;
		if (!livingVictim.isSilent()) {
			if (isDeath)
				sound = livingVictim.getDeathSound();
			else {
				SoundEvent nmsSound = nmsVictim.getHurtSound0(source);
				sound = CraftSound.minecraftToBukkit(nmsSound);
			}
		}
		else {
			sound = null;
		}

		if (livingVictim instanceof Player playerVictim) { // send to self if player
			// Update the player's damage tilt yaw first, and then send damage event packet.
			DamageTiltType tiltType = Main.getPlayerInfo(playerVictim).getPreference(Preferences.DIRECTIONAL_DAMAGE_TILT);
			if (tiltType != DamageTiltType.NONE && this.hasKnockback()) {
				Vector kb = this.getKnockback().clone();
				kb.setY(0).normalize();

				if (Math.abs(kb.getX()) > Vector.getEpsilon() && Math.abs(kb.getZ()) > Vector.getEpsilon()) {
					((CraftPlayer) playerVictim).getHandle().indicateDamage(-kb.getX(), -kb.getZ());
				}
			}
			else if (tiltType == DamageTiltType.ALL) {
				PlayerUtils.syncHurtDirection(playerVictim);
			}
			// Need to send hurt direction to client else it will play with the same direction as last damage event
			// Also covers preference changes
			else if (playerVictim.getHurtDirection() != 0f) {
				PlayerUtils.setAndSyncHurtDirection(playerVictim, 0f);
			}

			PlayerUtils.sendPacket(playerVictim, pLibPacket);
		}

		for (ServerPlayerConnection connection : EntityUtils.getTrackedPlayers0(livingVictim)) {
			Player player = connection.getPlayer().getBukkitEntity();
			PlayerUtils.sendPacket(player, pLibPacket);
			//connection.send(damageEventPacket);
			// Other players also need to have the sound played to them
			if (sound != null)
				player.playSound(victim, sound, nmsVictim.getSoundVolume(),
					nmsVictim.getVoicePitch());
		}
	}

	/** Whether a player receives the old or new kind of damage bob when taking damage */
	public enum DamageTiltType {
		ALL, // Receive new damage bob for all damage
		DIRECTED, // new damage bob only for damage with direction i.e melee attacks
		NONE // No new damage bob
	}

	public static Vector calculateKnockback(Entity victim, DamageType damageType, Entity attacker, boolean baseKnockback,
											double knockbackLevels, double knockbackResistance) {
		Vector knockback = new Vector();
		if (attacker != null)
		{
			final Location attackerLoc = attacker.getLocation();
			final boolean isProjectile = attacker instanceof Projectile && damageType.isProjectile();
			if(baseKnockback) {
				Vector offset;
				if (isProjectile) {
					offset = attackerLoc.getDirection();
					offset.setZ(-offset.getZ());
				}
				else {
					offset = victim.getLocation().subtract(attackerLoc).toVector().multiply(-1d);
				}

				double xDist = offset.getX();
				double zDist = offset.getZ();

				while (!Double.isFinite(xDist * xDist + zDist * zDist) || xDist * xDist + zDist * zDist < 0.0001) {
					xDist = MathUtils.randomRange(-0.01, -0.01);
					zDist = MathUtils.randomRange(-0.01, -0.01);
				}

				double dist = Math.sqrt(xDist * xDist + zDist * zDist);

				Vector vec = victim.getVelocity();

				vec.setX(vec.getX() / 2);
				vec.setY(vec.getY() / 2);
				vec.setZ(vec.getZ() / 2);

				vec.add(new Vector(-(xDist / dist * xzVal * knockbackResistance), yVal, -(zDist / dist * xzVal * knockbackResistance)));

				if (vec.getY() > yVal)
					vec.setY(yVal);

				knockback.add(vec);
			}

			if (knockbackLevels != 0)
			{
				knockbackLevels *= xzMult;
				knockbackLevels /= 2;

				if (isProjectile) {
					Vector dir = attackerLoc.getDirection();
					dir.setX(-dir.getX());
					attackerLoc.setDirection(dir); // Misuse for getYaw()
				}
				double xKb = -Math.sin(attackerLoc.getYaw() * Math.PI / 180.0d) * knockbackLevels;
				double zKb = Math.cos(attackerLoc.getYaw() * Math.PI / 180.0d) * knockbackLevels;

				Vector kbEnch = new Vector(xKb * knockbackResistance, yMult, zKb * knockbackResistance);
				knockback.add(kbEnch);
			}
		}

		return knockback;
	}

	public void setRealAttacker(Entity attacker) {
		if(damageType.isProjectile() && attacker instanceof Projectile proj) {
			this.attacker = proj;
			if(proj.getShooter() instanceof Entity e) {
				this.realAttacker = e;
			}
		}
		else {
			this.attacker = attacker;
		}
	}

	public void setFinalAttacker(Entity attacker) {
		this.realAttacker = attacker;
	}

	public boolean hasKnockback() {
		return knockback != null;
	}

	public Vector getKnockback() {
		return knockback;
	}

	public void setKnockback(Vector knockback) {
		this.knockback = knockback;
	}

	public DamageType getDamageType() {
		return damageType;
	}

	public Entity getVictim() {
		return victim;
	}

	public Player getPlayerVictim() {
		return (Player) victim;
	}

	public void setNoKnockback() {
		this.knockback = null;
	}

	public Entity getAttacker() {
		return attacker;
	}

	public Entity getFinalAttacker() {
		return realAttacker != null ? realAttacker : attacker;
	}

	public double getFinalDamage() {
		return finalDamage;
	}

	public ItemStack getMeleeWeapon() {
		if (!CompileAsserts.OMIT && !(this.getFinalAttacker() instanceof LivingEntity) || !this.damageType.isMelee()) {
			Main.logger().warning("DamageEvent.getMeleeWeapon called on non-living-attacker or non-melee DamageEvent");
			Thread.dumpStack();
		}
		return this.meleeWeapon != null ? this.meleeWeapon : new ItemStack(Material.AIR);
	}

	public void setFinalDamage(double damage) {
		this.finalDamage = damage;
	}

	/** Get 'raw' damage done, meaning damage before armour calculations on the victim */
	public double getRawDamage() {
		return rawDamage;
	}

	/** Set the 'raw' damage done, meaning damage before victim's armour calculations. This will cause the calculations
	 * to be re-done and the finalDamage to be re-assigned.
	 * The DamageEvent's current DamageType is important! It will affect the outcome. */
	public void setRawDamage(double rawDamage) {
		this.rawDamage = rawDamage;
		if (this.victim instanceof LivingEntity) {
			this.recalculateFinalDamage();
		}
		else {
			this.finalDamage = rawDamage;
		}
	}

	public void recalculateFinalDamage() {
		if (this.victim instanceof LivingEntity living) {
			this.finalDamage = DamageCalculator.calcArmorReducedDamage(this.damageType, this.rawDamage, living);
		}
	}

	public void setDamageType(DamageType damageType) {
		this.damageType = damageType;
	}

	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

	public boolean isIgnoreInvulnerability() {
		return ignoreInvulnerability;
	}

	public void setIgnoreInvulnerability(boolean ignore) {
		this.ignoreInvulnerability = ignore;
	}

	public void setDamageTypeCause(Entity cause) {
		this.damageTypeCause = cause;
	}

	public Entity getDamageTypeCause() {
		return this.damageTypeCause;
	}

	public void setDeathMessageOverride(@Nullable DamageType.DeathMessage deathMessageOverride) {
		this.deathMessageOverride = deathMessageOverride;
	}

	@Nullable
	public DamageType.DeathMessage getDeathMessageOverride() {
		return deathMessageOverride;
	}

	public boolean isBroadcastsDeathMessage() {
		return this.broadcastsDeathMessage;
	}

	public void setBroadcastsDeathMessage(boolean broadcastsDeathMessage) {
		this.broadcastsDeathMessage = broadcastsDeathMessage;
	}

	public enum DeathMessageGrayOption {
		ALL,
		ENEMIES_ONLY,
		NONE
	}
	public void broadcastDeathMessage() {
		if (this.isBroadcastsDeathMessage()) {
			DamageType.DeathMessage deathMessage = deathMessageOverride != null ? deathMessageOverride : damageType.getDeathMessage();
			if (deathMessage != null) {
				Component message = deathMessage.render(this.getVictim(), this.getFinalAttacker(), this.getDamageTypeCause());
				Component darkened = deathMessage.renderDarkened(this.getVictim(), this.getFinalAttacker(), this.getDamageTypeCause());
				Main.componentLogger().info(message);
				var iter = Main.getPlayersIter();
				while (iter.hasNext()) {
					var entry = iter.next();
					Player viewer = entry.getKey();
					PlayerInfo pinfo = entry.getValue();
					DeathMessageGrayOption option = pinfo.getPreference(Preferences.DARKEN_DEATH_MESSAGES);
					if (option == DeathMessageGrayOption.ALL ||
						(option == DeathMessageGrayOption.ENEMIES_ONLY && !pinfo.team.hasMember(this.getVictim()))) {
						viewer.sendMessage(darkened);
					}
					else {
						viewer.sendMessage(message);
					}
				}
			}
		}
	}

	public boolean isCancelled() {
		return cancelled;
	}

	/** For debug only */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(2048);
		for (Field f : this.getClass().getDeclaredFields()) {
			try {
				if (f.canAccess(this)) {
					s.append(f.getName()).append("=").append(f.get(this));
					s.append("\n");
				}
			}
			catch (IllegalArgumentException | IllegalAccessException ignored) {}
		}

		return s.toString();
	}
}

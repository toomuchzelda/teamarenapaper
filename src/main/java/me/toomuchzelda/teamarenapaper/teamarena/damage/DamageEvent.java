package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftVector;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static me.toomuchzelda.teamarenapaper.teamarena.GameState.LIVE;

//a custom damage event for custom knockback and other customisability bukkit/spigot/paper can't provide
public class DamageEvent {

	private Entity victim;
	//damage before damage-reduction calculations
	private double rawDamage; // full raw damage
	private double baseDamage; //base damage if any
	private double enchantDamage; //enchantment damage from the item if any

	//damage after damage-reduction calculations
	private double finalDamage;
	private DamageType damageType;
	private Entity damageTypeCause; // for %Cause% in DamageType deathmessages

	//null implies no knockback
	// dont use 0,0,0 vector as that'll stop the player moving for a split moment
	private Vector knockback;
	private double knockbackLevels;
	//from 0 to 1, 0 = no knockback received, 1 = all knockback received
	private double knockbackResistance;

	private Entity attacker;
	//shooter of arrow, snowball etc where attacker would be the projectile
	private Entity realAttacker;
	private boolean isCritical;
	//if the attacker was sprinting, only applicable if the attacker is a Player
	private boolean wasSprinting;
	private boolean isSweep = false;
	private boolean ignoreInvulnerability = false;

	private boolean cancelled;

	//for knockback
	public static final double yVal = 0.4;
	public static final double xzVal = 0.4;
	public static final double yMult = 0.1;
	public static final double xzMult = 1;

	public static @Nullable DamageEvent handleBukkitEvent(EntityDamageEvent event) {

		event.setCancelled(true);
		if(Main.getGame().getGameState() != LIVE)
			return null;

		//marker armorstands must never be damaged/killed
		if(event.getEntity() instanceof ArmorStand stand && stand.isMarker())
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
			else if (dEvent.getCause() == EntityDamageEvent.DamageCause.PROJECTILE && dEvent.getDamager() instanceof AbstractArrow aa) {
				//fix arrow damage - no random crits
				//  arrow damage is the vanilla formula without the random crit part
				double damage = DamageNumbers.calcArrowDamage(aa.getDamage(), aa.getVelocity().length());

				dEvent.setDamage(damage);

				//stop arrows from bouncing off after this event is run
				//store info about how it's moving now, before the EntityDamageEvent ends and the cancellation
				// makes the arrow bounce off the damagee, so we can re-set the movement later
				ArrowPierceManager.addOrUpdateInfo(aa);

				//fix the movement after event is run
				Bukkit.getScheduler().runTaskLater(Main.getPlugin(), bukkitTask -> {
					if(aa.isValid())
						ArrowPierceManager.fixArrowMovement(aa);
				}, 0L);
			}
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
		cancelled = false;
	}

	public static DamageEvent newDamageEvent(@NotNull Entity victim, double rawDamage, @NotNull DamageType damageType,
											 @Nullable Entity attacker, boolean critical) {

		DamageEvent damageEvent = new DamageEvent(victim, rawDamage);

		double finalDamage = rawDamage;

		//if it's fire caused by a non-specified attacker, set the damager from the cached DamageTimes
		// sort of re-construct this DamageEvent so it's accurate
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

			if(times.getGiver() != null && TeamArena.getGameTick() - times.getTimeGiven() < 10 * 20) { // 10 seconds since last attacked
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

        /*Bukkit.broadcast(Component.text("DamageCause: " + event.getCause()));
        Bukkit.broadcast(Component.text("DamageType: " + damageType.toString()));*/

		//if(damageType.isKnockback())
		//   knockbackMults = new LinkedList<>();

		boolean doBaseKB = true;
		double knockbackLevels = 0;
		double knockbackResistance = 1;

		boolean alreadyCalcedArmor = false;
		Entity realAttacker = null;

		if(attacker != null) {
			//damageEvent.isCritical = dEvent.isCritical();
			if(attacker instanceof Projectile projectile) {
				if(attacker instanceof AbstractArrow aa) {
					knockbackLevels += aa.getKnockbackStrength();
				}

				if (projectile.getShooter() instanceof LivingEntity living) {
					realAttacker = living;
				}
			}
			else {
				if(attacker instanceof TNTPrimed tnt) {
					Entity tntSource = tnt.getSource();
					if(tntSource != null) {
						realAttacker = tntSource;
					}
				}

				if(attacker instanceof LivingEntity living) {
					if(damageType.isMelee() && living.getEquipment() != null) {
						//item used during the attack, if applicable
						ItemStack item = living.getEquipment().getItemInMainHand();

						//recalculate the damage done by the item
						double itemDamage = DamageNumbers.getMaterialBaseDamage(item.getType());
						//add damage from potion effects (strength and weakness)
						for(PotionEffect potEffect : living.getActivePotionEffects()) {
							itemDamage += DamageNumbers.getPotionEffectDamage(potEffect);
							//Bukkit.broadcastMessage("added " + potEffect.getType().getName() + ", new total: " + itemDamage);
						}
						//crit
						if(critical) {
							itemDamage = DamageNumbers.getCritDamage(itemDamage);
						}

						damageEvent.baseDamage = itemDamage;

						//Bukkit.broadcastMessage("Custom base damage: " + itemDamage);

						//add enchantments
						if(victim instanceof LivingEntity livingVictim) {
							double enchDamage = DamageCalculator.calcItemEnchantDamage(item, livingVictim);
							itemDamage += enchDamage;
							//Bukkit.broadcastMessage("Custom ench damage: " + enchDamage);
							//do armor calc on victim
							finalDamage = DamageCalculator.calcArmorReducedDamage(damageType, itemDamage, livingVictim);
							alreadyCalcedArmor = true;

							damageEvent.enchantDamage = enchDamage;
						}

						rawDamage = itemDamage;

						//halve the strength of knockback enchantments
						knockbackLevels += ((float) item.getEnchantmentLevel(Enchantment.KNOCKBACK)) / 2;
						//knockbackMults.add(level);

						//cancelled bukkit event doesn't do sweeping attacks, re-do them here
						if(living instanceof Player p && damageType.is(DamageType.MELEE)) {
							//Bukkit.broadcastMessage("DamageType is melee: line 99");
							//same as nmsP.getAttackStrengthCooldown(0.5f);
							boolean isChargedWeapon = p.getAttackCooldown() > 0.9f;

							boolean isChargedSprintAttack = isChargedWeapon && p.isSprinting();

							net.minecraft.world.entity.player.Player nmsPlayer = ((CraftPlayer) p).getHandle();
							double walkedDist = nmsPlayer.walkDist - nmsPlayer.walkDistO;
							boolean notExceedingWalkSpeed = walkedDist < p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
							boolean sweep = false;

							if(isChargedWeapon && !isChargedSprintAttack && critical &&
									notExceedingWalkSpeed) {
								if(ItemUtils.isSword(item))
									sweep = true;
							}

							if(sweep) {
								float sweepingEdgeDmg = (float) (1f + EnchantmentHelper.getSweepingDamageRatio(nmsPlayer)
										* finalDamage);

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
										// somewhat inefficient, but i don't wanna do the damage numbers myself
										nmsLivingEntity.hurt(DamageType.getSweeping(p).getDamageSource(), sweepingEdgeDmg);
									}
								}
							}

							damageEvent.isSweep = sweep;
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
				}
			}
		}

		if(victim instanceof LivingEntity living) {
			knockbackResistance = 1d - living.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();
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

		//if its an arrow check if it can hit this particular damagee
		// also fix it's movement
		if(attacker instanceof AbstractArrow aa && aa.isValid() && damageType.isProjectile()) {
			//Bukkit.broadcastMessage("arrow pirece levels: " + aa.getPierceLevel());
			//if(aa.getPierceLevel() > 0) {
			ArrowPierceManager.PierceType type = ArrowPierceManager.canPierce(aa, victim);
			//Bukkit.broadcastMessage(type.toString());
			if (type == ArrowPierceManager.PierceType.REMOVE_ARROW) {
				aa.remove();
			} else {
				//don't do damage to the same entity more than once for piercing enchanted arrows
				if (type == ArrowPierceManager.PierceType.ALREADY_HIT) {
					return;
				}
			}
            /*}
            else
                aa.remove();*/
		}

		//non-livingentitys dont have NDT or health, can't do much
		if(!(victim instanceof LivingEntity)) {
			//projectiles shouldn't be killable
			//if(!(damagee instanceof Projectile))
			victim.remove();
		}
		else
		{
			LivingEntity living = (LivingEntity) victim;

			net.minecraft.world.entity.LivingEntity nmsLiving = ((CraftLivingEntity) living).getHandle();
			nmsLiving.animationSpeed = 1.5f;

			DamageTimes.TrackedDamageTypes trackedType = damageType.getTrackedType();
			DamageTimes.DamageTime dTimes = DamageTimes.getDamageTime(living, trackedType);
			Bukkit.broadcastMessage(trackedType.toString());

			int ndt;

			boolean doHurtEffect = true;
			if(!damageType.isIgnoreRate() && !ignoreInvulnerability) {
				ndt = TeamArena.getGameTick() - dTimes.getTimeGiven();

				//they are still in no-damage-time
				// if they were hit with a stronger attack from a different entity, only apply
				// strength of new attack - strength of last attack, as if they were only hit
				// by the source of greater damage
				// also do not extra knockback, and don't play the hurt effect again
				// else if the new attack isn't stronger than the previous attack with this invulnerability period
				// then don't take damage
				// Does not apply to non ATTACK tracked types, they can only hit twice per second max
				if (ndt < living.getMaximumNoDamageTicks() / 2) {
					if (trackedType == DamageTimes.TrackedDamageTypes.ATTACK) {
						if (getFinalAttacker() != dTimes.getGiver() && finalDamage > dTimes.getDamage()) {
							this.setNoKnockback();
							this.finalDamage = this.finalDamage - dTimes.getDamage();
							doHurtEffect = false;
						}
						else {
							return;
						}
					}
					else {
						return;
					}
				}
			}


			//run modifications done by confirmed damage ability "Event Handlers"
			Main.getGame().onConfirmedDamage(this);

			if(cancelled)
				return;

			updateNDT(dTimes);

			//knockback
			if(knockback != null) {
				if (victim instanceof Player player) {
					//send knockback packet without modifying player's velocity
					Vec3 vec = CraftVector.toNMS(knockback);
					ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(player.getEntityId(), vec);
					PlayerUtils.sendPacket(player, packet);
				} else {
					victim.setVelocity(knockback);
				}
			}

			//damage
			boolean isDeath = false;

			//this should be impossible normally but can happen in some circumstances
			if(finalDamage < 0) {
				Main.logger().warning(getFinalAttacker().getName() + " is doing " + finalDamage + " damage to " + victim.getName() +
						" DamageType: " + damageType.toString() + " attacker: " + (attacker != null ? attacker.getName() : "null"));
				if(getFinalAttacker() instanceof Player p) {
					Main.logger().warning("attacker kit: " + Main.getPlayerInfo(p).activeKit.getName());
				}
				if(victim instanceof Player p) {
					Main.logger().warning("victim kit: " + Main.getPlayerInfo(p).activeKit.getName());
				}

				finalDamage = 0;
			}

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
				newHealth = living.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				isDeath = true;
			}

			living.setHealth(newHealth);
			living.setLastDamage(finalDamage);

			if(doHurtEffect)
				EntityUtils.playHurtAnimation(living, damageType, isDeath);

			if(isCritical)
				EntityUtils.playCritEffect(living);

			if(enchantDamage > 0d)
				EntityUtils.playMagicCritEffect(living);

			if(isDeath)
				Main.getGame().handleDeath(this); // run this after to ensure the animations are seen by viewers
			else if(attacker instanceof AbstractArrow aa && aa.getPierceLevel() == 0 &&
					damageType.is(DamageType.PROJECTILE)) {
				living.setArrowsInBody(living.getArrowsInBody() + 1);
			}

			//need to send this packet for the hearts to flash white when lost, otherwise they just decrease with no
			// effect
			if (victim instanceof Player player && Main.getPlayerInfo(player).getPreference(Preferences.HEARTS_FLASH_DAMAGE)) {
				PlayerUtils.sendHealth(player);
			}
		}
		net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) victim).getHandle();
		nmsEntity.hasImpulse = true;

		//damager stuff
		if(getFinalAttacker() instanceof LivingEntity livingDamager) {

			if(livingDamager.getEquipment() != null) {

				int fireTicks = 0;
				DamageType type = DamageType.FIRE_TICK;
				if(damageType.isMelee()) {
					ItemStack weapon = livingDamager.getEquipment().getItemInMainHand();
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
						time.update(getFinalAttacker(), time.getTimeGiven(), time.getDamage(), type);
					}

					//todo: custom fireticker thing
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
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1f);
					}
					else if (isSweep) {
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 1f);
						nmsPlayer.sweepAttack();
					}
					else if (wasSprinting) {
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1f, 1f);
					}
					else {
						Sound sound;
						if (p.getAttackCooldown() > 0.9f)
							sound = Sound.ENTITY_PLAYER_ATTACK_STRONG;
						else
							sound = Sound.ENTITY_PLAYER_ATTACK_WEAK;

						p.getWorld().playSound(p.getLocation(), sound, SoundCategory.PLAYERS, 1f, 1f);
					}

					//reset their attack cooldown
					p.resetCooldown();
				}
				else if(damageType.is(DamageType.PROJECTILE)) {
					Sound sound = Main.getPlayerInfo(p).getPreference(Preferences.BOW_HIT_SOUND);
					p.playSound(p.getLocation(), sound, SoundCategory.PLAYERS, 2f, 1f);
				}
			}
		}
	}

	// mfw java no primitive pointers
	private void updateNDT(DamageTimes.DamageTime dTime) {
		dTime.update(getFinalAttacker(), TeamArena.getGameTick(), this.finalDamage, this.damageType);
	}

	public static Vector calculateKnockback(Entity victim, DamageType damageType, Entity attacker, boolean baseKnockback,
											double knockbackLevels, double knockbackResistance) {
		Vector knockback = new Vector();
		if (attacker != null)
		{
			if(baseKnockback) {
				Vector offset;
				if (attacker instanceof Projectile && damageType.isProjectile()) {
					offset = attacker.getLocation().getDirection();
					offset.setZ(-offset.getZ());
				}
				else {
					offset = attacker.getLocation().toVector().subtract(victim.getLocation().toVector());
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

				Vector kbEnch;

				double xKb = -Math.sin(attacker.getLocation().getYaw() * 3.1415927F / 180.0f) * knockbackLevels;
				double zKb = Math.cos(attacker.getLocation().getYaw() * 3.1415927F / 180.0f) * knockbackLevels;

				kbEnch = new Vector(xKb * knockbackResistance, yMult, zKb * knockbackResistance);
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

	public boolean hasKnockback() {
		return knockback != null;
	}

	@Nullable
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

	public void setFinalDamage(double damage) {
		this.finalDamage = damage;
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

	public boolean isCancelled() {
		return cancelled;
	}
}

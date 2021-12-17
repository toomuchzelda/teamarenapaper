package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.EntityUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.core.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_17_R1.CraftSoundGroup;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftDamageSource;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftVector;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

//a custom damage event for custom knockback and other customisability bukkit/spigot/paper can't provide
public class DamageEvent {

    private Entity damagee;
    //damage before damage-reduction calculations
    private double rawDamage;
    //damage after damage-reduction calculations
    private double finalDamage;
    private DamageType damageType;

    //null implies no knockback
    // dont use 0,0,0 vector as that'll stop the player moving for a split moment
    private Vector knockback;
    private LinkedList<Double> knockbackMults;
    //from 0 to 1, 0 = no knockback received, 1 = all knockback received
    private double knockbackResistance;

    private Entity damager;
    //shooter of arrow, snowball etc where attacker would be the projectile
    private Entity realDamager;
    private boolean isCritical;
    //if the attacker was sprinting, only applicable if the attacker is a Player
    private boolean wasSprinting;
    private boolean isSweep = false;
    
    public static final double yVal = 0.4;
    public static final double xzVal = 0.4;
    public static final double yMult = 0.1;
    public static final double xzMult = 1;

    public DamageEvent(EntityDamageEvent event) {
        damagee = event.getEntity();
        rawDamage = event.getDamage();
        finalDamage = event.getFinalDamage();
        damageType = DamageType.getAttack(event);
        
        /*Bukkit.broadcast(Component.text("DamageCause: " + event.getCause()));
        Bukkit.broadcast(Component.text("DamageType: " + damageType.toString()));*/

        if(damageType.isKnockback())
            knockbackMults = new LinkedList<>();
    
        knockbackResistance = 1;
        
        if(damagee instanceof LivingEntity living) {
            knockbackResistance = 1 - living.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();
        }

        boolean doBaseKB = true;
        float kbLevels = 0;
        if(event instanceof EntityDamageByEntityEvent dEvent) {
            if(dEvent.getDamager() instanceof Projectile projectile) {

                if(dEvent.getDamager() instanceof AbstractArrow aa) {
                    //knockbackMults.add((double) aa.getKnockbackStrength());
                    kbLevels += aa.getKnockbackStrength();
                    if(aa.getPierceLevel() > 0) {
                        //store info about how it's moving now, before the EntityDamageEvent ends and the cancellation
                        // makes the arrow bounce off the damagee, so we can re-set the movement later
                        ArrowPierceManager.addInfo(aa);
                    }
                }

                if (projectile.getShooter() instanceof LivingEntity living) {
                    realDamager = living;
                    damager = projectile;
                }
            }
            else {
                damager = dEvent.getDamager();
                realDamager = null;

                if(dEvent.getDamager() instanceof LivingEntity living) {
                    if(living.getEquipment() != null) {
                        //item used during the attack, if applicable
                        ItemStack item = living.getEquipment().getItemInMainHand();
                        //halve the strength of knockback enchantments
                        kbLevels += ((float) item.getEnchantmentLevel(Enchantment.KNOCKBACK)) / 2;
                        //knockbackMults.add(level);

                        //cancelled event doesn't do sweeping attacks, re-do them here
                        if(living instanceof Player p && damageType.is(DamageType.MELEE)) {
                            //Bukkit.broadcastMessage("DamageType is melee: line 99");
                            //same as nmsP.getAttackStrengthCooldown(0.5f);
                            boolean isChargedWeapon = p.getAttackCooldown() > 0.9f;
                            
                            boolean isChargedSprintAttack = isChargedWeapon && p.isSprinting();
    
                            net.minecraft.world.entity.player.Player nmsPlayer = ((CraftPlayer) p).getHandle();
                            double walkedDist = nmsPlayer.walkDist - nmsPlayer.walkDistO;
                            boolean notExceedingWalkSpeed = walkedDist < p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
                            
                            if(isChargedWeapon && !isChargedSprintAttack && !isCritical &&
                                    notExceedingWalkSpeed) {
                                net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
                                if(nmsItem.getItem() instanceof SwordItem)
                                    isSweep = true;
                            }
                            
                            if(isSweep) {
                                float sweepingEdgeDmg = (float) (1f + EnchantmentHelper.getSweepingDamageRatio(nmsPlayer)
                                        * finalDamage);
                                
                                List<LivingEntity> list = p.getWorld().getLivingEntities();
                                Iterator<LivingEntity> iter = list.iterator();
                                //only mobs colliding with this bounding box
                                BoundingBox box = damagee.getBoundingBox().expand(1, 0.25, 1);
                                while(iter.hasNext()) {
                                    LivingEntity livingEntity = iter.next();
                                    net.minecraft.world.entity.LivingEntity nmsLivingEntity = ((CraftLivingEntity) livingEntity).getHandle();
                                    //disqualifying conditions
                                    if(livingEntity == p || livingEntity == damagee)
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
                        }
                        else if(damageType.is(DamageType.SWEEP_ATTACK)) {
                            //sweep attacks no do base kb
                            // use de kb level based on attacker looking direction instead
                            doBaseKB = false;
                            kbLevels += 1;
                        }
                    }

                    if(living instanceof Player p && damageType.isMelee()) {
                        wasSprinting = p.isSprinting();
                        if(wasSprinting)
                            kbLevels += 1;
                    }
                }
            }
            isCritical = dEvent.isCritical();
        }

        if(damageType.isKnockback()) {
            knockback = calculateKnockback(doBaseKB, kbLevels);
        }

        //Bukkit.broadcastMessage("kbresist: " + knockbackResistance);
        
        
        //queue this damage
        Main.getGame().queueDamage(this);
    }
    
    public DamageEvent(Entity damagee, double rawDamage, double finalDamage, DamageType damageType, Entity damager,
                       Entity realDamager) {
        this.damagee = damagee;
        this.rawDamage = rawDamage;
        this.finalDamage = finalDamage;
        this.damageType = damageType;
        this.damager = damager;
        this.realDamager = realDamager;
    }
    
    public void executeAttack() {

        //if its an arrow check if it can hit this particular damagee
        // also fix it's movement
        if(damager instanceof AbstractArrow aa && aa.isValid()) {
            //Bukkit.broadcastMessage("arrow pirece levels: " + aa.getPierceLevel());
            if(aa.getPierceLevel() > 0) {
                ArrowPierceManager.PierceType type = ArrowPierceManager.canPierce(aa, damagee);
                //Bukkit.broadcastMessage(type.toString());
                if (type == ArrowPierceManager.PierceType.REMOVE_ARROW) {
                    aa.remove();
                } else {
                    //cancelled EntityDamageEvent makes arrows bounce off hit entities.
                    // reset the arrow's direction and velocity at the end of the tick to counter this


                    ArrowPierceManager.fixArrowMovement(aa);


                    //don't do damage to the same entity more than once for piercing enchanted arrows
                    if (type == ArrowPierceManager.PierceType.ALREADY_HIT) {
                        return;
                    }
                }
            }
            else
                aa.remove();
        }

        //non-livingentitys dont have NDT or health, can't do much
        if(!(damagee instanceof LivingEntity)) {
            //projectiles shouldn't be killable
            //if(!(damagee instanceof Projectile))
                damagee.remove();
        }
        else
        {
            LivingEntity living = (LivingEntity) damagee;
            
            net.minecraft.world.entity.LivingEntity nmsLiving = ((CraftLivingEntity) living).getHandle();
            nmsLiving.animationSpeed = 1.5f;
            
            DamageTimes dTimes = DamageTimes.getDamageTimes(living);
            int ndt;
            
            boolean doHurtEffect = true;
    
            if(damageType.isMelee() || damageType.isProjectile())
            {
                ndt = TeamArena.getGameTick() - dTimes.lastAttackTime;
        
                //they are still in no-damage-time
                // if they were hit with a stronger attack, only apply
                // strength of new attack - strength of last attack, as if they were only hit
                // by the person with the stronger weapon
                // also do not extra knockback, and don't play the hurt effect again
                // else if the new attack isn't stronger than the previous attack with this invulnerability period
                // then don't take damage
                if(ndt < living.getMaximumNoDamageTicks() / 2) {
                    if(finalDamage > living.getLastDamage() && dTimes.lastDamager != damager) {
                        this.setNoKnockback();
                        this.finalDamage = this.finalDamage - living.getLastDamage();
                        doHurtEffect = false;
                    }
                    else {
                        return;
                    }
                }
            }
            else {
                if (damageType.isFire())
                    ndt = TeamArena.getGameTick() - dTimes.lastFireTime;
                else
                    ndt = TeamArena.getGameTick() - dTimes.lastMiscDamageTime;
        
                //not do damage if not enough invuln ticks elapsed
                if (ndt < living.getMaximumNoDamageTicks() / 2) {
                    return;
                }
            }
            
            updateNDT(dTimes, damageType, this.getFinalDamager());
            
            //knockback
            if(knockback != null) {
                if (damagee instanceof Player player) {
                    //send knockback packet
                    Vec3 vec = CraftVector.toNMS(knockback);
                    ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(player.getEntityId(), vec);
                    PlayerUtils.sendPacket(player, packet);
                } else {
                    damagee.setVelocity(knockback);
                }
            }
    
            //damage
            double newHealth = living.getHealth() - finalDamage;
            if (newHealth <= 0) {
                //todo: handle death here
                //Bukkit.broadcast(Component.text(living.getName() + " has died"));
                newHealth = living.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                Main.getGame().handleDeath(this);
            }
            living.setHealth(newHealth);
            living.setLastDamage(finalDamage);
            if(doHurtEffect)
                EntityUtils.playHurtAnimation(living, damageType);
            if(isCritical) {
                EntityUtils.playCritEffect(living);
            }
            //need to send this packet for the hearts to flash white when lost, otherwise they just decrease with no
            // effect
            if (damagee instanceof Player player) {
                PlayerUtils.sendHealth(player, player.getHealth());
            }
        }
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) damagee).getHandle();
        nmsEntity.hasImpulse = true;
    
        //damager stuff
        if(damager instanceof LivingEntity livingDamager) {

            if(livingDamager.getEquipment() != null) {
                ItemStack weapon = livingDamager.getEquipment().getItemInMainHand();
                int fireTime = weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
                fireTime *= 80; //80 ticks of fire per level

                damager.setFireTicks(fireTime);
            }

            if (damager instanceof Player p) {
                net.minecraft.world.entity.player.Player nmsPlayer = ((CraftPlayer) p).getHandle();
                if (wasSprinting) {
                    nmsPlayer.setDeltaMovement(nmsPlayer.getDeltaMovement().multiply(0.6, 1, 0.6));
                    //3 for sprinting flag
                    // dont use setSprinting as it sends an attribute packet that may stop the client from sprinting
                    // server-client desync good? it's 1.8 behaviour anyway, may change later
                    nmsPlayer.setSharedFlag(3, false);
                }

                if(isCritical) {
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1f);
                }

                if(isSweep) {
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 1f);
                    nmsPlayer.sweepAttack();
                }
                else if(wasSprinting) {
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1f, 1f);
                }
                else {
                    Sound sound;
                    if(p.getAttackCooldown() > 0.9f)
                        sound = Sound.ENTITY_PLAYER_ATTACK_STRONG;
                    else
                        sound = Sound.ENTITY_PLAYER_ATTACK_WEAK;
                    
                    p.getWorld().playSound(p.getLocation(), sound, SoundCategory.PLAYERS, 1f, 1f);
                }

                //reset their attack cooldown
                p.resetCooldown();
            }
        }
    }

    //if damagee is a player pinfo must not be null
    // mfw java no primitive pointers
    private static void updateNDT(DamageTimes dTimes, DamageType damageType, Entity lastDamager) {
        if(damageType.isMelee() || damageType.isProjectile())
        {
            dTimes.lastAttackTime = TeamArena.getGameTick();
            //Bukkit.broadcast(Component.text("Point 5"));
        }
        else if(damageType.isFire()) {
            dTimes.lastFireTime = TeamArena.getGameTick();
        }
        else {
            dTimes.lastMiscDamageTime = TeamArena.getGameTick();
            //Bukkit.broadcast(Component.text("Point 7"));
        }

        dTimes.lastDamager = lastDamager;
    }

    public Vector calculateKnockback(boolean baseKnockback, float knockbackLevels) {
        Vector knockback = new Vector();
        if (damager != null)
        {
            if(baseKnockback) {
                Vector offset;
                if (damager instanceof Projectile && damageType.isProjectile()) {
                    offset = damager.getLocation().getDirection();
                    offset.setZ(-offset.getZ());
                }
                else {
                    offset = damager.getLocation().toVector().subtract(damagee.getLocation().toVector());
                }
    
                double xDist = offset.getX();
                double zDist = offset.getZ();
    
                while (!Double.isFinite(xDist * xDist + zDist * zDist) || xDist * xDist + zDist * zDist < 0.0001) {
                    xDist = MathUtils.randomRange(-0.01, -0.01);
                    zDist = MathUtils.randomRange(-0.01, -0.01);
                }
    
                double dist = Math.sqrt(xDist * xDist + zDist * zDist);
    
                Vector vec = damagee.getVelocity();
    
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

                double xKb = -Math.sin(damager.getLocation().getYaw() * 3.1415927F / 180.0f) * knockbackLevels;
                double zKb = Math.cos(damager.getLocation().getYaw() * 3.1415927F / 180.0f) * knockbackLevels;

                kbEnch = new Vector(xKb * knockbackResistance, yMult, zKb * knockbackResistance);
                knockback.add(kbEnch);
            }
        }

        return knockback;
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

    public Entity getDamagee() {
        return damagee;
    }

    public void setNoKnockback() {
        this.knockback = null;
    }

    public Entity getFinalDamager() {
        return realDamager != null ? realDamager : damager;
    }
}

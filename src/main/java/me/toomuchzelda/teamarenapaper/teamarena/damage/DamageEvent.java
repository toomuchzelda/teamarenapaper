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
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftVector;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.LinkedList;

//a custom damage event; custom calculations for damage, knockback etc. good to enable things spigot/paper event can't
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

    private Entity damager;
    //shooter of arrow, snowball etc where attacker would be the projectile
    private Entity realDamager;
    private boolean isCritical;
    //if the attacker was sprinting, only applicable if the attacker is a Player
    private boolean wasSprinting;

    public static final double yVal = 0.4;
    public static final double xzVal = 0.4;
    public static final double yMult = 0.1;
    public static final double xzMult = 1;

    public DamageEvent(EntityDamageEvent event) {
        damagee = event.getEntity();
        rawDamage = event.getDamage();
        finalDamage = event.getFinalDamage();
        damageType = DamageType.getAttack(event);

        Bukkit.broadcast(Component.text("DamageCause: " + event.getCause()));
        Bukkit.broadcast(Component.text("DamageType: " + damageType.toString()));

        if(damageType.isKnockback())
            knockbackMults = new LinkedList<>();

        if(event instanceof EntityDamageByEntityEvent dEvent) {
            if(dEvent.getDamager() instanceof Projectile projectile) {

                if(dEvent.getDamager() instanceof AbstractArrow aa) {
                    knockbackMults.add((double) aa.getKnockbackStrength());
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
                        ItemStack item = living.getEquipment().getItemInMainHand();
                        //halve the strength of knockback enchantments
                        double level = ((double) item.getEnchantmentLevel(Enchantment.KNOCKBACK)) / 2;
                        knockbackMults.add(level);
                    }

                    if(living instanceof Player p) {
                        if(p.isSprinting()) {
                            knockbackMults.add(1d);
                        }
                        wasSprinting = p.isSprinting();
                    }
                }
            }
            isCritical = dEvent.isCritical();
        }

        //add stuff to knockback multipliers list before calculating knockback
        if(damageType.isKnockback())
            knockback = calculateKnockback();
        else
            knockback = null;
    }

    public void executeAttack() {
        if(damagee instanceof LivingEntity living) {
            if(knockback != null) {
                if (damagee instanceof Player player) {
                    //send knockback packet
                    Vec3 vec = CraftVector.toNMS(knockback);
                    ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(player.getEntityId(), vec);
                    PlayerUtils.sendPacket(player, packet);
                }
                else {
                    damagee.setVelocity(knockback);
                }
            }

            double newHealth = living.getHealth() - finalDamage;
            if(newHealth <= 0) {
                //todo: handle death here
                Bukkit.broadcast(Component.text(living.getName() + " has died"));
                newHealth = living.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            }
            living.setHealth(newHealth);
            EntityUtils.playHurtAnimation(living, damageType);
        }
        else if(!damagee.isInvulnerable()){
            damagee.remove();
        }

        if(damager instanceof LivingEntity living) {
            if (damager instanceof Player p) {
                if (wasSprinting) {
                    net.minecraft.world.entity.player.Player nmsPlayer = ((CraftPlayer) p).getHandle();
                    //3 for sprinting flag
                    // dont use setSprinting as it sends an attribute packet that may stop the client from sprinting
                    // server-client desync good? it's 1.8 behaviour anyway, may change later
                    nmsPlayer.setSharedFlag(3, false);
                }
                //reset their attack cooldown
                p.resetCooldown();
            }
        }
    }

    /**
     * determine if an entity can be hurt at any point in time
     * if yes, also update their invuln tick info
     * @param damagee entity getting hurt
     * @param damage amount of damage being dealt
     * @return
     */
    public boolean canHit(Entity damagee, double damage) {
        if(damagee instanceof Player p) {
            PlayerInfo info = Main.getPlayerInfo(p);
            long currentNDT = TeamArena.getGameTick() - info.lastHurt;

        }
        return false;
    }

    public Vector calculateKnockback() {
        Vector knockback = new Vector();
        if (damager != null)
        {
            Vector offset;
            if(damager instanceof Projectile && damageType.isProjectile())
            {
                offset = damager.getLocation().getDirection();
                offset.setZ(-offset.getZ());
            }
            else
            {
                offset = damager.getLocation().toVector().subtract(damagee.getLocation().toVector());
            }

            double xDist = offset.getX();
            double zDist = offset.getZ();

            while (!Double.isFinite(xDist * xDist + zDist * zDist) || xDist * xDist + zDist * zDist < 0.0001)
            {
                xDist = MathUtils.randomRange(-0.01, -0.01);
                zDist = MathUtils.randomRange(-0.01, -0.01);
            }

            double dist = Math.sqrt(xDist * xDist + zDist * zDist);

            Vector vec = damagee.getVelocity();

            vec.setX(vec.getX() / 2);
            vec.setY(vec.getY() / 2);
            vec.setZ(vec.getZ() / 2);

            vec.add(new Vector(-(xDist / dist * xzVal), yVal, -(zDist / dist * xzVal)));

            if(vec.getY() > yVal)
                vec.setY(yVal);

            knockback.add(vec);

            double level = 0;

            for (double value : knockbackMults)
            {
                level += value;
            }

            if (level != 0)
            {
                level *= xzMult;
                level /= 2;

                Vector kbEnch;

                double xKb = -Math.sin(damager.getLocation().getYaw() * 3.1415927F / 180.0f) * level;
                double zKb = Math.cos(damager.getLocation().getYaw() * 3.1415927F / 180.0f) * level;

                kbEnch = new Vector(xKb, yMult, zKb);
                knockback.add(kbEnch);
            }
        }

        return knockback;
    }

    @Nullable
    public Vector getKnockback() {
        return knockback;
    }
}

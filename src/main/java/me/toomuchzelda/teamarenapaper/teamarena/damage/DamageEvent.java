package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.core.EntityUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.core.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
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

    public static final double yVal = 0.4;
    public static final double xzVal = 0.4;
    public static final double yMult = 0.1;
    public static final double xzMult = 1;

    public DamageEvent(EntityDamageEvent event) {
        damagee = event.getEntity();
        rawDamage = event.getDamage();
        finalDamage = event.getFinalDamage();
        damageType = DamageType.getAttack(event.getCause());

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
                }
            }
            isCritical = dEvent.isCritical();
        }

        //add stuff to knockback multipliers list before calculating knockback
        knockback = calculateKnockback();
    }

    public void executeAttack() {
        if(damagee instanceof LivingEntity living) {
            EntityUtils.playHurtAnimation(living);
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
            if(finalDamage <= 0) {
                //todo: handle death here
                Bukkit.broadcast(Component.text(living.getName() + " has died"));
                newHealth = 10;
            }
            living.setHealth(newHealth);
        }
        else if(!damagee.isInvulnerable()){
            damagee.remove();
        }
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

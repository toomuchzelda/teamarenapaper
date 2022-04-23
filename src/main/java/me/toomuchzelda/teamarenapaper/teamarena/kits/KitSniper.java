package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.item.alchemy.Potion;

/**
 * @author onett425
 */
public class KitSniper extends Kit {

    public static int GRENADE_TIME = 40;
    public static boolean GRENADE_ARMED = false;
    public static boolean SNIPER_CHAT_MESSAGE_SENT = false;

    public KitSniper() {
        super("Sniper", "mlg", Material.SPYGLASS);

        ItemStack[] armour = new ItemStack[4];
        armour[3] = new ItemStack(Material.TURTLE_HELMET);
        armour[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
        armour[1] = new ItemStack(Material.LEATHER_LEGGINGS);
        armour[0] = new ItemStack(Material.LEATHER_BOOTS);
        this.setArmour(armour);

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemStack rifle = new ItemStack(Material.SPYGLASS);
        ItemMeta rifleMeta = rifle.getItemMeta();
        rifleMeta.displayName(ItemUtils.noItalics(Component.text("CheyTac Intervention")));
        rifle.setItemMeta(rifleMeta);
        ItemStack grenade = new ItemStack(Material.TURTLE_HELMET);
        ItemMeta grenadeMeta = grenade.getItemMeta();
        grenadeMeta.displayName(ItemUtils.noItalics(Component.text("Frag Grenade")));
        grenade.setItemMeta(grenadeMeta);

        setItems(sword, rifle, grenade);
        setAbilities(new SniperAbility());
    }
    
    public static class SniperAbility extends Ability{
        @Override
		public void giveAbility(Player player) {
            player.setExp(0.999f);
            GRENADE_ARMED = false;
		}
		
		@Override
		public void removeAbility(Player player) {
            // remove below line after testing
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(KitNinja.NINJA_SPEED_MODIFIER);
            player.setExp(0);
            GRENADE_ARMED = false;
		}

        @Override
        public void onInteract(PlayerInteractEvent event) {
           Material mat = event.getMaterial();
           Player player = event.getPlayer();
           Action action = event.getAction();
           PlayerInfo pinfo = Main.getPlayerInfo(player);
            
            
           //Grenade Pull Pin
            if(mat == Material.TURTLE_HELMET && !player.hasCooldown(Material.TURTLE_HELMET) && player.getExp() == 0.999f ){
                Component text = Component.text("Left Click to THROW    Right Click to TOSS").color(TextColor.color(242, 44, 44));
                if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(text);
				}
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					player.sendMessage(text);
				}
                player.setExp(0);
                player.setCooldown(Material.TURTLE_HELMET, 2 * 20);
                GRENADE_ARMED = true;
                
            }
            //Grenade Throw
            if(mat == Material.TURTLE_HELMET && player.hasCooldown(Material.TURTLE_HELMET)){
                //Left Click => Hard Throw
                if(action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)){
                    player.getInventory().removeItemAnySlot(new ItemStack(Material.TURTLE_HELMET));
                }
                //Right Click => Soft Toss
                if(action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)){
                    player.getInventory().removeItemAnySlot(new ItemStack(Material.TURTLE_HELMET));
                }

            }
        }

        //Headshot
        @Override
        public void projectileHitEntity(ProjectileCollideEvent event){
            Projectile projectile = event.getEntity();
            Entity victim = event.getCollidedWith();
            if(victim instanceof Player player && projectile.getType() == EntityType.ARROW){
                double headLocation = player.getLocation().getY();
                double projectileHitY = projectile.getLocation().getY();
                //Every shot is considered a headshot if you are too close
                if(projectileHitY - headLocation > 1.35d && projectile.getOrigin().distance(projectile.getLocation()) > 10){
                    player.damage(999);
                }
            }
        }
        //Sniper Rifle Shooting
        public void playerDropItem(PlayerDropItemEvent event) {
            Player player = event.getPlayer();
            //if(event.getItemDrop().getItemStack().equals(new ItemStack(Material.SPYGLASS)) && !player.hasCooldown(Material.SPYGLASS)){
                event.getItemDrop().setPickupDelay(1);
                //Inaccuracy based on movement (like CS:GO)
                //The more the player is moving, the less accurate their shot
                Vector direction = player.getLocation().getDirection();
                double inaccuracyMax = player.getVelocity().length();
                double inaccuracy = MathUtils.random.nextGaussian() * inaccuracyMax * 0.100d;
                double xDeviance = MathUtils.random.nextGaussian() * inaccuracy;
                double yDeviance = MathUtils.random.nextGaussian() * inaccuracy;
                double zDeviance = MathUtils.random.nextGaussian() * inaccuracy;
                direction.setX(direction.getX() + xDeviance);
		        direction.setY(direction.getY() + yDeviance);
		        direction.setZ(direction.getZ() + zDeviance);
                //Shooting Rifle
                Vector arrowVelocity = direction.normalize().multiply(15d);
                player.launchProjectile(Arrow.class, arrowVelocity).setPickupStatus(PickupStatus.DISALLOWED);
                player.setCooldown(Material.SPYGLASS, (int) (2.5*20));
           // }
            //else{
           //     event.setCancelled(true);
           // }
            //Testing below
        }

        @Override
        public void onPlayerTick(Player player) {
            float exp = player.getExp();
            
            //Speed based on held item
            if(player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD){
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 123123, 1));
            }
            else{
                if(player.hasPotionEffect(PotionEffectType.SPEED)){
                    player.removePotionEffect(PotionEffectType.SPEED);
                }
            }

            //Sniper Action Bar
            if(player.getInventory().getItemInMainHand().getType() == Material.SPYGLASS){
                Component text = Component.text("Drop Item to Shoot").color(TextColor.color(89, 237, 76));
                PlayerInfo pinfo = Main.getPlayerInfo(player);
                if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(text);
				}
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES) && !SNIPER_CHAT_MESSAGE_SENT) {
					player.sendMessage(text);
                    SNIPER_CHAT_MESSAGE_SENT = true;
				}
            }
            //Sniper Shoot
            //Problem: When players are sneaking, their velocity is automatically very low, test after implementing drop sniper to shoot
            if(player.getInventory().getItemInMainHand().getType() == Material.SPYGLASS && player.getPose() == Pose.SNEAKING && !player.hasCooldown(Material.SPYGLASS)){
                Vector direction = player.getLocation().getDirection();
                Vector velocity = player.getVelocity();
                //Nullifying gravity, which applies even if a player is standing still
                    if(Math.abs(velocity.getY()) <= 1){
                        velocity.setY(0);
                    }
                double inaccuracyAmp = velocity.length();
                //Ensuring extreme velocities does not make sniper TOO inaccurate
                //inaccuracyAmp will range from 0 to 1
                if(inaccuracyAmp > 5){
                    inaccuracyAmp = 5d;
                }
                inaccuracyAmp = inaccuracyAmp / 5;
                double xDeviance = (Math.random() - 0.5) * inaccuracyAmp * 10d;
                double yDeviance = (Math.random() - 0.5) * inaccuracyAmp * 10d;
                double zDeviance = (Math.random() - 0.5) * inaccuracyAmp * 10d;
                direction.setX(direction.getX() + xDeviance);
                if(Math.abs(player.getVelocity().getY()) >= 1){
                    direction.setY(direction.getY() + yDeviance);
                }
		        direction.setZ(direction.getZ() + zDeviance);
                direction = direction.normalize();
                Vector arrowVelocity = direction.multiply(10d);
                //Shooting Rifle + Arrow Properties
                Arrow arrow = player.launchProjectile(Arrow.class, arrowVelocity);
                arrow.setShotFromCrossbow(true);
                arrow.setGravity(false);
                arrow.setKnockbackStrength(1);
                arrow.setCritical(false);
                arrow.setPickupStatus(PickupStatus.DISALLOWED);
                arrow.setPierceLevel(100);
                arrow.setDamage(1.5);

                player.setCooldown(Material.SPYGLASS, (int) (2.5*20));
            }
            //Grenade Cooldown
            if(!GRENADE_ARMED){
                //10 second cooldown = 200 ticks
                //1/200 = 0.005
                if(exp + 0.005f >= 1){
                    player.setExp(0.999f);
                }
                else{
                    player.setExp(exp + 0.005f);
                }
            }
            //Grenade Pin Pulled
            //2 Seconds until detonation
            // 1/40 = 0.025
            if(GRENADE_ARMED){
                if(exp >= 0.025f){
                    player.setExp(exp - 0.025f);
                }
                //Grenade Explodes
                if(exp - 0.025f <= 0){
                    player.setExp(0);
                    //If Grenade is Armed and player has not thrown it, they insta-die
                   // if(player.getInventory().contain)
                   // GRENADE_ARMED = false;
                }
                
            }
            

        }
        
    }
}

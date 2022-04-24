package me.toomuchzelda.teamarenapaper.teamarena.kits;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.world.item.alchemy.Potion;

/**
 * @author onett425
 */
public class KitSniper extends Kit {

    public static int GRENADE_TIME = 40;
    public static boolean GRENADE_ARMED = false;
    public static boolean SNIPER_CHAT_MESSAGE_SENT = false;
    public static boolean GRENADE_CHAT_MESSAGE_SENT = false;
    public static int EXPECTED_GRENADE_COUNT = 0;
    public static final ItemStack SNIPER_RIFLE = new ItemStack(Material.SPYGLASS);
    public static final Set<BukkitTask> GRENADE_TASKS = new HashSet<>();
    static{
        ItemMeta rifleMeta = SNIPER_RIFLE.getItemMeta();
        rifleMeta.displayName(ItemUtils.noItalics(Component.text("CheyTac Intervention")));
        SNIPER_RIFLE.setItemMeta(rifleMeta);
    }

    public KitSniper() {
        super("Sniper", "mlg", Material.SPYGLASS);

        ItemStack[] armour = new ItemStack[4];
        armour[3] = new ItemStack(Material.TURTLE_HELMET);
        armour[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
        armour[1] = new ItemStack(Material.LEATHER_LEGGINGS);
        armour[0] = new ItemStack(Material.LEATHER_BOOTS);
        this.setArmour(armour);

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemStack rifle = SNIPER_RIFLE;
        ItemStack grenade = new ItemStack(Material.TURTLE_HELMET);
        ItemMeta grenadeMeta = grenade.getItemMeta();
        grenadeMeta.displayName(ItemUtils.noItalics(Component.text("Frag Grenade")));
        grenade.setItemMeta(grenadeMeta);

        setItems(sword, rifle, grenade);
        setAbilities(new SniperAbility());
    }
    
    public static class SniperAbility extends Ability{
        @Override
        public void unregisterAbility() {	
			Iterator<BukkitTask> iter = GRENADE_TASKS.iterator();
			while(iter.hasNext()) {
				BukkitTask task = iter.next();
				task.cancel();
				iter.remove();
			}
		}
        
        @Override
		public void giveAbility(Player player) {
            player.setExp(0.999f);
            GRENADE_ARMED = false;
		}
		
		@Override
		public void removeAbility(Player player) {
            player.setExp(0);
            GRENADE_ARMED = false;
            SNIPER_CHAT_MESSAGE_SENT = false;
            GRENADE_CHAT_MESSAGE_SENT = false;
		}

        public void throwGrenade(double amp, Player player){
            World world = player.getWorld();
            Item activeGrenade = world.dropItem(player.getLocation(), new ItemStack(Material.TURTLE_HELMET));
            activeGrenade.setCanPlayerPickup(false);
            activeGrenade.setCanMobPickup(false);
            Vector direction = player.getLocation().getDirection();
            activeGrenade.setVelocity(direction.multiply(amp));
             BukkitTask runnable = new BukkitRunnable(){
                //Grenade explosion
                public void run() {
                    //Detonation
                    if(GRENADE_TIME == 0){
                        world.createExplosion(activeGrenade.getLocation(), 0.5f, false, false);
                        GRENADE_TASKS.remove(this);
                        cancel();
                    }
                }
                 
             }.runTaskTimer(Main.getPlugin(), 0, 0);
             GRENADE_TASKS.add(runnable);
        }

        @Override
        public void onInteract(PlayerInteractEvent event) {
           Material mat = event.getMaterial();
           Player player = event.getPlayer();
           World world = player.getWorld();
           PlayerInventory inv = player.getInventory();
           Action action = event.getAction();
           PlayerInfo pinfo = Main.getPlayerInfo(player); 
            
           //Grenade Pull Pin
            if(mat == Material.TURTLE_HELMET && !player.hasCooldown(Material.TURTLE_HELMET) && player.getExp() == 0.999f && player.getInventory().getItemInMainHand().getType() == Material.TURTLE_HELMET){
                Component actionBar = Component.text("Left Click to THROW    Right Click to TOSS").color(TextColor.color(242, 44, 44));
                Component text = Component.text("Left Click to Throw the Grenade, Right Click to Lightly Toss").color(TextColor.color(242, 44, 44));
                if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					player.sendMessage(text);
				}
                player.setExp(0);
                player.setCooldown(Material.TURTLE_HELMET, 2 * 20);
                for (ItemStack stack : player.getInventory().getContents()) {
                    if (stack != null && stack.getType() == Material.TURTLE_HELMET) {
                        EXPECTED_GRENADE_COUNT += stack.getAmount();
                    }
                }
                GRENADE_ARMED = true;
                
            }
            //Grenade Throw
            else if(mat == Material.TURTLE_HELMET && player.hasCooldown(Material.TURTLE_HELMET) && player.getInventory().getItemInMainHand().getType() == Material.TURTLE_HELMET){
                //Left Click => Hard Throw
                if(action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)){
                    //Removes 1 grenade from hand
                    inv.setItem(inv.getHeldItemSlot(), inv.getItem(inv.getHeldItemSlot()).subtract());
                    throwGrenade(1, player);
                    
                }
                //Right Click => Soft Toss
                if(action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)){
                    inv.setItem(inv.getHeldItemSlot(), inv.getItem(inv.getHeldItemSlot()).subtract());
                    throwGrenade(0.2d, player);
                }
            }

            if(player.getInventory().getItemInOffHand().getType() == Material.TURTLE_HELMET){
                Component actionBar = Component.text("Grenade must be thrown with Main Hand").color(TextColor.color(224, 52, 69));
                Component text = Component.text("Frag Grenade is too heavy to be thrown with your Off Hand").color(TextColor.color(224, 52, 69));
                if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					player.sendMessage(text);
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
            Item item = event.getItemDrop();
            item.setOwner(player.getUniqueId());
            if(item.getItemStack().getType() == Material.SPYGLASS && !player.hasCooldown(Material.SPYGLASS)){
                //Inaccuracy based on movement
                //player.getVelocity() sux so i have to make a weird calculation
                Vector direction = player.getEyeLocation().getDirection();
                double inaccuracy = 0;
                if(player.isSprinting() || player.isGliding() || player.isJumping() || player.getLocation().subtract(0,1,0).getBlock().getType() == Material.AIR || Math.abs(player.getVelocity().length()) > 1){
                    inaccuracy = 0.2;
                }
                if(player.isInWater() || player.isSwimming()){
                    inaccuracy = 0.1;
                }
                direction.add(new Vector(Math.random() * inaccuracy - inaccuracy, Math.random() * inaccuracy - inaccuracy, Math.random() * inaccuracy - inaccuracy));
                Vector arrowVelocity = direction.multiply(10d);
                //System.out.println("direction" + direction + "mod Direction" + arrowVelocity + "block being standed on: " + player.getLocation().getBlock().getType());
                //Shooting Rifle + Arrow Properties
                Arrow arrow = player.launchProjectile(Arrow.class, arrowVelocity);
                arrow.setShotFromCrossbow(true);
                arrow.setGravity(false);
                arrow.setKnockbackStrength(1);
                arrow.setCritical(false);
                arrow.setPickupStatus(PickupStatus.DISALLOWED);
                arrow.setPierceLevel(100);
                arrow.setDamage(1);

                //Sniper Cooldown + ensuring the player picks it up again after dropping
                player.setCooldown(Material.SPYGLASS, (int) (2.5*20));
                item.setPickupDelay(0);
                item.setVelocity(player.getVelocity());
            }
            else{
                event.setCancelled(true);
            }
        }

        @Override
        public void onPlayerTick(Player player) {
            float exp = player.getExp();
            World world = player.getWorld();
            
            //Speed based on held item
            if(player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD){
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 123123, 0));
            }
            else{
                if(player.hasPotionEffect(PotionEffectType.SPEED)){
                    player.removePotionEffect(PotionEffectType.SPEED);
                }
            }

            //Sniper Information
            if(player.getInventory().getItemInMainHand().getType() == Material.SPYGLASS){
                Component actionBar = Component.text("Drop Spyglass in hand to Shoot").color(TextColor.color(89, 237, 76));
                Component text = Component.text("Drop Spyglass in your main hand to Shoot").color(TextColor.color(89, 237, 76));
                PlayerInfo pinfo = Main.getPlayerInfo(player);
                if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
                //Chat Message is only sent once per life
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES) && !SNIPER_CHAT_MESSAGE_SENT) {
					player.sendMessage(text);
                    SNIPER_CHAT_MESSAGE_SENT = true;
				}
            }

            //Grenade Information
            if(player.getInventory().getItemInMainHand().getType() == Material.TURTLE_HELMET){
                Component actionBar = Component.text("Left/Right Click to Arm").color(TextColor.color(66, 245, 158));
                Component text = Component.text("Click to arm the grenade").color(TextColor.color(66, 245, 158));
                PlayerInfo pinfo = Main.getPlayerInfo(player);
                if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(text);
				}
                //Chat Message is only sent once per life
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES) && !GRENADE_CHAT_MESSAGE_SENT) {
					player.sendMessage(text);
                    GRENADE_CHAT_MESSAGE_SENT = true;
				}
            }
            //Grenade Cooldown
            if(!GRENADE_ARMED){
                //10 second cooldown = 200 ticks
                //1/200 = 0.005
                if(exp == 0.999f){

                }
                else if(exp + 0.005f >= 1){
                    player.setExp(0.999f);
                }
                else{
                    player.setExp(exp + 0.005f);
                }
            }
            //Checking if the player failed to throw grenade
            if(GRENADE_ARMED){
                GRENADE_TIME--;
                //Grenade Explodes
                if(GRENADE_TIME <= 0){
                    int observedCount = 0;
                    for (ItemStack stack : player.getInventory().getContents()) {
                        if (stack != null && stack.getType() == Material.TURTLE_HELMET) {
                            observedCount += stack.getAmount();
                        }
                    }
                    //equality check prevents instant death when admin abused it use to give many grenades to self.
                    if(observedCount == EXPECTED_GRENADE_COUNT){
                        EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, 999d);
                        DamageEvent damageEvent = DamageEvent.createDamageEvent(event);
                        if (damageEvent != null) {
                            damageEvent.setDamageType(DamageType.SNIPER_GRENADE_FAIL);
                        }
                    }
                    GRENADE_ARMED = false;
                    GRENADE_TIME = 40;
                    EXPECTED_GRENADE_COUNT = -1;
                }
                
            }
            

        }
        
    }
}

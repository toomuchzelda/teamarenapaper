package me.toomuchzelda.teamarenapaper.teamarena.kits;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * @author onett425
 */
public class KitSniper extends Kit {
    public static boolean SNIPER_CHAT_MESSAGE_SENT = false;
    public static boolean GRENADE_CHAT_MESSAGE_SENT = false;
    public static int EXPECTED_GRENADE_COUNT = 0;
    public static final ItemStack GRENADE;
    public static final ItemStack SNIPER;
    public static final Set<BukkitTask> GRENADE_TASKS = new HashSet<>();
    
    static{
        GRENADE = new ItemStack(Material.TURTLE_HELMET);
        ItemMeta grenadeMeta = GRENADE.getItemMeta();
        grenadeMeta.displayName(ItemUtils.noItalics(Component.text("Frag Grenade")));
        GRENADE.setItemMeta(grenadeMeta);

        SNIPER = new ItemStack(Material.SPYGLASS);
        ItemMeta rifleMeta = SNIPER.getItemMeta();
        rifleMeta.displayName(ItemUtils.noItalics(Component.text("CheyTac Intervention")));
        SNIPER.setItemMeta(rifleMeta);
    }
    public KitSniper() {
        super("Sniper", "Be careful when sniping... Too much movement and your aim will become trash. Don't forget to throw the grenade if you pull the pin btw.", Material.SPYGLASS);

        ItemStack[] armour = new ItemStack[4];
        armour[3] = new ItemStack(Material.LEATHER_HELMET);
        armour[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
        armour[1] = new ItemStack(Material.LEATHER_LEGGINGS);
        armour[0] = new ItemStack(Material.LEATHER_BOOTS);
        this.setArmour(armour);

        ItemStack sword = new ItemStack(Material.IRON_SWORD);

        setItems(sword, SNIPER, GRENADE);
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
		}
		
		@Override
		public void removeAbility(Player player) {
            player.setExp(0);
            player.getInventory().remove(Material.TURTLE_HELMET);
            EXPECTED_GRENADE_COUNT = 0;
            SNIPER_CHAT_MESSAGE_SENT = false;
            GRENADE_CHAT_MESSAGE_SENT = false;
		}

        public void throwGrenade(Player player, double amp, int itemSlot){
            World world = player.getWorld();
            PlayerInfo pInfo = Main.getPlayerInfo(player);
             BukkitTask runnable = new BukkitRunnable(){
                //Grenade explosion
                int timer = player.getCooldown(Material.TURTLE_HELMET);
                boolean launched = false;
                Item activeGrenade;
                public void run() {
                    if(launched){
                        //Grenade Particles
                        //In Motion
                        TeamArenaTeam team = Main.getPlayerInfo(player).team;
                        Color teamColor = team.getColour();
                        if(activeGrenade.getVelocity().length() > 0){
                            world.spawnParticle(Particle.REDSTONE, activeGrenade.getLocation(), 1, new Particle.DustOptions(teamColor, 2f));
                        }
                        else{
                             //On the ground
                             world.spawnParticle(Particle.REDSTONE, activeGrenade.getLocation().add(Vector.getRandom().subtract(new Vector(-0.5,-0.5,-0.5)).multiply(2)), 1, new Particle.DustOptions(teamColor, 2f));
                        }
                        
                       
                    }
                    if(timer <= 0){
                        //Grenade Fail
                        //Check if inventory has any grenades, maybe update later to allow for admin abuse grenade spam
                        System.out.println(launched);
                        if(launched){
                            world.createExplosion(activeGrenade.getLocation(), 1.7f, false, false);
                            player.getInventory().addItem(GRENADE);
                            activeGrenade.remove();
                        }
                        if(!launched){
                            world.createExplosion(player.getLocation(), 2f, false, false);
                            player.damage(999);
                        }
                        cancel();   
                        GRENADE_TASKS.remove(this);
                    }
                    else if(!launched){
                        //Grenade Success
                        if(!player.getInventory().contains(Material.TURTLE_HELMET)){
                            activeGrenade = world.dropItem(player.getLocation(), new ItemStack(Material.TURTLE_HELMET));
                            activeGrenade.setCanPlayerPickup(false);
                            activeGrenade.setCanMobPickup(false);
                            Vector direction = player.getLocation().getDirection();
                            activeGrenade.setVelocity(direction.multiply(amp));
                            launched = true;
                            world.playSound(activeGrenade.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1f, 1.1f);
                        }             
                    }
                    timer--;
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
                Component text = Component.text("Left Click to throw the grenade, Right Click to lightly toss it").color(TextColor.color(242, 44, 44));
                if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					player.sendMessage(text);
				}
                player.setExp(0);
                player.setCooldown(Material.TURTLE_HELMET, (int) 3.5 * 20);
                world.playSound(player, Sound.ITEM_FLINTANDSTEEL_USE, 2.0f, 1.5f);
            }
            //Grenade Throw
            //Main Hand ONLY
            else if(mat == Material.TURTLE_HELMET && player.hasCooldown(Material.TURTLE_HELMET) && player.getInventory().getItemInMainHand().getType() == Material.TURTLE_HELMET){
                //Left Click => Hard Throw
                if(action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)){
                    //Removes 1 grenade from hand
                    inv.setItem(inv.getHeldItemSlot(), inv.getItem(inv.getHeldItemSlot()).subtract()); 
                    throwGrenade(player, 1.5d, player.getInventory().getHeldItemSlot());
                }
                //Right Click => Soft Toss
                if(action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)){
                    inv.setItem(inv.getHeldItemSlot(), inv.getItem(inv.getHeldItemSlot()).subtract());
                    throwGrenade(player, 0.8d, player.getInventory().getHeldItemSlot());
                }
            }
        }

        //Headshot
        @Override
        public void projectileHitEntity(ProjectileCollideEvent event){
            Projectile projectile = event.getEntity();
            Entity victim = event.getCollidedWith();
            Player shooter = (Player) projectile.getShooter();
            if(victim instanceof Player player && projectile.getType() == EntityType.ARROW){
                double headLocation = player.getLocation().getY();
                double projectileHitY = projectile.getLocation().getY();
                //Must consider when player is below the other player, which makes getting headshots much harder.
                double headshotThresh = 1.35d;
                double heightDiff = victim.getLocation().getBlockY() - shooter.getLocation().getBlockY();
                if(heightDiff > 0){
                    headshotThresh -= Math.min(0.35, (heightDiff/10));
                }
                //Disabled headshot if you are too close since it was buggy
                if(projectileHitY - headLocation > headshotThresh && projectile.getOrigin().distance(projectile.getLocation()) > 10){
                    player.damage(999);
                    //Hitmarker Sound effect
                    //shooter.playSound(shooter.getLocation(), Sound.ENTITY_ITEM_FRAME_PLACE, 2f, 2.0f);
                }
            }
        }
        //Sniper Rifle Shooting
        public void playerDropItem(PlayerDropItemEvent event) {
            Player player = event.getPlayer();
            Item item = event.getItemDrop();
            World world = player.getWorld();
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
                world.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 2.5f);

                //Sniper Cooldown + deleting the dropped sniper and returning a new one.
                player.setCooldown(Material.SPYGLASS, (int) (2.5*20));
                item.remove();
                //If selected slot is occupied, find next available slot and add.
                if(player.getInventory().getItem((player.getInventory().getHeldItemSlot())) != null){
                    player.getInventory().setItem(player.getInventory().firstEmpty(), SNIPER);
                }
                else{
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), SNIPER);
                }
            }
            else{
                event.setCancelled(true);
            }
        }

        @Override
        public void onPlayerTick(Player player) {
            float exp = player.getExp();

            //Grenade Cooldown
            if(exp == 0.999f){

            }
            else if(exp + 0.005f >= 1){
                player.setExp(0.999f);
            }
            else{
                player.setExp(exp + 0.005f);
            }
            
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
            if(player.getInventory().getItemInMainHand().getType() == Material.TURTLE_HELMET && player.getExp() == 0.999f){
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

            //Sniper Reload Sound
            if(player.getCooldown(Material.SPYGLASS) == 15){
                player.playSound(player, Sound.ITEM_ARMOR_EQUIP_CHAIN, 2f, 0.8f);
            }
        }
    }
}

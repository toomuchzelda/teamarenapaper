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
import org.bukkit.entity.Projectile;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * @author onett425
 */
public class KitSniper extends Kit {

    public static int GRENADE_TIME = 40;
    public static boolean GRENADE_ARMED = false;

    public KitSniper() {
        super("Sniper", "mlg", Material.SPYGLASS);

        ItemStack[] armour = new ItemStack[4];
        armour[3] = new ItemStack(Material.LEATHER_HELMET);
        armour[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
        armour[1] = new ItemStack(Material.LEATHER_LEGGINGS);
        armour[0] = new ItemStack(Material.LEATHER_BOOTS);
        this.setArmour(armour);

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemStack rifle = new ItemStack(Material.SPYGLASS);
        ItemMeta rifleMeta = rifle.getItemMeta();
        rifleMeta.displayName(ItemUtils.noItalics(Component.text("CheyTac Intervention")));
        rifle.setItemMeta(rifleMeta);
        ItemStack grenade = new ItemStack(Material.DRIED_KELP_BLOCK);
        ItemMeta grenadeMeta = grenade.getItemMeta();
        grenadeMeta.displayName(ItemUtils.noItalics(Component.text("Frag Grenade")));
        grenade.setItemMeta(grenadeMeta);

        setItems(sword, rifle, grenade);
        setAbilities(new SniperAbility());
    }
    
    public static class SniperAbility extends Ability{
        @Override
		public void giveAbility(Player player) {
			//Removed while testing
            //player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(KitNinja.NINJA_SPEED_MODIFIER);
            player.setExp(0.999f);
            GRENADE_ARMED = false;
		}
		
		@Override
		public void removeAbility(Player player) {
			//Removed while testing
            //player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(KitNinja.NINJA_SPEED_MODIFIER);
            player.setExp(0);
            GRENADE_ARMED = false;
		}

        @Override
        public void onInteract(PlayerInteractEvent event) {
           Material mat = event.getMaterial();
           Player player = event.getPlayer();
           Action action = event.getAction();
           PlayerInfo pinfo = Main.getPlayerInfo(player);
            //Sniper Rifle is fired
            if(player.getInventory().getItemInMainHand().getType() == Material.SPYGLASS && player.hasCooldown(Material.SPYGLASS)){
                event.setCancelled(true);
            }
            else if(player.getInventory().getItemInMainHand().getType() == Material.SPYGLASS && (action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK) && !player.hasCooldown(Material.SPYGLASS))){
                Vector velocity = player.getLocation().getDirection().normalize().multiply(8D);
                player.launchProjectile(Arrow.class, velocity).setPickupStatus(PickupStatus.DISALLOWED);
                player.setCooldown(Material.SPYGLASS, (int) (2.5*20));
            }
            
            //Grenade Pull Pin
            if(mat == Material.DRIED_KELP_BLOCK && !player.hasCooldown(Material.DRIED_KELP_BLOCK) && player.getExp() == 0.999f){
                Component text = Component.text("Left Click to THROW    ").color(TextColor.color(242, 44, 44));
                text.append(Component.text("Right Click to TOSS").color(TextColor.color(235, 152, 89)));  
                if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(text);
				}
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					player.sendMessage(text);
				}
                player.setExp(0);
                player.setCooldown(Material.DRIED_KELP, 2 * 20);
                GRENADE_ARMED = true;
                
            }
            //Grenade Throw
            if(mat == Material.DRIED_KELP_BLOCK && player.hasCooldown(Material.DRIED_KELP_BLOCK)){
                //Left Click => Hard Throw
                if(action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)){
                    player.getInventory().removeItemAnySlot(new ItemStack(Material.DRIED_KELP_BLOCK));
                }
                //Right Click => Soft Toss
                if(action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)){
                    player.getInventory().removeItemAnySlot(new ItemStack(Material.DRIED_KELP_BLOCK));
                }

            }
        }

        @Override
        public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
            Projectile projectile = event.getProjectile();
            Player player = event.getPlayer();
            ItemStack item = event.getItemStack();
            //Sniper Arrow Properties
            if(item.getType() == Material.SPYGLASS && projectile instanceof Arrow){
                if(player.hasCooldown(Material.SPYGLASS)){
                    event.setCancelled(true);
                }
                Arrow arrow = (Arrow) projectile;
                arrow.setGravity(false);
                arrow.setPierceLevel(5);
                arrow.setKnockbackStrength(1);
                arrow.setCritical(false);
                arrow.setDamage(12);
            }
        }

        //Headshot
        @Override
        public void projectileHitEntity(ProjectileCollideEvent event){
            Projectile projectile = event.getEntity();
            Entity victim = event.getCollidedWith();
            if(victim instanceof Player player && projectile.getType() == EntityType.ARROW){
                //Head region is the top 1/4 of the player's body
                double headLocation = player.getBoundingBox().getCenterY() + 0.5D;
                Location projectileHitLoc = projectile.getLocation();
                if(Math.abs(projectileHitLoc.getY() - headLocation) <= 0.5){
                    player.damage(999);
                }
            }
        }

        @Override
        public void onPlayerTick(Player player) {
            float exp = player.getExp();
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

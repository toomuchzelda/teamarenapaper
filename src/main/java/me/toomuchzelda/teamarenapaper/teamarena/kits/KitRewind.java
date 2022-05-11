package me.toomuchzelda.teamarenapaper.teamarena.kits;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.event.Event;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.LivingEntity;

public class KitRewind extends Kit{

    public static final HashMap<Integer, Location> PREV_LOCS = new HashMap<>();
    public static final Set<BukkitTask> REWIND_TASKS = new HashSet<>();

    public KitRewind(){
        super("Rewind", "Travel 15 seconds back in time with your rewind clock. sample text", Material.CLOCK);
        ItemStack helmet = new ItemStack(Material.CHAINMAIL_HELMET);
        ItemStack chest = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta chestMeta = chest.getItemMeta();
        chestMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, false);
        chest.setItemMeta(chestMeta);
        ItemStack legs = new ItemStack(Material.IRON_LEGGINGS);
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        setArmor(helmet, chest, legs, boots);

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta clockMeta = clock.getItemMeta();
        clockMeta.displayName(ItemUtils.noItalics(Component.text("Time Machine")));
        ItemStack timeStasis = new ItemStack(Material.LODESTONE);
        ItemMeta stasisMeta = timeStasis.getItemMeta();
        stasisMeta.displayName(ItemUtils.noItalics(Component.text("Time Stasis")));
        setItems(sword, clock, timeStasis);

        setAbilities(new RewindAbility());

    }

    public static class RewindAbility extends Ability{

        //clean up
		public void unregisterAbility() {
			PREV_LOCS.clear();
		}

        public void giveAbility(Player player) {
			player.setCooldown(Material.CLOCK, 15*20);
		}

        public void onPlayerTick(Player player) {
            Location loc = player.getLocation();
            int currTick = player.getTicksLived();
            int elapsedTick = currTick % (15 * 20);
            //Checking that the current location is a valid rewind location, if it is, add it to possible rewind locations.
            if(!loc.getBlock().isEmpty()){
              PREV_LOCS.put(player.getTicksLived(), player.getLocation());  
            }
            //Displaying the current cycle in the action bar, providing distinct sound cues for people who don't use action bar
            if(elapsedTick >= 0 && elapsedTick < 5*20){

            }
            else if(elapsedTick < 10*20){

            }
            else{

            }
        }

        //Cancels damage that is dealt while in stasis
        public void onAttemptedDamage(DamageEvent event) {
            Player player = event.getPlayerVictim();
            if(player.hasPotionEffect(PotionEffectType.INVISIBILITY)){
                event.setCancelled(true);
            }
        }

        public void onInteract(PlayerInteractEvent event) {
            Material mat = event.getMaterial();
            Player player = event.getPlayer();
            
            //preventing "Time Stasis" lodestone from being placed
            if(event.useItemInHand() != Event.Result.DENY && mat == Material.LODESTONE) {
				event.setUseItemInHand(Event.Result.DENY);
			}

            //Rewind Clock implementation
            if(mat == Material.CLOCK && player.getCooldown(Material.CLOCK) == 0){
                int currTick = player.getTicksLived();
                int pastTick = currTick - (15 * 20);
                //line 110 causes issues
                while(PREV_LOCS.get(pastTick) == null){
                    pastTick++;
                }
                Location pastLoc = PREV_LOCS.get(pastTick);
                rewindBuff(player, currTick);
                player.teleport(pastLoc);
                rewindBuff(player, currTick);
                player.setCooldown(Material.CLOCK, 15 * 20);
            }

            //Time Stasis implementation
            if(mat == Material.LODESTONE && player.getCooldown(Material.LODESTONE) == 0){
                //true duration: 0.5 * 20
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) 8 * 20, 1));
                player.setInvisible(true);
                BukkitTask runnable = new BukkitRunnable() {
                    //true duration: 0.5 * 20
                    int activeDuration = (int) 8 * 20;
                    public void run() {
                        if (activeDuration <= 0) {
                            cancel();
                            REWIND_TASKS.remove(this);
                            player.setInvisible(false);
                        }
                        else{
                            activeDuration--;
                            //Add particle effect which will show where the stasis player is

                        }
                    }  
                }.runTaskTimer(Main.getPlugin(), 0, 0);

                REWIND_TASKS.add(runnable);
                player.setCooldown(Material.LODESTONE, 10 * 20);
            }
        }

        //When rewinding, a buff is given based on a 15 second cycle with 3 sections, each with a 5 second timeframe
        public void rewindBuff(Player player, int currTick){
            //Returns how far the currTick is in the cycle
            //[0, 299]
            int elapsedTick = currTick % (15 * 20);
            Location loc = player.getLocation();
            if(elapsedTick >= 0 && elapsedTick < 5*20){
                //Regen 2 for 7.5 seconds => 3 hearts healed
                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 150, 1));
            }
            else if(elapsedTick < 10*20){
                //Time Dilation
                List<Entity> affectedEnemies = player.getNearbyEntities(8, 8, 8);
                for(Entity entity : affectedEnemies){
                    if(entity instanceof org.bukkit.entity.LivingEntity victim && !(entity.getType().equals(EntityType.ARMOR_STAND))){
                        //change to 3*20 tick duration, extended for testing purposes
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 8*20, 250, true));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 8*20, 2, true));
                    }
                }
            }
            else{
                //Knockback (not functional)
                List<Entity> affectedEnemies = player.getNearbyEntities(8, 8, 8);
                for(Entity entity : affectedEnemies){
                    if(entity instanceof org.bukkit.entity.LivingEntity victim && !(entity.getType().equals(EntityType.ARMOR_STAND))){
                        Location victimLoc = victim.getLocation();
                        Vector distanceVector = victimLoc.subtract(player.getLocation()).toVector();
                        Vector kbAmp = new Vector(1d,1d,1d);
                        if(distanceVector.lengthSquared() <= 3){
                            kbAmp.multiply(3);
                        }
                        else{
                            kbAmp.multiply(9 / distanceVector.lengthSquared());
                        }
                        Vector launch = distanceVector.normalize().multiply(kbAmp);
                        victim.setVelocity(launch);
                    }
                }
            }
        }
    }
}

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
        clock.setItemMeta(clockMeta);
        ItemStack timeStasis = new ItemStack(Material.SHULKER_SHELL);
        ItemMeta stasisMeta = timeStasis.getItemMeta();
        stasisMeta.displayName(ItemUtils.noItalics(Component.text("The World")));
        timeStasis.setItemMeta(stasisMeta);
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

        public void removeAbility(Player player){
            player.resetPlayerTime();
        }

        public void onPlayerTick(Player player) {
            //Player tick is used to determine cooldowns + abilities
            //Time tick is purely aesthetic
            Location loc = player.getLocation();
            int currTick = player.getTicksLived();
            int elapsedTick = currTick % (15 * 20);
            long timeTick = player.getPlayerTimeOffset();
            //Since each time period (day, sunset, night) has a different time frame, time step varies so each cycle lasts 5 seconds
            long timeStepSize = 0;
            //Checking that the current location is a valid rewind location, if it is, add it to possible rewind locations.
            if(!loc.getBlock().isEmpty() && loc.getBlock().getType() != Material.LAVA){
              PREV_LOCS.put(player.getTicksLived(), player.getLocation());  
            }

            //Displaying the current cycle in the action bar, providing distinct sound cues for people who don't use action bar
            Component currState = Component.text("sample text");;
            if(elapsedTick >= 0 && elapsedTick < 5*20){
                currState = Component.text("Current State: Regen");
            }
            else if(elapsedTick < 10*20){
                currState = Component.text("Current State: Time Dilation");
            }
            else{
                currState = Component.text("Current State: Knockback");
            }
            
            player.sendActionBar(currState);
            //Manipulating the time displayed on rewind clock
            timeStepSize = getTimeStep(timeTick, 1);
            player.setPlayerTime(player.getPlayerTimeOffset() + timeStepSize, false);
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

            //Rewind Clock implementation
            if(mat == Material.CLOCK && player.getCooldown(Material.CLOCK) == 0){
                int currTick = player.getTicksLived();
                int pastTick = currTick - (15 * 20);
                long timeTick = player.getPlayerTimeOffset();
                //Below while loop causes issues
                while(PREV_LOCS.get(pastTick) == null){
                    pastTick++;
                }
                Location pastLoc = PREV_LOCS.get(pastTick);
                rewindBuff(player, currTick);

                player.teleport(pastLoc);
                //Creating impression of "going back in time"
                //Algorithm first does a full day cycle rewind then applies correction based on the time that should've passed during the transition period
                BukkitTask runnable = new BukkitRunnable() {
                    int activeDuration = 2;
                    long timeStep = 24000 / 2;
                    long desiredTime = (player.getPlayerTimeOffset() - 24000 + getTimeStep(timeTick, 2)) % 24000;
                    public void run() {
                        if (activeDuration <= 0) {
                            player.setPlayerTime(desiredTime, false);
                            cancel();
                            REWIND_TASKS.remove(this);
                        }
                        else{
                            activeDuration--;
                            player.setPlayerTime((player.getPlayerTimeOffset() - timeStep) % 24000, false);
                        }
                    }  
                }.runTaskTimer(Main.getPlugin(), 0, 0);
                REWIND_TASKS.add(runnable);

                rewindBuff(player, currTick);
                player.setCooldown(Material.CLOCK, 15 * 20);
            }

            //Time Stasis implementation
            if(mat == Material.SHULKER_SHELL && player.getCooldown(Material.SHULKER_SHELL) == 0){
                //true duration: 0.5 * 20
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) 8 * 20, 1));
                player.setInvisible(true);
                ItemStack[] armor = player.getInventory().getArmorContents();
                player.getInventory().setArmorContents(null);
                BukkitTask runnable = new BukkitRunnable() {
                    //true duration: 0.5 * 20
                    int activeDuration = (int) 8 * 20;
                    public void run() {
                        if (activeDuration <= 0) {
                            cancel();
                            REWIND_TASKS.remove(this);
                            player.setInvisible(false);
                            player.getInventory().setArmorContents(armor);
                        }
                        else{
                            activeDuration--;
                            //Add particle effect which will show where the stasis player is
                        }
                    }  
                }.runTaskTimer(Main.getPlugin(), 0, 0);

                REWIND_TASKS.add(runnable);
                player.setCooldown(Material.SHULKER_SHELL, 10 * 20);
            }
        }

        //based on the current timeTick, find how big the timeStep is for the given tick and the given # of steps
        public long getTimeStep (long timeTick, int numSteps){
            long currTick = timeTick;
            long elapsedTick;
            long sum = 0;
            for(int i = 0; i < numSteps; i++){
                elapsedTick = currTick % 24000;
                if(elapsedTick >= 0 && elapsedTick < 12000){
                    sum += 12000 / (5*20);
                    currTick += 12000 / (5*20);
                }
                if(elapsedTick >= 12000 && elapsedTick < 13800){
                    sum += 1800 / (5*20);
                    currTick += 1800 / (5*20);
                }
                if(elapsedTick >= 13800 && elapsedTick < 24000){
                    sum += 10200 / (5*20);
                    currTick += 10200 / (5*20);
                }
            }
            return sum;
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

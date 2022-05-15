package me.toomuchzelda.teamarenapaper.teamarena.kits;

import java.util.*;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.event.Event;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.LivingEntity;

public class KitRewind extends Kit{

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

        public final HashMap<Integer, Location> PREV_LOCS = new HashMap<>();

        //clean up
		public void unregisterAbility() {
			PREV_LOCS.clear();
		}

        public void giveAbility(Player player) {
			player.setCooldown(Material.CLOCK, 15*20);
            player.setPlayerTime(6000, false);
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
            long timeTick = player.getPlayerTimeOffset() % 24000;
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
            //Time tick display calculation 
                if(timeTick >= 6000 && timeTick < 6100 - 1){
                    timeStepSize = 1;
                }
                if(timeTick == 6099){
                    timeStepSize = 12000 - 6100 + 1;
                }
                if(timeTick >= 12000 && timeTick < 13800 - 18){
                    timeStepSize = 18;
                }
                if(timeTick == 13782){
                    timeStepSize = 18000 - 13800 + 18;
                }
                if(timeTick >= 18000 && timeTick < 18100 - 1){
                    timeStepSize = 1;
                }
                if(timeTick == 18099){
                    timeStepSize = 11900 + 1;
                }
            //Manipulating the time displayed on rewind clock
            player.setPlayerTime((player.getPlayerTimeOffset() + timeStepSize) % 24000, false);
        }

        //Cancels damage that is received while in stasis
        public void onAttemptedDamage(DamageEvent event) {
            Player player = event.getPlayerVictim();
            //Change 8 * 20 to 0.5 * 20 once done testing
            if(player.getCooldown(Material.SHULKER_SHELL) >= (10 * 20 - (8 * 20))){
                event.setCancelled(true);
            }
        }

        //Cancels attacks that are attempted while in stasis
        public void onAttemptedAttack(DamageEvent event) {
            Player player = (Player) event.getAttacker();
            //Change 8 * 20 to 0.5 * 20 once done testing
            if(player.getCooldown(Material.SHULKER_SHELL) >= (10 * 20 - (8 * 20))){
                event.setCancelled(true);
            }
        }

        //Possibly prevent movement while in stasis? 
        /*
        public void onMove(PlayerMoveEvent event){
            Player player = event.getPlayer();
            if(player.hasPotionEffect(PotionEffectType.INVISIBILITY)){
                event.setCancelled(true);
            }
        }
        */

        public void onInteract(PlayerInteractEvent event) {
            Material mat = event.getMaterial();
            Player player = event.getPlayer();
            World world = player.getWorld();

            //Rewind Clock implementation
            if(mat == Material.CLOCK && player.getCooldown(Material.CLOCK) == 0){
                int currTick = player.getTicksLived();
                int pastTick = currTick - (15 * 20);
                Location pastLoc = null;
                //Below while loop causes issues
                while(!PREV_LOCS.isEmpty() && pastLoc != null && pastTick >= 0){
                    pastLoc = PREV_LOCS.remove(pastTick);
                    pastTick--;
                }

                if(pastLoc != null){
                    //Success
                    rewindBuff(player, currTick);
                    player.teleport(pastLoc);
                    rewindBuff(player, currTick);
                    player.setCooldown(Material.CLOCK, 15 * 20);
                    PREV_LOCS.clear();
                }
                else{
                    //Failure
                    Component warning = Component.text("The past seems unsafe");
                    player.sendMessage(warning);
                    System.out.print(pastTick);
                }
                
                //Creating impression of "going back in time"
                //Algorithm first does a full day cycle rewind then applies correction based on the time that should've passed during the transition period
                /*
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
                */
            }

            //Time Stasis implementation
            if(mat == Material.SHULKER_SHELL && player.getCooldown(Material.SHULKER_SHELL) == 0){
                //true duration: 0.5 * 20
                TeamArenaTeam team = Main.getPlayerInfo(player).team;
                ItemStack[] armor = player.getInventory().getArmorContents();
                HashMap swordSlots = player.getInventory().all(Material.IRON_SWORD);
                ItemStack barrier = new ItemStack(Material.BARRIER);
                ItemMeta barrierMeta = barrier.getItemMeta();
                Component barrierName = ItemUtils.noItalics(Component.text("Melee Disabled")).color(TextUtils.ERROR_RED);
                barrierMeta.displayName(barrierName);
                barrier.setItemMeta(barrierMeta);

                Iterator<Map.Entry<Integer, ItemStack>> iter = swordSlots.entrySet().iterator();
                player.getInventory().setArmorContents(null);
                while(iter.hasNext()){
                    Map.Entry<Integer, ItemStack> entry = iter.next();
                    int slot = entry.getKey();
                    player.getInventory().setItem(slot, barrier);
                }
                
                player.setInvisible(true);
                BukkitTask runnable = new BukkitRunnable() {
                    //true duration: 0.5 * 20
                    int activeDuration = (int) 8 * 20;
                    public void run() {
                        if (activeDuration <= 0) {
                            cancel();
                            REWIND_TASKS.remove(this);
                            player.setInvisible(false);
                            player.getInventory().setArmorContents(armor);
                            //Replacing the barriers with swords again
                            Iterator<Map.Entry<Integer, ItemStack>>iter = swordSlots.entrySet().iterator();
                                while(iter.hasNext()){
                                    Map.Entry<Integer, ItemStack> entry = iter.next();
                                    int slot = entry.getKey();
                                    player.getInventory().setItem(slot, new ItemStack(Material.IRON_SWORD));
                                }
                        }
                        else{
                            activeDuration--;
                            //Add particle effect which will show where the stasis player is
                            world.spawnParticle(Particle.REDSTONE, player.getLocation(), 4, Math.cos((Math.random() - 0.5) * 4 * Math.PI), 0, Math.sin((Math.random() - 0.5) * 4 * Math.PI), 8, new Particle.DustOptions(team.getColour(), 3));
                        }
                    }  
                }.runTaskTimer(Main.getPlugin(), 0, 0);

                REWIND_TASKS.add(runnable);
                player.setCooldown(Material.SHULKER_SHELL, 10 * 20);
            }

            //Prevents players from placing barriers
            if(mat == Material.BARRIER && player.isInvisible()){
                event.setCancelled(true);
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
                player.getWorld().createExplosion(player, 0.1f, false, false);
            }
        }
    }
}

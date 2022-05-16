package me.toomuchzelda.teamarenapaper.teamarena.kits;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.jetbrains.annotations.NotNull;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

//Kit Description:
/*
	Main Ability: Rewind
        CD: 15 seconds
            Every 15 seconds, this kit can travel to its previous location 15 seconds ago, with an extra buff depending on
            a 15 second cycle with 3 equivalent parts (each last for 5 seconds).
            They are denoted by the current time of day:
                Day: Regeneration
                Sunset: Time Dilation (AoE Slow)
                Night: Knockback (KB Explosion w/ minor damage)
	Sub Ability: Stasis
		CD: 12 sec
        Active Duration: 0.7 sec
            Provides Rewind with temporary invulnerability, but it is unable to attack during this time
*/

/**
 * @author onett425
 */

public class KitRewind extends Kit{

    public static final Set<BukkitTask> REWIND_TASKS = new HashSet<>();
    public static final int STASIS_DURATION = 14;

    public KitRewind(){
        super("Rewind", "Travel 15 seconds back in time with your rewind clock. Depending on the time, you gain a different buff. Time stasis allows you to not take damage but you cannot deal damage.", Material.CLOCK);
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
        stasisMeta.displayName(ItemUtils.noItalics(Component.text("Time Stasis")));
        timeStasis.setItemMeta(stasisMeta);
        setItems(sword, clock, timeStasis);

        setAbilities(new RewindAbility());
    }

    public static class RewindAbility extends Ability{

        public final HashMap<Integer, Location> PREV_LOCS = new HashMap<>();
        int startingTick = 0;

        //clean up
		public void unregisterAbility() {
			PREV_LOCS.clear();

            Iterator<BukkitTask> iter = REWIND_TASKS.iterator();
			while(iter.hasNext()) {
				BukkitTask task = iter.next();
				task.cancel();
				iter.remove();
			}
		}

        public void giveAbility(Player player) {
			player.setCooldown(Material.CLOCK, 15*20);
            player.setPlayerTime(6000, false);
            PREV_LOCS.clear();
            startingTick = TeamArena.getGameTick();
		}

        public void removeAbility(Player player){
            player.resetPlayerTime();
            //Fixes the display of the clock in kit selection menu
            player.setCooldown(Material.CLOCK, 0);
            PREV_LOCS.clear();
        }

        public void onPlayerTick(Player player) {
            //Player tick is used to determine cooldowns + abilities
            //Time tick is purely aesthetic
            Location loc = player.getLocation();
            Block currBlock = loc.getBlock().getRelative(BlockFace.DOWN);
            int currTick = TeamArena.getGameTick();
            int elapsedTick = (currTick - startingTick) % (15 * 20);
            long timeTick = player.getPlayerTimeOffset() % 24000;
            //Since each time period (day, sunset, night) has a different time frame, time step varies so each cycle lasts 5 seconds
            long timeStepSize = 0;

            //Checking that the current location is a valid rewind location, if it is, add it to possible rewind locations.
            if(player.isFlying() || player.isGliding() || (!currBlock.isEmpty() && currBlock.getType() != Material.LAVA)){
              PREV_LOCS.put(currTick, player.getLocation());  
            }

            //Displaying the current cycle in the action bar, providing distinct sound cues for people who don't use action bar
            Component currState = Component.text("sample text");;
            if(elapsedTick >= 0 && elapsedTick < 5*20){
                currState = Component.text("Current State: Regeneration").color(TextColor.color(245, 204, 91));
            }
            else if(elapsedTick < 10*20){
                currState = Component.text("Current State: Time Dilation").color(TextColor.color(237, 132, 83));
            }
            else{
                currState = Component.text("Current State: Knockback").color(TextColor.color(123, 101, 235));
            }
            player.sendActionBar(currState);

            //Time tick display calculation, with sounds to denote transition b/w states
                if(timeTick >= 6000 && timeTick < 6100 - 1){
                    timeStepSize = 1;
                }
                if(timeTick == 6099){
                    timeStepSize = 12000 - 6100 + 1;
                    player.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.MASTER, 1.0f, 1.0f);
                }
                if(timeTick >= 12000 && timeTick < 13800 - 18){
                    timeStepSize = 18;
                }
                if(timeTick == 13782){
                    timeStepSize = 18000 - 13800 + 18;
                    player.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.MASTER, 1.0f, 1.0f);
                }
                if(timeTick >= 18000 && timeTick < 18100 - 1){
                    timeStepSize = 1;
                }
                if(timeTick == 18099){
                    timeStepSize = 11900 + 1;
                    player.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            //Manipulating the time displayed on rewind clock
            player.setPlayerTime((player.getPlayerTimeOffset() + timeStepSize) % 24000, false);
        }

        //Cancels damage that is received while in stasis
        public void onAttemptedDamage(DamageEvent event) {
            Player player = event.getPlayerVictim();
            if(player.getCooldown(Material.SHULKER_SHELL) >= (12 * 20 - STASIS_DURATION)){
                event.setCancelled(true);
            }
        }

        //Cancels attacks that are attempted while in stasis
        public void onAttemptedAttack(DamageEvent event) {
            Player player = (Player) event.getAttacker();
            if(player.getCooldown(Material.SHULKER_SHELL) >= (12 * 20 - STASIS_DURATION)){
                event.setCancelled(true);
            }
        }

        public void onInteract(PlayerInteractEvent event) {
            Material mat = event.getMaterial();
            Player player = event.getPlayer();
            World world = player.getWorld();
            

            //Rewind Clock implementation
            if(mat == Material.CLOCK && player.getCooldown(Material.CLOCK) == 0){
                //No Rewinding w/ Flag
                if(Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player)) {
					Component cannotUseAbilityMsg = Component.text("You can't use Time Machine while holding the flag!").color(TextColor.color(255, 98, 20));
					player.sendMessage(cannotUseAbilityMsg);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 2, 0.5f);
				}
                else{
                    int currTick = TeamArena.getGameTick();
                    int pastTick = currTick - (15 * 20);
                    Location pastLoc = PREV_LOCS.get(pastTick);
                        while(pastLoc == null && pastTick < currTick){
                            pastLoc = PREV_LOCS.get(pastTick);
                            pastTick++;
                        }

                    if(pastLoc != null){
                        //Past Location succesfully found
                        //Apply buff at departure AND arrival location
                        rewindBuff(player, currTick);
                        player.teleport(pastLoc);
                        world.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT , 1f, 1.5f);
                        rewindBuff(player, currTick);
                        player.setCooldown(Material.CLOCK, 15 * 20);
                        PREV_LOCS.clear();
                    }
                    else{
                        //Failure
                        Component warning = Component.text("The past seems uncertain...");
                        player.sendMessage(warning);
                        player.setCooldown(Material.CLOCK, (int)0.5 * 20);
                    }
                }
            }

            //Time Stasis implementation
            if(mat == Material.SHULKER_SHELL && player.getCooldown(Material.SHULKER_SHELL) == 0){
                //No Time Stasis w/ Flag
                if(Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player)) {
					Component cannotUseAbilityMsg = Component.text("You can't use Time Stasis while holding the flag!").color(TextColor.color(255, 98, 20));
					player.sendMessage(cannotUseAbilityMsg);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 2, 0.5f);
				}
                else{
                    //"Glitching" aesthetic effect + particles
                    TeamArenaTeam team = Main.getPlayerInfo(player).team;
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    HashMap swordSlots = player.getInventory().all(Material.IRON_SWORD);
                    ItemStack barrier = new ItemStack(Material.BARRIER);
                    ItemMeta barrierMeta = barrier.getItemMeta();
                    Component barrierName = ItemUtils.noItalics(Component.text("Item Disabled")).color(TextUtils.ERROR_RED);
                    barrierMeta.displayName(barrierName);
                    barrier.setItemMeta(barrierMeta);

                    //Replacing all swords with barriers 
                    Iterator<Map.Entry<Integer, ItemStack>> iter = swordSlots.entrySet().iterator();
                    while(iter.hasNext()){
                        Map.Entry<Integer, ItemStack> entry = iter.next();
                        int slot = entry.getKey();
                        player.getInventory().setItem(slot, barrier);
                    }

                    player.setInvisible(true);
                    player.getInventory().setArmorContents(null);
                    world.playSound(player, Sound.BLOCK_BELL_RESONATE, 1f, 1.8f);

                    BukkitTask runnable = new BukkitRunnable() {
                        int activeDuration = STASIS_DURATION;
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
                                world.spawnParticle(Particle.REDSTONE, player.getLocation(), 8, Math.cos((Math.random() - 0.5) * Math.PI / 2), Math.random(), Math.sin((Math.random() - 0.5) * Math.PI / 2), 16, new Particle.DustOptions(team.getColour(), 2));
                                if(activeDuration % 2 == 0){
                                    player.setInvisible(true);
                                    player.getInventory().setArmorContents(null);
                                }
                                else{
                                    player.setInvisible(false);
                                    player.getInventory().setArmorContents(armor);
                                }
                            }
                        }  
                    }.runTaskTimer(Main.getPlugin(), 0, 0);

                    REWIND_TASKS.add(runnable);
                    player.setCooldown(Material.SHULKER_SHELL, 12 * 20);
                }
            }

            //Prevents players from placing barriers
            if(mat == Material.BARRIER && player.getCooldown(Material.SHULKER_SHELL) >= (12 * 20 - STASIS_DURATION)){
                event.setCancelled(true);
            }
        }

        //When rewinding, a buff is given based on a 15 second cycle with 3 sections, each with a 5 second timeframe
        public void rewindBuff(Player player, int currTick){
            //Returns how far the currTick is in the cycle
            //[0, 299]
            int elapsedTick = (currTick - startingTick) % (15 * 20);
            if(elapsedTick >= 0 && elapsedTick < 5*20){
                //Regen 2 for 7.5 seconds => 3 hearts healed
                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 150, 1));
            }
            else if(elapsedTick < 10*20){
                //Time Dilation: Gives nearby enemies Slow 3 + No Jump for 3 seconds
                List<Entity> affectedEnemies = player.getNearbyEntities(8, 8, 8);
                for(Entity entity : affectedEnemies){
                    if(entity instanceof org.bukkit.entity.LivingEntity victim && !(entity.getType().equals(EntityType.ARMOR_STAND))){
                        //change to 3*20 tick duration, extended for testing purposes
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 3*20, 250, true));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 3*20, 2, true));
                    }
                }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.AMBIENT, 0.5f, 1f);
            }
            else{
                //Knockback: Minor Damage and KB in an AoE
                player.getWorld().createExplosion(player, 1f, false, false);

                //KB Amp
                List<Entity> affectedEnemies = player.getNearbyEntities(3, 3, 3);
                for(Entity entity : affectedEnemies){
                    if(entity instanceof org.bukkit.entity.LivingEntity victim && !(entity.getType().equals(EntityType.ARMOR_STAND))){
                        org.bukkit.util.@NotNull Vector vel = victim.getVelocity();
                        if(vel.getY() < 0){
                            vel.setY(0);
                        }
                        victim.setVelocity(vel.multiply(10));
                    }
                }

                player.stopSound(Sound.ENTITY_GENERIC_EXPLODE);
                player.getWorld().stopSound(SoundStop.named(Sound.ENTITY_GENERIC_EXPLODE));
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.4f, 1.3f);
            }
        }
    }
}

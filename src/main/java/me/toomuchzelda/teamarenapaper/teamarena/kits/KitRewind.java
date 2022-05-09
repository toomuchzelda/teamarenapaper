package me.toomuchzelda.teamarenapaper.teamarena.kits;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;

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
        ItemStack timeDilate = new ItemStack(Material.HONEYCOMB);
        ItemMeta dilateMeta = timeDilate.getItemMeta();
        dilateMeta.displayName(ItemUtils.noItalics(Component.text("Time Dilater")));
        setItems(sword, clock, timeDilate);

        setAbilities(new RewindAbility());

    }

    public static class RewindAbility extends Ability{

        //clean up
		public void unregisterAbility() {
			PREV_LOCS.clear();
		}

        public void onPlayerTick(Player player) {
            Location loc = player.getLocation();
            if(!loc.getBlock().isEmpty()){
              PREV_LOCS.put(player.getTicksLived(), player.getLocation());  
            } 
        }

        public void onInteract(PlayerInteractEvent event) {
            Material mat = event.getMaterial();
            Player player = event.getPlayer();
            
            if(mat == Material.CLOCK && player.getCooldown(Material.CLOCK) == 0){
                int currTick = player.getTicksLived();
                int pastTick = currTick - (15 * 20);
                while(PREV_LOCS.get(pastTick) == null){
                    pastTick++;
                }
                Location pastLoc = PREV_LOCS.get(pastTick);
                player.teleport(pastLoc);
                player.setCooldown(Material.CLOCK, 15 * 20);
            }
        }
    }
}

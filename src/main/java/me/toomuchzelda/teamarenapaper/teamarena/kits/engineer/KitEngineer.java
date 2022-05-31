package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import java.util.*;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import it.unimi.dsi.fastutil.Hash;

import org.bukkit.Location;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

//Kit Description: 
/*
    Kit Goal: Utility/Ranged
    Defensive + Offensive play should be equally viable 

    WRENCH

    Main Ability: Sentry
        Active Time / CD = 20 seconds
        Similar to TF2 Sentry, visible range is restricted to a cone of view
        Angle at which sentry is placed is determined during placement
        Sentry can be picked up and repositioned, however there is a set-up time.
        Turret will slowly rotate back and forth within its range.
        Turret will target the closest enemy within its sight

        Upon placement, the turret will self destruct after 20 seconds.
        
        Turret Stats:
            -Visible Angle: 90 Degrees (Able to turn 360 degrees to track a locked-on enemy)
            -Health: 20 Hearts + Full Leather Armor
            -Fire-Rate: 
            -DMG: 
        

    Sub Ability: Teleporter
        RWF Tele, but no more remote teleporter
        Add set-up time and cooldown to building TPs 
*/
/**
 * @author onett425
 */
public class KitEngineer extends Kit{

    public static final ItemStack SENTRY;
    public static final ItemStack WRANGLER;
    public static final ItemStack ACTIVE_WRANGLER;
    
    static{
        SENTRY = new ItemStack(Material.CHEST_MINECART);
        ItemMeta sentryMeta = SENTRY.getItemMeta();
        sentryMeta.displayName(ItemUtils.noItalics(Component.text("Sentry Kit")));
        SENTRY.setItemMeta(sentryMeta);

        WRANGLER = new ItemStack(Material.STICK);
        ItemMeta wranglerMeta = WRANGLER.getItemMeta();
        wranglerMeta.displayName(ItemUtils.noItalics(Component.text("Manual Sentry Fire")));
        WRANGLER.setItemMeta(wranglerMeta);

        ACTIVE_WRANGLER = new ItemStack(Material.BLAZE_ROD);
        ItemMeta activeWranglerMeta = ACTIVE_WRANGLER.getItemMeta();
        activeWranglerMeta.displayName(ItemUtils.noItalics(Component.text("Manual Sentry Fire")));
        ACTIVE_WRANGLER.setItemMeta(activeWranglerMeta);
    }

    public KitEngineer(){
        super("Engineer", "Wrench and Turret and Teleporter", Material.IRON_SHOVEL);

        ItemStack pants = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta pantsMeta = (LeatherArmorMeta) pants.getItemMeta();
        pantsMeta.setColor(Color.WHITE);
        pants.setItemMeta(pantsMeta);
        setArmor(new ItemStack(Material.GOLDEN_HELMET), new ItemStack(Material.IRON_CHESTPLATE), 
                pants, new ItemStack(Material.GOLDEN_BOOTS));

        ItemStack wrench = new ItemStack(Material.IRON_SHOVEL);
        ItemMeta wrenchMeta = wrench.getItemMeta();
        wrenchMeta.displayName(ItemUtils.noItalics(Component.text("Wrench")));
        wrench.setItemMeta(wrenchMeta);

        ItemStack tele = new ItemStack(Material.QUARTZ);
        ItemMeta teleMeta = tele.getItemMeta();
        teleMeta.displayName(ItemUtils.noItalics(Component.text("Create Teleporter")));
        tele.setItemMeta(teleMeta);

        ItemStack pda = new ItemStack(Material.BOOK);
        ItemMeta pdaMeta = pda.getItemMeta();
        pdaMeta.displayName(ItemUtils.noItalics(Component.text("Destruction PDA")));
        pda.setItemMeta(pdaMeta);

        setItems(wrench, SENTRY, tele, pda);
        setAbilities(new EngineerAbility());
    }

    public static class EngineerAbility extends Ability {

        public static final HashMap<Player, List<Building>> ACTIVE_BUILDINGS = new HashMap<>();
        public static final HashMap<Player, List<Teleporter>> ACTIVE_TELEPORTERS = new HashMap<>();

        public static final int TP_CD = 60;


        public void giveAbility(Player player) {
            if(!ACTIVE_TELEPORTERS.containsKey(player)){
                List<Teleporter> teleporters = new LinkedList<>();
                ACTIVE_TELEPORTERS.put(player, teleporters);
            }
            if(!ACTIVE_BUILDINGS.containsKey(player)){
                List<Building> buildings = new LinkedList<>();
                ACTIVE_BUILDINGS.put(player, buildings);
            }
        }

        @Override
        public void onInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            Material mat = event.getMaterial();
            Block block = event.getClickedBlock();
            BlockFace blockFace = event.getBlockFace();
            Location tpLoc = block.getLocation();

            if(mat == Material.QUARTZ && block != null && block.isSolid() && 
            blockFace == BlockFace.UP && tpLoc.add(0, 1, 0).getBlock().getType() == Material.AIR){
                List<Teleporter> teleporters = ACTIVE_TELEPORTERS.get(player);
                List<Building> buildings = ACTIVE_BUILDINGS.get(player);
                //Breaking TP
                if(findDuplicate(teleporters, tpLoc) != null){
                    Teleporter dupeTele = findDuplicate(teleporters, tpLoc);
                    dupeTele.destroy();
                    teleporters.remove(dupeTele);
                    buildings.remove(dupeTele);
                }
                else{
                //Creating TP
                    if(teleporters.size() >= 2){
                        //Failure: 2 TPs already exist
                        player.sendMessage(Component.text("Two teleporters are already active! Destroy them with your Destruction PDA!"));
                    }
                    else if(teleporters.size() == 1){
                        //Success: 2nd TP is created
                        int lastUsedTick = teleporters.get(0).getLastUsedTick();
                        Teleporter newTP = new Teleporter(player, tpLoc);
                        newTP.setLastUsedTick(lastUsedTick);
                        teleporters.add(newTP);
                        buildings.add(newTP);
                    }
                    else{
                        //Success: 1st TP is created
                        Teleporter newTP = new Teleporter(player, tpLoc);
                        teleporters.add(newTP);
                        buildings.add(newTP);
                    }
                }
            }
        }

        public Teleporter findDuplicate(List<Teleporter> teleporters, Location loc){
            Teleporter dupTeleporter = null;
            for(Teleporter tele: teleporters){
                if(tele.getLoc().equals(loc)){
                   dupTeleporter = tele;
                }
            }
            return dupTeleporter;
        }

        @Override
        public void onPlayerTick(Player player) {
            List<Teleporter> teleporters = ACTIVE_TELEPORTERS.get(player);
            Component holoText;
            //2 Teleporters exist, so teleporter is active
            //Handling Teleporting teammates
            if(teleporters.size() == 2){
                for(Teleporter teleporter : teleporters){
                    Location teleLoc = teleporter.getLoc();
                    Collection<Player> players = teleporter.getLoc().getNearbyPlayers(0.5);
                    int currTick = TeamArena.getGameTick();
                    int lastUsedTick = teleporter.getLastUsedTick();
                    
                    players.forEach(p -> {
                        if(!Main.getGame().canAttack(player, p) && p.isSneaking() 
                        && p.getLocation().getY() == teleLoc.getY() &&
                        currTick - lastUsedTick > TP_CD){
                            useTeleporter(p, teleLoc);
                        }
                    }
                    );
                }
            }

            //Display of TP Hologram Text
                if(teleporters.size() == 1){
                    holoText = Component.text("Not Connected");
                    teleporters.get(0).setText(holoText);
                }
                else if(teleporters.size() == 2){
                    int currTick = TeamArena.getGameTick();
                    int lastUsedTick = teleporters.get(0).getLastUsedTick();
                    if(currTick - lastUsedTick > TP_CD){
                        holoText = Component.text("Teleport Ready");
                    }
                    else{
                        long percCD = 100 * Math.round((double)(currTick - lastUsedTick) / TP_CD);
                        holoText = Component.text("Recharging... " + percCD + "%");
                    }
                    teleporters.forEach((teleporter) -> {
                        teleporter.setText(holoText);
                    });
                }
        }

        //Assume there are 2 active TPs
        public void useTeleporter(Player player, Location currLoc){
            List<Teleporter> teleporters = ACTIVE_TELEPORTERS.get(player);
            Location destination = teleporters.get(0).getLoc();
            if(destination.equals(currLoc)){
                destination = teleporters.get(1).getLoc();
            }
            player.teleport(destination);
            teleporters.forEach((teleporter) -> {
                teleporter.setLastUsedTick(TeamArena.getGameTick());
            });
        }
    }
}
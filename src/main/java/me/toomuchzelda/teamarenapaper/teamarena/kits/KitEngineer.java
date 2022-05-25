package me.toomuchzelda.teamarenapaper.teamarena.kits;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;

//Kit Description: 
/*
    Main Ability: Sentry

    Sub Ability: Teleporter
*/
/**
 * @author onett425
 */
public class KitEngineer extends Kit{

    public KitEngineer(){
        super("Engineer", "Turret", Material.HONEYCOMB);

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

        ItemStack builder = new ItemStack(Material.HONEYCOMB);
        ItemMeta builderMeta = builder.getItemMeta();
        builderMeta.displayName(ItemUtils.noItalics(Component.text("Remote Control")));
        builder.setItemMeta(builderMeta);

        setItems(wrench, builder);

    }

    public static class EngineerAbility extends Ability {

    }
}
package me.toomuchzelda.teamarenapaper.teamarena.kits;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitTrooper extends Kit {

    public KitTrooper() {
        super("Trooper", "Your standard issue melee fighter, it can handle most 1-on-1 sword fights " +
                "and can do a little self-healing", Material.IRON_SWORD);

        ItemStack[] armour = new ItemStack[4];
        armour[3] = new ItemStack(Material.IRON_HELMET);
        armour[2] = new ItemStack(Material.IRON_CHESTPLATE);
        armour[1] = new ItemStack(Material.IRON_LEGGINGS);
        armour[0] = new ItemStack(Material.IRON_BOOTS);
        this.setArmour(armour);

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, false);
        sword.setItemMeta(swordMeta);

        ItemStack gapples = new ItemStack(Material.GOLDEN_APPLE);
        gapples.setAmount(5);

        setItems(sword, gapples);
    }
}

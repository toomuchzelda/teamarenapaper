package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitArcher extends Kit {
    public KitArcher(TeamArena tm) {
        super("Archer", "shoot bow arrows", Material.ARROW, tm);

        ItemStack[] armour = new ItemStack[4];
        armour[3] = new ItemStack(Material.LEATHER_HELMET);
        armour[2] = new ItemStack(Material.IRON_CHESTPLATE);
        armour[1] = new ItemStack(Material.IRON_LEGGINGS);
        armour[0] = new ItemStack(Material.CHAINMAIL_BOOTS);
        this.setArmour(armour);

        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.ARROW_DAMAGE, 2, false);
        bowMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, false);
        bow.setItemMeta(bowMeta);

        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);

        ItemStack[] items = new ItemStack[]{sword, bow, new ItemStack(Material.ARROW)};
        setItems(items);
    }
}

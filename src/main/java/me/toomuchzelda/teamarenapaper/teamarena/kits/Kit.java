package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Kit
{
    private final String name;
    private final String description;
    private final Material icon;
    //0: boots, 1: leggings, 2: chestplate, 3: helmet
    private ItemStack[] armour;
    private ItemStack[] items;
    private Ability[] abilities;

    private Set<Player> users;

    public Kit(String name, String description, Material icon) {
        this.name = name;
        this.description = description;
        this.icon = icon;

        //these are set via the setter methods
        ItemStack[] armour = new ItemStack[4];
        Arrays.fill(armour, new ItemStack(Material.AIR));
        this.armour = armour;

        this.items = new ItemStack[0];
        this.abilities = new Ability[0];

        users = ConcurrentHashMap.newKeySet();
    }

    //clearInventory and updateInventory happens outside the following two methods
    //give kit and it's abilities to player
    public void giveKit(Player player, boolean update) {
        users.add(player);

        PlayerInventory inventory = player.getInventory();
        inventory.setArmorContents(armour);

        //only give items if there are items
        if(items.length > 0) {
            //fill up from empty slots only
            int itemsIdx = 0;
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, items[itemsIdx]);
                    itemsIdx++;
                    if (itemsIdx == items.length)
                        break;
                }
            }
        }

        for(Ability ability : abilities) {
            ability.giveAbility(player);
        }

        if(update)
            player.updateInventory();
    }

    //remove abilities from player
    public void removeKit(Player player) {
        users.remove(player);

        for(Ability a : abilities) {
            a.removeAbility(player);
        }
    }

    public void setArmour(ItemStack[] armour) {
        this.armour = armour;
    }

    public void setItems(ItemStack[] items) {
        this.items = items;
    }

    public void setAbilities(Ability... abilities) {
        this.abilities = abilities;
    }

    public Material getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public Set<Player> getUsers() {
        return users;
    }

    public Ability[] getAbilities() {
        return abilities;
    }

}

package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Kit
{
    private String name;
    private String description;
    private Material icon;
    private ItemStack[] armour;
    private ItemStack[] items;
    private Ability[] abilities;

    public Set<Player> users;

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

}

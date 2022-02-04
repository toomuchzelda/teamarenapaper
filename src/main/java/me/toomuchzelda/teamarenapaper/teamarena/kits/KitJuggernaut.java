package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KitJuggernaut extends Kit {
    public KitJuggernaut(TeamArena teamArena) {
        super("Juggernaut", "The most beloved kit", Material.DIAMOND_CHESTPLATE, teamArena);
        setArmor(new ItemStack(Material.DIAMOND_HELMET),
                new ItemStack(Material.DIAMOND_CHESTPLATE),
                new ItemStack(Material.DIAMOND_LEGGINGS),
                new ItemStack(Material.DIAMOND_BOOTS));
        setItems(new ItemStack(Material.STONE_SWORD));
        setAbilities(new JuggernautAbility());
    }

    private static class JuggernautAbility extends Ability {

        @Override
        public void giveAbility(Player player) {
            player.setFoodLevel(6);
        }

        @Override
        public void removeAbility(Player player) {
            player.setFoodLevel(20);
        }

        @Override
        public void onTick(Player player) {
            player.setFoodLevel(6);
        }
    }
}

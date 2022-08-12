package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitJuggernaut extends Kit {
    public KitJuggernaut() {
        super("Juggernaut", "The most beloved kit", Material.DIAMOND_CHESTPLATE);
        setArmor(new ItemStack(Material.NETHERITE_HELMET),
                new ItemStack(Material.NETHERITE_CHESTPLATE),
                new ItemStack(Material.NETHERITE_LEGGINGS),
                new ItemStack(Material.NETHERITE_BOOTS));

        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
		swordMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
        sword.setItemMeta(swordMeta);

        setItems(sword);
        setAbilities(new JuggernautAbility());

		setCategory(KitCategory.FIGHTER);
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
        public void onPlayerTick(Player player) {
            player.setFoodLevel(6);
        }
    }
}

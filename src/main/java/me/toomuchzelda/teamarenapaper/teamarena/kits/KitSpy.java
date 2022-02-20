package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KitSpy extends Kit
{
	
	public KitSpy() {
		super("Spy", "sus", Material.SPYGLASS);
		
		setArmor(new ItemStack(Material.IRON_HELMET), new ItemStack(Material.IRON_CHESTPLATE),
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));
		
		setItems(new ItemStack(Material.IRON_SWORD));
		
		
		setAbilities(new SpyAbility());
	}
	
	public static class SpyAbility extends Ability
	{
		@Override
		public void giveAbility(Player player) {
		
		}
	}
}

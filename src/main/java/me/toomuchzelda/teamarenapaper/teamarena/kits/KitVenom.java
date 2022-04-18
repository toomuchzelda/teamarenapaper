package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.CustomEnchants;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.lang.reflect.Field;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageTimes;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class KitVenom extends Kit
{
	
	public KitVenom(TeamArena game) {
		super("Venom", "Poison dmg on hit, poisoned people cannot heal. Ability2", Material.POTION);
		this.setArmor(new ItemStack(Material.CHAINMAIL_HELMET), new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));
		
		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.displayName(Component.text("Poison Sword"));
		swordMeta.addEnchant(CustomEnchants.POISON, 1, true);
		sword.setItemMeta(swordMeta);
		ItemStack leap = new ItemStack(Material.IRON_SWORD);
		ItemMeta leapMeta = leap.getItemMeta();
		leapMeta.displayName(Component.text("Toxic Leap"));
		leap.setItemMeta(leapMeta);

		setItems(sword, leap);
		setAbilities(new VenomAbility());
	}
	
	public static class VenomAbility extends Ability
	{
		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(event.getMaterial() == Material.CHICKEN && !event.getPlayer().hasCooldown(Material.CHICKEN)){
				event.getPlayer().setCooldown(Material.CHICKEN, 12*20);
				Vector direction = event.getPlayer().getFacing().getDirection();
				Vector multiplier = new Vector(1.0, 0.5, 1.0);
				multiplier.multiply(1.5);
				Vector launch = multiplier.multiply(direction);
				event.getPlayer().setVelocity(event.getPlayer().getVelocity().add(launch));

				/*Bukkit.getScheduler().runTaskLater(Main.getPlugin(), new Runnable(){					
					public void run(){
					isLeapActive = false;
					}
				}, 10);
				*/			
			}
		}
	}
	
}

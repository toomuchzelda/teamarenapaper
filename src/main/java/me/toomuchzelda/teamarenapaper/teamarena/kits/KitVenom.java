package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.CustomEnchants;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageTimes;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minecraft.world.item.enchantment.ThornsEnchantment;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class KitVenom extends Kit
{
	
	public KitVenom() {
		super("Venom", "Poison dmg on hit, poisoned people cannot heal. It can also quickly jump in, afflicting all enemies it hits with poison and decreases cooldown with each enemy hit!", 
				Material.POTION);
		this.setArmor(new ItemStack(Material.CHAINMAIL_HELMET), new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));
		
		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.displayName(Component.text("Poison Sword"));
		swordMeta.addEnchant(Enchantment.THORNS, 1, true);
		List<Component> lore = new ArrayList<Component>();
		TextComponent message = Component.text("Poison I", TextColor.color(170, 170, 170));
		lore.add(message);
		swordMeta.lore(lore);
		sword.setItemMeta(swordMeta);

		ItemStack leap = new ItemStack(Material.CHICKEN);
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
				//event.getPlayer().setCooldown(Material.CHICKEN, 12*20);
				//^Removed for testing purposes
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
		@Override
		public void onAttemptedAttack(DamageEvent event) {
			Player player = (Player) event.getAttacker();
			if(player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD){
				if(event.getDamageType().isMelee() && event.getVictim() instanceof LivingEntity living) {
					LivingEntity victim = (LivingEntity) event.getVictim();
					int poisonDuration = 0;
						if(victim.hasPotionEffect(PotionEffectType.POISON)){
							poisonDuration = victim.getPotionEffect(PotionEffectType.POISON).getDuration();
						}
					if(poisonDuration <= 4 * 20){
						if(poisonDuration + 2 * 20 > 4 * 20){
							victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 4 * 20 - poisonDuration, 0));
						}
						else{
							victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 2 * 20 + poisonDuration, 0));
						}                    
					}
				}
			}
		}
	}
	
}

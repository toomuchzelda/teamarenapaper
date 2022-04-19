package me.toomuchzelda.teamarenapaper.teamarena.kits.abilities;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;

public class CustomEnchants{
		public static final Enchantment POISON = new EnchantmentWrapper("Poison");
		public static void register(){
			boolean registered = Arrays.stream(Enchantment.values()).collect(Collectors.toList()).contains(POISON);
			if(!registered){
				registerEnchantment(POISON);
			}
		}
		public static void registerEnchantment(Enchantment ench){
			boolean registered = true;
			try{
				Field f = Enchantment.class.getDeclaredField("acceptingNew");
				f.setAccessible(true);
				f.set(null,true);
				Enchantment.registerEnchantment(ench);
			} catch(Exception e){
				registered = false;
				e.printStackTrace();
			}
		}
	}
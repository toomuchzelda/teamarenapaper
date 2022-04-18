package me.toomuchzelda.teamarenapaper.teamarena.kits.abilities;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;

public class PoisonEnchant extends JavaPlugin implements Listener{
    public void onEnable(){
        CustomEnchants.register();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    //player represents the attacker
    public void onAttemptedAttack(DamageEvent event, Player player) {
        ItemStack poisonSword = new ItemStack(Material.IRON_SWORD);
        poisonSword.addEnchantment(CustomEnchants.POISON, 1);
        if(player.getInventory().getItemInMainHand().equals(poisonSword)){
            if(event.getDamageType().isMelee() && event.getVictim() instanceof LivingEntity living) {
                LivingEntity victim = (LivingEntity) event.getVictim();
                int poisonDuration = victim.getPotionEffect(PotionEffectType.POISON).getDuration();

                if(poisonDuration < 4 * 20){
                    if(poisonDuration + 2 * 20 > 4 * 20){
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 4 * 20 - poisonDuration, 1));
                    }
                    else{
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 2 * 20, 1));
                    }                    
                }
            }
        }
    }
    
    public void onDisable(){

    }


}
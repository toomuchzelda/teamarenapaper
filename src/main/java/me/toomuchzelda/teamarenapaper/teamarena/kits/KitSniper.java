package me.toomuchzelda.teamarenapaper.teamarena.kits;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * @author onett425
 */
public class KitSniper extends Kit {

    public KitSniper() {
        super("Sniper", "mlg", Material.SPYGLASS);

        ItemStack[] armour = new ItemStack[4];
        armour[3] = new ItemStack(Material.LEATHER_HELMET);
        armour[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
        armour[1] = new ItemStack(Material.LEATHER_LEGGINGS);
        armour[0] = new ItemStack(Material.LEATHER_BOOTS);
        this.setArmour(armour);

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemStack rifle = new ItemStack(Material.SPYGLASS);
        ItemMeta rifleMeta = rifle.getItemMeta();
        rifleMeta.displayName(ItemUtils.noItalics(Component.text("CheyTac Intervention")));
        rifle.setItemMeta(rifleMeta);
        ItemStack grenade = new ItemStack(Material.DRIED_KELP_BLOCK);
        ItemMeta grenadeMeta = grenade.getItemMeta();
        grenadeMeta.displayName(ItemUtils.noItalics(Component.text("Frag Grenade")));
        grenade.setItemMeta(grenadeMeta);

        setItems(sword, rifle, grenade);
        setAbilities(new SniperAbility());
    }
    
    public static class SniperAbility extends Ability{
        @Override
		public void giveAbility(Player player) {
			player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(KitNinja.NINJA_SPEED_MODIFIER);
            player.setExp(0.999f);
		}
		
		@Override
		public void removeAbility(Player player) {
			player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(KitNinja.NINJA_SPEED_MODIFIER);
            player.setExp(0);
		}

        @Override
        public void onInteract(PlayerInteractEvent event) {
           Material mat = event.getMaterial();
           Player player = event.getPlayer();
           Action action = event.getAction();
            //Sniper Rifle is fired
            if(mat == Material.SPYGLASS && player.getInventory().getItemInMainHand().getType() == Material.SPYGLASS && (action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK))){
                
                player.setCooldown(Material.SPYGLASS, (int) (1.5*20));
            }
            
            //Grenade Pull Pin
            if(mat == Material.DRIED_KELP_BLOCK){  
                Component text = Component.text("Grenade Explodes in: " + ).color(TextColor.color(242, 44, 44));
                player.setExp(0);
                player.getInventory().removeItemAnySlot(new ItemStack(Material.DRIED_KELP_BLOCK));
            }

            //Grenade Throw
        }

        @Override
        public void onPlayerTick(Player player) {
            //Grenade Cooldown
            float exp = player.getExp();
            if(exp < 0.999f){
                //10 second cooldown = 200 ticks
                //1/200 = 0.005
                if(exp + 0.005f >= 1){
                    exp = 0.999f;
                }
                else{
                    exp += 0.005f;
                }
            }

            float grenadeTimer = 2.000f;

        }
        
    }
}

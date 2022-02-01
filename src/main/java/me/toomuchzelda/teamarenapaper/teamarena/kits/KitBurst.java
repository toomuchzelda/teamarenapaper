package me.toomuchzelda.teamarenapaper.teamarena.kits;

import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

public class KitBurst extends Kit
{
	public KitBurst(TeamArena teamArena) {
		super("Burst", "firework shooty shooty", Material.FIREWORK_ROCKET, teamArena);
		
		ItemStack[] armour = new ItemStack[4];
		armour[3] = new ItemStack(Material.CHAINMAIL_HELMET);
		armour[2] = new ItemStack(Material.IRON_CHESTPLATE);
		armour[1] = new ItemStack(Material.CHAINMAIL_LEGGINGS);
		armour[0] = new ItemStack(Material.CHAINMAIL_BOOTS);
		this.setArmour(armour);
		
		ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
		
		ItemStack crossbow = new ItemStack(Material.CROSSBOW);
		
		ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET);
		
		ItemStack[] items = new ItemStack[]{sword, firework, crossbow};
		setItems(items);
		
		setAbilities(new BurstAbility());
	}
	
	public static class BurstAbility extends Ability
	{
		@Override
		public void onShootBow(EntityShootBowEvent event) {
			if(event.getProjectile() instanceof Firework firework && event.getEntity() instanceof Player p) {
				TeamArenaTeam team = Main.getPlayerInfo(p).team;
				
				FireworkMeta meta = firework.getFireworkMeta();
				meta.clearEffects();
				FireworkEffect effect = FireworkEffect.builder().trail(true).with(FireworkEffect.Type.BALL)
						.flicker(true).withColor(team.getColour()).build();
				
				meta.addEffect(effect);
				//meta.setPower(1);
				firework.setFireworkMeta(meta);
			}
		}
		
		@Override
		public void onLoadCrossbow(EntityLoadCrossbowEvent event) {
			event.setConsumeItem(false);
			((Player) event.getEntity()).updateInventory(); //do this to undo client prediction of using the firework
		}
		
		//stop them from accidentally placing the firework down and using it
		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(event.useItemInHand() != Event.Result.DENY && event.getMaterial() == Material.FIREWORK_ROCKET) {
				event.setUseItemInHand(Event.Result.DENY);
			}
		}
	}
}

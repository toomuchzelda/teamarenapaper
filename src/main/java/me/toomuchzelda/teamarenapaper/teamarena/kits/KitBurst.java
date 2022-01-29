package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

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
		public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
			if(event.getProjectile() instanceof Firework firework) {
				TeamArenaTeam team = Main.getPlayerInfo(event.getPlayer()).team;
				
				FireworkMeta meta = firework.getFireworkMeta();
				meta.clearEffects();
				boolean flicker = MathUtils.random.nextBoolean();
				FireworkEffect effect = FireworkEffect.builder().trail(true).with(FireworkEffect.Type.BURST)
						.flicker(flicker).withColor(team.getColour()).build();
				
				meta.addEffect(effect);
				//meta.setPower(1);
				firework.setFireworkMeta(meta);
				
				event.setShouldConsume(false);
			}
		}
	}
}

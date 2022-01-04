package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.core.ParticleUtils;
import me.toomuchzelda.teamarenapaper.core.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitGhost extends Kit
{
	public KitGhost(TeamArena tm) {
		super("Ghost", "Invisible, sneaky, has ender pearls, and sus O_O", Material.GHAST_TEAR, tm);
		
		//armor is already set to AIR in Kit.java
		
		ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
		sword.setItemMeta(swordMeta);
		
		ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
		
		setItems(new ItemStack[]{sword, new ItemStack(Material.AIR), pearls});
		
		setAbilities(new GhostAbility());
	}
	
	public static class GhostAbility extends Ability
	{
		@Override
		public void giveAbility(Player player) {
			PlayerUtils.setInvisible(player, true);
		}
		
		@Override
		public void removeAbility(Player player) {
			PlayerUtils.setInvisible(player, false);
		}
		
		@Override
		public void onReceiveDamage(DamageEvent event) {
			if(event.getDamageType().is(DamageType.PROJECTILE) && event.getAttacker() instanceof AbstractArrow aa
					&& aa.getPierceLevel() > 0) {
				//make arrows stick in the Ghost if it's a piercing projectile (normally doesn't)
				event.getPlayerVictim().setArrowsInBody(event.getPlayerVictim().getArrowsInBody() + 1);
			}
			
			//spawn le reveal particles
			Location baseLoc = event.getVictim().getLocation();
			double x = baseLoc.getX();
			double y = baseLoc.getY();
			double z = baseLoc.getZ();
			
			TeamArenaTeam team = Main.getPlayerInfo(event.getPlayerVictim()).team;
			
			for(int i = 0; i < 15; i++) {
				baseLoc.setX(x + MathUtils.randomRange(-0.3, 0.3));
				baseLoc.setY(y + MathUtils.randomRange(0, 1.4));
				baseLoc.setZ(z + MathUtils.randomRange(-0.3, 0.3));
				
				ParticleUtils.colouredRedstone(baseLoc, team.getColour(), 2, 1);
			}
		}
	}
}

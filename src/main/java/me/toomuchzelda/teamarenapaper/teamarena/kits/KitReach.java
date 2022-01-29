package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class KitReach extends Kit
{
	
	public KitReach(TeamArena game) {
		super("reach", "reach hacks", Material.CARROT_ON_A_STICK, game);
		
		setAbilities(new ReachAbility());
	}
	
	public static class ReachAbility extends Ability
	{
		@Override
		public void giveAbility(Player player) {
			player.setGameMode(GameMode.CREATIVE);
			player.setFlying(false);
			player.setAllowFlight(false);
		}
		
		@Override
		public void removeAbility(Player player) {
			player.setGameMode(GameMode.SURVIVAL);
		}
	}
	
}

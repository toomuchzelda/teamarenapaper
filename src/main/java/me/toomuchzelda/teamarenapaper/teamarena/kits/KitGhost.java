package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ArrowBodyCountChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitGhost extends Kit
{
	public KitGhost() {
		super("Ghost", "Invisible, sneaky, has ender pearls, and sus O_O! Although it's not very strong, and can't" +
				" push enemies very far", Material.GHAST_TEAR);
		
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
			//stop footstep sounds?
			//player.setSilent(true);
		}
		
		@Override
		public void removeAbility(Player player) {
			PlayerUtils.setInvisible(player, false);
			//player.setSilent(false);
		}
		
		//infinite enderpearls on a cooldown
		@Override
		public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
			if(event.getProjectile() instanceof EnderPearl) {
				event.setShouldConsume(false);
			}
		}

		@Override
		public void onItemCooldown(PlayerItemCooldownEvent event) {
			if(event.getType() == Material.ENDER_PEARL) {
				event.setCooldown(6 * 20);
			}
		}
		
		@Override
		public void onReceiveDamage(DamageEvent event) {
			if(event.getDamageType().is(DamageType.PROJECTILE) && event.getAttacker() instanceof AbstractArrow aa) {
				Player player = event.getPlayerVictim();
			
				//+1 for the one just added (yet to be added)
				int arrowsInBody = player.getArrowsInBody() + 1;
				Component obf = Component.text("as").decorate(TextDecoration.OBFUSCATED);
				
				PlayerInfo pinfo = Main.getPlayerInfo(player);
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					Component chatText = obf.append(Component.text(" You've been slammed by an arrow! You have "
							+ arrowsInBody + " arrows sticking out of you! ")
							.decoration(TextDecoration.OBFUSCATED, TextDecoration.State.FALSE)
							.append(obf)).color(NamedTextColor.AQUA);
					player.sendMessage(chatText);
				}
				if(pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					Component actionBarText = obf.append(Component.text("Hit by an arrow! " + arrowsInBody +
							" arrows in you now!").decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)).append(obf);
					player.sendActionBar(actionBarText);
				}
				
				if (aa.getPierceLevel() > 0) {
					//make arrows stick in the Ghost if it's a piercing projectile (normally doesn't)
					player.setArrowsInBody(event.getPlayerVictim().getArrowsInBody() + 1);
				}
			}
			
			//spawn le reveal particles
			Location baseLoc = event.getVictim().getLocation();
			double x = baseLoc.getX();
			double y = baseLoc.getY();
			double z = baseLoc.getZ();
			
			TeamArenaTeam team = Main.getPlayerInfo(event.getPlayerVictim()).team;
			
			for(int i = 0; i < 20; i++) {
				baseLoc.setX(x + MathUtils.randomRange(-0.3, 0.3));
				baseLoc.setY(y + MathUtils.randomRange(0, 1.4));
				baseLoc.setZ(z + MathUtils.randomRange(-0.3, 0.3));
				
				ParticleUtils.colouredRedstone(baseLoc, team.getColour(), 2, 1);
			}
		}

		@Override
		public void onDealtAttack(DamageEvent event) {
			if(event.hasKnockback()) {
				event.getKnockback().multiply(0.8);
			}
		}
		
		//@Not an override
		public void arrowCountDecrease(ArrowBodyCountChangeEvent event) {
			int amt = event.getNewAmount();
			
			Player player = (Player) event.getEntity();
			PlayerInfo pinfo = Main.getPlayerInfo(player);
			
			if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
				Component chat = Component.text("An arrow has disappeared... " + amt + " arrows stuck in you.").color(NamedTextColor.AQUA);
				player.sendMessage(chat);
			}
			if(pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
				Component actionBarText = Component.text(amt + " arrows left inside you").color(NamedTextColor.AQUA);
				player.sendActionBar(actionBarText);
			}
		}
	}
}

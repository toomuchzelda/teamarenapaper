package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.libraryaddict.core.inventory.PageInventory;
import me.libraryaddict.core.inventory.utils.IButton;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class KitSpy extends Kit
{
	
	public KitSpy() {
		super("Spy", "sus", Material.SPYGLASS);
		
		setArmor(new ItemStack(Material.IRON_HELMET), new ItemStack(Material.IRON_CHESTPLATE),
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));
		
		setItems(new ItemStack(Material.IRON_SWORD));
		
		
		setAbilities(new SpyAbility());
	}
	
	public static class SpyAbility extends Ability
	{
		@Override
		public void giveAbility(Player player) {
			//for each team show a different disguise
			TeamArenaTeam[] teams = Main.getGame().getTeams();
			TeamArenaTeam ownTeam = Main.getPlayerInfo(player).team;
			for(TeamArenaTeam team : teams) {
				if(team.isAlive() && team != ownTeam) {
					Player playerToCopy = team.getPlayerMembers().toArray(new Player[0])[MathUtils.randomMax(team.getPlayerMembers().size() - 1)];
					DisguiseManager.createDisguise(player, playerToCopy, team.getPlayerMembers());
				}
			}

			//use exp for skin cooldowns
			player.setLevel(0);
			player.setExp(0);
		}
		
		@Override
		public void removeAbility(Player player) {
			DisguiseManager.removeDisguises(player);
		}

		@Override
		public void onKill(DamageEvent event) {

		}

		@Override
		public void onPlayerTick(Player player) {
			float expToGain = 0.0025f; //20 seconds to fill

			float newExp = player.getExp() + expToGain;

			int currentLevel = player.getLevel();
			if (newExp > 1) {
					player.setLevel(currentLevel + 1);
					newExp = 0;

					PlayerInfo pinfo = Main.getPlayerInfo(player);

					new SpyInventory(player).openInventory();

					if(pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {

					}

					for(int i = 0; i < 3; i++) {
						Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () ->
								player.playSound(player.getLocation(), Sound.ITEM_AXE_STRIP, SoundCategory.PLAYERS,
								3f, 0f), i * 15);
					}
			}
			player.setExp(newExp);
		}
	}

	public static class SpyInventory extends PageInventory
	{

		public SpyInventory(Player player) {
			super(player, "Spy Inventory");

			ItemStack clickable = new ItemStack(Material.CHAIN);
			ItemMeta meta = clickable.getItemMeta();
			meta.displayName(Component.text("click Me!").color(TextColor.color(123, 231, 50)));
			clickable.setItemMeta(meta);

			IButton button = new IButton() {
				@Override
				public boolean onClick(ClickType clickType) {
					if(clickType.isLeftClick()) {
						player.sendMessage("You've clicked me!");
						closeInventory();
						return true;
					}
					else {
						player.sendMessage("Criminal right clicker!");
						closeInventory();
						return false;
					}
				}
			};

			ArrayList<Pair<ItemStack, IButton>> list = new ArrayList<>();

			list.add(new Pair<>(clickable, button));

			setPages(list);
		}
	}
}

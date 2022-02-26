package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.libraryaddict.core.inventory.DoNothingButton;
import me.libraryaddict.core.inventory.PageInventory;
import me.libraryaddict.core.inventory.utils.IButton;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class KitSpy extends Kit
{
	
	public KitSpy() {
		super("Spy", "sus", Material.SPYGLASS);
		
		setArmor(new ItemStack(Material.IRON_HELMET), new ItemStack(Material.IRON_CHESTPLATE),
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));
		
		setItems(new ItemStack(Material.IRON_SWORD), new ItemStack(Material.LEATHER_CHESTPLATE));
		
		
		setAbilities(new SpyAbility());
	}
	
	public static class SpyAbility extends Ability
	{
		@Override
		public void giveAbility(Player player) {
			//for each team show a different disguise
			/*TeamArenaTeam[] teams = Main.getGame().getTeams();
			TeamArenaTeam ownTeam = Main.getPlayerInfo(player).team;
			for(TeamArenaTeam team : teams) {
				if(team.isAlive() && team != ownTeam) {
					Player playerToCopy = team.getPlayerMembers().toArray(new Player[0])[MathUtils.randomMax(team.getPlayerMembers().size() - 1)];
					DisguiseManager.createDisguise(player, playerToCopy, team.getPlayerMembers());
				}
			}*/

			//use exp for skin cooldowns
			player.setLevel(0);
			player.setExp(0);
		}
		
		@Override
		public void removeAbility(Player player) {
			DisguiseManager.removeDisguises(player);
			player.setLevel(0);
			player.setExp(0);
		}

		@Override
		public void onKill(DamageEvent event) {

		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(event.useItemInHand() != Event.Result.DENY &&
					event.getAction() != Action.PHYSICAL && event.getMaterial() == Material.LEATHER_CHESTPLATE) {

				event.setUseItemInHand(Event.Result.DENY);
				new SpyInventory(event.getPlayer()).openInventory();
			}
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
		private Player player;
		public static final Component CLICK_TO_DISGUISE = Component.text("Click to disguise as this player")
				.color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false);

		public SpyInventory(Player player) {
			super(player, "Spy Inventory");

			/*ItemStack clickable = new ItemStack(Material.CHAIN);
			ItemMeta meta = clickable.getItemMeta();
			meta.displayName(Component.text("click Me!").color(TextColor.color(123, 231, 50)));
			clickable.setItemMeta(meta);

			IButton button = new IButton() {
				@Override
				public boolean onClick(ClickType clickType) {
					if(clickType.isLeftClick()) {
						player.sendMessage("You've clicked me!");
					}
					else {
						player.sendMessage("Criminal right clicker!");
					}
					closeInventory();
					return true;
				}
			};

			ArrayList<Pair<ItemStack, IButton>> list = new ArrayList<>();

			list.add(new Pair<>(clickable, button));

			setPages(list);*/

			this.player = player;
			this.fillInventory();
		}

		public void fillInventory() {
			ArrayList<Pair<ItemStack, IButton>> items = new ArrayList<>();
			TeamArena teamArena = Main.getGame();
			
			
			TeamArenaTeam[] gameTeams = teamArena.getTeams();
			TeamArenaTeam ownTeam = Main.getPlayerInfo(player).team;

			TeamArenaTeam[] sortedTeams = new TeamArenaTeam[gameTeams.length];
			
			//sort the teams so the user's own team appears last
			for(int i = 0; i < sortedTeams.length - 1; i++) {
				if(gameTeams[i] == ownTeam) {
					sortedTeams[sortedTeams.length - 1] = gameTeams[i];
					sortedTeams[i] = gameTeams[sortedTeams.length - 1];
				}
				else {
					sortedTeams[i] = gameTeams[i];
				}
			}
			//if the ownTeam was already in the last position of the array before sorting it'll be null here so set it
			if(sortedTeams[sortedTeams.length - 1] == null) {
				sortedTeams[sortedTeams.length - 1] = ownTeam;
			}

			for(TeamArenaTeam team : sortedTeams) {
				items.add(new Pair<>(team.getIconItem(), DoNothingButton.button));
				
				for(Player otherPlayer : team.getPlayerMembers()) {
					if(otherPlayer == player)
						continue;
					
					if(!teamArena.isSpectator(otherPlayer)) {
						Kit othersKit = Kit.getActiveKit(otherPlayer, team != ownTeam); //hide kit spies in the menu
						
						ItemStack kitIcon = new ItemStack(othersKit.getIcon());
						/**
						 *  |------|  playerName
						 *  |(icon)|  Kit: player's kit
						 *  |      |  Click to disguise as this player
						 *  |------|
						 */
						ItemMeta meta = kitIcon.getItemMeta();
						meta.displayName(otherPlayer.displayName());
						List<Component> lore = new ArrayList<>(2);
						lore.add(ItemUtils.noItalics(Component.text("Kit: " + othersKit.getName())));
						lore.add(CLICK_TO_DISGUISE);
						meta.lore(lore);
						kitIcon.setItemMeta(meta);
						
						items.add(new Pair<>(kitIcon, new IButton()
						{
							@Override
							public boolean onClick(ClickType clickType) {
								player.sendMessage("disguising as " + otherPlayer.getName());
								closeInventory();
								return true;
							}
						}));
					}
				}
				
				//make new line if needed and not at the end of the page
				while (team != sortedTeams[sortedTeams.length - 1] && items.size() % 9 != 0)
					items.add(null);
			}
			
			setPages(items);
		}

		@Override
		public void onTick() {
			if(TeamArena.getGameTick() % 10 == 0) { //every half second
				fillInventory();
			}
		}
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ClickableItem;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.PagedInventory;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class KitSpy extends Kit
{
	public static final Material DISGUISE_MENU_ITEM = Material.CARVED_PUMPKIN;
	
	public KitSpy() {
		super("Spy", "sus", Material.SPYGLASS);
		
		setArmor(new ItemStack(Material.IRON_HELMET), new ItemStack(Material.IRON_CHESTPLATE),
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));
		
		setItems(new ItemStack(Material.IRON_SWORD), new ItemStack(DISGUISE_MENU_ITEM));
		
		
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
					event.getAction() != Action.PHYSICAL && event.getMaterial() == DISGUISE_MENU_ITEM) {

				event.setUseItemInHand(Event.Result.DENY);
				Inventories.openInventory(event.getPlayer(), new SpyInventory());
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
		
		public static void disguisePlayer(Player player, TeamArenaTeam ownTeam, Player otherPlayer) {
			for(TeamArenaTeam team : Main.getGame().getTeams()) {
				if(team == ownTeam)
					continue;
			
				DisguiseManager.createDisguise(player, otherPlayer, team.getPlayerMembers());
			}
			
			//copy invisibility of kit
			player.setInvisible(otherPlayer.isInvisible());
		}
	}

	public static class SpyInventory extends PagedInventory {
		public static final Component CLICK_TO_DISGUISE = Component.text("Click to disguise as this player")
				.color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false);

		@Override
		public Component getTitle(Player player) {
			return Component.text("Spy Inventory");
		}

		@Override
		public int getRows() {
			return 6;
		}

		@Override
		public void init(Player player, InventoryAccessor inventory) {
			// set prev page/next page items first
			inventory.set(45, getPreviousPageItem(inventory));
			inventory.set(53, getNextPageItem(inventory));


			ArrayList<ClickableItem> items = new ArrayList<>();
			TeamArena teamArena = Main.getGame();

			TeamArenaTeam[] gameTeams = teamArena.getTeams();
			TeamArenaTeam ownTeam = Main.getPlayerInfo(player).team;

			//sort teams so own team is last
			TeamArenaTeam[] sortedTeams = Arrays.copyOf(gameTeams, gameTeams.length);
			Arrays.sort(sortedTeams, Comparator.<TeamArenaTeam, Integer>comparing(team -> team == ownTeam ? 1 : 0)
					.thenComparing(TeamArenaTeam::getSimpleName));

			for (TeamArenaTeam team : sortedTeams) {
				items.add(ClickableItem.empty(team.getIconItem()));

				for (Player otherPlayer : team.getPlayerMembers()) {
					if (!teamArena.isSpectator(otherPlayer)) {
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
						
						items.add(ClickableItem.of(kitIcon, e -> {
									player.sendMessage("Disguising as " + otherPlayer.getName());
									DisguiseManager.removeDisguises(player);
									if(otherPlayer != player)
										SpyAbility.disguisePlayer(player, ownTeam, otherPlayer);
									
									Bukkit.getScheduler().runTask(Main.getPlugin(),
											() -> {
												player.closeInventory();
											});
								}
						));
					}
				}
				
				//make new line if needed and not at the end of the page
				while (team != sortedTeams[sortedTeams.length - 1] && items.size() % 9 != 0)
					items.add(null);
			}

			setPageItems(items, inventory);
		}
	}
}

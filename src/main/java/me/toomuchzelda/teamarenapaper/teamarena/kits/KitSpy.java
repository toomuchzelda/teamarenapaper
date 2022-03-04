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
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.*;

public class KitSpy extends Kit
{
	public static final Material DISGUISE_MENU_MATERIAL = Material.CARVED_PUMPKIN;
	public static final float DISGUISE_MENU_COOLDOWN = 12; //in seconds
	public static final int TIME_TO_DISGUISE_MENU = 3 * 20;
	public static final int TIME_TO_DISGUISE_HEAD = 20;
	public static final Component COOLDOWN_MESSAGE = Component.text("Disguise pumpkin is still recharging!").color(TextUtils.ERROR_RED);
	//preferably wouldnt be static, but this is becoming really messy
	public static HashMap<Player, DisguiseInfo> currentlyDisguised;
	
	public KitSpy() {
		super("Spy", "sus", Material.SPYGLASS);
		
		ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
		LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
		meta.setColor(Color.WHITE);
		boots.setItemMeta(meta);
		
		setArmor(new ItemStack(Material.GOLDEN_HELMET), new ItemStack(Material.IRON_CHESTPLATE),
				new ItemStack(Material.IRON_LEGGINGS), boots);
		
		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		sword.addEnchantment(Enchantment.DAMAGE_ALL, 1);
		setItems(sword, new ItemStack(DISGUISE_MENU_MATERIAL));
		
		setAbilities(new SpyAbility());
		
		currentlyDisguised = new HashMap<>();
	}
	
	public class SpyAbility extends Ability
	{
		@Override
		public void giveAbility(Player player) {
			//use exp for skin cooldowns
			player.setLevel(0);
			player.setExp(0.9f); //start with a menu disguise ready
		}
		
		@Override
		public void removeAbility(Player player) {
			DisguiseManager.removeDisguises(player);
			currentlyDisguised.remove(player);
			player.setLevel(0);
			player.setExp(0);
		}

		@Override
		public void onKill(DamageEvent event) {

		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(event.useItemInHand() != Event.Result.DENY &&
					event.getAction() != Action.PHYSICAL && event.getMaterial() == DISGUISE_MENU_MATERIAL) {
				
				event.setUseItemInHand(Event.Result.DENY);
				if(event.getPlayer().getExp() >= 1) {
					Inventories.openInventory(event.getPlayer(), new SpyInventory());
				}
				else {
					event.getPlayer().sendMessage(COOLDOWN_MESSAGE);
				}
			}
		}

		@Override
		public void onPlayerTick(Player player) {
			final float expToGain = 1f / DISGUISE_MENU_COOLDOWN / 20f; //12 seconds
			float previousExp = player.getExp();
			float newExp = previousExp + expToGain;
			if (newExp > 1) {
				newExp = 1;
			}
			
			if(previousExp != newExp)
				player.setExp(newExp);
		}
		
		@Override
		public void onTick() {
			var iter = currentlyDisguised.entrySet().iterator();
			int gameTick = TeamArena.getGameTick();
			while(iter.hasNext()) {
				Map.Entry<Player, DisguiseInfo> entry = iter.next();
				
				//do disguise wearing cooldown if they're putting one on
				DisguiseInfo dinfo = entry.getValue();
				Player player = entry.getKey();
				if(dinfo != null) {
					if(dinfo.timeToApply >= gameTick) {
						int ticksLeft = dinfo.timeToApply - gameTick;
						if (ticksLeft % 10 == 0) {
							player.playEffect(EntityEffect.BREAK_EQUIPMENT_BOOTS);
							player.playEffect(EntityEffect.BREAK_EQUIPMENT_LEGGINGS);
							player.playEffect(EntityEffect.BREAK_EQUIPMENT_CHESTPLATE);
							player.playEffect(EntityEffect.BREAK_EQUIPMENT_HELMET);
						}
						
						if (ticksLeft == 0) {
							for (DisguiseManager.Disguise disguise : dinfo.disguises) {
								DisguiseManager.startDisgusie(disguise);
							}
							
							dinfo.informMessage = Component.text("Disguised as: " + dinfo.disguisingAsPlayerName + "    Kit: "
									+ dinfo.disguisingAsKit.getName()).color(NamedTextColor.LIGHT_PURPLE);
							
							PlayerInfo pinfo = Main.getPlayerInfo(player);
							if(pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
								player.sendActionBar(dinfo.informMessage);
							}
							if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
								player.sendMessage(dinfo.informMessage);
							}
							player.setExp(0);
						}
					}
					else if(gameTick % 20 == 0) {
						PlayerInfo pinfo = Main.getPlayerInfo(player);
						if(pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
							player.sendActionBar(dinfo.informMessage);
						}
					}
				}
			}
		}
		
		/**
		 * handle disguise change
		 */
		public static void disguisePlayer(Player player, TeamArenaTeam ownTeam, Player toDisguiseAs) {
			DisguiseManager.removeDisguises(player);
			
			if(toDisguiseAs == player) {
				currentlyDisguised.remove(player);
				Component yourselfText = Component.text("Removed disguise, you now look like yourself").color(NamedTextColor.DARK_PURPLE);
				PlayerUtils.sendKitMessage(player, yourselfText, yourselfText);
				return;
			}
			
			Component disguisingText = Component.text("Disguising as ").color(NamedTextColor.DARK_PURPLE).append(toDisguiseAs.playerListName());
			PlayerUtils.sendKitMessage(player, disguisingText, disguisingText);
			
			List<DisguiseManager.Disguise> disguises = new LinkedList<>();
			for(TeamArenaTeam team : Main.getGame().getTeams()) {
				if(team == ownTeam)
					continue;
			
				DisguiseManager.Disguise disguise = DisguiseManager.createDisguise(player, toDisguiseAs,
						team.getPlayerMembers(), false);
				disguises.add(disguise);
			}
			
			DisguiseInfo info = new DisguiseInfo(disguises, Main.getPlayerInfo(toDisguiseAs).activeKit,
					toDisguiseAs, TeamArena.getGameTick() + TIME_TO_DISGUISE_MENU);
			currentlyDisguised.put(player, info);
			
			//reset cooldown
			player.setExp(0);
		}
	}

	public class SpyInventory extends PagedInventory {
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
							//todo better message
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
	
	public static class DisguiseInfo
	{
		public List<DisguiseManager.Disguise> disguises;
		public Kit disguisingAsKit;
		public String disguisingAsPlayerName;
		public int timeToApply; //tick to apply disguise
		
		public Component informMessage;
		
		public DisguiseInfo(List<DisguiseManager.Disguise> disguises, Kit kit, Player player, int timeToApply) {
			this.disguises = disguises;
			this.disguisingAsKit = kit;
			this.disguisingAsPlayerName = player.getName();
			this.timeToApply = timeToApply;
		}
	}
}

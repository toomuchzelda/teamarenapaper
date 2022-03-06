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
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class KitSpy extends Kit
{
	public static final int TIME_TO_DISGUISE_MENU = 3 * 20;
	public static final int TIME_TO_DISGUISE_HEAD = 20;
	public static final float DISGUISE_MENU_COOLDOWN = 12; //in seconds
	
	public static final Material DISGUISE_MENU_MATERIAL = Material.CARVED_PUMPKIN;
	public static final Component DISGUISE_MENU_NAME = ItemUtils.noItalics(Component.text("sussy mask")
			.color(TextUtils.ERROR_RED));
	public static final Component DISGUISE_MENU_DESC = ItemUtils.noItalics(Component.text("Click to disguise!"));
	public static final Component DISGUISE_MENU_DESC2 = ItemUtils.noItalics(Component.text(DISGUISE_MENU_COOLDOWN + "sec recharge. "
			+ (TIME_TO_DISGUISE_MENU / 20) + "sec disguise time"));
	
	public static final List<Component> DISGUISE_MENU_LORE_LIST;
	
	public static final Component COOLDOWN_MESSAGE = Component.text("Disguise pumpkin is still recharging!").color(TextUtils.ERROR_RED);
	public static final Component CLICK_TO_DISGUISE = Component.text("Click to disguise as this player")
			.color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false);
	
	public static final Component HEAD_TIME_MESSAGE = ItemUtils.noItalics(Component.text((TIME_TO_DISGUISE_HEAD / 20)
			+ "sec disguise time").color(NamedTextColor.LIGHT_PURPLE));
	public static final Component ATTACKED_MESSAGE = Component.text("Lost your disguise because you attacked someone!").color(NamedTextColor.LIGHT_PURPLE);
	
	
	//preferably wouldnt be static, but this is becoming really messy
	public static HashMap<Player, SpyDisguiseInfo> currentlyDisguised;
	
	//need to store Kit and Player, player should be inside the skull's PlayerProfile already
	public final HashMap<ItemStack, Kit> skullItemDisguises = new HashMap<>();
	
	static {
		ArrayList<Component> lore = new ArrayList<>(2);
		lore.add(DISGUISE_MENU_DESC);
		lore.add(DISGUISE_MENU_DESC2);
		DISGUISE_MENU_LORE_LIST = Collections.unmodifiableList(lore);
	}
	
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
		
		ItemStack menu = new ItemStack(DISGUISE_MENU_MATERIAL);
		ItemMeta menuMeta = menu.getItemMeta();
		menuMeta.displayName(DISGUISE_MENU_NAME);
		menuMeta.lore(DISGUISE_MENU_LORE_LIST);
		menu.setItemMeta(menuMeta);
		
		setItems(sword, menu);
		
		setAbilities(new SpyAbility());
		
		currentlyDisguised = new HashMap<>();
	}
	
	/**
	 * for packet listener
	 */
	public static SpyDisguiseInfo getInfo(Player player) {
		return currentlyDisguised.get(player);
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
			player.setInvisible(false);
			
			//remove any skulls they might've had from the hashmap
			var iter = player.getInventory().iterator();
			while(iter.hasNext()) {
				ItemStack item = iter.next();
				if(item == null)
					continue;
				
				if(item.getType() == Material.PLAYER_HEAD) //not every head may be a spy disguise item but meh
					skullItemDisguises.remove(item);
			}
			
			player.setLevel(0);
			player.setExp(0);
		}
		
		@Override
		public void onDealtAttack(DamageEvent event) {
			Player spy = (Player) event.getFinalAttacker();
			if(currentlyDisguised.remove(spy) != null) {
				DisguiseManager.removeDisguises(spy);
				PlayerUtils.sendKitMessage(spy, ATTACKED_MESSAGE, ATTACKED_MESSAGE);
				for(int i = 0; i < 10; i++)
					spy.playSound(spy.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 99999f, ((float) ((i - 5) * 0.2)));
			}
		}

		@Override
		public void onAssist(Player player, double amount, Player victim) {
			if(amount >= 0.7) {
				Kit victimsKit = Kit.getActiveKitHideInvis(victim);
				
				ItemStack victimsHead = new ItemStack(Material.PLAYER_HEAD);
				SkullMeta meta = (SkullMeta) victimsHead.getItemMeta();
				meta.setPlayerProfile(victim.getPlayerProfile());
				meta.displayName(ItemUtils.noItalics(victim.playerListName().append(Component.text("'s skin (Click to disguise)").color(NamedTextColor.DARK_PURPLE))));
				List<Component> lore = new ArrayList<>(2);
				lore.add(ItemUtils.noItalics(Component.text("Kit: " + victimsKit.getName()).color(NamedTextColor.LIGHT_PURPLE)));
				lore.add(HEAD_TIME_MESSAGE);
				lore.add(Component.text(ItemUtils.getUniqueId()));
				meta.lore(lore);
				victimsHead.setItemMeta(meta);
				
				skullItemDisguises.put(victimsHead, victimsKit);
				player.getInventory().addItem(victimsHead);
			}
		}
		
		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(event.useItemInHand() != Event.Result.DENY && event.getAction() != Action.PHYSICAL) {
				if(event.getMaterial() == DISGUISE_MENU_MATERIAL) {
					event.setUseItemInHand(Event.Result.DENY);
					if (event.getPlayer().getExp() >= 1) {
						Inventories.openInventory(event.getPlayer(), new SpyInventory());
					}
					else {
						event.getPlayer().sendMessage(COOLDOWN_MESSAGE);
					}
				}
				else if (event.getMaterial() == Material.PLAYER_HEAD){
					ItemStack head = event.getItem();
					Kit kit = skullItemDisguises.remove(head);
					if(kit != null) {
						SkullMeta meta = (SkullMeta) head.getItemMeta();
						Player spy = event.getPlayer();
						Player toDisguiseAs = Bukkit.getPlayer(meta.getPlayerProfile().getId());
						
						disguisePlayer(spy, Main.getPlayerInfo(spy).team, toDisguiseAs, kit, TIME_TO_DISGUISE_HEAD, false);
						
						spy.getInventory().remove(head);
						event.setUseItemInHand(Event.Result.DENY);
					}
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
			
			//if(previousExp != newExp)
				player.setExp(newExp);
		}
		
		@Override
		public void onTick() {
			var iter = currentlyDisguised.entrySet().iterator();
			int gameTick = TeamArena.getGameTick();
			while(iter.hasNext()) {
				Map.Entry<Player, SpyDisguiseInfo> entry = iter.next();
				
				//do disguise wearing cooldown if they're putting one on
				SpyDisguiseInfo dinfo = entry.getValue();
				Player player = entry.getKey();
				if(dinfo != null) {
					if(dinfo.timeToApply >= gameTick) {
						int ticksLeft = dinfo.timeToApply - gameTick;
						if (ticksLeft % 10 == 0) {
							/*player.playEffect(EntityEffect.BREAK_EQUIPMENT_BOOTS);
							player.playEffect(EntityEffect.BREAK_EQUIPMENT_LEGGINGS);
							player.playEffect(EntityEffect.BREAK_EQUIPMENT_CHESTPLATE);
							player.playEffect(EntityEffect.BREAK_EQUIPMENT_HELMET);*/
							Location loc = player.getLocation();
							Location two = loc.clone().add(MathUtils.randomRange(-0.3, 0.3), 0.4, MathUtils.randomRange(-0.3, -0.3));
							Location three = player.getEyeLocation().add(MathUtils.randomRange(-0.3, 0.3), 0, MathUtils.randomRange(-0.3, 0.3));
							BlockData blockData = Material.REDSTONE_BLOCK.createBlockData();
							World world = loc.getWorld();
							for(byte i = 0; i < 4; i++) {
								world.spawnParticle(Particle.BLOCK_CRACK, loc, 2, blockData);
								world.spawnParticle(Particle.BLOCK_CRACK, two, 2, blockData);
								world.spawnParticle(Particle.BLOCK_CRACK, three, 2, blockData);
							}
						}
						
						if (ticksLeft == 0) {
							for (DisguiseManager.Disguise disguise : dinfo.disguises) {
								DisguiseManager.startDisgusie(disguise);
								if(dinfo.disguisingAsKit.isInvisKit())
									player.setInvisible(true);
							}
							
							dinfo.informMessage = Component.text("Disguised as: ").color(NamedTextColor.LIGHT_PURPLE).append(dinfo.disguisingAsPlayerName)
									.append(Component.text("    Kit: " + dinfo.disguisingAsKit.getName()).color(NamedTextColor.LIGHT_PURPLE));
							
							PlayerInfo pinfo = Main.getPlayerInfo(player);
							if(pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
								player.sendActionBar(dinfo.informMessage);
							}
							if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
								player.sendMessage(dinfo.informMessage);
							}
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
		public static void disguisePlayer(Player player, TeamArenaTeam ownTeam, Player toDisguiseAs, Kit theirKit,
										  int timeToDisguise, boolean resetCooldown) {
			DisguiseManager.removeDisguises(player);
			player.setInvisible(false);
			
			if(toDisguiseAs == player) {
				currentlyDisguised.remove(player);
				Component yourselfText = Component.text("Removed disguise, you now look like yourself").color(NamedTextColor.LIGHT_PURPLE);
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
			
			if(theirKit == null) {
				//theirKit = Kit.getActiveKit(toDisguiseAs, false);
				theirKit = Kit.getActiveKitHideInvis(toDisguiseAs);
			}
			
			SpyDisguiseInfo info = new SpyDisguiseInfo(disguises, theirKit, toDisguiseAs,
					TeamArena.getGameTick() + timeToDisguise);
			currentlyDisguised.put(player, info);
			
			//reset cooldown
			if(resetCooldown)
				player.setExp(0);
		}
	}

	public class SpyInventory extends PagedInventory {
		
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
			buildPages(player, inventory);
		}
		
		@Override
		public void update(Player player, InventoryAccessor inventory) {
			if(TeamArena.getGameTick() % 10 == 0) {
				inventory.invalidate();
			}
		}
		
		public void buildPages(Player player, InventoryAccessor inventory) {
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
						
						ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
						SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
						meta.setPlayerProfile(otherPlayer.getPlayerProfile());
						meta.displayName(otherPlayer.playerListName());
						meta.lore(Collections.singletonList(CLICK_TO_DISGUISE));
						playerHead.setItemMeta(meta);
						
						items.add(ClickableItem.of(playerHead, e -> {
									if(!Main.getGame().isSpectator(otherPlayer)) {
										
										SpyAbility.disguisePlayer(player, ownTeam, otherPlayer, null,
												TIME_TO_DISGUISE_MENU, true);
										
										Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
											player.closeInventory();
										});
									}
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
	
	/**
	 * specifically for kit Spy
	 */
	public static class SpyDisguiseInfo
	{
		public List<DisguiseManager.Disguise> disguises;
		public Kit disguisingAsKit;
		public Component disguisingAsPlayerName;
		public int timeToApply; //tick to apply disguise
		
		public Component informMessage;
		
		public SpyDisguiseInfo(List<DisguiseManager.Disguise> disguises, Kit kit, Player player, int timeToApply) {
			this.disguises = disguises;
			this.disguisingAsKit = kit;
			this.disguisingAsPlayerName = player.playerListName();
			this.timeToApply = timeToApply;
		}
	}
}

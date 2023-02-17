package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.inventory.SpectateInventory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class KitSpy extends Kit
{
	public static final int TIME_TO_DISGUISE_MENU = 3 * 20;
	public static final int TIME_TO_DISGUISE_HEAD = 20;
	public static final float DISGUISE_MENU_COOLDOWN = 12; //in seconds

	public static final Material DISGUISE_MENU_MATERIAL = Material.CARVED_PUMPKIN;
	public static final Component DISGUISE_MENU_NAME = ItemUtils.noItalics(Component.text("Sussy Mask")
			.color(TextColors.ERROR_RED));
	public static final Component DISGUISE_MENU_DESC = ItemUtils.noItalics(Component.text("Click to disguise!", TextUtils.RIGHT_CLICK_TO));
	public static final Component DISGUISE_MENU_DESC2 = ItemUtils.noItalics(Component.text(DISGUISE_MENU_COOLDOWN + "sec recharge. "
			+ (TIME_TO_DISGUISE_MENU / 20) + "sec disguise time"));

	public static final List<Component> DISGUISE_MENU_LORE_LIST;

	public static final Component COOLDOWN_MESSAGE = Component.text("Disguise pumpkin is still recharging!").color(TextColors.ERROR_RED);

	public static final Component HEAD_TIME_MESSAGE = ItemUtils.noItalics(Component.text((TIME_TO_DISGUISE_HEAD / 20)
			+ "sec disguise time").color(NamedTextColor.LIGHT_PURPLE));
	public static final Component ATTACKED_MESSAGE = Component.text("Lost your disguise because you attacked someone!").color(NamedTextColor.LIGHT_PURPLE);


	//preferably wouldnt be static, but this is becoming really messy
	public static HashMap<Player, SpyDisguiseInfo> currentlyDisguised;

	//need to store Kit and Player, player should be inside the skull's PlayerProfile already
	public final HashMap<ItemStack, Kit> skullItemDisguises = new HashMap<>();

	static {
		DISGUISE_MENU_LORE_LIST = List.of(DISGUISE_MENU_DESC, DISGUISE_MENU_DESC2);
	}

	public KitSpy() {
		super("Spy", "A master of disguise, this kit can choose to copy the appearance of any player in " +
				"the game. Most of the time, it copies their kit correctly too... just as long as they're not an invisible one." +
				"\n\nAfter killing someone, it gets a Quick Swap disguise of their victim which it can wear instantly, " +
				"making it easy to hide its tracks.\n\n" +
				"It's a kind of kit that makes enemies say: \"There's an impostor among us!\""
				, Material.CARVED_PUMPKIN);

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

		setCategory(KitCategory.STEALTH);

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
		public void unregisterAbility() {
			skullItemDisguises.clear();
		}

		@Override
		public void giveAbility(Player player) {
			//use exp for skin cooldowns
			player.setLevel(0);
			player.setExp(0.95f); //start with a menu disguise ready
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

			// close GUI
			Inventories.closeInventory(player, SpyInventory.class);

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
						var player = event.getPlayer();
						var team = Main.getPlayerInfo(player).team;
						Inventories.openInventory(player, new SpyInventory(team));
					}
					else {
						event.getPlayer().sendMessage(COOLDOWN_MESSAGE);
						// Need to update their client that the block wasn't placed
						if (event.getClickedBlock() != null) {
							event.getPlayer().updateInventory();
						}
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

	public class SpyInventory extends SpectateInventory {
		private TeamArenaTeam viewerTeam;
		public SpyInventory(@NotNull TeamArenaTeam viewerTeam) {
			super(null, false);
			this.viewerTeam = viewerTeam;
		}

		@Override
		public @NotNull Component getTitle(Player player) {
			return Component.text("Select a spy disguise");
		}

		@Override
		protected ItemStack teamToItem(@Nullable TeamArenaTeam team, boolean selected) {
			// conceal player count
			if (team != viewerTeam && team != null && team.isAlive()) {
				var stack = ItemBuilder.of(team.getIconItem().getType())
					.displayName(team.getComponentName())
					.lore(Component.text("Players: ???", NamedTextColor.GRAY),
						Component.text("Score: " + team.getTotalScore(), NamedTextColor.GRAY))
					.build();
				return ItemUtils.highlightIfSelected(stack, selected);
			}
			return super.teamToItem(team, selected);
		}

		// don't show sensitive information (kits and distance) for enemies
		@Override
		protected ClickableItem playerToItem(@NotNull Player player, Location distanceOrigin, boolean showKit) {
			Consumer<InventoryClickEvent> clickHandler = e -> {
				Player viewer = (Player) e.getWhoClicked();
				if (!Main.getGame().isSpectator(player)) {
					SpyAbility.disguisePlayer(viewer, viewerTeam, player, null,
						TIME_TO_DISGUISE_MENU, true);
					Inventories.closeInventory(viewer);
				}
			};

			var playerInfo = Main.getPlayerInfo(player);
			if (viewerTeam.equals(playerInfo.team)) {
				var originalItem = super.playerToItem(player, distanceOrigin, showKit);
				return ItemBuilder.from(originalItem.stack())
					.addLore(Component.empty(), Component.text("Click to disguise as this player", NamedTextColor.LIGHT_PURPLE))
					.toClickableItem(clickHandler);
			}

			return ItemBuilder.of(Material.PLAYER_HEAD)
				.displayName(Component.text(player.getName(), playerInfo.team.getRGBTextColor()))
				.lore(Component.text("Click to disguise as this player", NamedTextColor.LIGHT_PURPLE))
				.meta(SkullMeta.class, skullMeta -> skullMeta.setOwningPlayer(player))
				.toClickableItem(clickHandler);
		}
	}

	/**
	 * specifically for kit Spy
	 */
	public record SpyDisguiseInfo(List<DisguiseManager.Disguise> disguises,
								  Kit disguisingAsKit,
								  Player disguisingAsPlayer,
								  int timeToApply,
								  Component informMessage) {

		public SpyDisguiseInfo {
			disguises = List.copyOf(disguises); // defensive copy
		}

		public SpyDisguiseInfo(List<DisguiseManager.Disguise> disguises, Kit kit, Player player, int timeToApply) {
			this(disguises, kit, player, timeToApply, Component.textOfChildren(
				Component.text("Disguised as ", NamedTextColor.LIGHT_PURPLE),
				player.playerListName(),
				Component.text("    Kit: ", NamedTextColor.LIGHT_PURPLE),
				Component.text(kit.getName(), kit.getCategory().textColor())
			));
		}
	}
}

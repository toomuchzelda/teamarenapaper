package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingOutlineManager;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingSelector;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class KitDemolitions extends Kit
{
	public static final int TNT_MINE_COUNT = 2;
	public static final int PUSH_MINE_COUNT = 1;
	public static final TextColor TNT_COLOR = TextColor.color(0xd82e1a);
	public static final ItemStack TNT_MINE_ITEM;
	public static final ItemStack PUSH_MINE_ITEM;
	public static final ItemStack TNT_MINE_DEPLETED;
	public static final ItemStack PUSH_MINE_DEPLETED;
	public static final ItemStack REMOTE_DETONATOR_ITEM = ItemBuilder.of(Material.FLINT_AND_STEEL)
		.displayName(Component.text("Remote Trigger", NamedTextColor.BLUE))
		.lore(TextUtils.wrapString("Point at any of your mines from any distance to select one. " +
			"Once selected, it will turn blue. Right click and boom!", Style.style(TextUtils.RIGHT_CLICK_TO), 200))
		.build();

	//valid blocks for mines to be placed on
	private static final boolean[] VALID_MINE_BLOCKS;

	static {
		VALID_MINE_BLOCKS = new boolean[Material.values().length];

		for(Material mat : Material.values()) {
			if(!mat.isBlock())
				continue;

			if(mat.isOccluding() || mat.name().endsWith("SLAB") || mat.name().endsWith("STAIRS")) {
				setValidMineBlock(mat);
			}
			else if(mat.name().endsWith("LEAVES")) {
				setValidMineBlock(mat);
			}
			else if (mat.name().contains("GLASS")) {
				setValidMineBlock(mat);
			}
		}
		setValidMineBlock(Material.DIRT_PATH);
		setValidMineBlock(Material.FARMLAND);

		List<Component> usage = TextUtils.wrapString("Right click to place the mine. " +
				"It will be triggered by your remote detonator or when enemies step on it.",
			Style.style(TextUtils.RIGHT_CLICK_TO));

		List<Component> tntMineLore = TextUtils.wrapString("A TNT landmine trap that blows enemies to smithereens",
			Style.style(TNT_COLOR));
		TNT_MINE_ITEM = ItemBuilder.of(Material.TNT)
			.amount(TNT_MINE_COUNT)
			.displayName(Component.text("TNT Mine", TNT_COLOR))
			.lore(tntMineLore)
			.addLore(usage)
			.build();

		TNT_MINE_DEPLETED = ItemBuilder.of(Material.COAL_BLOCK)
			.displayName(Component.text("TNT Mine (Depleted)", TNT_COLOR))
			.lore(tntMineLore)
			.build();

		List<Component> pushMineLore = TextUtils.wrapString("A trap that creates an explosive gust of air, pushing away all enemies near it",
			Style.style(NamedTextColor.WHITE));
		PUSH_MINE_ITEM = ItemBuilder.of(Material.WHITE_WOOL)
			.amount(PUSH_MINE_COUNT)
			.displayName(Component.text("Push Mine", NamedTextColor.WHITE))
			.lore(pushMineLore)
			.addLore(usage)
			.build();

		PUSH_MINE_DEPLETED = ItemBuilder.of(Material.GRAY_WOOL)
			.displayName(Component.text("Push Mine (Depleted)", NamedTextColor.WHITE))
			.lore(pushMineLore)
			.build();
	}

	private static void setValidMineBlock(Material mat) {
		VALID_MINE_BLOCKS[mat.ordinal()] = true;
	}

	public KitDemolitions() {
		super("Demolitions", """
				This kit comes with traps! Specifically, those that explode when people step on them. They also blend in with the ground no matter the color!

				It comes with a TNT trap that does lots of damage and a Push Mine trap that sends enemies flying!

				After placing them down, enemies can step on them to trigger them or they can be triggered remotely by the kit user.""",
				Material.STONE_PRESSURE_PLATE);

		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);

		//Mine item stack sizes set in giveAbility()
		setItems(sword, TNT_MINE_ITEM, PUSH_MINE_ITEM, REMOTE_DETONATOR_ITEM);

		this.setAbilities(new DemolitionsAbility());

		this.setArmor(new ItemStack(Material.CHAINMAIL_HELMET), new ItemStack(Material.CHAINMAIL_CHESTPLATE),
				new ItemStack(Material.CHAINMAIL_LEGGINGS), new ItemStack(Material.GOLDEN_BOOTS));

		setCategory(KitCategory.SUPPORT);

		setFuseEnchantLevel(5);
	}

	public static boolean isValidMineBlock(Block block) {
		return VALID_MINE_BLOCKS[block.getType().ordinal()];
	}

	public static Block getMineBaseBlock(Block lookingAt, BlockFace lookingAtSide) {
		if (!lookingAt.isBuildable()) { // use base block if replaceable
			lookingAt = lookingAt.getRelative(BlockFace.DOWN);
		}

		Block block = lookingAt.getRelative(lookingAtSide);
		return block.getRelative(BlockFace.DOWN);
	}

	public static boolean checkMineLocation(Block base) {
		if (!isValidMineBlock(base))
			return false;

		Block mine = base.getRelative(BlockFace.UP);

		return (mine.getType() == Material.AIR || mine.isReplaceable()) && Main.getGame().canBuildAt(mine);
	}

	public record RegeneratingMine(MineType type, int removedTime) {}

	public static class DemolitionsAbility extends Ability
	{
		private static final Map<Player, List<RegeneratingMine>> regeneratingMines = new LinkedHashMap<>();

		public static final DamageType DEMO_TNTMINE_BYSTANDER = new DamageType(DamageType.DEMO_TNTMINE,
				"%Killed% was blown up by %Killer%'s TNT Mine because %Cause% stepped on it. Thanks a lot!");

		public static final DamageType DEMO_TNTMINE_REMOTE = new DamageType(DamageType.DEMO_TNTMINE,
				"%Killed% was blown up by %Killer%'s TNT Mine remotely");


		@Override
		public void unregisterAbility() {
			regeneratingMines.clear();
		}

		private static final Component RCLICK_PLACE = Component.text("Right click: place mine", TextUtils.RIGHT_CLICK_TO);
		private static final Component RCLICK_DETONATE = Component.text("Right click: detonate selected mine", TextUtils.RIGHT_CLICK_TO);

		private static final Map<ItemStack, BuildingSelector.Action> SELECTOR_ACTION = Map.of(
			REMOTE_DETONATOR_ITEM, BuildingSelector.Action.selectBuilding(RCLICK_DETONATE, building -> building instanceof DemoMine demoMine &&
				demoMine.isArmed() && !demoMine.isTriggered()),
			TNT_MINE_ITEM, BuildingSelector.Action.showBlockPreview(RCLICK_PLACE, TNTMine.class, TNTMine::new, null),
			PUSH_MINE_ITEM, BuildingSelector.Action.showBlockPreview(RCLICK_PLACE, PushMine.class, PushMine::new, null),
			TNT_MINE_DEPLETED, BuildingSelector.Action.filterBuilding(building -> building instanceof TNTMine),
			PUSH_MINE_DEPLETED, BuildingSelector.Action.filterBuilding(building -> building instanceof PushMine)
		);

		@Override
		public void giveAbility(Player player) {
			BuildingOutlineManager.registerSelector(player, BuildingSelector.fromAction(SELECTOR_ACTION));
		}

		@Override
		public void removeAbility(Player player) {
			BuildingOutlineManager.unregisterSelector(player);
			BuildingManager.getAllPlayerBuildings(player).forEach(building -> {
				if (building instanceof DemoMine) {
					BuildingManager.destroyBuilding(building);
				}
			});
			regeneratingMines.remove(player);

			player.setCooldown(TNT_MINE_DEPLETED.getType(), 0);
			player.setCooldown(PUSH_MINE_DEPLETED.getType(), 0);
		}

		private void subtractItem(MineType type, Player player, EquipmentSlot hand) {
			PlayerInventory inventory = player.getInventory();
			ItemStack stack = inventory.getItem(hand);
			if (stack.getAmount() == 1) {
				// replace with corresponding depleted item
				stack = type.itemDepleted.clone();
				if (!player.hasCooldown(stack.getType()))
					player.setCooldown(stack.getType(), 1_000_000);
			} else {
				stack.subtract();
			}
			player.getInventory().setItem(hand, stack);
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(!event.getAction().isRightClick())
				return;

			final ItemStack usedItem = event.getItem();
			if (REMOTE_DETONATOR_ITEM.isSimilar(usedItem)) {
				Player demo = event.getPlayer();
				event.setUseItemInHand(Event.Result.DENY);
				event.setUseInteractedBlock(Event.Result.DENY); //prevent arming tnt
				DemoMine mine = (DemoMine) BuildingOutlineManager.getSelector(demo).getSelected();
				if (mine != null && !mine.isTriggered()) {
					mine.trigger(demo);
				}
			} else {
				Block clicked = event.getClickedBlock();
				if (clicked == null || usedItem == null)
					return;
				Block base = getMineBaseBlock(clicked, event.getBlockFace());
				Block block = base.getRelative(BlockFace.UP);

				MineType type = MineType.getFromItemStack(usedItem);
				if (type == null) // not valid mine
					return;

				event.setCancelled(true);
				if (checkMineLocation(base)) {
					if (BuildingManager.getBuildingAt(block) == null) {
						DemoMine mine = type.constructor.apply(event.getPlayer(), block);
						BuildingManager.placeBuilding(mine);

						subtractItem(type, event.getPlayer(), event.getHand());
					} else {
						Component message = Component.text("This block is already occupied", TextColors.ERROR_RED);
						PlayerUtils.sendKitMessage(event.getPlayer(), message, message);
					}
				} else {
					Component message = Component.text("You can't place a mine here!", TextColors.ERROR_RED);
					PlayerUtils.sendKitMessage(event.getPlayer(), message, message);
				}
			}
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if(event.getDamageType().is(DamageType.DEMO_TNTMINE) && event.getAttacker() instanceof TNTPrimed dTnt) {
				Player demo = (Player) event.getFinalAttacker();
				if(event.getVictim() instanceof Player victim) {
					TNTMine mine = TNTMine.getByTNT(demo, dTnt);
					if (mine != null) {
						if (mine.triggerer == demo) {
							event.setDamageType(DEMO_TNTMINE_REMOTE);
						}
						else if (mine.triggerer == victim) {
							event.setDamageType(DamageType.DEMO_TNTMINE);
						}
						else {
							event.setDamageType(DEMO_TNTMINE_BYSTANDER);
							event.setDamageTypeCause(mine.triggerer);
						}
						event.setFinalDamage(event.getFinalDamage() * 0.75);
					}
				}
			}
			// If the victim is the demo, then allow it for the sake of push mine-jumping.
			// However, don't allow it if the demo is carrying the flag in CTF
			else if(event.getDamageType().is(DamageType.DEMO_PUSHMINE) && event.getFinalAttacker() == event.getVictim()) {
				event.setCancelled(false);
				if(Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(event.getPlayerVictim())) {
					event.setCancelled(true);
					// Send them a message if they're probably mine jumping
					if(event.hasKnockback() && event.getKnockback().length() >= 1d) {
						event.getPlayerVictim().sendMessage(Component.text(
								"The flag is too heavy for you to mine-jump with!", TextColors.ERROR_RED));
					}
				}
			}
		}

		@Override
		public void onTick() {
			int now = TeamArena.getGameTick();

			// tick regenerating mines
			regeneratingMines.entrySet().removeIf(entry -> {
				Player player = entry.getKey();
				ItemStack cursorItem = player.getItemOnCursor();
				PlayerInventory inventory = player.getInventory();
				List<RegeneratingMine> regeneratingMines = entry.getValue();
				EnumMap<MineType, Integer> nextRegen = new EnumMap<>(MineType.class);

				regeneratingMines.removeIf(mine -> {
					if (now - mine.removedTime >= mine.type.timeToRegen) {
						// replace depleted item with stack
						ItemStack toGive = mine.type.item();
						// imagine holding the depleted item in your cursor
						if (cursorItem.isSimilar(mine.type.itemDepleted)) {
							player.setItemOnCursor(toGive);
						} else {
							int slot = inventory.first(mine.type.itemDepleted.getType());
							if (slot != -1) {
								inventory.setItem(slot, toGive);
							} else {
								inventory.addItem(toGive);
							}
						}
						return true;
					} else {
						int secsRemaining = (mine.type.timeToRegen - (now - mine.removedTime)) / 20;
						// only display the mine that regenerates the soonest
						nextRegen.merge(mine.type, secsRemaining, Math::min);
					}

					return false;
				});

				// update depleted items
				nextRegen.forEach((type, secs) -> {
					int quantity = secs + 1;
					if (cursorItem.getType() == type.itemDepleted.getType()) {
						if (cursorItem.getAmount() != quantity)
							player.setItemOnCursor(type.itemDepleted.asQuantity(quantity));
					} else {
						int slot = inventory.first(type.itemDepleted.getType());
						if (slot == -1) return;
						ItemStack old = inventory.getItem(slot);
						if (old == null || old.getAmount() != quantity)
							inventory.setItem(slot, type.itemDepleted.asQuantity(quantity));
					}
				});

				return regeneratingMines.isEmpty();
			});
		}

		@Override
		public void onTeamSwitch(Player player, @Nullable TeamArenaTeam oldTeam, @Nullable TeamArenaTeam newTeam) {

		}
		public static void addRegeneratingMine(Player player, MineType type, int startTime) {
			List<RegeneratingMine> playerRegeneratingMines = regeneratingMines.computeIfAbsent(player,
					player1 -> new ArrayList<>(TNT_MINE_COUNT + PUSH_MINE_COUNT));

			playerRegeneratingMines.add(new RegeneratingMine(type, startTime));

			Component message = Component.textOfChildren(
				Component.text("You'll get your "),
				type.displayName(),
				Component.text(" back in " + (type.timeToRegen / 20) + " seconds")
			).color(NamedTextColor.AQUA);

			PlayerUtils.sendKitMessage(player, message, message);

			Material depletedItem = type.itemDepleted.getType();
			if (!player.hasCooldown(depletedItem) || player.getCooldown(depletedItem) > type.timeToRegen) {
				player.setCooldown(depletedItem, type.timeToRegen);
			}
		}
	}
}

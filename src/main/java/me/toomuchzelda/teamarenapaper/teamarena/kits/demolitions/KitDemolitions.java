package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.building.Building;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingOutlineManager;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingSelector;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;


public class KitDemolitions extends Kit
{
	public static final int TNT_MINE_COUNT = 2;
	public static final int PUSH_MINE_COUNT = 1;
	public static final TextColor TNT_COLOR = TextColor.color(187, 60, 23);
	public static final ItemStack TNT_MINE_ITEM;
	public static final ItemStack PUSH_MINE_ITEM;
	public static final ItemStack REMOTE_DETONATOR_ITEM;

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

		Style style = Style.style(TextUtils.RIGHT_CLICK_TO).decoration(TextDecoration.ITALIC, false);
		String strUsage = "Right click the top of a block to place the trap down. " +
				"It will be triggered by your remote detonator or when enemies step on it.";
		List<Component> usage = TextUtils.wrapString(strUsage, style, 200);

		TNT_MINE_ITEM = new ItemStack(Material.TNT, TNT_MINE_COUNT);
		ItemMeta meta = TNT_MINE_ITEM.getItemMeta();

		meta.displayName(ItemUtils.noItalics(Component.text("TNT Mine", TNT_COLOR)));

		List<Component> lore = new ArrayList<>();
		lore.addAll(TextUtils.wrapString("A TNT landmine trap that blows enemies to smithereens",
				Style.style(TNT_COLOR).decoration(TextDecoration.ITALIC, false), 200));
		lore.addAll(usage);
		meta.lore(lore);
		TNT_MINE_ITEM.setItemMeta(meta);

		PUSH_MINE_ITEM = new ItemStack(Material.WHITE_WOOL, PUSH_MINE_COUNT);
		meta = PUSH_MINE_ITEM.getItemMeta();
		meta.displayName(ItemUtils.noItalics(Component.text("Push Mine")));
		lore = new ArrayList<>();
		lore.addAll(TextUtils.wrapString("A trap that creates an explosive gust of air, pushing away all enemies near it"
				, Style.empty().decoration(TextDecoration.ITALIC, false), 200));
		lore.addAll(usage);
		meta.lore(lore);
		PUSH_MINE_ITEM.setItemMeta(meta);

		REMOTE_DETONATOR_ITEM = new ItemStack(Material.FLINT_AND_STEEL);
		meta = REMOTE_DETONATOR_ITEM.getItemMeta();
		meta.displayName(ItemUtils.noItalics(Component.text("Remote Trigger", NamedTextColor.BLUE)));
		lore = new ArrayList<>(TextUtils.wrapString("Point at any of your mines from any distance to select one. Once selected, it will turn blue. Right click and boom!", style, 200));
		meta.lore(lore);
		REMOTE_DETONATOR_ITEM.setItemMeta(meta);
	}

	private static void setValidMineBlock(Material mat) {
		VALID_MINE_BLOCKS[mat.ordinal()] = true;
	}

	public KitDemolitions() {
		super("Demolitions", "This kit comes with traps! Specifically, those that explode when people step on them. " +
				"They also blend in with the ground no matter the color!\n\n" +
				"It comes with a TNT trap that does lots of damage " +
				"and a Push Mine trap that sends enemies flying!\n\n" +
				"After placing them down, enemies can step on them to trigger them or they can be triggered remotely " +
				"by the kit user.",
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

	public record RegeneratingMine(MineType type, int removedTime) {}

	public static class DemolitionsAbility extends Ability
	{
		public static final Map<Player, List<RegeneratingMine>> REGENERATING_MINES = new LinkedHashMap<>();

		public static final DamageType DEMO_TNTMINE_BYSTANDER = new DamageType(DamageType.DEMO_TNTMINE,
				"%Killed% was blown up by %Killer%'s TNT Mine because %Cause% stepped on it. Thanks a lot!");

		public static final DamageType DEMO_TNTMINE_REMOTE = new DamageType(DamageType.DEMO_TNTMINE,
				"%Killed% was blown up by %Killer%'s TNT Mine remotely");


		@Override
		public void unregisterAbility() {
			REGENERATING_MINES.clear();
		}

		private static final Component RCLICK_PLACE = Component.text("Right click: place mine", TextUtils.RIGHT_CLICK_TO);
		private static final Component RCLICK_DETONATE = Component.text("Right click: detonate selected mine", TextUtils.RIGHT_CLICK_TO);
		private static final Predicate<Building> BUILDING_FILTER =
			building -> building instanceof DemoMine demoMine &&
				demoMine.isArmed() && !demoMine.isTriggered();
		@Override
		public void giveAbility(Player player) {
			BuildingOutlineManager.registerSelector(player, new BuildingSelector(
				Map.of(
					REMOTE_DETONATOR_ITEM, List.of(BuildingSelector.Action.selectBuilding(RCLICK_DETONATE, BUILDING_FILTER, BUILDING_FILTER))//,
					// TODO preview landmines
//					TNT_MINE_ITEM, List.of(BuildingSelector.Action.showPreview(RCLICK_PLACE, TNTMine.class, p -> new TNTMine(p, p.getLocation().getBlock()), null)),
//					PUSH_MINE_ITEM, List.of(BuildingSelector.Action.showPreview(RCLICK_PLACE, PushMine.class, p -> new PushMine(p, p.getLocation().getBlock()), null))
				)
			));
		}

		@Override
		public void removeAbility(Player player) {
			BuildingOutlineManager.unregisterSelector(player);
			BuildingManager.getAllPlayerBuildings(player).forEach(building -> {
				if (building instanceof DemoMine) {
					BuildingManager.destroyBuilding(building);
				}
			});
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(!event.getAction().isRightClick())
				return;

			Material mat = event.getMaterial();
			int type = 0;
			if(TNT_MINE_ITEM.getType() == mat)
				type = 1;
			else if(PUSH_MINE_ITEM.getType() == mat)
				type = 2;

			Block block = event.getClickedBlock();
			if (type > 0 && block != null && event.getBlockFace() == BlockFace.UP) {
				event.setUseItemInHand(Event.Result.DENY);
				if(isValidMineBlock(block)) {
					if (BuildingManager.canPlaceAt(block.getRelative(BlockFace.UP))) {
						if (type == 1) { //tnt mine
							DemoMine mine = new TNTMine(event.getPlayer(), block);
							BuildingManager.placeBuilding(mine);
						}
						else { //push mine
							DemoMine mine = new PushMine(event.getPlayer(), block);
							BuildingManager.placeBuilding(mine);
						}

						event.getItem().subtract();
					}
					else {
						final Component message = Component.text("A Mine has already been placed here",
								TextColors.ERROR_RED);
						PlayerUtils.sendKitMessage(event.getPlayer(), message, message);
					}
				}
				else {
					final Component message = Component.text("You can't place a Mine here", TextColors.ERROR_RED);
					PlayerUtils.sendKitMessage(event.getPlayer(), message, message);
//					event.getPlayer().updateInventory(); // Refresh the 'placed' block
				}
			}
			else if(mat == REMOTE_DETONATOR_ITEM.getType()) {
				Player demo = event.getPlayer();
				event.setUseItemInHand(Event.Result.DENY);
				event.setUseInteractedBlock(Event.Result.DENY); //prevent arming tnt
				DemoMine mine = (DemoMine) BuildingOutlineManager.getSelector(demo).getSelected();
				if(mine != null) {
					mine.trigger(demo);
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
			final int gameTick = TeamArena.getGameTick();

			//tick regenerating mines
			{
				var regPlayersIter = REGENERATING_MINES.entrySet().iterator();
				while (regPlayersIter.hasNext()) {
					var entry = regPlayersIter.next();

					var regMinesIter = entry.getValue().iterator();
					while (regMinesIter.hasNext()) {
						RegeneratingMine regMine = regMinesIter.next();

						//check if time up and give back one mine here
						if (gameTick - regMine.removedTime() >= regMine.type().timeToRegen) {
							Player owner = entry.getKey();
							ItemStack mineItem;
							if (regMine.type() == MineType.TNTMINE) {
								mineItem = TNT_MINE_ITEM;
							}
							else {
								mineItem = PUSH_MINE_ITEM;
							}

							owner.getInventory().addItem(mineItem.asOne());

							regMinesIter.remove();
						}
					}

					if (entry.getValue().size() == 0)
						regPlayersIter.remove();
				}
			}
		}

		@Override
		public void onTeamSwitch(Player player, @Nullable TeamArenaTeam oldTeam, @Nullable TeamArenaTeam newTeam) {

		}
		public static void addRegeneratingMine(Player player, MineType type, int startTime) {
			List<RegeneratingMine> regenningMines = REGENERATING_MINES.computeIfAbsent(player,
					player1 -> new ArrayList<>(TNT_MINE_COUNT + PUSH_MINE_COUNT));

			regenningMines.add(new RegeneratingMine(type, startTime));

			final Component message = Component.text("You'll get " + type.name + " back in " + (type.timeToRegen / 20) + " seconds",
					NamedTextColor.AQUA);

			PlayerUtils.sendKitMessage(player, message, message);
		}
	}
}

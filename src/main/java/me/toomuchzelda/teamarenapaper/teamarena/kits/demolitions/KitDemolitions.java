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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


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

		List<Component> usage = TextUtils.wrapString("Right click the top of a block to place the trap down. " +
				"It will be triggered by your remote detonator or when enemies step on it.",
			Style.style(TextUtils.RIGHT_CLICK_TO), 200);

		TNT_MINE_ITEM = ItemBuilder.of(Material.TNT)
			.amount(TNT_MINE_COUNT)
			.displayName(Component.text("TNT Mine", TNT_COLOR))
			.lore(TextUtils.wrapString("A TNT landmine trap that blows enemies to smithereens",
				Style.style(TNT_COLOR), 200))
			.addLore(usage)
			.build();

		PUSH_MINE_ITEM = ItemBuilder.of(Material.WHITE_WOOL)
			.amount(PUSH_MINE_COUNT)
			.displayName(Component.text("Push Mine"))
			.lore(TextUtils.wrapString("A trap that creates an explosive gust of air, pushing away all enemies near it",
				Style.empty().decoration(TextDecoration.ITALIC, false), 200))
			.addLore(usage)
			.build();

		REMOTE_DETONATOR_ITEM = ItemBuilder.of(Material.FLINT_AND_STEEL)
			.displayName(Component.text("Remote Trigger", NamedTextColor.BLUE))
			.lore(TextUtils.wrapString("Point at any of your mines from any distance to select one. " +
				"Once selected, it will turn blue. Right click and boom!", Style.style(TextUtils.RIGHT_CLICK_TO), 200))
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

		private static final Map<ItemStack, BuildingSelector.Action> SELECTOR_ACTION = Map.of(
			REMOTE_DETONATOR_ITEM, BuildingSelector.Action.selectBuilding(RCLICK_DETONATE, building -> building instanceof DemoMine demoMine &&
				demoMine.isArmed() && !demoMine.isTriggered()),
			TNT_MINE_ITEM, BuildingSelector.Action.showBlockPreview(RCLICK_PLACE, TNTMine.class, TNTMine::new, null),
			PUSH_MINE_ITEM, BuildingSelector.Action.showBlockPreview(RCLICK_PLACE, PushMine.class, PushMine::new, null)
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
			REGENERATING_MINES.remove(player);
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(!event.getAction().isRightClick())
				return;

			Material mat = event.getMaterial();
			if (mat == REMOTE_DETONATOR_ITEM.getType()) {
				Player demo = event.getPlayer();
				event.setUseItemInHand(Event.Result.DENY);
				event.setUseInteractedBlock(Event.Result.DENY); //prevent arming tnt
				DemoMine mine = (DemoMine) BuildingOutlineManager.getSelector(demo).getSelected();
				if (mine != null && !mine.isTriggered()) {
					mine.trigger(demo);
				}
			} else {
				ItemStack stack = event.getItem();
				Block base = event.getClickedBlock();
				if (base == null || stack == null)
					return;
				Block block = base.getRelative(BlockFace.UP);

				MineType type;
				if (TNT_MINE_ITEM.getType() == mat)
					type = MineType.TNTMINE;
				else if (PUSH_MINE_ITEM.getType() == mat)
					type = MineType.PUSHMINE;
				else
					return;

				event.setCancelled(true);
				if (isValidMineBlock(base)) {
					if (BuildingManager.getBuildingAt(block) == null) {
						DemoMine mine = type.constructor.apply(event.getPlayer(), block);
						BuildingManager.placeBuilding(mine);

						stack.subtract();
					} else {
						Component message = Component.text("This block is already occupied", TextColors.ERROR_RED);
						PlayerUtils.sendKitMessage(event.getPlayer(), message, message);
					}
				} else {
					Component message = Component.text("You can't place a mine here", TextColors.ERROR_RED);
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
							} else {
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

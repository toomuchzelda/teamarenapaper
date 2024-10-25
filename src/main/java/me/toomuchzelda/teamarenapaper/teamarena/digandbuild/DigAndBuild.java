package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.explosions.ExplosionManager;
import me.toomuchzelda.teamarenapaper.explosions.VanillaExplosionInfo;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades.TeamUpgrades;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades.UpgradeBase;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades.UpgradeSpawnState;
import me.toomuchzelda.teamarenapaper.teamarena.map.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy.SearchAndDestroy;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DigAndBuild extends TeamArena
{
	/**
	 * Items marked with this marker can be put in chests
	 */
	public static final NamespacedKey ITEM_MARKER = new NamespacedKey(Main.getPlugin(), "dig_and_build_item");
	public static boolean isDNBItem(ItemStack stack) {
		return stack.getItemMeta().getPersistentDataContainer().has(ITEM_MARKER);
	}

	private static final Component GAME_NAME = Component.text("Dig and Build", NamedTextColor.DARK_GREEN);
	private static final Component HOW_TO_PLAY = Component.text("Make your way around the map and break the enemies' " +
		"Life Ore!!", NamedTextColor.DARK_GREEN);

	// MESSAGES

	public static final Style CORE_STYLE = Style.style(TextColor.color(0x63bb83), TextDecoration.BOLD);
	public static final Component CORE = Component.text("⭐ Core", CORE_STYLE);

	private record YourOreMinedContext(@NotNull Component miner) {}
	private static final CompileSafePlaceholder<YourOreMinedContext> YOUR_ORE_MINED_TITLE = ctx -> Component.textOfChildren(
		ctx.miner, Component.text(" is mining your "),
		CORE, Component.text("!")
	).color(NamedTextColor.GOLD);
	private static final CompileSafePlaceholder<YourOreMinedContext> YOUR_ORE_MINED_CHAT = ctx -> Component.textOfChildren(
		Component.text("Your "), CORE,
		Component.text(" is being mined by "), ctx.miner, Component.text("!")
	).color(NamedTextColor.GOLD);

	private record TeamsOreMinedContext(@NotNull Component miner, @NotNull Component team) {}
	private static final CompileSafePlaceholder<TeamsOreMinedContext> TEAMS_ORE_MINED_CHAT = ctx -> Component.textOfChildren(
		ctx.miner, Component.text(" damaged "),
		ctx.team, Component.text("'s "),
		CORE, Component.text("!")
	).color(NamedTextColor.GOLD);

	private record TeamsOreDestroyedTitleContext(@NotNull Component miner, @NotNull Component minerTeamColoredPickaxe,
												 @NotNull Component team, @NotNull Component teamColoredCore) {}
	private static final CompileSafePlaceholder<TeamsOreDestroyedTitleContext> TEAMS_ORE_DESTROYED_TITLE = ctx -> Component.textOfChildren(
		ctx.miner, Component.space(), ctx.minerTeamColoredPickaxe, Component.space(), ctx.team, Component.space(), ctx.teamColoredCore
	);
	private record TeamsOreDestroyedChatContext(@NotNull Component team, @NotNull Component miner) {}
	private static final CompileSafePlaceholder<TeamsOreDestroyedChatContext> TEAMS_ORE_DESTROYED_CHAT = ctx -> Component.textOfChildren(
		ctx.team, Component.text("'s "), CORE,
		Component.text(" has been destroyed by "), ctx.miner,
		Component.text("! They will no longer respawn!")
	).color(NamedTextColor.GOLD);

	private record TeamDefeatedContext(@NotNull Component team) {}
	private static final CompileSafePlaceholder<TeamDefeatedContext> TEAM_DEFEATED = ctx -> Component.textOfChildren(
		ctx.team, Component.text(" has been defeated!", NamedTextColor.GOLD)
	);

	private static final Component YOU_WONT_RESPAWN = Component.text("You will no longer respawn!", TextColors.ERROR_RED);

	private static final Component FASTER_TOOLS_CHAT = Component.text("You're taking too long! Everyone gets faster tools", TextColors.ERROR_RED);
	private static final Component FASTER_TOOLS_TITLE = Component.text("Everyone gets faster tools!", TextColors.ERROR_RED);
	// END MESSAGES

	private static final int EFF_TIME = 3 * 60 * 20; // Time until enchantments are given. Anti stall measure.
	private static final int EFF_ACTIVE = 0;
	private static final Map<Enchantment, Integer> DEFAULT_EFF_ENCHANTS = Map.of(
		Enchantment.EFFICIENCY, 2
	);

	private static final int TICKS_PER_GAIN_BLOCK = 30;
	private static final int TICKS_PER_LOSE_BLOCK = 40;
	private static final int MAX_BLOCK_COUNT = 16;

	private static final int TEAM_DEAD_SCORE = 1;
	private static final int TEAM_LASTMAN_SCORE = 2;

	private Vector middle;
	private final PointMarker midMarker;

	private List<ItemStack> defaultTools;
	private List<ItemStack> defaultBlocks;
	private List<IntBoundingBox> noBuildZones;
	private static class BlockTimes {
		static final int NO_TIME = -1;
		int blockPlaceTime;
		int blockBreakTime;
		public BlockTimes() { this.blockPlaceTime = NO_TIME; this.blockBreakTime = NO_TIME; }
	}
	private final Map<Player, @Nullable BlockTimes> blockTimes;

	private DigAndBuildInfo mapInfo;

	private Map<TeamArenaTeam, TeamLifeOres> teamOres;
	private Map<Block, TeamArenaTeam> oreToTeamLookup;
	private Map<Block, TeamArenaTeam> chestLookup;

	private List<PointMarker> pointMarkers;

	private List<UpgradeBase> upgrades;
	// upgrade dispatcher and state manager for individual teams
	private Map<TeamArenaTeam, TeamUpgrades> teamUpgrades;

	private List<UpgradeSpawnState> upgradeSpawners;

	/** If players can join after the game has started.
	 *  Players won't be allowed to join after an Ore has been broken */
	private boolean canJoinMidGame = true;

	private int effTime; // Game timestamp of when eff2 will be given

	public DigAndBuild(TeamArenaMap map) {
		super(map);

		// Make player nametags visible to teammates only
		for (TeamArenaTeam team : this.teams) {
			team.setNametagVisible(Team.OptionStatus.FOR_OWN_TEAM);
		}

		this.midMarker = new PointMarker(middle.toLocation(this.gameWorld), Component.text("Middle"), Color.WHITE,
			Material.LIGHTNING_ROD);
		this.blockTimes = new HashMap<>();
	}

	private final Map<TeamArenaTeam, Component> sidebarCache = new LinkedHashMap<>();
	@Override
	public void updateSidebar(Player player, SidebarManager sidebar) {
		var playerTeam = Main.getPlayerInfo(player).team;
		sidebar.setTitle(player, getGameName());

		int teamsShown = 0;

		for (var entry : sidebarCache.entrySet()) {
			var team = entry.getKey();
			Component line = entry.getValue();

			if (teamsShown >= 4 && team != playerTeam)
				continue; // don't show
			teamsShown++;
			if (team == playerTeam) {
				sidebar.addEntry(Component.textOfChildren(OWN_TEAM_PREFIX, line));
			} else {
				sidebar.addEntry(line);
			}
		}
		// unimportant teams
		if (sidebarCache.size() != teamsShown)
			sidebar.addEntry(Component.text("+ " + (sidebarCache.size() - teamsShown) + " teams", NamedTextColor.GRAY));
		sidebar.addEntry(Component.empty());
		sidebar.addEntry(Component.text("$ Team Resources", NamedTextColor.YELLOW));
		sidebar.addEntry(Component.text("  ur team broke bro ☠", NamedTextColor.GRAY));
	}

	@Override
	public Collection<Component> updateSharedSidebar() {
		sidebarCache.clear();
		// sort by ascending health, or if health = 0, by ascending player count
		var aliveCounts = teamOres.keySet().stream()
			.collect(Collectors.toMap(Function.identity(), team -> {
				int alive = 0;
				for (var player : team.getPlayerMembers())
					if (!isDead(player))
						alive++;
				return alive;
			}));

        List<Map.Entry<TeamArenaTeam, TeamLifeOres>> sorted = new ArrayList<>(teamOres.entrySet());
		sorted.sort(Comparator.comparingInt(entry -> entry.getValue().getHealth() == 0 ?
			-aliveCounts.get(entry.getKey()) :
			entry.getValue().getHealth()));
		for (Map.Entry<TeamArenaTeam, TeamLifeOres> entry : sorted) {
			var team = entry.getKey();
			if (team.score == TEAM_DEAD_SCORE)
				continue;
			var ore = entry.getValue();

			sidebarCache.put(entry.getKey(), Component.textOfChildren(
				team.getComponentName(),
				Component.text(": "),
				ore.getHealth() != 0 ?
					formatOreHealth(ore.getHealth()) :
					Component.text(aliveCounts.get(team) + " alive", NamedTextColor.DARK_RED)
			));
		}

		Component antiStallAction = Component.text("Faster tools", TextColor.color(PotionEffectType.HASTE.getColor().asRGB()));
		return List.of(Component.text("Last to stand", NamedTextColor.GRAY),
			effTime == EFF_ACTIVE ?
				Component.textOfChildren(antiStallAction, Component.text(" active")) :
				Component.textOfChildren(
					antiStallAction,
					Component.text(" in "),
					TextUtils.formatDurationMmSs(Ticks.duration(effTime - getGameTick()))
				));
	}

	private static TextComponent formatOreHealth(int health) {
		if (health < TeamLifeOres.STARTING_HEALTH) {
			float percentage = (float) health / TeamLifeOres.STARTING_HEALTH;
			return Component.text(health + "⛏",
				percentage < 0.5f ?
					TextColor.lerp(percentage * 2, NamedTextColor.DARK_RED, NamedTextColor.YELLOW) :
					TextColor.lerp((percentage - 0.5f) * 2, NamedTextColor.YELLOW, NamedTextColor.GREEN));
		} else {
			int extra = health - TeamLifeOres.STARTING_HEALTH;
			if (extra != 0)
				return Component.textOfChildren(
					Component.text(TeamLifeOres.STARTING_HEALTH, NamedTextColor.BLUE),
					Component.text(" + ", NamedTextColor.GRAY),
					Component.text(extra + "⛏", TextColors.ABSORPTION_HEART)
				);
			else
				return Component.text(TeamLifeOres.STARTING_HEALTH + "⛏", NamedTextColor.BLUE);
		}
	}

	@Override
	protected void loadConfig(TeamArenaMap map) {
		super.loadConfig(map);

		DigAndBuildInfo mapInfo = map.getDnbInfo();
		this.mapInfo = mapInfo;

		this.middle = mapInfo.middle;
		this.spawnPos = mapInfo.middle.toLocation(this.gameWorld);

		var toolLore = Component.text("Get digging!");
		defaultTools = mapInfo.defaultTools.stream()
			.map(mat -> ItemBuilder.of(mat)
				.lore(toolLore)
				.build())
			.toList();

		var blockLore = Component.text("Get building!");
		defaultBlocks = mapInfo.defaultBlocks.stream()
			.map(mat -> ItemBuilder.of(mat)
				.lore(blockLore)
				.setPdc(ITEM_MARKER, PersistentDataType.BOOLEAN, true)
				.build())
			.toList();

		// Make a copy of bounding boxes. May be able to use the provided list as-is instead.
		noBuildZones = List.copyOf(mapInfo.noBuildZones);

		this.pointMarkers = new ArrayList<>();

		this.teamOres = new HashMap<>();
		this.chestLookup = new HashMap<>();
		this.oreToTeamLookup = new HashMap<>();
		this.teamUpgrades = new HashMap<>();

		this.upgrades = new ArrayList<>();
		this.upgradeSpawners = new ArrayList<>();

		if (mapInfo.healUpgrade != null) {
			upgrades.add(mapInfo.healUpgrade);
			if (mapInfo.healUpgrade.spawns() != null) {
				upgradeSpawners.add(new UpgradeSpawnState(gameWorld, mapInfo.healUpgrade.makeItemStack(), mapInfo.healUpgrade.spawns()));
			}
		}
		if (mapInfo.hasteUpgrade != null) {
			upgrades.add(mapInfo.hasteUpgrade);
			if (mapInfo.hasteUpgrade.spawns() != null) {
				upgradeSpawners.add(new UpgradeSpawnState(gameWorld, mapInfo.hasteUpgrade.makeItemStack(), mapInfo.hasteUpgrade.spawns()));
			}
		}
		if (mapInfo.trapUpgrade != null) {
			upgrades.add(mapInfo.trapUpgrade);
		}

		BlockData defaultLifeOreBlock = mapInfo.defaultLifeOreBlock != null ? mapInfo.defaultLifeOreBlock : Material.BEACON.createBlockData();

		for (var entry : mapInfo.teams.entrySet()) {
			TeamArenaTeam team = this.getTeamByLegacyConfigName(entry.getKey());
			var teamInfo = entry.getValue();

			TeamLifeOres lifeOre = new TeamLifeOres(gameWorld, team, defaultLifeOreBlock, teamInfo.lifeOres());
			this.teamOres.put(team, lifeOre);
			for (Block block : lifeOre.getLifeOres().keySet()) {
				oreToTeamLookup.put(block, team);
			}

			TeamUpgrades upgrades = new TeamUpgrades(this, team, mapInfo);
			this.teamUpgrades.put(team, upgrades);

			BlockCoords chestCoords = teamInfo.chest();
			if (chestCoords != null) {
				PointMarker marker = new PointMarker(chestCoords.toLocation(gameWorld).add(0.5d, 2.2d, 0.5d),
					team.getComponentName().append(Component.text("'s chest", team.getRGBTextColor())),
					team.getColour(), Material.CHEST, true);
				this.pointMarkers.add(marker);

				this.chestLookup.put(chestCoords.toBlock(gameWorld), team);
			}
		}
	}

	public boolean isUpgradeItem(ItemStack stack) {
		return getUpgradeFromItem(stack) != null;
	}

	@Contract("null -> null")
	public UpgradeBase getUpgradeFromItem(ItemStack stack) {
		if (stack == null)
			return null;
		Material material = stack.getType();
		for (UpgradeBase upgrade : upgrades) {
			if (upgrade.item() == material)
				return upgrade;
		}
		return null;
	}

	public DigAndBuildInfo getMapInfo() {
		return mapInfo;
	}

	public TeamLifeOres getTeamLifeOre(TeamArenaTeam team) {
		return teamOres.get(team);
	}

	public TeamUpgrades getTeamUpgrades(TeamArenaTeam team) {
		return teamUpgrades.get(team);
	}

	@Nullable
	public IntBoundingBox findNoBuildZone(BlockCoords coords) {
		for (IntBoundingBox noBuildZone : this.noBuildZones) {
			if (noBuildZone.contains(coords)) {
				return noBuildZone;
			}
		}
		return null;
	}

	// Prevent placing buildings in no-build zones
	@Override
	public boolean canBuildAt(Block block) {
		if (!super.canBuildAt(block))
			return false;
		Location blockLocation = block.getLocation();
		for (TeamLifeOres teamLifeOres : teamOres.values()) {
			if (teamLifeOres.findNearbyProtectedLifeOre(blockLocation) != null)
				return false;
		}

		BlockCoords coords = new BlockCoords(block);
		return findNoBuildZone(coords) == null;
	}

	/**
	 * Handle block breaking.
	 * Prevent breaking blocks in no-build-zones and near the life ores. Also handle breaking the life ore block.
	 */
	@Override
	protected boolean onBreakBlockSub(BlockBreakEvent event) {
		if (event.isCancelled())
			return true;

		final Block block = event.getBlock();
		final BlockCoords coords = new BlockCoords(block);
		Player breaker = event.getPlayer();
		if (isDead(breaker))
			return true;

		Location blockLocation = block.getLocation();
		for (TeamLifeOres teamLifeOres : teamOres.values()) {
			if (teamLifeOres.isLifeOre(block)) {
				event.setCancelled(true);
				handleLifeOreBreak(event, block, teamLifeOres, breaker);
				return true;
			}
			Block ore = teamLifeOres.findNearbyProtectedLifeOre(blockLocation);
			if (ore != null) {
				event.setCancelled(true);
				teamLifeOres.playDenyBuildEffect(breaker, block, ore, true);
				return true;
			}
		}

		IntBoundingBox nbz = findNoBuildZone(coords);
		if (nbz != null) {
			event.setCancelled(true);
			playNoBuildEffect(block, event.getPlayer(), nbz);
			return true;
		}

		for (UpgradeSpawnState upgradeSpawner : upgradeSpawners) {
			if (upgradeSpawner.isOreBlock(block)) {
				event.setCancelled(true);
				upgradeSpawner.onBreak(block, breaker);
				return true;
			}
		}

		// Give them an extra block
		this.giveBlockOnBreak(block, event.getPlayer());

		return true;
	}

	// Delay time until anti stall
	private void delayAntiStall() {
		if (this.effTime != EFF_ACTIVE)
			this.effTime += 7 * 20;
	}

	private void handleLifeOreBreak(BlockBreakEvent event, Block block, TeamLifeOres ore, Player breaker) {
		final TeamLifeOres.OreBreakResult result = ore.onBreak(breaker, block);
		switch (result) {
			// no-ops
			case null -> {}
			case ALREADY_DEAD -> {}
			case DAMAGED_BY_TEAMMATE -> ore.playDenyBuildEffect(breaker, block, block, true);
			case DAMAGED_BY_ENEMY -> {
				Location loc = block.getLocation();
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (p != breaker && EntityUtils.distanceSqr(p, loc) <= 30d * 30d) {
						ParticleUtils.blockBreakEffect(p, block);
					}
				}

				if (ore.getHealth() != 0) {
					this.announceOreDamaged(ore);
				} else {
					// broken
					for (Player loser : ore.getTeam().getPlayerMembers()) {
						if (this.respawnTimers.remove(loser) != null) {
							loser.sendActionBar(YOU_WONT_RESPAWN);
						}
						SpectatorAngelManager.removeAngel(loser);
					}
					this.announceOreKilled(ore);

					this.canJoinMidGame = false; // Prevent players from joining the game from now on.
				}

				ore.clearMiners();
				delayAntiStall();
			}
		}
	}

	/** Find the right type of block to give upon breaking a block. If none can be found then default to first
	 *  one in this.blocks.
	 *  Does not handle status ore blocks */
	private void giveBlockOnBreak(Block block, Player breaker) {
		final Material brokenBlockType = block.getType();
		// Don't give blocks for instant-breakable and other misc blocks.
		if (BreakableBlocks.isBlockBreakable(brokenBlockType)) return;
		if (!block.isSolid()) return;

		BlockTimes times = this.blockTimes.computeIfAbsent(breaker, player -> new BlockTimes());
		times.blockBreakTime = TeamArena.getGameTick();

		for (ItemStack stack : this.defaultBlocks) {
			if (stack.getType() == brokenBlockType) {
				breaker.getInventory().addItem(stack.asOne());
				return;
			}
		}

		breaker.getInventory().addItem(this.defaultBlocks.getFirst().asOne());
	}

	/**
	 * Handle block placement. Don't place blocks in no build zones, near life ores.
	 */
	@Override
	public void onPlaceBlock(BlockPlaceEvent event) {
		final Player placer = event.getPlayer();
		final Block block = event.getBlock();
		BlockCoords coords = new BlockCoords(block);

		IntBoundingBox nbz = findNoBuildZone(coords);
		if (nbz != null) {
			event.setCancelled(true);
			playNoBuildEffect(block, placer, nbz);
			return;
		}

		Location location = block.getLocation();
		for (TeamLifeOres teamLifeOres : teamOres.values()) {
			Block ore = teamLifeOres.findNearbyProtectedLifeOre(location);
			if (ore != null) {
				event.setCancelled(true);
				teamLifeOres.playDenyBuildEffect(placer, block, ore, false);
				return;
			}
		}

		// Event not cancelled if reached here
		// Set the timer for next block regen
		this.blockTimes.computeIfAbsent(placer, player -> new BlockTimes()).blockPlaceTime = TeamArena.getGameTick();

		if (mapInfo.specialInstantlyPrimeTnt && block.getType() == Material.TNT) {
			block.setType(Material.AIR);
			var tnt = gameWorld.spawn(block.getLocation().add(0.5, 0, 0.5), TNTPrimed.class, tntPrimed -> {
				tntPrimed.setFuseTicks(80);
				tntPrimed.setSource(placer);
			});
			ExplosionManager.setEntityInfo(tnt,
				new VanillaExplosionInfo(false, VanillaExplosionInfo.FireMode.NO_FIRE,
					VanillaExplosionInfo.DEFAULT_FLOAT_VALUE, VanillaExplosionInfo.DEFAULT_FLOAT_VALUE, true,
					broken -> {
						Location blockLocation = broken.getLocation();
						for (TeamLifeOres teamLifeOres : teamOres.values()) {
							Block ore = teamLifeOres.findNearbyProtectedLifeOre(blockLocation);
							if (ore != null) {
								return true;
							}
						}

						IntBoundingBox noBuildZone = findNoBuildZone(new BlockCoords(broken));
						return noBuildZone != null;
					}));
		} else if (mapInfo.specialReplaceWoolWithTeamColor && block.getType() == Material.WHITE_WOOL) {
			DyeColor dyeColor = Main.getPlayerInfo(placer).team.getDyeColour();
			Material teamWool = Objects.requireNonNull(Material.getMaterial(dyeColor.name() + "_WOOL"));
			block.setType(teamWool);
		}
	}

	@Override
	public void onInteract(final PlayerInteractEvent event) {
		super.onInteract(event);

		if (event.useItemInHand() == Event.Result.DENY) return;
		if (event.useInteractedBlock() == Event.Result.DENY) return;
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		final Player clicker = event.getPlayer();
		if (this.isDead(clicker)) return;

		// Clicked a team chest
		TeamArenaTeam clickedChestTeam = this.chestLookup.get(event.getClickedBlock());
		if (clickedChestTeam != null) {
			// Checking if player is dead is done in super and sets useInteractedBlock to DENY
			assert CompileAsserts.OMIT || !this.isDead(clicker);
			if (Main.getPlayerInfo(clicker).team != clickedChestTeam) {
				event.setUseInteractedBlock(Event.Result.DENY);
				clicker.playSound(clicker, Sound.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1f, 1.7f);
			}

			return;
		}

		TeamArenaTeam team = Main.getPlayerInfo(clicker).team;
		TeamUpgrades upgrades = teamUpgrades.get(team);

		if (upgrades.onInteract(clicker, event)) {
			//noinspection UnnecessaryReturnStatement // shut up
			return;
		}
	}

	public static boolean canMoveInChests(ItemStack stack) {
		return stack == null || stack.isEmpty() || isDNBItem(stack);
	}

	public boolean handleInventoryClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		Inventory inventory = event.getInventory();
		if (!(inventory.getHolder() instanceof Chest chest))
			return false;
		TeamArenaTeam team = chestLookup.get(chest.getBlock());
		if (team == null)
			return false;

		if (Main.getPlayerInfo(player).team != team) {
			event.setCancelled(true);
			return true;
		}

		if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
			// goofy ahh edge case
			if (!(canMoveInChests(event.getCurrentItem()) &&
				canMoveInChests(player.getInventory().getItem(event.getHotbarButton())))) {
				event.setCancelled(true);
				return true;
			}
		} else {
			if (!canMoveInChests(event.getCurrentItem())) {
				event.setCancelled(true);
				return true;
			}
		}

		return true;
	}

	/** Broadcast ore damage */
	private void announceOreDamaged(TeamLifeOres ore) {
		final Component oreMinerShort = ore.getMinerComponent(true);
		final Component oreMinerLong = ore.getMinerComponent(false);
		Component teamName = ore.getTeam().getComponentSimpleName();

		Component yourTitle = YOUR_ORE_MINED_TITLE.apply(new YourOreMinedContext(oreMinerShort));
		Component yourChat = YOUR_ORE_MINED_CHAT.apply(new YourOreMinedContext(oreMinerLong));

		Component teamTitle = TEAMS_ORE_MINED_CHAT.apply(new TeamsOreMinedContext(oreMinerLong, teamName));

		var iter = Main.getPlayersIter();
		while (iter.hasNext()) {
			var entry = iter.next();
			final Player p = entry.getKey();
			final PlayerInfo pinfo = entry.getValue();

			if (pinfo.team == ore.getTeam()) {
				if (pinfo.getPreference(Preferences.RECEIVE_GAME_TITLES))
					PlayerUtils.sendTitle(p, Component.empty(), yourTitle, 1, 10, 10);

				p.sendMessage(yourChat);

				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.AMBIENT, 1f, 1f);
			}
			else {
				p.sendMessage(teamTitle);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 0.5f, 0.6f);
			}
		}
	}

	private void announceOreKilled(TeamLifeOres ore) {
		final Component oreDestroyer = ore.getMinerComponent(false);
		Component teamName = ore.getTeam().getComponentSimpleName();

		final Component title = TEAMS_ORE_DESTROYED_TITLE.apply(new TeamsOreDestroyedTitleContext(
			oreDestroyer, Component.text("⛏️", oreDestroyer.color()),
			teamName, ore.getTeam().colourWord("⭐")
		));
		final Component chat = TEAMS_ORE_DESTROYED_CHAT.apply(new TeamsOreDestroyedChatContext(teamName, oreDestroyer));

		Bukkit.broadcast(chat);
		PlayerUtils.sendOptionalTitle(Component.empty(), title, 1, 20, 10);

		for (Player p : Bukkit.getOnlinePlayers()) {
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.AMBIENT, 2f, 1f);
		}

		Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
			for (Player p : Bukkit.getOnlinePlayers()) {
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.AMBIENT, 2f, 0.5f);
			}
		}, 5);

		this.gameWorld.strikeLightningEffect(ore.getLifeOres().keySet().iterator().next().getLocation().add(0.5, 0, 0.5));
	}

	private static final Component CANT_BUILD_HERE = Component.text("You can't build here", TextColors.ERROR_RED);
	private void playNoBuildEffect(Block block, Player player, IntBoundingBox noBuildZone) {
		Location loc = block.getLocation().add(0.5d, 0.5d, 0.5d);
		player.spawnParticle(Particle.ANGRY_VILLAGER, loc, 2);
		player.playSound(loc, Sound.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 0.5f, 2f);

		var min = noBuildZone.getMin();
		var max = noBuildZone.getMax();
		Particle.DustOptions data = new Particle.DustOptions(Color.RED, 1);
		float xInterval = (max.x() + 1 - min.x()) / 10f,
			zInterval = (max.z() + 1 - min.z()) / 10f,
			yInterval = (max.y() + 1 - min.y()) / 10f;
		for (int i = 0; i < 10; i++) {
			float x = min.x() + xInterval * i;
			float yOffset = yInterval * (i / 10f);
			for (int j = 0; j < 10; j++) {
				float y = min.y() + yInterval * j + yOffset;
				player.spawnParticle(Particle.DUST, x, y, min.z(), 0, data);
				player.spawnParticle(Particle.DUST, x, y, max.z() + 1, 0, data);
			}
		}
		for (int k = 0; k < 10; k++) {
			float z = min.z() + zInterval * k;
			float yOffset = yInterval * (k / 10f);
			for (int j = 0; j < 10; j++) {
				float y = min.y() + yInterval * j + yOffset;
				player.spawnParticle(Particle.DUST, min.x(), y, z, 0, data);
				player.spawnParticle(Particle.DUST, max.x() + 1, y, z, 0, data);
			}
		}

		player.sendMessage(CANT_BUILD_HERE);
	}

	/** Add players to ore current miners when they start digging it */
	@Override
	public void onBlockDig(BlockDamageEvent event) {
		super.onBlockDig(event);
		if (event.isCancelled()) return;
		if (this.gameState != GameState.LIVE) return;

		Player digger = event.getPlayer();
		if (this.isDead(digger)) return;

		TeamArenaTeam ore = this.oreToTeamLookup.get(event.getBlock());

		if (ore != null) {
			teamOres.get(ore).addMiner(digger);
		}
	}

	@Override
	public void onBlockStopDig(BlockDamageAbortEvent event) {
		super.onBlockStopDig(event);
		if (this.gameState != GameState.LIVE) return;

		Player digger = event.getPlayer();
		if (this.isDead(digger)) return;

		TeamArenaTeam ore = this.oreToTeamLookup.get(event.getBlock());

		if (ore != null) {
			teamOres.get(ore).removeMiner(digger);
		}
	}

	@Override
	public void onDamage(DamageEvent event) {
		super.onDamage(event);

		// Nerf damage of tools to max(fist, tool_damage / 3)
		if (event.getDamageType().isMelee() && event.getFinalAttacker() instanceof Player) {
			for (ItemStack tool : defaultTools) {
				if (tool.isSimilar(event.getMeleeWeapon())) {
					event.setRawDamage(Math.max(1d, event.getRawDamage() / 3d));
					break;
				}
			}
		}
	}

	// Allow dropping of status ore items.
	@Override
	public void onDropItem(PlayerDropItemEvent event) {
		super.onDropItem(event);

		if (isUpgradeItem(event.getItemDrop().getItemStack())) {
			event.setCancelled(false);
		}
	}

	@Override
	public void onAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
		super.onAttemptPickupItem(event);

		if (!this.isDead(event.getPlayer()) &&
			isUpgradeItem(event.getItem().getItemStack())) {

			event.setCancelled(false);
		}
	}

	@Override
	public void handleDeath(DamageEvent event) {
		boolean callSuper = true;
		if (event.getVictim() instanceof Player playerVictim) {
			this.blockTimes.remove(playerVictim);

			// Drop half of their status ores
			List<ItemStack> items = ItemUtils.getItemsInInventory(this::isUpgradeItem, playerVictim.getInventory());
			for (ItemStack statusOreItem : items) {
				int halfQty = (statusOreItem.getAmount() / 2) + MathUtils.randomMax(1);
				if (halfQty != 0) {
					ItemStack half = statusOreItem.asQuantity(halfQty);
					this.gameWorld.spawn(playerVictim.getLocation(), Item.class, item -> {
						item.setItemStack(half);
						item.setVelocity(item.getVelocity().multiply(1.45d));
					});
				}
			}

			// Prevent players on teams with destroyed ores from respawning.
			TeamArenaTeam victimsTeam = Main.getPlayerInfo(playerVictim).team;
			TeamLifeOres ore = teamOres.get(victimsTeam);

			assert ore != null : playerVictim.getName() + " died and didn't have an ore??";

			if (ore.isDead()) {
				// Spawn the angel before calling super, so the angel spawned by super has no effect.
				// We want an angel that doesn't lock the player's position.
				SpectatorAngelManager.spawnAngel(playerVictim, false);
				callSuper = false; // Don't call super again
				super.handleDeath(event);
				this.respawnTimers.remove(playerVictim); // Also don't respawn them

				this.checkWinner();
			}


			// Should be removed by super.handleDeath -> PlayerUtils.resetState
			//assert !PotionEffectManager.hasEffect(playerVictim, PotionEffectType.FAST_DIGGING, HasteOreAction.HASTE_EFFECT_KEY);
		}

		if (callSuper)
			super.handleDeath(event);
	}

	@Override
	public void prepLive() {
		super.prepLive();
		this.effTime = TeamArena.getGameTick() + EFF_TIME;
	}

	@Override
	public void liveTick() {
		super.liveTick();

		for (TeamLifeOres lifeOres : teamOres.values()) {
			lifeOres.tick();
		}

		for (TeamUpgrades teamUpgrades : teamUpgrades.values()) {
			teamUpgrades.tick();
		}

		for (UpgradeSpawnState upgradeSpawner : upgradeSpawners) {
			upgradeSpawner.tick();
		}

		// Anti stall
		if (this.effTime != EFF_ACTIVE && TeamArena.getGameTick() - this.effTime >= 0) { // Activate
			this.effTime = EFF_ACTIVE;

			this.upgradeTools(DEFAULT_EFF_ENCHANTS);

			Bukkit.broadcast(FASTER_TOOLS_CHAT);
			PlayerUtils.sendOptionalTitle(Component.empty(), FASTER_TOOLS_TITLE, 1, 30, 20);
			for (Player p : Bukkit.getOnlinePlayers()) {
				p.playSound(p, Sound.ENTITY_CAT_PURREOW, SoundCategory.AMBIENT, 1f, 2f);
			}
		}

		this.doBlockCooldowns();

		// Check if game should be over
		this.checkWinner();

		if (this.winningTeam != null) {
			this.prepEnd();
		}
	}

	private void doBlockCooldowns() {
		if (mapInfo.specialNoBlockRegeneration) return;

		final int currentTick = TeamArena.getGameTick();
		// Block stack regeneration
		for (Player player : this.players) {
			// Only increment and decrement block stacks one at a time, priority in the order of this.blocks
			boolean incremented = false;
			boolean decremented = false;
			for (ItemStack blockStack : this.defaultBlocks) {
				final PlayerInventory inventory = player.getInventory();
				final int totalCount = ItemUtils.getItemCount(inventory, blockStack);
				if (totalCount != MAX_BLOCK_COUNT) {
					BlockTimes times = this.blockTimes.get(player);
					if (times == null)
						continue;

					if (!incremented && times.blockPlaceTime != BlockTimes.NO_TIME &&
						currentTick - times.blockPlaceTime >= TICKS_PER_GAIN_BLOCK && totalCount < MAX_BLOCK_COUNT) {
						incremented = true;
						times.blockPlaceTime = currentTick;
						inventory.addItem(blockStack.asOne());
					}
					else if (!decremented && times.blockBreakTime != BlockTimes.NO_TIME &&
						currentTick - times.blockBreakTime >= TICKS_PER_LOSE_BLOCK && totalCount > MAX_BLOCK_COUNT) {
						decremented = true;
						times.blockBreakTime = currentTick;
						ItemUtils.maxItemAmount(inventory, blockStack, totalCount - 1);

						assert ItemUtils.getItemCount(inventory, blockStack) == totalCount - 1;
					}
				}

				if (incremented && decremented)
					break;
			}
		}
	}

	@Override
	public void prepEnd() {
		super.prepEnd();

		//LoadedChunkTracker.clearTrackedChunks();
	}

	@Override
	public void prepDead() {
		this.pointMarkers.forEach(PointMarker::remove);
		this.midMarker.remove();

		super.prepDead();
	}

	/** Apply enchantments to all the default tools to be given to players */
	private void upgradeTools(final Map<Enchantment, Integer> enchantmentsAndLevels) {
		// Apply the upgrades to the tools living players have right now.
		// Do this before mutating this.tools
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (this.isDead(p)) continue;

			for (ItemStack defaultTool : defaultTools) {
				List<ItemStack> tool = ItemUtils.getItemsInInventory(
					defaultTool::isSimilar,
					p.getInventory());

				tool.forEach(toolStack -> {
					ItemUtils.applyEnchantments(toolStack, enchantmentsAndLevels);
				});
			}
		}

		// Apply to this.tools so the new tools are given to respawning players.
		for (ItemStack item : this.defaultTools) {
			ItemUtils.applyEnchantments(item, enchantmentsAndLevels);
		}
	}

	/**
	 * Check if there is one team left alive and end the game
	 */
	public void checkWinner() {
		TeamArenaTeam winnerTeam = null;
		int aliveTeamCount = 0;
		for (TeamArenaTeam team : this.teams) {
			if (!team.isAlive()) continue;
			if (team.score == TEAM_DEAD_SCORE)
				continue;

			TeamLifeOres ore = this.teamOres.get(team);
			if (!ore.isDead()) {
				winnerTeam = team;
				aliveTeamCount++;
			}
			else {
				int aliveTeamMemberCount = 0;
				Player lastMan = null;
				for (Player teamMember : team.getPlayerMembers()) {
					if (!this.isDead(teamMember)) {
						aliveTeamMemberCount++;
						lastMan = teamMember;
					}
				}

				if (aliveTeamMemberCount >= 1) {
					aliveTeamCount++;
				}

				if (aliveTeamMemberCount == 1 && team.score != TEAM_LASTMAN_SCORE) {
					team.score = TEAM_LASTMAN_SCORE;
					SearchAndDestroy.announceLastManStanding(lastMan, team, this.middle.toLocation(this.gameWorld));
				}
				else if (aliveTeamMemberCount == 0) {
					team.score = TEAM_DEAD_SCORE;

					Component message = TEAM_DEFEATED.apply(new TeamDefeatedContext(team.getComponentName()));
					Bukkit.broadcast(message);
					PlayerUtils.sendOptionalTitle(Component.empty(), message, 1, 20, 10);
				}
			}
		}

		if (!CommandDebug.ignoreWinConditions && aliveTeamCount == 1) {
			//this method may be called during a damage tick, so signal to end game later instead by assigning
			// winningTeam
			this.winningTeam = winnerTeam;
		}
	}

	@Override
	protected boolean canJoinMidGame() {
		return this.canJoinMidGame;
	}

	@Override
	public void giveKitAndGameItems(Player player, PlayerInfo pinfo, boolean clear) {
		super.giveKitAndGameItems(player, pinfo, true);

		player.getInventory().addItem(defaultTools.toArray(new ItemStack[0]));
		player.getInventory().addItem(defaultBlocks.toArray(new ItemStack[0]));
	}

	@Override
	public void leavingPlayer(Player player) {
		super.leavingPlayer(player);

		this.blockTimes.remove(player);
		//LoadedChunkTracker.removeTrackedChunks(player);
	}

	@Override
	public boolean isVandalisableBlock(Block block) {
		if (this.oreToTeamLookup.containsKey(block) ||
			upgradeSpawners.stream().anyMatch(spawner -> spawner.isOreBlock(block)) ||
			this.chestLookup.containsKey(block)) {
			return false;
		}
		else return super.isVandalisableBlock(block);
	}

	@Override
	public boolean canSelectKitNow(Player player) {
		return !this.gameState.isEndGame();
	}

	@Override
	public boolean canSelectTeamNow() {
		return gameState == GameState.PREGAME;
	}

	@Override
	public boolean isRespawningGame() {
		return true;
	}

	@Override
	public Component getGameName() {
		return GAME_NAME;
	}

	@Override
	public Component getHowToPlayBrief() {
		return HOW_TO_PLAY;
	}

	@Override
	public String getDebugAntiStall() {
		return """
			effTime: %d
			*antiStallTime: %d""".formatted(effTime, effTime - gameTick);
	}

	@Override
	public void setDebugAntiStall(int antiStallCountdown) {
		effTime = antiStallCountdown + gameTick;
	}
}

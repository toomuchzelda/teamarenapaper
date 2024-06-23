package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
//import me.toomuchzelda.teamarenapaper.potioneffects.PotionEffectManager;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.statusorebuffactions.HasteOreAction;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy.SearchAndDestroy;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DigAndBuild extends TeamArena
{
	private static final Component GAME_NAME = Component.text("Dig and Build", NamedTextColor.DARK_GREEN);
	private static final Component HOW_TO_PLAY = Component.text("Make your way around the map and break the enemies' " +
		"Life Ore!!", NamedTextColor.DARK_GREEN);

	// MESSAGES
	@RegExp
	private static final String MINER_KEY = "%miner%"; // The player mining, or the team's short name if > 1 player
	@RegExp
	private static final String TEAM_KEY = "%oreTeam"; // Team

	private static final Component CANT_BUILD_HERE = Component.text("You can't build here", TextColors.ERROR_RED);
	private static final Component CANT_BREAK_YOUR_ORE = Component.text("You can't break your Life Ore", TextColors.ERROR_RED);

	private static final Component YOUR_ORE_MINED_TITLE = Component.text(MINER_KEY + " is mining your ore!", NamedTextColor.GOLD);
	private static final Component YOUR_ORE_MINED_CHAT = Component.text("Your ore is being mined by " + MINER_KEY + "!", NamedTextColor.GOLD);

	private static final Component TEAMS_ORE_MINED_CHAT = Component.text(MINER_KEY + " damaged " +  TEAM_KEY + "'s Ore!", NamedTextColor.GOLD);

	private static final Component TEAMS_ORE_DESTROYED_TITLE = Component.text(TEAM_KEY + "'s ore destroyed by " + MINER_KEY + "!", NamedTextColor.GOLD);
	private static final Component TEAMS_ORE_DESTROYED_CHAT = Component.text(TEAM_KEY + "'s ore has been destroyed by " + MINER_KEY +
		"! They will no longer respawn!", NamedTextColor.GOLD);

	private static final Component TEAM_DEFEATED = Component.text(TEAM_KEY + " has been defeated!", NamedTextColor.GOLD);

	private static final Component YOU_WONT_RESPAWN = Component.text("You will no longer respawn!", TextColors.ERROR_RED);

	private static final Component FASTER_TOOLS_CHAT = Component.text("You're taking too long! Everyone gets faster tools", TextColors.ERROR_RED);
	private static final Component FASTER_TOOLS_TITLE = Component.text("Everyone gets faster tools!", TextColors.ERROR_RED);
	// END MESSAGES

	private static final int EFF_TIME = 3 * 60 * 20; // Time until enchantments are given. Anti stall measure.
	private static final int EFF_ACTIVE = 0;
	private static final Map<Enchantment, Integer> DEFAULT_EFF_ENCHANTS = Map.of(
		Enchantment.DIG_SPEED, 2
	);

	private static final int TICKS_PER_GAIN_BLOCK = 30;
	private static final int TICKS_PER_LOSE_BLOCK = 40;
	private static final int MAX_BLOCK_COUNT = 16;

	private static final int TEAM_DEAD_SCORE = 1;
	private static final int TEAM_LASTMAN_SCORE = 2;

	private Vector middle;
	private final PointMarker midMarker;

	private ItemStack[] tools;
	private ItemStack[] blocks;
	private List<IntBoundingBox> noBuildZones;
	private static class BlockTimes {
		static final int NO_TIME = -1;
		int blockPlaceTime;
		int blockBreakTime;
		public BlockTimes() { this.blockPlaceTime = NO_TIME; this.blockBreakTime = NO_TIME; }
	}
	private final Map<Player, BlockTimes> blockTimes;

	private Map<TeamArenaTeam, LifeOre> teamOres;
	private Map<BlockCoords, LifeOre> oreLookup;
	private Map<BlockCoords, TeamArenaTeam> chestLookup;

	private List<PointMarker> pointMarkers;
	private Map<StatusOreType, TeamArenaMap.DNBStatusOreInfo> statusOreInfos;
	private Map<StatusOreType, ItemStack> statusOreItems;
	private Map<ItemStack, StatusOreType> statusItemLookup;
	private Map<BlockCoords, StatusOre> statusOreLookup;

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

        List<Map.Entry<TeamArenaTeam, LifeOre>> sorted = new ArrayList<>(teamOres.entrySet());
		sorted.sort(Comparator.comparingInt(entry -> entry.getValue().getHealth() == 0 ?
			-aliveCounts.get(entry.getKey()) :
			entry.getValue().getHealth()));
		for (Map.Entry<TeamArenaTeam, LifeOre> entry : sorted) {
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

		Component antiStallAction = Component.text("Faster tools", TextColor.color(PotionEffectType.FAST_DIGGING.getColor().asRGB()));
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
		if (health < LifeOre.STARTING_HEALTH) {
			float percentage = (float) health / LifeOre.STARTING_HEALTH;
			return Component.text(health + "⛏",
				percentage < 0.5f ?
					TextColor.lerp(percentage * 2, NamedTextColor.DARK_RED, NamedTextColor.YELLOW) :
					TextColor.lerp((percentage - 0.5f) * 2, NamedTextColor.YELLOW, NamedTextColor.GREEN));
		} else {
			int extra = health - LifeOre.STARTING_HEALTH;
			if (extra != 0)
				return Component.textOfChildren(
					Component.text(LifeOre.STARTING_HEALTH, NamedTextColor.BLUE),
					Component.text(" + ", NamedTextColor.GRAY),
					Component.text(extra + "⛏", TextColors.ABSORPTION_HEART)
				);
			else
				return Component.text(LifeOre.STARTING_HEALTH + "⛏", NamedTextColor.BLUE);
		}
	}

	@Override
	protected void loadConfig(TeamArenaMap map) {
		super.loadConfig(map);

		TeamArenaMap.DNBInfo mapInfo = map.getDnbInfo();

		this.middle = mapInfo.middle();
		this.spawnPos = mapInfo.middle().toLocation(this.gameWorld);

		//TOOLS
		this.tools = new ItemStack[mapInfo.tools().size()];
		int i = 0;
		for (Material mat : mapInfo.tools()) {
			this.tools[i++] = ItemBuilder.of(mat).lore(Component.text("Get digging!")).build();
		}

		//BLOCKS
		this.blocks = new ItemStack[mapInfo.blocks().size()];
		i = 0;
		for (Material mat : mapInfo.blocks()) {
			this.blocks[i++] = ItemBuilder.of(mat).lore(Component.text("Get building!")).amount(MAX_BLOCK_COUNT).build();
		}

		// Make a copy of bounding boxes. May be able to use the provided list as-is instead.
		this.noBuildZones = new ArrayList<>(mapInfo.noBuildZones().size());
		//int i = 0;
		for (IntBoundingBox noBuildZone : mapInfo.noBuildZones()) {
			this.noBuildZones.add(new IntBoundingBox(noBuildZone));

			//RealHologram hol1 = new RealHologram(noBuildZone.getMax().toLocation(this.gameWorld), RealHologram.Alignment.TOP, Component.text("one"+i));
			//RealHologram hol2 = new RealHologram(noBuildZone.getMin().toLocation(this.gameWorld), RealHologram.Alignment.TOP, Component.text("two"+i));
			//i++;
		}

		this.pointMarkers = new ArrayList<>();

		this.teamOres = new HashMap<>();
		this.oreLookup = new HashMap<>();
		this.chestLookup = new HashMap<>();
		for (var entry : mapInfo.teams().entrySet()) {
			TeamArenaTeam team = this.getTeamByLegacyConfigName(entry.getKey());
			final TeamArenaMap.DNBTeamInfo tinfo = entry.getValue();

			LifeOre lifeOre = new LifeOre(team, mapInfo.oreType(), tinfo.oreCoords(), tinfo.protectionRadius(),
				this.gameWorld);

			this.teamOres.put(team, lifeOre);
			this.oreLookup.put(tinfo.oreCoords(), lifeOre);

			BlockCoords chestCoords = tinfo.teamChest();
			if (chestCoords != null) {
				PointMarker marker = new PointMarker(chestCoords.toLocation(this.gameWorld).add(0.5d, 2.2d, 0.5d),
					team.getComponentName().append(Component.text("'s chest", team.getRGBTextColor())),
					team.getColour(), Material.CHEST, true);
				this.pointMarkers.add(marker);

				this.chestLookup.put(chestCoords, team);
			}
		}

		// Should never be modified (even if we made a copy and weren't referencing the original TeamArenaMap data)
		this.statusOreInfos = Collections.unmodifiableMap(mapInfo.statusOres());
		this.statusOreItems = new EnumMap<>(StatusOreType.class);
		this.statusItemLookup = new HashMap<>();
		this.statusOreLookup = new HashMap<>();
		for (var statusOreEntry : this.statusOreInfos.entrySet()) {
			final StatusOreType type = statusOreEntry.getKey();

			for (BlockCoords coords : statusOreEntry.getValue().coords()) {
				StatusOre ore = new StatusOre(type, coords, this.gameWorld);
				this.statusOreLookup.put(coords, ore);
			}

			final ItemStack itemStack = createItemStack(type, statusOreEntry.getValue());
			this.statusOreItems.put(type, itemStack);
			this.statusItemLookup.put(itemStack, type);

			for (Vector markerLoc : statusOreEntry.getValue().hologramLocs()) {
				PointMarker marker = new PointMarker(markerLoc.toLocation(this.gameWorld), type.displayName, type.color,
					statusOreEntry.getValue().itemType());

				this.pointMarkers.add(marker);
			}
		}
	}

	private static ItemStack createItemStack(StatusOreType type, TeamArenaMap.DNBStatusOreInfo info) {
		Component name = createOreItemName(info.required(), type.displayName);
		List<Component> lore = TextUtils.wrapString(type.description, Style.style(TextUtils.RIGHT_CLICK_TO));

		return ItemBuilder.of(info.itemType()).displayName(name).lore(lore).build();
	}

	private static Component createOreItemName(int required, Component displayName) {
		return displayName.append(Component.text(" - Need " + required, NamedTextColor.RED));
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
		BlockCoords coords = new BlockCoords(block);
		return findOreInRange(block.getLocation()) == null && findNoBuildZone(coords) == null;
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

		final LifeOre ore = this.oreLookup.get(coords);
		if (ore != null) {
			event.setCancelled(true);
			Player breaker = event.getPlayer();
			this.handleLifeOreBreak(event, block, ore, breaker);
			return true;
		}

		final StatusOre statusOre = this.statusOreLookup.get(coords);
		if (statusOre != null) {
			event.setCancelled(true);

			final Player breaker = event.getPlayer();
			if (statusOre.onBreak(breaker)) { // If broken goodly then give them ore item
				this.giveStatusOreItem(breaker, statusOre);
			}
			return true;
		}

		IntBoundingBox nbz = findNoBuildZone(coords);
		if (nbz != null) {
			event.setCancelled(true);
			playNoBuildEffect(block, event.getPlayer(), nbz);
			return true;
		}

		LifeOre oreInRange = findOreInRange(block.getLocation());
		if (oreInRange != null) {
			event.setCancelled(true);
			playNoBuildEffect(block, event.getPlayer(), oreInRange, false);
			return true;
		}

		// Give them an extra block
		this.giveBlockOnBreak(block, event.getPlayer());

		return true;
	}

	private void handleLifeOreBreak(BlockBreakEvent event, Block block, LifeOre ore, Player breaker) {
		final LifeOre.OreBreakResult result = ore.onBreak(breaker);
		if (result == LifeOre.OreBreakResult.ALREADY_DEAD) return;

		if (result == LifeOre.OreBreakResult.BROKEN_BY_TEAMMATE) {
			playNoBuildEffect(block, event.getPlayer(), ore, true);
		}
		else  {
			if (result == LifeOre.OreBreakResult.BROKEN_BY_ENEMY) {
				// Need to manually play block break effect since event was cancelled.
				// For everyone except the breaker.
				Location loc = block.getLocation();
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (p != breaker && EntityUtils.distanceSqr(p, loc) <= 30d * 30d) {
						ParticleUtils.blockBreakEffect(p, block);
					}
				}

				this.announceOreDamaged(ore);
			}
			else { // KILLED
				event.setCancelled(false);
				// Stop the team from respawning
				for (Player loser : ore.owningTeam.getPlayerMembers()) {
					if (this.respawnTimers.remove(loser) != null) {
						loser.sendActionBar(YOU_WONT_RESPAWN);
					}
				}
				this.announceOreKilled(ore);

				this.canJoinMidGame = false; // Prevent players from joining the game from now on.
			}
			// Bit messy to do here, need to clear current miners manually after successfully damaged
			ore.clearMiners();

			if (this.effTime != EFF_ACTIVE)
				this.effTime += 7 * 20; // Delay time until anti stall
		}
	}

	private void giveStatusOreItem(final Player breaker, final StatusOre brokenOre) {
		breaker.getInventory().addItem(this.statusOreItems.get(brokenOre.getType()));
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

		for (ItemStack stack : this.blocks) {
			if (stack.getType() == brokenBlockType) {
				breaker.getInventory().addItem(stack.asOne());
				return;
			}
		}

		breaker.getInventory().addItem(this.blocks[0].asOne());
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

		LifeOre oreInRange = findOreInRange(block.getLocation());
		if (oreInRange != null) {
			event.setCancelled(true);
			playNoBuildEffect(block, placer, oreInRange, false);
			return;
		}

		// Event not cancelled if reached here
		// Set the timer for next block regen
		this.blockTimes.computeIfAbsent(placer, player -> new BlockTimes()).blockPlaceTime = TeamArena.getGameTick();
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
		TeamArenaTeam clickedChestTeam = this.chestLookup.get(new BlockCoords(event.getClickedBlock()));
		if (clickedChestTeam != null) {
			// Checking if player is dead is done in super and sets useInteractedBlock to DENY
			assert CompileAsserts.OMIT || !this.isDead(clicker);
			if (Main.getPlayerInfo(clicker).team != clickedChestTeam) {
				event.setUseInteractedBlock(Event.Result.DENY);
				clicker.playSound(clicker, Sound.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1f, 1.7f);
			}

			return;
		}

		// If they're redeeming status ore items to get the buff
		final LifeOre clickedOre = this.findOreInRange(event.getClickedBlock().getLocation());
		if (clickedOre == null) return;
		if (!clickedOre.owningTeam.hasMember(clicker)) return;

		final ItemStack usedItemStack = event.getItem();
		if (usedItemStack != null) {
			final StatusOreType oreType = this.getStatusOreByItem(usedItemStack);
			if (oreType != null) { // A valid item was used and we have the corresponding StatusOreType
				int itemAmount = ItemUtils.getItemCount(clicker.getInventory(), usedItemStack);
				final int required = this.statusOreInfos.get(oreType).required();
				final int toTake = oreType.action.buff(clicker, required, itemAmount, clickedOre, this);
				if (toTake > 0) {
					itemAmount -= toTake;
					if (itemAmount < 0) {
						Main.logger().severe("itemAmount less than 0");
						Thread.dumpStack();
						itemAmount = 0; // fallback
					}

					ItemUtils.maxItemAmount(clicker.getInventory(), usedItemStack, itemAmount);

					assert CompileAsserts.OMIT ||
						ItemUtils.getItemCount(clicker.getInventory(), usedItemStack) == itemAmount;
				}
			}
		}
	}

	private StatusOreType getStatusOreByItem(ItemStack item) {
		return this.statusItemLookup.get(item.asOne());
	}

	public boolean canMoveToInventory(Player user, ItemStack item, Inventory inventory) {
		if (inventory.getType() != InventoryType.CHEST)
			return false;

		if (this.getStatusOreByItem(item) == null) // Only move status ore items in team chest.
			return false;

		for (var entry : this.chestLookup.entrySet()) {
			BlockCoords coords = entry.getKey();
			int x = coords.x();
			int y = coords.y();
			int z = coords.z();
			Block block = this.gameWorld.getBlockAt(x, y, z);
			assert CompileAsserts.OMIT || block.getType().name().endsWith("CHEST");

			if (block.getState() instanceof Chest chestState) {
				Inventory teamChestInv = chestState.getBlockInventory();

				if (teamChestInv.equals(inventory))
					//Bukkit.broadcastMessage("chest invs Did equals");

				if (teamChestInv.equals(inventory) && Main.getPlayerInfo(user).team == entry.getValue())
					return true;
			}
		}

		return false;
	}

	/** Broadcast ore damage */
	private void announceOreDamaged(LifeOre ore) {
		final Component oreMinerShort = ore.getMinerComponent(true);
		final Component oreMinerLong = ore.getMinerComponent(false);

		TextReplacementConfig minerShortReplacement =
			TextReplacementConfig.builder().match(MINER_KEY).replacement(oreMinerShort).build();
		TextReplacementConfig minerLongReplacement =
			TextReplacementConfig.builder().match(MINER_KEY).replacement(oreMinerLong).build();

		TextReplacementConfig teamReplacement =
			TextReplacementConfig.builder().match(TEAM_KEY).replacement(ore.owningTeam.getComponentName()).build();

		Component yourTitle = YOUR_ORE_MINED_TITLE.replaceText(minerShortReplacement);
		Component yourChat = YOUR_ORE_MINED_CHAT.replaceText(minerLongReplacement);

		Component teamTitle = TEAMS_ORE_MINED_CHAT.replaceText(teamReplacement).replaceText(minerLongReplacement);

		var iter = Main.getPlayersIter();
		while (iter.hasNext()) {
			var entry = iter.next();
			final Player p = entry.getKey();
			final PlayerInfo pinfo = entry.getValue();

			if (pinfo.team == ore.owningTeam) {
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

	private void announceOreKilled(LifeOre ore) {
		final Component oreDestroyer = ore.getMinerComponent(false);

		TextReplacementConfig minerReplacementLong =
			TextReplacementConfig.builder().match(MINER_KEY).replacement(oreDestroyer).build();
		TextReplacementConfig minerReplacementShort =
			TextReplacementConfig.builder().match(MINER_KEY).replacement(ore.getMinerComponent(true)).build();

		TextReplacementConfig teamReplacement =
			TextReplacementConfig.builder().match(TEAM_KEY).replacement(ore.owningTeam.getComponentName()).build();

		final Component title = TEAMS_ORE_DESTROYED_TITLE.replaceText(teamReplacement).replaceText(minerReplacementShort);
		final Component chat = TEAMS_ORE_DESTROYED_CHAT.replaceText(teamReplacement).replaceText(minerReplacementLong);

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

		this.gameWorld.strikeLightningEffect(ore.coordsAsLoc.clone().add(0.5, 0, 0.5));
	}

	private void playNoBuildEffect(Block block, Player player, IntBoundingBox noBuildZone) {
		Location loc = block.getLocation().add(0.5d, 0.5d, 0.5d);
		player.spawnParticle(Particle.VILLAGER_ANGRY, loc, 2);
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
				player.spawnParticle(Particle.REDSTONE, x, y, min.z(), 0, data);
				player.spawnParticle(Particle.REDSTONE, x, y, max.z() + 1, 0, data);
			}
		}
		for (int k = 0; k < 10; k++) {
			float z = min.z() + zInterval * k;
			float yOffset = yInterval * (k / 10f);
			for (int j = 0; j < 10; j++) {
				float y = min.y() + yInterval * j + yOffset;
				player.spawnParticle(Particle.REDSTONE, min.x(), y, z, 0, data);
				player.spawnParticle(Particle.REDSTONE, max.x() + 1, y, z, 0, data);
			}
		}

		player.sendMessage(CANT_BUILD_HERE);
	}

	private void playNoBuildEffect(Block block, Player player, LifeOre source, boolean isBreaking) {
		Location loc = block.getLocation().add(0.5d, 0.5d, 0.5d).subtract(player.getLocation().getDirection());
		if (isBreaking) {
			player.spawnParticle(Particle.BLOCK_MARKER, loc, 1, Material.BARRIER.createBlockData());
		} else {
			Particle.DustOptions data = new Particle.DustOptions(source.owningTeam.getColour(), 1);
			double radius = Math.sqrt(source.protectionRadiusSqr);
			double sample = radius / 5;
			var coords = source.coords;
			double centerX = coords.x() + 0.5;
			double centerY = coords.y() + 0.5;
			double centerZ = coords.z() + 0.5;
			double minY = centerY - radius;
			double maxY = centerY + radius;
			// bottom and top
			player.spawnParticle(Particle.REDSTONE, centerX, minY, centerZ, 0, data);
			player.spawnParticle(Particle.REDSTONE, centerX, maxY, centerZ, 0, data);
			for (int j = 1; j <= 5; j++) {
				double y1 = minY + sample * j;
				double y2 = maxY - sample * j;
				double a = Math.acos((centerY - y1) / radius);
				double effectiveRadius = radius * Math.sin(a);
				for (int i = 0; i < 10; i++) {
					double b = (360 / 10d) * i;
					double x = centerX + effectiveRadius * Math.cos(b);
					double z = centerZ + effectiveRadius * Math.sin(b);
					player.spawnParticle(Particle.REDSTONE, x, y1, z, 0, data);
					if (j != 5)
						player.spawnParticle(Particle.REDSTONE, x, y2, z, 0, data);
				}
			}
		}
		player.playSound(loc, Sound.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 0.5f, 2f);
		player.sendMessage(isBreaking ? CANT_BREAK_YOUR_ORE : CANT_BUILD_HERE);
	}

	/** Add players to ore current miners when they start digging it */
	@Override
	public void onBlockDig(BlockDamageEvent event) {
		super.onBlockDig(event);
		if (event.isCancelled()) return;
		if (this.gameState != GameState.LIVE) return;

		Player digger = event.getPlayer();
		if (this.isDead(digger)) return;

		BlockCoords coords = new BlockCoords(event.getBlock());
		LifeOre ore = this.oreLookup.get(coords);

		if (ore != null) {
			ore.addMiner(digger);
		}
	}

	@Override
	public void onBlockStopDig(BlockDamageAbortEvent event) {
		super.onBlockStopDig(event);

		BlockCoords coords = new BlockCoords(event.getBlock());
		LifeOre ore = this.oreLookup.get(coords);

		if (ore != null) {
			ore.removeMiner(event.getPlayer());
		}
	}

	@Override
	public void onDamage(DamageEvent event) {
		super.onDamage(event);

		// Nerf damage of tools to max(fist, tool_damage / 3)
		if (event.getDamageType().isMelee() && event.getFinalAttacker() instanceof Player) {
			for (ItemStack tool : tools) {
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

		if (this.getStatusOreByItem(event.getItemDrop().getItemStack()) != null) {
			event.setCancelled(false);
		}
	}

	@Override
	public void onAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
		super.onAttemptPickupItem(event);

		if (!this.isDead(event.getPlayer()) &&
			this.getStatusOreByItem(event.getItem().getItemStack()) != null) {

			event.setCancelled(false);
		}
	}

	@Override
	public void handleDeath(DamageEvent event) {
		boolean callSuper = true;
		if (event.getVictim() instanceof Player playerVictim) {
			this.blockTimes.remove(playerVictim);

			// Drop half of their status ores
			List<ItemStack> items = ItemUtils.getItemsInInventory(itemStack ->
				this.getStatusOreByItem(itemStack) != null, playerVictim.getInventory());
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
			LifeOre ore = teamOres.get(victimsTeam);

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

		this.statusOreLookup.forEach((blockCoords, statusOre) -> {
			statusOre.tick();
		});

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
		final int currentTick = TeamArena.getGameTick();
		// Block stack regeneration
		for (Player player : this.players) {
			// Only increment and decrement block stacks one at a time, priority in the order of this.blocks
			boolean incremented = false;
			boolean decremented = false;
			for (ItemStack blockStack : this.blocks) {
				final PlayerInventory inventory = player.getInventory();
				final int totalCount = ItemUtils.getItemCount(inventory, blockStack);
				if (totalCount != MAX_BLOCK_COUNT) {
					BlockTimes times = this.blockTimes.get(player); // Should never be null
					if (times == null) {
						Main.logger().warning(player.getName() + " has " + totalCount + " blocks and doesn't have lastPlaceTime cooldown");
						continue;
					}

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

			for (int i = 0; i < this.tools.length; i++) {
				final int index = i; // epic java
				List<ItemStack> tool = ItemUtils.getItemsInInventory(
					itemStack -> this.tools[index].isSimilar(itemStack),
					p.getInventory());

				tool.forEach(toolStack -> {
					ItemUtils.applyEnchantments(toolStack, enchantmentsAndLevels);
				});
			}
		}

		// Apply to this.tools so the new tools are given to respawning players.
		for (ItemStack item : this.tools) {
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

			LifeOre ore = this.teamOres.get(team);
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

					TextReplacementConfig config = TextReplacementConfig.builder().match(TEAM_KEY).replacement(team.getComponentName()).build();
					Component message = TEAM_DEFEATED.replaceText(config);
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

	/**
	 * @return The LifeOre that loc is in range of, or null if none.
	 */
	private LifeOre findOreInRange(Location loc) {
		for (LifeOre ore : teamOres.values()) {
			if (loc.distanceSquared(ore.coordsAsLoc) <= ore.protectionRadiusSqr) {
				return ore;
			}
		}

		return null;
	}

	@Override
	public void givePlayerItems(Player player, PlayerInfo pinfo, boolean clear) {
		super.givePlayerItems(player, pinfo, true);

		player.getInventory().addItem(this.tools);
		player.getInventory().addItem(this.blocks);
	}

	@Override
	public void leavingPlayer(Player player) {
		super.leavingPlayer(player);

		this.blockTimes.remove(player);
		//LoadedChunkTracker.removeTrackedChunks(player);
	}

	@Override
	public boolean canSelectKitNow() {
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

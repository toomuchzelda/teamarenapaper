package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy.SearchAndDestroy;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.intellij.lang.annotations.RegExp;

import java.util.*;

public class DigAndBuild extends TeamArena
{
	private static final Component GAME_NAME = Component.text("Dig and Build", NamedTextColor.DARK_GREEN);
	private static final Component HOW_TO_PLAY = Component.text("Dig your way around the map and break the enemies' " +
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

	private static final int TICKS_PER_BLOCK = 20;
	private static final int MAX_BLOCK_COUNT = 16;

	private static final int TEAM_DEAD_SCORE = 1;
	private static final int TEAM_LASTMAN_SCORE = 2;


	private Vector middle;
	private PointMarker midMarker;

	private ItemStack[] tools;
	private ItemStack[] blocks;
	private List<IntBoundingBox> noBuildZones;

	private Map<TeamArenaTeam, LifeOre> teamOres;
	private Map<BlockCoords, LifeOre> oreLookup;

	/** If players can join after the game has started.
	 *  Players won't be allowed to join after an Ore has been broken */
	private boolean canJoinMidGame = true;

	private int effTime; // Game timestamp of when eff2 will be given

	// Keep track of chunks loaded by players for sending block change packets
	private final Map<Player, Set<Chunk>> loadedChunks = new HashMap<>();

	public void addTrackedChunk(Player tracker, Chunk chunk) {
		Set<Chunk> chunks = this.loadedChunks.computeIfAbsent(tracker, player -> new HashSet<>(100));
		chunks.add(chunk);
	}

	public void removeTrackedChunk(Player tracker, Chunk chunk) {
		Set<Chunk> chunks = this.loadedChunks.get(tracker);
		if (chunks != null) chunks.remove(chunk);
	}

	public void removeTrackedChunks(Player tracker) {
		this.loadedChunks.remove(tracker);
	}

	public void clearTrackedChunks() {
		this.loadedChunks.clear();
	}

	public DigAndBuild(TeamArenaMap map) {
		super(map);

		this.midMarker = new PointMarker(middle.toLocation(this.gameWorld), Component.text("Middle"), Color.WHITE,
			Material.LIGHTNING_ROD);
	}

	@Override
	public void updateSidebar(Player player, SidebarManager sidebar) {
		// TODO
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

		this.teamOres = new HashMap<>();
		this.oreLookup = new HashMap<>();
		for (var entry : mapInfo.teams().entrySet()) {
			TeamArenaTeam team = this.getTeamByLegacyConfigName(entry.getKey());
			final TeamArenaMap.DNBTeamInfo tinfo = entry.getValue();

			LifeOre lifeOre = new LifeOre(team, mapInfo.oreType(), tinfo.oreCoords(), tinfo.protectionRadius(),
				this.gameWorld);

			this.teamOres.put(team, lifeOre);
			this.oreLookup.put(tinfo.oreCoords(), lifeOre);
		}
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
		BlockCoords coords = new BlockCoords(block);

		LifeOre ore = this.oreLookup.get(coords);
		if (ore != null) {
			event.setCancelled(true);
			Player breaker = event.getPlayer();
			LifeOre.OreBreakResult result = ore.onBreak(breaker);

			if (result == LifeOre.OreBreakResult.BROKEN_BY_TEAMMATE) {
				playNoBuildEffect(block, event.getPlayer(), true);
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

			return true;
		}

		for (IntBoundingBox noBuildZone : this.noBuildZones) {
			if (noBuildZone.contains(coords)) {
				event.setCancelled(true);
				playNoBuildEffect(block, event.getPlayer(), false);
				return true;
			}
		}

		LifeOre oreInRange = isWithinOreRadius(block.getLocation());
		if (oreInRange != null) {
			event.setCancelled(true);
			playNoBuildEffect(block, event.getPlayer(), false);
		}

		return true;
	}

	/**
	 * Handle block placement. Don't place blocks in no build zones, near life ores.
	 */
	@Override
	public void onPlaceBlock(BlockPlaceEvent event) {
		super.onPlaceBlock(event);

		if (event.isCancelled())
			return;

		final Block block = event.getBlock();
		BlockCoords coords = new BlockCoords(block);
		for (IntBoundingBox noBuildZone : this.noBuildZones) {
			if (noBuildZone.contains(coords)) {
				event.setCancelled(true);
				playNoBuildEffect(block, event.getPlayer(), false);
				return;
			}
		}

		LifeOre oreInRange = isWithinOreRadius(block.getLocation());
		if (oreInRange != null) {
			event.setCancelled(true);
			playNoBuildEffect(block, event.getPlayer(), false);
			return;
		}

		// Event not cancelled if reached here

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
	}

	private void playNoBuildEffect(Block block, Player player, boolean lifeOre) {
		Location loc = block.getLocation().add(0.5d, 0.5d, 0.5d);
		player.spawnParticle(Particle.VILLAGER_ANGRY, loc, 2);
		player.playSound(loc, Sound.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 0.5f, 2f);

		if (!lifeOre)
			player.sendMessage(CANT_BUILD_HERE);
		else
			player.sendMessage(CANT_BREAK_YOUR_ORE);
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
	public void handleDeath(DamageEvent event) {
		// Prevent players on teams with destroyed ores from respawning.
		if (event.getVictim() instanceof Player playerVictim) {
			TeamArenaTeam victimsTeam = Main.getPlayerInfo(playerVictim).team;
			LifeOre ore = teamOres.get(victimsTeam);

			if (ore != null) {
				if (ore.isDead()) {
					// Spawn the angel before calling super, so the angel spawned by super has no effect.
					// We want an angle that doesn't lock the player's position.
					SpectatorAngelManager.spawnAngel(playerVictim, false);
					super.handleDeath(event);
					this.respawnTimers.remove(playerVictim); // Also don't respawn them

					this.checkWinner();
					return; // Don't call super again
				}
			}
			else {
				Main.logger().warning("DigAngBuild.handleDeath() player that died didn't have an ore??");
				Thread.dumpStack();
			}
		}

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

		// Anti stall
		if (this.effTime != EFF_ACTIVE && TeamArena.getGameTick() - this.effTime >= 0) { // Activate
			this.effTime = EFF_ACTIVE;

			this.upgradeTools(DEFAULT_EFF_ENCHANTS);

			Bukkit.broadcast(FASTER_TOOLS_CHAT);
			PlayerUtils.sendOptionalTitle(Component.empty(), FASTER_TOOLS_TITLE, 1, 20, 10);
			for (Player p : Bukkit.getOnlinePlayers()) {
				p.playSound(p, Sound.ENTITY_CAT_PURREOW, SoundCategory.AMBIENT, 1f, 2f);
			}
		}

		// Check if game should be over
		this.checkWinner();

		if (this.winningTeam != null) {
			this.prepEnd();
		}
	}

	@Override
	public void prepEnd() {
		super.prepEnd();
		this.clearTrackedChunks();
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
	private LifeOre isWithinOreRadius(Location loc) {
		for (var entry : this.teamOres.entrySet()) {
			final LifeOre ore = entry.getValue();
			final double distanceSqr = loc.distanceSquared(ore.coordsAsLoc);

			if (distanceSqr <= ore.protectionRadiusSqr) {
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

		this.removeTrackedChunks(player);
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
	public boolean canTeamChatNow(Player player) {
		return gameState != GameState.PREGAME && gameState != GameState.DEAD;
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
}

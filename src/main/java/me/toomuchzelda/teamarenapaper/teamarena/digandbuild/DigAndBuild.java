package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.IntBoundingBox;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DigAndBuild extends TeamArena
{
	private static final Component GAME_NAME = Component.text("Dig and Build", NamedTextColor.DARK_GREEN);
	private static final Component HOW_TO_PLAY = Component.text("Dig your way around the map and break the enemies' " +
		"Life Ore!!", NamedTextColor.DARK_GREEN);

	// MESSAGES
	private static final Component CANT_BUILD_HERE = Component.text("You can't build here", TextColors.ERROR_RED);
	// END MESSAGES

	private ItemStack[] tools;
	private ItemStack[] blocks;
	private List<IntBoundingBox> noBuildZones;

	private Map<TeamArenaTeam, LifeOre> teamOres;
	private Map<BlockCoords, LifeOre> oreLookup;

	public DigAndBuild(TeamArenaMap map) {
		super(map);
	}

	@Override
	public void updateSidebar(Player player, SidebarManager sidebar) {

	}

	@Override
	protected void loadConfig(TeamArenaMap map) {
		super.loadConfig(map);

		TeamArenaMap.DNBInfo mapInfo = map.getDnbInfo();

		this.spawnPos = mapInfo.middle().toLocation(this.gameWorld);

		//TOOLS
		this.tools = new ItemStack[mapInfo.tools().size()];
		int i = 0;
		for (Material mat : mapInfo.tools()) {
			this.tools[i++] = new ItemStack(mat);
		}

		//BLOCKS
		this.blocks = new ItemStack[mapInfo.blocks().size()];
		i = 0;
		for (Material mat : mapInfo.blocks()) {
			this.blocks[i++] = new ItemStack(mat, 32);
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

		if (oreLookup.containsKey(coords)) {
			//event.getPlayer().sendMessage("Hit ore");
			event.setCancelled(true);
			return true;
		}

		for (IntBoundingBox noBuildZone : this.noBuildZones) {
			if (noBuildZone.contains(coords)) {
				event.setCancelled(true);
				playNoBuildEffect(block, event.getPlayer());
				return true;
			}
		}

		LifeOre oreInRange = isWithinOreRadius(block.getLocation());
		if (oreInRange != null) {
			event.setCancelled(true);
			playNoBuildEffect(block, event.getPlayer());
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
				playNoBuildEffect(block, event.getPlayer());
				return;
			}
		}

		LifeOre oreInRange = isWithinOreRadius(block.getLocation());
		if (oreInRange != null) {
			event.setCancelled(true);
			playNoBuildEffect(block, event.getPlayer());
		}
	}

	private void playNoBuildEffect(Block block, Player player) {
		Location loc = block.getLocation().add(0.5d, 0.5d, 0.5d);
		player.spawnParticle(Particle.VILLAGER_ANGRY, loc, 2);
		player.playSound(loc, Sound.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 0.5f, 2f);

		player.sendMessage(CANT_BUILD_HERE);
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

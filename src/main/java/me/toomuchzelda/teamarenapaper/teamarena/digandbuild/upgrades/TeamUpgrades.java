package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuildInfo;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.TeamLifeOres;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class TeamUpgrades {
	private final DigAndBuild game;

	private final TeamArenaTeam team;

	@Nullable
	private final HealUpgradeInfo healUpgrade;
	@Nullable
	private final HasteUpgradeInfo hasteUpgrade;
	@Nullable
	private final TrapUpgradeInfo trapUpgrade;

	private int traps = 0;

	public TeamUpgrades(DigAndBuild game, TeamArenaTeam team, DigAndBuildInfo dnbInfo) {
		this.game = game;
		this.team = team;
		this.healUpgrade = dnbInfo.healUpgrade;
		this.hasteUpgrade = dnbInfo.hasteUpgrade;
		this.trapUpgrade = dnbInfo.trapUpgrade;
	}

	public boolean isUpgradeItem(ItemStack stack) {
		return getUpgrade(stack) != null;
	}

	@Contract("null -> null")
	public UpgradeBase getUpgrade(ItemStack stack) {
		if (stack == null)
			return null;
		Material material = stack.getType();
		if (healUpgrade != null && healUpgrade.item() == material)
			return healUpgrade;
		if (hasteUpgrade != null && hasteUpgrade.item() == material)
			return hasteUpgrade;
		if (trapUpgrade != null && trapUpgrade.item() == material)
			return trapUpgrade;
		return null;
	}

	/**
	 * Handles interactions with the core
	 * @param player The player
	 * @param e The event
	 * @return Whether the event is handled successfully
	 */
	public boolean onInteract(Player player, PlayerInteractEvent e) {
		PlayerInfo info = Main.getPlayerInfo(player);
		if (info.team != team)
			return false;
		ItemStack item = e.getItem();
		UpgradeBase upgrade = getUpgrade(item);
		if (upgrade == null)
			return false;
		// check item amount
		PlayerInventory inventory = player.getInventory();
		var matchingItems = ItemUtils.findMatchingItems(inventory, item, upgrade.required());
		int matchingCount = matchingItems.values().stream().mapToInt(i -> i).sum();
		if (matchingCount < upgrade.required()) {
			player.sendMessage(Component.textOfChildren(
				Component.text("You need " + (upgrade.required() - matchingCount) + " more "),
				upgrade.displayName(),
				Component.text("!")
			).color(TextColors.ERROR_RED));
			e.setCancelled(true);
			return true;
		}

		// check if clicking core
		Location playerLoc = player.getLocation();
		Location clickLoc = e.getClickedBlock() != null ? e.getClickedBlock().getLocation() : null;
		Location oreLoc = playerLoc.clone();
		TeamLifeOres ores = game.getTeamLifeOre(team);
		Block core = null;
		for (var entry : ores.getLifeOres().entrySet()) {
			entry.getKey().getLocation(oreLoc);
			TeamLifeOres.LifeOreProperties props = entry.getValue();
			if (props.isInteractable(oreLoc, playerLoc) ||
				(clickLoc != null && props.isInteractable(oreLoc, clickLoc))) {
				core = entry.getKey();
				break;
			}
		}
		if (core == null) {
			player.sendMessage(Component.textOfChildren(
				Component.text("Right click", TextUtils.RIGHT_CLICK_TO),
				Component.text(" near your "),
				DigAndBuild.CORE,
				Component.text(" to apply this upgrade!")
			));
			e.setCancelled(true);
			return true;
		}
		boolean success = upgrade.apply(game, team, core, player);
		if (success) {
			ItemUtils.removeMatchingItems(inventory, matchingItems);
		}
		e.setCancelled(true);
		return true;
	}

	public int getTraps() {
		return traps;
	}

	public void addTrap() {
		traps++;
	}

	public void tick() {
	}

	public @Nullable TrapUpgradeInfo getTrapUpgrade() {
		return trapUpgrade;
	}

	public @Nullable HasteUpgradeInfo getHasteUpgrade() {
		return hasteUpgrade;
	}

	public @Nullable HealUpgradeInfo getHealUpgrade() {
		return healUpgrade;
	}
}

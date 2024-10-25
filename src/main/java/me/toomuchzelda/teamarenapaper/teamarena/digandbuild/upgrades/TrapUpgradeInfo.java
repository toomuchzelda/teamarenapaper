package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.utils.ConfigOptional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record TrapUpgradeInfo(Material item, @Nullable @ConfigOptional Component customName, int required,
							  int max, double triggerRadius) implements UpgradeBase {

	public static final @NotNull TextComponent DEFAULT_NAME = Component.text("Enemy Detector 3000", TextColor.color(0xb19e8f));

	@Override
	public @NotNull Component displayName() {
		return customName != null ? customName : DEFAULT_NAME;
	}

	@Override
	public boolean apply(DigAndBuild game, TeamArenaTeam team, Block core, Player applier) {
		TeamUpgrades upgrades = game.getTeamUpgrades(team);
		if (upgrades.getTraps() < max) {
			upgrades.addTrap();
			return true;
		}
		return false;
	}

	@Override
	public ItemStack makeItemStack() {
		/*
		Have you ever seen your teammates swing
		their weapons like they're schizophrenic?
		Are you deeply concerned that non-existent
		ghosts are a threat to your <core>?
		Fear not! With the <display_name>,
		your <core> can now alert you of the
		presence of all hostiles, visible or not.

		<yellow>Warning: Ghosts are not real.
		 */
		var lore = List.of(
			Component.text("Have you ever seen your teammates swing"),
			Component.text("their weapons like they're schizophrenic?"),
			Component.text("Are you deeply concerned that non-existent"),
			Component.textOfChildren(
				Component.text("ghosts are a threat to your "),
				DigAndBuild.CORE,
				Component.text("?")
			),
			Component.textOfChildren(
				Component.text("Fear not! With the "),
				customName != null ? customName : DEFAULT_NAME,
				Component.text(",")
			),
			Component.textOfChildren(
				Component.text("your "),
				DigAndBuild.CORE,
				Component.text(" can now alert you of the")
			),
			Component.text("presence of all hostiles, visible or not."),
			Component.newline(),
			Component.text("Warning: Ghosts are not real.", NamedTextColor.YELLOW)
		);
		// <#b19e8f>Detects enemies (Can be installed up to " + max + " times)
		var effect = Component.text("Detects enemies (Can be installed up to " + max + " times)", TextColor.color(0xb19e8f));
		return UpgradeBase.makeItemStackBaseCompileSafelyAndSecurely(this,
			lore, effect);
	}
}

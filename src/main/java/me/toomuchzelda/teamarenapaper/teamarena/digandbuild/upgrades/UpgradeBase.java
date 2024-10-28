package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface UpgradeBase permits HasteUpgradeInfo, HealUpgradeInfo, TrapUpgradeInfo {
	Material item();
	@Nullable
	Component customName();
	int required();

	@NotNull
	Component displayName();

	/**
	 * Applies an upgrade. Callers should ensure the {@code applier} is of the valid team
	 * and subtract items if this method returns {@code true}.
	 * @param game The game instance
	 * @param team The team to apply this upgrade to
	 * @param core The core that this upgrade is applied to
	 * @param applier The player who applied this upgrade
	 * @return Whether the upgrade can be applied at this moment
	 */
	boolean apply(DigAndBuild game, TeamArenaTeam team, Block core, Player applier);
	ItemStack makeItemStack();

	static ItemStack makeItemStackBaseCompileSafelyAndSecurely(UpgradeBase upgradeBase, List<? extends Component> lore, Component effect) {
		Component displayName = upgradeBase.displayName();
		Component itemName = upgradeBase.required() != 1 ?
			displayName.append(Component.text(" - " + upgradeBase.required() + " required", NamedTextColor.RED)) :
			displayName;
		return ItemBuilder.of(upgradeBase.item())
			.displayName(itemName)
			.setPdc(DigAndBuild.ITEM_MARKER, PersistentDataType.BOOLEAN, true)
			.lore(lore)
			.addLore(
				/*
				<rclick>Right click near your <core> to:
				  <effect>
				  <red>(<required> <display_name> required)</red>
				 */
				Component.empty(),
				Component.textOfChildren(
					Component.text("Right click near your "),
					DigAndBuild.CORE,
					Component.text(":")
				).color(TextUtils.RIGHT_CLICK_TO),
				Component.textOfChildren(
					Component.text("  "),
					effect
				),
				Component.textOfChildren(
					Component.text("  (" + upgradeBase.required() + " "),
					displayName,
					Component.text(" required)")
				).color(NamedTextColor.RED)
			)
			.build();
	}
}

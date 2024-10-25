package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.utils.ConfigOptional;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record HealUpgradeInfo(Material item, @Nullable @ConfigOptional Component customName, int required,
							  @ConfigOptional Integer maxShield,
							  @Nullable @ConfigOptional UpgradeSpawning spawns) implements UpgradeBase {
	public static final int DEFAULT_MAX_SHIELD = 5;
	public static final int DEFAULT_SPAWN_INTERVAL = 6 * 20;
	public static final @NotNull TextComponent DEFAULT_NAME = Component.text("Revitalizing Crystal", TextColors.ABSORPTION_HEART);

	public HealUpgradeInfo {
		if (maxShield == null) maxShield = DEFAULT_MAX_SHIELD;
		if (spawns != null) spawns = spawns.withDefaultSpawnInterval(DEFAULT_SPAWN_INTERVAL);
	}

	@Override
	public @NotNull Component displayName() {
		return customName != null ? customName : DEFAULT_NAME;
	}

	public ItemStack makeItemStack() {
		/*
		A strange mineral capable of <regen>repairing</regen> your <core>,
		or provide up to <absorption><shield> Shield</absorption> if already repaired.
		 */
		var lore = List.of(
			Component.textOfChildren(
				Component.text("A strange mineral capable of "),
				Component.text("repairing"),
				Component.text(" your "),
				DigAndBuild.CORE
			),
			Component.textOfChildren(
				Component.text("or provide up to"),
				Component.text(maxShield + " Shield", TextColors.ABSORPTION_HEART),
				Component.text(" if already repaired.")
			)
		);
		// <regen>Repairs</regen> your <core>, or provide up to <absorption><shield> Shield</absorption>
		var effect = Component.textOfChildren(
			Component.text("Repairs", TextColors.HEALTH),
			Component.text(" your "),
			DigAndBuild.CORE,
			Component.text(", or provide up to "),
			Component.text(maxShield + " Shield", TextColors.ABSORPTION_HEART)
		);
		return UpgradeBase.makeItemStackBaseCompileSafelyAndSecurely(this,
			lore, effect);
	}

	@Override
	public boolean apply(DigAndBuild game, TeamArenaTeam team, Block core, Player applier) {
		var ore = game.getTeamLifeOre(team);
		if (ore.isDead()) {
			applier.sendMessage(Component.textOfChildren(
				Component.text("Your "),
				DigAndBuild.CORE,
				Component.text(" is gone!")
			).color(TextColors.ERROR_RED));
			return false;
		} else if (ore.getHealth() >= ore.getMaxHealth() + maxShield) {
			applier.sendMessage(Component.textOfChildren(
				Component.text("Your "),
				DigAndBuild.CORE,
				Component.text(" is already fully shielded!")
			).color(TextColors.ERROR_RED));
			return false;
		}
		ore.setHealth(ore.getHealth() + 1);
		ore.playCoolUpgradeEffect(core, makeItemStack());
		return true;
	}
}

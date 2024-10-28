package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.utils.ConfigOptional;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record HasteUpgradeInfo(Material item, @Nullable @ConfigOptional Component customName, int required,
							   @Nullable @ConfigOptional UpgradeSpawning spawns) implements UpgradeBase {
	public static final int DEFAULT_SPAWN_INTERVAL = 10 * 20;
	public static final TextColor COLOR = TextColor.color(0x9ff8e5);
	public static final @NotNull TextComponent DEFAULT_NAME = Component.text("Tool Drill Attachment", COLOR);
	public static final @NotNull TextColor HASTE_COLOR = TextColor.color(PotionEffectType.HASTE.getColor().asRGB());

	public HasteUpgradeInfo {
		if (spawns != null)
			spawns = spawns.withDefaultSpawnInterval(DEFAULT_SPAWN_INTERVAL);
	}

	@Override
	public @NotNull Component displayName() {
		return customName != null ? customName : DEFAULT_NAME;
	}

	@Override
	public ItemStack makeItemStack() {
		var toolStyle = Style.style(COLOR, TextDecoration.BOLD);
		/*
		Unlock the full potetial of your tools with
		the <#9ff8e5><b>MineSoft (R) Tool Drill Attachment 365 (TM)
		<c:#9ff8e5><b>Volume License Activation</b></c>.
		This attachment drastically enhances your mining speed
		and overall company productivity.

		Note: License is valid for 30 seconds
		 */
		var lore = List.of(
			Component.text("Unlock the full potential of your tools with", NamedTextColor.GRAY),
			Component.textOfChildren(
				Component.text("the ", NamedTextColor.GRAY),
				Component.text("MineSoft (R) Tool Drill Attachment 365 (TM)", toolStyle)
			),
			Component.textOfChildren(
				Component.text("Volume License Activation", toolStyle),
				Component.text(".", NamedTextColor.GRAY)
			),
			Component.text("This attachment drastically enhances your mining speed", NamedTextColor.GRAY),
			Component.text("and overall company productivity.", NamedTextColor.GRAY),
			Component.empty(),
			Component.text("Note: License is valid for 30 seconds", NamedTextColor.GRAY)
		);
		// <#9ff8e5>Gives teammates <c:#D9C043>Haste II</c> for 30 seconds
		var effect = Component.textOfChildren(
			Component.text("Gives teammates "),
			Component.text("Haste II", HASTE_COLOR),
			Component.text(" for 30 seconds")
		).color(COLOR);
		return UpgradeBase.makeItemStackBaseCompileSafelyAndSecurely(this,
			lore, effect);
	}


	private static final PotionEffect HASTE_EFFECT = new PotionEffect(PotionEffectType.HASTE, 30 * 20, 0, false, true);
	@Override
	public boolean apply(DigAndBuild game, TeamArenaTeam team, Block core, Player applier) {
		game.getTeamUpgrades(team).playSacrificeAnimation(makeItemStack(), core.getLocation().toCenterLocation(), () -> giveEffects(team, applier));
		return true;
	}

	private static void giveEffects(TeamArenaTeam team, Player applier) {
		Component message = Component.textOfChildren(
			applier.playerListName(),
			Component.text(" has given your team "),
			Component.text("Haste II", HASTE_COLOR),
			Component.text(" for 30 seconds!")
		).color(COLOR);
		Component messageExisting = message.append(
			Component.text("\nSince you already have Haste, the duration has been added onto your existing effect.", TextUtils.betterDarken(COLOR))
		);
		for (Player member : team.getPlayerMembers()) {
			PotionEffect existing = member.getPotionEffect(PotionEffectType.HASTE);
			if (existing != null && existing.getAmplifier() == HASTE_EFFECT.getAmplifier()) {
				// increment duration
				member.addPotionEffect(HASTE_EFFECT.withDuration(HASTE_EFFECT.getDuration() + existing.getDuration()));
				member.sendMessage(messageExisting);
				member.playSound(member, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1, 1);
			} else {
				member.addPotionEffect(HASTE_EFFECT);
				member.sendMessage(message);
				member.playSound(member, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1, 1);
			}
		}
	}
}

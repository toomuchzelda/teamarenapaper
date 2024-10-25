package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades.HasteOreAction;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades.HealOreAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.entity.Player;

public enum StatusOreType
{
	HEAL(Color.GREEN, Component.text("LifeOre Heal"),
		"Use on your Life Ore to repair it",
		6 * 20,
		new HealOreAction()),


	HASTE(Color.ORANGE, Component.text("Team Haste"),
		"Use on your Life Ore to speed up your team's digging",
		10 * 20,
		new HasteOreAction());

	public final Color color;
	public final Component displayName;
	public final String description;
	public final int regenTime; // Ticks to regenerate a broken ore

	public final BuffAction action;

	StatusOreType(Color color, Component displayName, String description, int regenTime, BuffAction action) {
		this.regenTime = regenTime;
		this.color = color;
		this.displayName = displayName.color(this.getTextColor());
		this.description = description;

		this.action = action;
	}

	public TextColor getTextColor() {
		return TextColor.color(this.color.asRGB());
	}

	/** Determines what the StatusOreType actually does */
	public interface BuffAction {
		/** Return number of the items used in the action. 0 if not used at all / nothing applied */
		int buff(final Player redeemer, int required, int itemsUsed, TeamLifeOres ore, DigAndBuild gameInstance);
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.statusorebuffactions.HasteOreAction;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.statusorebuffactions.HealOreAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;

public enum StatusOreType
{
	HEAL(Color.GREEN, Component.text("LifeOre Heal"),
		"Trade at the canteen to repair your Life Ore",
		6 * 20,
		new HealOreAction()),


	HASTE(Color.ORANGE, Component.text("Team Haste"),
		"Trade at the canteen to speed up your team's digging",
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
		int buff(final Player redeemer, int required, int itemsUsed, LifeOre ore, DigAndBuild gameInstance);
	}
}

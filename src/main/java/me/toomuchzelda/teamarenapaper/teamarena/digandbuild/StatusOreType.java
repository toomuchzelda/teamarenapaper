package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;

public enum StatusOreType
{
	HEAL(Color.GREEN, Component.text("LifeOre Heal"),
		"Trade at the canteen to repair your Life Ore",
		6 * 20),
	HASTE(Color.ORANGE, Component.text("Team Haste"),
		"Trade at the canteen to speed up your team's digging",
		10 * 20);

	public final Color color;
	public final Component displayName;
	public final String description;
	public final int regenTime; // Ticks to regenerate a broken ore

	StatusOreType(Color color, Component displayName, String description, int regenTime) {
		this.regenTime = regenTime;
		this.color = color;
		this.displayName = displayName.color(this.getTextColor());
		this.description = description;
	}

	public TextColor getTextColor() {
		return TextColor.color(this.color.asRGB());
	}
}

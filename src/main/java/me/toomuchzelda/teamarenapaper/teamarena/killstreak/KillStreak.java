package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Abstract killstreak class.
 * All killstreaks must have:
 * - Name
 * - Description
 * - Some colour distinct from other killstreaks.
 * - An item that the player interacts with to use the killstreak
 * - And the code to do the killstreak thing in the form of an Abilit(y|ies)

 * @author toomuchzelda
 */
public abstract class KillStreak
{
	private final String name;
	private final String description;
	private final TextColor textColor;
	private final ItemStack item;

	private final List<Ability> abilities;

	KillStreak(String name, String description, TextColor color, ItemStack item, Ability... abilities) {
		this.name = name;
		this.description = description;
		this.textColor = color;
		this.item = item;

		// These abilities are put into player's PlayerInfo and then called by Bukkit EventHandlers appropriately.
		this.abilities = List.of(abilities);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public TextColor getTextColor() {
		return textColor;
	}

	public ItemStack getItem() {
		return item;
	}

	public List<Ability> getAbilities() {
		return this.abilities;
	}
}

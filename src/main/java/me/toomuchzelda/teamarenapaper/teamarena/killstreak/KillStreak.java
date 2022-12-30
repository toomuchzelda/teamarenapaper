package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Abstract killstreak class.
 * All killstreaks must have:
 * - Name
 * - Description
 * - Some colour distinct from other killstreaks.
 * - An item that the player interacts with to use the killstreak
 * - And the code to do the killstreak thing in the form of an Abilit(y|ies) (or none)
 *
 * Killstreak can also specify if it is given to the player instantly or delivered by a crate

 * @author toomuchzelda
 */
public abstract class KillStreak
{
	private final String name;
	private final String description;
	private final TextColor textColor;
	private final Component componentName;
	private final ItemStack item;

	protected Material crateItemType; // Item used to call the crate.
	protected Material crateBlockType; // Type of block the falling crate should be.

	private final List<Ability> abilities;

	KillStreak(String name, String description, TextColor color, ItemStack item, Ability... abilities) {
		this.name = name;
		this.description = description;
		this.textColor = color;
		this.componentName = Component.text(this.name, this.textColor);
		this.item = item;

		// These abilities are put into player's PlayerInfo and then called by Bukkit EventHandlers appropriately.
		this.abilities = List.of(abilities);
	}

	public void giveStreak(Player player, PlayerInfo pinfo) {
		if(this.getItem() != null)
			player.getInventory().addItem(this.getItem());

		this.getAbilities().forEach(ability -> {
			Ability.giveAbility(player, ability, pinfo);
		});
	}

	Material getCrateItemType() {
		return this.crateItemType;
	}

	public Material getCrateBlockType() {
		return crateBlockType;
	}

	public void onFireworkFinish(Player player, Location destination, Crate crate) {}

	public void onCrateLand(Player player, Location destination) {}

	 public boolean isDeliveredByCrate() {
		return false;
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

	public Component getComponentName() {
		return this.componentName;
	}

	public @Nullable ItemStack getItem() {
		return item;
	}

	public List<Ability> getAbilities() {
		return this.abilities;
	}
}

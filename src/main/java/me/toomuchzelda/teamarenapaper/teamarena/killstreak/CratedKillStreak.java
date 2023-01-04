package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate.CratePayload;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link KillStreak} that is delivered by a crate.
 */
public abstract class CratedKillStreak extends KillStreak {
	CratedKillStreak(String name, String description, TextColor color, ItemStack item, Ability... abilities) {
		super(name, description, color, item, abilities);
	}

	/**
	 * Gets the payload of the crate.
	 */
	@NotNull
	public abstract CratePayload getPayload(Player player, Location destination);

	/**
	 * Returns if the payload needs to be protected with a parachute.
	 */
	public boolean isPayloadFragile(Player player, Location destination) {
		return true;
	}

	/**
	 *
	 * @implNote Implementations are not required to make the {@link ItemStack} "unique".
	 */
	@NotNull
	public ItemStack createCrateItem(Player player) {
		return createSimpleCrateItem(Material.COMMAND_BLOCK); // placeholder
	}


	private static final List<Component> USE_MSG = List.of(
		Component.text("Right Click on the ground to summon", TextUtils.RIGHT_CLICK_TO),
		Component.text("this Killstreak to that location", TextUtils.RIGHT_CLICK_TO)
	);
	protected final ItemStack createSimpleCrateItem(Material material) {
		List<Component> lore = new ArrayList<>(5);
		lore.add(getComponentName());
		lore.addAll(TextUtils.wrapString(getDescription(), TextUtils.PLAIN_STYLE, TextUtils.DEFAULT_WIDTH));
		lore.addAll(USE_MSG);

		return ItemBuilder.of(material)
			.displayName(Component.textOfChildren(
				Component.text("Summon ", NamedTextColor.LIGHT_PURPLE),
				getComponentName()
			))
			.lore(lore)
			.enchant(Enchantment.DURABILITY, 1)
			.hide(ItemFlag.HIDE_ENCHANTS)
			.build();
	}

	public void onCratePlace(Player player, Location destination) {}

	public void onFireworkFinish(Player player, Location destination, Crate crate) {}

	public void onCrateTick(Player player, Location destination, int timeElapsed) {}

	public void onCrateLand(Player player, Location destination) {
		destination.getWorld().playSound(destination, Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);
	}

}

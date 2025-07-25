package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.TriState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a building in the world.
 * Note that the building will not be in the world before {@link Building#onPlace()} is called.
 * As such, in-world side effects before then are discouraged.
 */
public abstract sealed class Building permits BlockBuilding, EntityBuilding {
	public final Player owner;
	protected Location location;
	private RealHologram hologram;
	String name;
	private ItemStack icon = NO_ICON;
	private NamedTextColor outlineColor = NamedTextColor.WHITE;
	boolean invalid;

	public Building(Player player, Location loc) {
		this.owner = player;
		this.location = loc.clone();
	}

	private static final ItemStack NO_ICON = new ItemStack(Material.BARRIER);

	public Location getLocation() {
		return this.location.clone();
	}

	public void setLocation(Location newLoc) {
		this.location = newLoc.clone();
	}

	public void setText(Component @Nullable ... newText) {
		if (newText == null) {
			if (hologram != null)
				hologram.remove();
		} else if (hologram == null) {
			hologram = new RealHologram(getHologramLocation(), RealHologram.Alignment.TOP, newText);
		} else {
			// TODO telepot the hologram
			hologram.setText(newText);
		}
	}

	protected Location getHologramLocation() {
		return getLocation();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ItemStack getIcon() {
		return icon.clone();
	}

	public void setIcon(ItemStack icon) {
		this.icon = icon.clone();
	}

	public TextColor getOutlineColor() {
		return outlineColor;
	}

	public void setOutlineColor(TextColor color) {
		this.outlineColor = NamedTextColor.nearestTo(color);
	}

	/**
	 * {@return Whether owners should have their outline visibility overridden}
	 */
	@NotNull
	public TriState isOutlineVisibleToOwner() {
		return TriState.NOT_SET;
	}

	public void onPlace() {

	}

	public void onTick() {

	}

	public void onDestroy() {
		invalid = true;
		if (hologram != null)
			hologram.remove();
	}

	protected void markInvalid() {
		this.invalid = true;
	}
}

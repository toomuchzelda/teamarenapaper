package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract sealed class Building permits BlockBuilding, EntityBuilding {
	public final Player owner;
	protected Location location;
	private RealHologram hologram;
	String name;
	private ItemStack icon = NO_ICON;
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

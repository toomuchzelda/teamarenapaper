package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public abstract class Building {
	public Player owner;
	protected Location location;
	private RealHologram hologram;
	String name;
	boolean invalid;

	public Building(Player player, Location loc) {
		this.owner = player;
		this.location = loc.clone();
	}

	public Location getLocation() {
		return this.location.clone();
	}

	public void setLocation(Location newLoc) {
		this.location = newLoc;
	}

	public void setText(Component @Nullable ... newText) {
		if (newText == null) {
			if (hologram != null)
				hologram.remove();
		} else if (hologram == null) {
			hologram = new RealHologram(getHologramLocation(), newText);
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

	public void onPlace() {

	}

	public void onTick() {

	}

	public void onDestroy() {
		invalid = true;
		if (hologram != null)
			hologram.remove();
	}
}

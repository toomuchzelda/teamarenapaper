package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Little abstraction for a point marker
 *
 * @author toomuchzelda
 */
public class PointMarker
{
	private final TextDisplay textDisplay;
	private final ItemDisplay itemDisplay;

	private Component currentText; // Cache textDisplay Component for faster .equals() comparison

	public PointMarker(Location loc, Component pointName, Color color, Material mat) {
		this(loc, pointName, color, mat, false);
	}

	public PointMarker(Location loc, Component pointName, Color color, Material mat, boolean smallItem) {
		this.currentText = pointName;
		this.textDisplay = loc.getWorld().spawn(smallItem ? loc.clone().subtract(0d, 0.6d, 0d) : loc,
			TextDisplay.class, display -> {

			display.text(pointName);
			display.setAlignment(TextDisplay.TextAlignment.CENTER);
			display.setBillboard(Display.Billboard.CENTER);
			display.setSeeThrough(true);

			display.setBrightness(new Display.Brightness(15, 15));
			display.setGlowColorOverride(color);

			// Scale to make larger
			if (!smallItem)
				display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(2.5f, 2.5f, 2.5f), new AxisAngle4f()));

			display.setGlowing(true);
		});

		this.itemDisplay = loc.getWorld().spawn(loc.clone().subtract(0d, 1d, 0d), ItemDisplay.class, display -> {
			if (smallItem) {
				display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(.5f, .5f, .5f), new AxisAngle4f()));
				display.setBillboard(Display.Billboard.VERTICAL);
			}
			else
				display.setBillboard(Display.Billboard.CENTER);

			display.setBrightness(new Display.Brightness(15, 15));
			display.setGlowColorOverride(color);

			display.setItemStack(new ItemStack(mat));

			display.setGlowing(true);
		});
	}

	public void setText(Component newText) {
		if (!Objects.equals(this.currentText, newText)) {
			this.currentText = newText;
			this.textDisplay.text(newText);
		}
	}

	public void remove() {
		this.textDisplay.remove();
		this.itemDisplay.remove();
	}
}

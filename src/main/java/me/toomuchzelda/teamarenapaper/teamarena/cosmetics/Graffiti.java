package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Graffiti extends CosmeticItem {

	public final BufferedImage image;
	public Graffiti(File file, File companionFile) {
		super(file, companionFile);
		try {
			BufferedImage image = ImageIO.read(file);

			if (image.getWidth() != 128 || image.getHeight() != 128) {
				Main.logger().warning("File " + file.getName() + " is not 128*128, resizing automatically");
				image = resizeImage(image);
			}
			this.image = image;
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read image " + file.getPath(), ex);
		}
	}

	private static BufferedImage resizeImage(BufferedImage original) {
		Image scaled = original.getScaledInstance(128, 128, Image.SCALE_FAST);
		BufferedImage ret = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);

		var graphics = ret.createGraphics();
		graphics.drawImage(scaled, 0, 0, null);
		graphics.dispose();

		return ret;
	}


	private static final World dummyWorld = Bukkit.getWorlds().get(0);
	private static MapView getNextMapView() {
		MapView view = Bukkit.createMap(dummyWorld);
		view.removeRenderer(view.getRenderers().get(0));

		// hope that the file is removed on exit
		File mainWorldFile = new File(Bukkit.getWorldContainer(), dummyWorld.getName());
		File mapDataFile = new File(mainWorldFile, "data" + File.separator + "map_" + view.getId() + ".dat");
		mapDataFile.deleteOnExit();
		return view;
	}

	MapView cachedMapView = null;
	public MapView getMapView() {
		if (cachedMapView == null) {
			cachedMapView = getNextMapView();
			cachedMapView.addRenderer(new MapRenderer() {
				boolean drawn = false;
				@Override
				public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
					if (drawn)
						return;
					drawn = true;
					canvas.drawImage(0, 0, image);
				}
			});
		}
		return cachedMapView;
	}

	@Override
	public ItemStack getDisplay(boolean complex) {
		return ItemBuilder.of(complex ? Material.FILLED_MAP : Material.MAP)
			.displayName(Component.text(name, NamedTextColor.GOLD))
			.lore(getInfo())
			.hideAll()
			.meta(MapMeta.class, mapMeta -> {
				if (complex) {
					mapMeta.setMapView(getMapView());
				}
			})
			.build();
	}
}

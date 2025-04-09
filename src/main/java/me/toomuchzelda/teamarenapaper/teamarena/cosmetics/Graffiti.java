package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class Graffiti extends CosmeticItem {
	BufferedImage[] frames;
	int frameTime;
	public Graffiti(NamespacedKey key, File file, YamlConfiguration info) {
		super(key, file, info);
		frameTime = info.getInt("frame-time", 1);

		File imageFile = new File(file.getParent(), file.getName().substring(0, file.getName().lastIndexOf('.')));
		try {
			BufferedImage image = ImageIO.read(imageFile);

			if (image.getWidth() != 128 || image.getHeight() % 128 != 0) {
				throw new IllegalArgumentException("File " + imageFile.getName() + " is not in a valid format (W: 128, H: multiple of 128)");
			}
			int frameCount = image.getHeight() / 128;
			this.frames = new BufferedImage[frameCount];

			for (int i = 0; i < frameCount; i++) {
				frames[i] = image.getSubimage(0, i * 128, 128, 128);
			}
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to read image " + imageFile.getPath(), ex);
		}
	}

	@Override
	public CosmeticType getCosmeticType() {
		return CosmeticType.GRAFFITI;
	}

	@Override
	public void unload() {
		if (cachedMapViews != null) {
			for (var view : cachedMapViews)
				view.getRenderers().forEach(view::removeRenderer);
		}
	}

	public boolean isAnimated() {
		return frames.length != 1;
	}

	private static final World dummyWorld = Bukkit.getWorlds().get(0);
	protected static MapView getNextMapView() {
		MapView view = Bukkit.createMap(dummyWorld);
		view.removeRenderer(view.getRenderers().get(0));

		// hope that the file is removed on exit
		File mainWorldFile = new File(Bukkit.getWorldContainer(), dummyWorld.getName());
		File mapDataFile = new File(mainWorldFile, "data" + File.separator + "map_" + view.getId() + ".dat");
		mapDataFile.deleteOnExit();
		return view;
	}

	MapView[] cachedMapViews;
	ItemStack[] cachedMaps;
	protected void createMapView() {
		cachedMapViews = new MapView[frames.length];
		cachedMaps = new ItemStack[frames.length];
		for (int i = 0; i < frames.length; i++) {
			var mapView = getNextMapView();

			BufferedImage frame = frames[i];
			mapView.addRenderer(new MapRenderer() {
				boolean drawn = false;
				@Override
				public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
					if (drawn)
						return;
					drawn = true;
					canvas.drawImage(0, 0, frame);
				}
			});

			cachedMapViews[i] = mapView;
			ItemStack stack = cachedMaps[i] = new ItemStack(Material.FILLED_MAP);
			ItemMeta meta = stack.getItemMeta();
			((MapMeta) meta).setMapView(mapView);
			stack.setItemMeta(meta);
		}

		sendMapView();
	}

	final Set<Player> mapsSent = Collections.newSetFromMap(new WeakHashMap<>());
	public void sendMapView() {
		// send players the map
		Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (mapsSent.add(player)) {
					for (int i = 0; i < frames.length; i++) {
						player.sendMap(cachedMapViews[i]);
					}
				}
			}
		});
	}

	public void sendMapView(Player player) {
		if (mapsSent.add(player)) {
			Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
				for (int i = 0; i < frames.length; i++) {
					player.sendMap(cachedMapViews[i]);
				}
			});
		}
	}

	public MapView getMapView() {
		if (cachedMapViews == null) {
			createMapView();
		}
		if (frames.length == 1)
			return cachedMapViews[0];
		int now = TeamArena.getGameTick();
		return cachedMapViews[now / frameTime % frames.length];
	}

	public ItemStack getMapItem() {
		if (cachedMaps == null) {
			createMapView();
		}
		if (frames.length == 1)
			return cachedMaps[0];
		int now = TeamArena.getGameTick();
		return cachedMaps[now / frameTime % frames.length];
	}

	@Override
	public @NotNull ItemStack getDisplay(boolean complex) {
		if (!complex)
			return super.getDisplay(false);
		return ItemBuilder.of(Material.FILLED_MAP)
			.displayName(name)
			.lore(getExtraInfo())
			.hideAll()
			.meta(MapMeta.class, mapMeta -> mapMeta.setMapView(getMapView()))
			.build();
	}
}

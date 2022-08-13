package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.utils.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockSupport;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class GraffitiManager {
	public static final int GRAFFITI_DURATION = 20 * 20;

	final TeamArena game;
	final Logger logger;
	private static final World dummyWorld = Bukkit.getWorlds().get(0);
	final Map<NamespacedKey, BufferedImage> loadedGraffiti = new HashMap<>();
	static final Deque<MapView> pooledMapView = new ArrayDeque<>();
	final Map<ItemFrame, MapView> spawnedMaps = new LinkedHashMap<>();

	public GraffitiManager(TeamArena game) {
		this.game = game;
		this.logger = Main.logger();

		loadNamespace(new File("graffiti"));
	}

	public Set<NamespacedKey> getAllGraffiti() {
		return Collections.unmodifiableSet(loadedGraffiti.keySet());
	}

	public void spawnGraffiti(Player player, NamespacedKey graffiti) {
		var eyeLocation = player.getEyeLocation();
		RayTraceResult result = player.getWorld().rayTrace(eyeLocation, eyeLocation.getDirection(), 5,
			FluidCollisionMode.NEVER, true, 0, ignored -> false);
		if (result == null || result.getHitBlock() == null || result.getHitBlockFace() == null)
			return;
		var block = result.getHitBlock();
		var blockFace = result.getHitBlockFace();
		if (!block.getBlockData().isFaceSturdy(blockFace, BlockSupport.FULL))
			return;
		spawnGraffiti(block, blockFace, player.getFacing(), graffiti);
	}

	public void spawnGraffiti(Block attached, BlockFace blockFace, BlockFace xzDirection, NamespacedKey graffiti) {
		BufferedImage image = loadedGraffiti.get(graffiti);
		if (image == null)
			return;

		var mapView = getNextMapView();
		mapView.addRenderer(new MapRenderer() {
			boolean drawn = false;
			@Override
			public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
				if (drawn)
					return;
				drawn = true;
				canvas.drawImage(0, 0, image);
			}
		});
		var stack = ItemBuilder.of(Material.FILLED_MAP)
			.meta(MapMeta.class, mapMeta -> mapMeta.setMapView(mapView))
			.build();

		var world = attached.getWorld();
		var itemFrame = world.spawn(attached.getRelative(blockFace).getLocation(), ItemFrame.class, frame -> {
			frame.setItemDropChance(0);
			frame.setItem(stack, false);
			frame.setVisible(false);
			frame.setFixed(true);
			frame.setFacingDirection(blockFace, true);
			if (blockFace == BlockFace.UP || blockFace == BlockFace.DOWN) {
				// NESW
				frame.setRotation(Rotation.values()[xzDirection.ordinal()]);
			}
		});
		world.playSound(itemFrame, Sound.ENTITY_SILVERFISH_HURT, 0.2f, 1);
		spawnedMaps.put(itemFrame, mapView);
	}

	private static MapView getNextMapView() {
		MapView pooled = pooledMapView.poll();
		if (pooled == null) {
			pooled = Bukkit.createMap(dummyWorld);
			pooled.removeRenderer(pooled.getRenderers().get(0));

			// hope that the file is removed on exit
			File mainWorldFile = new File(Bukkit.getWorldContainer(), dummyWorld.getName());
			File mapDataFile = new File(mainWorldFile, "data" + File.separator + "map_" + pooled.getId() + ".dat");
			mapDataFile.deleteOnExit();
		}
		return pooled;
	}

	private static void releaseMapView(MapView view) {
		view.getRenderers().forEach(view::removeRenderer);
		pooledMapView.add(view);
	}

	public void tick() {
		spawnedMaps.entrySet().removeIf(entry -> {
			var itemFrame = entry.getKey();
			var mapView = entry.getValue();

			if (itemFrame.getTicksLived() >= GRAFFITI_DURATION) {
				itemFrame.setItem(null, false);
				itemFrame.remove();
				releaseMapView(mapView);
				return true;
			}
			return false;
		});
	}

	public void cleanUp() {
		spawnedMaps.forEach((itemFrame, mapView) -> {
			itemFrame.setItem(null, false);
			itemFrame.remove();
			releaseMapView(mapView);
		});
		spawnedMaps.clear();
		loadedGraffiti.clear();
	}

	public void loadNamespace(File root) {
		File[] namespaceFolders = root.listFiles();
		if (namespaceFolders == null)
			return;
		for (File folder : namespaceFolders) {
			loadGraffiti(folder.getName(), "graffiti/", folder);
		}
	}

	public void loadGraffiti(String namespace, String prefix, File directory) {
		File[] files = directory.listFiles();
		if (files == null)
			return;
		for (File file : files) {
			String fileName = file.getName();
			if (file.isDirectory()) {
				loadGraffiti(namespace, prefix + fileName + "/", directory);
				continue;
			}

			FileUtils.FileInfo fileInfo = FileUtils.getFileExtension(fileName);

			try {
				BufferedImage image = ImageIO.read(file);
				NamespacedKey key = new NamespacedKey(namespace, prefix + fileInfo.fileName());

				if (image.getWidth() != 128 || image.getHeight() != 128) {
					logger.warning("File " + key + " is not 128*128, skipping");
					continue;
				}

				loadedGraffiti.put(key, image);
				logger.info("Loaded " + file.getPath() + " as " + key);
			} catch (IOException ex) {
				logger.warning("Failed to read image " + file.getPath());
			}
		}
	}
}

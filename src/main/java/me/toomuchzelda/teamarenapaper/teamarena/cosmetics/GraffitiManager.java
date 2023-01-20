package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockSupport;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class GraffitiManager {
	public static final int GRAFFITI_DURATION = 20 * 20;

	final TeamArena game;
	final Logger logger;
	private static final World dummyWorld = Bukkit.getWorlds().get(0);
	static final Deque<MapView> pooledMapView = new ArrayDeque<>();
	final Map<ItemFrame, MapView> spawnedMaps = new LinkedHashMap<>();

	public GraffitiManager(TeamArena game) {
		this.game = game;
		this.logger = Main.logger();
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

	public void spawnGraffiti(Block attached, BlockFace blockFace, BlockFace xzDirection, NamespacedKey key) {
		Graffiti graffiti = CosmeticsManager.getCosmetic(CosmeticType.GRAFFITI, key);
		if (graffiti == null)
			return;
		BufferedImage image = graffiti.image;

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
	}

	private static final WeakHashMap<Player, Integer> itemSwapTimes = new WeakHashMap<>();
	private static final int ITEM_SWAP_DURATION = 20;
	private static final WeakHashMap<Player, Integer> graffitiCooldown = new WeakHashMap<>();
	private static final int GRAFFITI_COOLDOWN = 40 * 20;
	public void onSwapHandItems(PlayerSwapHandItemsEvent e) {
		Player player = e.getPlayer();
		int now = TeamArena.getGameTick();
		Integer lastSwapTick = itemSwapTimes.put(player, now);
		if (lastSwapTick != null && now - lastSwapTick <= ITEM_SWAP_DURATION) {
			PlayerInfo playerInfo = Main.getPlayerInfo(player);
			Optional<NamespacedKey> selected = playerInfo.getSelectedCosmetic(CosmeticType.GRAFFITI);
			if (selected.isPresent()) {
				int lastGraffiti = graffitiCooldown.getOrDefault(player, 0);
				int ticksElapsed = now - lastGraffiti;
				if (ticksElapsed >= GRAFFITI_COOLDOWN) {
					graffitiCooldown.put(player, now);
					spawnGraffiti(player, selected.get());
				} else {
					player.sendActionBar(Component.text("Graffiti cooldown: " +
						TextUtils.ONE_DECIMAL_POINT.format((GRAFFITI_COOLDOWN - ticksElapsed) / 20f) + "s",
						NamedTextColor.RED));
				}
			} else {
				player.sendMessage(Component.text("Please select a graffiti first!", TextColors.ERROR_RED));
			}
		}
	}
}

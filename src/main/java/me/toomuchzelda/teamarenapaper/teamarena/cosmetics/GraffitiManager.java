package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockSupport;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class GraffitiManager {
	public static final int GRAFFITI_DURATION = 20 * 20;

	final Map<BlockCoords, ItemFrame> spawnedMaps = new LinkedHashMap<>();
	final Map<UUID, ItemFrame> playerPlacedMaps = new HashMap<>();

	final Map<ItemFrame, Graffiti> animatedGraffiti = new LinkedHashMap<>();

	public GraffitiManager(TeamArena game) {
	}

	public boolean spawnGraffiti(Player player, NamespacedKey graffiti) {
		var eyeLocation = player.getEyeLocation();
		RayTraceResult result = player.getWorld().rayTrace(eyeLocation, eyeLocation.getDirection(), 5,
			FluidCollisionMode.NEVER, true, 0, ignored -> false);
		if (result == null || result.getHitBlock() == null || result.getHitBlockFace() == null)
			return false;
		var block = result.getHitBlock();
		var blockData = block.getBlockData();
		var blockFace = result.getHitBlockFace();
		if (blockData instanceof Door) // disallowed at zelda's request
			return false;
		if (!blockData.isFaceSturdy(blockFace, BlockSupport.FULL))
			return false;
		return spawnGraffiti(player.getUniqueId(), block, blockFace, player.getFacing(), graffiti);
	}

	public boolean spawnGraffiti(UUID owner, Block attached, BlockFace blockFace, BlockFace xzDirection, NamespacedKey key) {
		Graffiti graffiti = CosmeticsManager.getCosmetic(CosmeticType.GRAFFITI, key);
		if (graffiti == null) {
			Main.logger().info("Graffiti " + key + " was null?");
			return false;
		}

		var mapView = graffiti.getMapView();
		var stack = ItemBuilder.of(Material.FILLED_MAP)
			.meta(MapMeta.class, mapMeta -> mapMeta.setMapView(mapView))
			.build();

		World world = attached.getWorld();
		Location location = attached.getRelative(blockFace).getLocation();
		BlockCoords coords = new BlockCoords(location);
		ItemFrame old = playerPlacedMaps.get(owner);
		ItemFrame sameBlock = spawnedMaps.get(coords);
		if (sameBlock != null) {
			if (sameBlock.equals(old)) {
				// player owns the graffiti, replace it with new art
				old.setItem(stack, false);
				world.playSound(old, Sound.ENTITY_SILVERFISH_HURT, 0.2f, 1);
				if (graffiti.isAnimated()) {
					graffiti.sendMapView();
					animatedGraffiti.put(old, graffiti);
				} else {
					animatedGraffiti.remove(old);
				}
				return true;
			} else {
				return false; // prevent Z-fighting
			}
		} else if (old != null) {
			spawnedMaps.remove(new BlockCoords(old.getLocation()));
			old.remove();
			animatedGraffiti.remove(old);
		}

		var itemFrame = world.spawn(location, ItemFrame.class, frame -> {
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
		spawnedMaps.put(coords, itemFrame);
		playerPlacedMaps.put(owner, itemFrame);
		if (graffiti.isAnimated()) {
			graffiti.sendMapView();
			animatedGraffiti.put(itemFrame, graffiti);
		}
		return true;
	}

	public void tick() {
		animatedGraffiti.forEach((frame, graffiti) -> frame.setItem(graffiti.getMapItem(), false));
//		spawnedMaps.values().removeIf(itemFrame -> {
//			if (itemFrame.getTicksLived() >= GRAFFITI_DURATION) {
//				itemFrame.setItem(null, false);
//				itemFrame.remove();
//				return true;
//			}
//			return false;
//		});
	}

	public void cleanUp() {
		spawnedMaps.values().forEach(itemFrame -> {
			itemFrame.setItem(null, false);
			itemFrame.remove();
		});
		spawnedMaps.clear();
		playerPlacedMaps.clear();
		animatedGraffiti.clear();
	}

	private static final Map<UUID, Integer> itemSwapTimes = new HashMap<>();
	private static final int ITEM_SWAP_DURATION = 20;
	private static final Map<UUID, Integer> graffitiCooldown = new HashMap<>();
	private static final int GRAFFITI_COOLDOWN = 50; // to prevent spamming noise
	private static final HashSet<UUID> setCosmeticReminder = new HashSet<>();
	public void onSwapHandItems(PlayerSwapHandItemsEvent e) {
		Player player = e.getPlayer();
		UUID uuid = player.getUniqueId();
		if (Main.getGame().isDead(player)) // dead players can't spray graffiti
			return;

		int now = TeamArena.getGameTick();
		Integer lastSwapTick = itemSwapTimes.get(uuid);
		// check if the players swapped hands twice within 20 ticks
		if (lastSwapTick == null || now - lastSwapTick > ITEM_SWAP_DURATION) {
			itemSwapTimes.put(uuid, now);
			return;
		}
		itemSwapTimes.put(uuid, 0); // reset swap time to avoid consecutive triggers
		PlayerInfo playerInfo = Main.getPlayerInfo(player);
		Optional<NamespacedKey> optionalGraffiti = playerInfo.getSelectedCosmetic(CosmeticType.GRAFFITI);
		NamespacedKey graffiti = optionalGraffiti.orElseGet(() -> {
			// randomly pick an owned graffiti
			var owned = playerInfo.getCosmeticItems(CosmeticType.GRAFFITI);
			if (owned.size() == 0)
				return null;
			if (setCosmeticReminder.add(uuid)) {
				player.sendMessage(Component.textOfChildren(
					Component.text("You can pick a graffiti you like "),
					Component.text("here", Style.style(TextDecoration.UNDERLINED))
						.clickEvent(ClickEvent.runCommand("/cosmetics gui"))
						.hoverEvent(Component.text("Click to run /cosmetics gui", NamedTextColor.YELLOW).asHoverEvent()),
					Component.text("!")
				).color(NamedTextColor.AQUA));
			}
			var arr = owned.toArray(new NamespacedKey[0]);
			return arr[MathUtils.random.nextInt(arr.length)];
		});
		if (graffiti == null) // fail-fast
			return;
		int lastGraffiti = graffitiCooldown.getOrDefault(uuid, 0);
		int ticksElapsed = now - lastGraffiti;
		if (ticksElapsed >= GRAFFITI_COOLDOWN || playerInfo.permissionLevel == PermissionLevel.OWNER) {
			if (spawnGraffiti(player, graffiti)) {
				graffitiCooldown.put(uuid, now);
			}
		} else {
			player.sendActionBar(Component.text("Graffiti cooldown: " +
					TextUtils.formatNumber((GRAFFITI_COOLDOWN - ticksElapsed) / 20f) + "s",
				NamedTextColor.RED));
		}
	}
}

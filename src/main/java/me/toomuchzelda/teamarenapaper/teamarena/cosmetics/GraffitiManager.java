package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
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

	final TeamArena game;
	final List<ItemFrame> spawnedMaps = new ArrayList<>();

	public GraffitiManager(TeamArena game) {
		this.game = game;
	}

	public void spawnGraffiti(Player player, NamespacedKey graffiti) {
		var eyeLocation = player.getEyeLocation();
		RayTraceResult result = player.getWorld().rayTrace(eyeLocation, eyeLocation.getDirection(), 5,
			FluidCollisionMode.NEVER, true, 0, ignored -> false);
		if (result == null || result.getHitBlock() == null || result.getHitBlockFace() == null)
			return;
		var block = result.getHitBlock();
		var blockData = block.getBlockData();
		var blockFace = result.getHitBlockFace();
		if (blockData instanceof Door) // disallowed at zelda's request
			return;
		if (!blockData.isFaceSturdy(blockFace, BlockSupport.FULL))
			return;
		spawnGraffiti(block, blockFace, player.getFacing(), graffiti);
	}

	public void spawnGraffiti(Block attached, BlockFace blockFace, BlockFace xzDirection, NamespacedKey key) {
		Graffiti graffiti = CosmeticsManager.getCosmetic(CosmeticType.GRAFFITI, key);
		if (graffiti == null) {
			Main.logger().info("Graffiti " + key + " was null?");
			return;
		}

		var mapView = graffiti.getMapView();
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
		spawnedMaps.add(itemFrame);
	}

	public void tick() {
		spawnedMaps.removeIf(itemFrame -> {
			if (itemFrame.getTicksLived() >= GRAFFITI_DURATION) {
				itemFrame.setItem(null, false);
				itemFrame.remove();
				return true;
			}
			return false;
		});
	}

	public void cleanUp() {
		spawnedMaps.forEach(itemFrame -> {
			itemFrame.setItem(null, false);
			itemFrame.remove();
		});
		spawnedMaps.clear();
	}

	private static final WeakHashMap<Player, Integer> itemSwapTimes = new WeakHashMap<>();
	private static final int ITEM_SWAP_DURATION = 20;
	private static final WeakHashMap<Player, Integer> graffitiCooldown = new WeakHashMap<>();
	private static final int GRAFFITI_COOLDOWN = 40 * 20;
	private static final HashSet<UUID> setCosmeticReminder = new HashSet<>();
	public void onSwapHandItems(PlayerSwapHandItemsEvent e) {
		Player player = e.getPlayer();
		int now = TeamArena.getGameTick();
		Integer lastSwapTick = itemSwapTimes.get(player);
		// check if the players swapped hands twice within 20 ticks
		if (lastSwapTick == null || now - lastSwapTick > ITEM_SWAP_DURATION) {
			itemSwapTimes.put(player, now);
			return;
		}
		itemSwapTimes.put(player, 0); // reset swap time to avoid consecutive triggers
		PlayerInfo playerInfo = Main.getPlayerInfo(player);
		Optional<NamespacedKey> optionalGraffiti = playerInfo.getSelectedCosmetic(CosmeticType.GRAFFITI);
		NamespacedKey graffiti = optionalGraffiti.orElseGet(() -> {
			// randomly pick an owned graffiti
			var owned = playerInfo.getCosmeticItems(CosmeticType.GRAFFITI);
			if (owned.size() == 0)
				return null;
			if (setCosmeticReminder.add(player.getUniqueId())) {
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
		int lastGraffiti = graffitiCooldown.getOrDefault(player, 0);
		int ticksElapsed = now - lastGraffiti;
		if (ticksElapsed >= GRAFFITI_COOLDOWN || playerInfo.permissionLevel == CustomCommand.PermissionLevel.OWNER) {
			graffitiCooldown.put(player, now);
			spawnGraffiti(player, graffiti);
		} else {
			player.sendActionBar(Component.text("Graffiti cooldown: " +
				TextUtils.ONE_DECIMAL_POINT.format((GRAFFITI_COOLDOWN - ticksElapsed) / 20f) + "s",
				NamedTextColor.RED));
		}
	}
}

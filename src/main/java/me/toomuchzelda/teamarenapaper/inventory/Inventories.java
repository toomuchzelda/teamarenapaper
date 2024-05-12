package me.toomuchzelda.teamarenapaper.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import io.papermc.paper.adventure.PaperAdventure;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jacky8399
 */
public final class Inventories implements Listener {
    private static final WeakHashMap<Player, Inventory> playerInventories = new WeakHashMap<>();
    private static final WeakHashMap<Inventory, InventoryData> pluginInventories = new WeakHashMap<>();

	private record ManagedSign(BlockCoords location, BlockState originalState, CompletableFuture<String> future) {}
	private static final WeakHashMap<Player, ManagedSign> managedSigns = new WeakHashMap<>();
    public static Inventories INSTANCE = new Inventories();

	private static Logger logger;

    public static boolean debug = false;

    private Inventories() {
		logger = Main.logger();

        Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), Inventories::tick, 1, 1);

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Main.getPlugin(), PacketType.Play.Client.UPDATE_SIGN) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				Player player = event.getPlayer();
				ManagedSign managedSign = managedSigns.remove(player);
				if (managedSign == null) // ignore if not our sign
					return;
				ServerboundSignUpdatePacket packet = (ServerboundSignUpdatePacket) event.getPacket().getHandle();
				// put it back if not the correct sign somehow
				if (packet.getPos().getX() != managedSign.location.x() ||
					packet.getPos().getY() != managedSign.location.y() ||
					packet.getPos().getZ() != managedSign.location.z()) {
					managedSigns.put(player, managedSign);
					return;
				}

				event.setCancelled(true);
				String[] lines = packet.getLines();
				String fullMessage = lines[0];
				if (!lines[1].isBlank()) {
					fullMessage += " " + lines[1];
				}
				managedSign.future.complete(fullMessage); // not null
				player.sendBlockChanges(List.of(managedSign.originalState), true);
			}
		});
    }

    public static void tick() {
        pluginInventories.forEach((inv, data) -> {
            Player player = (Player) inv.getHolder();
            data.provider.update(player, data);
        });
    }

    public static void openInventory(Player player, InventoryProvider provider) {
        if (debug) {
            Main.logger().info("[GUI] Opening GUI " + provider + " for " + player.getName());
        }

        Component title = provider.getTitle(player);
        int size = 9 * provider.getRows();
        Inventory inv = Bukkit.createInventory(player, size, title);
        InventoryData data = new InventoryData(inv, provider);
        Inventory oldInv = playerInventories.put(player, inv);
		boolean canOpen = true;
        if (oldInv != null) {
            InventoryData old = pluginInventories.remove(oldInv);
            // clean up old inventory
            if (debug) {
                Main.logger().info("[GUI] Cleaning up GUI " + old.provider + " (inventory " + oldInv + ") for " + player.getName());
            }
            if (old != null) {
                try {
                    canOpen = old.provider.close(player, InventoryCloseEvent.Reason.OPEN_NEW);
					if (!canOpen) {
						pluginInventories.put(oldInv, old);
						playerInventories.put(player, oldInv);
						data.provider.init(player, data);
						Bukkit.getScheduler().runTask(Main.getPlugin(), () -> player.openInventory(oldInv));
					}
                } catch (Exception ex) {
					logger.log(Level.WARNING, "Closing GUI " + old.provider + " for " + player.getName(), ex);
                }
            }
        }
		if (canOpen) {
			pluginInventories.put(inv, data);
			provider.init(player, data);
			// just to be safe
			Bukkit.getScheduler().runTask(Main.getPlugin(), () -> player.openInventory(inv));
		}
    }

    public static void closeInventory(Player player) {
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> player.closeInventory(InventoryCloseEvent.Reason.PLUGIN));
    }

    public static void closeInventory(Player player, Class<? extends InventoryProvider> clazz) {
        Inventory inv = playerInventories.get(player);
        if (inv != null) {
            InventoryData data = pluginInventories.get(inv);
            if (clazz.isInstance(data.provider)) {
                closeInventory(player);
            }
        }
    }

	public static CompletableFuture<String> openSign(Player player, Component message, String defaultValue) {
		if (!player.isValid()) { // sanity check
			return CompletableFuture.completedFuture("");
		}

		var future = new CompletableFuture<String>();
		World world = player.getWorld();
		Location signLocation = player.getLocation().toBlockLocation();
		signLocation.setY(world.getMinHeight());

		BlockState originalState = world.getBlockState(signLocation);

		// send a fake sign and make the player edit it
		var fakeData = Material.OAK_SIGN.createBlockData();
		// send the associated data
		var blockPos = new BlockPos(signLocation.getBlockX(), signLocation.getBlockY(), signLocation.getBlockZ());
		var fakeSign = new SignBlockEntity(blockPos, ((CraftBlockData) fakeData).getState());

		List<Component> wrapped = TextUtils.wrapString(defaultValue, Style.empty(), 80);
		net.minecraft.network.chat.Component[] lines = new net.minecraft.network.chat.Component[4];
		for (int i = 0; i < Math.min(wrapped.size(), 2); i++) {
			//fakeSign.setMessage(i, PaperAdventure.asVanilla(wrapped.get(i)));
			lines[i] = PaperAdventure.asVanilla(wrapped.get(i));
		}

		lines[2] = PaperAdventure.asVanilla(Component.text("^^^^^^^^^^^^^^^"));
		lines[3] = PaperAdventure.asVanilla(message);

		fakeSign.setText(new SignText(lines, lines, DyeColor.BLACK, false), true);

		Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
			player.closeInventory();
			player.sendBlockChange(signLocation, fakeData);
			managedSigns.put(player, new ManagedSign(new BlockCoords(signLocation), originalState, future));
			// we will just have to pray that the player isn't currently editing a sign
			PlayerUtils.sendPacket(player, fakeSign.getUpdatePacket(), new ClientboundOpenSignEditorPacket(blockPos, true));
		}, 1);
		return future;
	}

    @EventHandler
    public void onCleanUp(PluginDisableEvent e) {
        if (e.getPlugin() instanceof Main) {
            playerInventories.keySet().forEach(Player::closeInventory);
            playerInventories.clear();
            pluginInventories.clear();

			managedSigns.values().forEach(sign -> sign.future.complete(""));
			managedSigns.clear();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Inventory inv = playerInventories.get(e.getPlayer());
        if (inv != null) {
            inv.close();
        }
		ManagedSign managedSign = managedSigns.remove(e.getPlayer());
		if (managedSign != null) {
			logger.warning("Player " + e.getPlayer().getName() + " quit before sending sign edit packet");
			managedSign.future.complete("");
		}
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (debug)
            Main.logger().info("[GUI] Player closing GUI, reason: " + e.getReason());
        Player player = (Player) e.getPlayer();
        Inventory inv = playerInventories.get(player);
        if (e.getInventory() == inv) {
            playerInventories.remove(player);
        }
        InventoryData data = pluginInventories.remove(e.getInventory());
        if (data != null) {
            if (debug)
                Main.logger().info("[GUI] Closed GUI has provider " + data.provider);
            try {
				var reason = e.getReason();
                if (!data.provider.close(player, reason)) {
					if (reason == InventoryCloseEvent.Reason.TELEPORT || reason == InventoryCloseEvent.Reason.PLAYER) {
						// safe to reopen
						playerInventories.put(player, e.getInventory());
						pluginInventories.put(e.getInventory(), data);
						data.provider.init(player, data);
						Bukkit.getScheduler().runTask(Main.getPlugin(), () -> player.openInventory(e.getInventory()));
					}
				}
            } catch (Exception ex) {
				logger.log(Level.WARNING, "Closing GUI " + data + " for player " + player.getName(), ex);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        InventoryData data = pluginInventories.get(inv);
        if (debug) {
            Main.logger().info("[GUI] Player " + e.getWhoClicked().getName() +
                    String.format(" clicked (click: %s, slot %s: %d, action: %s) in ",
                            e.getClick(), e.getSlotType(), e.getSlot(), e.getAction()) +
                    (data != null ? data.provider : "[unmanaged inventory]"));
        }
        if (data == null) // not our inventory
            return;
        if (inv != e.getClickedInventory()) {
            InventoryAction action = e.getAction();
            // actions that might influence our inventory
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.COLLECT_TO_CURSOR ||
                    action == InventoryAction.NOTHING || action == InventoryAction.UNKNOWN)
                e.setResult(Event.Result.DENY);
            return;
        }
        e.setResult(Event.Result.DENY);

        Consumer<InventoryClickEvent> eventHandler = data.eventHandlers.get(e.getSlot());
        if (eventHandler != null) {
            try {
                eventHandler.accept(e);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Handling slot %d %s click (%s) for %s in %s".formatted(
					e.getSlot(), e.getClick(), e.getAction(), e.getWhoClicked().getName(), data.provider
				), ex);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryDrag(InventoryDragEvent e) {
        Inventory inventory = e.getInventory();
        if (!pluginInventories.containsKey(e.getInventory()))
            return;
        int size = inventory.getSize();
        // check if any of the slots involved is in the top inventory
        for (int slotID : e.getRawSlots()) {
            if (slotID < size) {
                e.setResult(Event.Result.DENY);
                return;
            }
        }
    }

    private static class InventoryData implements InventoryProvider.InventoryAccessor {
        private InventoryData(Inventory inv, InventoryProvider provider) {
            this.inv = inv;
            this.provider = provider;
            this.eventHandlers = new ArrayList<>(Collections.nCopies(inv.getSize(), null));
        }

        private final Inventory inv;
        private final InventoryProvider provider;
        private final ArrayList<Consumer<InventoryClickEvent>> eventHandlers;

        @Override
        public void set(int slot, @Nullable ItemStack stack, @Nullable Consumer<InventoryClickEvent> eventHandler) {
            inv.setItem(slot, stack);
            eventHandlers.set(slot, eventHandler);
        }

		@Override
		public void fill(@Nullable ClickableItem item) {
			if (item == null) {
				inv.clear();
				Collections.fill(eventHandlers, null);
			} else {
				for (int i = 0; i < inv.getSize(); i++) {
					inv.setItem(i, item.stack());
				}
				Collections.fill(eventHandlers, item.eventHandler());
			}
		}

		@Override
        public ClickableItem get(int slot) {
            return new ClickableItem(inv.getItem(slot), eventHandlers.get(slot));
        }

        @Override
        public void invalidate() {
            Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                inv.clear();
				Collections.fill(eventHandlers, null);
                provider.init((Player) inv.getHolder(), this);
            });
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inv;
        }
    }
}
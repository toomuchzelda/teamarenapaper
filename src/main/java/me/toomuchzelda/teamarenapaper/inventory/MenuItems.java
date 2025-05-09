package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class MenuItems {

	public static final ItemStack BORDER = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
		.displayName(Component.empty())
		.build();

	public static final ClickableItem CLOSE = ItemBuilder.of(Material.BARRIER)
		.displayName(Component.text("Close", NamedTextColor.RED))
		.toClickableItem(e -> {
			Player clicked = (Player) e.getWhoClicked();
			Bukkit.getScheduler().runTask(Main.getPlugin(), () -> clicked.closeInventory(InventoryCloseEvent.Reason.PLAYER));
		});

}

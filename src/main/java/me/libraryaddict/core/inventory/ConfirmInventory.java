package me.libraryaddict.core.inventory;

import me.libraryaddict.core.inventory.utils.IButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ConfirmInventory extends BasicInventory {

    private static final ItemStack CONFIRM;
    private static final ItemStack CANCEL;

    static {
        CONFIRM = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = CONFIRM.getItemMeta();
        confirmMeta.displayName(Component.text("CONFIRM").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        CONFIRM.setItemMeta(confirmMeta);

        CANCEL = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = CANCEL.getItemMeta();
        cancelMeta.displayName(Component.text("CANCEL").color(TextColor.color(255, 0, 0)).decorate(TextDecoration.BOLD));
        CANCEL.setItemMeta(cancelMeta);
    }

    public ConfirmInventory(Player player, ItemStack displayItem, Runnable onPurchase, Runnable onCancel) {
        super(player, "Confirm Purchase");

        addItem(4, displayItem);

        //new ItemBuilder(Material.GREEN_WOOL, 1).setTitle(C.Green + C.Bold + "CONFIRM").build()
        addButton(20, CONFIRM, new IButton() {
            @Override
            public boolean onClick(ClickType clickType) {
                onPurchase.run();
                return true;
            }
        });

        //new ItemBuilder(Material.RED_WOOL, 1).setTitle(C.DRed + C.Bold + "CANCEL").build()
        addButton(24, CANCEL, new IButton() {
            @Override
            public boolean onClick(ClickType clickType) {
                onCancel.run();
                return true;
            }
        });
    }

}

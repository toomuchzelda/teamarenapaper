package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class KitInventory extends PagedInventory {

    private ArrayList<Kit> kits;
    public KitInventory(Collection<? extends Kit> kits) {
        this.kits = new ArrayList<>(kits);
        this.kits.sort(Kit.COMPARATOR);
    }

    public KitInventory() {
        this(Arrays.asList(Main.getGame().getKits()));
    }

    @Override
    public Component getTitle(Player player) {
        return Component.text("Select kit").color(NamedTextColor.BLUE);
    }

    @Override
    public int getRows() {
        return Math.min(6, kits.size() / 9 + 1);
    }

    private static Style LORE_STYLE = Style.style(NamedTextColor.YELLOW, TextDecoration.ITALIC);
    public static ClickableItem getKitItem(Kit kit, boolean glow) {
        List<? extends Component> loreLines = Arrays.stream(kit.getDescription().split("\\n"))
                .map(str -> Component.text(str).style(LORE_STYLE))
                .toList();

        return ClickableItem.of(
                ItemBuilder.of(kit.getIcon())
                        .displayName(Component.text(kit.getName())
                                .style(Style.style(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)))
                        .lore(loreLines)
                        .meta(meta -> {
                            if (glow) {
                                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            }
                        })
                        .build(),
                e -> {
                    Player player = (Player) e.getWhoClicked();
                    Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                        Main.getGame().selectKit(player, kit.getName());
                        player.closeInventory();
                    });
                }
        );
    }

    @Override
    public void init(Player player, InventoryAccessor inventory) {
        Kit selected = Main.getPlayerInfo(player).kit;
        if (kits.size() > 45) { // 6 rows
            // set page items
            inventory.set(45, getPreviousPageItem(inventory));
            inventory.set(53, getNextPageItem(inventory));
        }
        List<ClickableItem> kitItems = kits.stream()
                .map(kit -> getKitItem(kit, kit == selected))
                .toList();
        setPageItems(kitItems, inventory, 0, 45);
    }
}

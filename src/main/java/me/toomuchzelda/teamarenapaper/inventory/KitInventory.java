package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class KitInventory extends PagedInventory {
    @Override
    public Component getTitle(Player player) {
        return Component.text("Select kit").color(NamedTextColor.BLUE);
    }

    @Override
    public int getRows() {
        return Math.min(6, Main.getGame().getKits().length / 9 + 1);
    }

    @Override
    public void init(Player player, InventoryAccessor inventory) {
        Kit[] kits = Main.getGame().getKits();
        Kit selected = Main.getPlayerInfo(player).kit;
        if (kits.length > 45) {
            // set last row first
            ItemStack filler = ItemBuilder.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build();
            for (int i = 0; i < 9; i++) {
                int slotID = 45 + i;
                if (i == 0) {
                    inventory.set(slotID, getPreviousPageItem(inventory));
                } else if (i == 8) {
                    inventory.set(slotID, getNextPageItem(inventory));
                } else {
                    inventory.set(slotID, filler);
                }
            }
        }
        List<ClickableItem> kitItems = Arrays.stream(kits)
                .sorted(Comparator.comparing(Kit::getName))
                .map(kit -> {
                    List<? extends Component> loreLines = Arrays.stream(kit.getDescription().split("\\n"))
                            .map(str -> Component.text(str).style(Style.style(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)))
                            .toList();

                    return ClickableItem.of(
                            ItemBuilder.of(kit.getIcon())
                                    .displayName(Component.text(kit.getName())
                                            .style(Style.style(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)))
                                    .lore(loreLines)
                                    .meta(meta -> {
                                        // glow if selected
                                        if (kit == selected) {
                                            meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                                        }
                                    })
                                    .build(),
                            e -> Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                                Main.getGame().selectKit(player, kit.getName());
                                player.closeInventory();
                            })
                    );
                })
                .toList();
        setPageItems(kitItems, inventory);
    }
}

package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.map.MinecraftFont;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class KitInventory extends PagedInventory {

    private final ArrayList<Kit> kits;
    public KitInventory(Collection<? extends Kit> kits) {
        this.kits = new ArrayList<>(kits);
        this.kits.sort(Kit.COMPARATOR);
    }

    public KitInventory() {
        this(Main.getGame().getKits());
    }

    @Override
    public Component getTitle(Player player) {
        return Component.text("Select kit").color(NamedTextColor.BLUE);
    }

    @Override
    public int getRows() {
        return Math.min(6, kits.size() / 9 + 1);
    }

    private static final Style NAME_STYLE = Style.style(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false);
    private static final Style LORE_STYLE = Style.style(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false);
    private static final TextComponent SELECTED_COMPONENT = Component.text("Currently selected!", NamedTextColor.GREEN, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false);
    public static ClickableItem getKitItem(Kit kit, boolean selected) {
        String desc = kit.getDescription();
        // word wrapping because some command-loving idiot didn't add line breaks in kit descriptions
        List<String> lines = new ArrayList<>();
        StringJoiner line = new StringJoiner(" ");
        for (String word : desc.split("\\s|\\n")) {
            // arbitrary width
            if (MinecraftFont.Font.getWidth(line.toString()) < 200) {
                line.add(word);
            } else {
                lines.add(line.toString());
                line = new StringJoiner(" ");
                line.add(word);
            }
        }
        // final line
        lines.add(line.toString());

        ArrayList<TextComponent> loreLines = lines.stream()
                .map(str -> Component.text(str).style(LORE_STYLE))
                .collect(Collectors.toCollection(ArrayList::new));

        if (selected) {
            loreLines.add(Component.empty());
            loreLines.add(SELECTED_COMPONENT);
        }

        return ClickableItem.of(
                ItemBuilder.from(kit.getIcon())
                        .displayName(Component.text(kit.getName(), NAME_STYLE))
                        .lore(loreLines)
                        .hide(ItemFlag.values())
                        .meta(meta -> {
                            if (selected) {
                                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            }
                        })
                        .build(),
                e -> {
                    Player player = (Player) e.getWhoClicked();
                    Main.getGame().selectKit(player, kit);
                    Inventories.closeInventory(player);
                }
        );
    }

    @Override
    public void init(Player player, InventoryAccessor inventory) {
        Main.getGame().interruptRespawn(player);
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

    @Override
    public void close(Player player) {
        // what a shitty method name
        Main.getGame().setToRespawn(player);
    }
}

package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.kits.*;
import me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions.KitDemolitions;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KitInventory implements InventoryProvider {

	private final List<Kit> kits;
	private final TabBar<KitCategory> categoryTab = new TabBar<>(null);
	private final Pagination pagination = new Pagination();

	public KitInventory(Collection<? extends Kit> kits) {
		categoryTab.setClickSound(Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.BLOCKS, 0.5f, 1);

		var temp = new ArrayList<>(kits);
		temp.sort(Kit.COMPARATOR);
		this.kits = List.copyOf(temp);
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
		return 6;
	}

	private static final Style NAME_STYLE = Style.style(NamedTextColor.BLUE);
	private static final Style LORE_STYLE = Style.style(NamedTextColor.YELLOW);
	private static final TextComponent SELECTED_COMPONENT = Component.text("Currently selected!", NamedTextColor.GREEN, TextDecoration.BOLD);

	public static ClickableItem getKitItem(Kit kit, boolean selected) {
		boolean disabled = !CommandDebug.kitPredicate.test(kit);

		String desc = kit.getDescription();
		// word wrapping because some command-loving idiot didn't add line breaks in kit descriptions
		List<Component> loreLines = new ArrayList<>(TextUtils.wrapString(desc, LORE_STYLE, 200));

		if (selected) {
			loreLines.add(Component.empty());
			loreLines.add(SELECTED_COMPONENT);
		}
		if (disabled) {
			loreLines.add(Component.empty());
			loreLines.add(Component.text("This kit has been disabled by an admin.", NamedTextColor.DARK_RED, TextDecoration.BOLD));
		}

		return ClickableItem.of(
				ItemBuilder.from(disabled ? new ItemStack(Material.BARRIER) : kit.getIcon())
						.displayName(Component.text(kit.getName(),
								disabled ? NAME_STYLE.decorate(TextDecoration.STRIKETHROUGH) : NAME_STYLE))
						.lore(loreLines)
						.hide(ItemFlag.values())
						.meta(meta -> {
							if (selected) {
								meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
							}
						})
						.build(),
				e -> {
					if (!CommandDebug.kitPredicate.test(kit))
						return;
					Player player = (Player) e.getWhoClicked();
					Main.getGame().selectKit(player, kit);
					Inventories.closeInventory(player, KitInventory.class);
				}
		);
	}

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		Main.getGame().interruptRespawn(player);

		// include null tab to specify no filter
		categoryTab.showTabs(inventory,
				Arrays.asList(null, KitCategory.FIGHTER, KitCategory.RANGED,
						KitCategory.SUPPORT, KitCategory.STEALTH, KitCategory.UTILITY),
				(category, selected) -> category == null ?
						TabBar.highlightIfSelected(ALL_TAB_ITEM, selected) :
						category.display(selected),
				0, 9, true);


		// 6th row
		ItemStack borderItem = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();
		// max 4 rows
		boolean showPageItems = kits.size() > 9 * 4;
		for (int i = 45; i < 54; i++) {
			if (i == 45 && showPageItems)
				inventory.set(i, pagination.getPreviousPageItem(inventory));
			else if (i == 46 && showPageItems)
				inventory.set(i, pagination.getNextPageItem(inventory));
			else if (i == 53)
				inventory.set(i, ItemBuilder.of(Material.ENDER_CHEST)
						.displayName(Component.text("Save as default kit", NamedTextColor.YELLOW))
						.lore(TextUtils.toLoreList("""
								Warning: Unfortunately, the default kit is not
								actually saved. To make your changes persist
								across sessions, nag toomuchzelda.""", NamedTextColor.GRAY))
						.toClickableItem(e -> {
							Player clicker = (Player) e.getWhoClicked();
							PlayerInfo playerInfo = Main.getPlayerInfo(clicker);
							playerInfo.defaultKit = playerInfo.kit.getName();
							clicker.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
							clicker.sendMessage(Component.textOfChildren(
									Component.text("Saved ", NamedTextColor.GREEN),
									Component.text(playerInfo.defaultKit, NamedTextColor.YELLOW),
									Component.text(" as your default kit.", NamedTextColor.GREEN)
							));
							Inventories.closeInventory(clicker, KitInventory.class);
						})
				);
			else
				inventory.set(i, borderItem);
		}

		Kit selected = Main.getPlayerInfo(player).kit;
		KitCategory filter = categoryTab.getCurrentTab();
		List<ClickableItem> kitItems = kits.stream()
				.filter(kit -> filter == null || kit.getCategory() == filter)
				.map(kit -> getKitItem(kit, kit == selected))
				.toList();
		pagination.showPageItems(inventory, kitItems, 9, 45);
	}

	@Override
	public void close(Player player) {
		Main.getGame().setToRespawn(player);
	}

	private static final ItemStack ALL_TAB_ITEM = ItemBuilder.of(Material.BOOK)
			.displayName(Component.text("All kits", NamedTextColor.WHITE))
			.lore(Component.text("Show all kits in Team Arena", NamedTextColor.GRAY))
			.build();

	static final Map<KitCategory, Set<Class<? extends Kit>>> KIT_CATEGORIES = Map.of(
			KitCategory.FIGHTER, Set.of(
					KitTrooper.class, KitValkyrie.class, KitJuggernaut.class, KitRewind.class
			),
			KitCategory.RANGED, Set.of(
					KitArcher.class, KitBurst.class, KitPyro.class, KitSniper.class
			),
			KitCategory.SUPPORT, Set.of(
					KitDemolitions.class
			),
			KitCategory.STEALTH, Set.of(
					KitGhost.class, KitSpy.class, KitNinja.class
			),
			KitCategory.UTILITY, Set.of(
					KitVenom.class
			)
	);
}

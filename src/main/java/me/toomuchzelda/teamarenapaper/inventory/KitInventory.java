package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.kits.*;
import me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions.KitDemolitions;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.*;

public class KitInventory extends TabInventory<KitInventory.KitCategory> {

	private final ArrayList<Kit> kits;

	public KitInventory(Collection<? extends Kit> kits) {
		super(null);
		setClickSound(Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.BLOCKS, 0.5f, 1);

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
		return 6;
	}

	private static final Style NAME_STYLE = Style.style(NamedTextColor.BLUE);
	private static final Style LORE_STYLE = Style.style(NamedTextColor.YELLOW);
	private static final TextComponent SELECTED_COMPONENT = Component.text("Currently selected!", NamedTextColor.GREEN, TextDecoration.BOLD);

	public static ClickableItem getKitItem(Kit kit, boolean selected) {
		String desc = kit.getDescription();
		// word wrapping because some command-loving idiot didn't add line breaks in kit descriptions

		List<Component> loreLines = new ArrayList<>(TextUtils.wrapString(desc, LORE_STYLE, 200));

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
							}
						})
						.build(),
				e -> {
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
		showTabs(inventory,
				Arrays.asList(null, KitCategory.FIGHTER, KitCategory.RANGED,
						KitCategory.SUPPORT, KitCategory.STEALTH, KitCategory.UTILITY),
				(category, selected) -> category == null ?
						(selected ? highlight(ALL_TAB_ITEM) : ALL_TAB_ITEM) :
						category.display(selected),
				0, 9, true);

		Kit selected = Main.getPlayerInfo(player).kit;
		if (kits.size() > 9 * 4) { // max 4 rows
			// set page items
			inventory.set(45, getPreviousPageItem(inventory));
			inventory.set(53, getNextPageItem(inventory));
		}
		KitCategory filter = getCurrentTab();
		List<ClickableItem> kitItems = kits.stream()
				.filter(kit -> filter == null || KIT_CATEGORIES.get(filter).contains(kit.getClass()))
				.map(kit -> getKitItem(kit, kit == selected))
				.toList();
		setPageItems(kitItems, inventory, 9, 45);
	}

	@Override
	public void close(Player player) {
		Main.getGame().setToRespawn(player);
	}

	private static final ItemStack ALL_TAB_ITEM = ItemBuilder.of(Material.BOOK)
			.displayName(Component.text("All kits", NamedTextColor.WHITE))
			.lore(Component.text("Show all kits in Team Arena", NamedTextColor.GRAY))
			.build();

	private static final ItemStack HEALER_BANNER = ItemBuilder.of(Material.WHITE_BANNER)
			// blatant violation of the Geneva Conventions
			.meta(BannerMeta.class, bannerMeta -> bannerMeta.setPatterns(Arrays.asList(
					new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_MIRROR),
					new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL),
					new Pattern(DyeColor.RED, PatternType.STRAIGHT_CROSS),
					new Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP),
					new Pattern(DyeColor.WHITE, PatternType.STRIPE_BOTTOM),
					new Pattern(DyeColor.WHITE, PatternType.BORDER)
			)))
			.hide(ItemFlag.values())
			.build();

	private static final ItemStack HEALER_BANNER_INVERTED = ItemBuilder.of(Material.RED_BANNER)
			.meta(BannerMeta.class, bannerMeta -> bannerMeta.setPatterns(Arrays.asList(
					new Pattern(DyeColor.RED, PatternType.HALF_HORIZONTAL_MIRROR),
					new Pattern(DyeColor.RED, PatternType.HALF_HORIZONTAL),
					new Pattern(DyeColor.WHITE, PatternType.STRAIGHT_CROSS),
					new Pattern(DyeColor.RED, PatternType.STRIPE_TOP),
					new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM),
					new Pattern(DyeColor.RED, PatternType.BORDER)
			)))
			.hide(ItemFlag.values())
			.build();
	enum KitCategory {
		FIGHTER(ItemBuilder.of(Material.IRON_SWORD)
				.displayName(Component.text("Fighter", NamedTextColor.RED))
				.lore(TextUtils.toLoreList("""
						Kits that mainly engage in
						head-on melee combat.
						""", NamedTextColor.GRAY))
				.hide(ItemFlag.HIDE_ATTRIBUTES)
				.build()
		),
		RANGED(ItemBuilder.of(Material.BOW)
				.displayName(Component.text("Ranged", NamedTextColor.BLUE))
				.lore(TextUtils.toLoreList("""
						Kits that mainly deal damage
						from a range.
						""", NamedTextColor.GRAY))
				.build()),
		SUPPORT(
				ItemBuilder.from(HEALER_BANNER.clone())
						.displayName(Component.text("Support", NamedTextColor.YELLOW))
						.lore(TextUtils.toLoreList("""
								Kits that provide buffs for
								teammates / defensive potential
								""", NamedTextColor.GRAY))
						.build(),
				ItemBuilder.from(HEALER_BANNER_INVERTED)
						.displayName(Component.text("Support", NamedTextColor.YELLOW))
						.lore(TextUtils.toLoreList("""
								Kits that provide buffs for
								teammates / defensive potential
								""", NamedTextColor.GRAY))
						.build()
		),
		STEALTH(ItemBuilder.of(Material.SPYGLASS)
				.displayName(Component.text("Stealth", NamedTextColor.GRAY))
				.lore(TextUtils.toLoreList("""
						Kits that use stealth to
						gain information or to
						pick off enemies.
						""", NamedTextColor.GRAY))
				.build()),
		UTILITY(ItemBuilder.of(Material.POTION)
				.displayName(Component.text("Utility", NamedTextColor.GOLD))
				.lore(TextUtils.toLoreList("""
						Kits that debuff enemies/
						disorients enemy positioning""", NamedTextColor.GRAY))
				.hide(ItemFlag.HIDE_POTION_EFFECTS)
				.build());

		final ItemStack display, displaySelected;

		KitCategory(ItemStack display) {
			this(display, highlight(display));
		}

		KitCategory(ItemStack display, ItemStack displaySelected) {
			this.display = display;
			this.displaySelected = displaySelected;
		}

		ItemStack display(boolean selected) {
			return selected ? this.displaySelected : this.display;
		}
	}

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

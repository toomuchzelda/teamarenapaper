package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.Arrays;
import java.util.Locale;

public enum KitCategory {
	FIGHTER(NamedTextColor.RED, new ItemStack(Material.IRON_SWORD), """
			Kits that mainly engage in
			head-on melee combat.
			"""),
	RANGED(NamedTextColor.BLUE, new ItemStack(Material.BOW), """
			Kits that mainly deal damage
			from a range.
			"""),
	SUPPORT(NamedTextColor.YELLOW,
			ItemBuilder.from(Banners.HEALER_BANNER)
					.displayName(Component.text("Support", NamedTextColor.YELLOW))
					.lore(TextUtils.toLoreList("""
							Kits that provide buffs for
							teammates / defensive potential
							""", NamedTextColor.GRAY))
					.build(),
			ItemBuilder.from(Banners.HEALER_BANNER_INVERTED)
					.displayName(Component.text("Support", NamedTextColor.YELLOW))
					.lore(TextUtils.toLoreList("""
							Kits that provide buffs for
							teammates / defensive potential
							""", NamedTextColor.GRAY))
					.build()
	),
	STEALTH(NamedTextColor.GRAY, new ItemStack(Material.SPYGLASS), """
			Kits that use stealth to
			gain information or to
			pick off enemies.
			"""),
	UTILITY(NamedTextColor.GOLD, new ItemStack(Material.POTION), """
			Kits that debuff enemies/
			disorients enemy positioning""");

	private final TextColor color;
	private final Component displayName;
	private final ItemStack display;
	private final ItemStack displaySelected;

	KitCategory(TextColor color, ItemStack display, String description) {
		var stylizedName = capitalize(name().toLowerCase(Locale.ENGLISH).replace('_', ' '));
		this.color = color;
		this.displayName = Component.text(stylizedName, color);
		this.display = ItemBuilder.from(display)
				.displayName(displayName)
				.lore(TextUtils.toLoreList(description, NamedTextColor.GRAY))
				.hide(ItemFlag.values())
				.build();
		this.displaySelected = ItemUtils.highlight(this.display);
	}

	KitCategory(TextColor color, ItemStack display, ItemStack displaySelected) {
		this.color = color;
		this.displayName = display.getItemMeta().displayName();
		this.display = display;
		this.displaySelected = displaySelected;
	}

	public Component displayName() {
		return displayName;
	}

	public ItemStack display(boolean selected) {
		return (selected ? this.displaySelected : this.display).clone();
	}

	public TextColor textColor() {
		return color;
	}

	private static String capitalize(String string) {
		return Character.toTitleCase(string.charAt(0)) + string.substring(1);
	}

	private static final class Banners {
		public static final ItemStack HEALER_BANNER = ItemBuilder.of(Material.WHITE_BANNER)
				// blatant violation of the Geneva Conventions
				.meta(BannerMeta.class, bannerMeta -> bannerMeta.setPatterns(Arrays.asList(
						new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_BOTTOM),
						new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL),
						new Pattern(DyeColor.RED, PatternType.STRAIGHT_CROSS),
						new Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP),
						new Pattern(DyeColor.WHITE, PatternType.STRIPE_BOTTOM),
						new Pattern(DyeColor.WHITE, PatternType.BORDER)
				)))
				.hide(ItemFlag.values())
				.build();
		public static final ItemStack HEALER_BANNER_INVERTED = ItemBuilder.of(Material.RED_BANNER)
				.meta(BannerMeta.class, bannerMeta -> bannerMeta.setPatterns(Arrays.asList(
						new Pattern(DyeColor.RED, PatternType.HALF_HORIZONTAL_BOTTOM),
						new Pattern(DyeColor.RED, PatternType.HALF_HORIZONTAL),
						new Pattern(DyeColor.WHITE, PatternType.STRAIGHT_CROSS),
						new Pattern(DyeColor.RED, PatternType.STRIPE_TOP),
						new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM),
						new Pattern(DyeColor.RED, PatternType.BORDER)
				)))
				.hide(ItemFlag.values())
				.build();
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class CosmeticItem {

	@NotNull
	public final String name;
	@Nullable
	public final String author;
	@Nullable
	public final String desc;

	private final ItemStack display;
	protected CosmeticItem(@NotNull File file, @NotNull YamlConfiguration info) {
		var name = info.getString("name");
		if (name == null) {
			name = file.getName().substring(0, file.getName().indexOf('.'));
		}
		this.name = name;
		this.author = info.getString("author");
		this.desc = info.getString("desc");
		var displayString = info.getString("display");
		if (displayString == null) {
			display = new ItemStack(Material.ARMOR_STAND);
		} else {
			display = Bukkit.getItemFactory().createItemStack(displayString);
		}
	}

	public abstract CosmeticType getCosmeticType();

	public void unload() {

	}

	@NotNull
	public final Component getExtras() {
		return Component.join(JoinConfiguration.newlines(), getExtraInfo());
	}

	protected List<Component> getExtraInfo() {
		var author = Component.textOfChildren(Component.text("Author: ", NamedTextColor.GOLD),
			this.author != null ?
				Component.text(this.author, NamedTextColor.AQUA) :
				Component.text("Unknown", NamedTextColor.DARK_GRAY)
		);
		if (this.desc != null) {
			var wrappedDesc = TextUtils.wrapString(this.desc, Style.style(NamedTextColor.GRAY), TextUtils.DEFAULT_WIDTH, true);
			var list = new ArrayList<Component>(wrappedDesc.size() + 2);
			list.add(author);
			list.add(Component.text("Description:", NamedTextColor.GOLD));
			list.addAll(wrappedDesc);

			return List.copyOf(list);
		} else {
			return List.of(author);
		}
	}

	@NotNull
	public Component getInfo() {
		return Component.textOfChildren(
			Component.text("Name: ", NamedTextColor.GOLD), Component.text(this.name, NamedTextColor.YELLOW),
			Component.newline(),
			getExtras()
		);
	}

	@NotNull
	public ItemStack getDisplay(boolean complex) {
		return ItemBuilder.from(display.clone())
			.displayName(Component.text(this.name, NamedTextColor.YELLOW))
			.lore(getExtraInfo())
			.hideAll()
			.build();
	}
}

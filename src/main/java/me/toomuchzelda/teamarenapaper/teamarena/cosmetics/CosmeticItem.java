package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public abstract class CosmeticItem {
	@NotNull
	public final NamespacedKey key;
	@NotNull
	public final Component name;
	@Nullable
	public final Component author;
	@Nullable
	public final List<Component> desc;

	private final ItemStack display;
	protected CosmeticItem(@NotNull NamespacedKey key, @NotNull File file, @NotNull YamlConfiguration info) {
		this.key = key;
		String name = info.getString("name");
		if (name == null) {
			name = file.getName().substring(0, file.getName().indexOf('.'));
		}
		MiniMessage miniMessage = MiniMessage.miniMessage();
		this.name = miniMessage.deserialize(name).colorIfAbsent(NamedTextColor.YELLOW);
		String author = info.getString("author");
		this.author = author != null ? miniMessage.deserialize(author).colorIfAbsent(NamedTextColor.AQUA) : null;
		String description = info.getString("description");
		if (description != null) {
			String wrapped = String.join("\n", TextUtils.wrapStringRaw(description, TextUtils.DEFAULT_WIDTH, true));
			this.desc = TextUtils.splitLines(miniMessage.deserialize(wrapped).colorIfAbsent(NamedTextColor.GRAY));
		} else {
			this.desc = null;
		}
		var displayString = info.getString("display");
		if (displayString == null) {
			display = new ItemStack(Material.ARMOR_STAND);
		} else {
			ItemStack display;
			try {
				byte[] decoded = Base64.getDecoder().decode(displayString);
				display = ItemStack.deserializeBytes(decoded);
			} catch (Exception ex) {
				try {
					display = Bukkit.getItemFactory().createItemStack(displayString);
				} catch (IllegalArgumentException ex2) {
					var iae = new IllegalArgumentException("Neither Base64 stack nor item command format: " + displayString, ex2);
					iae.addSuppressed(ex);
					throw iae;
				}
				// one-time migration
				Main.componentLogger().warn("Migrating {} cosmetic {} display item to bytes: {}", getCosmeticType(), key, displayString);
				byte[] stack = display.serializeAsBytes();
				String encoded = Base64.getEncoder().encodeToString(stack);
				info.set("display", encoded);
				try {
					info.save(file);
				} catch (IOException ex2) {
					Main.componentLogger().error("Failed to save migrated file {}", file.getPath(), ex2);
				}
			}
			this.display = display;
		}
	}

	@Contract(pure = true)
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
				this.author :
				Component.text("Unknown", NamedTextColor.DARK_GRAY)
		);
		if (this.desc != null) {
			var list = new ArrayList<Component>(desc.size() + 2);
			list.add(author);
			list.add(Component.text("Description:", NamedTextColor.GOLD));
			list.addAll(desc);

			return List.copyOf(list);
		} else {
			return List.of(author);
		}
	}

	@NotNull
	public Component getInfo() {
		return Component.textOfChildren(
			Component.text("Name: ", NamedTextColor.GOLD), this.name,
			Component.newline(),
			getExtras()
		);
	}

	@NotNull
	public ItemStack getDisplay(boolean complex) {
		return ItemBuilder.from(display.clone())
			.displayName(name)
			.lore(getExtraInfo())
			.hideAll()
			.build();
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.utils.FileUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class CosmeticItem {

	@NotNull
	public final String name;
	@Nullable
	public final String author;
	@Nullable
	public final String desc;
	protected CosmeticItem(File file, File companionFile) {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(companionFile);
		var name = config.getString("name");
		if (name == null) {
			name = FileUtils.getFileExtension(file.getName()).fileName();
		}
		this.name = name;
		this.author = config.getString("author");
		this.desc = config.getString("desc");
	}

	public Component getInfo() {
		var name = Component.text(this.name, NamedTextColor.YELLOW);
		var author = this.author != null ?
			Component.text(this.author, NamedTextColor.AQUA) :
			Component.text("Stolen work", NamedTextColor.RED);
		var description = this.desc != null ?
			Component.text(this.desc, NamedTextColor.GRAY) :
			Component.text("No information given.", NamedTextColor.DARK_GRAY);
		return Component.textOfChildren(
			Component.text("Name: ", NamedTextColor.GOLD), name, Component.newline(),
			Component.text("Author: ", NamedTextColor.BLUE), author, Component.newline(),
			Component.text("Description"), description
		);
	}

	abstract ItemStack getDisplay(boolean complex);
}

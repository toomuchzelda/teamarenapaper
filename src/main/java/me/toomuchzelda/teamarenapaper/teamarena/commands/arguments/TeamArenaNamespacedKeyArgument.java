package me.toomuchzelda.teamarenapaper.teamarena.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Namespaced key argument that defaults to the plugin namespace, instead of {@code minecraft:}
 */
public class TeamArenaNamespacedKeyArgument implements CustomArgumentType.Converted<NamespacedKey, String> {
	public static final TeamArenaNamespacedKeyArgument INSTANCE = new TeamArenaNamespacedKeyArgument();
	@Override
	public @NotNull NamespacedKey convert(@NotNull String nativeType) throws CommandSyntaxException {
		return NamespacedKey.fromString(nativeType, Main.getPlugin());
	}

	@Override
	public @NotNull ArgumentType<String> getNativeType() {
		return StringArgumentType.word();
	}
}

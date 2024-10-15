package me.toomuchzelda.teamarenapaper.teamarena.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static me.toomuchzelda.teamarenapaper.teamarena.commands.brigadier.BrigadierCommand.message;

public class KitNamesArgument implements CustomArgumentType<Set<String>, String> {
	public static KitNamesArgument INSTANCE = new KitNamesArgument();

	@SuppressWarnings("unchecked")
	public static Set<String> getKitNames(CommandContext<CommandSourceStack> context, String name) {
		return context.getArgument(name, Set.class);
	}

	DynamicCommandExceptionType DUPLICATE_ELEMENT = new DynamicCommandExceptionType(elem ->
		message(Component.text("Duplicate element: " + elem, TextColors.ERROR_RED)));
	DynamicCommandExceptionType UNEXPECTED_CHARACTER = new DynamicCommandExceptionType(elem ->
		message(Component.text("Unexpected character: " + elem, TextColors.ERROR_RED)));

	@Override
	public @NotNull Set<String> parse(@NotNull StringReader reader) throws CommandSyntaxException {
		Set<String> set = new HashSet<>();
		int index = reader.getCursor();
		char peek;
		while (true) {
			while (reader.canRead() && (Character.isLetterOrDigit(peek = reader.peek()) || peek == '_')) {
				reader.skip();
			}
			if (!reader.canRead())
				break;
			peek = reader.peek();
			if (peek == ',') {
				String substring = reader.getString().substring(index, reader.getCursor());
				if (!set.add(substring)) {
					reader.setCursor(index);
					throw DUPLICATE_ELEMENT.createWithContext(reader, substring);
				}
				reader.skip();
				index = reader.getCursor();
			} else if (peek == ' ') {
				break;
			} else {
				throw UNEXPECTED_CHARACTER.createWithContext(reader, peek);
			}
		}
		String substring = reader.getString().substring(index, reader.getCursor());
		if (!set.add(substring)) {
			reader.setCursor(index);
			throw DUPLICATE_ELEMENT.createWithContext(reader, substring);
		}
		set.remove("");

		return set;
	}

	@Override
	public @NotNull ArgumentType<String> getNativeType() {
		return StringArgumentType.word();
	}

	@Override
	public @NotNull <S> CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
		String input = builder.getRemaining();
		int idx = input.lastIndexOf(',');
		String existing = input.substring(0, idx + 1);
		Set<String> existingSet = idx != -1 ? new HashSet<>(Arrays.asList(existing.split(","))) : Set.of();
		if (idx != -1)
			builder = builder.createOffset(builder.getStart() + idx + 1);
		String partial = input.substring(idx + 1);
		for (Kit kit : Main.getGame().getKits()) {
			String kitName = kit.getKey();
			if (existingSet.contains(kitName)) continue;
			if (kitName.equals(partial)) { // suggest comma first
				builder = builder.createOffset(builder.getStart() + kitName.length());
				builder.suggest(",");
				return builder.buildFuture();
			} else if (kitName.startsWith(partial)) {
				builder.suggest(kitName, message(kit.getDisplayName()));
			}
		}

		return builder.buildFuture();
	}
}

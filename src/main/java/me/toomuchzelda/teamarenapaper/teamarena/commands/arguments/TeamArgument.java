package me.toomuchzelda.teamarenapaper.teamarena.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class TeamArgument implements CustomArgumentType.Converted<TeamArenaTeam, String> {
	public static final TeamArgument INSTANCE = new TeamArgument();

	@Override
	public @NotNull ArgumentType<String> getNativeType() {
		return StringArgumentType.string();
	}

	@Override
	public @NotNull <S> CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
		String remaining = builder.getRemainingLowerCase();
		for (TeamArenaTeam team : Main.getGame().getTeams()) {
			if (team.getName().toLowerCase(Locale.ENGLISH).contains(remaining)) {
				builder.suggest(team.getName(), MessageComponentSerializer.message().serialize(team.getComponentName()));
			}
		}
		return builder.buildFuture();
	}

	private static final DynamicCommandExceptionType TEAM_NOT_FOUND = new DynamicCommandExceptionType(
		name -> MessageComponentSerializer.message().serialize(Component.translatable("team.notFound", Component.text((String) name)))
	);
	@Override
	public @NotNull TeamArenaTeam convert(@NotNull String nativeType) throws CommandSyntaxException {
		for (TeamArenaTeam team : Main.getGame().getTeams()) {
			if (team.getName().equals(nativeType)) {
				return team;
			}
		}
		throw TEAM_NOT_FOUND.create(nativeType);
	}
}

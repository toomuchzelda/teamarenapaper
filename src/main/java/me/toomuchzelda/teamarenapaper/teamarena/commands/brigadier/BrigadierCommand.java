package me.toomuchzelda.teamarenapaper.teamarena.commands.brigadier;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import net.kyori.adventure.text.Component;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public abstract class BrigadierCommand {


	public record RequiresPermission(PermissionLevel level) implements Predicate<CommandSourceStack> {
		@Override
		public boolean test(CommandSourceStack commandSourceStack) {
			if (commandSourceStack.getSender() instanceof Player player) {
				PlayerInfo playerInfo = Main.getPlayerInfo(player);
				if (playerInfo == null)
					return true;
				return playerInfo.hasPermission(level);
			}
			return commandSourceStack.getSender() instanceof ConsoleCommandSender;
		}
	}
	protected static Predicate<CommandSourceStack> permission(PermissionLevel level) {
		return new RequiresPermission(level);
	}

	private static final MessageComponentSerializer MESSAGE = MessageComponentSerializer.message();
	@NotNull
	public static Message message(@NotNull Component component) {
		return MESSAGE.serialize(component);
	}

	protected static SuggestionProvider<CommandSourceStack> suggestList(Iterable<? extends String> iterable) {
		return (ctx, builder) -> {
			String remaining = builder.getRemaining();
			for (String s : iterable) {
				if (s.startsWith(remaining)) {
					builder.suggest(s);
				}
			}
			return builder.buildFuture();
		};
	}

	protected static SuggestionProvider<CommandSourceStack> suggestDynamicList(Function<CommandContext<CommandSourceStack>, Iterable<? extends String>> iterableFunction) {
		return (ctx, builder) -> {
			String remaining = builder.getRemaining();
			for (String s : iterableFunction.apply(ctx)) {
				if (s.startsWith(remaining)) {
					builder.suggest(s);
				}
			}
			return builder.buildFuture();
		};
	}

	protected static void sendFeedback(CommandContext<CommandSourceStack> ctx, Component message) {
		ctx.getSource().getSender().sendMessage(message);
	}
}

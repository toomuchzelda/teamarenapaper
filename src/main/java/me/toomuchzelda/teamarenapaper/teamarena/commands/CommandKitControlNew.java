package me.toomuchzelda.teamarenapaper.teamarena.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.commands.arguments.KitNamesArgument;
import me.toomuchzelda.teamarenapaper.teamarena.commands.arguments.TeamArenaNamespacedKeyArgument;
import me.toomuchzelda.teamarenapaper.teamarena.commands.arguments.TeamArgument;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterAction;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterRule;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.exceptions.CommandSyntaxException.BUILT_IN_EXCEPTIONS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

@SuppressWarnings("UnstableApiUsage")
public class CommandKitControlNew extends BrigadierCommand {
	private CommandKitControlNew() {}

	public static void register(Commands registrar) {
		LiteralArgumentBuilder<CommandSourceStack> builder = literal("kitcontrol")
			.requires(permission(PermissionLevel.MOD));
		builder
			.then(literal("clear").executes(ctx -> {
				KitFilter.resetFilter();
				ctx.getSource().getSender().sendMessage(Component.text("Allowing all kits", NamedTextColor.YELLOW));
				return SINGLE_SUCCESS;
			}))
			.then(literal("option")
				.then(argument("option", word())
					.suggests(suggestList(KitOptions.getOptions()))
					.executes(ctx -> {
						String option = getString(ctx, "option");
						if (KitOptions.toggleOption(option)) {
							ctx.getSource().getSender().sendMessage(Component.text("Toggled option", NamedTextColor.BLUE));
							return SINGLE_SUCCESS;
						} else {
							throw BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
						}
					})
				)
			)
			.then(literal("preset")
				.then(argument("preset", word())
					.suggests(suggestList(KitFilter.PRESETS.keySet()))
					.executes(ctx -> {
						String presetName = getString(ctx, "preset");
						KitFilter.FilterPreset preset = KitFilter.PRESETS.get(presetName);
						if (preset == null)
							throw BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
						KitFilter.setPreset(Main.getGame(), preset);
						ctx.getSource().getSender().sendMessage(Component.textOfChildren(
							Component.text("Changed the preset to ", NamedTextColor.BLUE),
							Component.text(preset.name(), NamedTextColor.GOLD)
						));
						return SINGLE_SUCCESS;
					})))
			.then(literal("allow").then(argument("kits", KitNamesArgument.INSTANCE).executes(ctx -> doAllowBlock(ctx, false))))
			.then(literal("block").then(argument("kits", KitNamesArgument.INSTANCE).executes(ctx -> doAllowBlock(ctx, true))))
			.then(literal("rules")
				.then(appendRuleActionsFor(literal("global"), ignored -> Target.Global.INSTANCE))
				.then(literal("team")
					.then(appendRuleActionsFor(
						argument("team", StringArgumentType.string())
							.suggests(TeamArgument.INSTANCE::listSuggestions),
						ctx -> new Target.Team(getString(ctx, "team"))
					))
				)
				.then(literal("player")
					.then(appendRuleActionsFor(
						argument("player", word())
							.suggests(ArgumentTypes.player()::listSuggestions),
						ctx -> new Target.Player(getString(ctx, "player"))
					))
				)
			);
		registrar.register(builder.build());
	}

	private static final SimpleCommandExceptionType CANNOT_BLOCK_ALL = new SimpleCommandExceptionType(
		MessageComponentSerializer.message().serialize(Component.text("Cannot block all kits! Resetting to all kits allowed", TextColors.ERROR_RED)));
	private static int doAllowBlock(CommandContext<CommandSourceStack> ctx, boolean block) throws CommandSyntaxException {
		CommandSender sender = ctx.getSource().getSender();
		Set<String> kitNames = KitNamesArgument.getKitNames(ctx, "kits");
		try {
			if (block)
				KitFilter.setAdminBlocked(Main.getGame(), kitNames);
			else
				KitFilter.setAdminAllowed(Main.getGame(), kitNames);
			sender.sendMessage(Component.textOfChildren(
				Component.text("Set kit restrictions to: ", NamedTextColor.YELLOW),
				Component.text((block ? "block " : "allow ") + kitNames, block ? NamedTextColor.GREEN : NamedTextColor.RED)
			));
			return 1;
		} catch (IllegalArgumentException ex) {
			throw CANNOT_BLOCK_ALL.create();
		}
	}

	private static int doAddRule(CommandContext<CommandSourceStack> ctx, Target target, boolean allow) {
		NamespacedKey key = Objects.requireNonNull(ctx.getArgument("key", NamespacedKey.class));
		var kits = KitNamesArgument.getKitNames(ctx, "kits");
		var rule = new FilterRule(key, "kitcontrol - " + kits,
			allow ? FilterAction.allow(kits) : FilterAction.block(kits)
		);
		switch (target) {
			case Target.Global ignored -> KitFilter.addGlobalRule(rule);
			case Target.Team(String teamName) -> KitFilter.addTeamRule(teamName, rule);
			case Target.Player(String playerName) -> KitFilter.addPlayerRule(playerName, rule);
		}
		KitFilter.updateKitsFor(Main.getGame(), Bukkit.getOnlinePlayers());
		ctx.getSource().getSender().sendMessage(Component.text("Added rule " + key, NamedTextColor.YELLOW));
		return SINGLE_SUCCESS;
	}

	private static final DynamicCommandExceptionType RULE_NOT_FOUND = new DynamicCommandExceptionType(
		obj -> MessageComponentSerializer.message().serialize(Component.text("Rule " + obj + " not found", TextColors.ERROR_RED)));
	private static ArgumentBuilder<CommandSourceStack, ?> appendRuleActionsFor(ArgumentBuilder<CommandSourceStack, ?> root, Function<CommandContext<CommandSourceStack>, Target> targetFunction) {
		return root
			.then(literal("add").then(
				argument("key", TeamArenaNamespacedKeyArgument.INSTANCE)
					.then(literal("allow")
						.then(argument("kits", KitNamesArgument.INSTANCE)
							.executes(ctx -> doAddRule(ctx, targetFunction.apply(ctx), true))))
					.then(literal("block")
						.then(argument("kits", KitNamesArgument.INSTANCE)
							.executes(ctx -> doAddRule(ctx, targetFunction.apply(ctx), false))))
			))
			.then(literal("remove").then(
				argument("key", TeamArenaNamespacedKeyArgument.INSTANCE)
					// https://github.com/PaperMC/Paper/issues/11384#issuecomment-2339090369
					.suggests(suggestDynamicList(ctx -> (switch (targetFunction.apply(ctx.getLastChild())) {
						case Target.Global ignored -> KitFilter.getGlobalRules();
						case Target.Team(String teamName) -> KitFilter.getTeamRules(teamName);
						case Target.Player(String playerName) -> KitFilter.getPlayerRules(playerName);
					}).stream()
						.map(NamespacedKey::toString)
						.toList()
					))
					.executes(ctx -> {
						NamespacedKey key = ctx.getArgument("key", NamespacedKey.class);
						boolean success = switch (targetFunction.apply(ctx)) {
							case Target.Global ignored -> KitFilter.removeGlobalRule(key);
							case Target.Team(String teamTeam) -> KitFilter.removeTeamRule(teamTeam, key);
							case Target.Player(String playerTeam) -> KitFilter.removePlayerRule(playerTeam, key);
						};
						if (success) {
							ctx.getSource().getSender().sendMessage(Component.text("Removed rule " + key, NamedTextColor.YELLOW));
							return SINGLE_SUCCESS;
						} else {
							throw RULE_NOT_FOUND.create(key);
						}
					})
			))
			.then(literal("inspect").executes(ctx -> {
				var message = switch (targetFunction.apply(ctx)) {
					case Target.Global ignored -> KitFilter.inspectRules(null, null);
					case Target.Team(String teamName) -> KitFilter.inspectRules(teamName, null);
					case Target.Player(String playerName) -> {
						// if they are online, also get team
						var player = Bukkit.getPlayerExact(playerName);
						String teamName = null;
						if (player != null)
							teamName = Main.getPlayerInfo(player).team.getSimpleName();
						yield KitFilter.inspectRules(teamName, playerName);
					}
				};
				ctx.getSource().getSender().sendMessage(message);
				return SINGLE_SUCCESS;
			}));
	}

	sealed interface Target {
		enum Global implements Target { INSTANCE }
		record Team(String team) implements Target {}
		record Player(String player) implements Target {}

		static Target parse(String input) throws IllegalArgumentException {
			if (input.equals("global"))
				return Global.INSTANCE;
			String[] split = input.split(":", 2);
			if (split.length != 2 || split[1].isEmpty())
				throw new IllegalArgumentException("Invalid target " + input);
			return switch (split[0]) {
				case "team" -> new Team(split[1].replace('_', ' '));
				case "player" -> new Player(split[1]);
				default -> throw new IllegalArgumentException("Invalid target " + input);
			};
		}

	}
}

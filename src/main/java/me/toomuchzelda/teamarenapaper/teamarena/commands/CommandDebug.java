package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MinecraftFont;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandDebug extends CustomCommand {

	// TODO temporary feature
	public static boolean ignoreWinConditions;
	public static boolean sniperAccuracy;

	public CommandDebug() {
		super("debug", "", "/debug ...", PermissionLevel.OWNER);
	}

	private final List<MiniMapManager.CanvasOperation> canvasOperations = new ArrayList<>();
	private final MiniMapManager.CanvasOperation operationExecutor = (viewer, info, canvas, renderer) -> {
		for (var operation : canvasOperations)
			operation.render(viewer, info, canvas, renderer);
	};

	private static final Pattern MAP_COLOR = Pattern.compile("#([0-9A-Fa-f]{6})");

	public boolean runConsoleCommands(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
		switch (args[0]) {
			case "gui" -> {
				Inventories.debug = args.length == 2 ? args[1].equalsIgnoreCase("true") : !Inventories.debug;
				sender.sendMessage(Component.text("GUI DEBUG: " + Inventories.debug, NamedTextColor.DARK_GREEN));
			}
			case "game" -> {
				if (args.length < 2)
					throw throwUsage("/debug game start/<...> [value]");

				if (args[1].equalsIgnoreCase("start")) {
					Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), task -> {
						var state = Main.getGame().getGameState();
						if (state == GameState.PREGAME) {
							if (Bukkit.getOnlinePlayers().size() < 2) {
								ignoreWinConditions = true;
							}
							Main.getGame().prepTeamsDecided();
						} else if (state == GameState.TEAMS_CHOSEN) {
							Main.getGame().prepGameStarting();
						} else if (state == GameState.GAME_STARTING) {
							Main.getGame().prepLive();
							task.cancel();
						} else {
							task.cancel();
						}
					}, 0, 1);
				} else if (args[1].equalsIgnoreCase("ignorewinconditions")) {
					ignoreWinConditions = args.length == 3 ? "true".equalsIgnoreCase(args[2]) : !ignoreWinConditions;
					sender.sendMessage(Component.text("Set ignore win conditions to " + ignoreWinConditions, NamedTextColor.GREEN));
				} else if (args[1].equalsIgnoreCase("sniperaccuracy")) {
					sniperAccuracy = args.length == 3 ? "true".equalsIgnoreCase(args[2]) : !sniperAccuracy;
					sender.sendMessage(Component.text("Set sniper accuracy debug to " + sniperAccuracy, NamedTextColor.GREEN));
				}
			}
			case "draw" -> {
				if (args.length < 4)
					throw throwUsage("/debug draw clear/<text/area> <x> <z> ...");

				int x = Integer.parseInt(args[2]), z = Integer.parseInt(args[3]);
				MiniMapManager.CanvasOperation operation;
				if ("text".equalsIgnoreCase(args[1])) {
					if (args.length < 5)
						throw throwUsage("/debug draw text <x> <z> <text>");

					// white by default
					String text = "\u00A734;" + MAP_COLOR.matcher(
							String.join(" ", Arrays.copyOfRange(args, 4, args.length))
									.replace('&', ChatColor.COLOR_CHAR)
					).replaceAll(result -> {
						int hex = Integer.parseInt(result.group(1), 16);
						//noinspection deprecation
						return "\u00A7" + MapPalette.matchColor(new java.awt.Color(hex)) + ";";
					});
					canvasOperations.add((viewer, ignored, canvas, renderer) ->
							canvas.drawText((renderer.convertX(x) + 128) / 2, (renderer.convertZ(z) + 128) / 2,
									MinecraftFont.Font, text));
				} else if ("area".equalsIgnoreCase(args[1])) {
					if (args.length < 7)
						throw throwUsage("/debug draw area <x> <z> <x2> <z2> <color>");
					int x2 = Integer.parseInt(args[4]), z2 = Integer.parseInt(args[5]);
					byte color;
					Matcher matcher = MAP_COLOR.matcher(args[6]);
					if (matcher.matches()) {
						int hex = Integer.parseInt(matcher.group(1), 16);
						//noinspection deprecation
						color = MapPalette.matchColor(new java.awt.Color(hex));
					} else {
						color = Byte.parseByte(args[6]);
					}
					int minX = Math.min(x, x2), maxX = Math.max(x, x2), minY = Math.min(z, z2), maxY = Math.max(z, z2);
					canvasOperations.add((viewer, ignored, canvas, renderer) -> {
						int startX = (renderer.convertX(minX) + 128) / 2, endX = (renderer.convertX(maxX) + 128) / 2;
						int startY = (renderer.convertZ(minY) + 128) / 2, endY = (renderer.convertZ(maxY) + 128) / 2;
						for (int i = startX; i < endX; i++)
							for (int j = startY; j < endY; j++)
								canvas.setPixel(i, j, color);
					});
				} else if ("clear".equalsIgnoreCase(args[1])) {
					canvasOperations.clear();
				}
				if (!Main.getGame().miniMap.hasCanvasOperation(operationExecutor)) {
					Main.getGame().miniMap.registerCanvasOperation(operationExecutor);
				}
			}
			case "votetest" -> {
				CommandCallvote.instance.createVote(null, sender.name(),
						Component.text("Next player to ban?"),
						new CommandCallvote.TopicOptions(true, null, CommandCallvote.LE_FUNNY_VOTE,
								CommandCallvote.VoteOption.getOptions(
										new CommandCallvote.VoteOption("zelda", TextUtils.getUselessRainbowText("toomuchzelda"), TextUtils.getUselessRainbowText("toomuchzelda: ")),
										new CommandCallvote.VoteOption("toed", Component.text("T_0_E_D", NamedTextColor.DARK_GREEN)),
										new CommandCallvote.VoteOption("onett", Component.text("Onett_", NamedTextColor.BLUE))
								)));
			}
			case "setrank" -> {
				if (args.length < 2)
					throw throwUsage("/debug setrank <rank> [player]");

				PermissionLevel level = PermissionLevel.valueOf(args[1]);
				Player target = getPlayerOrThrow(sender, args, 2);
				PlayerInfo info = Main.getPlayerInfo(target);
				info.permissionLevel = level;
			}
			case "setteam" -> {
				if (args.length < 2)
					throw throwUsage("/debug setteam <team> [player]");

				TeamArenaTeam[] teams = Main.getGame().getTeams();
				TeamArenaTeam targetTeam = null;
				for (TeamArenaTeam team : teams) {
					if (team.getSimpleName().replace(' ', '_').equalsIgnoreCase(args[1])) {
						targetTeam = team;
						break;
					}
				}
				if (targetTeam == null)
					throw new CommandException(Component.text("Team not found!", NamedTextColor.RED));

				Player target = getPlayerOrThrow(sender, args, 2);
				targetTeam.addMembers(target);
			}
			case "setgame", "setnextgame" -> {
				if (args.length < 2)
					throw throwUsage("/debug " + args[0] + " <game> [map]");

				TeamArena.nextGameType = GameType.valueOf(args[1]);
				if (args.length > 2) {
					TeamArena.nextMapName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
				} else {
					TeamArena.nextMapName = null;
				}
				sender.sendMessage(Component.textOfChildren(
						Component.text("Set game mode to ", NamedTextColor.GREEN),
						Component.text(TeamArena.nextGameType.name(), NamedTextColor.YELLOW),
						Component.text(" and map to ", NamedTextColor.GREEN),
						Component.text(TeamArena.nextMapName != null ? TeamArena.nextMapName : "random", NamedTextColor.YELLOW)
				));
				// abort the current game
				if ("setgame".equalsIgnoreCase(args[0])) {
					Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), task -> {
						var state = Main.getGame().getGameState();
						if (state == GameState.PREGAME) {
							if (Bukkit.getOnlinePlayers().size() < 2) {
								ignoreWinConditions = true;
							}
							Main.getGame().prepTeamsDecided();
						} else if (state == GameState.TEAMS_CHOSEN) {
							Main.getGame().prepGameStarting();
						} else if (state == GameState.GAME_STARTING) {
							Main.getGame().prepLive();
						} else if (state == GameState.LIVE) {
							Main.getGame().prepEnd();
						} else if (state == GameState.END) {
							Main.getGame().prepDead();
							task.cancel();
						} else {
							task.cancel();
						}
					}, 0, 1);
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
		if (args.length == 0) {
			showUsage(sender);
			return;
		}

		if (runConsoleCommands(sender, commandLabel, args)) {
			// handled by console commands
			return;
		}

		if (!(sender instanceof Player player)) {
			sender.sendMessage(PLAYER_ONLY);
			return;
		}

		switch (args[0]) {
			case "hide" -> {
				for (Player viewer : Bukkit.getOnlinePlayers()) {
					if (viewer.canSee(player)) {
						viewer.hidePlayer(Main.getPlugin(), player);
					} else {
						viewer.showPlayer(Main.getPlugin(), player);
					}
				}
			}
			default -> showUsage(sender);
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return Arrays.asList("hide", "gui", "game", "setrank", "setteam", "setgame", "setnextgame", "votetest", "draw");
		} else if (args.length == 2) {
			return switch (args[0].toLowerCase(Locale.ENGLISH)) {
				case "gui" -> Arrays.asList("true", "false");
				case "game" -> Arrays.asList("start", "ignorewinconditions", "sniperaccuracy");
				case "setrank" -> Arrays.stream(PermissionLevel.values()).map(Enum::name).toList();
				case "setteam" -> Arrays.stream(Main.getGame().getTeams())
						.map(team -> team.getSimpleName().replace(' ', '_'))
						.toList();
				case "setgame", "setnextgame" -> Arrays.stream(GameType.values()).map(Enum::name).toList();
				case "draw" -> Arrays.asList("text", "area");
				default -> Collections.emptyList();
			};
		} else if (args.length == 3) {
			return switch (args[0].toLowerCase(Locale.ENGLISH)) {
				case "setrank", "setteam" -> Bukkit.getOnlinePlayers().stream()
						.map(Player::getName).toList();
				case "game" -> Arrays.asList("true", "false");
				case "setgame", "setnextgame" -> {
					String gameMode = args[1].toUpperCase(Locale.ENGLISH);
					File mapContainer = new File("Maps" + File.separator + gameMode);
					if (!mapContainer.exists() || !mapContainer.isDirectory())
						yield Collections.emptyList();

					String[] files = mapContainer.list();
					yield files != null ? Arrays.asList(files) : Collections.emptyList();
				}
				default -> Collections.emptyList();
			};
		}
		return Collections.emptyList();
	}
}

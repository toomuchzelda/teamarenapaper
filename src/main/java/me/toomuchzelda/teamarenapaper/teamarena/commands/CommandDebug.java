package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.map.MinecraftFont;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class CommandDebug extends CustomCommand {

    // TODO temporary feature
    public static boolean ignoreWinConditions;
    public static boolean disableAnimations;
	public static boolean sniperAccuracy;

    public CommandDebug() {
        super("debug", "", "/debug ...", PermissionLevel.OWNER);
    }

    private List<MiniMapManager.CanvasOperation> canvasOperations = new ArrayList<>();
    private final MiniMapManager.CanvasOperation operationApplier = (viewer, info, canvas, renderer) -> {
        for (var operation : canvasOperations)
            operation.render(viewer, info, canvas, renderer);
    };

	public boolean runConsoleCommands(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
		switch (args[0]) {
			case "gui" -> {
				if (args.length == 2) {
					Inventories.debug = args[1].equalsIgnoreCase("true");
				} else {
					Inventories.debug = !Inventories.debug;
				}
				sender.sendMessage(Component.text("GUI DEBUG: " + Inventories.debug).color(NamedTextColor.DARK_GREEN));
			}
			case "game" -> {
				if (args.length < 2) {
					showUsage(sender, "/debug game <ignorewinconditions> [true/false]");
					break;
				}
				if (args[1].equalsIgnoreCase("ignorewinconditions")) {
					ignoreWinConditions = args.length == 3 ? "true".equalsIgnoreCase(args[2]) : !ignoreWinConditions;
					sender.sendMessage(Component.text("Set ignore win conditions to " + ignoreWinConditions)
							.color(NamedTextColor.GREEN));
				} else if (args[1].equalsIgnoreCase("stopwastingmybandwidth")) {
					disableAnimations = args.length == 3 ? "true".equalsIgnoreCase(args[2]) : !disableAnimations;
					sender.sendMessage(Component.text("Set disable animations to " + disableAnimations)
							.color(NamedTextColor.GREEN));
				} else if (args[1].equalsIgnoreCase("sniperaccuracy")) {
					sniperAccuracy = args.length == 3 ? "true".equalsIgnoreCase(args[2]) : !sniperAccuracy;
					sender.sendMessage(Component.text("Set sniper accuracy debug to " + sniperAccuracy)
							.color(NamedTextColor.GREEN));
				}
			}
			case "draw" -> {
				if (args.length < 4) {
					showUsage(sender, "/debug draw <text/area> <x> <z> ...");
					break;
				}
				int x = Integer.parseInt(args[2]), z = Integer.parseInt(args[3]);
//                byte color = Byte.parseByte(args[4]);
				MiniMapManager.CanvasOperation operation;
				if ("text".equalsIgnoreCase(args[1])) {
					if (args.length < 5) {
						showUsage(sender, "/debug draw text <x> <z> <text>");
						break;
					}
					// white by default
					String text = "\u00A732;" + String.join(" ", Arrays.copyOfRange(args, 4, args.length))
							.replace('&', ChatColor.COLOR_CHAR);
					operation = (viewer, ignored, canvas, renderer) ->
							canvas.drawText((renderer.convertX(x) + 128) / 2, (renderer.convertZ(z) + 128) / 2,
									MinecraftFont.Font, text);
				} else {
					if (args.length < 7) {
						showUsage(sender, "/debug draw area <x> <z> <x2> <z2> <color>");
						break;
					}
					int x2 = Integer.parseInt(args[4]), z2 = Integer.parseInt(args[5]);
					byte color = Byte.parseByte(args[6]);
					operation = (viewer, ignored, canvas, renderer) -> {
						int startX = (renderer.convertX(Math.min(x, x2)) + 128) / 2;
						int endX = (renderer.convertX(Math.max(x, x2)) + 128) / 2;
						int startY = (renderer.convertZ(Math.min(z, z2)) + 128) / 2;
						int endY = (renderer.convertZ(Math.max(z, z2)) + 128) / 2;
						for (int i = startX; i < endX; i++) {
							for (int j = startY; j < endY; j++) {
								canvas.setPixel(i, j, color);
							}
						}
					};
				}
				canvasOperations.add(operation);
				if (!Main.getGame().miniMap.hasCanvasOperation(operationApplier)) {
					Main.getGame().miniMap.registerCanvasOperation(operationApplier);
				}
			}
			case "votetest" -> {
				var options = CommandCallvote.VoteOption.getOptions(
						new CommandCallvote.VoteOption("toomuchzelda",
								TextUtils.getUselessRainbowText("toomuchzelda"),
								TextUtils.getUselessRainbowText("toomuchzelda: ")),
						new CommandCallvote.VoteOption("toed", Component.text("T_0_E_D", NamedTextColor.DARK_GREEN)),
						new CommandCallvote.VoteOption("onett", Component.text("Onett_", NamedTextColor.BLUE))
				);
				CommandCallvote.instance.createVote(null, sender.name(),
						Component.text("Next player to ban?"),
						new CommandCallvote.TopicOptions(true, null, CommandCallvote.LE_FUNNY_VOTE, options));
			}
			case "setrank" -> {
				if (args.length < 2) {
					showUsage(sender, "/debug setrank <rank> [player]");
					break;
				}
				PermissionLevel level = PermissionLevel.valueOf(args[1]);
				Player target = getPlayerOrThrow(sender, args, 2);
				if (target == null) {
					sender.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
					break;
				}
				PlayerInfo info = Main.getPlayerInfo(target);
				info.permissionLevel = level;
			}
			case "setteam" -> {
				if (args.length < 2) {
					showUsage(sender, "/debug setteam <team> [player]");
					break;
				}
				TeamArenaTeam[] teams = Main.getGame().getTeams();
				TeamArenaTeam targetTeam = null;
				for (TeamArenaTeam team : teams) {
					if (team.getSimpleName().replace(' ', '_').equalsIgnoreCase(args[1])) {
						targetTeam = team;
						break;
					}
				}
				if (targetTeam == null) {
					sender.sendMessage(Component.text("Team not found!", NamedTextColor.RED));
					break;
				}
				Player target = getPlayerOrThrow(sender, args, 2);
				if (target == null) {
					sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
					break;
				}
				PlayerInfo info = Main.getPlayerInfo(target);
				targetTeam.addMembers(target);
			}
			case "setgame" -> {
				if (args.length < 2) {
					showUsage(sender, "/debug setgame <game> [map]");
				}
				TeamArena.nextGameType = GameType.valueOf(args[1]);
				if (args.length > 2) {
					TeamArena.nextMapName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
				} else {
					TeamArena.nextMapName = null;
				}
				sender.sendMessage(MiniMessage.miniMessage().deserialize(
						"<green>Set game mode to <yellow><ctf></yellow> and map to <yellow><map></yellow>",
						Placeholder.unparsed("ctf", TeamArena.nextGameType.name()),
						Placeholder.unparsed("map", TeamArena.nextMapName != null ? TeamArena.nextMapName : "random")
				));

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
            return Arrays.asList("hide", "gui", "game", "setrank", "setteam", "setgame", "votetest");
        } else if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "gui" -> Arrays.asList("true", "false");
                case "game" -> Arrays.asList("ignorewinconditions", "stopwastingmybandwidth", "sniperaccuracy");
                case "setrank" -> Arrays.stream(PermissionLevel.values()).map(Enum::name).toList();
                case "setteam" -> Arrays.stream(Main.getGame().getTeams())
                        .map(team -> team.getSimpleName().replace(' ', '_'))
                        .toList();
				case "setgame" -> Arrays.stream(GameType.values()).map(Enum::name).toList();
                default -> Collections.emptyList();
            };
        } else if (args.length == 3) {
            return switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "setrank", "setteam" -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName).toList();
                case "game" -> Arrays.asList("true", "false");
				case "setgame" -> {
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

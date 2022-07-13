package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MinecraftFont;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandDebug extends CustomCommand {

	// TODO temporary feature
	public static boolean ignoreWinConditions;
	public static boolean sniperAccuracy;
	public static PacketEntity testPacketEntity = null;
	public static Predicate<Kit> kitPredicate = ignored -> true;

	public CommandDebug() {
		super("debug", "", "/debug ...", PermissionLevel.OWNER, "abuse");
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
				} else if (args[1].equalsIgnoreCase("kitfilter")) {
					setKitRestrictions(sender, args);
				}
			}
			case "draw" -> doDrawCommand(args);
			case "votetest" -> CommandCallvote.instance.createVote(null, sender.name(),
					Component.text("Next player to ban?"),
					new CommandCallvote.TopicOptions(true, null, CommandCallvote.LE_FUNNY_VOTE,
							CommandCallvote.VoteOption.getOptions(
									new CommandCallvote.VoteOption("zelda", TextUtils.getUselessRainbowText("toomuchzelda"), TextUtils.getUselessRainbowText("toomuchzelda: ")),
									new CommandCallvote.VoteOption("toed", Component.text("T_0_E_D", NamedTextColor.DARK_GREEN)),
									new CommandCallvote.VoteOption("onett", Component.text("Onett_", NamedTextColor.BLUE))
							)));
			case "respawn" -> {
				var game = Main.getGame();
				var targetPlayers = selectPlayersOrThrow(sender, args, 2);
				for (var target : targetPlayers) {
					PlayerInfo info = Main.getPlayerInfo(target);
					game.respawnPlayer(target);
					game.givePlayerItems(target, info, true);
				}
			}
			case "setrank" -> {
				if (args.length < 2)
					throw throwUsage("/debug setrank <rank> [player]");

				PermissionLevel level = PermissionLevel.valueOf(args[1]);
				Player target = getPlayerOrThrow(sender, args, 2);
				PlayerInfo info = Main.getPlayerInfo(target);
				info.permissionLevel = level;

				target.sendMessage(Component.text("Your rank has been updated to " + level.name(), NamedTextColor.GREEN));
			}
			case "setteam" -> {
				if (args.length < 2)
					throw throwUsage("/debug setteam <team> [selector]");

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

				var targetPlayers = selectPlayersOrThrow(sender, args, 2);
				targetTeam.addMembers(targetPlayers.toArray(new Player[0]));

				var message = Component.textOfChildren(
						Component.text("Your team has been updated to ", NamedTextColor.GREEN),
						targetTeam.getComponentName()
				);
				targetPlayers.forEach(player -> player.sendMessage(message));
			}
			case "setkit" -> {
				if (args.length < 2)
					throw throwUsage("/debug setkit <kit> [selector]");
				var game = Main.getGame();
				var kit = game.findKit(args[1]);
				if (kit == null) {
					throw new CommandException("Invalid kit " + args[1]);
				}
				var targetPlayers = selectPlayersOrThrow(sender, args, 2);
				for (var target : targetPlayers) {
					PlayerInfo info = Main.getPlayerInfo(target);

					if (info.activeKit != null) {
						info.activeKit.removeKit(target, info);
					}
					info.kit = kit;
					if (game.getGameState() == GameState.LIVE) {
						game.givePlayerItems(target, info, true);
					}
					target.sendMessage(Component.textOfChildren(
							Component.text("Your kit has been updated to ", NamedTextColor.GREEN),
							kit.getDisplayName()
					));
				}
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
			case "packetent" -> {
				Player p = (Player) sender;
				if(testPacketEntity == null) {
					testPacketEntity = new PacketEntity(PacketEntity.NEW_ID, EntityType.AXOLOTL, p.getLocation(), null, PacketEntity.VISIBLE_TO_ALL) {
						@Override
						public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
							this.setMetadata(MetaIndex.AXOLOTL_COLOR, MathUtils.randomMax(4));
							EntityUtils.playEffect(this, ClientboundAnimatePacket.CRITICAL_HIT);
							this.getWorld().playSound(this.getLocation(), Sound.ENTITY_AXOLOTL_SWIM, 1f, 1f);
							if(attack)
								Bukkit.broadcast(Component.text("Curse you " + player.getName() + "!", NamedTextColor.DARK_RED));
						}
					};

					testPacketEntity.respawn();
				}
				else if(!testPacketEntity.isAlive()) {
					testPacketEntity.respawn();
				}
				else {
					testPacketEntity.setMetadata(MetaIndex.AXOLOTL_COLOR, MathUtils.randomMax(4));
					Location pLoc = p.getLocation();
					Vector dir = pLoc.toVector().subtract(testPacketEntity.getLocation().toVector()).normalize();

					Location newLoc = testPacketEntity.getLocation().setDirection(dir).add(0, 0.2, 0);
					testPacketEntity.move(newLoc);
					testPacketEntity.refreshViewerMetadata();
				}

			}
			case "packetentkill" -> {
				if(testPacketEntity != null) {
					if(testPacketEntity.isAlive()) {
						testPacketEntity.despawn();
					}
					else {
						testPacketEntity.remove();
						testPacketEntity = null;
					}
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	private void setKitRestrictions(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
		if (args.length < 3)
			throw throwUsage("/debug game kitfilter <allow/block> [kit1,...]");
		boolean block = args[2].equalsIgnoreCase("block");
		Set<String> kitNames;
		if (args.length == 4) {
			kitNames = Set.of(args[3].split(","));
		} else {
			kitNames = Set.of();
		}
		// Truth table
		// Action    | Contains | Kit allowed
		// block (T) | T        | F
		// block (T) | F        | T
		// allow (F) | T        | T
		// allow (F) | F        | F
		kitPredicate = kit -> block != kitNames.contains(kit.getName().toLowerCase(Locale.ENGLISH));
		sender.sendMessage(Component.text("Set kit restrictions to: " +
				args[2] + " " + kitNames, NamedTextColor.GREEN));
		Optional<Kit> optionalFallbackKit = Main.getGame().getKits().stream()
				.filter(kitPredicate)
				.findFirst();
		if (optionalFallbackKit.isPresent()) {
			Kit fallbackKit = optionalFallbackKit.get();
			Main.getPlayerInfoMap().forEach((player, playerInfo) -> {
				if (playerInfo.kit != null && !kitPredicate.test(playerInfo.kit)) {
					playerInfo.kit = fallbackKit;
					player.sendMessage(Component.text("The kit you have selected has been disabled. " +
							"It has been replaced with: " + fallbackKit.getName(), NamedTextColor.YELLOW));
				}
				if (playerInfo.activeKit != null && !kitPredicate.test(playerInfo.activeKit)) {
					// also change active kit
					playerInfo.activeKit.removeKit(player, playerInfo);
					Main.getGame().givePlayerItems(player, playerInfo, true);
					player.sendMessage(Component.text("The kit you are using has been disabled. " +
							"It has been replaced with your selected kit.", NamedTextColor.YELLOW));
				}
			});
		} else { // cannot allow blocking all kits!
			sender.sendMessage(Component.text("Warning: no fallback kit found. Allowing all kits instead.", NamedTextColor.YELLOW));
			kitPredicate = kit -> true;
		}
	}

	private void doDrawCommand(@NotNull String @NotNull [] args) {
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
		} else if ("invalidatebase".equalsIgnoreCase(args[1])) {
			((MiniMapManager.GameMapRenderer) Main.getGame().miniMap.view.getRenderers().get(0)).hasDrawn = false;
		}
		if (!Main.getGame().miniMap.hasCanvasOperation(operationExecutor)) {
			Main.getGame().miniMap.registerCanvasOperation(operationExecutor);
		}
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
			case "guitest" -> {
				if (args.length < 2)
					throw throwUsage("/debug guitest <tab/spectate>");
				var inventory = switch (args[1]) {
					case "tab" -> new TabTest();
					case "spectate" -> new SpectateInventory(null);
					default -> throw throwUsage("/debug guitest <tab/spectate>");
				};
				Inventories.openInventory(player, inventory);
			}
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
			return Arrays.asList("hide", "gui", "guitest", "game", "setrank", "setteam", "setkit", "setgame", "setnextgame", "votetest", "draw");
		} else if (args.length == 2) {
			return switch (args[0].toLowerCase(Locale.ENGLISH)) {
				case "gui" -> Arrays.asList("true", "false");
				case "guitest" -> Arrays.asList("tab", "spectate");
				case "game" -> Arrays.asList("start", "ignorewinconditions", "sniperaccuracy", "kitfilter");
				case "setrank" -> Arrays.stream(PermissionLevel.values()).map(Enum::name).toList();
				case "setteam" -> Arrays.stream(Main.getGame().getTeams())
						.map(team -> team.getSimpleName().replace(' ', '_'))
						.toList();
				case "setkit" -> Main.getGame().getTabKitList();
				case "setgame", "setnextgame" -> Arrays.stream(GameType.values()).map(Enum::name).toList();
				case "draw" -> Arrays.asList("text", "area", "clear", "invalidatebase");
				default -> Collections.emptyList();
			};
		} else if (args.length == 3) {
			return switch (args[0].toLowerCase(Locale.ENGLISH)) {
				case "setrank", "setteam", "setkit" -> Bukkit.getOnlinePlayers().stream()
						.map(Player::getName).toList();
				case "game" -> switch (args[1]) {
					case "start" -> Collections.emptyList();
					case "kitfilter" -> Arrays.asList("allow", "block");
					default -> Arrays.asList("true", "false");
				};
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

	static class TabTest implements InventoryProvider {
		boolean extended = false;
		TabBar<@NotNull Material> tab = new TabBar<>(Material.BLACK_WOOL);

		@Override
		public Component getTitle(Player player) {
			return Component.text("Tab test");
		}

		@Override
		public int getRows() {
			return 6;
		}

		List<Material> wools = List.copyOf(Tag.WOOL.getValues());
		@Override
		public void init(Player player, InventoryAccessor inventory) {
			tab.showTabs(inventory, wools, TabBar.highlightWhenSelected(ItemStack::new),
					0, extended ? 3 : 7, true);

			if (extended) {
				for (int i = 4; i < 8; i++) {
					inventory.set(i, new ItemStack(Material.CLOCK));
				}
			}

			inventory.set(8, ItemBuilder.of(Material.PAPER)
					.displayName(Component.text("Toggle ADVANCED options", NamedTextColor.YELLOW))
					.toClickableItem(e -> {
						extended = !extended;
						inventory.invalidate();
					}));

			inventory.set(9, new ItemStack(tab.getCurrentTab()));
		}
	}
}

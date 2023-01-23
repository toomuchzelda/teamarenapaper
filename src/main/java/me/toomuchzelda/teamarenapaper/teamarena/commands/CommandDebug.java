package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.explosions.CustomExplosion;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.InventoryProvider;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.inventory.TabBar;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.MiniMapManager;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticsManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.inventory.SpectateInventory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitBurst;
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

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandDebug extends CustomCommand {

	// TODO temporary feature
	public static boolean ignoreWinConditions;
	public static boolean kitSniper;
	public static boolean sniperAccuracy;
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
					ignoreWinConditions = true;
					skipGameState(GameState.LIVE);
				} else if (args[1].equals("ignorewinconditions")) {
					ignoreWinConditions = args.length == 3 ? Boolean.parseBoolean(args[2]) : !ignoreWinConditions;
					sender.sendMessage(Component.text("Set ignore win conditions to " + ignoreWinConditions, NamedTextColor.GREEN));
				} else if (args[1].equals("sniperaccuracy")) {
					sniperAccuracy = args.length == 3 ? Boolean.parseBoolean(args[2]) : !sniperAccuracy;
					sender.sendMessage(Component.text("Set sniper accuracy debug to " + sniperAccuracy, NamedTextColor.GREEN));
				} else if (args[1].equals("enablekitsniper")) {
					kitSniper = args.length == 3 ? Boolean.parseBoolean(args[2]) : !kitSniper;
					sender.sendMessage(Component.text("Set enable kit sniper to " + kitSniper, NamedTextColor.GREEN));
				} else if (args[1].equals("kitfilter")) {
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
				selectPlayersOrThrow(sender, args, 2).forEach(game::respawnPlayer);
			}
			case "setrank" -> {
				if (args.length < 2)
					throw throwUsage("/debug setrank <rank> [player]");

				PermissionLevel level = PermissionLevel.valueOf(args[1]);
				Player target = getPlayerOrThrow(sender, args, 2);
				PlayerInfo info = Main.getPlayerInfo(target);
				info.permissionLevel = level;
				target.updateCommands();
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
			case "explode" -> {
				if(args.length < 2) {
					throw throwUsage("rad, guar, damage, minDamage, knockbackStrength");
				}

				Player p = (Player) sender;
				double rad = Double.parseDouble(args[1]);
				double guar = Double.parseDouble(args[2]);
				double damage = Double.parseDouble(args[3]);
				double minDamage = Double.parseDouble(args[4]);
				double knockbackStrength = Double.parseDouble(args[5]);

				CustomExplosion explosion = new CustomExplosion(p.getLocation().add(0, 0.2, 0),
						rad, guar, damage, minDamage, knockbackStrength, DamageType.EXPLOSION, p);
				explosion.explode();
			}
			case "burst" -> {
				KitBurst.BurstAbility.HIDE_SHOTGUN_ARROWS = !KitBurst.BurstAbility.HIDE_SHOTGUN_ARROWS;
				sender.sendMessage("Set burst show arrows to: " + KitBurst.BurstAbility.HIDE_SHOTGUN_ARROWS);
			}
			case "fakehitbox" -> {
				boolean show = args.length == 2 ? Boolean.parseBoolean(args[1]) : !FakeHitboxManager.show;
				FakeHitboxManager.setVisibility(show);
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	public static Kit filterKit(Kit kit) {
		if (!kitPredicate.test(kit))
			return Main.getGame().getKits().stream()
					.filter(kitPredicate)
					.findFirst().orElse(null);
		else
			return kit;
	}

	private void setKitRestrictions(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
		if (args.length < 3)
			throw throwUsage("/debug game kitfilter <allow/block/clear> [kit1,...]");
		if (args[2].equalsIgnoreCase("clear")) {
			kitPredicate = kit -> true;
			sender.sendMessage(Component.text("Allowing all kits.", NamedTextColor.YELLOW));
			return;
		}

		boolean block = args[2].equalsIgnoreCase("block");
		Set<String> kitNames;
		if (args.length == 4) {
			kitNames = Set.of(args[3].split(","));
		} else {
			kitNames = Set.of();
		}
		// XOR
		kitPredicate = kit -> block != kitNames.contains(kit.getName().toLowerCase(Locale.ENGLISH));
		sender.sendMessage(Component.text("Set kit restrictions to: " +
				args[2] + " " + kitNames, NamedTextColor.GREEN));
		Main.getGame().getKits().stream()
				.filter(kitPredicate)
				.findFirst()
				.ifPresentOrElse(fallbackKit -> Main.getPlayerInfoMap().forEach((player, playerInfo) -> {
					if (playerInfo.kit != null && !kitPredicate.test(playerInfo.kit)) {
						playerInfo.kit = fallbackKit;
						player.sendMessage(Component.text("The kit you have selected has been disabled by an admin. " +
								"It has been replaced with: " + fallbackKit.getName(), NamedTextColor.YELLOW));
					}
					if (playerInfo.activeKit != null && !kitPredicate.test(playerInfo.activeKit)) {
						// also change active kit
						playerInfo.activeKit.removeKit(player, playerInfo);
						Main.getGame().givePlayerItems(player, playerInfo, true);
						player.sendMessage(Component.text("The kit you are using has been disabled by an admin. " +
								"It has been replaced with your selected kit.", NamedTextColor.YELLOW));
					}
				}), /* else */ () -> {
					// cannot allow blocking all kits!
					kitPredicate = kit -> true;
					sender.sendMessage(Component.text("Warning: no fallback kit found. Allowing all kits instead.", NamedTextColor.YELLOW));
				});
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

	@Deprecated
	private static void skipGameState(GameState dest) {
		int ordinal = dest.ordinal();
		boolean didIgnoreWinConditions = ignoreWinConditions;
		Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), task -> {
			var state = Main.getGame().getGameState();
			if (state.ordinal() >= ordinal) {
				ignoreWinConditions = didIgnoreWinConditions;
				task.cancel();
				return;
			}

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
				ignoreWinConditions = didIgnoreWinConditions;
				task.cancel();
			} else {
				ignoreWinConditions = didIgnoreWinConditions;
				task.cancel();
			}
		}, 0, 1);
	}


	private static PacketEntity testPacketEntity = null;

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
			case "packetent" -> {
				if (testPacketEntity == null) {
					testPacketEntity = new PacketEntity(PacketEntity.NEW_ID, EntityType.AXOLOTL, player.getLocation(), null, PacketEntity.VISIBLE_TO_ALL) {
						@Override
						public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
							this.setMetadata(MetaIndex.AXOLOTL_COLOR_OBJ, MathUtils.randomMax(4));
							EntityUtils.playEffect(this, ClientboundAnimatePacket.CRITICAL_HIT);
							this.getWorld().playSound(this.getLocation(), Sound.ENTITY_AXOLOTL_SWIM, 1f, 1f);
							if (attack)
								Bukkit.broadcast(Component.text("Curse you " + player.getName() + "!", NamedTextColor.DARK_RED));
						}
					};

					testPacketEntity.respawn();
				} else if (!testPacketEntity.isAlive()) {
					testPacketEntity.respawn();
				} else {
					testPacketEntity.setMetadata(MetaIndex.AXOLOTL_COLOR_OBJ, MathUtils.randomMax(4));
					Location pLoc = player.getLocation();
					Vector dir = pLoc.toVector().subtract(testPacketEntity.getLocation().toVector()).normalize();

					Location newLoc = testPacketEntity.getLocation().setDirection(dir).add(0, 0.2, 0);
					testPacketEntity.move(newLoc);
					testPacketEntity.refreshViewerMetadata();
				}

			}
			case "packetentkill" -> {
				if (testPacketEntity != null) {
					if (testPacketEntity.isAlive()) {
						testPacketEntity.despawn();
					} else {
						testPacketEntity.remove();
						testPacketEntity = null;
					}
				}
			}
			case "graffititest" -> {
				if (args.length < 2)
					throw throwUsage("/debug graffititest <graffiti>");
				NamespacedKey graffiti = NamespacedKey.fromString(args[1]);
				Main.getGame().graffiti.spawnGraffiti(player, graffiti);
			}
			default -> showUsage(sender);
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return Arrays.asList("hide", "gui", "guitest", "game", "setrank", "setteam", "setkit",
				"setgame", "setnextgame", "votetest", "draw", "graffititest", "burst", "respawn", "fakehitbox");
		} else if (args.length == 2) {
			return switch (args[0].toLowerCase(Locale.ENGLISH)) {
				case "gui" -> Arrays.asList("true", "false");
				case "guitest" -> Arrays.asList("tab", "spectate");
				case "game" -> Arrays.asList("start", "ignorewinconditions", "sniperaccuracy", "enablekitsniper", "kitfilter");
				case "setrank" -> Arrays.stream(PermissionLevel.values()).map(Enum::name).toList();
				case "setteam" -> Arrays.stream(Main.getGame().getTeams())
						.map(team -> team.getSimpleName().replace(' ', '_'))
						.toList();
				case "setkit" -> Main.getGame().getTabKitList();
				case "draw" -> Arrays.asList("text", "area", "clear", "invalidatebase");
				case "graffititest" -> CosmeticsManager.getLoadedCosmetics(CosmeticType.GRAFFITI).stream().map(NamespacedKey::toString).toList();
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
				default -> Collections.emptyList();
			};
		}
		return Collections.emptyList();
	}

	static class TabTest implements InventoryProvider {
		boolean extended = false;
		TabBar<@NotNull Material> tab = new TabBar<>(Material.BLACK_WOOL);

		@Override
		public @NotNull Component getTitle(Player player) {
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
					0, extended ? 3 : 8, true);

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

package me.toomuchzelda.teamarenapaper.teamarena.commands;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.network.StatusClient;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.ServerListPingManager;
import me.toomuchzelda.teamarenapaper.explosions.CustomExplosion;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.InventoryProvider;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.inventory.TabBar;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticsManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.ArrowManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.StatusOreType;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.hideandseek.PacketFlyingPoint;
import me.toomuchzelda.teamarenapaper.teamarena.inventory.SpectateInventory;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.PayloadTestKillstreak;
import me.toomuchzelda.teamarenapaper.utils.*;
import me.toomuchzelda.teamarenapaper.utils.packetentities.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class CommandDebug extends CustomCommand {

	// TODO temporary feature
	public static boolean ignoreWinConditions;
	public static boolean disableMiniMapInCaseSomethingTerribleHappens;

	public static boolean sniperShowRewind;

	public CommandDebug() {
		super("debug", "", "/debug ...", PermissionLevel.OWNER, "abuse");
	}

	private final List<MiniMapManager.CanvasOperation> canvasOperations = new ArrayList<>();
	private final MiniMapManager.CanvasOperation operationExecutor = (viewer, info, canvas, renderer) -> {
		for (var operation : canvasOperations)
			operation.render(viewer, info, canvas, renderer);
	};

	private static final Pattern MAP_COLOR = Pattern.compile("#([0-9A-Fa-f]{6})");

	private static String namePlayers(Collection<Player> players) {
		return players.stream()
			.map(Player::getName)
			.collect(Collectors.joining(", ", "[", "]"));
	}

	private void auditEvent(CommandSender initiator, String message, Object... args) {
		Component broadcast = Component.textOfChildren(
			Component.text("[DEBUG] "),
			initiator instanceof Player player ? EntityUtils.getComponent(player) : initiator.name(),
			Component.text(": "),
			Component.text(args.length != 0 ? message.formatted(args) : message)
		).color(NamedTextColor.BLUE);
		Main.getPlayerInfoMap().forEach((player, info) -> {
			if (info.hasPermission(PermissionLevel.MOD)) {
				player.sendMessage(broadcast);
			}
		});
		Main.componentLogger().info(broadcast);
	}

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
					auditEvent(sender, "game ignoreWinConditions %s", true);
					skipGameState(GameState.LIVE);
					auditEvent(sender, "game start");
				} else if (args[1].equals("ignorewinconditions")) {
					ignoreWinConditions = args.length == 3 ? Boolean.parseBoolean(args[2]) : !ignoreWinConditions;
					auditEvent(sender, "game ignoreWinConditions %s", ignoreWinConditions);
					sender.sendMessage(Component.text("Set ignore win conditions to " + ignoreWinConditions, NamedTextColor.GREEN));
				} else if (args[1].equals("snipershowrewind")) {
					sniperShowRewind = args.length == 3 ? Boolean.parseBoolean(args[2]) : !sniperShowRewind;
					auditEvent(sender, "game sniperShowRewind %s", sniperShowRewind);
					sender.sendMessage(Component.text("Set sniper show rewind to " + sniperShowRewind, NamedTextColor.GREEN));
				} else if (args[1].equals("antistall")) {
					doGameAntiStall(sender, args);
				}
			}
			case "draw" -> {
				auditEvent(sender, "miniMapDraw", (Object[]) Arrays.copyOfRange(args, 1, args.length));
				doDrawCommand(sender, args);
			}
			case "votetest" -> CommandCallvote.instance.createVote(null, sender.name(),
					Component.text("Next player to ban?"),
					new CommandCallvote.TopicOptions(true, null, CommandCallvote.LE_FUNNY_VOTE,
							CommandCallvote.VoteOption.getOptions(
									new CommandCallvote.VoteOption("zelda", TextUtils.getUselessRainbowText("toomuchzelda"), TextUtils.getUselessRainbowText("toomuchzelda: ")),
									new CommandCallvote.VoteOption("toed", Component.text("T_0_E_D", NamedTextColor.DARK_GREEN)),
									new CommandCallvote.VoteOption("onett", Component.text("Onett_", NamedTextColor.BLUE))
							), true));
			case "respawn" -> {
				var game = Main.getGame();
				var toRespawn = selectPlayersOrThrow(sender, args, 1);
				toRespawn.forEach(game::respawnPlayer);
				auditEvent(sender, "game respawn %s", namePlayers(toRespawn));
			}
			case "setrank" -> {
				if (args.length < 2)
					throw throwUsage("/debug setrank <rank> [player]");

				PermissionLevel level = PermissionLevel.valueOf(args[1]);
				Player target = getPlayerOrThrow(sender, args, 2);
				PlayerInfo info = Main.getPlayerInfo(target);
				info.permissionLevel = level;
				auditEvent(sender, "game setRank %s %s", target.getName(), level.name());
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
				auditEvent(sender, "game setTeam %s %s", namePlayers(targetPlayers), targetTeam.getName());

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
				auditEvent(sender, "game setKit %s %s", namePlayers(targetPlayers), kit.getName());
				for (var target : targetPlayers) {
					PlayerInfo info = Main.getPlayerInfo(target);

					if (info.activeKit != null) {
						info.activeKit.removeKit(target, info);
					}
					info.kit = kit;
					if (game.getGameState() == GameState.LIVE) {
						game.giveKitAndGameItems(target, info, true);
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
				auditEvent(sender, "game explode %d %d %d %d %d", (Object[]) Arrays.copyOfRange(args, 1, args.length));
			}
			case "fakehitbox" -> {
				boolean show = args.length == 2 ? Boolean.parseBoolean(args[1]) : !FakeHitboxManager.show;
				auditEvent(sender, "game setFakeHitboxVisibility %s", show);
				FakeHitboxManager.setVisibility(show);
			}
			case "testmotd" -> {
				var fakeEvent = new PaperServerListPingEvent(new StatusClient() {
					@Override
					public @NotNull InetSocketAddress getAddress() {
						return InetSocketAddress.createUnresolved("127.0.0.1", 25565);
					}

					@Override
					public int getProtocolVersion() {
						return 0;
					}

					@Override
					public @Nullable InetSocketAddress getVirtualHost() {
						return null;
					}
				}, Component.empty(), 0, 0, "1.1x", -1, null);
				ServerListPingManager.handleEvent(fakeEvent);
				sender.sendMessage(fakeEvent.motd());
			}
			case "showSpawns" -> {
				TeamArena game = Main.getGame();
				for (var entry : game.gameMap.getTeamSpawns().entrySet()) {
					int ctr = 0;
					for (Vector spawnLoc : entry.getValue()) {
						new PacketHologram(spawnLoc.toLocation(game.getWorld()), null, PacketEntity.VISIBLE_TO_ALL,
							Component.text(entry.getKey() + ctr++)).respawn();
					}
				}
			}
			case "darken" -> {
				if (args.length >= 2) {
					try {
						Component text = MiniMessage.miniMessage().deserialize(args[1]);
						sender.sendMessage(text);

						sender.sendMessage(TextUtils.darken(text));
					} catch (ParsingException ex) {
						throw new CommandException("Unsafe MiniMessage input: " + args[1], ex);
					}
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	private void doGameAntiStall(CommandSender sender, String[] args) {
		var game = Main.getGame();
		if (args.length < 3) {
			// info
			sender.sendMessage(Component.textOfChildren(
				Component.text("Anti-stall info:\ngameTick: " + TeamArena.getGameTick() + "\n", NamedTextColor.GOLD),
				Component.text(game.getDebugAntiStall())
			));
		} else {
			int antiStallCountdown = Integer.parseInt(args[2]);
			game.setDebugAntiStall(antiStallCountdown);
			sender.sendMessage(Component.text("Set anti-stall countdown to " + antiStallCountdown));
		}
	}

	private void doDrawCommand(CommandSender sender, String[] args) {
		if (args.length < 2)
			throw throwUsage("/debug draw clear/disable/invalidate");
		if ("clear".equalsIgnoreCase(args[1])) {
			canvasOperations.clear();
		} else if ("invalidate".equalsIgnoreCase(args[1])) {
			((MiniMapManager.GameMapRenderer) Main.getGame().miniMap.view.getRenderers().get(0)).hasDrawn = false;
		} else if ("disable".equalsIgnoreCase(args[1])) {
			disableMiniMapInCaseSomethingTerribleHappens = !disableMiniMapInCaseSomethingTerribleHappens;
			sender.sendMessage(Component.text("Minimap is now " + (disableMiniMapInCaseSomethingTerribleHappens ? "disabled" : "enabled")));
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
			case "movemaxxing" -> {
				new BukkitRunnable() {
					int ticks = 0;
					@Override
					public void run() {
						if (!player.isOnline())
							cancel();
						Vector vector;
						if (ticks < 10)
							vector = new Vector(0.4, 0, 0);
						else if (ticks < 20)
							vector = new Vector(0, 0, 0.4);
						else if (ticks < 30)
							vector = new Vector(-0.4, 0, 0);
						else
							vector = new Vector(0, 0, -0.4);
						ticks = (ticks + 1) % 40;
						vector.setY(player.getVelocity().getY());
						player.setVelocity(vector);
					}
				}.runTaskTimer(Main.getPlugin(), 0, 1);
			}
			case "guitest" -> {
				if (args.length < 2)
					throw throwUsage("/debug guitest <tab/spectate>");
				var inventory = switch (args[1]) {
					case "tab" -> new TabTest();
					case "spectate" -> new SpectateInventory(null, false);
					default -> throw throwUsage("/debug guitest <tab/spectate>");
				};
				Inventories.openInventory(player, inventory);
			}
			case "signtest" -> {
				var message = args.length < 2 ?
					Component.text("signtest") :
					Component.text(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
				Inventories.openSign(player, message, message.content())
					.thenAccept(input -> player.sendMessage(Component.text("Got input: " + input)));
			}
			case "hide" -> {
				auditEvent(player, "hideSelf");
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
							this.playEffect(ClientboundAnimatePacket.CRITICAL_HIT);
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
			case "testyaw" -> {
				if (args.length < 2)
					throw throwUsage("testyaw float");

				float yaw = Float.parseFloat(args[1]);
				ClientboundHurtAnimationPacket hurtAnimationPacket = new ClientboundHurtAnimationPacket(player.getEntityId(), yaw);
				PlayerUtils.sendPacket(player, hurtAnimationPacket);
			}
			case "chunktracker" -> {
				player.sendMessage(LoadedChunkTracker.getStatus());

				if (args.length >= 2) {
					if (args[1].equalsIgnoreCase("refresh")) {
						LoadedChunkTracker.cleanup();
						Bukkit.broadcastMessage("refreshed");
					}
					else {
						throw throwUsage("chunktracker refresh");
					}
				}
			}
			case "villager" -> {
				Villager villager = player.getWorld().spawn(player.getLocation(), Villager.class);
				villager.setMaxHealth(999);
				villager.setAI(false);
				villager.setGlowing(true);
				Main.getPlayerInfo(player).team.addMembers(villager);
			}
			case "arrowMarker" -> {
				ArrowManager.spawnArrowMarkers = !ArrowManager.spawnArrowMarkers;
			}
			case "packetcache" -> {
				if (args.length < 2)
					throw throwUsage("packetCache boolean");

				boolean bool = Boolean.parseBoolean(args[1]);
				PacketEntityManager.toggleCache(bool);
				sender.sendMessage(Component.text("Toggle cache to " + bool));
			}
			case "flyingpoint" -> {
				if (args.length >= 2 && "toggle".equalsIgnoreCase(args[1])) {
					PacketFlyingPoint.VISIBLE = !PacketFlyingPoint.VISIBLE;
				}
				else {
					for (int i = 0; i < 10; i++) {
						new PacketFlyingPoint(player, 5d, 5d,
							MathUtils.randomRange(0, 0.5d), MathUtils.randomRange(0.85, 1.0),
							MathUtils.randomRange(0.2, 1),
							Math.max(1, MathUtils.randomMax(5 * 20))
						).respawn();
					}
				}
			}
			case "fakeBlock" -> {
				if (args.length < 3)
					throw throwUsage("fakeBlock add/remove MATERIAL/KEY");

				if (args[1].equalsIgnoreCase("add")) {

					Block targetBlock = player.getTargetBlock(null, 5);
					Material desiredMat = Material.valueOf(args[2]);
					FakeBlockManager fbManager = Main.getGame().getFakeBlockManager();

					long key = fbManager.setFakeBlock(new BlockCoords(targetBlock),
						((CraftBlockData) desiredMat.createBlockData()).getState(), viewer -> viewer == player);

					Bukkit.broadcastMessage("Key is " + key);
				}
				else if (args[1].equalsIgnoreCase("remove")) {

					Block targetBlock = player.getTargetBlock(null, 5);
					long key = Long.parseLong(args[2]);
					FakeBlockManager fbManager = Main.getGame().getFakeBlockManager();

					if(fbManager.removeFakeBlock(new BlockCoords(targetBlock), key))
						player.sendMessage("Successfully removed");
					else
						player.sendMessage("Did not remove anything");
				}
			}
			case "elevator" -> {
				final Location loc = player.getLocation();

				List<PacketDisplay> displays = new ArrayList<>();
				BlockState nmsBlockState = ((CraftBlockData) Material.OAK_PLANKS.createBlockData()).getState();
				for (int x = 0; x < 4; x++) {
					for (int y = 0; y < 4; y++) {
						for (int z = 0; z < 4; z++) {
							if ((x == 1 || x == 2) && (y == 1 || y == 2 || y == 3) && (z == 1 || z == 2)) {
								continue;
							}

							PacketDisplay pdisplay = new PacketDisplay(PacketEntity.NEW_ID, EntityType.BLOCK_DISPLAY,
								loc.clone().add(x, y, z), null, viewer -> true);

							pdisplay.setMetadata(MetaIndex.DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ, 2);
							pdisplay.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, nmsBlockState);
							pdisplay.updateMetadataPacket();
							pdisplay.respawn();
							displays.add(pdisplay);
						}
					}
				}

				final PacketEntity pig = new PacketEntity(PacketEntity.NEW_ID, EntityType.PIG, loc.clone().add(1.5, 1.5, 1.5),
					null, viewer -> true);
				pig.respawn();

				Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), () -> {
					final double thing = ((double) System.currentTimeMillis()) / 2000d;
					final double y = Math.sin(thing) * 10d;
					for (PacketDisplay display : displays) {
						Location xz = display.getLocation();
						xz.setY(loc.getY() + y);
						display.move(xz);
					}

					pig.move(loc.clone().add(0, y, 0));
				}, 1L, 1L);

			}
			case "showores" -> {
				if (Main.getGame() instanceof DigAndBuild dnb) {
					Map<StatusOreType, TeamArenaMap.DNBStatusOreInfo> map = dnb.gameMap.getDnbInfo().statusOres();
					for (var entry : map.entrySet()) {
						String text = entry.getKey().name();
						for (BlockCoords coords : entry.getValue().coords()) {
							new PacketHologram(coords.toLocation(dnb.getWorld()).add(0.5, 0.5, 0.5), null, viewer -> true, Component.text(text)).respawn();
						}
					}
				}
			}
			case "amogus" -> PayloadTestKillstreak.playAmogus(player.getWorld(), player);
			case "loadsong" -> {
				if (args.length == 1)
					throw throwUsage("/debug loadsong <songPath...>");
				String songPath = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				if (!songPath.endsWith(".nbs"))
					throw new CommandException("File name must end in .nbs");
				Path songFolder = Paths.get("songs").toAbsolutePath();
				Path filePath = songFolder.resolve(songPath).toAbsolutePath();
				if (!filePath.startsWith(songFolder))
					throw new CommandException("real");
				try (var is = Files.newInputStream(filePath)) {
					var song = PayloadTestKillstreak.loadSong(is);
					new PayloadTestKillstreak.NbsSongPlayer(song, player).schedule();
					Duration duration = Duration.ofSeconds(Math.round(song.length()));
					player.sendMessage(Component.text("Loaded \"" + songPath + "\" (" +
						duration.toMinutesPart() + ":" + duration.toSecondsPart() +
						")"));
				} catch (IOException e) {
					throw new CommandException("Failed to load song: " + e.getMessage(), e);
				}
			}
			case "packethuman" -> {
				Herobrine h = new Herobrine(Main.getGame(), player.getTargetBlockExact(10), player.getLocation(), "fakehuman");
				h.spawn();
			}
			default -> showUsage(sender);
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return Arrays.asList("hide", "gui", "guitest", "signtest", "game", "setrank", "setteam", "setkit",
				"votetest", "draw", "graffititest", "respawn", "fakehitbox", "testmotd", "arrowMarker", "packetcache", "showSpawns",
				"flyingpoint", "fakeBlock", "elevator", "showores", "darken", "amogus", "loadsong", "movemaxxing", "packethuman");
		} else if (args.length == 2) {
			return switch (args[0].toLowerCase(Locale.ENGLISH)) {
				case "gui" -> Arrays.asList("true", "false");
				case "guitest" -> Arrays.asList("tab", "spectate");
				case "game" -> List.of("start", "ignorewinconditions", "antistall");
				case "setrank" -> Arrays.stream(PermissionLevel.values()).map(Enum::name).toList();
				case "setteam" -> Arrays.stream(Main.getGame().getTeams())
						.map(team -> team.getSimpleName().replace(' ', '_'))
						.toList();
				case "setkit" -> Main.getGame().getTabKitList();
				case "draw" -> Arrays.asList("text", "area", "clear", "invalidate", "disable");
				case "graffititest" -> CosmeticsManager.getLoadedCosmetics(CosmeticType.GRAFFITI).stream().map(NamespacedKey::toString).toList();
				case "respawn" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
				case "packetcache" -> List.of("true", "false");
				case "flyingpoint" -> List.of("toggle");
				default -> Collections.emptyList();
			};
		} else if (args.length == 3) {
			return switch (args[0].toLowerCase(Locale.ENGLISH)) {
				case "setrank", "setteam", "setkit" -> Bukkit.getOnlinePlayers().stream()
						.map(Player::getName).toList();
				case "game" -> switch (args[1]) {
					case "start" -> Collections.emptyList();
					case "antistall" -> List.of("0");
					default -> List.of("true", "false");
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

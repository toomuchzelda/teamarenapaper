package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.InventoryProvider;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.inventory.TicTacToe;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class CommandTicTacToe extends CustomCommand {

	public static final List<String> BOTS = Arrays.stream(TicTacToe.StandardBots.values())
		.map(Enum::name)
		.map(s -> s.toLowerCase(Locale.ENGLISH))
		.toList();

	public CommandTicTacToe() {
		super("tictactoe", "Better than team arena", "/tictactoe <player>/bot [difficulty]", PermissionLevel.ALL, "ttt", "iambored");
	}

	record Invitation(UUID uuid, long timestamp, BukkitTask task) {
	}

	HashMap<UUID, Invitation> requests = new HashMap<>();

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(PLAYER_ONLY);
			return;
		}
		if (args.length == 0) {
			Inventories.openInventory(player, new TargetSelector());
			return;
		}
		String target = args[0];
		if ("bot".equalsIgnoreCase(target)) {
			TicTacToe.StandardBots difficulty = TicTacToe.StandardBots.EASY;
			if (args.length == 2) {
				difficulty = TicTacToe.StandardBots.valueOf(args[1].toUpperCase(Locale.ENGLISH));
			}
			TicTacToe game = new TicTacToe(TicTacToe.getPlayer(player), TicTacToe.getBot(difficulty));
			game.schedule();
		} else if ("botfirst".equalsIgnoreCase(target)) {
			TicTacToe.StandardBots difficulty = TicTacToe.StandardBots.EASY;
			if (args.length == 2) {
				difficulty = TicTacToe.StandardBots.valueOf(args[1].toUpperCase(Locale.ENGLISH));
			}
			TicTacToe game = new TicTacToe(TicTacToe.getBot(difficulty), TicTacToe.getPlayer(player));
			game.schedule();
		} else if ("bvb".equalsIgnoreCase(target)) {
			if (args.length != 3)
				throw throwUsage("/tictactoe bvb <bot1> <bot2>");
			TicTacToe.StandardBots bot1 = TicTacToe.StandardBots.valueOf(args[1].toUpperCase(Locale.ENGLISH));
			TicTacToe.StandardBots bot2 = TicTacToe.StandardBots.valueOf(args[2].toUpperCase(Locale.ENGLISH));
			TicTacToe game = new TicTacToe(bot1, bot2);
			TicTacToe.Inventory inventory = new TicTacToe.Inventory();
			inventory.initGame(game, null);
			Inventories.openInventory(player, inventory);
			game.schedule();
		} else {
			sendInvitation(player, getPlayerOrThrow(sender, args, 0));
		}
	}

	private void sendInvitation(Player player, Player targetPlayer) {
		if (player.equals(targetPlayer)) {
			player.sendMessage(Component.text("Player not found!", TextColors.ERROR_RED));
			return;
		}
		long now = System.currentTimeMillis();
		// check for incoming invitation
		Invitation invitation = requests.remove(targetPlayer.getUniqueId());
		if (invitation != null && now - invitation.timestamp() <= 60000 && invitation.uuid().equals(player.getUniqueId())) {
			var player1 = TicTacToe.getPlayer(targetPlayer);
			var player2 = TicTacToe.getPlayer(player);
			TicTacToe game = new TicTacToe(player1, player2);
			player1.getInventory().initGame(game, TicTacToe.State.CIRCLE);
			player2.getInventory().initGame(game, TicTacToe.State.CROSS);
			game.schedule();
			if (!invitation.task.isCancelled())
				invitation.task().cancel();
			return;
		}

		invitation = requests.get(player.getUniqueId());
		if (invitation != null && now - invitation.timestamp() <= 60000) {
			player.sendMessage(Component.text("You already have a pending invitation!", NamedTextColor.RED));
			return;
		}

		player.sendMessage(Component.text("Invitation sent to " + targetPlayer.getName() + ".", NamedTextColor.GREEN));
		targetPlayer.sendMessage(Component.text()
			.content(player.getName() + " invited you to a game of tic tac toe! Click here to accept.")
			.color(NamedTextColor.YELLOW)
			.clickEvent(ClickEvent.runCommand("/tictactoe " + player.getName()))
			.hoverEvent(Component.text("/tictactoe " + player.getName(), NamedTextColor.WHITE))
		);
		UUID senderUuid = player.getUniqueId();
		UUID targetUuid = targetPlayer.getUniqueId();
		BukkitTask task = Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
			// don't capture player objects
			Player sender1 = Bukkit.getPlayer(senderUuid);
			Player target1 = Bukkit.getPlayer(targetUuid);
			if (sender1 == null || target1 == null)
				return;
			sender1.sendMessage(Component.text("The tic tac toe invitation to " + target1.getName() + " has expired.", NamedTextColor.YELLOW));
			target1.sendMessage(Component.text("The tic tac toe invitation from " + sender1.getName() + " has expired.", NamedTextColor.YELLOW));
		}, 60 * 20);
		requests.put(senderUuid, new Invitation(targetUuid, now, task));
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return Stream.concat(Bukkit.getOnlinePlayers().stream().map(Player::getName), Stream.of("bot", "botfirst")).toList();
		}
		if (args.length == 2 && args[0].startsWith("bot")) {
			return BOTS;
		} else if (args[0].equalsIgnoreCase("bvb") && args.length < 4) {
			return BOTS;
		}
		return Collections.emptyList();
	}

	class TargetSelector implements InventoryProvider {
		@Override
		public @NotNull Component getTitle(Player player) {
			return Component.text("Pick an opponent", NamedTextColor.GREEN);
		}

		@Override
		public int getRows() {
			return Math.min(6, 3 + Bukkit.getOnlinePlayers().size() / 9);
		}

		@Override
		public void init(Player player, InventoryAccessor inventory) {
			inventory.fillRow(0, ItemBuilder.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE).hideTooltip().build());
			TicTacToe.StandardBots[] bots = TicTacToe.StandardBots.values();
			for (int i = 0; i < bots.length; i++) {
				TicTacToe.StandardBots bot = bots[i];
				inventory.set(i * 2 + 2, bot.getDisplayItem(),
					e -> new TicTacToe(TicTacToe.getPlayer(player), bot).schedule());
			}
			inventory.fillRow(1, ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).hideTooltip().build());
			var players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
			for (int i = 0, count = Math.min(9 * 4, players.length); i < count; i++) {
				Player other = players[i];
				if (player == other) continue;
				inventory.set(9 * 2 + i, ItemBuilder.of(Material.PLAYER_HEAD)
					.displayName(other.playerListName())
					.meta(SkullMeta.class, meta -> meta.setOwningPlayer(other))
					.toClickableItem(e -> {
						sendInvitation(player, other);
						Inventories.closeInventory(player);
					})
				);
			}
		}
	}
}

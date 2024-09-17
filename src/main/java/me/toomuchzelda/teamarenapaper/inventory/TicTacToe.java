package me.toomuchzelda.teamarenapaper.inventory;

import io.papermc.paper.util.Tick;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class TicTacToe {

	public interface TicTacToeAudience extends Function<TicTacToe, CompletableFuture<Integer>> {
		default ItemStack getDisplayItem() {
			return ItemBuilder.of(Material.STRUCTURE_BLOCK)
				.displayName(Component.text("Unknown player", NamedTextColor.LIGHT_PURPLE))
				.build();
		}
	}

	TicTacToeAudience player1, player2;

	public TicTacToe(TicTacToeAudience circle, TicTacToeAudience cross) {
		this.player1 = circle;
		this.player2 = cross;
	}

	@ApiStatus.Internal
	public static boolean UNIT_TEST = false;

	@ApiStatus.Internal
	public TicTacToe(TicTacToeAudience circle, TicTacToeAudience cross, State[] board) {
		if (!UNIT_TEST) throw new IllegalStateException();
		if (board.length != 9) throw new IllegalArgumentException("board");
		this.player1 = circle;
		this.player2 = cross;
		this.board = board.clone();
	}

	public enum State {
		CIRCLE, CROSS, DRAW;

		public State getOpposite() {
			return this == CIRCLE ? CROSS : CIRCLE;
		}
	}

	public State[] board = new State[9];
	public State currentPlayer = State.CIRCLE;
	public State winner = null;
	public int[] winnerSlots = null;
	public int moves = 0;

	public State checkWinner() {
		for (int i = 0; i < 3; i++) {
			// horizontals
			if (board[i * 3] != null && board[i * 3] == board[i * 3 + 1] && board[i * 3] == board[i * 3 + 2]) {
				winner = board[i * 3];
				winnerSlots = new int[]{i * 3, i * 3 + 1, i * 3 + 2};
			}
			// verticals
			if (board[i] != null && board[i] == board[i + 3] && board[i] == board[i + 6]) {
				winner = board[i];
				winnerSlots = new int[]{i, i + 3, i + 6};
			}
		}
		// diagonals
		if (board[0] != null && board[0] == board[4] && board[0] == board[8]) {
			winner = board[0];
			winnerSlots = new int[]{0, 4, 8};
		} else if (board[2] != null && board[2] == board[4] && board[2] == board[6]) {
			winner = board[2];
			winnerSlots = new int[]{2, 4, 6};
		}

		if (winner == null) {
			// check for any empty slot
			for (int i = 0; i < 9; i++) {
				if (board[i] == null)
					return null;
			}
			winner = State.DRAW;
		}
		return winner;
	}

	public void run() {
		// check for wins first
		if (checkWinner() != null)
			return;

		CompletableFuture<Integer> action = switch (currentPlayer) {
			case CIRCLE -> player1.apply(this);
			case CROSS -> player2.apply(this);
			case DRAW -> throw new IllegalStateException("Draw");
		};
		action.thenAccept(num -> {
			board[num] = currentPlayer;
			moves++;
			currentPlayer = currentPlayer.getOpposite();
			Bukkit.getScheduler().runTask(Main.getPlugin(), this::run);
		});
	}

	public void schedule() {
		Bukkit.getScheduler().runTask(Main.getPlugin(), this::run);
	}

	private static CompletableFuture<Integer> delay(Integer response, int delay) {
		if (response == null) return null;
		if (UNIT_TEST) return CompletableFuture.completedFuture(response);
		CompletableFuture<Integer> ret = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> ret.complete(response), delay);
		return ret;
	}

	private static CompletableFuture<Integer> delay(Integer response) {
		return delay(response, 15 + MathUtils.random.nextInt(15));
	}

	private static Integer getRandomNumber(TicTacToe ticTacToe) {
		while (true) {
			int num = MathUtils.randomMax(8);
			if (ticTacToe.board[num] == null) {
				return num;
			}
		}
	}

	private static int checkPairs(State[] board, State player, int pos1, int pos2, int pos3) {
		if (board[pos1] == player && board[pos1] == board[pos2] && board[pos3] == null) {
			// XX_
			return pos3;
		} else if (board[pos2] == player && board[pos2] == board[pos3] && board[pos1] == null) {
			// _XX
			return pos1;
		} else if (board[pos3] == player && board[pos3] == board[pos1] && board[pos2] == null) {
			// X_X
			return pos2;
		}
		return -1;
	}

	private static Integer pickRandomAvailable(TicTacToe ticTacToe, Integer... slots) {
		List<Integer> target = Arrays.asList(slots);
		Collections.shuffle(target);
		for (int i : target) {
			if (ticTacToe.board[i] == null)
				return i;
		}
		return null;
	}

	private static Integer getWinningMove(TicTacToe ticTacToe, State player) {
		State[] board = ticTacToe.board;
		int response;
		for (int i = 0; i < 3; i++) {
			// horizontals
			response = checkPairs(board, player, i * 3, i * 3 + 1, i * 3 + 2);
			if (response != -1) {
				return response;
			}
			// verticals
			response = checkPairs(board, player, i, i + 3, i + 6);
			if (response != -1) {
				return response;
			}
		}
		// diagonals
		response = checkPairs(board, player, 0, 4, 8);
		if (response != -1) {
			return response;
		}
		response = checkPairs(board, player, 2, 4, 6);
		if (response != -1) {
			return response;
		}

		if (ticTacToe.currentPlayer != player) {
			// check for extra advantageous positions
			// since the function returns the empty slot, we can just check for that
			response = checkPairs(board, player, 1, 3, 5);
			if (response == 3 && board[2] == null) {
				// .X!
				// ..X
				return 2;
			} else if (response == 5 && board[0] == null) {
				// !X.
				// X..
				return 0;
			}
			response = checkPairs(board, player, 7, 3, 5);
			if (response == 3 && board[8] == null) {
				// ..X
				// .X!
				return 8;
			} else if (response == 5 && board[6] == null) {
				// X..
				// !X.
				return 6;
			}
		}
		return null;
	}

	private static Integer getWinningMove(TicTacToe ticTacToe) {
		// prioritize winning in 1 move, then try preventing the enemy from winning
		Integer future;
		future = getWinningMove(ticTacToe, ticTacToe.currentPlayer);
		if (future != null)
			return future;
		future = getWinningMove(ticTacToe, ticTacToe.currentPlayer.getOpposite());
		if (future != null)
			return future;

		// no winning move, just do something random
		// but slots 1, 3, 5, 7 are prioritized
		future = pickRandomAvailable(ticTacToe, 1, 3, 5, 7);
		if (future != null)
			return future;
		return getRandomNumber(ticTacToe);
	}

	private static int rotate(int idx, int times) {
		if (idx == 4) return 4;
		int result = idx;
		for (int i = 0; i < times; i++) {
			// 012
			// 345
			// 678
			result = switch (result) {
				case 0 -> 2;
				case 1 -> 5;
				case 2 -> 8;
				case 3 -> 1;
				case 5 -> 7;
				case 6 -> 0;
				case 7 -> 3;
				case 8 -> 6;
				default -> throw new IllegalStateException(String.valueOf(result));
			};
		}
		return result;
	}

	private static Integer getPreemptiveMove(TicTacToe ticTacToe) {
		State enemy = ticTacToe.currentPlayer.getOpposite();
		State[] board = ticTacToe.board;
		if (ticTacToe.moves == 3) {
			// O..
			// .X.
			// .*O: block the enemy from getting a third corner
			if (board[0] == enemy && board[8] == enemy || board[2] == enemy && board[6] == enemy) {
				return pickRandomAvailable(ticTacToe, 1, 3, 5, 7);
			}
			// O..
			// .X.
			// *O.: block the enemy from getting an advantageous corner
			for (int rot = 0; rot <= 3; rot++) {
				// cases for cells adjacent to 0 (1, 3) are already handled by getWinningMove
				if (board[rotate(0, rot)] == enemy) {
					if (board[rotate(5, rot)] == enemy) {
						if (board[rotate(2, rot)] == null)
							return rotate(2, rot);
						else if (board[rotate(1, rot)] == null)
							return rotate(1, rot);
					}
					if (board[rotate(7, rot)] == enemy) {
						if (board[rotate(6, rot)] == null)
							return rotate(6, rot);
						else if (board[rotate(3, rot)] == null)
							return rotate(3, rot);
					}
				}
			}
		}
		return null;
	}

	public enum StandardBots implements TicTacToeAudience {
		EASY(
			// https://minecraft-heads.com/custom-heads/head/84905-robot
			() -> ItemBuilder.from(ItemUtils.createPlayerHead("4a794f8f82d46191181ebf8e2ee22854d04ec9bfb0711682a7603535305a98dc"))
				.displayName(Component.text("Easy Bot", NamedTextColor.GREEN))
				.build(),
			ticTacToe -> delay(getRandomNumber(ticTacToe))
		),
		MEDIUM(
			// https://minecraft-heads.com/custom-heads/head/84904-robot
			() -> ItemBuilder.from(ItemUtils.createPlayerHead("23dbd0224bd1dc73e60815ada74103052b91399caa6d8a83220e4d1574d0dafe"))
				.displayName(Component.text("Medium Bot", NamedTextColor.YELLOW))
				.build(),
			ticTacToe -> {
				Integer move = getWinningMove(ticTacToe, ticTacToe.currentPlayer);
				if (move != null)
					return delay(move);
				move = getWinningMove(ticTacToe, ticTacToe.currentPlayer.getOpposite());
				if (move != null)
					return delay(move);
				return delay(getRandomNumber(ticTacToe));
			}
		),
		IMPOSSIBLE(
			// https://minecraft-heads.com/custom-heads/head/84906-robot
			() -> ItemBuilder.from(ItemUtils.createPlayerHead("68bf39b376b71432d0147da37f379a4e38bef7ad479bbc7b8a30b6407e67a3c9"))
				.displayName(Component.text("IMPOSSIBLE Bot", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
				.build(),
			ticTacToe -> {
				State[] board = ticTacToe.board;
				// special moves
				return delay(switch (ticTacToe.moves) {
					case 0 -> // take one of the corners
						pickRandomAvailable(ticTacToe, 0, 2, 6, 8);
					case 1 -> // take middle or one of the corners
						board[4] == null ?
							4 :
							Objects.requireNonNull(pickRandomAvailable(ticTacToe, 0, 2, 6, 8));
					default -> {
						Integer ourWinningMove = getWinningMove(ticTacToe, ticTacToe.currentPlayer);
						if (ourWinningMove != null)
							yield ourWinningMove;
						Integer theirWinningMove = getWinningMove(ticTacToe, ticTacToe.currentPlayer.getOpposite());
						if (theirWinningMove != null)
							yield theirWinningMove;
						// additionally, force a draw by predicting an enemy's move
						Integer preemptiveMove = getPreemptiveMove(ticTacToe);
						if (preemptiveMove != null)
							yield preemptiveMove;
						// try occupying two other advantageous corners (0, 2, 6, 8)
						// by checking if the cells between them (1, 3, 5, 7) is empty
						boolean c1 = board[1] == null, c3 = board[3] == null, c5 = board[5] == null, c7 = board[7] == null;
						if (c1) {
							if (c3 && board[0] == null)
								yield 0;
							if (c5 && board[2] == null)
								yield 2;
						}
						if (c3 && c7 && board[6] == null)
							yield 6;
						if (c5 && c7 && board[8] == null)
							yield 8;
						yield getWinningMove(ticTacToe);
					}
				});
			}
		);

		private final Supplier<ItemStack> display;
		private final TicTacToeAudience behavior;

		StandardBots(Supplier<ItemStack> display, TicTacToeAudience behavior) {
			this.display = display;
			this.behavior = behavior;
		}

		@Override
		public ItemStack getDisplayItem() {
			return display.get();
		}

		@Override
		public CompletableFuture<Integer> apply(TicTacToe ticTacToe) {
			return behavior.apply(ticTacToe);
		}
	}

	public static TicTacToeAudience getBot(StandardBots difficulty) {
		return difficulty;
	}

	public static TicTacToeAudience getPlayer(Player player) {
		Inventory inventory = new Inventory();
		Inventories.openInventory(player, inventory);
		return new TicTacToeAudience() {
			@Override
			public ItemStack getDisplayItem() {
				return ItemBuilder.of(Material.PLAYER_HEAD)
					.displayName(player.playerListName())
					.meta(SkullMeta.class, meta -> meta.setOwningPlayer(player))
					.build();
			}

			@Override
			public CompletableFuture<Integer> apply(TicTacToe ticTacToe) {
				if (inventory.game == null) {
					inventory.initGame(ticTacToe, ticTacToe.currentPlayer);
				}
				var future = new CompletableFuture<Integer>();
				inventory.enableMove(future);
				return future;
			}
		};
	}

	public static class Inventory implements InventoryProvider {
		TicTacToe game;
		State self;
		CompletableFuture<Integer> future;
		private InventoryAccessor inventory;

		boolean pendingClose = false;
		int activePlayerAnimation = 0;
		int lastAnimationSlot = -1;
		int winAnimation = 0;
		int player1Time = 0;
		int player2Time = 0;


		@Override
		public @NotNull Component getTitle(Player player) {
			return Component.text("Tic Tac Toe", NamedTextColor.GOLD);
		}

		@Override
		public int getRows() {
			return 3;
		}

		@Override
		public void init(Player player, InventoryAccessor inventory) {
			update(player, inventory);
			this.inventory = inventory;
			drawPlayers();
		}

		public void initGame(TicTacToe ticTacToe, @Nullable State self) {
			this.game = ticTacToe;
			this.self = self;
			drawPlayers();
		}

		private void drawPlayers() {
			if (game != null && inventory != null) {
				inventory.set(1, 1, game.player1.getDisplayItem());
				inventory.set(1, 7, game.player2.getDisplayItem());
				ItemStack initialTime = formatTimeTaken(0);
				inventory.set(2, 1, initialTime);
				inventory.set(2, 7, initialTime);
			}
		}

		// allows the user to click on cells, must be called every round on the player's turn
		public void enableMove(CompletableFuture<Integer> handler) {
			this.future = handler;
		}

		public static final ItemStack CIRCLE_ITEM = ItemBuilder.of(Material.BLUE_CONCRETE)
			.displayName(Component.text("Player 1").color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)).build();
		public static final ItemStack CROSS_ITEM = ItemBuilder.of(Material.RED_CONCRETE)
			.displayName(Component.text("Player 2").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)).build();
		public static final ItemStack EMPTY_ITEM = ItemBuilder.of(Material.WHITE_CONCRETE)
			.displayName(Component.text("...").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)).build();

		@Override
		public boolean close(Player player, InventoryCloseEvent.Reason reason) {
			if (game != null) {
				State winner = game.winner;
				if (self != null) {
					if (winner == null) {
						player.sendMessage(Component.text("You lost! You forfeited the match!", NamedTextColor.RED));
						game.winner = self == State.CIRCLE ? State.CROSS : State.CIRCLE;
					} else if (winner == self) {
						player.sendMessage(Component.text("You won!", NamedTextColor.GREEN));
					} else if (winner == State.DRAW) {
						player.sendMessage(Component.text("Draw!", NamedTextColor.YELLOW));
					} else {
						player.sendMessage(Component.text("You lost!", NamedTextColor.RED));
					}
				} else {
					player.sendMessage(Component.text(
						winner == State.DRAW ? "Draw!" : "Player " + (winner == State.CIRCLE ? "1" : "2") + " won!", NamedTextColor.AQUA));
				}
			}
			return true;
		}

		ItemStack getItem(State player) {
			return player == State.CIRCLE ? CIRCLE_ITEM : CROSS_ITEM;
		}

		private static int cellSlot(int index) {
			return (index / 3) * 9 + 3 + index % 3;
		}

		private static final int[] PLAYER_ANIMATION_SLOTS = {
			18, 9, 0, 1, 2, 11, 20
		};
		private static int playerAnimationSlot(int tick) {
			// fit 20 ticks into 7 slots
			// handle ticks 0 - 19
			int tickPart = tick % 20;
			int seq = tickPart < 9 ? tickPart / 3 :
				tickPart >= 11 ? 4 + (tickPart - 11) / 3 :
					3; // ticks 9 and 10
			// for ticks 20 - 39, traverse the sequence in reverse
			return PLAYER_ANIMATION_SLOTS[tick % 40 >= 20 ? 6 - seq : seq];
		}

		private static ItemStack formatTimeTaken(int ticks) {
			return ItemBuilder.of(Material.CLOCK)
				.displayName(Component.textOfChildren(
					Component.text("⌚ Time taken: ", NamedTextColor.GOLD),
					TextUtils.formatDurationMmSs(Tick.of(ticks))
				))
				.build();
		}

		private static final ItemStack ACTIVE_PLAYER = ItemBuilder.of(Material.ORANGE_STAINED_GLASS_PANE).hideTooltip().build();

		@Override
		public void update(Player player, InventoryAccessor inventory) {
			if (game == null) {
				drawBoard(player, inventory);
				return;
			}

			if (game.winner != null) {
				// schedule ending the game
				if (lastAnimationSlot != -1)
					inventory.set(lastAnimationSlot, (ItemStack) null);
				if (future != null) {
					future = null; // no more interactions!
				}
				if (!pendingClose) {
					pendingClose = true;
					Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> player.closeInventory(), 60);
				}
				if (game.winner == State.DRAW) {
					drawBoard(player, inventory);
					return; // don't animate if draw
				}

				if (game.winnerSlots != null) {
					ItemStack winningMove;
					if (winAnimation++ % 20 < 10) {
						winningMove = getItem(game.winner).clone();
						winningMove.setType(Material.YELLOW_CONCRETE);
					} else {
						winningMove = getItem(game.winner);
					}
					for (int slot : game.winnerSlots) {
						inventory.set(cellSlot(slot), winningMove);
					}
				}
				return;
			}

			drawBoard(player, inventory);
			int animationSlot = playerAnimationSlot(activePlayerAnimation);
			if (game.currentPlayer == State.CIRCLE) {
				inventory.set(2, 1, formatTimeTaken(player1Time++));
			} else {
				inventory.set(2, 7, formatTimeTaken(player2Time++));
				animationSlot += 6; // offset 6 slots to player 2
			}
			if (lastAnimationSlot != -1)
				inventory.set(lastAnimationSlot, (ItemStack) null);
			lastAnimationSlot = animationSlot;
			inventory.set(animationSlot, ACTIVE_PLAYER);
			activePlayerAnimation++;
		}

		public void drawBoard(Player player, InventoryAccessor inventory) {
			for (int i = 0; i < 9; i++) {
				int slot = cellSlot(i);
				State cell = game != null ? game.board[i] : null;
				if (cell == null) {
					final int finalI = i;
					inventory.set(slot, EMPTY_ITEM, e -> {
						if (future != null && !future.isDone()) {
							player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1);
							future.complete(finalI);
						} else {
							player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1, 1);
						}
					});
				} else {
					inventory.set(slot, cell == State.CIRCLE ? CIRCLE_ITEM : CROSS_ITEM,
						e -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1, 1));
				}
			}
		}
	}
}

import com.google.common.collect.Collections2;
import me.toomuchzelda.teamarenapaper.inventory.TicTacToe;
import me.toomuchzelda.teamarenapaper.inventory.TicTacToe.State;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class TicTacToeTest {

	@BeforeAll
	static void ensureUnitTest() {
		TicTacToe.UNIT_TEST = true;
	}

	private static char formatState(State state) {
		return switch (state) {
			case CIRCLE -> 'O';
			case CROSS -> 'X';
			case null, default -> '.';
		};
	}

	private static String formatBoard(TicTacToe.State[] board) {
		var sb = new StringBuilder(11);
		for (int i = 0; i < 3; i++) {
			sb.append(formatState(board[i * 3]))
				.append(formatState(board[i * 3 + 1]))
				.append(formatState(board[i * 3 + 2]));
			if (i != 2)
				sb.append('\n');
		}
		return sb.toString();
	}

	private static void assertWinner(State winner, String board) {
		assertWinner(winner, null, board);
	}

	private static void assertWinner(State winner, int[] winningSlots, String board) {
		TicTacToe ticTacToe = new TicTacToe(null, null, buildBoard(board));
		ticTacToe.checkWinner();
		assertEquals(ticTacToe.winner, winner);
		if (winner != null && winner != State.DRAW) {
			assertArrayEquals(ticTacToe.winnerSlots, winningSlots);
		}
	}

	private static final int[] BOARD_INDICES = {0, 1, 2, 4, 5, 6, 8, 9, 10};
	private static State[] buildBoard(String board) {
		State[] arr = new State[9];
		for (int i = 0; i < 9; i++) {
			int idx = BOARD_INDICES[i];
			if (idx >= board.length())
				break;
			char chr = board.charAt(idx);
			arr[i] = chr == 'O' ? State.CIRCLE : chr == 'X' ? State.CROSS : null;
		}
		return arr;
	}

	private static TicTacToe buildGame(char currentPlayer, String board) {
		State[] arr = buildBoard(board);
		int moves = 0;
		for (int i = 0; i < 9; i++) {
			if (arr[i] != null)
				moves++;
		}
		TicTacToe ticTacToe = new TicTacToe(null, null, arr);
		ticTacToe.moves = moves;
		ticTacToe.currentPlayer = currentPlayer == 'O' ? State.CIRCLE : State.CROSS;
		return ticTacToe;
	}

	@Test
	void testWinners() {
		assertWinner(null, "XOX");
		assertWinner(State.DRAW, """
			XOO
			OXX
			OXO""");
		assertWinner(null, """
			O O
			XOX
			XOX""");

		assertWinner(State.CROSS, new int[]{0, 1, 2}, "XXX");
		assertWinner(State.CIRCLE, new int[]{3, 4, 5}, "...\nOOO");
		assertWinner(State.CROSS, new int[]{0, 3, 6}, """
			XOO
			XOO
			X..""");
		assertWinner(State.CIRCLE, new int[]{2, 5, 8}, """
			XOO
			XOO
			..O""");
		assertWinner(State.CROSS, new int[]{0, 4, 8}, """
			XOX
			OXO
			.OX""");
		assertWinner(State.CIRCLE, new int[]{2, 4, 6}, """
			XXO
			XOO
			OOX""");
	}

	private static void assertMove(TicTacToe game, TicTacToe.TicTacToeAudience audience, int... allowed) {
		CompletableFuture<Integer> future = audience.apply(game);
		assertTrue(future.isDone(), "Pending future");
		int move = future.join();
		for (int i : allowed) {
			if (move == i)
				return;
		}
		fail("Expected one of: " + Arrays.toString(allowed) + ", got: " + move + "\nBoard:\n" + formatBoard(game.board));
	}

	@Test
	void testNextMoves() {
		TicTacToe.TicTacToeAudience bot = TicTacToe.getBot(TicTacToe.StandardBots.IMPOSSIBLE);

		int[] corners = {0, 2, 6, 8};
		// moves: 0
		// must pick one of the corners if going first
		assertMove(buildGame('X', """
			...
			...
			..."""), bot, corners);
		// moves: 1
		// must pick center if possible
		assertMove(buildGame('O', """
			X..
			...
			..."""), bot, 4);
		assertMove(buildGame('O', """
			...
			.X.
			..."""), bot, corners);
		// moves: 2
		// try to pick corners
		assertMove(buildGame('X', """
			XO.
			...
			..."""), bot, 6, 8);
		assertMove(buildGame('X', """
			...
			.O.
			X.."""), bot, corners);
		assertMove(buildGame('X', """
			...
			O..
			X.."""), bot, 2, 8);
		// moves: 4
		// similar to above, but will also try to win/block the enemy from winning
		assertMove(buildGame('X', """
			..X
			O.O
			X.."""), bot, 4);
		assertMove(buildGame('X', """
			..X
			O.O
			X.."""), bot, 4);
		assertMove(buildGame('X', """
			X..
			O.O
			X.."""), bot, 4);
		assertMove(buildGame('X', """
			X..
			O..
			XO."""), bot, 2);
		// move to block enemy from winning
		assertMove(buildGame('X', """
			OO."""), bot, 2);
		assertMove(buildGame('X', """
			O.O
			.O.
			..."""), bot, 1, 6, 8);
		assertMove(buildGame('X', """
			...
			.O.
			OO."""), bot, 1, 2);
		assertMove(buildGame('X', """
			O..
			.X.
			.O."""), bot, 6);
		// move to win
		assertMove(buildGame('O', """
			OO."""), bot, 2);
		assertMove(buildGame('O', """
			O.O
			.O.
			..."""), bot, 1, 6, 8);
		assertMove(buildGame('O', """
			...
			.O.
			OO."""), bot, 1, 2);
		// move to avoid cornering ourselves
		assertMove(buildGame('X', """
			OX.
			X..
			O.."""), bot, 8);
	}

	private static TicTacToe.TicTacToeAudience buildBot(List<Integer> moves) {
		var deque = new ArrayDeque<>(moves);
		return ticTacToe -> {
			int value = deque.remove();
			while (ticTacToe.board[value] != null)
				value = deque.remove();
			return CompletableFuture.completedFuture(value);
		};
	}

	private static void assertWin(TicTacToe game, TicTacToe.TicTacToeAudience circle, TicTacToe.TicTacToeAudience cross, State winner, boolean allowDraw) {
		TicTacToe.State[] initial = game.board.clone();
		List<State[]> steps = new ArrayList<>();
		while (game.checkWinner() == null) {
			CompletableFuture<Integer> action = switch (game.currentPlayer) {
				case CIRCLE -> circle.apply(game);
				case CROSS -> cross.apply(game);
				case DRAW -> throw new IllegalStateException("Draw");
			};
			action.thenAccept(num -> {
				game.board[num] = game.currentPlayer;
				game.moves++;
				game.currentPlayer = game.currentPlayer.getOpposite();
				steps.add(game.board.clone());
			});
		}
		State actualWinner = game.winner;
		if (actualWinner != winner && !(allowDraw && actualWinner == State.DRAW)) {
			var sb = new StringBuilder();
			sb.append("Initial:\n").append(formatBoard(initial));
			for (int i = 0; i < steps.size(); i++) {
				sb.append("\nStep ").append(i + 1).append(":\n").append(formatBoard(steps.get(i)));
			}
			fail("Expected " + winner + (allowDraw ? " or draw" : "") + ", got: " + actualWinner + "\n" + sb);
		}
	}

	@Test
	public void testImpossibleBot() {
		TicTacToe.TicTacToeAudience bot = TicTacToe.getBot(TicTacToe.StandardBots.IMPOSSIBLE);
		// for battles between two impossible bots, the only allowed outcome is a draw
		for (int i = 0; i < 100; i++) {
			assertWin(buildGame('O', """
				...
				...
				..."""), bot, bot, null, true);
		}
		// the impossible bot must win or draw against all permutations of input
		for (var permutation : Collections2.permutations(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8))) {
			assertWin(new TicTacToe(null, null), buildBot(permutation), bot, State.CROSS, true);
			assertWin(new TicTacToe(null, null), bot, buildBot(permutation), State.CIRCLE, true);
		}
	}
}

package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class TicTacToe {

    public interface TicTacToeAudience extends Function<TicTacToe, CompletableFuture<Integer>> {}

    TicTacToeAudience player1, player2;
    public TicTacToe(TicTacToeAudience circle, TicTacToeAudience cross) {
        this.player1 = circle;
        this.player2 = cross;
    }

    enum State {
        CIRCLE, CROSS, DRAW
    }
    State[] board = new State[9];
    State currentPlayer = State.CIRCLE;
    State winner = null;
    int[] winnerSlots = null;

    void getWinner() {
        for (int i = 0; i < 3; i++) {
            // horizontals
            if (board[i * 3] != null && board[i * 3] == board[i * 3 + 1] && board[i * 3] == board[i * 3 + 2]) {
                winner = board[i * 3];
                winnerSlots = new int[] {i * 3, i * 3 + 1, i * 3 + 2};
            }
            // verticals
            if (board[i] != null && board[i] == board[i + 3] && board[i] == board[i + 6]) {
                winner = board[i];
                winnerSlots = new int[] {i, i + 3, i + 6};
            }
        }
        // diagonals
        if (board[0] != null && board[0] == board[4] && board[0] == board[8]) {
            winner = board[0];
            winnerSlots = new int[] {0, 4, 8};
        } else if (board[2] != null && board[2] == board[4] && board[2] == board[6]) {
            winner = board[2];
            winnerSlots = new int[] {2, 4, 6};
        }

        // check for any empty slot
        for (int i = 0; i < 9; i++) {
            if (board[i] == null)
                return;
        }
        winner = State.DRAW;
    }

    public void run() {
        // check for wins first
        getWinner();
        if (winner != null)
            return;

        CompletableFuture<Integer> action = switch (currentPlayer) {
            case CIRCLE -> player1.apply(this);
            case CROSS -> player2.apply(this);
            case DRAW -> throw new IllegalStateException("Draw");
        };
        action.thenAccept(num -> {
            board[num] = currentPlayer;
            currentPlayer = currentPlayer == State.CIRCLE ? State.CROSS : State.CIRCLE;
            Bukkit.getScheduler().runTask(Main.getPlugin(), this::run);
        });
    }

    public static TicTacToeAudience getBot() {
        return ticTacToe -> {
            while (true) {
                int num = MathUtils.randomMax(8);
                if (ticTacToe.board[num] == null) {
                    CompletableFuture<Integer> ret = new CompletableFuture<>();
                    Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> ret.complete(num), 10);
                    return ret;
                }
            }
        };
    }

    public static TicTacToeAudience getPlayer(Player player) {
        Inventory inventory = new Inventory(null, null, null);
        Inventories.openInventory(player, inventory);
        return ticTacToe -> {
            if (inventory.game == null) {
                inventory.game = ticTacToe;
                inventory.self = ticTacToe.currentPlayer;
            }
            inventory.future = new CompletableFuture<>();
            return inventory.future;
        };
    }

    private static class Inventory implements InventoryProvider {
        TicTacToe game;
        State self;
        CompletableFuture<Integer> future;
        Inventory(TicTacToe game, State self, CompletableFuture<Integer> future) {
            this.game = game;
            this.self = self;
            this.future = future;
        }

        @Override
        public Component getTitle(Player player) {
            return Component.text("Tic Tac Toe").color(NamedTextColor.GOLD);
        }

        @Override
        public int getRows() {
            return 3;
        }

        @Override
        public void init(Player player, InventoryAccessor inventory) {
            update(player, inventory);
        }

        public static final ItemStack CIRCLE_ITEM = ItemBuilder.of(Material.BLUE_CONCRETE)
                .displayName(Component.text("Player 1").color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)).build();
        public static final ItemStack CROSS_ITEM = ItemBuilder.of(Material.RED_CONCRETE)
                .displayName(Component.text("Player 2").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)).build();
        public static final ItemStack EMPTY_ITEM = ItemBuilder.of(Material.WHITE_CONCRETE)
                .displayName(Component.text("...").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)).build();

        @Override
        public void close(Player player) {
            if (game != null) {
                State winner = game.winner;
                if (winner == null) {
                    player.sendMessage(Component.text("You lost! You forfeited the match!").color(NamedTextColor.RED));
                    game.winner = self == State.CIRCLE ? State.CROSS : State.CIRCLE;
                } else if (winner == self) {
                    player.sendMessage(Component.text("You won!").color(NamedTextColor.GREEN));
                } else if (winner == State.DRAW) {
                    player.sendMessage(Component.text("Draw!").color(NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("You lost!").color(NamedTextColor.RED));
                }
            }
        }

        ItemStack getItem(State player) {
            return player == State.CIRCLE ? CIRCLE_ITEM : CROSS_ITEM;
        }

        int getSlot(int index) {
            return (index / 3) * 9 + 3 + index % 3;
        }

        int animation = 0;
        @Override
        public void update(Player player, InventoryAccessor inventory) {
            if (game == null)
                return;

            if (game.winner != null) {
                // schedule ending the game
                if (future != null) {
                    future = null; // no more interactions!
                    Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> player.closeInventory(), 60);
                }
                if (game.winner == State.DRAW) {
                    drawBoard(player, inventory);
                    return; // don't animate if draw
                }

                if (game.winnerSlots != null) {
                    ItemStack winningMove;
                    if (animation++ % 20 < 10) {
                        winningMove = getItem(game.winner).clone();
                        winningMove.setType(Material.YELLOW_CONCRETE);
                    } else {
                        winningMove = getItem(game.winner);
                    }
                    for (int slot : game.winnerSlots) {
                        inventory.set(getSlot(slot), winningMove);
                    }
                }
                return;
            }

            drawBoard(player, inventory);
        }

        public void drawBoard(Player player, InventoryAccessor inventory) {
            for (int i = 0; i < 9; i++) {
                int slot = getSlot(i);
                State cell = game.board[i];
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

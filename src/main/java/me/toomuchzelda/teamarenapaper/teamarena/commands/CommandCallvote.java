package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

public class CommandCallvote extends CustomCommand {

    public record Topic(String owner, @NotNull Component display, @Nullable Consumer<Boolean> resultConsumer) {}

    public final Deque<Topic> queue = new ArrayDeque<>(); // obviously arrays > linked nodes
    public final Map<UUID, Boolean> ballot = new LinkedHashMap<>(Bukkit.getMaxPlayers());
    Topic currentTopic = null;
    int timeLeft;

    public CommandCallvote() {
        super("callvote", "The most important feature", "/callvote ...", Arrays.asList("voteyes", "voteno"),PermissionLevel.ALL);

        Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), this::tick, 0, 20);

    }

    static final Component SEPARATOR_COMPONENT = Component.text("==========")
            .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
    static final Component LINKS_COMPONENT = Component.text()
            .append(Component.text("Click to vote: ").color(NamedTextColor.YELLOW))
            .append(Component.text("[YES]").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/voteyes"))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to vote").append(
                            Component.text("YES").color(NamedTextColor.GREEN))))
            )
            .append(Component.space())
            .append(Component.text("[NO]").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/voteno"))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to vote").append(
                            Component.text("NO").color(NamedTextColor.RED))))
            ).build();
    void tick() {
        if (currentTopic == null) {
            if ((currentTopic = queue.poll()) == null)
                return; // no topic
            timeLeft = 30;

            Bukkit.broadcast(Component.text()
                    .append(Component.text(currentTopic.owner != null ?
                            currentTopic.owner + " has called for a new vote!" :
                            "A new vote has been called!").color(NamedTextColor.YELLOW))
                    .append(Component.newline())
                    .append(Component.text("Topic: ").color(NamedTextColor.GOLD))
                    .append(currentTopic.display)
                    .append(Component.newline())
                    .append(SEPARATOR_COMPONENT)
                    .append(Component.newline())
                    .append(LINKS_COMPONENT)
                    .append(Component.newline())
                    .append(SEPARATOR_COMPONENT)
                    .build());

            // abstinence by default
            Bukkit.getOnlinePlayers().forEach(player -> ballot.put(player.getUniqueId(), null));
        } else {
            if (timeLeft == 30 || timeLeft == 20 || timeLeft == 10 || timeLeft == 5) {
                Bukkit.broadcast(Component.text(timeLeft + " seconds until the vote ends!").color(NamedTextColor.YELLOW));
            } else if (timeLeft == 0) {
                // cock and balls
                LinkedList<Component> yays = new LinkedList<>(), nays = new LinkedList<>(), abstinence = new LinkedList<>();
                for (var entry : ballot.entrySet()) {
                    Component component;
                    OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                    Boolean vote = entry.getValue();
                    if (player.getName() != null) {
                        component = Component.text(player.getName());
                    } else {
                        component = Component.text("???").color(NamedTextColor.GOLD);
                    }
                    if (vote == null) {
                        abstinence.add(component);
                    } else if (vote) {
                        yays.add(component);
                    } else {
                        nays.add(component);
                    }
                }
                boolean passed = yays.size() > nays.size();
                BinaryOperator<Component> componentAccumulator = (comp1, comp2) -> comp1.append(comp2).append(Component.newline());
                TextComponent.Builder message = Component.text()
                        .append(Component.text("The vote has ended!\nTopic: ").color(NamedTextColor.YELLOW))
                        .append(currentTopic.display())
                        .append(Component.newline())
                        .append(Component.text("Yays: %d Nays: %d Abstinence: %d"
                                        .formatted(yays.size(), nays.size(), abstinence.size()))
                                .color(NamedTextColor.YELLOW));
                if (currentTopic.resultConsumer() == null) {
                    message.append(
                            Component.newline(),
                            Component.text("Reminder: This vote has ").color(NamedTextColor.DARK_RED),
                            Component.text("NO").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                            Component.text(" actual effect!").color(NamedTextColor.DARK_RED)
                    );
                }
                message.append(Component.newline())
                        .append(Component.text("Result: ").color(NamedTextColor.YELLOW))
                        .append(passed ?
                                Component.text("PASSED").color(NamedTextColor.GREEN) :
                                Component.text("DID NOT PASS").color(NamedTextColor.RED));
                Bukkit.broadcast(message.build());
                if (currentTopic.resultConsumer() != null) {
                    currentTopic.resultConsumer().accept(passed);
                }
                currentTopic = null;
                ballot.clear();
                return;
            }
            timeLeft--;
        }
    }

    public void createVote(String owner, Component display, Consumer<Boolean> resultConsumer) {
        queue.add(new Topic(owner, display, resultConsumer));
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PLAYER_ONLY);
            return;
        }

        if (args.length == 0 && ("voteyes".equalsIgnoreCase(label) || "voteno".equalsIgnoreCase(label))) {
            if (currentTopic == null) {
                player.sendMessage(Component.text("There is no topic to vote on!").color(NamedTextColor.RED));
                return;
            }
            boolean vote = "voteyes".equalsIgnoreCase(label);
            Boolean oldVote = ballot.put(player.getUniqueId(), vote);
            if (oldVote != null) {
                player.sendMessage(Component.text("You changed your vote to " + (vote ? "YES" : "NO") + ".")
                        .color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("You casted your vote.").color(NamedTextColor.GREEN));
            }
        } else {
            switch (args[0]) {
                case "extend" -> {
                    if (!hasPermission(player, PermissionLevel.MOD)) {
                        player.sendMessage(NO_PERMISSION);
                        return;
                    }
                    if (args.length != 2) {
                        showUsage(player, "/callvote extend <seconds>");
                        return;
                    }
                    int time;
                    try {
                        time = Math.max(Integer.parseInt(args[1]), 0);
                    } catch (NumberFormatException e) {
                        showUsage(player, "/callvote extend <seconds>");
                        return;
                    }
                    Bukkit.broadcast(Component.text(player.getName() + " has extended the voting time by " +
                            time + " seconds!").color(NamedTextColor.YELLOW));
                    timeLeft += time;
                }
                case "myvoteissuperimportant" -> {
                    if (!hasPermission(player, PermissionLevel.OWNER)) {
                        player.chat("I am Kim Jong Un (101% support rating)");
                        player.getWorld().playSound(player, Sound.ENTITY_CAT_AMBIENT, 1, 0);
                        return;
                    }
                    if (args.length < 2) {
                        showUsage(player, "/callvote myvoteissuperimportant <vote> [amount]");
                        return;
                    }
                    boolean vote = "true".equalsIgnoreCase(args[1]);
                    int amount = 10;
                    if (args.length == 3) {
                        try {
                            amount = Integer.parseInt(args[2]);
                        } catch (NumberFormatException e) {
                            player.chat("I (Kim Jong Un) can't express numbers");
                            player.getWorld().playSound(player, Sound.ENTITY_CAT_AMBIENT, 1, 0);
                            return;
                        }
                    }
                    Bukkit.broadcast(Component.text(player.getName() + " has casted " + amount + "x ")
                            .color(NamedTextColor.YELLOW)
                            .append(Component.text(vote ? "YES" : "NO")
                                    .color(vote ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .append(Component.text(" votes!"))
                            .append(Component.text("Some people are, of course, more equal than others.")
                                    .color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)));
                }
                default -> {
                    String topic = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
                    createVote(player.getName(), LegacyComponentSerializer.legacyAmpersand().deserialize(topic), null);
                    player.sendMessage(Component.text("Queued a vote!").color(NamedTextColor.GREEN));
                }
            }
        }
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player))
            return Collections.emptyList();
        if (args.length == 1) {
            return hasPermission(sender, PermissionLevel.MOD) ?
                    Arrays.asList("extend", "myvoteissuperimportant") :
                    Collections.singletonList("myvoteissuperimportant");
        }
        return Collections.emptyList();
    }
}

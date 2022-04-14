package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
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
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * Great for hackusations
 * @author jacky8399
 */
public class CommandCallvote extends CustomCommand {

    public record TopicOptions(@Nullable Consumer<VotingResults> action, @NotNull Component actionMessage, boolean priority) {}
    public record Topic(@NotNull Component owner,
                        @NotNull Component display,
                        @NotNull TopicOptions options,
                        @NotNull ZonedDateTime time) {}
    public record VotingResults(List<Component> yeas, List<Component> nays, List<Component> didNotVote) {

        public boolean passed() {
            return yeas.size() > nays.size();
        }
    }

    public final Queue<Topic> priorityQueue = new ArrayDeque<>();
    public final Queue<Topic> queue = new ArrayDeque<>(); // obviously arrays > linked nodes
    public final Map<UUID, Boolean> ballot = new LinkedHashMap<>(Bukkit.getMaxPlayers());
    Topic currentTopic = null;
    BossBar bar;
    int maxTime = 1, timeLeft;

    public CommandCallvote() {
        super("callvote", "The most important feature", "/callvote ...", Arrays.asList("voteyes", "voteno"),PermissionLevel.ALL);

//        bar = BossBar.bossBar(Component.empty(), 0, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        bar = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);

        Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), this::tick, 0, 20);
    }

    static final Component SEPARATOR_COMPONENT = Component.text("==========", NamedTextColor.GOLD, TextDecoration.BOLD);
    static final Component LINKS_COMPONENT = Component.text()
            .append(Component.text("Click to vote: ", NamedTextColor.YELLOW))
            .append(Component.text("[YES]", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/voteyes"))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to vote").append(
                            Component.text("YES", NamedTextColor.GREEN))))
            )
            .append(Component.space())
            .append(Component.text("[NO]", NamedTextColor.RED, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/voteno"))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to vote").append(
                            Component.text("NO", NamedTextColor.RED))))
            ).build();
    void tick() {
        if (currentTopic == null) {
            if ((currentTopic = priorityQueue.poll()) == null) // poll priority queue first
                // don't start a new vote in game
                if (Main.getGame().getGameState() == GameState.LIVE || (currentTopic = queue.poll()) == null)
                    return; // no topic
            maxTime = 30;
            timeLeft = 30;

            Component topicComponent = Component.textOfChildren(
                    Component.text("Topic: ", NamedTextColor.GOLD, TextDecoration.BOLD), currentTopic.display);

            Bukkit.broadcast(Component.textOfChildren(
                    currentTopic.owner,
                    Component.text(" has called for a new vote!", NamedTextColor.YELLOW),
                    Component.newline(), topicComponent,
                    Component.newline(), SEPARATOR_COMPONENT,
                    Component.newline(), LINKS_COMPONENT,
                    Component.newline(), SEPARATOR_COMPONENT));
            // abstinence by default
            Bukkit.getOnlinePlayers().forEach(player -> ballot.put(player.getUniqueId(), null));
        } else {
            if (timeLeft == 30 || timeLeft == 20 || timeLeft == 10 || timeLeft == 5) {
                // don't spam the console
                var message = Component.text(timeLeft + " seconds until the vote ends!", NamedTextColor.YELLOW);
                Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
            } else if (timeLeft == 0) {
                VotingResults results = tallyResults();
                Bukkit.broadcast(Component.text("The vote has ended!", NamedTextColor.AQUA));
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(getTopicDisplay(player.locale(), results));
                }
                Bukkit.getConsoleSender().sendMessage(getTopicDisplay(null, results));
                if (currentTopic.options().action() != null) {
                    currentTopic.options().action().accept(results);
                }
                currentTopic = null;
                ballot.clear();
                updateBossBar();
                return;
            }
            timeLeft--;
        }
        updateBossBar();
    }

    void updateBossBar() {
        if (currentTopic != null) {
            Component title = Component.textOfChildren(
                    Component.text("Topic: ", NamedTextColor.GOLD, TextDecoration.BOLD),
                    currentTopic.display(),
                    Component.text(" | "),
                    Component.text("Time left: ", NamedTextColor.GREEN),
                    Component.text(timeLeft + "s", NamedTextColor.YELLOW)
            );
            bar.setTitle(LegacyComponentSerializer.legacySection().serialize(title));
            bar.setProgress((float) timeLeft / maxTime);

            Bukkit.getOnlinePlayers().forEach(bar::addPlayer);
        } else {
            bar.removeAll();
        }
    }

    VotingResults tallyResults() {
        LinkedList<Component> yeas = new LinkedList<>(), nays = new LinkedList<>(), didNotVote = new LinkedList<>();
        for (var entry : ballot.entrySet()) {
            Component component;
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            Boolean vote = entry.getValue();
            if (player.getName() != null) {
                component = Component.text(player.getName());
            } else {
                component = Component.text("???", NamedTextColor.GOLD);
            }
            if (vote == null) {
                didNotVote.add(component);
            } else if (vote) {
                yeas.add(component);
            } else {
                nays.add(component);
            }
        }
        return new VotingResults(yeas, nays, didNotVote);
    }

    Component getTopicDisplay(@Nullable Locale locale, @Nullable VotingResults results) {
        if (currentTopic == null)
            return Component.text("There is no ongoing vote.", NamedTextColor.DARK_RED);
        Duration time = Duration.between(currentTopic.time(), ZonedDateTime.now());

        TextComponent.Builder message = Component.text();
        message.append(
                SEPARATOR_COMPONENT,
                Component.newline(),
                Component.text("Topic: ", NamedTextColor.YELLOW),
                currentTopic.display(),
                Component.text("\nSubmitted by ", NamedTextColor.YELLOW),
                currentTopic.owner(),
                Component.space(),
                TextUtils.formatDuration(time, currentTopic.time(), locale),
                Component.text(" ago", NamedTextColor.YELLOW),
                Component.newline(),
                currentTopic.options().actionMessage(),
                Component.newline()
        );
        if (results != null) {
            message.append(Component.text("Yeas: " + results.yeas().size(), NamedTextColor.GREEN),
                    Component.text(" Nays: " + results.nays().size(), NamedTextColor.RED),
                    Component.text(" Hates democracy: " + results.didNotVote().size(), NamedTextColor.YELLOW),
                    Component.newline(),
                    Component.text("Result: ", NamedTextColor.YELLOW),
                    results.passed() ?
                            Component.text("PASSED", NamedTextColor.GREEN) :
                            Component.text("DID NOT PASS", NamedTextColor.RED));
        } else {
            long noOfVotes = ballot.values().stream().filter(Objects::nonNull).count();
            message.append(Component.text("Votes: " + noOfVotes, NamedTextColor.GOLD));
        }
        return message.append(Component.newline(), SEPARATOR_COMPONENT).build();
    }

    static final Component LE_FUNNY_VOTE = Component.textOfChildren(
            Component.text("Reminder: This vote has ", NamedTextColor.DARK_RED),
            Component.text("NO", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" actual effect!", NamedTextColor.DARK_RED));
    public void createVote(Component owner, Component display) {
        createVote(owner, display, new TopicOptions(null, LE_FUNNY_VOTE, false));
    }
    public void createVote(Component owner, Component display, @NotNull TopicOptions options) {
        Topic topic = new Topic(owner, display, options, ZonedDateTime.now());
        if (options.priority())
            priorityQueue.add(topic);
        else
            queue.add(topic);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 && ("voteyes".equalsIgnoreCase(label) || "voteno".equalsIgnoreCase(label))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Computers don't have voting rights!", NamedTextColor.RED));
                return;
            }
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
            if (args.length == 0) {
                showUsage(sender);
                return;
            }
            switch (args[0]) {
                case "info" -> sender.sendMessage(getTopicDisplay(sender instanceof Player player ? player.locale() : null, null));
                case "extend" -> {
                    if (!hasPermission(sender, PermissionLevel.MOD)) {
                        sender.sendMessage(NO_PERMISSION);
                        return;
                    }
                    if (args.length != 2) {
                        showUsage(sender, "/callvote extend <seconds>");
                        return;
                    }
                    int time;
                    try {
                        time = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        showUsage(sender, "/callvote extend <seconds>");
                        return;
                    }
                    Bukkit.broadcast(Component.textOfChildren(sender.name(),
                                    Component.text(" has extended the voting time by " + time + " seconds.")
                            ).color(NamedTextColor.YELLOW));
                    maxTime += time;
                    timeLeft += Math.max(0, time);
                }
                case "myvoteissuperimportant" -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("Computers don't have voting rights!", NamedTextColor.RED));
                        return;
                    }
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
                case "createpriority" -> {
                    if (!hasPermission(sender, PermissionLevel.MOD)) {
                        showUsage(sender);
                        return;
                    }
                    String topic = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    createVote(sender instanceof Player player ? player.teamDisplayName() : sender.name(),
                            LegacyComponentSerializer.legacyAmpersand().deserialize(topic),
                            new TopicOptions(null, LE_FUNNY_VOTE, true));
                    sender.sendMessage(Component.text("Queued a topic!").color(NamedTextColor.GREEN));
                }
                default -> {
                    String topic = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
                    createVote(sender instanceof Player player ? player.teamDisplayName() : sender.name(),
                            LegacyComponentSerializer.legacyAmpersand().deserialize(topic));
                    sender.sendMessage(Component.text("Queued a topic!").color(NamedTextColor.GREEN));
                }
            }
        }
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return hasPermission(sender, PermissionLevel.MOD) ?
                    Arrays.asList("extend", "myvoteissuperimportant", "createpriority", "info") :
                    Arrays.asList("myvoteissuperimportant", "info");
        }
        return Collections.emptyList();
    }
}

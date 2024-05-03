package me.toomuchzelda.teamarenapaper.teamarena.commands;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.GameScheduler;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Great for hackusations
 * @author jacky8399
 */
public class CommandCallvote extends CustomCommand {

	public record TopicOptions(boolean priority,
							   @Nullable Consumer<VotingResults> action,
							   @NotNull Component actionMessage,
							   // <Command string to vote for it, the option>
							   // String must not have spaces.
							   @NotNull Map<String, VoteOption> options,
							   // Added for not spamming chat with map vote result
							   boolean broadcastResult) {
	}
	public record Topic(@Nullable UUID caller,
						@Nullable Component owner,
						@NotNull Component display,
						@NotNull TopicOptions options,
						@NotNull ZonedDateTime time) {}

	public record VoteOption(String id, Component display, Function<Set<UUID>, Component> votesFormatter, Component result) {
		public VoteOption(String id, Component display) {
			this(id, display, results -> display.append(Component.text(": " + results.size())), display);
		}

		public VoteOption(String id, Component display, Component votes) {
			this(id, display, votes, display);
		}

		public VoteOption(String id, Component display, Component votes, Component result) {
			this(id, display, results -> votes.append(Component.text(results.size())), result);
		}

		public static final VoteOption YEA = new VoteOption("yes", Component.text("Yes", NamedTextColor.GREEN),
				Component.text("Yeas: "), Component.text("PASSED", NamedTextColor.DARK_GREEN, TextDecoration.BOLD));
		public static final VoteOption NAY = new VoteOption("no", Component.text("No", NamedTextColor.RED),
				Component.text("Nays: "), Component.text("DID NOT PASS", NamedTextColor.DARK_RED, TextDecoration.BOLD));
		public static final Map<String, VoteOption> DEFAULT_OPTIONS = Map.of("yes", YEA, "no", NAY);

		public static Map<String, VoteOption> getOptions(VoteOption... options) {
			var map = new HashMap<String, VoteOption>();
			for (var option : options) {
				map.put(option.id(), option);
			}
			return map;
		}
	}
	public record VotingResults(Map<VoteOption, Set<UUID>> votes, @Nullable VoteOption result) {
		public VotingResults(Map<VoteOption, Set<UUID>> votes) {
			this(votes, votes.size() == 0 ? null :
				Collections.max(votes.entrySet(),
						Map.Entry.comparingByValue(Comparator.comparingInt(Set::size))).getKey()
			);
		}

		@NotNull
		public Set<UUID> votes(VoteOption option) {
			return votes.getOrDefault(option, Collections.emptySet());
		}
	}

	private static final Component NO_TOPIC = Component.text("There is no topic to vote on! Suggest one with /callvote [topic]").color(NamedTextColor.RED);

	public final Queue<Topic> priorityQueue = new ArrayDeque<>();
	public final Queue<Topic> queue = new ArrayDeque<>(); // obviously arrays > linked lists
	public final Map<UUID, @NotNull VoteOption> ballot = new LinkedHashMap<>(Bukkit.getMaxPlayers());
	public final Set<UUID> calledVotes = new HashSet<>();
	Topic currentTopic = null;
	BossBar bar;
	int maxTime = 1, timeLeft;

	public static CommandCallvote instance;

	public CommandCallvote() {
		super("callvote", "The most important feature", "/callvote ...", PermissionLevel.ALL,
				"vote");

        bar = BossBar.bossBar(Component.empty(), 0, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

		Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), this::tick, 0, 20);
		Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), this::updateBossBar, 0, 1);

		instance = this;
	}

	public boolean isVoteActive() {
		return this.currentTopic != null;
	}

	public enum StartVoteOption {
		MAP, MISC
	}
	public void startVote(StartVoteOption option) {
		assert CompileAsserts.OMIT || Main.getGame().getGameState() != GameState.LIVE;
		if (currentTopic != null) {
			Main.logger().info("CommandCallvote.startVote() called while a vote in progress already. Overriding...");
			this.cancelVote();
		}
		if (option == StartVoteOption.MAP) {
			currentTopic = GameScheduler.getVoteTopic();
		}
		else {
			currentTopic = priorityQueue.poll();
			if (currentTopic == null) {
				currentTopic = queue.poll();
				if (currentTopic == null) {
					return;
				}
			}
		}

		Component topicComponent = Component.textOfChildren(
			Component.text("Topic: ", NamedTextColor.GOLD, TextDecoration.BOLD), currentTopic.display);
		Component instruction;

		if (option == StartVoteOption.MAP) {
			maxTime = 10;
			timeLeft = 10;
			//instruction = Component.text("Vote for the next map:", NamedTextColor.YELLOW);
			instruction = Component.empty();
		}
		else {
			maxTime = 30;
			timeLeft = 30;
			assert CompileAsserts.OMIT || currentTopic.owner != null;
			instruction = currentTopic.owner.append(Component.text(" has called for a new vote!", NamedTextColor.YELLOW));
		}

		Bukkit.broadcast(Component.textOfChildren(
			instruction,
			Component.newline(), topicComponent,
			//Component.newline(), SEPARATOR_COMPONENT,
			Component.newline(), getTopicVoteLinks(),
			Component.newline(), SEPARATOR_COMPONENT));
	}

	static final Component SEPARATOR_COMPONENT = Component.text("==========", NamedTextColor.GOLD, TextDecoration.BOLD);
	void tick() {
		if (currentTopic != null) {
			if (timeLeft == 30 || /*timeLeft == 20 ||*/ timeLeft == 10 /*|| timeLeft == 5*/) {
				// don't spam the console
				var message = Component.text(timeLeft + " seconds until the vote ends!", NamedTextColor.YELLOW);
				Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
			} else if (timeLeft == 0) {
				VotingResults results = tallyResults();
				if (currentTopic.options().broadcastResult()) {
					Bukkit.broadcast(Component.text("The vote has ended!", NamedTextColor.AQUA));
					for (Player player : Bukkit.getOnlinePlayers()) {
						player.sendMessage(getTopicDisplay(currentTopic, player.locale(), results));
					}
				}
				Bukkit.getConsoleSender().sendMessage(getTopicDisplay(currentTopic, null, results));

				if (currentTopic.options().action() != null) {
					currentTopic.options().action().accept(results);
				}
				if (currentTopic.caller != null)
					calledVotes.remove(currentTopic.caller);

				currentTopic = null;
				ballot.clear();
				return;
			}
			timeLeft--;
		}
	}

	void updateBossBar() {
		if (currentTopic == null) {
			for (var player : Bukkit.getOnlinePlayers()) {
				player.hideBossBar(bar);
			}
			return;
		}

		var separator = Component.text(" | ");
		if (ballot.size() == 0 || currentTopic.options().options() == VoteOption.DEFAULT_OPTIONS) {
			// no votes yet or yes/no vote
			bar.name(Component.textOfChildren(
					Component.text("Topic: ", NamedTextColor.GOLD, TextDecoration.BOLD),
					currentTopic.display(),
					separator,
					Component.text("Time left: ", NamedTextColor.GREEN),
					Component.text(timeLeft + "s", NamedTextColor.YELLOW)
			));
			int yeas = 0;
			int total = ballot.size();
			for (var vote : ballot.values()) {
				if (vote == VoteOption.YEA) {
					yeas += 1;
				}
			}
			bar.progress(total != 0 ? (float) yeas / total : 0.5f);
		} else {
			// the ballot can't be empty
			Map<VoteOption, Long> count = ballot.values().stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
			var popular = Collections.max(count.entrySet(), Map.Entry.comparingByValue());
			var percentage = (float) popular.getValue() / ballot.size();
			bar.name(Component.textOfChildren(
					Component.text("Topic: ", NamedTextColor.GOLD, TextDecoration.BOLD),
					currentTopic.display(),
					separator,
					Component.text("Time left: ", NamedTextColor.GREEN),
					Component.text(timeLeft + "s", NamedTextColor.YELLOW),
					separator,
					Component.text("Most votes: ", NamedTextColor.GREEN),
					popular.getKey().display(),
					Component.text(" (" + TextUtils.formatNumber(percentage * 100) + "%)")
			));
			bar.progress(percentage);
		}

		for (var player : Bukkit.getOnlinePlayers()) {
			player.showBossBar(bar);
		}
	}

	public void cancelVote() {
		if (currentTopic != null && currentTopic.caller != null)
			calledVotes.remove(currentTopic.caller);
		currentTopic = null;
		ballot.clear();
	}

	VotingResults tallyResults() {
		// transform map of voter to vote into map of vote to list of voters
		var map = ballot.entrySet().stream()
				.collect(Collectors.groupingBy(Map.Entry::getValue,
						Collectors.mapping(Map.Entry::getKey, ImmutableSet.toImmutableSet())
				));
		return new VotingResults(ImmutableMap.copyOf(map));
	}

	Component getTopicDisplay(@Nullable Topic topic, @Nullable Locale locale, @Nullable VotingResults results) {
		if (topic == null)
			return Component.text("There is no ongoing vote.", NamedTextColor.DARK_RED);
		Duration time = Duration.between(currentTopic.time(), ZonedDateTime.now());

		TextComponent.Builder message = Component.text();
		message.append(
				SEPARATOR_COMPONENT,
				Component.newline(),
				Component.text("Topic: ", NamedTextColor.YELLOW),
				topic.display(),
				Component.newline()
		);
		if (topic.owner() != null) {
			message.append(
				Component.text("Submitted by ", NamedTextColor.YELLOW),
				topic.owner(),
				Component.space(),
				TextUtils.formatDuration(time, currentTopic.time(), locale),
				Component.text(" ago", NamedTextColor.YELLOW),
				Component.newline());
		}
		message.append(
				topic.options().actionMessage(),
				Component.newline()
		);

		if (topic == currentTopic) {
			if (results != null) {
				var options = new ArrayList<Component>(results.votes().size());
				for (var entry : results.votes().entrySet()) {
					var option = entry.getKey();
					var voters = entry.getValue();
					options.add(option.votesFormatter().apply(voters));
				}
				message.append(Component.join(JoinConfiguration.separator(Component.space()), options));
				message.append(Component.newline(),
						Component.text("Result: ", NamedTextColor.YELLOW),
						results.result() != null ?
								results.result().display() :
								Component.text("No result", NamedTextColor.DARK_GRAY));
			} else {
				long noOfVotes = ballot.size();
				message.append(Component.text("Votes: " + noOfVotes, NamedTextColor.GOLD));
			}
		}
		return message.append(Component.newline(), SEPARATOR_COMPONENT).build();
	}

	static final Component OPENING_BRACKET = Component.text("["),
			CLOSING_BRACKET = Component.text("]");
	Component getTopicVoteLinks() {
		var builder = Component.text().append(
				Component.text("Click to vote: ", NamedTextColor.YELLOW),
				Component.newline()
		);
		var options = currentTopic.options().options().values();
		var links = new ArrayList<Component>(options.size());
		for (var option : options) {
			var component = Component.text();
			component.append(OPENING_BRACKET, option.display(), CLOSING_BRACKET);
			component.clickEvent(ClickEvent.runCommand("/vote " + option.id));
			component.hoverEvent(HoverEvent.showText(Component.text("Click to vote for ").append(option.display)));
			links.add(component.build());
		}
		builder.append(Component.join(JoinConfiguration.separator(Component.space()), links));
		return builder.build();
	}

	static final Component LE_FUNNY_VOTE = Component.text("Reminder: This vote has no actual effect!", NamedTextColor.YELLOW);
	public void createVote(@Nullable UUID caller, Component owner, Component display) {
		createVote(caller, owner, display, new TopicOptions(false, null, LE_FUNNY_VOTE, VoteOption.DEFAULT_OPTIONS, true));
	}
	public void createVote(@Nullable UUID caller, Component owner, Component display, @NotNull TopicOptions options) {
		Topic topic = new Topic(caller, owner, display, options, ZonedDateTime.now());
		if (options.priority())
			priorityQueue.add(topic);
		else
			queue.add(topic);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
		// Cast a ballot. if no option given, send them the clickable ones
		if ("vote".equalsIgnoreCase(label)) {
			if (!(sender instanceof Player player)) {
				sender.sendMessage(Component.text("Computers don't have voting rights!", NamedTextColor.RED));
				return;
			}
			if (currentTopic == null) {
				player.sendMessage(NO_TOPIC);
				return;
			}
			if (args.length != 1) {
				player.sendMessage(getTopicVoteLinks());
				return;
			}
			var options = currentTopic.options().options();
			var vote = options.get(args[0]);
			if (vote == null) {
				player.sendMessage(Component.text(args[0] + " is not a valid option."));
				return;
			}
			var oldVote = ballot.put(player.getUniqueId(), vote);
			if (oldVote != null) {
				player.sendMessage(Component.text().color(NamedTextColor.GREEN)
						.append(Component.text("You changed your vote from "))
						.append(oldVote.display())
						.append(Component.text(" to "))
						.append(vote.display())
						.append(Component.text("."))
				);
			} else {
				player.sendMessage(Component.text("You casted your vote.").color(NamedTextColor.GREEN));
			}
		} else { // "callvote", Call a new election thing
			if (args.length == 0) {
				showUsage(sender);
				return;
			}
			switch (args[0]) {
				case "info" -> sender.sendMessage(getTopicDisplay(currentTopic, sender instanceof Player player ? player.locale() : null, null));
				case "queue" -> {
					int index = 1;
					sender.sendMessage(Component.text("Priority queue:", NamedTextColor.GOLD));
					for (var topic : priorityQueue) {
						sender.sendMessage(Component.text().append(
								Component.text(index++ + ". ", NamedTextColor.GOLD),
								topic.display()
						).hoverEvent(HoverEvent.showText(getTopicDisplay(topic, null, null))));
					}
					sender.sendMessage(Component.text("Queue:", NamedTextColor.GOLD));
					for (var topic : queue) {
						sender.sendMessage(Component.text().append(
								Component.text(index++ + ". ", NamedTextColor.GOLD),
								topic.display()
						).hoverEvent(HoverEvent.showText(getTopicDisplay(topic, null, null))));
					}
				}
				case "clearqueue" -> {
					if (!hasPermission(sender, PermissionLevel.MOD)) {
						sender.sendMessage(NO_PERMISSION);
						return;
					}
					queue.removeIf(topic -> {
						if (topic.caller != null) {
							calledVotes.remove(topic.caller);
						}
						return true;
					});
					sender.sendMessage(Component.text("Normal queue cleared."));
				}
				case "abort" -> {
					if (!hasPermission(sender, PermissionLevel.MOD)) {
						sender.sendMessage(NO_PERMISSION);
						return;
					}
					if (currentTopic == null) {
						return;
					}
					cancelVote();
					Bukkit.broadcast(Component.text("The ongoing poll has been aborted.", NamedTextColor.YELLOW));
				}
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
					if (currentTopic == null) {
						sender.sendMessage(Component.text("There is no topic to vote on!", NamedTextColor.RED));
						return;
					}
					if (sender instanceof Player player && !hasPermission(sender, PermissionLevel.OWNER)) {
						player.chat("I am Kim Jong Un (101% support rating)");
						player.getWorld().playSound(player, Sound.ENTITY_CAT_AMBIENT, 1, 0);
						return;
					}
					if (args.length < 2) {
						showUsage(sender, "/callvote myvoteissuperimportant <vote> [amount]");
						return;
					}
					VoteOption option = currentTopic.options().options().get(args[1]);
					if (option == null) {
						sender.sendMessage(Component.text("Invalid option " + args[1], NamedTextColor.RED));
						return;
					}

					int amount = 10;
					if (args.length == 3) {
						try {
							amount = Integer.parseInt(args[2]);
						} catch (NumberFormatException ignored) { }
					}
					Bukkit.broadcast(Component.textOfChildren(
							sender.name(),
							Component.text(" has casted " + amount + "x ", NamedTextColor.YELLOW),
							option.display(),
							Component.text(" votes!", NamedTextColor.YELLOW),
							Component.newline(),
							Component.text("Some people are, of course, more equal than others.", NamedTextColor.GRAY, TextDecoration.ITALIC)
					));
					for (int i = 0; i < amount; i++) {
						ballot.put(UUID.randomUUID(), option);
					}
				}
				// TODO allow specifying options
				case "createpriority" -> {
					if (!hasPermission(sender, PermissionLevel.MOD)) {
						showUsage(sender);
						return;
					}
					String topic = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
					UUID caller;
					Component name;
					if(sender instanceof Player player) {
						caller = player.getUniqueId();
						name = player.playerListName();
					} else {
						caller = null;
						name = sender.name();
					}
					createVote(caller, name, LegacyComponentSerializer.legacyAmpersand().deserialize(topic),
							new TopicOptions(true, null, LE_FUNNY_VOTE, VoteOption.DEFAULT_OPTIONS, true));
					sender.sendMessage(Component.text("Queued a topic!").color(NamedTextColor.GREEN));
				}
				default -> {
					String topic = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
					UUID caller;
					Component name;
					if (sender instanceof Player player) {
						caller = player.getUniqueId();
						name = player.playerListName();

						if (!calledVotes.add(caller)) {
							player.sendMessage(Component.text("You've already queued a topic!", TextColors.ERROR_RED));
							return;
						}
					} else {
						caller = null;
						name = sender.name();
					}
					createVote(caller, name, LegacyComponentSerializer.legacyAmpersand().deserialize(topic));
					sender.sendMessage(Component.text("Queued a topic!").color(NamedTextColor.GREEN));
				}
			}
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return hasPermission(sender, PermissionLevel.MOD) ?
					Arrays.asList("extend", "myvoteissuperimportant", "createpriority", "info", "queue", "clearqueue", "abort") :
					Arrays.asList("myvoteissuperimportant", "info");
		}
		return Collections.emptyList();
	}
}

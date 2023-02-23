package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public record DamageLogEntry(DamageType damageType, double damage, @Nullable Component damager, int time) {
	public static final Comparator<DamageLogEntry> ASCENDING = Comparator.comparingInt(DamageLogEntry::time);
	public static final Comparator<DamageLogEntry> DESCENDING = ASCENDING.reversed();

	private static final DecimalFormat DAMAGE_FORMAT = new DecimalFormat("#.##");
	private static final TextColor HEART_COLOR = TextColors.HEALTH;

	public Component asComponent() {
		return asComponent(false);
	}

	private static Component getDamageTypeComponent(DamageType type) {
		if (type.is(DamageType.FREEZE)) {
			return Component.text("❄ " + type.getName(), NamedTextColor.AQUA);
		} else if (type.is(DamageType.POISON)) {
			return Component.text("☠ " + type.getName(), NamedTextColor.GREEN);
		} else if (type.isFire() || type.isBurn()) {
			return Component.text("\uD83D\uDD25 " + type.getName(), NamedTextColor.RED); // lol emoji
		} else {
			return Component.text(type.getName(), NamedTextColor.DARK_GREEN);
		}
	}

	public Component asComponent(boolean legacy) {
		var builder = Component.text();
		builder.append(
				Component.text("-" + TextUtils.formatHealth(damage), HEART_COLOR),
				Component.text(" from ", NamedTextColor.YELLOW),
				getDamageTypeComponent(damageType)
		);
		if (damager != null) {
			builder.append(
					Component.text(" by ", NamedTextColor.DARK_GREEN),
					damager
			);
		}
		long timeAgo = (TeamArena.getGameTick() - time) * 50L;
		if (timeAgo == 0) {
			builder.append(Component.text(" just now", NamedTextColor.YELLOW));
		} else {
			builder.append(Component.space(),
					TextUtils.formatDuration(Duration.ofMillis(timeAgo)),
					Component.text(" ago", NamedTextColor.YELLOW));
		}
		return builder.build();
	}

	public record DamageSummary(DamageLogEntry summaryEntry, List<DamageLogEntry> entries) {}

	public static Map<DamageType, DamageSummary> createSummary(List<DamageLogEntry> entries) {
		Map<DamageType, List<DamageLogEntry>> damageTypes = entries.stream()
				.sorted(DESCENDING)
				.collect(Collectors.groupingBy(
						DamageLogEntry::damageType,
						LinkedHashMap::new,
						Collectors.toList()
				));

		LinkedHashMap<DamageType, DamageSummary> summary = new LinkedHashMap<>();
		damageTypes.forEach((type, list) -> {
			// get damager and time from the first entry
			DamageLogEntry firstEntry = list.get(0);
			double totalDamage = 0;
			for (var entry : list) {
				totalDamage += entry.damage;
			}

			summary.put(type, new DamageSummary(
					new DamageLogEntry(type, totalDamage, firstEntry.damager, firstEntry.time),
					Collections.unmodifiableList(list)
			));
		});
		return Collections.unmodifiableMap(summary);
	}

	public static void sendDamageLog(Player player) {
		PlayerInfo pinfo = Main.getPlayerInfo(player);
		Style style = pinfo.getPreference(Preferences.RECEIVE_DAMAGE_RECEIVED_LIST);

		if (style == Style.NONE)
			return;

		List<DamageLogEntry> list = pinfo.getDamageReceivedLog();
		double totalDamage = list.stream().mapToDouble(DamageLogEntry::damage).sum();
		if (style == Style.COMPACT) {
			player.sendMessage(Component.text("Here's how you died: (hover to see more)", NamedTextColor.DARK_PURPLE));
			Map<DamageType, DamageLogEntry.DamageSummary> damageSummary = createSummary(list);

			damageSummary.values().stream()
					.map(summary -> {
						TextComponent.Builder builder = Component.text().content("  "); // indentation
						builder.append(summary.summaryEntry.asComponent());
						// show all damage received on hover, separated by newlines
						builder.hoverEvent(HoverEvent.showText(
								summary.entries.stream()
										.map(DamageLogEntry::asComponent)
										.collect(Component.toComponent(Component.newline()))
						));
						return builder.build();
					})
					.forEach(player::sendMessage);

		} else {
			player.sendMessage(Component.text("Here's how you died:", NamedTextColor.DARK_PURPLE));
			List<Component> components = new ArrayList<>(list.size());
			for (DamageLogEntry dinfo : list) {
				TextComponent.Builder builder = Component.text();
				builder.content("  "); // indentation
				builder.append(dinfo.asComponent(true));
				components.add(builder.build());
			}
			player.sendMessage(Component.join(JoinConfiguration.newlines(), components));
		}
		player.sendMessage(Component.textOfChildren(
			Component.text("You took "),
			Component.text(TextUtils.formatHealth(totalDamage), TextColors.HEALTH),
			Component.text(" damage this life.")
		).color(NamedTextColor.WHITE));
	}

	public enum Style {
		NONE,
		COMPACT,
		FULL
	}
}

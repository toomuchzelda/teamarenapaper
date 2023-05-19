package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import com.google.gson.Gson;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ChangelogMenu implements InventoryProvider {
	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text("Blue Warfare News!!!", NamedTextColor.BLUE);
	}

	@Override
	public int getRows() {
		return 3;
	}

	static final ItemStack FETCHING = ItemBuilder.of(Material.CLOCK)
		.displayName(Component.text("Fetching the latest news for you!!", NamedTextColor.YELLOW))
		.build();
	static final ClickableItem CLOSE = ItemBuilder.of(Material.BARRIER)
		.displayName(Component.text("Close", NamedTextColor.RED))
		.toClickableItem(e -> Inventories.closeInventory((Player) e.getWhoClicked()));

	boolean shouldCheckStatus;
	@Override
	public void init(Player player, InventoryAccessor inventory) {
		inventory.fill(MenuItems.BORDER);
		if (shouldFetch()) {
			inventory.set(1, 4, FETCHING);
			shouldCheckStatus = true;
			fetch();
		} else {
			populate(player, inventory);
		}
		inventory.set(2, 4, CLOSE);
		if (Main.getPlayerInfo(player).permissionLevel == CustomCommand.PermissionLevel.OWNER) {
			inventory.set(2, 5, ItemBuilder.of(Material.BEDROCK)
				.displayName(Component.text("Force refetch (ADMIN)"))
				.toClickableItem(e -> LAST_FETCH = null));
		}
	}

	@Override
	public void update(Player player, InventoryAccessor inventory) {
		if (!shouldFetch()) // finished fetching
			populate(player, inventory);
	}

	void populate(Player player, InventoryAccessor inventory) {
		if (changelogs == null)
			return;
		for (int i = 0; i < 7; i++) {
			ClickableItem item;
			if (i >= changelogs.size()) {
				item = null;
			} else {
				Changelog changelog = changelogs.get(i);
				item = ItemBuilder.of(Material.BOOK)
					.displayName(Component.text(changelog.title, Style.style(NamedTextColor.GOLD, TextDecoration.BOLD)))
					.lore(TextUtils.wrapString(changelog.desc, Style.empty(), TextUtils.DEFAULT_WIDTH, true))
					.addLore(Component.empty(), Component.text("Click to read more!"))
					.toClickableItem(e -> {
						Player clicker = (Player) e.getWhoClicked();
						clicker.sendMessage(Component.text(changelog.url)
							.clickEvent(ClickEvent.openUrl(changelog.url)));
					});
			}

			inventory.set(10 + i, item);
		}
	}

	@Nullable
	static List<Changelog> changelogs;
	@Nullable
	static LocalDateTime LAST_FETCH;
	static final Duration CACHE_DURATION = Duration.ofDays(1);
	static boolean fetching = false;

	static boolean shouldFetch() {
		return (LAST_FETCH == null || Duration.between(LAST_FETCH, LocalDateTime.now()).compareTo(CACHE_DURATION) > 0);
	}

	static HttpClient client = HttpClient.newHttpClient();
	static void fetch() {
		if (fetching) // Don't fetch twice
			return;
		fetching = true;
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create("https://bluewarfare.xyz/updates/json"))
			.timeout(Duration.ofMinutes(2))
			.header("Content-Type", "application/json")
			.GET()
			.build();
		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenApply(HttpResponse::body)
			.thenAccept(str -> {
				Main.logger().info("Got response " + str);
				ChangelogResponse response = new Gson().fromJson(str, ChangelogResponse.class);
				changelogs = List.copyOf(response.posts);

				LAST_FETCH = LocalDateTime.now();
				fetching = false;
			});
		Main.logger().info("Sent GET request to https://bluewarfare.xyz/updates/json");
	}

	record ChangelogResponse(List<Changelog> posts) {}

	record Changelog(String title, String url, String desc) {}
}

package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import com.google.gson.Gson;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.inventory.Book;
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

	static final ItemStack FETCHING = ItemBuilder.of(Material.LIME_STAINED_GLASS_PANE)
		.displayName(Component.text("Fetching updates...", NamedTextColor.YELLOW))
		.build();
	static final ItemStack FETCHING_2 = ItemBuilder.of(Material.YELLOW_STAINED_GLASS_PANE)
		.displayName(Component.text("Fetching updates...", NamedTextColor.YELLOW))
		.build();

	boolean waiting;
	int animationTick;
	@Override
	public void init(Player player, InventoryAccessor inventory) {
		inventory.fill(MenuItems.BORDER);
		if (shouldFetch()) {
			inventory.set(1, 4, FETCHING);
			waiting = true;
			animationTick = TeamArena.getGameTick();
			fetch();
		} else {
			populate(player, inventory);
		}
		inventory.set(2, 4, MenuItems.CLOSE);
		if (Main.getPlayerInfo(player).permissionLevel == PermissionLevel.OWNER) {
			inventory.set(2, 5, ItemBuilder.of(Material.BEDROCK)
				.displayName(Component.text("Force refetch (ADMIN)"))
				.toClickableItem(e -> {
					LAST_FETCH = null;
					Player clicker = (Player) e.getWhoClicked();
					Inventories.openInventory(clicker, this);
				}));
		}
	}

	@Override
	public void update(Player player, InventoryAccessor inventory) {
		if (waiting) {
			if (!shouldFetch()) { // finished fetching
				populate(player, inventory);
				waiting = false;
			} else { // animation
				// 6 phases
				int phase = (TeamArena.getGameTick() - animationTick) / 2 % 6;
				if (phase >= 4) phase = 6 - phase;
				for (int i = 0; i < 4; i++) {
					ItemStack stack = i == phase ? FETCHING : FETCHING_2;
					inventory.set(13 + i, stack);
					inventory.set(13 - i, stack);
				}
			}
		}
	}

	static final Component LINK_COMPONENT = Component.textOfChildren(
		Component.text("Click here", Style.style(NamedTextColor.DARK_GREEN, TextDecoration.UNDERLINED)),
		Component.text(" to see the full changelog!", NamedTextColor.GOLD)
	);
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
					.lore(TextUtils.wrapString(changelog.desc, Style.style(NamedTextColor.YELLOW), TextUtils.DEFAULT_WIDTH, true))
					.addLore(Component.empty(), Component.text("Click to read more!", NamedTextColor.WHITE))
					.toClickableItem(e -> {
						Player clicker = (Player) e.getWhoClicked();
						clicker.openBook(Book.book(Component.empty(), Component.empty(),
							LINK_COMPONENT.clickEvent(ClickEvent.openUrl(changelog.url))));
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
		//return (LAST_FETCH == null || Duration.between(LAST_FETCH, LocalDateTime.now()).compareTo(CACHE_DURATION) > 0);
		return false;
	}

	static HttpClient client = HttpClient.newHttpClient();
	public static void fetch() {
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
				Main.logger().info("Fetched latest changelog");
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

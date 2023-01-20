package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class CommandCosmetics extends CustomCommand {
	public CommandCosmetics() {
		super("cosmetics", "Manage cosmetic items", "/cosmetics ...", PermissionLevel.ALL);
	}

	private void listCosmetics(CommandSender sender, boolean shouldListAll, CosmeticType type, Set<NamespacedKey> keys) {

		sender.sendMessage(Component.textOfChildren(
			Component.text((shouldListAll ? "Loaded" : "Owned") + " " + type.name() + "s\n", NamedTextColor.GOLD),
			Component.join(JoinConfiguration.commas(true),
				keys.stream().map(key -> Component.text(key.toString())).toList()
			).color(NamedTextColor.YELLOW)
		));
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length == 0)
			throw throwUsage();

		switch (args[0]) {
			case "list", "listall" -> {
				boolean shouldListAll = args[0].equals("listall") && hasPermission(sender, PermissionLevel.OWNER);
				if (args.length > 1) {
					CosmeticType type = CosmeticType.valueOf(args[1].toUpperCase(Locale.ENGLISH));

					Set<NamespacedKey> keys = shouldListAll ?
						CosmeticsManager.getLoadedCosmetics(type) :
						Main.getPlayerInfo((Player) sender).getCosmeticItems(type);
					listCosmetics(sender, shouldListAll, type, keys);
				} else {
					for (CosmeticType type : CosmeticType.values()) {

						Set<NamespacedKey> keys = shouldListAll ?
							CosmeticsManager.getLoadedCosmetics(type) :
							Main.getPlayerInfo((Player) sender).getCosmeticItems(type);
						listCosmetics(sender, shouldListAll, type, keys);
					}
				}
			}
			case "set" -> {
				if (!(sender instanceof Player player))
					throw new CommandException(PLAYER_ONLY);
				if (args.length <= 2)
					throw throwUsage("/cosmetics set <cosmeticType> <id>");

				CosmeticType type = CosmeticType.valueOf(args[1].toUpperCase(Locale.ENGLISH));
				NamespacedKey key = NamespacedKey.fromString(args[2]);
				if (key == null)
					throw new IllegalArgumentException("Invalid key " + args[2]);

				PlayerInfo info = Main.getPlayerInfo(player);
				if (info.hasCosmeticItem(key)) {
					info.setSelectedCosmetic(type, key);
					player.sendMessage(Component.text("Selected " + key + " as your " + type.name(), NamedTextColor.GREEN));
				} else {
					player.sendMessage(Component.text("You don't own " + key + "!", NamedTextColor.RED));
				}
			}
			case "info" -> {
				if (args.length <= 2)
					throw throwUsage("/cosmetics set <cosmeticType> <id>");

				CosmeticType type = CosmeticType.valueOf(args[1].toUpperCase(Locale.ENGLISH));
				NamespacedKey key = NamespacedKey.fromString(args[2]);
				CosmeticsManager.CosmeticInfo info;
				if (key == null || (info = CosmeticsManager.getCosmeticInfo(type, key)) == null)
					throw new IllegalArgumentException("Invalid key " + args[2]);

				sender.sendMessage(info.getDisplay());
			}
			case "reload" -> {
				if (!hasPermission(sender, PermissionLevel.OWNER))
					throw new CommandException(NO_PERMISSION);
				CosmeticsManager.reloadCosmetics();
			}
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		return switch (args.length) {
			case 1 -> {
				var subcommands = List.of("list", "set", "info");
				if (hasPermission(sender, PermissionLevel.OWNER)) {
					yield Stream.concat(Stream.of("listall", "reload"), subcommands.stream()).toList();
				} else {
					yield subcommands;
				}
			}
			case 2 -> switch (args[0]) {
				case "list", "set", "info", "listall" -> Arrays.stream(CosmeticType.values())
					.map(Enum::name)
					.map(s -> s.toLowerCase(Locale.ENGLISH))
					.toList();
				default -> List.of();
			};
			case 3 -> switch (args[0]) {
				case "set", "info" -> {
					CosmeticType type = CosmeticType.valueOf(args[1].toUpperCase(Locale.ENGLISH));
					yield CosmeticsManager.getLoadedCosmetics(type).stream()
						.map(NamespacedKey::toString)
						.toList();
				}
				default -> List.of();
			};
			default -> List.of();
		};
	}
}

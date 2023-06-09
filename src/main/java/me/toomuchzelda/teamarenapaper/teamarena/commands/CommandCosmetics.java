package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticItem;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticsManager;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.PlayerCosmetics;
import me.toomuchzelda.teamarenapaper.teamarena.inventory.CosmeticsInventory;
import net.kyori.adventure.text.Component;
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

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length == 0)
			throw throwUsage();

		switch (args[0]) {
			case "list", "listall" -> {
				boolean listAll = !(sender instanceof Player) || (args[0].equals("listall") &&
					hasPermission(sender, PermissionLevel.OWNER));
				CosmeticType[] toList;
				if (args.length > 1) {
					CosmeticType type = CosmeticType.valueOf(args[1].toUpperCase(Locale.ENGLISH));
					toList = new CosmeticType[] {type};
				} else {
					toList = CosmeticType.values();
				}
				for (CosmeticType type : toList) {

					Set<NamespacedKey> keys = listAll ?
						CosmeticsManager.getLoadedCosmetics(type) :
						Main.getPlayerInfo((Player) sender).getCosmetics().getAllCosmeticItems(type);

					Component title = Component.text((listAll ? "Loaded" : "Owned") + " " + type.name() + "s\n", NamedTextColor.GOLD);
					sender.sendMessage(Component.textOfChildren(
						title,
						keys.stream().map(key -> Component.text(key.toString())).collect(Component.toComponent(Component.text(", ")))
					).color(NamedTextColor.YELLOW));
				}
			}
			case "select", "unselect" -> {
				if (!(sender instanceof Player player))
					throw new CommandException(PLAYER_ONLY);
				if (args.length < 2)
					throw throwUsage("/cosmetics select/unselect <cosmeticType> [id]");

				PlayerInfo info = Main.getPlayerInfo(player);
				PlayerCosmetics cosmetics = info.getCosmetics();

				boolean select = "select".equals(args[0]);
				CosmeticType type = CosmeticType.valueOf(args[1].toUpperCase(Locale.ENGLISH));
				if (args.length == 2) {
					cosmetics.setSelectedCosmetic(type, null);
					player.sendMessage(Component.text("Unselected all " + type.name(), NamedTextColor.YELLOW));
				}
				NamespacedKey key = NamespacedKey.fromString(args[2]);
				if (key == null)
					throw new IllegalArgumentException("Invalid key " + args[2]);

				if (cosmetics.checkCosmeticItem(type, key)) {
					if (select)
						cosmetics.selectCosmetic(type, key);
					else
						cosmetics.unselectCosmetic(type, key);
					player.sendMessage(Component.text(
						(select ? "Selected " : "Unselected ") + key + " as your " + type.name(), NamedTextColor.GREEN));
				} else {
					player.sendMessage(Component.text("You don't own " + key + "!", NamedTextColor.RED));
				}
			}
			case "info" -> {
				if (args.length < 3)
					throw throwUsage("/cosmetics info <cosmeticType> <id>");

				CosmeticType type = CosmeticType.valueOf(args[1].toUpperCase(Locale.ENGLISH));
				NamespacedKey key = NamespacedKey.fromString(args[2]);
				CosmeticItem info;
				if (key == null || (info = CosmeticsManager.getCosmetic(type, key)) == null)
					throw new IllegalArgumentException("Invalid key " + args[2]);

				sender.sendMessage(info.getInfo());
			}
			case "gui" -> {
				if (!(sender instanceof Player player))
					throw new CommandException(PLAYER_ONLY);
				CosmeticType type = args.length >= 2 ?
					CosmeticType.valueOf(args[1].toUpperCase(Locale.ENGLISH)) :
					CosmeticType.GRAFFITI;
				Inventories.openInventory(player, new CosmeticsInventory(type));
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
				var subcommands = List.of("list", "select", "unselect", "info", "gui");
				if (hasPermission(sender, PermissionLevel.OWNER)) {
					yield Stream.concat(Stream.of("listall", "reload"), subcommands.stream()).toList();
				} else {
					yield subcommands;
				}
			}
			case 2 -> switch (args[0]) {
				case "list", "select", "unselect", "info", "listall", "gui" -> Arrays.stream(CosmeticType.values())
					.map(Enum::name)
					.map(s -> s.toLowerCase(Locale.ENGLISH))
					.toList();
				default -> List.of();
			};
			case 3 -> switch (args[0]) {
				case "select", "unselect", "info" -> {
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

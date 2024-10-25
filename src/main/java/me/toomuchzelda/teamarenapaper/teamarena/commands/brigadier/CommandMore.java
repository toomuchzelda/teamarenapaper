package me.toomuchzelda.teamarenapaper.teamarena.commands.brigadier;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.Commands;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import static io.papermc.paper.command.brigadier.Commands.literal;

public class CommandMore extends BrigadierCommand {

	private static final SimpleCommandExceptionType NOT_A_PLAYER = new SimpleCommandExceptionType(message(Component.text("Not a player", TextColors.ERROR_RED)));

	public static void register(Commands commands) {
		commands.register(
			literal("more").requires(permission(PermissionLevel.OWNER))
				.executes(ctx -> {
					if (!(ctx.getSource().getExecutor() instanceof Player player)) {
						throw NOT_A_PLAYER.create();
					}

					PlayerInventory inventory = player.getInventory();
					ItemStack item = inventory.getItemInMainHand();
					if (!item.isEmpty()) {
						inventory.setItemInMainHand(item.asQuantity(item.getMaxStackSize()));
						return 1;
					}
					return 0;
				})
				.build(),
			"Gives you more of the item you are currently holding"
		);
	}
}

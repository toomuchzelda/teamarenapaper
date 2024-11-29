package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.explosives.ExplosiveProjectilesAbility;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.RailgunAbility;
import me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions.KitDemolitions;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CommandItem extends CustomCommand {

	private static final Map<String, Supplier<ItemStack>> items = Map.of(
		"TNTMine", () -> KitDemolitions.TNT_MINE_ITEM,
		"PushMine", () -> KitDemolitions.PUSH_MINE_ITEM,
		"Railgun", () -> RailgunAbility.RAILGUN,
		"RPG", () -> ExplosiveProjectilesAbility.RPG,
		"Grenade", () -> ExplosiveProjectilesAbility.GRENADE
		// TODO add more as needed
	);

	public CommandItem() {
		super("item", "Get team arena items", "/item player item [count]", PermissionLevel.OWNER);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length < 2)
			throw throwUsage();

		List<Entity> entities = CustomCommand.selectEntities(sender, args[0]);
		ItemStack item = items.getOrDefault(args[1],
			() -> ItemBuilder.of(Material.CAKE).displayName(Component.text(sender.getName() + " tried to give you " + args[1] + " but got the command horribly, horribly wrong.")).build()
		).get();

		int count = 1;
		if (args.length >= 3) {
			try { count = Integer.parseInt(args[2]); }
			catch (NumberFormatException ignored) {
				throw throwUsage("Bad count");
			}
		}
		item = item.asQuantity(count);

		for (Entity e : entities) {
			if (e instanceof HumanEntity human) {
				human.getInventory().addItem(item);
			}
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return CustomCommand.suggestPlayerSelectors();
		}
		else if (args.length == 2) {
			return items.keySet();
		}
		else if (args.length == 3) {
			return List.of("count");
		}

		return Collections.emptyList();
	}
}

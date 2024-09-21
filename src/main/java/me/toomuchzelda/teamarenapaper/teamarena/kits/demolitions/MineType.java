package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public enum MineType
{
	TNTMINE("TNT Mine", TextColor.color(0xd82e1a), KitDemolitions.TNT_MINE_ITEM, KitDemolitions.TNT_MINE_DEPLETED,
		TNTMine::new, 3, 12, 25, 30 * 20),
	PUSHMINE("Push Mine", NamedTextColor.WHITE, KitDemolitions.PUSH_MINE_ITEM, KitDemolitions.PUSH_MINE_DEPLETED,
		PushMine::new, 3, 12, 25, 30 * 20);

	public final String name;
	public final TextColor color;
	final ItemStack item;
	final ItemStack itemDepleted;
	public final BiFunction<Player, Block, ? extends DemoMine> constructor;
	public final int damageToKill;
	public final int timeToDetonate;
	public final int timeToDetonateRemote;
	public final int timeToRegen;

	MineType(String name, TextColor color, ItemStack item, ItemStack itemDepleted,
			 BiFunction<Player, Block, ? extends DemoMine> constructor, int health, int ticksToDetonate, int ticksToDetonateRemote, int ticksToRegen) {
		this.name = name;
		this.color = color;
		this.item = item.asOne();
		this.itemDepleted = itemDepleted.asOne();
		this.constructor = constructor;
		this.damageToKill = health;
		this.timeToDetonate = ticksToDetonate;
		this.timeToDetonateRemote = ticksToDetonateRemote;
		this.timeToRegen = ticksToRegen;
	}

	public ItemStack item() {
		return item.clone();
	}

	public ItemStack itemDepleted() {
		return itemDepleted.clone();
	}

	public Component displayName() {
		return Component.text(name, color);
	}

	@Nullable
	public static MineType getFromItemStack(ItemStack item) {
		for (MineType type : values()) {
			if (type.item.isSimilar(item))
				return type;
		}
		return null;
	}
}

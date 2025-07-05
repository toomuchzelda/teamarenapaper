package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public enum MineType
{
	TNTMINE("TNT Mine", TextColor.color(0xd82e1a), Keys.TNT_MINE, KitDemolitions.TNT_MINE_ITEM, KitDemolitions.TNT_MINE_DEPLETED,
		TNTMine::new, 3, 12, 25, 30 * 20),
	PUSHMINE("Push Mine", NamedTextColor.WHITE, Keys.PUSH_MINE, KitDemolitions.PUSH_MINE_ITEM, KitDemolitions.PUSH_MINE_DEPLETED,
		PushMine::new, 3, 12, 25, 30 * 20);

	public static class Keys {
		// lookup keys
		public static final NamespacedKey DEPLETED = Main.key("mine/depleted");
		public static final NamespacedKey TYPE = Main.key("mine/type");

		// mine types
		public static final NamespacedKey TNT_MINE = Main.key("mine/tnt_mine");
		public static final NamespacedKey PUSH_MINE = Main.key("mine/push_mine");

		// ItemBuilder composers
		public static Consumer<ItemBuilder> set(NamespacedKey key) {
			return builder -> builder.setPDC(TYPE, PersistentDataType.STRING, key.asString());
		}

		public static Consumer<ItemBuilder> setDepleted(NamespacedKey key) {
			return builder -> {
				builder.setPDC(TYPE, PersistentDataType.STRING, key.asString());
				builder.setPDCFlag(DEPLETED);
			};
		}
	}

	public final String name;
	public final TextColor color;
	private final NamespacedKey key;
	final ItemStack item;
	final ItemStack itemDepleted;
	public final BiFunction<Player, Block, ? extends DemoMine> constructor;
	public final int damageToKill;
	public final int timeToDetonate;
	public final int timeToDetonateRemote;
	public final int timeToRegen;

	MineType(String name, TextColor color, NamespacedKey key, ItemStack item, ItemStack itemDepleted,
			 BiFunction<Player, Block, ? extends DemoMine> constructor, int health, int ticksToDetonate, int ticksToDetonateRemote, int ticksToRegen) {
		this.name = name;
		this.color = color;
		this.key = key;
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

	public NamespacedKey key() {
		return this.key;
	}

	private static final Map<String, MineType> TYPE_LOOKUP = Arrays.stream(values())
		.collect(Collectors.toUnmodifiableMap(t -> t.key.asString(), t -> t));

	@Nullable
	public static MineType getFromItemStack(ItemStack item) {
		if (item == null || item.isEmpty() || isDepleted(item))
			return null;
		String type = item.getPersistentDataContainer().get(Keys.TYPE, PersistentDataType.STRING);
		return type != null ? TYPE_LOOKUP.get(type) : null;
	}

	public static boolean isDepleted(ItemStack stack) {
		return stack.getPersistentDataContainer().has(Keys.DEPLETED);
	}
}

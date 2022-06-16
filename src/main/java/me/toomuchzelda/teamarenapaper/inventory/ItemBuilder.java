package me.toomuchzelda.teamarenapaper.inventory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class ItemBuilder {
    private final ItemStack stack;
    private final ItemMeta meta;
    private ItemBuilder(Material material) {
        this(new ItemStack(material));
    }

    private ItemBuilder(ItemStack stack) {
        this.stack = stack;
        this.meta = stack.getItemMeta();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder from(ItemStack stack) {
        return new ItemBuilder(stack);
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(amount);
        return this;
    }

    public ItemBuilder meta(Consumer<ItemMeta> consumer) {
        consumer.accept(meta);
        return this;
    }

    public <T extends ItemMeta> ItemBuilder meta(Class<T> clazz, Consumer<T> consumer) {
        if (clazz.isInstance(meta)) {
            consumer.accept(clazz.cast(meta));
        }
        return this;
    }

    public ItemBuilder displayName(ComponentLike component) {
		var actualComponent = component.asComponent();
		if (actualComponent.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET)
			actualComponent = actualComponent.decoration(TextDecoration.ITALIC, false);
        meta.displayName(actualComponent);
        return this;
    }

    public ItemBuilder lore(ComponentLike... components) {
        return lore(Arrays.asList(components));
    }

    public ItemBuilder lore(List<? extends ComponentLike> components) {
        List<Component> lore = new ArrayList<>(components.size());
        for (ComponentLike componentLike : components) {
			var actualComponent = componentLike.asComponent();
			if (actualComponent.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET)
				lore.add(actualComponent.decoration(TextDecoration.ITALIC, false));
			else
            	lore.add(actualComponent);
        }
        meta.lore(lore);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder color(Color color) {
        meta(LeatherArmorMeta.class, armorMeta -> armorMeta.setColor(color));
        return this;
    }

    public ItemBuilder hide(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemStack build() {
        stack.setItemMeta(meta);
        return stack;
    }
}

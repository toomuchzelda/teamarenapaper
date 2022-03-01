package me.toomuchzelda.teamarenapaper.inventory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class ItemBuilder {
    private ItemStack stack;
    private ItemMeta meta;
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
        meta.displayName(component.asComponent());
        return this;
    }

    public ItemBuilder lore(ComponentLike... components) {
        return lore(Arrays.asList(components));
    }

    public ItemBuilder lore(List<? extends ComponentLike> components) {
        List<Component> actualComponents = new ArrayList<>(components.size());
        for (ComponentLike componentLike : components) {
            actualComponents.add(componentLike.asComponent());
        }
        meta.lore(actualComponents);
        return this;
    }

    public ItemStack build() {
        stack.setItemMeta(meta);
        return stack;
    }
}

package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

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

	@SuppressWarnings("unchecked")
    public <T extends ItemMeta> ItemBuilder meta(Class<T> clazz, Consumer<T> consumer) {
        if (clazz.isInstance(meta)) {
            consumer.accept((T) meta);
        }
        return this;
    }

	@Nullable
	public Component displayName() {
		return meta.displayName();
	}

    public ItemBuilder displayName(@Nullable ComponentLike component) {
        meta.displayName(component != null ?
			component.asComponent().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) :
			null);
        return this;
    }

	@Nullable
	public List<Component> lore() {
		return meta.lore();
	}

    public ItemBuilder lore(ComponentLike... components) {
        return lore(Arrays.asList(components));
    }

    public ItemBuilder lore(List<? extends ComponentLike> components) {
        List<Component> lore = new ArrayList<>(components.size());
        for (ComponentLike componentLike : components) {
			lore.add(componentLike.asComponent().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        meta.lore(lore);
        return this;
    }

	public ItemBuilder addLore(ComponentLike... components) {
		return addLore(Arrays.asList(components));
	}

	public ItemBuilder addLore(List<? extends ComponentLike> components) {
		List<Component> oldLore = lore(), newLore;
		if (oldLore != null) {
			newLore = new ArrayList<>(oldLore.size() + components.size());
			newLore.addAll(oldLore);
		} else {
			newLore = new ArrayList<>(components.size());
		}
		for (var componentLike : components) {
			newLore.add(componentLike.asComponent().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
		}
		meta.lore(newLore);
		return this;
	}

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder color(@Nullable Color color) {
        meta(LeatherArmorMeta.class, armorMeta -> armorMeta.setColor(color));
        return this;
    }

	public ItemBuilder customModelData(@Nullable Integer data) {
		meta.setCustomModelData(data);
		return this;
	}


	@Deprecated
	public ItemBuilder customModelData(@Nullable String data) {
		if (data != null)
			meta.setCustomModelData(ItemUtils.getCustomModelData(data));
		else
			meta.setCustomModelData(null);
		return this;
	}

    public ItemBuilder hide(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

	public ItemBuilder hideAll() {
		return hide(ItemFlag.values());
	}

	private static final NamespacedKey UNIQUE_KEY = new NamespacedKey(Main.getPlugin(), "item_id");
	public ItemBuilder unique() {
		meta.getPersistentDataContainer().set(UNIQUE_KEY, PersistentDataType.INTEGER, ItemUtils._uniqueName++);
		return this;
	}

    public ItemStack build() {
        stack.setItemMeta(meta);
        return stack;
    }

	public ClickableItem toClickableItem(Consumer<InventoryClickEvent> handler) {
		return ClickableItem.of(build(), handler);
	}

	public ClickableItem toEmptyClickableItem() {
		return ClickableItem.empty(build());
	}
}

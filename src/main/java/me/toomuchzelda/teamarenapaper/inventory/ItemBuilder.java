package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.MutableDataComponentPatch;
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
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class ItemBuilder {
    private final ItemStack stack;
    private final ItemMeta meta;
	private MutableDataComponentPatch dataComponentPatch;
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
	public Component name() {
		return meta.hasItemName() ? meta.itemName() : null;
	}

	public ItemBuilder name(@Nullable ComponentLike component) {
		meta.itemName(component != null ? component.asComponent() : null);
		return this;
	}

	@Nullable
	public List<Component> lore() {
		return meta.lore();
	}

    public ItemBuilder lore(ComponentLike... components) {
        return lore(Arrays.asList(components));
    }

    public ItemBuilder lore(Collection<? extends ComponentLike> components) {
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

	public ItemBuilder addLore(Collection<? extends ComponentLike> components) {
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

	public ItemBuilder enchantmentGlint(boolean glint) {
		meta.setEnchantmentGlintOverride(glint);
		return this;
	}

    public ItemBuilder color(@Nullable Color color) {
        meta(LeatherArmorMeta.class, armorMeta -> armorMeta.setColor(color));
        return this;
    }

	public ItemBuilder armourTrim(ArmorTrim trim) {
		meta(ArmorMeta.class, armorMeta -> armorMeta.setTrim(trim));
		return this;
	}

	public ItemBuilder customModelData(@Nullable Integer data) {
		meta.setCustomModelData(data);
		return this;
	}

    public ItemBuilder hide(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

	public ItemBuilder hideAll() {
		return hide(ItemFlag.values());
	}

	public ItemBuilder hideTooltip() {
		meta.setHideTooltip(true);
		return this;
	}

	private static final NamespacedKey UNIQUE_KEY = new NamespacedKey(Main.getPlugin(), "item_id");
	private static int id = 0;
	public ItemBuilder unique() {
		meta.getPersistentDataContainer().set(UNIQUE_KEY, PersistentDataType.INTEGER, id++);
		return this;
	}

	// Data Components API
	private MutableDataComponentPatch components() {
		if (dataComponentPatch == null)
			dataComponentPatch = MutableDataComponentPatch.fromItem(stack);
		return dataComponentPatch;
	}

	public ItemBuilder removeData(io.papermc.paper.datacomponent.DataComponentType type) {
		components().remove(type);
		return this;
	}

	public ItemBuilder setData(io.papermc.paper.datacomponent.DataComponentType.NonValued component) {
		components().set(component);
		return this;
	}

	public <T> ItemBuilder setData(io.papermc.paper.datacomponent.DataComponentType.Valued<T> component, T value) {
		components().set(component, value);
		return this;
	}

	public <T> T getData(io.papermc.paper.datacomponent.DataComponentType.Valued<T> component) {
		return components().get(component);
	}

	public <T> T getDataOrDefault(io.papermc.paper.datacomponent.DataComponentType.Valued<T> component, T fallback) {
		return components().getOrDefault(component, fallback);
	}

	public boolean hasData(io.papermc.paper.datacomponent.DataComponentType component) {
		return components().has(component);
	}

    public ItemStack build() {
        stack.setItemMeta(meta);
		if (dataComponentPatch != null) {
			dataComponentPatch.apply(stack);
		}
        return stack;
    }

	public ClickableItem toClickableItem(Consumer<InventoryClickEvent> handler) {
		return ClickableItem.of(build(), handler);
	}

	public ClickableItem toEmptyClickableItem() {
		return ClickableItem.empty(build());
	}
}

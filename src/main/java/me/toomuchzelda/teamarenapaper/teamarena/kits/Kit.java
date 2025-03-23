package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public abstract class Kit {
    public static final Comparator<Kit> COMPARATOR = Comparator.comparing(Kit::getName);

    private static final Ability[] EMPTY_ABILITIES = new Ability[0];

	private final String name;
	private Component displayName;
	private final String key; // used in commands
    private final List<Component> description;
    private final ItemStack display;
	@NotNull
	private KitCategory category = KitCategory.FIGHTER;
    //0: boots, 1: leggings, 2: chestplate, 3: helmet
    private ItemStack[] armour;
    private ItemStack[] items;
    private Ability[] abilities;
	private int fuseEnchantLevel;

    //active users of a kit ie they are alive and using, not dead and just have the kit selected
    private final Set<Player> activeUsers;

    public Kit(String name, String description, Material icon) {
        this(name, description, new ItemStack(icon));
    }

	protected static final Style DESC_STYLE = Style.style(NamedTextColor.YELLOW);
    public Kit(String name, String description, ItemStack display) {
		this(name.toLowerCase(Locale.ENGLISH), name, TextUtils.wrapString(description, DESC_STYLE, TextUtils.DEFAULT_WIDTH, true), display);
    }

	public Kit(String key, String name, List<? extends Component> description, ItemStack display) {
		this.key = key;
		this.name = name;
		this.description = List.copyOf(description);
		this.display = display.clone();

		//these are set via the setter methods
		ItemStack[] armour = new ItemStack[4];
		Arrays.fill(armour, new ItemStack(Material.AIR));
		this.armour = armour;

		this.items = new ItemStack[0];
		this.abilities = EMPTY_ABILITIES;
		this.fuseEnchantLevel = 0;

		activeUsers = new LinkedHashSet<>();
	}

	/** Copy ctor */
	public Kit (Kit other, String name, String description, ItemStack icon) {
		this.name = name;
		this.key = this.name.toLowerCase(Locale.ENGLISH);
		this.description = description;
		this.display = icon.clone();

		this.armour = other.armour;
		this.items = other.items;
		this.abilities = other.abilities;
		this.fuseEnchantLevel = other.fuseEnchantLevel;

		this.activeUsers = new LinkedHashSet<>();
	}

	public Kit setCategory(@NotNull KitCategory category) {
		this.category = category;
		return this;
	}

	@NotNull
	public KitCategory getCategory() {
		return category;
	}

	protected void setDisplayName(Component displayName) {
		this.displayName = displayName;
	}

	public Component getDisplayName() {
		if (displayName == null)
			displayName = Component.text(getName(), category.textColor());
		return displayName;
	}

    //clearInventory and updateInventory happens outside the following two methods
    //give kit and it's abilities to player
    public void giveKit(Player player, boolean update) {
        giveKit(player, update, Main.getPlayerInfo(player));
    }

    public void giveKit(Player player, boolean update, PlayerInfo pinfo) {
		if (pinfo.activeKit != null) {
			pinfo.activeKit.removeKit(player, pinfo);
		}
        activeUsers.add(player);

        PlayerInventory inventory = player.getInventory();
        inventory.setArmorContents(armour);

        //only give items if there are items
        if(items.length > 0) {
            //fill up from empty slots only
            int itemsIdx = 0;
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, items[itemsIdx]);
                    itemsIdx++;
                    if (itemsIdx == items.length)
                        break;
                }
            }
        }

        for(Ability ability : abilities) {
			Ability.giveAbility(player, ability, pinfo);
        }

        pinfo.activeKit = this;

        if(update)
            player.updateInventory();
    }

    //remove abilities from player
    public void removeKit(Player player) {
        removeKit(player, Main.getPlayerInfo(player));
    }

    public void removeKit(Player player, PlayerInfo pinfo) {
        activeUsers.remove(player);

        for(Ability a : abilities) {
			Ability.removeAbility(player, a, pinfo);
        }

        pinfo.activeKit = null;
    }

    public void setArmour(ItemStack[] armour) {
        this.armour = armour;
    }

    /**
     * Sets the armor pieces given by this kit
     * @param head The item in the head slot
     * @param chest The item in the chest slot
     * @param legs The item in the legs slot
     * @param feet The item in the feet slot
     */
    public void setArmor(@Nullable ItemStack head, @Nullable ItemStack chest,
                         @Nullable ItemStack legs, @Nullable ItemStack feet) {
        this.armour = new ItemStack[] {feet, legs, chest, head};
    }

    public ItemStack[] getArmour() {
        return armour;
    }

    public void setItems(ItemStack... items) {
        this.items = items;
    }

	/** Note returned array is mutable */
	public ItemStack[] getItems() {
		return this.items;
	}

    public void setAbilities(Ability... abilities) {
        this.abilities = abilities;
    }

	public void setFuseEnchantLevel(int level) {
		this.fuseEnchantLevel = level;
	}

    public ItemStack getIcon() {
        return display.clone();
    }

    public static @Nullable Kit getActiveKit(Player player) {
        Kit kit = Main.getPlayerInfo(player).activeKit;
        return kit;
    }

    /**
     * for spy
     */
    public static Kit getActiveKitHideInvis(Player player) {
        Kit kit = getActiveKit(player);
        if(kit == null)
            return null;

        while(kit.isInvisKit()) {
            kit = getRandomKit();
        }

        return kit;
    }

    public static Kit getRandomKit() {
        Kit[] kits = Main.getGame().getKits().toArray(new Kit[0]);
        return kits[MathUtils.randomMax(kits.length - 1)];
    }

    public String getName() {
        return name;
    }

	/**
	 * Gets the key of this kit, to be used in commands
	 * @return The key of this kit
	 */
	public String getKey() {
		return key;
	}

	@Unmodifiable
    public List<? extends Component> getDescription() {
        return description;
    }

    public Set<Player> getActiveUsers() {
        return activeUsers;
    }

    public Ability[] getAbilities() {
        return abilities;
    }

    public boolean isInvisKit() {
        return false;
    }

	/**
	 * For SearchAndDestroy, get the level of enchantment the fuse has.
	 * @return 0 for no enchantment, 1-10 for that level of bomb arming/disarming enchantment.
	 */
	public int getFuseEnchantmentLevel() {
		return this.fuseEnchantLevel;
	}

	/**
	 * For killstreaks, if a kit will handle it's own killstreaks or if it should be handled the default way by TeamArena
	 */
	public boolean handlesStreaksManually() {
		return false;
	}

    /**
     * Get all the abilities the player currently has.
	 * This method shouldn't really be in this class, but it is because tech debt.
     */
    public static Set<Ability> getAbilities(Player player) {
       return Ability.getAbilities(player);
        /*if(pinfo.activeKit != null) {
            return pinfo.activeKit.getAbilities();
        }
        else {
            return EMPTY_ABILITIES;
        }*/
    }

	/** Out of date: Abilities are now tracked in PlayerInfo */
    public static boolean hasAbility(Player player, Class<? extends Ability> ability) {
        Kit kit = getActiveKit(player);
        return hasAbility(kit, ability);
    }

    public static boolean hasAbility(Kit kit, Class<? extends Ability> ability) {
        if(kit != null) {
            for (Ability a : kit.getAbilities()) {
                if (ability.isInstance(a))
                    return true;
            }
        }

        return false;
    }
}

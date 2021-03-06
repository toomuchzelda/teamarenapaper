package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Kit {
    public static final Comparator<Kit> COMPARATOR = Comparator.comparing(Kit::getName);

    private static final Ability[] EMPTY_ABILITIES = new Ability[0];

	private final String name;
    private final String description;
    private final ItemStack display;
	@NotNull
	private KitCategory category = KitCategory.FIGHTER;
    //0: boots, 1: leggings, 2: chestplate, 3: helmet
    private ItemStack[] armour;
    private ItemStack[] items;
    private Ability[] abilities;

    //active users of a kit ie they are alive and using, not dead and just have the kit selected
    private final Set<Player> activeUsers;

    public Kit(String name, String description, Material icon) {
        this(name, description, new ItemStack(icon));
    }

    public Kit(String name, String description, ItemStack display) {
        this.name = name;
        this.description = description;
        this.display = display.clone();

        //these are set via the setter methods
        ItemStack[] armour = new ItemStack[4];
        Arrays.fill(armour, new ItemStack(Material.AIR));
        this.armour = armour;

        this.items = new ItemStack[0];
        this.abilities = new Ability[0];

        activeUsers = ConcurrentHashMap.newKeySet();
    }

	public Kit setCategory(@NotNull KitCategory category) {
		this.category = category;
		return this;
	}

	@NotNull
	public KitCategory getCategory() {
		return category;
	}

	public Component getDisplayName() {
		return Component.text(getName(), category.textColor());
	}

    //clearInventory and updateInventory happens outside the following two methods
    //give kit and it's abilities to player
    public void giveKit(Player player, boolean update) {
        giveKit(player, update, Main.getPlayerInfo(player));
    }

    public void giveKit(Player player, boolean update, PlayerInfo pinfo) {
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
            ability.giveAbility(player);
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
            a.removeAbility(player);
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

    public void setAbilities(Ability... abilities) {
        this.abilities = abilities;
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
     * @return
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

    public String getDescription() {
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
     * get abilities of the kit the player is actively using
     */
    public static Ability[] getAbilities(Player player) {
        PlayerInfo pinfo = Main.getPlayerInfo(player);
        if(pinfo.activeKit != null) {
            return pinfo.activeKit.getAbilities();
        }
        else {
            return EMPTY_ABILITIES;
        }
    }

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

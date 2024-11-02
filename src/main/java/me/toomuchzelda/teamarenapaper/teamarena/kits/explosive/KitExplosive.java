package me.toomuchzelda.teamarenapaper.teamarena.kits.explosive;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.ExplosiveProjectilesAbility;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.*;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
/*
//Kit Explosive:
// Primary: Utility
// Secondary: Ranged

RWF explosive but...

RPG has lower cooldown and lower dmg + Rocket Jump
Grenade has more up-time but cannot be spammed as much

Overall lower cooldowns and less burst damage, so it has more consistent damage output
 */

/**
 * @author onett425
 */
public class KitExplosive extends Kit
{
	public KitExplosive() {
		super("Explosive", "Destroy waves of enemies with the power of explosives!\n\n" +
				"Deal massive boom damage from a range with the RPG (actually a very explosive egg)!\n" +
				"Make enemies run for their lives by throwing out grenades!\n\n" +
				"You'll never have enough explosives with Kit Explosive!", Material.FIREWORK_STAR);

		ItemStack sword = new ItemStack(Material.STONE_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
		sword.setItemMeta(swordMeta);

		setItems(sword, ExplosiveProjectilesAbility.RPG.asQuantity(2), ExplosiveProjectilesAbility.GRENADE.asQuantity(5));
		setArmor(new ItemStack(Material.DIAMOND_HELMET), new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.GOLDEN_LEGGINGS), new ItemStack(Material.DIAMOND_BOOTS));
		setAbilities(new ExplosiveAbility());

		this.setCategory(KitCategory.UTILITY);
	}

	public static class ExplosiveAbility extends Ability {
		//recharge time = time to get +1 item back
		//max active = max # of items you can have active at a time
		//max in inv = max # of items you can have in inventory
		public static final int RPG_RECHARGE_TIME = 16 * 20;
		public static final int RPG_MAX_IN_INV = 2;

		public static final int GRENADE_RECHARGE_TIME = 80;
		public static final int GRENADE_MAX_IN_INV = 5;

		private static class UsageTimes {
			private int lastRpgUse;
			private int lastGrenadeUse;

			public UsageTimes() { lastRpgUse = TeamArena.getGameTick(); lastGrenadeUse = TeamArena.getGameTick(); }
		}

		private final Map<Player, UsageTimes> usageTimes = new HashMap<>();

		@Override
		protected void giveAbility(Player player) {
			this.usageTimes.put(player, new UsageTimes());
		}

		@Override
		protected void removeAbility(Player player) {
			usageTimes.remove(player);
		}

		@Override
		public void unregisterAbility() {
			this.usageTimes.clear();
		}

		// hack
		public void onUse(Player thrower, boolean grenade) {
			UsageTimes times = this.usageTimes.get(thrower);
			if (times != null) {
				if (grenade)
					times.lastGrenadeUse = TeamArena.getGameTick();
				else
					times.lastRpgUse = TeamArena.getGameTick();
			}
		}

		@Override
		public void onTick() {
			final int currentTick = TeamArena.getGameTick();
			for (var entry : usageTimes.entrySet()) {
				Player thrower = entry.getKey();
				UsageTimes times = entry.getValue();

				ItemUtils.addRechargedItem(thrower, currentTick, times.lastGrenadeUse, GRENADE_MAX_IN_INV,
					GRENADE_RECHARGE_TIME, ExplosiveProjectilesAbility.GRENADE);
				ItemUtils.addRechargedItem(thrower, currentTick, times.lastRpgUse, RPG_MAX_IN_INV,
					RPG_RECHARGE_TIME, ExplosiveProjectilesAbility.RPG);
			}
		}

		/*@Override // No longer needed
		public void onPlayerTick(Player player) {
			//Fixing glitch where player can get extra explosives by "hiding" grenades
			//in inventory's crafting menu
			PlayerInventory inv = player.getInventory();
			//Ignore excess explosives if the player is in creative mode and is admin abusing
			if (player.getGameMode() != GameMode.CREATIVE) {
				ItemUtils.maxItemAmount(inv, GRENADE, GRENADE_MAX_IN_INV);
				ItemUtils.maxItemAmount(inv, RPG, RPG_MAX_IN_INV);
			}
		}*/
	}
}

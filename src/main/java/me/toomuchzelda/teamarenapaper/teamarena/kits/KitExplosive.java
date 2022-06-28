package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.FireworkStarItem;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
/*
//Kit Explosive:
// Primary: Utility
// Secondary: Ranged

RWF explosive but...

RPG has lower cooldown and lower dmg + Rocket Jump
Grenade has more up-time but cannot be spammed as much

Overall smaller cooldowns and less burst damage so it has more consistent damage output
 */

/**
 * @author onett425
 */
public class KitExplosive extends Kit{

	public KitExplosive() {
		super("Explosive", "The classic kit from the golden days... " +
				"but with even more explosive power.", Material.FIREWORK_STAR);

		ItemStack sword = new ItemStack(Material.STONE_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
		sword.setItemMeta(swordMeta);

		ItemStack rpg = ItemBuilder.of(Material.EGG).color(Color.YELLOW)
				.displayName(ItemUtils.noItalics(Component.text("RPG"))).build();
		ItemStack grenade = ItemBuilder.of(Material.FIREWORK_STAR).color(Color.YELLOW)
				.displayName(ItemUtils.noItalics(Component.text("Grenade"))).build();

		setItems(sword, rpg, grenade);
		setArmor(new ItemStack(Material.DIAMOND_HELMET), new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.GOLDEN_LEGGINGS), new ItemStack(Material.DIAMOND_BOOTS));
		setAbilities(new ExplosiveAbility());
	}

	public static class ExplosiveAbility extends Ability{

		public static final int RPG_CD = 200;
		//recharge time = time to get +1 grenade back
		//max active = max # of grenades you can have active at a time
		//max in inv = max # of grenades you can have in inventory
		public static final int GRENADE_RECHARGE_TIME = 60;
		public static final int GRENADE_MAX_ACTIVE = 3;
		public static final int GRENADE_MAX_IN_INV = 5;

		@Override
		public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
			Material mat = event.getItemStack().getType();
			Projectile proj = event.getProjectile();
			Player shooter = event.getPlayer();

			//Launching RPG
			if(mat == Material.EGG){
				event.setShouldConsume(false);
				//Only apply CD when thrower is not in creative mode to allow for admin abuse
				if(shooter.getGameMode() != GameMode.CREATIVE){
					shooter.setCooldown(Material.EGG, RPG_CD);
				}
			}
		}

		public void onInteract(PlayerInteractEvent event) {
			Material mat = event.getMaterial();
			Player player = event.getPlayer();
			World world = player.getWorld();
			Color teamColor = Main.getPlayerInfo(player).team.getColour();

			//Launching Grenade
			if(mat == Material.FIREWORK_STAR && event.getAction() == Action.RIGHT_CLICK_BLOCK ||
												event.getAction() == Action.RIGHT_CLICK_AIR){
				Location initialPoint = player.getEyeLocation().clone().subtract(0,0.2,0);
				ItemStack grenade = ItemBuilder.of(Material.FIREWORK_STAR).color(teamColor).build();
				Item grenadeDrop = world.dropItem(initialPoint, grenade);
				grenadeDrop.setCanMobPickup(false);
				grenadeDrop.setCanPlayerPickup(false);
				grenadeDrop.setUnlimitedLifetime(true);
				grenadeDrop.setWillAge(false);

				Vector vel = player.getLocation().getDirection().normalize().multiply(0.8);
				grenadeDrop.setVelocity(vel);

			}
		}
	}

	public record GrenadeInfo(Player thrower, Color color, int spawnTime) {}
}

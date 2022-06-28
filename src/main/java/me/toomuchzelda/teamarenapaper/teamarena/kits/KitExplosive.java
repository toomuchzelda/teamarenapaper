package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;
/*
//Kit Explosive:
// Primary: Utility
// Secondary: Ranged

RWF explosive but...

RPG has lower cooldown and lower dmg + Rocket Jump
Grenade has more up-time but cannot be spammed as much

Overall smaller cooldowns and less burst damage, so it has more consistent damage output
 */

/**
 * @author onett425
 */
public class KitExplosive extends Kit{

	public static ItemStack GRENADE;
	public static TextColor ITEM_YELLOW = TextColor.color(255, 241, 120);

	static{
		GRENADE = ItemBuilder.of(Material.FIREWORK_STAR)
				.displayName(ItemUtils.noItalics(Component.text("Grenade")
						.color(ITEM_YELLOW))).build();
	}

	public KitExplosive() {
		super("Explosive", "The classic kit from the golden days... " +
				"but with even more explosive power. And Rocket Jumping.", Material.FIREWORK_STAR);

		ItemStack sword = new ItemStack(Material.STONE_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
		sword.setItemMeta(swordMeta);

		ItemStack rpg = ItemBuilder.of(Material.EGG)
				.displayName(ItemUtils.noItalics(Component.text("RPG")
						.color(ITEM_YELLOW))).build();

		setItems(sword, rpg, GRENADE.asQuantity(5));
		setArmor(new ItemStack(Material.DIAMOND_HELMET), new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.GOLDEN_LEGGINGS), new ItemStack(Material.DIAMOND_BOOTS));
		setAbilities(new ExplosiveAbility());
	}

	public static class ExplosiveAbility extends Ability{

		public static final int RPG_CD = 200;
		//recharge time = time to get +1 grenade back
		//max active = max # of grenades you can have active at a time
		//max in inv = max # of grenades you can have in inventory
		public static final int GRENADE_RECHARGE_TIME = 80;
		public static final int GRENADE_MAX_ACTIVE = 3;
		public static final int GRENADE_MAX_IN_INV = 5;
		public static final int GRENADE_FUSE_TIME = 60;

		public static final LinkedList<GrenadeInfo> ACTIVE_GRENADES = new LinkedList<>();
		public final Map<Player, Integer> GRENADE_RECHARGES = new LinkedHashMap<>();

		@Override
		public void unregisterAbility() {
			ACTIVE_GRENADES.forEach(grenadeInfo -> grenadeInfo.grenade().remove());
			ACTIVE_GRENADES.clear();
			GRENADE_RECHARGES.clear();
		}
		@Override
		public void giveAbility(Player player) {
			GRENADE_RECHARGES.put(player, TeamArena.getGameTick());
		}

		@Override
		public void removeAbility(Player player) {
			GRENADE_RECHARGES.remove(player);
		}

		@Override
		public void onTick() {
			//Handling Grenade Behavior
			ACTIVE_GRENADES.forEach(grenadeInfo -> {
				World world = grenadeInfo.thrower().getWorld();
				Player thrower = grenadeInfo.thrower();
				Item grenade = grenadeInfo.grenade();
				Particle.DustOptions particleOptions = new Particle.DustOptions(grenadeInfo.color(), 1);

				//Explode grenade if fuse time passes
				if(TeamArena.getGameTick() - grenadeInfo.spawnTime >= GRENADE_FUSE_TIME){
					//Only explode if the thrower is still alive
					if(Main.getPlayerInfo(thrower).activeKit != null){
						world.createExplosion(grenade.getLocation(), 1.3f, false, false,
								thrower);
					}
					grenade.remove();
					ACTIVE_GRENADES.remove(grenadeInfo);
				}
				//Grenade particles
				else{
					//Particles for when grenade has landed
					if(grenade.isOnGround()){
						world.spawnParticle(Particle.REDSTONE, grenade.getLocation(),
								1, 0.25, 0.25,0.25, particleOptions);
					}
					//Particles for when grenade is in motion
					else{
						world.spawnParticle(Particle.REDSTONE, grenade.getLocation(),
								1, particleOptions);
					}
				}
			});

			//Handling giving grenades to players
			GRENADE_RECHARGES.forEach((player, lastUsedTick) -> {
				PlayerInventory inv = player.getInventory();
				int grenadeCount = getInvCount(inv, GRENADE);

				if(grenadeCount < GRENADE_MAX_IN_INV &&
						(TeamArena.getGameTick() - lastUsedTick) % GRENADE_RECHARGE_TIME == 0){
					if(inv.getItemInOffHand().isSimilar(GRENADE)){
						inv.getItemInOffHand().add();
					}
					else{
						inv.addItem(GRENADE);
					}
				}
			});
		}
		public int getInvCount(PlayerInventory inv, ItemStack desiredItem){
			ItemStack[] items = inv.getContents();
			int itemCount = 0;
			for(ItemStack item : items){
				if(item != null && item.getType() == desiredItem.getType()){
					itemCount += item.getAmount();
				}
			}
			return itemCount;
		}

		@Override
		public void onPlayerTick(Player player) {
			//Fixing glitch where player can get extra grenades by "hiding" grenades
			//in inventory's crafting menu
			PlayerInventory inv = player.getInventory();
			//Ignore excess grenades if the player is in creative mode and is admin abusing
			if(player.getGameMode() != GameMode.CREATIVE &&
					getInvCount(inv, GRENADE) > GRENADE_MAX_IN_INV){
				Iterator<ItemStack> iter = Arrays.stream(inv.getContents()).iterator();
				while(iter.hasNext() && getInvCount(inv, GRENADE) > GRENADE_MAX_IN_INV){
					ItemStack item = iter.next();
					if(item != null && item.isSimilar(GRENADE)){
						item.subtract(getInvCount(inv, GRENADE) - GRENADE_MAX_IN_INV);
					}
				}
			}
		}

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
			ItemStack item = event.getItem();
			Material mat = event.getMaterial();
			Player player = event.getPlayer();
			World world = player.getWorld();
			Color teamColor = Main.getPlayerInfo(player).team.getColour();

			//Launching Grenade
			if(mat == Material.FIREWORK_STAR && (event.getAction() == Action.RIGHT_CLICK_BLOCK ||
												event.getAction() == Action.RIGHT_CLICK_AIR)){
				//Finding all the currently active grenades that are owned by the current thrower
				List<GrenadeInfo> currActiveGrenades = ACTIVE_GRENADES.stream()
						.filter(grenadeInfo -> grenadeInfo.thrower().equals(player)).toList();

				//Throw grenade if # of active grenades doesn't exceed the cap
				if(player.getGameMode() == GameMode.CREATIVE ||
						currActiveGrenades.size() < GRENADE_MAX_ACTIVE) {
					//Creating the grenade item to be thrown
					PlayerInventory inv = player.getInventory();
					ItemStack grenade = new ItemStack(Material.FIREWORK_STAR);
					FireworkEffectMeta grenadeMeta = (FireworkEffectMeta) grenade.getItemMeta();
					FireworkEffect fireworkColor = FireworkEffect.builder().withColor(teamColor).build();
					grenadeMeta.setEffect(fireworkColor);
					grenade.setItemMeta(grenadeMeta);

					Location initialPoint = player.getEyeLocation().clone().subtract(0, 0.2, 0);
					Item grenadeDrop = world.dropItem(initialPoint, grenade);
					grenadeDrop.setCanMobPickup(false);
					grenadeDrop.setCanPlayerPickup(false);
					grenadeDrop.setUnlimitedLifetime(true);
					grenadeDrop.setWillAge(false);

					//Throwing the grenade and activating it
					Vector vel = player.getLocation().getDirection().normalize().multiply(0.8);
					grenadeDrop.setVelocity(vel);
					world.playSound(grenadeDrop, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.1f);
					ACTIVE_GRENADES.add(new GrenadeInfo(grenadeDrop, player, teamColor, TeamArena.getGameTick()));

					//Remove grenade from Inventory after it is thrown
					if(inv.getItemInMainHand().isSimilar(GRENADE)){
						inv.setItemInMainHand(item.subtract());
					}
					else{
						inv.setItemInOffHand(item.subtract());
					}
				}
				else{
					player.sendMessage(Component.text("Only " + GRENADE_MAX_ACTIVE +
							" Grenades may be active at once!").color(ITEM_YELLOW));
				}
			}
		}
	}

	public record GrenadeInfo(Item grenade, Player thrower, Color color, int spawnTime) {}
}

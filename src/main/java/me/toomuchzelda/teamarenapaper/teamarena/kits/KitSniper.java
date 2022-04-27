package me.toomuchzelda.teamarenapaper.teamarena.kits;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * @author onett425
 */
public class KitSniper extends Kit {
	public static final ItemStack GRENADE;
	public static final ItemStack SNIPER;
	public static final Set<BukkitTask> GRENADE_TASKS = new HashSet<>();
	
	public static final AttributeModifier KNIFE_SPEED = new AttributeModifier("Sniper Knife", 0.2, //20% = speed 1
			AttributeModifier.Operation.ADD_SCALAR);
	
	static{
		GRENADE = new ItemStack(Material.TURTLE_HELMET);
		ItemMeta grenadeMeta = GRENADE.getItemMeta();
		grenadeMeta.displayName(ItemUtils.noItalics(Component.text("Frag Grenade")));
		GRENADE.setItemMeta(grenadeMeta);
		
		SNIPER = new ItemStack(Material.SPYGLASS);
		ItemMeta rifleMeta = SNIPER.getItemMeta();
		rifleMeta.displayName(ItemUtils.noItalics(Component.text("CheyTac Intervention")));
		SNIPER.setItemMeta(rifleMeta);
	}
	public KitSniper() {
		super("Sniper", "Be careful when sniping... Too much movement and your aim will worsen. Don't forget to throw the grenade if you pull the pin btw.", Material.SPYGLASS);
		
		ItemStack[] armour = new ItemStack[4];
		armour[3] = new ItemStack(Material.LEATHER_HELMET);
		armour[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
		armour[1] = new ItemStack(Material.LEATHER_LEGGINGS);
		armour[0] = new ItemStack(Material.LEATHER_BOOTS);
		this.setArmour(armour);
		
		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		
		setItems(sword, SNIPER, GRENADE);
		setAbilities(new SniperAbility());
	}
	
	public static class SniperAbility extends Ability{
		private final Set<Player> RECEIVED_SNIPER_CHAT_MESSAGE = new HashSet<>();
		private final Set<Player> RECEIVED_GRENADE_CHAT_MESSAGE = new HashSet<>();
		public static final TextColor SNIPER_MSG_COLOR = TextColor.color(89, 237, 76);
		public static final TextColor GRENADE_MSG_COLOR = TextColor.color(66, 245, 158);
		
		@Override
		public void unregisterAbility() {
			Iterator<BukkitTask> iter = GRENADE_TASKS.iterator();
			while(iter.hasNext()) {
				BukkitTask task = iter.next();
				task.cancel();
				iter.remove();
			}
		}
		
		@Override
		public void giveAbility(Player player) {
			player.setExp(0.999f);
		}
		
		@Override
		public void removeAbility(Player player) {
			player.setExp(0);
			player.getInventory().remove(Material.TURTLE_HELMET);
			player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(KNIFE_SPEED);
			RECEIVED_SNIPER_CHAT_MESSAGE.remove(player);
			RECEIVED_GRENADE_CHAT_MESSAGE.remove(player);
		}
		
		public void throwGrenade(Player player, double amp, int itemSlot){
			World world = player.getWorld();
			BukkitTask runnable = new BukkitRunnable(){
				//Grenade explosion
				int timer = player.getCooldown(Material.TURTLE_HELMET);
				boolean launched = false;
				Item activeGrenade;
				final TeamArenaTeam team = Main.getPlayerInfo(player).team;
				Color teamColor = team.getColour();
				public void run() {
					if(launched){
						//Grenade Particles when it is thrown
						//In Motion
						if(activeGrenade.getVelocity().length() > 0){
							world.spawnParticle(Particle.REDSTONE, activeGrenade.getLocation(), 1, new Particle.DustOptions(teamColor, 2f));
						}
						else{
							//On the ground
							world.spawnParticle(Particle.REDSTONE, activeGrenade.getLocation().add(Vector.getRandom().subtract(new Vector(-0.5,-0.5,-0.5)).multiply(4)), 2, new Particle.DustOptions(teamColor, 2f));
						}
					}
					if(timer <= 0){
						//Grenade Success
						if(launched){
							world.createExplosion(activeGrenade.getLocation(), 1.7f, false, false);
							player.getInventory().addItem(GRENADE);
							activeGrenade.remove();
						}
						cancel();
						GRENADE_TASKS.remove(this);
					}
					else if(!launched){
						//Grenade Throw Initiated
						if(!player.getInventory().contains(Material.TURTLE_HELMET)){
							activeGrenade = world.dropItem(player.getLocation(), new ItemStack(Material.TURTLE_HELMET));
							activeGrenade.setCanPlayerPickup(false);
							activeGrenade.setCanMobPickup(false);
							Vector direction = player.getLocation().getDirection();
							activeGrenade.setVelocity(direction.multiply(amp));
							launched = true;
							world.playSound(activeGrenade.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1f, 1.1f);
						}
					}
					timer--;
				}
			}.runTaskTimer(Main.getPlugin(), 0, 0);
			GRENADE_TASKS.add(runnable);
		}
		
		@Override
		public void onInteract(PlayerInteractEvent event) {
			Material mat = event.getMaterial();
			Player player = event.getPlayer();
			World world = player.getWorld();
			PlayerInventory inv = player.getInventory();
			Action action = event.getAction();
			PlayerInfo pinfo = Main.getPlayerInfo(player);
			
			//Grenade Pull Pin
			if(mat == Material.TURTLE_HELMET && !player.hasCooldown(Material.TURTLE_HELMET) && player.getExp() == 0.999f && player.getInventory().getItemInMainHand().getType() == Material.TURTLE_HELMET){
				Component actionBar = Component.text("Left Click to THROW    Right Click to TOSS").color(TextColor.color(242, 44, 44));
				Component text = Component.text("Left Click to throw the grenade, Right Click to lightly toss it").color(TextColor.color(242, 44, 44));
				if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					player.sendMessage(text);
				}
				player.setExp(0);
				player.setCooldown(Material.TURTLE_HELMET, (int) 3.5 * 20);
				world.playSound(player, Sound.ITEM_FLINTANDSTEEL_USE, 2.0f, 1.5f);
			}
			//Grenade Throw
			//Main Hand ONLY
			else if(mat == Material.TURTLE_HELMET && player.hasCooldown(Material.TURTLE_HELMET) && player.getInventory().getItemInMainHand().getType() == Material.TURTLE_HELMET){
				//Left Click => Hard Throw
				if(action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)){
					//Removes 1 grenade from hand
					inv.setItem(inv.getHeldItemSlot(), inv.getItem(inv.getHeldItemSlot()).subtract());
					throwGrenade(player, 1.5d, player.getInventory().getHeldItemSlot());
				}
				//Right Click => Soft Toss
				if(action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)){
					inv.setItem(inv.getHeldItemSlot(), inv.getItem(inv.getHeldItemSlot()).subtract());
					throwGrenade(player, 0.8d, player.getInventory().getHeldItemSlot());
				}
			}
		}
		
		//Headshot
		@Override
		public void projectileHitEntity(ProjectileCollideEvent event){
			Projectile projectile = event.getEntity();
			Entity victim = event.getCollidedWith();
			Player shooter = (Player) projectile.getShooter();
			if(victim instanceof Player player && projectile.getType() == EntityType.ARROW){
				double headLocation = player.getLocation().getY();
				double projectileHitY = projectile.getLocation().getY();
				//Must consider when player is below the other player, which makes getting headshots much harder.
				double headshotThresh = 1.35d;
				double heightDiff = victim.getLocation().getBlockY() - shooter.getLocation().getBlockY();
				if(heightDiff > 0){
					headshotThresh -= Math.min(0.35, (heightDiff/10));
				}
				//Disabled headshot if you are too close since it was buggy
				if(projectileHitY - headLocation > headshotThresh && projectile.getOrigin().distance(projectile.getLocation()) > 10){
					player.damage(999);
					//Hitmarker Sound effect
					//shooter.playSound(shooter.getLocation(), Sound.ENTITY_ITEM_FRAME_PLACE, 2f, 2.0f);
				}
			}
		}
		//Sniper Rifle Shooting
		public void playerDropItem(PlayerDropItemEvent event) {
			Player player = event.getPlayer();
			Item item = event.getItemDrop();
			World world = player.getWorld();
			item.setOwner(player.getUniqueId());
			if(item.getItemStack().getType() == Material.SPYGLASS && !player.hasCooldown(Material.SPYGLASS)){
				//Inaccuracy based on movement
				//player.getVelocity() sux so i will base movement on the player's state
				Location loc = player.getLocation();
				Vector direction = loc.getDirection();
				double inaccuracy = 0;
				if(player.isSprinting() || player.isGliding() || player.isJumping() ||
						loc.subtract(0,1,0).getBlock().getType() == Material.AIR ||
						player.getVelocity().lengthSquared() > 1){
					inaccuracy = 0.2;
				}
				if(player.isInWater() || player.isSwimming()){
					inaccuracy = 0.1;
				}
				direction.add(new Vector(Math.random() * inaccuracy - inaccuracy, Math.random() * inaccuracy - inaccuracy, Math.random() * inaccuracy - inaccuracy));
				direction.multiply(10d);
				//Shooting Rifle + Arrow Properties
				Arrow arrow = player.launchProjectile(Arrow.class, direction);
				arrow.setShotFromCrossbow(true);
				arrow.setGravity(false);
				arrow.setKnockbackStrength(1);
				arrow.setCritical(false);
				arrow.setPickupStatus(PickupStatus.DISALLOWED);
				arrow.setPierceLevel(100);
				arrow.setDamage(1);
				world.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 2.5f);
				
				//Sniper Cooldown + deleting the dropped sniper and returning a new one.
				player.setCooldown(Material.SPYGLASS, (int) (2.5*20));
				item.remove();
				//If selected slot is occupied, find next available slot and add.
				event.setCancelled(true);
                /*if(player.getInventory().getItem((player.getInventory().getHeldItemSlot())) != null){
                    player.getInventory().setItem(player.getInventory().firstEmpty(), SNIPER);
                }
                else{
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), SNIPER);
                }*/
			}
			else{
				event.setCancelled(true);
			}
		}
		
		@Override
		public void onPlayerTick(Player player) {
			float exp = player.getExp();
			World world = player.getWorld();
			
			//Grenade Cooldown
			if(exp == 0.999f){
			
			}
			else if(exp + 0.005f >= 1){
				player.setExp(0.999f);
			}
			else{
				player.setExp(exp + 0.005f);
			}
			
			//Speed based on held item
			AttributeInstance instance = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
			if(player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD){
				//player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 123123, 0));
				if(!instance.getModifiers().contains(KNIFE_SPEED)) {
					instance.addModifier(KNIFE_SPEED);
				}
			}
			else{
				if(instance.getModifiers().contains(KNIFE_SPEED)){
					instance.removeModifier(KNIFE_SPEED);
				}
			}
			
			//Sniper Information message
			if(player.getInventory().getItemInMainHand().getType() == Material.SPYGLASS){
				Component actionBar = Component.text("Drop Spyglass in hand to shoot").color(SNIPER_MSG_COLOR);
				Component text = Component.text("Drop Spyglass in your main hand to shoot").color(SNIPER_MSG_COLOR);
				PlayerInfo pinfo = Main.getPlayerInfo(player);
				if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				//Chat Message is only sent once per life
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES) && !RECEIVED_SNIPER_CHAT_MESSAGE.contains(player)) {
					player.sendMessage(text);
					RECEIVED_SNIPER_CHAT_MESSAGE.add(player);
				}
			}
			
			//Grenade Information message
			if(player.getInventory().getItemInMainHand().getType() == Material.TURTLE_HELMET && player.getExp() == 0.999f){
				
				Component actionBar = Component.text("Left/Right Click to Arm").color(GRENADE_MSG_COLOR);
				Component text = Component.text("Click to arm the grenade").color(GRENADE_MSG_COLOR);
				PlayerInfo pinfo = Main.getPlayerInfo(player);
				if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				//Chat Message is only sent once per life
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES) && !RECEIVED_GRENADE_CHAT_MESSAGE.contains(player)) {
					player.sendMessage(text);
					RECEIVED_GRENADE_CHAT_MESSAGE.add(player);
				}
			}
			
			//Grenade Fail Check
			//Check if inventory has any grenades, maybe update later to allow for admin abuse grenade spam
			ItemStack helmet = player.getEquipment().getHelmet();
			boolean wearingTurtleOnHead = false;
			if(helmet != null && helmet.getType() == Material.TURTLE_HELMET) {
				wearingTurtleOnHead = true;
			}
			if(player.getCooldown(Material.TURTLE_HELMET) == 1 && player.getInventory().contains(Material.TURTLE_HELMET) || wearingTurtleOnHead){
				if(wearingTurtleOnHead){
					Component text = Component.text("Please do not wear the grenade on your head. Thank you.").color(GRENADE_MSG_COLOR);
					player.sendMessage(text);
				}
				
				world.createExplosion(player.getLocation(), 2.5f, false, false);
				player.damage(999);
			}
			//Sniper Reload Sound
			if(player.getCooldown(Material.SPYGLASS) == 15){
				player.playSound(player, Sound.ITEM_ARMOR_EQUIP_CHAIN, 2f, 0.8f);
			}
		}
	}
}

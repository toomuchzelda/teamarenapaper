package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.Particle.DustTransition;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

//Kit Description:
/*
    Kit Goal: Initiator / Fighter kit w/ cool melee ability

    Passive: Regen on kill

    Main Ability: Spin Attack
        After a short windup, hit all enemies within a short radius
        Damage based on distance from player

	Sub Ability: Gravity Bomb
		Charge CD: ~10 seconds
        Detonation Time: ~1.5 seconds

        Launches a Heart of the Sea like an explosive grenade,
        which will detonate after a short fuse time.
        (Can be thrown with low or high velocity depending on type of click)

	    Gravity Bomb Detonation
            Upon detonation, the bomb will pull enemies towards itself
*/
/**
 * @author onett425
 */
public class KitValkyrie extends Kit {
	public static final Material VALK_AXE_MAT = Material.GOLDEN_AXE;
	public static final int AXE_WINDUP = 10;
	public static final int AXE_CD = 120;
	public static final int ATTACK_RADIUS = 3;

	//Bonus damage adds extra sharpness levels to the given hit
	public static final int NORMAL_BONUS = 1;
	public static final int SWEET_SPOT_BONUS = 3;
	public static final ItemStack VALK_AXE = ItemBuilder.of(VALK_AXE_MAT)
			.displayName(Component.text("Battle Axe"))
			.lore(Component.text("Right click to activate your Cleave ability!", TextColors.LIGHT_YELLOW),
					Component.text("After a short windup (exp bar), Battle Axe hits all nearby enemies, ", TextColors.LIGHT_YELLOW),
					Component.text("and deals more damage the further they are from you!", TextColors.LIGHT_YELLOW),
					Component.text("(Battle Axe Must be kept in hand during wind-up)", TextColors.LIGHT_BROWN),
					Component.text("Cooldown: " + AXE_CD/20 + " seconds", TextColors.LIGHT_BROWN),
					Component.text("Direct Attacks are disabled while ability is active.", TextColors.ERROR_RED))
			//.enchant(Enchantment.DAMAGE_ALL, AXE_SHARP)
			//.enchant(Enchantment.KNOCKBACK, 1)
			.hide(ItemFlag.HIDE_ATTRIBUTES)
			.build();

	public static final ItemStack GRAV_BOMB = ItemBuilder.of(Material.HEART_OF_THE_SEA)
			.displayName(Component.text("Gravity Bomb"))
			.lore(Component.text("Pulls enemies towards itself upon detonation", TextColors.LIGHT_YELLOW),
					KitSniper.BOMB_LORE,
					KitSniper.BOMB_LORE2)
			.build();
	public static final int BOMB_CD = 10 * 20;
	public static final int DETONATION_TIME = 30;
	public static final double PULL_AMP = 0.2;

	public static final Set<BukkitTask> VALK_TASKS = new HashSet<>();

	public KitValkyrie(){
		super("Valkyrie", "A tanky melee kit that excels in group combat. " +
						"It can easily destroy large groups of enemies with its Cleave ability and Gravity bomb! " +
						"As a reward for your hard work, you gain Regeneration for every kill!",
				Material.DIAMOND_AXE);

		setArmor(new ItemStack(Material.IRON_HELMET), new ItemStack(Material.DIAMOND_CHESTPLATE),
				new ItemStack(Material.CHAINMAIL_LEGGINGS), new ItemStack(Material.IRON_BOOTS));
		setItems(VALK_AXE, GRAV_BOMB);
		setAbilities(new ValkyrieAbility());

		setCategory(KitCategory.FIGHTER);
	}

	public static class ValkyrieAbility extends Ability {
		public static final HashMap<Player, Integer> ACTIVE_WINDUP = new HashMap<>();
		public final HashMap<Player, Integer> BOMB_RECHARGES = new HashMap<>();
		private final Set<Player> RECEIVED_AXE_CHAT_MESSAGE = new HashSet<>();
		public static final Component AXE_ATTACK_CANCEL_MESSAGE = Component.text("Attacks cannot be dealt while Cleave is Active!", TextColors.ERROR_RED);

		public static final int REGEN_DURATION_ON_KILL = 75;
		//public static final double MAX_AXE_DMG = 10.0;
		public void unregisterAbility() {
			Iterator<BukkitTask> iter = VALK_TASKS.iterator();
			while(iter.hasNext()) {
				BukkitTask task = iter.next();
				task.cancel();
				iter.remove();
			}
		}

		public void removeAbility(Player player) {
			player.setExp(0);
			player.removePotionEffect(PotionEffectType.REGENERATION);
			ACTIVE_WINDUP.remove(player);
			BOMB_RECHARGES.remove(player);
			RECEIVED_AXE_CHAT_MESSAGE.remove(player);
		}

		public void onAttemptedAttack(DamageEvent event) {
			Player player = (Player) event.getAttacker();
			ItemStack item = player.getInventory().getItemInMainHand();

			if(item.getType() == VALK_AXE_MAT) {
				//Cancelling attacks attempted during Cleave wind-up
				if(ACTIVE_WINDUP.containsKey(player)) {
					event.setCancelled(true);

					if(RECEIVED_AXE_CHAT_MESSAGE.add(player)) {
						player.sendMessage(AXE_ATTACK_CANCEL_MESSAGE);
					}
				}
			}
		}

		@Override
		public void onAssist(Player player, double assist, Player victim) {
			//double currKills = Main.getPlayerInfo(player).kills;
			//double priorKills = Main.getPlayerInfo(player).kills - assist;

			//Bonus DMG on Kill
			//Bonus DMG stops at MAX_AXE_DMG
			/*
			if(Math.floor(currKills) - Math.floor(priorKills) >= 1 &&
					Math.floor(currKills) * 0.25 + 7.0 <= MAX_AXE_DMG) {

				AttributeModifier axeBonus = new AttributeModifier(UUID.randomUUID(),"Valk Axe Bonus Dmg",
						0.25, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);

				player.getInventory().all(VALK_AXE_MAT).forEach((slot, item) -> {
					ItemMeta axeMeta = item.getItemMeta();
					axeMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, axeBonus);
					item.setItemMeta(axeMeta);
					Bukkit.broadcast(Component.text(item.getItemMeta().getAttributeModifiers().toString()));
				});

				String newDmg = TextUtils.TWO_DECIMAL_POINT.format(axeBonus.getAmount() + 7.0);
				String newDmgHearts = TextUtils.TWO_DECIMAL_POINT.format((axeBonus.getAmount() + 7.0) / 2.0) + " ♥";
				player.sendMessage(Component.text(
						"Your axe grows sharper... Current Damage: " + newDmg + " (" + newDmgHearts + ")", TextColors.LIGHT_YELLOW));
			}
			 */

			//Regen on Kill or Substantial Assist
			if(assist >= 0.7) {
				if(player.hasPotionEffect(PotionEffectType.REGENERATION)) {
					int currDuration = player.getPotionEffect(PotionEffectType.REGENERATION).getDuration();
					player.addPotionEffect(
							PotionEffectType.REGENERATION.createEffect(
									REGEN_DURATION_ON_KILL + currDuration, 2));
				}
				else {
					player.addPotionEffect(
							PotionEffectType.REGENERATION.createEffect(
									REGEN_DURATION_ON_KILL, 2));
				}
			}
		}

		public void onPlayerTick(Player player) {

			//Handling display of valk axe uptime
			float percentInc = (float) 1 / (float) AXE_CD;
			float percentDec = (float) 1 / (float) AXE_WINDUP;
			float newPercent;

			//if Valk is not currently winding up, slowly fill exp bar
			if(!ACTIVE_WINDUP.containsKey(player)) {
				newPercent = player.getExp() + percentInc;
			}
			//if Valk is currently Winding up, deplete exp bar
			else {
				newPercent = player.getExp() - percentDec;
			}

			newPercent = MathUtils.clamp(0f, 1f, newPercent);
			player.setExp(newPercent);

			//Gravity Bomb Action Bar
			if(Main.getPlayerInfo(player).getPreference(Preferences.KIT_ACTION_BAR) &&
					player.getInventory().getItemInMainHand().getType() == Material.HEART_OF_THE_SEA){
				Component actionBar = Component.text("Left Click to THROW    Right Click to TOSS").color(TextColor.color(3, 182, 252));
				player.sendActionBar(actionBar);
			}
		}

		public void onTick() {
			//Stolen from toomuchzelda, manages giving out gravity bombs
			int currentTick = TeamArena.getGameTick();
			var itemIter = BOMB_RECHARGES.entrySet().iterator();
			while(itemIter.hasNext()) {
				Map.Entry<Player, Integer> entry = itemIter.next();
				if(currentTick - entry.getValue() >= BOMB_CD){
					entry.getKey().getInventory().addItem(GRAV_BOMB);
					itemIter.remove();
				}
			}

			//Handling Cleave Ability
			var axeIter = ACTIVE_WINDUP.entrySet().iterator();
			while(axeIter.hasNext()) {
				Map.Entry<Player, Integer> entry = axeIter.next();
				Player player = entry.getKey();
				int initTick = entry.getValue();

				//If Axe is removed from hand, cancel the attack
				if(player.getInventory().getItemInMainHand().getType() != VALK_AXE_MAT) {
					player.setExp(0f);
					axeIter.remove();

					//Remove Enchantment that indicates wind-up is in progress
					PlayerInventory inv = player.getInventory();
					inv.all(VALK_AXE_MAT).forEach(
							(k, v) -> { v.removeEnchantment(Enchantment.KNOCKBACK);
							});
					if(inv.getItemInOffHand().getType() == VALK_AXE_MAT) {
						inv.getItemInOffHand().removeEnchantment(Enchantment.KNOCKBACK);
					}
				}
				//Else, check if Windup was succesful, if so, proceed with Cleave Attack
				else if(TeamArena.getGameTick() - initTick >= AXE_WINDUP) {
					cleaveAttack(player);
					player.setExp(0f);
					axeIter.remove();
				}
			}

			//clean up cancelled or ended bukkit tasks
			VALK_TASKS.removeIf(BukkitTask::isCancelled);
		}

		public void onInteract(PlayerInteractEvent event) {
			Player player = event.getPlayer();
			PlayerInventory inv = player.getInventory();
			Material mat = event.getMaterial();
			Action action = event.getAction();

			//Initiates Cleave upon Right Click
			if(action.isRightClick() && mat == VALK_AXE_MAT && player.getExp() >= 1f) {
				windUp(player);
			}

			//Throwing Gravity Bombs
			if(mat == Material.HEART_OF_THE_SEA) {
				//inv.setItem(inv.getHeldItemSlot(), inv.getItem(inv.getHeldItemSlot()).subtract());
				player.getEquipment().setItem(event.getHand(), event.getItem().subtract());
				if(action.isLeftClick()){
					throwGrenade(player, 1.5d);
				}
				else if(action.isRightClick()) {
					throwGrenade(player, 0.8d);
				}
			}
		}

		//When Cleave is activated, wind-up period must be completed to initiate an attack
		public void windUp(Player player) {

			ACTIVE_WINDUP.put(player, TeamArena.getGameTick());
			World world = player.getWorld();
			world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.78f);

			//Add indicator enchantment to show that wind-up is in progress
			PlayerInventory inv = player.getInventory();
			inv.all(VALK_AXE_MAT).forEach(
					(k, v) -> v.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1));

			if(inv.getItemInOffHand().getType() == VALK_AXE_MAT) {
				inv.getItemInOffHand().addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
			}
		}

		//An AOE attack that follows a succesful windup, hits all enemies within an area, applies bonus damage based on distance
		public void cleaveAttack(Player player){
			List<Entity> nearbyEnt = player.getNearbyEntities(ATTACK_RADIUS, ATTACK_RADIUS, ATTACK_RADIUS);
			Location playerLoc = player.getLocation();
			ItemStack item = player.getInventory().getItemInMainHand();
			ItemMeta meta = item.getItemMeta();
			World world = player.getWorld();
			Iterator<Entity> iter = nearbyEnt.iterator();
			while(iter.hasNext()) {
				Entity entity = iter.next();
				double distance = entity.getLocation().distance(playerLoc);
				if (entity instanceof LivingEntity victim &&
						!victim.getType().equals(EntityType.ARMOR_STAND) &&
						distance <= ATTACK_RADIUS){

					Vector attackerToVictim = victim.getEyeLocation().subtract(player.getEyeLocation()).toVector().normalize();
					//Raytrace checks for blocks between attacker and victim,
					//if collidable block is found, the attack is cancelled
					RayTraceResult rayTrace = null;
					//When distance = 0, rayTrace throws exception (i.e. you TP to someone and are in the exact same spot)
					if(distance != 0) {
						rayTrace = player.getWorld().rayTraceBlocks(
								player.getEyeLocation(), attackerToVictim, distance,
								FluidCollisionMode.NEVER, true);
					}

					if(rayTrace == null){

						//If distance is < 1, it is a "sour hit" and no bonus damage is given
						if (distance < 1) {

						} else if (distance < 2.6 && meta != null) {
							meta.addEnchant(Enchantment.DAMAGE_ALL, NORMAL_BONUS, true);
						} else if (distance >= 2.6 && meta != null) {
							meta.addEnchant(Enchantment.DAMAGE_ALL, SWEET_SPOT_BONUS, true);
							player.playSound(player, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.9f);
						}

						if(meta != null) {
							item.setItemMeta(meta);
						}
						player.attack(victim);
					}
				}
				iter.remove();
			}
			axeParticles(player);
			world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);

			PlayerInventory inv = player.getInventory();
			inv.all(VALK_AXE_MAT).forEach(
					(k, v) -> { v.removeEnchantment(Enchantment.KNOCKBACK);
								v.removeEnchantment(Enchantment.DAMAGE_ALL);
					});
			if(inv.getItemInOffHand().getType() == VALK_AXE_MAT) {
				inv.getItemInOffHand().removeEnchantment(Enchantment.KNOCKBACK);
				inv.getItemInOffHand().removeEnchantment(Enchantment.DAMAGE_ALL);
			}
		}

		//Particle effect to show the effective range of valkAttack
		public void axeParticles(Player player){
			Location loc = player.getLocation();
			loc.add(0, 1, 0);
			World world = player.getWorld();
			final TeamArenaTeam team = Main.getPlayerInfo(player).team;
			Color teamColor = team.getColour();
			DustTransition particle = new DustTransition(teamColor, Color.WHITE, 2);
			BukkitTask runnable = new BukkitRunnable() {

				int duration = 4;
				int increment = 0;

				public void run() {
					if(increment >= duration){
						cancel();
					}
					else{
						for(int i = 0; i < 10; i++){
							double rad = (increment * Math.PI / 2) + (i * Math.PI / (10 * 2));
							double radius = 3;
							loc.add(radius * Math.cos(rad), 0, radius * Math.sin(rad));
							world.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0,0,0, 5.0f, particle);
							loc.subtract(radius * Math.cos(rad), 0, radius * Math.sin(rad));
						}
						increment++;


					}

				}
			}.runTaskTimer(Main.getPlugin(), 0, 0);
			VALK_TASKS.add(runnable);
		}

		public void throwGrenade(Player player, double amp){
			World world = player.getWorld();
			Location location = player.getEyeLocation();
			final TeamArenaTeam team = Main.getPlayerInfo(player).team;
			Color teamColor = team.getColour();

			Item activeGrenade = world.dropItem(location, new ItemStack(Material.HEART_OF_THE_SEA), item -> {
				item.setCanPlayerPickup(false);
				item.setCanMobPickup(false);
				item.setVelocity(location.getDirection().multiply(amp));
			});
			world.playSound(activeGrenade, Sound.ENTITY_CREEPER_PRIMED, 1f, 1.1f);

			BukkitTask runnable = new BukkitRunnable() {
				//Grenade explosion
				int timer = DETONATION_TIME;
				DustTransition particle = new DustTransition(teamColor, Color.WHITE, 2);
				public void run() {
					//Grenade Particles when it is thrown
					//In Motion
					if(activeGrenade.getVelocity().length() > 0){
						world.spawnParticle(Particle.REDSTONE, activeGrenade.getLocation(), 1, new Particle.DustOptions(teamColor, 2f));
						world.spawnParticle(Particle.ENCHANTMENT_TABLE, activeGrenade.getLocation(), 8, 0,0,0, 0.5f);
					}
					else{
						//On the ground
						world.spawnParticle(Particle.DUST_COLOR_TRANSITION, activeGrenade.getLocation().add(Vector.getRandom().subtract(new Vector(-0.5,-0.5,-0.5)).multiply(4)), 2, particle);
						world.spawnParticle(Particle.ENCHANTMENT_TABLE, activeGrenade.getLocation(), 20,  1,1,1, 1.5f);
					}
					timer--;
					if(timer <= 0){
						player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.3f);
						//Pull-in + Explosion
						BukkitTask runnable = new BukkitRunnable(){

							List<Entity> affectedEnemies = activeGrenade.getNearbyEntities(5, 5, 5);
							int duration = 5;
							Location currLoc = activeGrenade.getLocation().clone();

							public void run(){
								if(duration <= 0){
									//Explosion
									world.createExplosion(activeGrenade.getLocation(), 0.2f, false, false);
									player.stopSound(Sound.ENTITY_GENERIC_EXPLODE);
									player.getWorld().stopSound(SoundStop.named(Sound.ENTITY_GENERIC_EXPLODE));
									activeGrenade.remove();
									BOMB_RECHARGES.put(player, TeamArena.getGameTick());
									cancel();
								}
								else{
									for(Entity entity : affectedEnemies){
										//Pull-in
										if(entity.getLocation().distanceSquared(activeGrenade.getLocation()) <= 25 &&
												entity instanceof org.bukkit.entity.LivingEntity victim && !(entity.getType().equals(EntityType.ARMOR_STAND))){
											if(!(victim instanceof Player p) || Main.getGame().canAttack(player, p)) {
												//Preventing spectators from getting pulled in
												if(victim instanceof Player p && Main.getPlayerInfo(p).activeKit != null){
													//Vector currVel = entity.getVelocity().clone();
													Vector nadeLoc = activeGrenade.getLocation().clone().toVector();
													Vector entLoc = entity.getLocation().clone().toVector();
													entity.setVelocity(nadeLoc.subtract(entLoc).multiply(new Vector(PULL_AMP, PULL_AMP/8, PULL_AMP)));
												}
											}
										}
									}

									//Particle Effect, Shows the max radius of grenade pull then shrinks
									//duration * (grenadeRadius / 5.0);
									double radius = duration * (5.0/5.0);
									for(int i = 0; i < 10; i++){
										Location locClone = currLoc.clone();
										double rad = i * (2 * Math.PI) / 10;
										double x = radius * Math.sin(rad);
										double z = radius * Math.cos(rad);
										locClone.add(x, 0, z);
										world.spawnParticle(Particle.REDSTONE, locClone, 1, new Particle.DustOptions(teamColor, 2f));
									}
									duration--;
								}
							}
						}.runTaskTimer(Main.getPlugin(), 0, 0);
						VALK_TASKS.add(runnable);

						cancel();
					}
				}
			}.runTaskTimer(Main.getPlugin(), 0, 0);
			VALK_TASKS.add(runnable);
		}
	}
}

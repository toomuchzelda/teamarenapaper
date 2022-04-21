package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

//Kit Description:
/*
	Main Ability: Poison Attack
		Deals +2 seconds of poison on hit which caps at 4 seconds
	Sub Ability: Toxic Leap
		CD: 12 sec
		Deals 1.5 DMG (~1 Heart to Full Iron), 
		Each enemy hit during the dash receives +2 sec Poison,
		CD is reduced by 6 sec per enemy hit.
		
		Poison Duration Cap of 4 Seconds is still respected by Toxic Leap
*/

/**
 * @author onett425
 */
public class KitVenom extends Kit
{
	public static final HashMap<LivingEntity, Integer> POISONED_ENTITIES = new HashMap<>();
	//keep track of all the bukkit tasks so we can cancel them in the event of some disaster or crash
	public static final Set<BukkitTask> LEAP_TASKS = new HashSet<>();
	public static final TextColor POISON_PURP = TextColor.color(145, 86, 204);
	
	public KitVenom() {
		super("Venom", "Poison dmg on hit, poisoned people cannot be healed. It can also quickly jump in, afflicting all enemies it hits with poison and decreasing its cooldown with each enemy hit!", 
				Material.POTION);
		this.setArmor(new ItemStack(Material.CHAINMAIL_HELMET), new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));
		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
			swordMeta.displayName(ItemUtils.noItalics(Component.text("Poison Sword")));
			swordMeta.addEnchant(Enchantment.SOUL_SPEED, 1, true);
			swordMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		List<Component> lore = new ArrayList<Component>();
			TextComponent message = (TextComponent) ItemUtils.noItalics(Component.text("Poison I", TextColor.color(170, 170, 170)));
			lore.add(message);
			swordMeta.lore(lore);
		sword.setItemMeta(swordMeta);

		ItemStack leap = new ItemStack(Material.CHICKEN);
		ItemMeta leapMeta = leap.getItemMeta();
		leapMeta.displayName(ItemUtils.noItalics(Component.text("Toxic Leap")).color(POISON_PURP));
		leap.setItemMeta(leapMeta);

		setItems(sword, leap);
		setAbilities(new VenomAbility());
	}
	
	public static class VenomAbility extends Ability
	{
		//clean up
		public void unregisterAbility() {
			POISONED_ENTITIES.clear();
			
			Iterator<BukkitTask> iter = LEAP_TASKS.iterator();
			while(iter.hasNext()) {
				BukkitTask task = iter.next();
				task.cancel();
				iter.remove();
			}
		}
		
		//When Poison is applied
		public void applyPoison(LivingEntity victim){
			int poisonDuration = 0;
			if(victim.hasPotionEffect(PotionEffectType.POISON)){
				poisonDuration = victim.getPotionEffect(PotionEffectType.POISON).getDuration();
			}
			//At level 1, Poison deals damage every 25 ticks.
			if(poisonDuration <= 4 * 25){
				if(poisonDuration > 2 * 25){
					victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 4 * 25, 0));
				}
				else{
					victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 2 * 25 + poisonDuration, 0));
				}					
			}
			//Adds new victim, or updates current vicim's poison duration.
			//Extra check is necessary since some mobs are immune to poison.
			if(victim.hasPotionEffect(PotionEffectType.POISON)){
				POISONED_ENTITIES.put(victim, (Integer) victim.getPotionEffect(PotionEffectType.POISON).getDuration());			
			}
		}

		//Leap Ability
		@Override
		public void onInteract(PlayerInteractEvent event) {
			Player player = event.getPlayer();
			if(event.getMaterial() == Material.CHICKEN) {
				if(Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player)) {
					Component cannotUseAbilityMsg = Component.text("You can't use Toxic Leap while holding the flag!").color(TextColor.color(255, 98, 20));
					player.sendMessage(cannotUseAbilityMsg);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 2, 0.5f);
				}
				else if(!player.hasCooldown(Material.CHICKEN)) {
					//Leap Ability Implementation
					World world = player.getWorld();
					Vector direction = player.getLocation().getDirection();
					Vector multiplier = new Vector(1.0, 0.5, 1.0);
					multiplier.multiply(1.5);
					direction.multiply(multiplier);
					player.setCooldown(Material.CHICKEN, 12 * 20);
					//world.playSound to play sound for all playesr
					world.playSound(player, Sound.ENTITY_WITHER_SHOOT, 0.3f, 1.1f);
					EntityUtils.setVelocity(player, event.getPlayer().getVelocity().add(direction));
					player.setFallDistance(0);
					
					//Checking for collision during the leap, and reducing cooldown + applying poison accordingly
					//keeps track of whose already been hit with leapVictims
					if (player.getVelocity().length() > 0.8) {
						BukkitTask runnable = new BukkitRunnable()
						{
							int activeDuration = 10;
							Set<LivingEntity> leapVictims = new HashSet<>();
							
							public void run() {
								if (activeDuration <= 0) {
									cancel();
									LEAP_TASKS.remove(this);
								}
								else {
									activeDuration--;
									List<Entity> nearby = player.getNearbyEntities(1, 2, 1);
									for (Entity entity : nearby) {
										if (entity instanceof LivingEntity && !leapVictims.contains(entity) && !(entity.getType().equals(EntityType.ARMOR_STAND))) {
											//Applying DMG + Sounds
											LivingEntity victim = (LivingEntity) entity;
											int newCooldown = player.getCooldown(Material.CHICKEN) - 6 * 20;
											if (newCooldown <= 0) {
												newCooldown = 0;
												player.stopSound(Sound.BLOCK_CONDUIT_ACTIVATE);
												player.playSound(player, Sound.BLOCK_CONDUIT_ACTIVATE, 1, 1.5f);
											}
											victim.damage(2, player);
											player.stopSound(Sound.ENTITY_PLAYER_ATTACK_NODAMAGE);
											player.stopSound(Sound.ENTITY_PLAYER_ATTACK_WEAK);
											player.stopSound(Sound.ENTITY_ILLUSIONER_MIRROR_MOVE);
											player.playSound(player, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1.2f);
											player.setCooldown(Material.CHICKEN, newCooldown);
											
											//Applying Poison, tracking the poisoned entity
											applyPoison(victim);
											leapVictims.add(victim);
										}
									}
								}
							}
						}.runTaskTimer(Main.getPlugin(), 0, 0);
						
						LEAP_TASKS.add(runnable);
					}
				}
			}
		}
		//Poison Sword Ability
		@Override
		public void onAttemptedAttack(DamageEvent event){
			Player player = (Player) event.getAttacker();
			if(player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD){
				if(event.getDamageType().isMelee() && event.getVictim() instanceof LivingEntity){
					LivingEntity victim = (LivingEntity) event.getVictim();
					//prevent friendly poison
					if(!(victim instanceof Player p) || Main.getGame().canAttack(player, p)) {
						applyPoison(victim);
					}
				}
			}
		}
		//Ensures poisonedEntities cannot be healed/eat
		@Override
		public void onTick(){
			Iterator<Map.Entry<LivingEntity, Integer>> iter = POISONED_ENTITIES.entrySet().iterator();
			while(iter.hasNext()) {
				Map.Entry<LivingEntity, Integer> entry = iter.next();
				LivingEntity entity = entry.getKey();
				Integer durationLeft = entry.getValue();

				//Checking if the duration is up
				//If it is not, decrease duration by 1 tick
				if(durationLeft <= 0){
					iter.remove();
				}
				else{
					entry.setValue(durationLeft - 1);
				}

				if(entity.isDead()){
					iter.remove();
					entity.removePotionEffect(PotionEffectType.POISON);
				}
				//Preventing Healing/Eating is handled in EventListeners.java
				//Entities cannot be healed
				//Players cannot be healed + cannot eat
        	}		
		}
	}
}

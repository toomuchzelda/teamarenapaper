package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageTimes;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;

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
public class KitVenom extends Kit {
	private static final ItemStack POTION_OF_POISON = ItemBuilder.of(Material.POTION)
			.meta(PotionMeta.class, potionMeta -> {
				potionMeta.setBasePotionData(new PotionData(PotionType.POISON));
				potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
			})
			.build();

	public KitVenom() {
		super("Venom", "Poison damage on hit, poisoned people cannot be healed. It can also quickly jump in, afflicting" +
						" all enemies it hits with poison and decreasing its cooldown with each enemy hit!",
				POTION_OF_POISON);
		setArmor(new ItemStack(Material.CHAINMAIL_HELMET), new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));
		ItemStack sword = ItemBuilder.of(Material.IRON_SWORD)
				.displayName(Component.text("Poison Sword"))
				.lore(Component.text("Poison I", TextColor.color(170, 170, 170)))
				.enchant(Enchantment.SOUL_SPEED, 1)
				.hide(ItemFlag.HIDE_ENCHANTS)
				.build();

		ItemStack leap = ItemBuilder.of(Material.CHICKEN)
				.displayName(Component.text("Toxic Leap", TextColor.color(145, 86, 204)))
				.lore(TextUtils.wrapString("Right click to leap forward, infecting everyone you touch!\n" +
						"Cooldown decreases with each player hit", Style.style(TextUtils.RIGHT_CLICK_TO)))
				.build();

		setItems(sword, leap);
		setAbilities(new VenomAbility());

		setCategory(KitCategory.UTILITY);
	}

	public static class VenomAbility extends Ability
	{
		public static final HashMap<LivingEntity, Integer> POISONED_ENTITIES = new HashMap<>();

		//clean up
		public void unregisterAbility() {
			POISONED_ENTITIES.clear();
		}

		@Override
		public void removeAbility(Player player) {
			player.setCooldown(Material.CHICKEN, 0);
		}

		//When Poison is applied
		public void applyPoison(LivingEntity victim, @Nullable Entity giver){
			int poisonDuration = 0;
			boolean hadPoison = false;

			if(victim.hasPotionEffect(PotionEffectType.POISON)){
				poisonDuration = victim.getPotionEffect(PotionEffectType.POISON).getDuration();
				hadPoison = true;
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
			//Adds new victim, or updates current victim's poison duration.
			//Extra check is necessary since some mobs are immune to poison.
			if(victim.hasPotionEffect(PotionEffectType.POISON)){
				POISONED_ENTITIES.put(victim, (Integer) victim.getPotionEffect(PotionEffectType.POISON).getDuration());

				DamageTimes.DamageTime poisonTime = DamageTimes.getDamageTime(victim, DamageTimes.TrackedDamageTypes.POISON);
				int timeGiven;
				if(hadPoison)
					timeGiven = poisonTime.getTimeGiven();
				else
					timeGiven = TeamArena.getGameTick();

				poisonTime.update(giver, timeGiven);
			}
		}

		//Leap Ability
		@Override
		public void onInteract(PlayerInteractEvent event) {
			Player player = event.getPlayer();
			//Raw Chicken is proper ability form, Cooked Chicken is funny admin abuse mode
			if(event.getMaterial() == Material.CHICKEN || event.getMaterial() == Material.COOKED_CHICKEN) {
				if(Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player)) {
					Component cannotUseAbilityMsg = Component.text("You can't use Toxic Leap while holding the flag!").color(TextColor.color(255, 98, 20));
					player.sendMessage(cannotUseAbilityMsg);
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 2, 0.5f);
				}
				else if(!player.hasCooldown(Material.CHICKEN) || (event.getMaterial() == Material.COOKED_CHICKEN && !player.hasCooldown(Material.COOKED_CHICKEN))) {
					//Leap Ability Implementation
					World world = player.getWorld();
					Vector direction = player.getLocation().getDirection();
					Vector multiplier = new Vector(1.0, 0.5, 1.0);
					multiplier.multiply(1.5);
					direction.multiply(multiplier);
					player.setCooldown(Material.CHICKEN, 12 * 20);
					player.setCooldown(Material.COOKED_CHICKEN, 4 * 20);
					//world.playSound to play sound for all players
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
											//victim.damage(2, player);
											DamageEvent.newDamageEvent(victim ,2, DamageType.TOXIC_LEAP, player, false);

											player.stopSound(Sound.ENTITY_ILLUSIONER_MIRROR_MOVE);
											player.playSound(player, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1.2f);
											player.setCooldown(Material.CHICKEN, newCooldown);

											player.setCooldown(Material.COOKED_CHICKEN, 0);


											//Applying Poison, tracking the poisoned entity
											applyPoison(victim, player);
											leapVictims.add(victim);
										}
									}
								}
							}
						}.runTaskTimer(Main.getPlugin(), 0, 0);
					}
				}
			}
		}
		//Poison Sword Ability
		@Override
		public void onAttemptedAttack(DamageEvent event){
			Player player = (Player) event.getFinalAttacker();
			if(player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD){
				if(event.getDamageType().isMelee() && event.getVictim() instanceof LivingEntity){
					LivingEntity victim = (LivingEntity) event.getVictim();
					//prevent friendly poison
					if(!(victim instanceof Player p) || Main.getGame().canAttack(player, p)) {
						applyPoison(victim, player);
					}
				}
			}
		}

		public static boolean isVenomBlockingEating(LivingEntity eater) {
			return POISONED_ENTITIES.containsKey(eater);
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

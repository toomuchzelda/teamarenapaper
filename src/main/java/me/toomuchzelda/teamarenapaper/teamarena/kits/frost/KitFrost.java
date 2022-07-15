package me.toomuchzelda.teamarenapaper.teamarena.kits.frost;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitExplosive;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author onett425
 */
public class KitFrost extends Kit
{
	public static final HashMap<Player, Integer> FROSTED_ENTITIES = new HashMap<>();
	public static final Component FROST_FROZEN_MESSAGE = Component.text("YOU ARE FROSTED");
	public KitFrost() {
		super("Frost", "Parry + League of Legends Flash", Material.ICE);

		ItemStack flashFreeze = ItemBuilder.of(Material.AMETHYST_SHARD)
				.displayName(Component.text("Flash Freeze"))
				.build();
		ItemStack chest = ItemBuilder.of(Material.LEATHER_CHESTPLATE)
						.color(Color.AQUA)
								.build();

		setItems(new ItemStack(Material.IRON_SWORD), flashFreeze);
		setArmor(new ItemStack(Material.IRON_HELMET), chest,
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));

		setAbilities(new FrostAbility());

		setCategory(KitCategory.UTILITY);
	}

	public static class FrostAbility extends Ability {

		//public static final int FLASH_CD = 8 * 20;
		public static final int FLASH_CD = 2 * 20;
		public static final double FLASH_DISTANCE = 8.0;
		public static final double FLASH_HITBOX_SIZE = 0.3;
		public static final int FROST_DURATION = 120;
		//public static final int PARRY_CD = 120;
		public static final int PARRY_CD = 20;
		//public static final int ACTIVE_PARRY_TIME = 20;
		public static final int ACTIVE_PARRY_TIME = 120;
		public static final double PARRY_HITBOX_SIZE = 0.3;
		public static final double PARRY_RANGE = 3.0;

		public static final Component PARRY_READY_MESSAGE = Component.text("PARRY IS READY");
		public static final Enchantment PARRY_ENCHANT = Enchantment.PROTECTION_PROJECTILE;

		//Yaw / Pitch Range = player yaw / pitch can deviate from the incoming projectile's
		// yaw/pitch by +/- the # of degrees listed below
		public static final double PARRY_YAW_RANGE = 45;
		public static final HashSet<Player> ACTIVE_PARRY = new HashSet<>();
		public static final HashMap<Player, Entity> LATEST_DEFLECT = new HashMap<>();

		@Override
		public void unregisterAbility() {
			FROSTED_ENTITIES.clear();
			ACTIVE_PARRY.clear();
			LATEST_DEFLECT.clear();
		}

		//Ensuring EXP bar is clean upon spawn
		@Override
		public void giveAbility(Player player) {
			player.setExp(0f);
		}

		//Cleaning up EXP bar upon player death
		@Override
		public void removeAbility(Player player) {
			disableParry(player);
			LATEST_DEFLECT.remove(player);
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {

			//Certain projectiles cancel direct hits, so check for that here first before proceeding
			event.setCancelled(ProjDeflect.cancelDirectHit(event));

			if(event.getDamageType().is(DamageType.MELEE) &&
					event.getFinalAttacker() instanceof Player attacker &&
					ACTIVE_PARRY.contains(attacker)) {
				//Prevent melee attacks when the Frost is parrying
				event.setCancelled(true);
			}
		}

		@Override
		public void onTick() {
			//Handling Frost Debuff
			Iterator<Map.Entry<Player, Integer>> iter = FROSTED_ENTITIES.entrySet().iterator();
			while(iter.hasNext()) {
				Map.Entry<Player, Integer> entry = iter.next();
				LivingEntity entity = entry.getKey();
				Integer initTick = entry.getValue();
				int elapsedTick = TeamArena.getGameTick() - initTick;

				if(elapsedTick >= FROST_DURATION){
					iter.remove();
				}
				if(entity.isDead()){
					iter.remove();
					entity.removePotionEffect(PotionEffectType.SLOW);
				}
				//Actual Frost debuff is handled in EventHandlers, SearchAndDestroy and CaptureTheFlag
			}

			HashMap<Player, Entity> staleProj = new HashMap<>();
			//Handling teleporting to the latest deflected projectile
			LATEST_DEFLECT.forEach((player, entity) -> {

				Location tpLoc = entity.getLocation();
				World world = player.getWorld();
				//Maintaining the player's original rotation upon teleport
				tpLoc.setYaw(player.getLocation().getYaw());
				tpLoc.setPitch(player.getLocation().getPitch());

				if(entity instanceof Projectile proj) {

					Vector inc;
					//Apply extra correction for rockets since they tend to be extra inaccurate
					if(proj instanceof ShulkerBullet) {
						inc = tpLoc.getDirection().multiply(0.2);
					}
					else {
						inc = tpLoc.getDirection().multiply(0.1);
					}
					//Apply correction to tpLoc to ensure it is in bounds + does not suffocate the user
					while(tpLoc.getBlock().isSolid() ||
							!Main.getGame().getBorder().contains(tpLoc.toVector())) {

						tpLoc.subtract(inc);
					}

					//Teleport to arrows if they hit a block / removed
					if(proj instanceof AbstractArrow abstractArrow &&
							(abstractArrow.isInBlock() || abstractArrow.isDead())) {

							player.teleport(tpLoc);
							staleProj.put(player, entity);
					}
					//Only allow them to teleport to Burst rocket if it hits a block or near a player
					else if (proj instanceof ShulkerBullet rocket &&
							(rocket.isDead() &&
									(rocket.getLocation().clone().add(rocket.getLocation().getDirection())
									.getBlock().isSolid() ||
									!rocket.getLocation().getNearbyPlayers(1).isEmpty()))) {

						player.teleport(tpLoc);
						staleProj.put(player, entity);
					}
					else if (proj instanceof EnderPearl && proj.isDead()) {

						player.teleport(tpLoc);
						staleProj.put(player, entity);
					}
					else {
						if(proj.isDead()) {

							player.teleport(tpLoc);
							staleProj.put(player, entity);
						}
					}
				}
				//All deflectable Items are grenades, which are marked for removal upon detonation
				else if(entity instanceof Item item) {
					if(item.isDead()) {
						player.teleport(tpLoc);
						staleProj.put(player, entity);
					}
				}
			});

			staleProj.forEach(LATEST_DEFLECT::remove);
		}

		@Override
		public void onPlayerTick(Player player) {

			//Removing Parry if player is not holding a sword at all
			if(ACTIVE_PARRY.contains(player) &&
					(player.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD &&
							player.getInventory().getItemInOffHand().getType() != Material.IRON_SWORD)) {

				disableParry(player);
			}

			//Handling display of frost sword uptime
			float percent = (float) 1 / (float) PARRY_CD;
			//if Frost is not currently parrying, slowly fill exp bar
			if(!ACTIVE_PARRY.contains(player)) {
				float newPercent = player.getExp() + percent;
				newPercent = MathUtils.clamp(0f, 1f, newPercent);
				player.setExp(newPercent);
			}
			//if Frost is currently parrying, deplete exp bar
			//EXP bar is completely depleted when parry is performed
			else {
				percent = (float) 1 / (float) ACTIVE_PARRY_TIME;
				float newPercent = player.getExp() - percent;
				newPercent = MathUtils.clamp(0f, 1f, newPercent);
				player.setExp(newPercent);

				parryDeflectTick(player);

				if(newPercent == 0f) {
					//If parry remains unused, disable it when exp bar reaches 0
					disableParry(player);
				}
			}
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if (!event.getAction().isRightClick())
				return;

			Player player = event.getPlayer();
			Material mat = event.getMaterial();

			// will be uncancelled later if not handled
			event.setUseItemInHand(Event.Result.DENY);
			event.setUseInteractedBlock(Event.Result.DENY);

			if(mat == Material.AMETHYST_SHARD && !player.hasCooldown(mat)) {
				flashFreeze(player);
			}
			else if(mat == Material.IRON_SWORD && player.getExp() >= 1f) {
				activateParry(player);
			}
		}

		public void activateParry(Player player) {
			player.getInventory().all(Material.IRON_SWORD).forEach(
					(k, v) -> v.addUnsafeEnchantment(PARRY_ENCHANT, 1));
			if(player.getInventory().getItemInOffHand().getType() == Material.IRON_SWORD) {
				player.getInventory().getItemInOffHand().addUnsafeEnchantment(PARRY_ENCHANT, 1);
			}

			ACTIVE_PARRY.add(player);
		}

		public void disableParry(Player player) {
			player.getInventory().all(Material.IRON_SWORD).forEach(
					(k, v) -> v.removeEnchantment(PARRY_ENCHANT));
			if(player.getInventory().getItemInOffHand().getType() == Material.IRON_SWORD) {
				player.getInventory().getItemInOffHand().removeEnchantment(PARRY_ENCHANT);
			}

			ACTIVE_PARRY.remove(player);
			player.setExp(0f);
		}

		public void parryDeflectTick(Player player) {
			World world = player.getWorld();
			RayTraceResult trace = world.rayTraceEntities(player.getEyeLocation(), player.getLocation().getDirection(),
					PARRY_RANGE, PARRY_HITBOX_SIZE, entity -> ProjDeflect.isDeflectable(player, entity));

			if(trace != null) {
				attemptDeflect(player, trace.getHitEntity());
			}
		}

		public void attemptDeflect(Player player, Entity entity) {
			if(ProjDeflect.tryDeflect(player, entity)){
				LATEST_DEFLECT.put(player, entity);
				//Only 1 projectile is able to be deflected per Parry use.
				disableParry(player);
				Bukkit.broadcast(Component.text("DEFLECTED"));
			}
		}

		public void flashFreeze(Player player) {
			//Teleporting the user
			World world = player.getWorld();
			Location initLoc = player.getLocation().clone();
			Location startPoint = initLoc.clone().add(0,player.getEyeHeight(),0);
			Vector dir = initLoc.getDirection();

			RayTraceResult trace = world.rayTraceBlocks(startPoint, dir,
					FLASH_DISTANCE, FluidCollisionMode.NEVER, true);
			Location destination;
			if(trace == null){
				//No block is hit, so travel the entire distance
				destination = startPoint.clone().add(dir.clone().multiply(FLASH_DISTANCE));
			}
			else{
				//Block is hit, so travel to the hit point
				destination = trace.getHitPosition().toLocation(world,
						initLoc.getYaw(), initLoc.getPitch());
			}

			//Prevent Frost from suffocating itself by correcting location
			//Ignore blocks which are considerably short (i.e. carpet, snow)
			while(destination.getBlock().isSolid() &&
					destination.getBlock().getBoundingBox().getHeight() > 0.25){
				destination.subtract(dir.clone().multiply(0.1));
			}

			world.playSound(player, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.4f);
			player.teleport(destination);
			flashFreezeDebuff(player, initLoc.clone(), destination.clone(), dir.clone());
			player.setCooldown(Material.AMETHYST_SHARD, FLASH_CD);
		}

		//Handling applying Debuff + Particle Trail
		public void flashFreezeDebuff(Player user, Location initLoc, Location dest, Vector dir){
			World world = user.getWorld();
			//Adding to y component so everything is centered at the middle of the player's location
			Location departure = initLoc.clone().add(0, 1, 0);
			Location currPoint = departure.clone();
			Location destination = dest.clone().add(0, 1, 0);
			double flashDistance = departure.distance(destination);
			int length = ((int) flashDistance);
			HashSet<Player> frostVictims = new HashSet<>();

			for(int i = 0; i <= length; i++){
				world.spawnParticle(Particle.TOTEM, currPoint, 1);
				currPoint = currPoint.add(dir);
			}

			for(Player player : Bukkit.getOnlinePlayers()){
				RayTraceResult trace = world.rayTraceEntities(departure, dir, flashDistance,
						FLASH_HITBOX_SIZE, entity -> entity.equals(player));

				if(trace != null){
					frostVictims.add((Player) trace.getHitEntity());
				}
			}

			//Filtering out allies and spectators
			//Applying frost effect to all enemies hit by the flash
			frostVictims.stream()
					.filter(player -> Main.getGame().canAttack(player, user) &&
							!Main.getGame().isSpectator(player))
					.forEach(enemy -> {

				FROSTED_ENTITIES.put(enemy, TeamArena.getGameTick());
					enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
							FROST_DURATION, 0, true));

					world.spawnParticle(Particle.BLOCK_CRACK, enemy.getEyeLocation(), 10,
							0.25,0.25,0.25, Material.FROSTED_ICE.createBlockData());
					world.playSound(enemy, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.8f);
			});
		}
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.frost;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

import static me.toomuchzelda.teamarenapaper.teamarena.kits.KitExplosive.ExplosiveAbility.RPG_ARROW_COLOR;
import static me.toomuchzelda.teamarenapaper.teamarena.kits.KitPyro.MOLOTOV_ARROW_COLOR;

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
		public static final int FROST_DURATION = 120;

		@Override
		public void unregisterAbility() {
			FROSTED_ENTITIES.clear();
		}

		@Override
		public void onTick() {
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
				//Actual Frost debuff is handled in EventHandlers
			}
		}

		@Override
		public void onPlayerTick(Player player){
			//deflectTest(player);
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

			if(mat == Material.AMETHYST_SHARD && !player.hasCooldown(mat)){
				flashFreeze(player);
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
						0.1, entity -> entity.equals(player));

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

		public void deflectTest(Player player){
			List<Entity> nearbyEnt = player.getNearbyEntities(4,4,4);
			nearbyEnt.stream()
					.filter(entity -> (entity.getVelocity().lengthSquared()  >= 0 &&
							(entity instanceof Projectile || entity instanceof Item)))
					.forEach(entity -> {
						if(entity instanceof ShulkerBullet rocket){
							//Only allow them to teleport to the rocket if it hits a block
							//rocket.getLocation().getBlock().isSolid();
							ProjDeflect.deflectProj(player, rocket);
						}
						if(entity instanceof Firework firework){
							ProjDeflect.deflectBurstFirework(player, firework);
						}
						else if(entity instanceof AbstractArrow arrow){
							//For arrows which are associated with special abilities,
							//The shooter must be changed last second to preserve the properties
							if(arrow instanceof Arrow abilityArrow &&
								abilityArrow.getColor() != null){
								if(abilityArrow.getColor().equals(MOLOTOV_ARROW_COLOR) ||
										abilityArrow.getColor().equals(RPG_ARROW_COLOR)){
									//Used to mark the "true" shooter of the arrow
									//the actual shooter must be preserved so the arrows behave
									//according to their respective kit's implementation
									if(abilityArrow.hasMetadata("shooterOverride")){
										//First, clear any other past overrides if
										//another Frost had already deflected the projectile
										abilityArrow.removeMetadata("shooterOverride",
												Main.getPlugin());
									}
									abilityArrow.setMetadata("shooterOverride",
											new FixedMetadataValue(Main.getPlugin(), player));
									ProjDeflect.deflectSameShooter(player, abilityArrow);
								}
								else{
									ProjDeflect.deflectArrow(player, arrow);
								}
							}
							else{
								ProjDeflect.deflectArrow(player, arrow);
							}
						}
						else if(entity instanceof EnderPearl pearl){
							ProjDeflect.deflectSameShooter(player, pearl);
						}
						else if (entity instanceof Item item){
							if(item.getItemStack().getType() == Material.TURTLE_HELMET ||
									item.getItemStack().getType() == Material.HEART_OF_THE_SEA ||
									item.getItemStack().getType() == Material.FIREWORK_STAR) {
								ProjDeflect.addShooterOverride(player, item);
								ProjDeflect.deflectSameShooter(player, item);
							}
						}
						else{
							//For non-specific projectiles that are not used for kits
							ProjDeflect.deflectProj(player, (Projectile) entity);
						}
					});
		}
	}
}

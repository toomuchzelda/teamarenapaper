package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.ItemUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.core.ParticleUtils;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftArrow;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftVector;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

public class KitPyro extends Kit
{
	public static final ItemStack MOLOTOV_ARROW;
	public static final ItemStack FIRE_ARROW;
	public static final Color MOLOTOV_ARROW_COLOR = Color.fromRGB(255, 84, 10);
	
	static {
		MOLOTOV_ARROW = new ItemStack(Material.TIPPED_ARROW);
		PotionMeta molotovMeta = (PotionMeta) MOLOTOV_ARROW.getItemMeta();
		molotovMeta.clearCustomEffects();
		molotovMeta.setColor(MOLOTOV_ARROW_COLOR);
		molotovMeta.displayName(Component.text("Incendiary Projectile").color(TextColor.color(255, 84, 10)));
		MOLOTOV_ARROW.setItemMeta(molotovMeta);
		
		FIRE_ARROW = new ItemStack(Material.TIPPED_ARROW);
		PotionMeta fireMeta = (PotionMeta) FIRE_ARROW.getItemMeta();
		fireMeta.clearCustomEffects();
		fireMeta.setColor(Color.fromRGB(255, 130, 77));
		fireMeta.displayName(Component.text("Fire Arrow").color(TextColor.color(255, 130, 77)));
		FIRE_ARROW.setItemMeta(fireMeta);
	}
	
	public KitPyro() {
		super("Pyro", "fire burn burn fire!", Material.FIRE);
		
		ItemStack boots = new ItemStack(Material.CHAINMAIL_BOOTS);
		boots.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
		ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
		leggings.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 3);
		ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
		chestplate.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 3);
		ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
		ItemUtils.colourLeatherArmor(Color.RED, leggings);
		ItemUtils.colourLeatherArmor(Color.RED, chestplate);
		ItemUtils.colourLeatherArmor(Color.RED, helmet);
		
		this.setArmor(helmet, chestplate, leggings, boots);
		
		ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
		sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);
		
		ItemStack bow = new ItemStack(Material.BOW);
		
		this.setItems(sword, bow, MOLOTOV_ARROW, FIRE_ARROW);
		
		this.setAbilities(new PyroAbility());
	}
	
	public static class PyroAbility extends Ability
	{
		public final HashMap<Player, Integer> MOLOTOV_RECHARGES = new HashMap<>();
		public final LinkedList<MolotovInfo> ACTIVE_MOLOTOVS = new LinkedList<>();
		public static final int MOLOTOV_RECHARGE_TIME = 10 * 20;
		public static final int MOLOTOV_ACTIVE_TIME = 5 * 20;
		public static final int BOX_RADIUS = 2;
		
		@Override
		public void onShootBow(EntityShootBowEvent event) {
			if(event.getConsumable().equals(FIRE_ARROW)) {
				event.setConsumeItem(false);
				event.getProjectile().setFireTicks(2000);
			}
			else if(event.getConsumable().equals(MOLOTOV_ARROW)) {
				((AbstractArrow) event.getProjectile()).setBounce(true);
				MOLOTOV_RECHARGES.put((Player) event.getEntity(), TeamArena.getGameTick());
			}
		}
		
		@Override
		public void onTick() {
			var itemIter = MOLOTOV_RECHARGES.entrySet().iterator();
			while(itemIter.hasNext()) {
				Map.Entry<Player, Integer> entry = itemIter.next();
				if(TeamArena.getGameTick() - entry.getValue() >= MOLOTOV_RECHARGE_TIME) {
					entry.getKey().getInventory().addItem(MOLOTOV_ARROW);
					itemIter.remove();
				}
			}
			
			var iter = ACTIVE_MOLOTOVS.iterator();
			int currentTick = TeamArena.getGameTick();
			while(iter.hasNext()) {
				MolotovInfo minfo = iter.next();
				if(currentTick - minfo.spawnTime >= MOLOTOV_ACTIVE_TIME) {
					iter.remove();
					minfo.arrow.remove();
				}
				else {
					for(int i = 0; i < 2; i++) {
						Location randomLoc = minfo.box.getCenter().toLocation(minfo.thrower.getWorld());
						
						randomLoc.add(MathUtils.randomRange((double) -BOX_RADIUS, (double) BOX_RADIUS),
								MathUtils.randomRange(-0.1, 0.5), MathUtils.randomRange((double) -BOX_RADIUS, (double) BOX_RADIUS));
						
						randomLoc.getWorld().spawnParticle(Particle.FLAME,
								randomLoc.getX(), randomLoc.getY(), randomLoc.getZ(), 1, 0, 0, 0, 0);
						
						ParticleUtils.colouredRedstone(randomLoc, Main.getPlayerInfo(minfo.thrower).team.getColour(), 1, 2);
					}
					
					for(LivingEntity living : minfo.thrower.getWorld().getLivingEntities()) {
						if(living instanceof Player p && Main.getGame().isSpectator(p))
							continue;
						
						if(living != minfo.thrower && living.getBoundingBox().overlaps(minfo.box)) {
							EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
									minfo.thrower, living, EntityDamageEvent.DamageCause.FIRE_TICK, 1);
							
							DamageEvent damageEvent = DamageEvent.createDamageEvent(event);
							if(damageEvent != null) {
								damageEvent.setDamageType(DamageType.PYRO_MOLOTOV);
								damageEvent.setRealAttacker(minfo.thrower);
							}
						}
					}
				}
			}
		}
		
		//called manually in EventListeners.projectileHit
		// spawn the molotov effect
		public void onProjectileHit(ProjectileHitEvent event) {
			Arrow arrow = (Arrow) event.getEntity();
			boolean hasColour = false;
			//arrow.getColour() throws an exception if the arrow has no colour for some reason
			try{
				hasColour = arrow.getColor().equals(MOLOTOV_ARROW_COLOR);
			}
			catch(IllegalArgumentException e) {
				//ignore
			}
			
			if (hasColour) {
				if(event.getHitBlock() != null) {
					if(event.getHitBlockFace() == BlockFace.UP) {
						//use the colour to know if it's a molotov arrow
						
						//arrow.setColor(Color.BLACK);
						
						Location loc = event.getEntity().getLocation();
						loc.setY(event.getHitBlock().getY() + 1); //set it to floor level of hit floor
						Vector corner1 = loc.toVector().add(new Vector(BOX_RADIUS, -0.1, BOX_RADIUS));
						Vector corner2 = loc.toVector().add(new Vector(-BOX_RADIUS, 0.5, -BOX_RADIUS));
						
						BoundingBox box = BoundingBox.of(corner1, corner2);
						//shooter will always be a player because this method will only be called if the projectile of a pyro hits smth
						ACTIVE_MOLOTOVS.add(new MolotovInfo(box, (Player) arrow.getShooter(), arrow, TeamArena.getGameTick()));
						
						loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 2, 2f);
						loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 2, 0.5f);
						
					}
					else { //it hit a wall, make it not stick in the wall
						event.setCancelled(true);
						
						//credit jacky8399 for the following
						BlockFace hitFace = event.getHitBlockFace();
						Vector dirVector = hitFace.getDirection();
						if (hitFace != BlockFace.DOWN) { // ignore Y component
							dirVector.setY(0).normalize();
						}
						Vector velocity = event.getEntity().getVelocity();
						
						// https://math.stackexchange.com/questions/13261/how-to-get-a-reflection-vector
						Vector newVelocity = velocity.subtract(dirVector.multiply(2 * velocity.dot(dirVector)));
						newVelocity.multiply(0.2);
						arrow.getWorld().spawn(arrow.getLocation(), arrow.getClass(), newProjectile -> {
							newProjectile.setShooter(arrow.getShooter());
							newProjectile.setVelocity(newVelocity);
							newProjectile.setColor(arrow.getColor());
							newProjectile.setDamage(arrow.getDamage());
							newProjectile.setKnockbackStrength(arrow.getKnockbackStrength());
							newProjectile.setShotFromCrossbow(arrow.isShotFromCrossbow());
							newProjectile.setPickupStatus(arrow.getPickupStatus());
						});
						arrow.remove();
					}
				}
			}
		}
		
		@Override
		public void projectileHitEntity(ProjectileCollideEvent event) {
			if(event.getEntity() instanceof Arrow a) {
				boolean isColor = false;
				try {
					isColor = a.getColor().equals(MOLOTOV_ARROW_COLOR);
				}
				catch(IllegalArgumentException e) {
				}
				if(isColor) {
					event.setCancelled(true);
					Vector vel = a.getVelocity();
					vel.setX(vel.getX() * 0.4);
					vel.setZ(vel.getZ() * 0.4);
					a.setVelocity(vel);
				}
			}
		}
	}
	
	public static class MolotovInfo
	{
		public BoundingBox box;
		public Player thrower;
		public Arrow arrow;
		public int spawnTime;
		
		public MolotovInfo(BoundingBox box, Player thrower, Arrow arrow, int spawnTime) {
			this.box = box;
			this.thrower = thrower;
			this.arrow = arrow;
			this.spawnTime = spawnTime;
		}
		
	}
}

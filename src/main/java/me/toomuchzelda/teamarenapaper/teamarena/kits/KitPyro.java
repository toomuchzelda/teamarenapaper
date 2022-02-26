package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
		
		/*FIRE_ARROW = new ItemStack(Material.TIPPED_ARROW);
		PotionMeta fireMeta = (PotionMeta) FIRE_ARROW.getItemMeta();
		fireMeta.clearCustomEffects();
		fireMeta.setColor(Color.fromRGB(255, 130, 77));
		fireMeta.displayName(Component.text("Fire Arrow").color(TextColor.color(255, 130, 77)));
		FIRE_ARROW.setItemMeta(fireMeta);*/

		FIRE_ARROW = new ItemStack(Material.ARROW);
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
		ItemMeta bowMeta = bow.getItemMeta();
		bowMeta.addEnchant(Enchantment.ARROW_FIRE, 1, true);
		bow.setItemMeta(bowMeta);

		this.setItems(sword, bow, FIRE_ARROW, MOLOTOV_ARROW);
		
		this.setAbilities(new PyroAbility());
	}
	
	public static class PyroAbility extends Ability
	{
		public final HashMap<Player, Integer> MOLOTOV_RECHARGES = new HashMap<>();
		public final LinkedList<MolotovInfo> ACTIVE_MOLOTOVS = new LinkedList<>();
		public static final int MOLOTOV_RECHARGE_TIME = 10 * 20;
		public static final int MOLOTOV_ACTIVE_TIME = 5 * 20;
		public static final double BOX_RADIUS = 2.5;
		
		@Override
		public void onShootBow(EntityShootBowEvent event) {
				if (FIRE_ARROW.equals(event.getConsumable())) {
					event.setConsumeItem(false);
					event.getProjectile().setFireTicks(2000);
				}
				else if (MOLOTOV_ARROW.equals(event.getConsumable())) {
					event.getProjectile().setFireTicks(0);
					MOLOTOV_RECHARGES.put((Player) event.getEntity(), TeamArena.getGameTick());
				}
		}
		
		@Override
		public void onTick() {
			int currentTick = TeamArena.getGameTick();
			var itemIter = MOLOTOV_RECHARGES.entrySet().iterator();
			while(itemIter.hasNext()) {
				Map.Entry<Player, Integer> entry = itemIter.next();
				if (currentTick - entry.getValue() >= MOLOTOV_RECHARGE_TIME) {
					entry.getKey().getInventory().addItem(MOLOTOV_ARROW);
					itemIter.remove();
				}
			}
			
			var iter = ACTIVE_MOLOTOVS.iterator();
			while(iter.hasNext()) {
				MolotovInfo minfo = iter.next();
				if (currentTick - minfo.spawnTime >= MOLOTOV_ACTIVE_TIME) {
					minfo.arrow.remove();
					iter.remove();
					continue;
				}
				World world = minfo.thrower.getWorld();
				for (int i = 0; i < 2; i++) {
					Location randomLoc = minfo.box.getCenter().toLocation(world);

					randomLoc.add(MathUtils.randomRange(-BOX_RADIUS, BOX_RADIUS),
							MathUtils.randomRange(-0.1, 0.5),
							MathUtils.randomRange(-BOX_RADIUS, BOX_RADIUS));

					randomLoc.getWorld().spawnParticle(Particle.FLAME, randomLoc, 1, 0, 0, 0, 0);

					ParticleUtils.colouredRedstone(randomLoc, minfo.color, 1, 2);
				}

				for (Entity entity : world.getNearbyEntities(minfo.box)) {
					if (!(entity instanceof LivingEntity living)) // only damage living entities
						continue;
					if (living instanceof Player p && Main.getGame().isSpectator(p)) // no postmortem kill?
						continue;
					if (living == minfo.thrower)
						continue;

					@SuppressWarnings("deprecation")
					EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(minfo.thrower, living,
							EntityDamageEvent.DamageCause.FIRE_TICK, 1);

					DamageEvent damageEvent = DamageEvent.createDamageEvent(event);
					if (damageEvent != null) {
						damageEvent.setDamageType(DamageType.PYRO_MOLOTOV);
						damageEvent.setRealAttacker(minfo.thrower);
					}
				}
			}
		}
		
		//called manually in EventListeners.projectileHit
		// spawn the molotov effect
		public void onProjectileHit(ProjectileHitEvent event) {
			if (!(event.getEntity() instanceof Arrow arrow))
				return;
			boolean hasColour = false;
			//arrow.getColour() throws an exception if the arrow has no colour for some reason
			try {
				hasColour = arrow.getColor().equals(MOLOTOV_ARROW_COLOR);
			} catch(IllegalArgumentException ignored) {}

			if (!hasColour)
				return;
			if (event.getHitBlock() == null)
				return;

			if (event.getHitBlockFace() == BlockFace.UP) {
				//use the colour to know if it's a molotov arrow
				Location loc = event.getEntity().getLocation();
				loc.setY(event.getHitBlock().getY() + 1); //set it to floor level of hit floor
				Vector corner1 = loc.toVector().add(new Vector(BOX_RADIUS, -0.1, BOX_RADIUS));
				Vector corner2 = loc.toVector().add(new Vector(-BOX_RADIUS, 0.5, -BOX_RADIUS));

				BoundingBox box = BoundingBox.of(corner1, corner2);
				//shooter will always be a player because this method will only be called if the projectile of a pyro hits smth
				Player player = (Player) arrow.getShooter();
				ACTIVE_MOLOTOVS.add(new MolotovInfo(box, player, arrow, Main.getPlayerInfo(player).team.getColour(), TeamArena.getGameTick()));

				loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 2, 2f);
				loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 2, 0.5f);

			} else { // it hit a wall, make it not stick in the wall
				event.setCancelled(true);

				//credit jacky8399 for bouncing arrow code
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
		
		@Override
		public void projectileHitEntity(ProjectileCollideEvent event) {
			if(event.getEntity() instanceof Arrow a) {
				boolean isColor = false;
				try {
					isColor = a.getColor().equals(MOLOTOV_ARROW_COLOR);
				}
				catch(IllegalArgumentException ignored) {
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

		@Override
		public void onDeath(DamageEvent event) {
			MOLOTOV_RECHARGES.remove(event.getPlayerVictim());
		}
	}

	public record MolotovInfo(BoundingBox box, Player thrower, Arrow arrow, Color color, int spawnTime) {}
}

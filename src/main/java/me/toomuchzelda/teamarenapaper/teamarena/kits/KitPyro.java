package me.toomuchzelda.teamarenapaper.teamarena.kits;

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
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
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
		public final HashSet<MolotovInfo> ACTIVE_MOLOTOVS = new HashSet<>();
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
			if (arrow.getColor().equals(MOLOTOV_ARROW_COLOR)) {
				if(event.getHitBlock() != null) {
					if(event.getHitBlockFace() == BlockFace.UP) {
						//use the colour to know if it's a molotov arrow
						
						//arrow.setColor(Color.BLACK);
						
						Location loc = event.getEntity().getLocation();
						loc.setY(event.getHitBlock().getY() + 1); //set it to floor level of hit floor
						Vector corner1 = loc.toVector().add(new Vector(BOX_RADIUS, -0.1, BOX_RADIUS));
						Vector corner2 = loc.toVector().add(new Vector(-BOX_RADIUS, 0.5, -BOX_RADIUS));
						
						BoundingBox box = BoundingBox.of(corner1, corner2);
						
						ACTIVE_MOLOTOVS.add(new MolotovInfo(box, (Player) arrow.getShooter(), arrow, TeamArena.getGameTick()));
					}
					else { //it hit a wall, make it not stick in the wall
						event.setCancelled(true);
						Vector direction = event.getHitBlockFace()/*.getOppositeFace()*/.getDirection();
						// if the direction towards the Block was 0,0,1 (towards Z) it should flip to 1,1,0
						// multiplying the arrows velocity by this should give 0 Z velocity which should make it slide
						// along the wall
						MathUtils.flipZerosAndOnes(direction);
						
						//move it back a little bit to pull it out of the wall
						net.minecraft.world.entity.projectile.AbstractArrow nmsArrow = ((CraftArrow) arrow).getHandle();
						nmsArrow.moveTo(nmsArrow.position().add(CraftVector.toNMS(arrow.getLocation().getDirection()).multiply(-1, -1, -1)));
						
						arrow.setVelocity(arrow.getVelocity().multiply(direction));
						
						Bukkit.broadcastMessage("Reached here");
					}
				}
				else if(event.getHitEntity() != null) {
					event.setCancelled(true);
					arrow.setVelocity(new Vector(0, -0.1, 0));
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

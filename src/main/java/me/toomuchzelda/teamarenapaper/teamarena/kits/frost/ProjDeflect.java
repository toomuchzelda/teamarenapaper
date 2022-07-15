package me.toomuchzelda.teamarenapaper.teamarena.kits.frost;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.List;

import static me.toomuchzelda.teamarenapaper.teamarena.kits.KitExplosive.ExplosiveAbility.RPG_ARROW_COLOR;
import static me.toomuchzelda.teamarenapaper.teamarena.kits.KitPyro.MOLOTOV_ARROW_COLOR;
import static me.toomuchzelda.teamarenapaper.teamarena.kits.frost.KitFrost.FrostAbility.PARRY_YAW_RANGE;

public class ProjDeflect {

	public static boolean isDeflectable(Player deflector, Entity entity) {
		//Allow deflection for all generic Projectiles
		//Only deflect Items that are grenades
		//Only allow entities that are in motion
		if(entity.getVelocity().lengthSquared() <= 0) {
			return false;
		}

		if(entity instanceof Projectile proj) {
			//Ensure the projectile shot from a player is from an enemy
			if(proj.getShooter() instanceof Player player) {
				//Ensure arrow is not in block for it to be deflected + is from an enemy
				if(proj instanceof AbstractArrow arrow) {
					return !arrow.isInBlock() && Main.getGame().canAttack(player, deflector);
				}
				//If it's not an arrow, just make sure it's from an enemy
				else {
					return Main.getGame().canAttack(player, deflector);
				}
			}
			//If the shooter is not a player, deflect as long as the projectile is in motion
			else {
				if(proj instanceof AbstractArrow arrow) {
					return !arrow.isInBlock();
				}
				else {
					return true;
				}
			}
		}

		else if(entity instanceof Item item &&
		(item.getItemStack().getType() == Material.TURTLE_HELMET ||
				item.getItemStack().getType() == Material.HEART_OF_THE_SEA ||
				item.getItemStack().getType() == Material.FIREWORK_STAR)) {

			//Item has no thrower, so it is not an ability grenade
			if(item.getThrower() == null) {
				return false;
			}
			//If Item has a thrower, check that the thrower is an enemy + item is in the air
			else {
				return Main.getGame().canAttack(deflector, Bukkit.getPlayer(item.getThrower())) &&
							!item.isOnGround();
			}
		}
		//Non-projectiles and Non-item grenades cannot be deflected
		return false;
	}

	public static boolean cancelDirectHit(DamageEvent event) {
		//Prevent damage from direct player collision with rockets
		if(event.getAttacker() instanceof ShulkerBullet) {
			Player shooter = (Player) event.getFinalAttacker();
			event.getAttacker().remove();

			shooter.playSound(shooter, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
			return true;
		}

		//Preventing RPG direct hit
		if (event.getDamageType().is(DamageType.PROJECTILE) &&
				(event.getAttacker() instanceof Arrow arrow &&
						arrow.getColor() != null &&
						arrow.getColor().equals(RPG_ARROW_COLOR))) {

			event.getAttacker().remove();
			return true;
		}

		return false;
	}

	public static void addShooterOverride(Player newShooter, Entity entity){
		if (entity.hasMetadata("shooterOverride")) {
			//First, clear any other past overrides if
			//another Frost had already deflected the projectile
			entity.removeMetadata("shooterOverride",
					Main.getPlugin());
		}
		entity.setMetadata("shooterOverride",
				new FixedMetadataValue(Main.getPlugin(), newShooter));
	}

	public static Player getShooterOverride(Entity entity){
		return entity.hasMetadata("shooterOverride")?
				(Player) entity.getMetadata("shooterOverride").get(0).value() : null;
	}

	private static void deflectProj(Player newOwner, Projectile proj){
		//Vector = unit vector * magnitude
		Vector dir = newOwner.getEyeLocation().getDirection();
		double magnitude = proj.getVelocity().length();
		Vector deflectVel = dir.multiply(magnitude);

		proj.setVelocity(deflectVel);
		proj.setShooter(newOwner);
	}

	private static void deflectArrow(Player newOwner, AbstractArrow arrow){
		arrow.setShooter(newOwner);
		arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

		//Vector = unit vector * magnitude
		Vector dir = newOwner.getEyeLocation().getDirection();
		double magnitude = arrow.getVelocity().length();
		Vector deflectVel = dir.multiply(magnitude);

		arrow.setVelocity(deflectVel);

	}

	private static void deflectSameShooter(Player newOwner, Entity projectile){
		Vector dir = newOwner.getEyeLocation().getDirection();
		double magnitude = projectile.getVelocity().length();
		Vector deflectVel = dir.multiply(magnitude);

		projectile.setVelocity(deflectVel);
	}

	private static void deflectBurstFirework(Player newOwner, Firework firework){
		TeamArenaTeam team = Main.getPlayerInfo(newOwner).team;

		FireworkMeta meta = firework.getFireworkMeta();
		meta.clearEffects();
		FireworkEffect effect = FireworkEffect.builder().trail(true).with(FireworkEffect.Type.BALL)
				.flicker(true).withColor(team.getColour()).build();

		meta.addEffect(effect);
		firework.setFireworkMeta(meta);
		deflectProj(newOwner, firework);
	}

	//Returns true if deflect is successful, else, return false;
	public static boolean tryDeflect(Player player, Entity entity) {

		if(!isDeflectable(player, entity)){
			return false;
		}

		if(entity instanceof ShulkerBullet rocket) {
			//Only allow them to teleport to the rocket if it hits a block
			//rocket.getLocation().getBlock().isSolid();
			ProjDeflect.deflectProj(player, rocket);
		}

		else if(entity instanceof Firework firework) {
			ProjDeflect.deflectBurstFirework(player, firework);
		}

		else if(entity instanceof AbstractArrow arrow) {
			//For arrows which are associated with special abilities,
			//The shooter must be changed last second to preserve the properties
			if(arrow instanceof Arrow abilityArrow &&
					abilityArrow.getColor() != null) {
				if(abilityArrow.getColor().equals(MOLOTOV_ARROW_COLOR) ||
						abilityArrow.getColor().equals(RPG_ARROW_COLOR)) {
					//Used to mark the "true" shooter of the arrow
					//the actual shooter must be preserved so the arrows behave
					//according to their respective kit's implementation
					if(abilityArrow.hasMetadata("shooterOverride")) {
						//First, clear any other past overrides if
						//another Frost had already deflected the projectile
						abilityArrow.removeMetadata("shooterOverride",
								Main.getPlugin());
					}
					abilityArrow.setMetadata("shooterOverride",
							new FixedMetadataValue(Main.getPlugin(), player));
					ProjDeflect.deflectSameShooter(player, abilityArrow);
				}
				else {
					ProjDeflect.deflectArrow(player, arrow);
				}
			}
			else {
				ProjDeflect.deflectArrow(player, arrow);
			}
		}

		else if(entity instanceof EnderPearl pearl) {
			ProjDeflect.deflectSameShooter(player, pearl);
		}

		else if (entity instanceof Item item) {
			if(item.getItemStack().getType() == Material.TURTLE_HELMET ||
					item.getItemStack().getType() == Material.HEART_OF_THE_SEA ||
					item.getItemStack().getType() == Material.FIREWORK_STAR) {
				ProjDeflect.addShooterOverride(player, item);
				ProjDeflect.deflectSameShooter(player, item);
			}
		}
		else {
			//For non-specific projectiles that are not used for kits
			ProjDeflect.deflectProj(player, (Projectile) entity);
		}
		return true;
	}
}

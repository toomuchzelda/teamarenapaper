package me.toomuchzelda.teamarenapaper.teamarena.kits.frost;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

public class ProjDeflect {

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

	public static void deflectProj(Player newOwner, Projectile proj){
		//Vector = unit vector * magnitude
		Vector dir = newOwner.getEyeLocation().getDirection();
		double magnitude = proj.getVelocity().length();
		Vector deflectVel = dir.multiply(magnitude);

		proj.setVelocity(deflectVel);
		proj.setShooter(newOwner);
	}

	public static void deflectArrow(Player newOwner, AbstractArrow arrow){
		//Vector = unit vector * magnitude
		Vector dir = newOwner.getEyeLocation().getDirection();
		double magnitude = arrow.getVelocity().length();
		Vector deflectVel = dir.multiply(magnitude);

		arrow.setVelocity(deflectVel);
		arrow.setShooter(newOwner);
		arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
	}

	public static void deflectSameShooter(Player newOwner, Entity projectile){
		Vector dir = newOwner.getEyeLocation().getDirection();
		double magnitude = projectile.getVelocity().length();
		Vector deflectVel = dir.multiply(magnitude);

		projectile.setVelocity(deflectVel);
	}

	public static void deflectBurstFirework(Player newOwner, Firework firework){
		TeamArenaTeam team = Main.getPlayerInfo(newOwner).team;

		FireworkMeta meta = firework.getFireworkMeta();
		meta.clearEffects();
		FireworkEffect effect = FireworkEffect.builder().trail(true).with(FireworkEffect.Type.BALL)
				.flicker(true).withColor(team.getColour()).build();

		meta.addEffect(effect);
		firework.setFireworkMeta(meta);
		deflectProj(newOwner, firework);
	}
}

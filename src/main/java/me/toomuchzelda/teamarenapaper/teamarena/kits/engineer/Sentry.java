package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSpy;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Optional;

public class Sentry extends Building{

	//Recovery represents downtime after wrangler is used
	enum State{
		STARTUP,
		NEUTRAL,
		LOCKED,
		RECOVERY,
		WRANGLED
	}

	State currState;
	float initYaw;
	LivingEntity sentry;
	int initTick;
	int creationTick;
	int downTimeTick;
	boolean isDead;
	ItemStack[] armor;
	//public static final int SENTRY_LIFETIME = KitEngineer.EngineerAbility.SENTRY_CD;
	public static final int SENTRY_LIFETIME = 300;
	public static final int SENTRY_STARTUP_TIME = 40;
	public static final int SENTRY_DOWN_TIME = 40;
	public static final int SENTRY_CYCLE_TIME = 120;
	public static final int SENTRY_SIGHT_RANGE = 15;
	//degree rotation = how much the sentry will rotate
	//degree view = the sentry's cone of vision
	public static final int SENTRY_DEGREE_ROTATION = 90;
	public static final double SENTRY_YAW_VIEW = 15.0;
	public static final double SENTRY_PITCH_VIEW = 70.0;
	//Fire every SENTRY_FIRE_RATE ticks
	public static final int SENTRY_FIRE_RATE = 12;
    public Sentry(Player player, LivingEntity sentry){
		super(player, sentry.getLocation());
		this.name = "Sentry";
		this.type = BuildingType.SENTRY;
		this.currState = State.STARTUP;
		this.sentry = sentry;
		this.initYaw = this.loc.getYaw();
		this.initTick = TeamArena.getGameTick();
		this.creationTick = TeamArena.getGameTick() + SENTRY_STARTUP_TIME;
		this.downTimeTick = -1;
		this.isDead = false;
		this.holo = new RealHologram(this.loc.clone().add(0,2.0,0), this.holoText);

		TextColor teamColorText = Main.getPlayerInfo(player).team.getRGBTextColor();
		Color teamColor = Main.getPlayerInfo(player).team.getColour();
		this.setText(Component.text(player.getName() + "'s Sentry").color(teamColorText));

		//Changing properties from Projection state to Active state

		sentry.setInvisible(false);
		sentry.setInvulnerable(false);
		sentry.setCollidable(true);
		sentry.setSilent(false);
		this.armor = new ItemStack[4];
		armor[3] = new ItemStack(Material.LEATHER_HELMET);
		armor[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
		armor[1] = new ItemStack(Material.LEATHER_LEGGINGS);
		armor[0] = new ItemStack(Material.LEATHER_BOOTS);
		ItemUtils.colourLeatherArmor(teamColor, armor[3]);
		ItemUtils.colourLeatherArmor(teamColor, armor[2]);
		ItemUtils.colourLeatherArmor(teamColor, armor[1]);
		ItemUtils.colourLeatherArmor(teamColor, armor[0]);

		ItemStack sentryBow = new ItemStack(Material.BOW);
		ItemMeta bowMeta = sentryBow.getItemMeta();
		bowMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
		sentryBow.setItemMeta(bowMeta);
		sentry.getEquipment().setItemInMainHand(sentryBow, true);
		this.loc.setPitch(0);
	}

	public boolean isDestroyed(){
		return isDead;
	}
	public boolean isExpired(){
		return TeamArena.getGameTick() - creationTick > SENTRY_LIFETIME || isDead;
	}

	public void setText(Component newText){
		this.holoText = newText.color(teamColor);
		this.holo.setText(this.holoText);
	}

	public boolean sentryCanSee(LivingEntity sentry, Player target){
		//Since AI is disabled, line of sight must be calculated manually
		Vector sentryToTarget = target.getEyeLocation().clone().subtract(sentry.getEyeLocation()).clone().toVector();
		sentryToTarget.normalize(); //set to a distance of 1
		Location relAngle = sentry.getEyeLocation();
		relAngle.setDirection(sentryToTarget);
		double yawAngle = relAngle.getYaw();
		double currYaw = sentry.getLocation().getYaw();

		double pitchAngle = relAngle.getPitch();
		double currPitch = sentry.getLocation().getPitch();
		//line of sight rn is not smart, cannot detect player if they are above/below
		//This is an "intended feature" to prevent camping
		return (Math.abs(yawAngle - currYaw) <= SENTRY_YAW_VIEW &&
				//Math.abs(pitchAngle - currPitch) <= SENTRY_PITCH_VIEW &&
						sentry.hasLineOfSight(target));
	}

	//WIP custom line of sight, currently unused
	public boolean hasObstruction(LivingEntity sentry, Player target, Vector lineOfSight){
		Vector terminationPoint = target.getEyeLocation().toVector();
		Vector startingPoint = sentry.getEyeLocation().toVector();
		Vector inc = lineOfSight.clone().multiply(0.1);
		while(startingPoint.distanceSquared(terminationPoint) >= 0.2){
			startingPoint.add(inc);
			Block currBlock = startingPoint.toLocation(target.getWorld()).getBlock();
			Material currBlockType = startingPoint.toLocation(target.getWorld()).getBlock().getType();
			if(currBlockType != Material.AIR ||
					currBlock.isCollidable() ||
						!currBlock.isLiquid()
				){
				return true;
			}
		}
		return false;
	}

	public void setIdleRotation(){
		//Handling idle rotation of Sentry
		int elapsedTick = (TeamArena.getGameTick() - creationTick) % SENTRY_CYCLE_TIME;
		double degInc = ((SENTRY_DEGREE_ROTATION/2.0) / (SENTRY_CYCLE_TIME/6.0));
		Location sentryLoc = sentry.getLocation().clone();
		//Rotate from center to left side
		if(elapsedTick < SENTRY_CYCLE_TIME/6){
			sentryLoc.setYaw((float) (sentryLoc.getYaw() - degInc));
		}
		//Pause at Left Side
		else if(elapsedTick < 2 * SENTRY_CYCLE_TIME/6){

		}
		//Rotate to Right Side
		else if(elapsedTick < 4 * SENTRY_CYCLE_TIME/6){
			sentryLoc.setYaw((float) (sentryLoc.getYaw() + degInc));
		}
		//Pause at Right Side
		else if(elapsedTick < 5 * SENTRY_CYCLE_TIME/6){

		}
		//Return back to center position
		else{
			sentryLoc.setYaw((float) (sentryLoc.getYaw() - degInc));
		}

		//Handling Pitch, should only be changed during RECOVERY state
		if(sentryLoc.getPitch() != 0){
			sentryLoc.setPitch((float) Math.max(0, sentryLoc.getPitch() - 10.0));
		}
		sentry.teleport(sentryLoc);
	}

	public void findTarget(){
		TeamArenaTeam ownerTeam = Main.getPlayerInfo(owner).team;
		Location sentryLoc = sentry.getLocation().clone();
		Mob sentryCasted = (Mob) sentry;
		Collection<Player> nearbyTargets = sentryLoc.getNearbyPlayers(SENTRY_SIGHT_RANGE);
		Optional<Player> nearestPlayer = nearbyTargets.stream()
				//Always ignore teammates and players who are invisible or are obstructed from view
				.filter(player -> !player.isInvisible()
						&& sentryCanSee(sentry, player)
						&& Main.getGame().canAttack(player, owner))
				//Ignore spies who are disguised as allies
				.filter(enemy -> {
					//If enemy has no kit, or they
					if(Main.getPlayerInfo(enemy).activeKit != null &&
							Main.getPlayerInfo(enemy).activeKit.getName().equalsIgnoreCase("Spy") &&
							Main.getPlayerInfo(enemy).activeKit.getActiveUsers().contains(enemy)){
							//If enemy is disguised, but is not disguised as an ally, it is a valid target.
								return !PlayerUtils.isDisguisedAsAlly(owner, enemy);
					}
					//If any of the above properties is false, the enemy is a valid target
					else{
						return true;
					}
				})
				.reduce((currClosest, currPlayer) -> {
					if(loc.distanceSquared(currPlayer.getLocation()) <
							loc.distanceSquared(currClosest.getLocation())){
						//If currPlayer is closer than currMax, it is the new closest
						return currPlayer;
					}
					else{
						return currClosest;
					}
				});

		if(nearestPlayer.isPresent()){
			//sentryCasted.lookAt(nearestPlayer.get(), 1.0f, 90.0f);
			sentryCasted.setTarget(nearestPlayer.get());
			this.currState = State.LOCKED;
		}
		else{
			sentryCasted.setTarget(null);
			this.currState = State.NEUTRAL;
		}
	}

	public void lockOn(){
		if(((Mob) sentry).getTarget() != null){
			Mob sentryCasted = (Mob) sentry;
			Player target = (Player) sentryCasted.getTarget();
			Location sentryLoc = sentry.getLocation();
			Vector diff = target.getEyeLocation().toVector().subtract(sentryCasted.getEyeLocation().toVector());
			diff.normalize();
			sentryLoc.setDirection(diff);
			sentry.teleport(sentryLoc);
		}
	}

	public void construction(){
		int elapsedTick = TeamArena.getGameTick() - this.initTick;
		long percCD = Math.round(100 * (double)(elapsedTick) / SENTRY_STARTUP_TIME);
		EntityEquipment equipment = sentry.getEquipment();
		holoText = Component.text("Building... " + percCD + "%");
		this.setText(this.holoText);
		if(elapsedTick < SENTRY_STARTUP_TIME/4.0){
			if(equipment.getBoots().getType() == Material.AIR){
				equipment.setBoots(this.armor[0]);
			}
		}
		else if(elapsedTick < 2.0 * SENTRY_STARTUP_TIME/4.0){
			if(equipment.getLeggings().getType() == Material.AIR){
				equipment.setLeggings(this.armor[1]);
			}
		}
		else if(elapsedTick < 3.0 * SENTRY_STARTUP_TIME/4.0){
			if(equipment.getChestplate().getType() == Material.AIR){
				equipment.setChestplate(this.armor[2]);
			}
		}
		else if(elapsedTick < 4.0 * SENTRY_STARTUP_TIME/4.0){
			if(equipment.getHelmet().getType() == Material.AIR){
				equipment.setHelmet(this.armor[3]);
			}
		}
		else{
			this.currState = State.NEUTRAL;
			TextColor teamColorText = Main.getPlayerInfo(owner).team.getRGBTextColor();
			Color teamColor = Main.getPlayerInfo(owner).team.getColour();
			this.setText(Component.text(owner.getName() + "'s Sentry").color(teamColorText));
		}
	}

	public void shoot(Vector direction){
		AbstractArrow sentryFire = sentry.launchProjectile(Arrow.class);
		sentryFire.setVelocity(direction.multiply(2));
		sentryFire.setDamage(2.0);
		sentryFire.setKnockbackStrength(0);
		sentryFire.setShooter(owner);
		sentryFire.setCritical(false);
		sentryFire.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
	}

	public void tick(){

		if(sentry.isDead()){
			this.destroy();
			this.isDead = true;
		}
		if(this.currState == State.STARTUP){
			construction();
		}
		else if(this.currState == State.NEUTRAL){
			//Handles idle rotation animation of sentry
			setIdleRotation();
			findTarget();
		}
		else if(this.currState == State.LOCKED){
			Mob sentryCasted = (Mob) sentry;
			//Sentry periodically checks its current target is the one closest to itself
			if(TeamArena.getGameTick() % 5 == 0){
				findTarget();
			}
			//Sentry remains locked on until the target becomes obstructed from view OR target dies
			if((sentryCasted.getTarget() != null && sentryCanSee(sentry, (Player) sentryCasted.getTarget()))
					&& Main.getPlayerInfo((Player) sentryCasted.getTarget()).activeKit != null){
				if(TeamArena.getGameTick() % SENTRY_FIRE_RATE == 0){
					Vector direction = sentryCasted.getTarget().getLocation().subtract(sentry.getLocation()).toVector().normalize();
					shoot(direction);
				}
				lockOn();
			}
			//View is now obstructed, enter recovery state
			else{
				sentryCasted.setTarget(null);
				this.currState = State.NEUTRAL;
			}
		}
		else if(this.currState == State.WRANGLED){
			if((owner.getInventory().getItemInMainHand().getType() != Material.STICK &&
					owner.getInventory().getItemInOffHand().getType() != Material.STICK) ||
						owner.hasCooldown(Material.STICK)){
				this.currState = State.NEUTRAL;
			}
		}
		else if(this.currState == State.RECOVERY){

		}
	}
	@Override
	public void destroy(){
		sentry.remove();
		holo.remove();
		this.isDead = true;
	}
}

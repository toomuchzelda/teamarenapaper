package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

public class Sentry extends Building{

	enum State{
		NEUTRAL,
		LOCKED,
		RECOVERY
	}

	State currState;
	float initYaw;
	LivingEntity sentry;
	int creationTick;
	//public static final int SENTRY_LIFETIME = KitEngineer.EngineerAbility.SENTRY_CD;
	public static final int SENTRY_LIFETIME = 300;
	public static final int SENTRY_CYCLE_TIME = 120;
	public static final int SENTRY_SIGHT_RANGE = 15;
    public Sentry(Player player, LivingEntity sentry){
		super(player, sentry.getLocation());
		this.currState = State.NEUTRAL;
		this.sentry = sentry;
		this.initYaw = this.loc.getYaw();

		//Changing properties from Projection state to Active state
		Color teamColor = Main.getPlayerInfo(player).team.getColour();
		sentry.setInvisible(false);
		sentry.setInvulnerable(false);
		sentry.setGlowing(false);
		sentry.setCollidable(true);
		ItemStack helm = new ItemStack(Material.LEATHER_HELMET);
		ItemUtils.colourLeatherArmor(teamColor, helm);
		sentry.getEquipment().setHelmet(helm, true);
		this.loc.setPitch(0);
	}

	public void tick(){
		if(this.currState == State.NEUTRAL){
			//Handling idle rotation of Sentry
			int elapsedTick = TeamArena.getGameTick() - creationTick % SENTRY_CYCLE_TIME;
			double radInc = ((Math.PI/2) / (SENTRY_CYCLE_TIME/6.0));
			//Rotate from center to left side
			if(elapsedTick < SENTRY_CYCLE_TIME/6){
				this.loc.setYaw((float) (loc.getYaw() - radInc));
			}
			//Pause at Left Side
			else if(elapsedTick < 2 * SENTRY_CYCLE_TIME/6){

			}
			//Rotate to Right Side
			else if(elapsedTick < 4 * SENTRY_CYCLE_TIME/6){
				this.loc.setYaw((float) (loc.getYaw() + radInc));
			}
			//Pause at Right Side
			else if(elapsedTick < 5 * SENTRY_CYCLE_TIME/6){

			}
			//Return back to center position
			else{
				this.loc.setYaw((float) (loc.getYaw() - radInc));
			}

			Mob sentryCasted = (Mob) sentry;
			Collection<Player> nearbyTargets = loc.getNearbyPlayers(SENTRY_SIGHT_RANGE, SENTRY_SIGHT_RANGE/2.0);
			Optional<Player> nearestPlayer = nearbyTargets.stream().reduce((currClosest, currPlayer) -> {
				if(currClosest == null){
					return currPlayer;
				}
				else if(!sentry.hasLineOfSight(currPlayer)){
					return currClosest;
				}
				else{
					return loc.distanceSquared(currPlayer.getLocation()) <
								loc.distanceSquared(currClosest.getLocation())?
									currPlayer : currClosest;
				}
			});

			if(nearestPlayer.isPresent()){
				sentryCasted.lookAt(nearestPlayer.get(), 1.0f, 90.0f);
				this.currState = State.LOCKED;
			}
		}
		else if(this.currState == State.RECOVERY){

		}
		else if(this.currState == State.LOCKED){

		}
	}

	@Override
	public void destroy(){
		sentry.remove();
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.abilities;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;

import javax.annotation.Nullable;

//methods aren't abstract as kit abilities may not need to override them
public abstract class Ability {

	protected Ability() {}

	//register all one-time-registered things for this ability
	public void registerAbility() {}

	public void unregisterAbility() {}

	//'give' this ability to one player
	// whatever that means for a specific ability
	public void giveAbility(Player player) {}

	public void removeAbility(Player player) {}

	/**
	 * when an attack is *attempted* on the ability user
	 * may or may not be cancelled at this point
	 */
	public void onAttemptedDamage(DamageEvent event) {}

	/**
	 * when the ability user actually takes damage
	 */
	public void onReceiveDamage(DamageEvent event) {}

	/**
	 * when the user *attempts* to attack another entity
	 */
	public void onAttemptedAttack(DamageEvent event) {}

	/**
	 * when the user successfully deals damage to another entity
	 */
	public void onDealtAttack(DamageEvent event) {}

	/**
	 * when user dies from a DamageEvent
	 * not cancellable
	 */
	public void onDeath(DamageEvent event) {}

	/**
	 * when the user gets a kill
	 * not cancellable
	 */
	public void onKill(DamageEvent event) {}

	/**
	 * when user is awarded a kill or kill assist on someone when they die
	 */
	public void onAssist(Player player, double assist, Player victim) {}

	/**
	 * when the user launches a projectile (not a bow)
	 * @param event
	 */
	public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {}

	/**
	 * when user shoots a bow
	 * @param event
	 */
	public void onShootBow(EntityShootBowEvent event) {}

	/**
	 * when user loads a crossbow
	 */
	public void onLoadCrossbow(EntityLoadCrossbowEvent event) {}

	/**
	 * run every tick for an individual ability user
	 */
	public void onPlayerTick(Player player) {}

	/**
	 * run every tick for this ability globally (once per ability per tick)
	 */
	public void onTick() {}

	/**
	 * when the player receieves a cooldown on any of their items (e.g enderpearl after throwing)
	 */
	public void onItemCooldown(PlayerItemCooldownEvent event) {}

	/**
	 * When user interacts with something (Mouse buttons or physical interaction)
	 * Event results may be modified at this point
	 */
	public void onInteract(PlayerInteractEvent event) {}

	public void onInteractEntity(PlayerInteractEntityEvent event) {}

	/**
	 * When a projectile shot by this user hits an entity
	 * @param event
	 * @deprecated Try to use ProjectileHitEvent instead
	 */
	public void onProjectileHitEntity(ProjectileCollideEvent event) {}

	public void onProjectileHit(ProjectileHitEvent event) {}

	public void onPlayerDropItem(PlayerDropItemEvent event) {}

	/**
	 * Player places a block. By default, already cancelled. May be un-cancelled by the TeamArena instance before this
	 * method has been called
	 */
	public void onPlaceBlock(BlockPlaceEvent event) {}

	public void onMove(PlayerMoveEvent event) {}

	/**
	 * When ability user consumes an item. May be cancelled at this point.
	 */
	public void onConsumeItem(PlayerItemConsumeEvent event) {}

	public void onTeamSwitch(Player player, @Nullable TeamArenaTeam oldTeam, @Nullable TeamArenaTeam newTeam) {}
}
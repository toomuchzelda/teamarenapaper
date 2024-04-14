package me.toomuchzelda.teamarenapaper.teamarena.kits.abilities;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DetailedProjectileHitEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;

import javax.annotation.Nullable;

//methods aren't abstract as abilities may not need to override them
public abstract class Ability {
	protected Ability() {}

	public static void giveAbility(Player player, Ability ability, PlayerInfo pinfo) {
		ability.giveAbility(player);
		pinfo.abilities.add(ability);
	}

	public static void removeAbility(Player player, Ability ability, PlayerInfo pinfo) {
		ability.removeAbility(player);
		pinfo.abilities.remove(ability);
	}

	//register all one-time-registered things for this ability
	public void registerAbility() {}

	public void unregisterAbility() {}

	//'give' this ability to one player
	// whatever that means for a specific ability
	protected void giveAbility(Player player) {}

	protected void removeAbility(Player player) {}

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
	 */
	public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {}

	/**
	 * when user shoots a bow
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
	 * when the player receives a cooldown on any of their items (e.g enderpearl after throwing)
	 */
	public void onItemCooldown(PlayerItemCooldownEvent event) {}

	/**
	 * When user interacts with something (Mouse buttons or physical interaction)
	 * Event results may be modified at this point
	 */
	public void onInteract(PlayerInteractEvent event) {}

	public void onInteractEntity(PlayerInteractEntityEvent event) {}

	public void onProjectileHit(DetailedProjectileHitEvent event) {}

	/** When a projectile hits the user */
	public void onHitByProjectile(DetailedProjectileHitEvent event) {}

	public void onReflect(ProjectileReflectEvent event) {}

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

	public void onHeal(EntityRegainHealthEvent event) {}

	public void onSwapHandItems(PlayerSwapHandItemsEvent event) {}

	public void onSwitchItemSlot(PlayerItemHeldEvent event) {}

	public void onInventoryClick(InventoryClickEvent event) {}

	public void onInventoryDrag(InventoryDragEvent event) {}

	public void onFish(PlayerFishEvent event) {}

	public void onTeamSwitch(Player player, @Nullable TeamArenaTeam oldTeam, @Nullable TeamArenaTeam newTeam) {}
}
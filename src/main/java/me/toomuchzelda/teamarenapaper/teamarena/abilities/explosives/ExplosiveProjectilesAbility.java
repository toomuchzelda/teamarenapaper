package me.toomuchzelda.teamarenapaper.teamarena.abilities.explosives;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.ProjectileReflectEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.explosive.KitExplosive;
import me.toomuchzelda.teamarenapaper.teamarena.kits.explosive.RPGExplosion;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.*;

// RPG and grenades of kit Explosive
public class ExplosiveProjectilesAbility extends Ability {
	public static final int RPG_CD = 7 * 20;
	public static final double RPG_BLAST_RADIUS = 8;
	private static final int RPG_CHARGEUP_TIME = 35; //1.75 secs
	private static final Component RPG_CHARGE_BOSSBAR_NAME = Component.text("CHARGING", NamedTextColor.YELLOW, TextDecoration.BOLD);
	private static final Component RPG_CHARGE_ALMOST_READY = Component.text("CHARGING", NamedTextColor.GOLD, TextDecoration.BOLD);
	private static final NamespacedKey RPG_ARROW_MARKER = new NamespacedKey(Main.getPlugin(), "rpgarrow");

	public static final int GRENADE_MAX_ACTIVE = 3;
	public static final int GRENADE_FUSE_TIME = 60;

	private static final TextColor ITEM_YELLOW = TextColor.color(255, 241, 120);

	public static final ItemStack GRENADE = ItemBuilder.of(Material.FIREWORK_STAR)
		.displayName(Component.text("Grenade", ITEM_YELLOW))
		.lore(TextUtils.wrapString("Right click to throw one. You can only have "
			+ GRENADE_MAX_ACTIVE + " out at a time", Style.style(TextUtils.RIGHT_CLICK_TO)))
		.build();

	public static final ItemStack RPG = ItemBuilder.of(Material.EGG)
		.displayName(Component.text("RPG", ITEM_YELLOW))
		.lore(TextUtils.wrapString("Right click to charge one up. Once charged, it'll fire itself forward!", Style.style(TextUtils.RIGHT_CLICK_TO)))
		.build();

	//info for thrown rpgs
	private static final class RPGInfo {
		private final Arrow rpgArrow;
		private int spawnTime;
		private Player originalThrower; // Null until reflected

		private RPGInfo(Arrow rpgArrow, int spawnTime) {
			this.rpgArrow = rpgArrow;
			this.spawnTime = spawnTime;
		}
	}

	private static class ExplosiveInfo {
		private RPGChargeInfo chargingRpg;
		private final List<RPGInfo> activeRpgs;
		private final List<GrenadeInfo> activeGrenades;

		public ExplosiveInfo() {
			this.activeRpgs = new ArrayList<>(1);
			this.activeGrenades = new ArrayList<>(GRENADE_MAX_ACTIVE);
		}
	}

	//info for charging up rpgs
	private record RPGChargeInfo(BossBar bossbar, int throwTime) {}

	private record GrenadeInfo(Item grenade, Color color, int spawnTime, GrenadeHitbox hitbox) {}

	private final Map<Player, ExplosiveInfo> explosiveInfos = new HashMap<>();

	@Override
	public void unregisterAbility() {
		for (ExplosiveInfo einfo : explosiveInfos.values()) {
			einfo.activeGrenades.forEach(grenadeInfo -> {
				grenadeInfo.grenade.remove();
				grenadeInfo.hitbox.remove();
			});
			einfo.activeGrenades.clear();

			einfo.activeRpgs.forEach(rpgInfo -> rpgInfo.rpgArrow.remove());
			einfo.activeRpgs.clear();
		}
		explosiveInfos.clear();
	}

	private ExplosiveInfo getEinfo(Player player) {
		return this.explosiveInfos.computeIfAbsent(player, p -> new ExplosiveInfo());
	}

	@Override
	public void removeAbility(Player player) {
		final ExplosiveInfo einfo = explosiveInfos.remove(player);
		if (einfo == null) return;

		if (einfo.chargingRpg != null)
			player.hideBossBar(einfo.chargingRpg.bossbar);

		einfo.activeRpgs.forEach(rpgInfo -> rpgInfo.rpgArrow.remove());
		einfo.activeRpgs.clear();

		einfo.activeGrenades.forEach(grenadeInfo -> {
			grenadeInfo.grenade.remove();
			grenadeInfo.hitbox.remove();
		});
		einfo.activeGrenades.clear();
	}

	@Override
	public void onTick() {
		//Handling Grenade Behavior
		grenadeTick();

		//Handling RPG Behavior
		for (var entry : explosiveInfos.entrySet()) {
			var playerRpgIter = entry.getValue().activeRpgs.iterator();
			while (playerRpgIter.hasNext()) {
				final RPGInfo rpgInfo = playerRpgIter.next();

				if (rpgTick(entry.getKey(), rpgInfo)) {
					playerRpgIter.remove();
				}
			}
		}

		//Tick RPG launches that are charging up
		rpgChargeTick();

		// cleanup
		for (var iterator = explosiveInfos.entrySet().iterator(); iterator.hasNext(); ) {
			var entry = iterator.next();

			ExplosiveInfo einfo = entry.getValue();
			if (einfo.chargingRpg == null && einfo.activeRpgs.isEmpty() && einfo.activeGrenades.isEmpty()) {
				iterator.remove();
			}
		}
	}

	private void rpgChargeTick() {
		for (var entry : explosiveInfos.entrySet()) {
			final ExplosiveInfo einfo = entry.getValue();
			final RPGChargeInfo cinfo = einfo.chargingRpg;
			if (cinfo == null)
				continue;

			final int currentTick = TeamArena.getGameTick();
			final Player thrower = entry.getKey();

			final int timeSince = currentTick - cinfo.throwTime();
			if(timeSince % 6 == 0)
				thrower.getWorld().playSound(thrower, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.6f, 0.8f);
			if(timeSince % 2 == 0)
				EntityUtils.playCritEffect(thrower);

			if(timeSince >= RPG_CHARGEUP_TIME) {
				einfo.chargingRpg = null;
				thrower.hideBossBar(cinfo.bossbar());

				rpgLaunch(thrower, einfo);
			}
			//still charging: increment the progress bar
			else {
				float newProgress = ((float) timeSince) / (float) RPG_CHARGEUP_TIME;
				newProgress = MathUtils.clamp(0f, 1f, newProgress);
				cinfo.bossbar().progress(newProgress);

				if(newProgress >= 0.5f) {
					cinfo.bossbar().name(RPG_CHARGE_ALMOST_READY);
				}
			}
		}
	}

	// return true if RPG is done and cleanup needed
	private boolean rpgTick(final Player owner, RPGInfo rpgInfo) {
		final Arrow rpgArrow = rpgInfo.rpgArrow;
		final Location arrowLoc = rpgArrow.getLocation();

		//Explode RPG if it hits block or player
		if (rpgArrow.isInBlock() || rpgArrow.isOnGround() || rpgArrow.isDead() ||
			rpgArrow.getTicksLived() >= 38) {

			boolean reflected = rpgInfo.originalThrower != null;
			rpgBlast(arrowLoc, owner, reflected, rpgInfo.originalThrower);

			rpgArrow.remove();
			return true;
		}
		//RPG particle trail
		else {
			final int currentTick = TeamArena.getGameTick();
			if ((currentTick - rpgInfo.spawnTime) % 5 == 0) {
				owner.getWorld().spawnParticle(Particle.EXPLOSION, arrowLoc, 1);
			}

			if((currentTick - rpgInfo.spawnTime) % 2 == 0) {
				ParticleUtils.colouredRedstone(arrowLoc, Main.getPlayerInfo(owner).team.getColour(), 1d, 3f);
			}

			return false;
		}
	}

	private void grenadeTick() {
		final int currentTick = TeamArena.getGameTick();
		for (var entry : explosiveInfos.entrySet()) {
			var playerGrenadesIter = entry.getValue().activeGrenades.iterator();
			while (playerGrenadesIter.hasNext()) {
				final GrenadeInfo grenadeInfo = playerGrenadesIter.next();

				final Item grenade = grenadeInfo.grenade();
				final Particle.DustOptions particleOptions = new Particle.DustOptions(grenadeInfo.color(), 1);

				//Explode grenade if fuse time passes
				if (currentTick - grenadeInfo.spawnTime >= GRENADE_FUSE_TIME) {
					//real thrower info is passed on through grenade's thrower field
					TeamArenaExplosion explosion = new TeamArenaExplosion(null, 4, 0.5, 12, 3.5, 0.35, DamageType.EXPLOSIVE_GRENADE, grenade);
					explosion.explode();

					grenadeInfo.hitbox.remove();
					grenade.remove();
					playerGrenadesIter.remove();
				}
				//Grenade particles
				else {
					//Particles for when grenade has landed
					World world = entry.getKey().getWorld();
					if (grenade.isOnGround()) {
						world.spawnParticle(Particle.DUST, grenade.getLocation(), 1, 0.25, 0.25, 0.25, particleOptions);
					}
					//Particles for when grenade is in motion
					else {
						world.spawnParticle(Particle.DUST, grenade.getLocation(), 1, particleOptions);
					}
				}
			}
		}
	}

	@Override
	public void onAttemptedAttack(DamageEvent event) { // Also called by porc onAttack
		if (event.getDamageType().is(DamageType.PROJECTILE) &&
			event.getAttacker() instanceof AbstractArrow aa &&
			Boolean.TRUE.equals(aa.getPersistentDataContainer().get(RPG_ARROW_MARKER, PersistentDataType.BOOLEAN))) { // Thanks Intellij IDEA Integrated Development Environment

			event.setCancelled(true);
			event.getAttacker().remove();
		}
	}

	/*
	@Override
	public void onAttemptedDamage(DamageEvent event) {
		if(event.getDamageType().is(DamageType.EXPLOSIVE_RPG_SELF)) {
			event.setFinalDamage(5); //self RPG always does 5 damage
		}
	}
	*/

	@Override
	public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
		final Player shooter = event.getPlayer();
		//Launching RPG
		if (RPG.isSimilar(event.getItemStack())) {
			event.setCancelled(true);
			//make sure they're not already charging up a shot
			final ExplosiveInfo einfo = getEinfo(shooter);
			if(einfo.chargingRpg == null) {
				shooter.getWorld().playSound(shooter, Sound.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 3f, 1.1f);

				//start charging their rpg charge
				BossBar chargeBar = BossBar.bossBar(RPG_CHARGE_BOSSBAR_NAME, 0f, BossBar.Color.YELLOW,
					BossBar.Overlay.PROGRESS);
				shooter.showBossBar(chargeBar);
				einfo.chargingRpg = new RPGChargeInfo(chargeBar, TeamArena.getGameTick());
			}
		}
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		final ItemStack item = event.getItem();

		//Launching Grenade
		if (!GRENADE.isSimilar(item) || !event.getAction().isRightClick()) {
			return;
		}

		final Player thrower = event.getPlayer();

		assert item != null; // so IDEA stops complaining
		if (thrower.hasCooldown(item)) return;

		final Color teamColor = Main.getPlayerInfo(thrower).team.getColour();

		//Finding all the currently active grenades that are owned by the current thrower
		final ExplosiveInfo einfo = getEinfo(thrower);
		final List<GrenadeInfo> currActiveGrenades = einfo.activeGrenades;

		//Throw grenade if # of active grenades doesn't exceed the cap
		if (thrower.getGameMode() != GameMode.CREATIVE && currActiveGrenades.size() >= GRENADE_MAX_ACTIVE) {
			thrower.sendMessage(Component.text("Only " + GRENADE_MAX_ACTIVE + " Grenades may be active at once!",
				ITEM_YELLOW));
			thrower.playSound(thrower, Sound.ENTITY_CREEPER_HURT, 0.9f, 0.5f);
			return;
		}
		//Creating the grenade item to be thrown
		final ItemStack grenade = new ItemStack(Material.FIREWORK_STAR);
		FireworkEffectMeta grenadeMeta = (FireworkEffectMeta) grenade.getItemMeta();
		FireworkEffect fireworkColor = FireworkEffect.builder().withColor(teamColor).build();
		grenadeMeta.setEffect(fireworkColor);
		grenade.setItemMeta(grenadeMeta);

		//Initializing the grenade Item entity
		final World world = thrower.getWorld();
		Location initialPoint = thrower.getEyeLocation().subtract(0, 0.2, 0);
		Item grenadeDrop = world.dropItem(initialPoint, grenade, entity -> {
			entity.setCanMobPickup(false);
			entity.setCanPlayerPickup(false);
			entity.setUnlimitedLifetime(true);
			entity.setWillAge(false);
			entity.setThrower(thrower.getUniqueId());
		});

		//Throwing the grenade and activating it
		Vector vel = thrower.getLocation().getDirection().multiply(0.8);
		grenadeDrop.setVelocity(vel);
		world.playSound(grenadeDrop, Sound.ENTITY_CREEPER_PRIMED, 0.85f, 1.1f);

		GrenadeHitbox hitbox = new GrenadeHitbox(grenadeDrop);
		hitbox.respawn();

		currActiveGrenades.add(new GrenadeInfo(grenadeDrop, teamColor, TeamArena.getGameTick(), hitbox));

		final PlayerInventory inv = thrower.getInventory();

		//Remove grenade from Inventory after it is thrown
		if (event.getHand() == EquipmentSlot.HAND) {
			inv.setItemInMainHand(item.subtract());
		} else {
			inv.setItemInOffHand(item.subtract());
		}

		// If they are kit explosive, notify the item regenerator
		KitExplosive.ExplosiveAbility explosiveAbility = Ability.getAbility(thrower, KitExplosive.ExplosiveAbility.class);
		if (explosiveAbility != null)
			explosiveAbility.onUse(thrower, true);

		thrower.setCooldown(item, 10);
	}

	private void rpgLaunch(final Player shooter, final ExplosiveInfo einfo) {
		//Only apply CD when thrower is not in creative mode to allow for admin abuse
		if (shooter.getGameMode() != GameMode.CREATIVE) {
			shooter.setCooldown(Material.EGG, RPG_CD);
		}

		final PlayerInventory inv = shooter.getInventory();
		//remove one egg for launch rpg
		List<ItemStack> eggs = ItemUtils.getItemsInInventory(RPG, inv);
		if(eggs.isEmpty()) {
			Main.logger().severe(shooter.getName() + " is firing an RPG but does not have any eggs in their inventory?!");
			Thread.dumpStack();
		}
		else {
			ItemStack eggStack = eggs.get(0);
			eggStack.setAmount(eggStack.getAmount() - 1);
		}

		Location loc = shooter.getEyeLocation();
		Vector vel = loc.getDirection().multiply(1.2d);

		World world = shooter.getWorld();
		Arrow rpgArrow = world.spawn(loc, Arrow.class, arrow -> {
			arrow.setVelocity(vel);
			arrow.setSilent(true);
			arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
			arrow.setShooter(shooter);
		});
		rpgArrow.getPersistentDataContainer().set(RPG_ARROW_MARKER, PersistentDataType.BOOLEAN, true);

		List<RPGInfo> list = einfo.activeRpgs;
		list.add(new RPGInfo(rpgArrow, TeamArena.getGameTick()));

		//sound effect
		shooter.getWorld().playSound(shooter, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 2f, 0.5f);

		// If they are kit explosive, notify the item regenerator
		KitExplosive.ExplosiveAbility explosiveAbility = Ability.getAbility(shooter, KitExplosive.ExplosiveAbility.class);
		if (explosiveAbility != null)
			explosiveAbility.onUse(shooter, false);
	}

	public void rpgBlast(Location explodeLoc, Player owner, boolean reflected, Entity originalThrower) {
		double selfDamageMult, selfKnockbackMult;
		DamageType damageType = reflected ? DamageType.EXPLOSIVE_RPG_REFLECTED : DamageType.EXPLOSIVE_RPG;
		if (!reflected) {
			selfDamageMult = 1.2d; selfKnockbackMult = 1d;
		}
		else {
			selfDamageMult = 0d; selfKnockbackMult = 0d;
			assert CompileAsserts.OMIT || originalThrower != null;
		}
		RPGExplosion explosion = new RPGExplosion(explodeLoc, RPG_BLAST_RADIUS, 1.4d,
			25, 2, 1.7, damageType, owner,
			selfDamageMult, selfKnockbackMult, DamageType.EXPLOSIVE_RPG_SELF);
		explosion.setCause(originalThrower);
		explosion.explode();
	}

	//stop player from switching items when charging RPG
	@Override
	public void onSwitchItemSlot(PlayerItemHeldEvent event) {
		ExplosiveInfo einfo = explosiveInfos.get(event.getPlayer());
		if(einfo != null && einfo.chargingRpg != null) {
			event.setCancelled(true);
		}
	}
	@Override
	public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
		ExplosiveInfo einfo = explosiveInfos.get(event.getPlayer());
		if (einfo != null && einfo.chargingRpg != null) {
			event.setCancelled(true);
		}
	}
	@Override
	public void onInventoryClick(InventoryClickEvent event) {
		ExplosiveInfo einfo = explosiveInfos.get((Player) event.getWhoClicked());
		if (einfo != null && einfo.chargingRpg != null) {
			event.setCancelled(true);
		}
	}
	@Override
	public void onInventoryDrag(InventoryDragEvent event) {
		ExplosiveInfo einfo = explosiveInfos.get((Player) event.getWhoClicked());
		if (einfo != null && einfo.chargingRpg != null) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onReflect(ProjectileReflectEvent event) {
		// Maybe an RPG
		if (event.projectile instanceof Arrow arrow && event.shooter instanceof Player shooter) {
			final ExplosiveInfo einfo = explosiveInfos.get(shooter);
			if (einfo != null) {
				for (Iterator<RPGInfo> iterator = einfo.activeRpgs.iterator(); iterator.hasNext(); ) {
					RPGInfo rinfo = iterator.next();
					if (rinfo.rpgArrow.equals(arrow)) { // Indeed an explosive's RPG
						if (!CompileAsserts.OMIT && Main.getPlayerInfo(event.reflector).team.hasMember(shooter)) {
							Main.logger().warning("ExplosiveAbility.onReflect(), RPG was reflected by teammate");
							Thread.dumpStack();
						}

						event.attackFunc = this::onAttemptedAttack;

						rinfo.originalThrower = shooter;
						rinfo.spawnTime = TeamArena.getGameTick();

						// Remove ownership from original thrower and add to reflector
						iterator.remove();
						ExplosiveInfo refEinfo = getEinfo(event.reflector);
						refEinfo.activeRpgs.add(rinfo);

						break;
					}
				}
			}
		}
	}
}

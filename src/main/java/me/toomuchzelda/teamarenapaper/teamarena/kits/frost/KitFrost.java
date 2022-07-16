package me.toomuchzelda.teamarenapaper.teamarena.kits.frost;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * @author onett425
 */
public class KitFrost extends Kit
{
	public static final HashMap<Player, Integer> FROSTED_ENTITIES = new HashMap<>();
	public static final ItemStack MIRROR_BLADE;
	public static final ItemStack FLASH_FREEZE;

	static {
		MIRROR_BLADE = new ItemStack(Material.IRON_SWORD);
		ItemMeta swordMeta = MIRROR_BLADE.getItemMeta();
		swordMeta.displayName(Component.text("Mirror Blade", TextColors.LIGHT_BLUE)
				.decoration(TextDecoration.ITALIC, false));

		Style style = Style.style(TextUtils.RIGHT_CLICK_TO).decoration(TextDecoration.ITALIC, false);
		String strUsage = "Right click to activate your Deflection ability! " +
				"You will deflect the next incoming projectile and teleport to it!";
		List<Component> usage = TextUtils.wrapString(strUsage, style, 200);

		List<Component> lore = new ArrayList<>();
		lore.addAll(usage);
		lore.addAll(TextUtils.wrapString("Attacking is disabled while Deflection is active. " +
						"Deflection is disabled if sword is not in hand.",
				Style.style(TextColors.LIGHT_YELLOW), 200));
		lore.addAll(TextUtils.wrapString("Cooldown: " + FrostAbility.DEFLECT_CD /20 + " seconds",
				Style.style(TextColors.LIGHT_BROWN), 200));
		swordMeta.lore(lore);
		MIRROR_BLADE.setItemMeta(swordMeta);

		FLASH_FREEZE = new ItemStack(Material.AMETHYST_SHARD);
		ItemMeta flashMeta = FLASH_FREEZE.getItemMeta();
		flashMeta.displayName(Component.text("Flash Freeze", TextColors.LIGHT_PURPLE)
				.decoration(TextDecoration.ITALIC, false));

		Style flashStyle = Style.style(TextUtils.RIGHT_CLICK_TO).decoration(TextDecoration.ITALIC, false);
		String flashUsageString = "Right click to instantly teleport several blocks ahead! " +
				"All enemies hit in your path will be Frozen for " + FrostAbility.FROST_DURATION/20 + " seconds!";
		List<Component> flashUsage = TextUtils.wrapString(flashUsageString, flashStyle, 200);

		List<Component> flashLore = new ArrayList<>();
		flashLore.addAll(flashUsage);
		flashLore.addAll(TextUtils.wrapString("Frozen enemies are slowed and cannot switch items or right click",
				Style.style(TextColors.LIGHT_YELLOW), 200));
		flashLore.addAll(TextUtils.wrapString("Cooldown: " + FrostAbility.FLASH_CD/20 + " seconds",
				Style.style(TextColors.LIGHT_BROWN), 200));
		flashMeta.lore(flashLore);
		FLASH_FREEZE.setItemMeta(flashMeta);
	}

	public KitFrost() {
		super("Frost", "Parry + League of Legends Flash", Material.ICE);

		ItemStack chest = ItemBuilder.of(Material.LEATHER_CHESTPLATE)
						.color(Color.AQUA)
								.build();

		setItems(MIRROR_BLADE, FLASH_FREEZE);
		setArmor(new ItemStack(Material.IRON_HELMET), chest,
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));

		setAbilities(new FrostAbility());

		setCategory(KitCategory.UTILITY);
	}

	public static class FrostAbility extends Ability {

		public static final int FLASH_CD = 8 * 20;
		public static final double FLASH_DISTANCE = 8.0;
		public static final double FLASH_HITBOX_SIZE = 0.3;
		public static final int FROST_DURATION = 120;
		public static final int DEFLECT_CD = 120;
		public static final int ACTIVE_DEFLECT_TIME = 40;
		public static final double DEFLECT_HITBOX_SIZE = 0.3;
		public static final double DEFLECT_RANGE = 3.0;

		public static final Component DEFLECT_ACTION_BAR = Component.text("You prepare to deflect the next projectile...")
				.color(TextColors.LIGHT_BLUE);
		public static final Component FROSTED_MESSAGE = Component.text("You are Frozen! Switching Items and Right Clicking are temporarily disabled. ")
				.color(TextColors.FROST_BLUE);
		public static final Component THAW_MESSAGE = Component.text("You Thaw out...")
				.color(TextColors.THAW_BLUE);
		public static final Enchantment DEFLECT_ENCHANT = Enchantment.PROTECTION_PROJECTILE;
		public static final HashSet<Player> ACTIVE_DEFLECT = new HashSet<>();
		public static final HashMap<Player, Entity> LATEST_DEFLECT = new HashMap<>();

		@Override
		public void unregisterAbility() {
			FROSTED_ENTITIES.clear();
			ACTIVE_DEFLECT.clear();
			LATEST_DEFLECT.clear();
		}

		//Ensuring EXP bar is clean upon spawn
		@Override
		public void giveAbility(Player player) {
			player.setExp(0f);
		}

		@Override
		public void removeAbility(Player player) {
			disableDeflect(player);
			LATEST_DEFLECT.remove(player);
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			//Certain projectiles cancel direct hits, so check for that here first before proceeding
			event.setCancelled(ProjDeflect.cancelDirectHit(event));

			if(event.getDamageType().is(DamageType.MELEE) &&
					event.getFinalAttacker() instanceof Player attacker &&
					ACTIVE_DEFLECT.contains(attacker)) {
				//Prevent melee attacks when the Frost is deflecting
				event.setCancelled(true);
			}
		}

		@Override
		public void onTick() {
			//Handling Frost Debuff
			Iterator<Map.Entry<Player, Integer>> iter = FROSTED_ENTITIES.entrySet().iterator();
			while(iter.hasNext()) {
				Map.Entry<Player, Integer> entry = iter.next();
				Player victim = entry.getKey();
				Integer initTick = entry.getValue();
				int elapsedTick = TeamArena.getGameTick() - initTick;

				if(elapsedTick >= FROST_DURATION) {
					victim.sendMessage(THAW_MESSAGE);
					iter.remove();
				}
				if(victim.isDead()) {
					iter.remove();
					victim.removePotionEffect(PotionEffectType.SLOW);
				}
				//Actual Frost debuff is handled in EventHandlers, SearchAndDestroy and CaptureTheFlag
			}

			HashMap<Player, Entity> staleProj = new HashMap<>();
			//Handling teleporting to the latest deflected projectile
			LATEST_DEFLECT.forEach((player, entity) -> {

				Location tpLoc = entity.getLocation();
				//Maintaining the player's original rotation upon teleport
				tpLoc.setYaw(player.getLocation().getYaw());
				tpLoc.setPitch(player.getLocation().getPitch());

				if(entity instanceof Projectile proj) {

					Vector inc;
					//Apply extra correction for rockets since they tend to be extra inaccurate
					if(proj instanceof ShulkerBullet) {
						inc = tpLoc.getDirection().multiply(0.2);
					}
					else {
						inc = tpLoc.getDirection().multiply(0.1);
					}
					//Apply correction to tpLoc to ensure it is in bounds + does not suffocate the user
					while(tpLoc.getBlock().isSolid()) {

						tpLoc.subtract(inc);
					}

					//Do not allow teleportation to projectiles that are out of bounds
					if(!Main.getGame().getBorder().contains(tpLoc.toVector())) {

						staleProj.put(player, entity);
					}
					//Teleport to arrow if they hit a block / removed
					else if(proj instanceof AbstractArrow abstractArrow &&
							(abstractArrow.isInBlock() || abstractArrow.isDead())) {

							tpToProj(entity, player, tpLoc);
							staleProj.put(player, entity);
					}
					//Only allow them to teleport to Burst rocket if it hits a block or near a player
					else if (proj instanceof ShulkerBullet rocket &&
							(rocket.isDead() &&
									(rocket.getLocation().clone().add(rocket.getLocation().getDirection())
									.getBlock().isSolid() ||
									!rocket.getLocation().getNearbyPlayers(1).isEmpty()))) {

						tpToProj(entity, player, tpLoc);
						staleProj.put(player, entity);
					}
					else {
						if(proj.isDead()) {

							tpToProj(entity, player, tpLoc);
							staleProj.put(player, entity);
						}
					}
				}
				//All deflectable Items are grenades, which are marked for removal upon detonation
				else if(entity instanceof Item item) {
					if(item.isDead()) {
						tpToProj(entity, player, tpLoc);
						staleProj.put(player, entity);
					}
				}
			});

			staleProj.forEach(LATEST_DEFLECT::remove);
		}

		/** Teleports Frost to projectile location + plays effects
		 */
		public void tpToProj(Entity proj, Player player, Location tpLoc) {
			World world = player.getWorld();
			player.teleport(tpLoc);
			world.playSound(proj.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.4f);
			world.spawnParticle(Particle.SNOWFLAKE, tpLoc.clone().add(0,2,0), 12, 0,0,0, 0.2f);
		}

		@Override
		public void onPlayerTick(Player player) {
			//Removing Deflect if player is not holding a sword at all
			if(ACTIVE_DEFLECT.contains(player) &&
					(player.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD &&
							player.getInventory().getItemInOffHand().getType() != Material.IRON_SWORD)) {

				disableDeflect(player);
			}

			//Handling display of frost sword uptime
			float percent = (float) 1 / (float) DEFLECT_CD;
			//if Frost is not currently deflecting, slowly fill exp bar
			if(!ACTIVE_DEFLECT.contains(player)) {
				float newPercent = player.getExp() + percent;
				newPercent = MathUtils.clamp(0f, 1f, newPercent);
				player.setExp(newPercent);
			}
			//if Frost is currently deflecting, deplete exp bar
			//EXP bar is completely depleted when deflect is performed
			else {
				percent = (float) 1 / (float) ACTIVE_DEFLECT_TIME;
				float newPercent = player.getExp() - percent;
				newPercent = MathUtils.clamp(0f, 1f, newPercent);
				player.setExp(newPercent);

				deflectTick(player);

				if(newPercent == 0f) {
					//If deflect remains unused, disable it when exp bar reaches 0
					disableDeflect(player);
				}
			}
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if (!event.getAction().isRightClick())
				return;

			Player player = event.getPlayer();
			Material mat = event.getMaterial();

			// will be uncancelled later if not handled
			event.setUseItemInHand(Event.Result.DENY);
			event.setUseInteractedBlock(Event.Result.DENY);

			if(mat == Material.AMETHYST_SHARD && !player.hasCooldown(mat)) {
				flashFreeze(player);
			}
			else if(mat == Material.IRON_SWORD && player.getExp() >= 1f) {
				activateDeflect(player);
			}
		}

		public void activateDeflect(Player player) {
			//Adds Enchantment to sword while deflect is active
			PlayerInventory inv = player.getInventory();
			inv.all(Material.IRON_SWORD).forEach(
					(k, v) -> v.addUnsafeEnchantment(DEFLECT_ENCHANT, 1));
			if(inv.getItemInOffHand().getType() == Material.IRON_SWORD) {
				inv.getItemInOffHand().addUnsafeEnchantment(DEFLECT_ENCHANT, 1);
			}

			player.playSound(player, Sound.ITEM_AXE_SCRAPE, 1f, 1.2f);
			player.playSound(player, Sound.BLOCK_BELL_RESONATE, 1f, 2.0f);

			PlayerInfo pinfo = Main.getPlayerInfo(player);
			if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
				player.sendActionBar(DEFLECT_ACTION_BAR);
			}

			ACTIVE_DEFLECT.add(player);
		}

		public void disableDeflect(Player player) {
			//Remove Enchantment from sword(s)
			PlayerInventory inv = player.getInventory();
			inv.all(Material.IRON_SWORD).forEach(
					(k, v) -> v.removeEnchantment(DEFLECT_ENCHANT));
			if(inv.getItemInOffHand().getType() == Material.IRON_SWORD) {
				inv.getItemInOffHand().removeEnchantment(DEFLECT_ENCHANT);
			}

			ACTIVE_DEFLECT.remove(player);
			player.setExp(0f);
			player.stopSound(SoundStop.named(Sound.ITEM_AXE_SCRAPE));
			player.stopSound(SoundStop.named(Sound.BLOCK_BELL_RESONATE));
		}

		/** While player is deflecting, check for nearby deflectable projectiles every tick.
		 */
		public void deflectTick(Player player) {
			World world = player.getWorld();
			RayTraceResult trace = world.rayTraceEntities(player.getEyeLocation(), player.getLocation().getDirection(),
					DEFLECT_RANGE, DEFLECT_HITBOX_SIZE, entity -> ProjDeflect.isDeflectable(player, entity));

			if(trace != null) {
				attemptDeflect(player, trace.getHitEntity());
			}
		}

		public void attemptDeflect(Player player, Entity entity) {
			if(ProjDeflect.tryDeflect(player, entity)) {
				//Deflect was succesful
				World world = player.getWorld();
				LATEST_DEFLECT.put(player, entity);
				//Only 1 projectile is able to be deflected per Deflect use.
				disableDeflect(player);
				world.playSound(player, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.3f);
				world.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
			}
		}

		public void flashFreeze(Player player) {
			//Teleporting the user
			World world = player.getWorld();
			Location initLoc = player.getLocation();
			Location startPoint = initLoc.clone().add(0,player.getEyeHeight(),0);
			Vector dir = initLoc.getDirection();

			RayTraceResult trace = world.rayTraceBlocks(startPoint, dir,
					FLASH_DISTANCE, FluidCollisionMode.NEVER, true);
			Location destination;

			if(trace == null) {
				//No block is hit, so travel the entire distance
				destination = startPoint.clone().add(dir.clone().multiply(FLASH_DISTANCE));
			}
			else {
				//Block is hit, so travel to the hit point
				destination = trace.getHitPosition().toLocation(world,
						initLoc.getYaw(), initLoc.getPitch());
			}

			//Prevent Frost from suffocating itself by correcting location
			//Ignore blocks which are considerably short (i.e. carpet, snow)
			//Also prevent teleporting out of bounds
			while((destination.getBlock().isSolid() &&
					destination.getBlock().getBoundingBox().getHeight() > 0.25) ||
					!Main.getGame().getBorder().contains(destination.toVector())) {

				destination.subtract(dir.clone().multiply(0.1));
			}

			world.playSound(player, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.4f);
			world.spawnParticle(Particle.SNOWFLAKE, initLoc.add(0,1,0), 6, 0,0,0, 0.1f);

			player.teleport(destination);

			world.spawnParticle(Particle.SNOWFLAKE, destination.clone().add(0,1,0), 6, 0,0,0, 0.1f);
			flashFreezeDebuff(player, initLoc.clone(), destination.clone(), dir.clone());
			player.setCooldown(Material.AMETHYST_SHARD, FLASH_CD);
		}

		//Handling applying Debuff + Particle Trail
		public void flashFreezeDebuff(Player user, Location initLoc, Location dest, Vector dir){
			World world = user.getWorld();
			Location departure = initLoc.clone();
			Location currPoint = departure.clone();
			//Adding to y component so everything is centered at the middle of the player's location
			Location destination = dest.clone().add(0, 1, 0);
			double flashDistance = departure.distance(destination);
			int length = ((int) flashDistance);
			HashSet<Player> frostVictims = new HashSet<>();

			//Particle Effect
			for(int i = 0; i <= length; i++) {
				world.spawnParticle(Particle.END_ROD, currPoint, 1, 0,0,0,0);
				currPoint = currPoint.add(dir);
			}

			//Applying Debuff
			for(Player player : Bukkit.getOnlinePlayers()){
				RayTraceResult trace = world.rayTraceEntities(departure, dir, flashDistance,
						FLASH_HITBOX_SIZE, entity -> entity.equals(player));

				if(trace != null){
					frostVictims.add((Player) trace.getHitEntity());
				}
			}

			//Filtering out allies and spectators
			frostVictims.stream()
					.filter(player -> Main.getGame().canAttack(player, user) &&
							!Main.getGame().isSpectator(player))
					.forEach(enemy -> {
						//Applying frost effect to all enemies hit by the flash
						if(!FROSTED_ENTITIES.containsKey(enemy)) {
							enemy.sendMessage(FROSTED_MESSAGE);
						}

				FROSTED_ENTITIES.put(enemy, TeamArena.getGameTick());
					enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
							FROST_DURATION, 0, true));

					world.spawnParticle(Particle.BLOCK_CRACK, enemy.getEyeLocation(), 10,
							0.25,0.25,0.25, Material.FROSTED_ICE.createBlockData());
					world.playSound(enemy, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.8f);
			});
		}
	}
}

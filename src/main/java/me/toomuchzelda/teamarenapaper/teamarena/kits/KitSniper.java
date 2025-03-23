package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.google.common.collect.EvictingQueue;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.centurion.ShieldInstance;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.DisplayUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author onett425
 */
public class KitSniper extends Kit {

	// shared between all kits with grenades
	public static final Component BOMB_LORE = Component.text("Left Click to throw with high velocity", TextUtils.LEFT_CLICK_TO);
	public static final Component BOMB_LORE2 = Component.text("Right Click to toss with low velocity", TextUtils.RIGHT_CLICK_TO);

	public static final ItemStack GRENADE = ItemBuilder.of(Material.TURTLE_HELMET)
			.displayName(Component.text("Frag Grenade"))
			.lore(Component.text("A grenade that deals high explosive damage.", TextColors.LIGHT_YELLOW),
					Component.text("First click to pull the pin, Then click again to throw it!", TextColors.LIGHT_YELLOW),
					Component.text("Make sure to pay attention to its fuse time... (item cd)", TextColors.LIGHT_YELLOW),
					BOMB_LORE,
					BOMB_LORE2)
			.build();

	public static final ItemStack SNIPER = ItemBuilder.of(Material.SPYGLASS)
			.displayName(Component.text("CheyTac Intervention"))
			.build();

	public static final AttributeModifier KNIFE_SPEED = new AttributeModifier(
			new NamespacedKey(Main.getPlugin(), "sniper_knife_speed"),
			0.2, //20% = speed 1
			AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.HAND);

	public KitSniper() {
		super("Sniper", "Be careful when sniping... Too much movement and your aim will worsen. " +
				"Make sure to aim for the head! Don't forget to throw the grenade if you pull the pin btw.", Material.SPYGLASS);

		setArmor(new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE),
				new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_BOOTS));

		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		ItemMeta meta = sword.getItemMeta();
		meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, KNIFE_SPEED);
		sword.setItemMeta(meta);

		setItems(sword, SNIPER, GRENADE);
		setAbilities(new SniperAbility());

		setCategory(KitCategory.RANGED);
	}

	public static class SniperAbility extends Ability {
		public static final @NotNull TextColor SNIPER_COLOR = TextColor.color(0xd28a37);
		private final Set<UUID> RECEIVED_GRENADE_CHAT_MESSAGE = new HashSet<>();
		public static final TextColor GRENADE_MSG_COLOR = TextColor.color(66, 245, 158);

		private static final DamageType REFLECTED_SNIPER = new DamageType(DamageType.SNIPER_HEADSHOT,
			"%Cause% did not have enough map knowledge",
			"%Killed% suffered from %Cause%'s skill issue");

		final List<BukkitTask> grenadeTasks = new ArrayList<>();
		final Random gunInaccuracy = new Random();

		final Map<Player, EvictingQueue<Vector>> inaccuracyTracker = new HashMap<>();

		public static final int SPAWN_PROTECTION_DURATION = 5 * 20;
		final Map<Player, Integer> spawnTime = new HashMap<>();
		final Set<Player> spawnProtectionExpired = new HashSet<>();

		@Override
		public void unregisterAbility() {
			grenadeTasks.forEach(BukkitTask::cancel);
			grenadeTasks.clear();

			inaccuracyTracker.clear();

			spawnTime.clear();
			spawnProtectionExpired.clear();
		}

		@Override
		public void giveAbility(Player player) {
			player.setExp(0.999f);
			spawnTime.put(player, TeamArena.getGameTick());
			inaccuracyTracker.put(player, EvictingQueue.create(3));
			RewindablePlayerBoundingBoxManager.trackClientTick(player);
		}

		@Override
		public void removeAbility(Player player) {
			player.setExp(0);
			player.getInventory().remove(Material.TURTLE_HELMET);
			spawnTime.remove(player);
			spawnProtectionExpired.remove(player);
			inaccuracyTracker.remove(player);
			var uuid = player.getUniqueId();
			RECEIVED_GRENADE_CHAT_MESSAGE.remove(uuid);
			RewindablePlayerBoundingBoxManager.untrackClientTick(player);
		}

		public void throwGrenade(Player player, double amp) {
			World world = player.getWorld();
			Location origin = player.getEyeLocation();
			Item grenade = world.dropItem(origin, new ItemStack(Material.TURTLE_HELMET), item -> {
				item.setCanMobPickup(false);
				item.setCanPlayerPickup(false);
				item.setUnlimitedLifetime(true);
				item.setVelocity(origin.getDirection().multiply(amp));
			});
			world.playSound(origin, Sound.ENTITY_CREEPER_PRIMED, 1f, 1.1f);

			// schedule task
			TeamArenaTeam team = Main.getPlayerInfo(player).team;
			Particle.DustOptions particleOptions = new Particle.DustOptions(team.getColour(), 2);
			BukkitTask runnable = new BukkitRunnable() {
				//Grenade explosion
				int timer = player.getCooldown(Material.TURTLE_HELMET);

				public void run() {
					//Grenade Particles when it is thrown
					//In Motion
					if (!grenade.isOnGround()) {
						world.spawnParticle(Particle.DUST, grenade.getLocation(), 1, particleOptions);
					} else {
						//On the ground
						world.spawnParticle(Particle.DUST, grenade.getLocation(),
								2, 0.5, 0.5,0.5, particleOptions);
					}
					if (timer <= 0) {
						world.createExplosion(grenade.getLocation(), 1.7f, false, false);
						player.getInventory().addItem(GRENADE);
						grenade.remove();
						cancel();
					}
					timer--;
				}
			}.runTaskTimer(Main.getPlugin(), 0, 0);
			grenadeTasks.add(runnable);
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			Material mat = event.getMaterial();
			ItemStack item = event.getItem();
			Player player = event.getPlayer();
			World world = player.getWorld();
			PlayerInventory inv = player.getInventory();
			Action action = event.getAction();
			PlayerInfo pinfo = Main.getPlayerInfo(player);

			//Grenade Pull Pin
			if (mat == Material.TURTLE_HELMET && !player.hasCooldown(Material.TURTLE_HELMET) && player.getExp() == 0.999f && player.getInventory().getItemInMainHand().getType() == Material.TURTLE_HELMET) {
				Component actionBar = Component.text("Left Click to THROW    Right Click to TOSS").color(TextColor.color(242, 44, 44));
				Component text = Component.text("Left Click to throw the grenade, Right Click to lightly toss it").color(TextColor.color(242, 44, 44));
				if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				if (pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					player.sendMessage(text);
				}
				player.setExp(0);
				player.setCooldown(Material.TURTLE_HELMET, (int) 3.5 * 20);
				world.playSound(player, Sound.ITEM_FLINTANDSTEEL_USE, 2.0f, 1.5f);
			}
			//Grenade Throw
			//Main Hand ONLY
			else if (mat == Material.TURTLE_HELMET) {
				event.setCancelled(true);
				if (player.hasCooldown(Material.TURTLE_HELMET) && event.getHand() == EquipmentSlot.HAND) {
					//Removes 1 grenade from hand
					inv.setItemInMainHand(item.subtract());
					if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
						//Left Click => Hard Throw
						throwGrenade(player, 1.5d);
					} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
						//Right Click => Soft Toss
						throwGrenade(player, 0.8d);
					}
				}
			}
		}

		public static final double MAX_INACCURACY = 0.4;
		double calcInaccuracy(Player player) {
			if (KitOptions.sniperAccuracy)
				return 0;
			EvictingQueue<Vector> queue = inaccuracyTracker.get(player);
			if (queue.size() < 2)
				return 0;
			var iterator = queue.iterator();
			Vector lastLocation = iterator.next();
			double total = 0;
			while (iterator.hasNext()) {
				Vector location = iterator.next();
				total += lastLocation.distance(location);
				lastLocation = location;
			}
			return Math.min(MAX_INACCURACY, total / 3);
		}

		Component buildInaccuracyMessage(double inaccuracy) {
			inaccuracy = Math.min(inaccuracy, 0.4);
			int spaces = (int) Math.ceil(inaccuracy * 10);
			TextColor color = inaccuracy < 0.2 ?
				TextColor.lerp((float) (inaccuracy / 0.2f), NamedTextColor.GREEN, NamedTextColor.YELLOW) :
				TextColor.lerp((float) (inaccuracy - 0.2f) / 0.2f, NamedTextColor.YELLOW, NamedTextColor.RED);
			var lb = Component.text(" ".repeat(4 - spaces) + "[" + " ".repeat(spaces), color);
			var rb = Component.text(" ".repeat(spaces) + "]" + " ".repeat(4 - spaces), color);
			return Component.textOfChildren(
				Component.text("   Press "),
				lb, Component.keybind("key.drop", NamedTextColor.YELLOW), rb,
				Component.text(" to shoot")
			);
		}

		public record SniperRayTrace(@Nullable List<EntityHit> entityHits,
									 @Nullable RayTraceResult terminatingBlock) {}
		public record EntityHit(Entity victim, Vector hitPosition, boolean isHeadshot) {}

		public static @NotNull SniperRayTrace doRayTrace(Player player,
														 Map<? extends Player, PlayerBoundingBox> playerHitboxes,
														 List<? extends Entity> entities,
														 Location start, Vector direction, double maxDistance) {
			Vector startVec = start.toVector();

			TreeMap<Double, EntityHit> hitEntities = new TreeMap<>();

			RayTraceResult blockHit = player.getWorld().rayTraceBlocks(start, direction, maxDistance, FluidCollisionMode.NEVER, true);

			double blockHitDistance = maxDistance;
			// limiting the entity search range if we found a block hit:
			if (blockHit != null) {
				blockHitDistance = startVec.distance(blockHit.getHitPosition());
			}

			BoundingBox boundingBox = new BoundingBox();

			for (var entry : playerHitboxes.entrySet()) {
				Player victim = entry.getKey();
				PlayerBoundingBox boxTracker = entry.getValue();
				boundingBox = boxTracker.getBoundingBox(boundingBox);

				RayTraceResult hitResult = boundingBox.rayTrace(startVec, direction, blockHitDistance);
				if (hitResult != null) {
					double distance = startVec.distance(hitResult.getHitPosition());
					// check for headshots
					double headHeight = boxTracker.maxY() - boxTracker.eyeY();
					boundingBox.resize(boxTracker.minX(), boxTracker.eyeY() - headHeight, boxTracker.minZ(),
						boxTracker.maxX(), boxTracker.maxY(), boxTracker.maxZ());
					// limit distance here to prevent the case where the bullet first enters the victim's body
					// and subsequently hits the head hitbox
					boolean headshot = boundingBox.rayTrace(startVec, direction, distance + headHeight / 2) != null;

					hitEntities.put(distance, new EntityHit(victim, hitResult.getHitPosition(), headshot));
				}
			}

			for (var entity : entities) {
				BoundingBox entityBox = entity.getBoundingBox();
				RayTraceResult hitResult = entityBox.rayTrace(startVec, direction, blockHitDistance);

				if (hitResult != null) {
					double distance = startVec.distance(hitResult.getHitPosition());
					// check for headshots
					boolean headshot = false;
					if (entity instanceof LivingEntity livingEntity) {
						double eyeHeight = livingEntity.getEyeHeight();
						double headHeight = (entityBox.getHeight() - eyeHeight) * 2;
						entityBox.resize(entityBox.getMinX(), entityBox.getMaxY() - headHeight, entityBox.getMinZ(),
							entityBox.getMaxX(), entityBox.getMaxY(), entityBox.getMaxZ());
						// limit distance here to prevent the case where the bullet first enters the victim's body
						// and subsequently hits the head hitbox
						headshot = entityBox.rayTrace(startVec, direction, distance + headHeight / 2) != null;
					}
					hitEntities.put(distance, new EntityHit(entity, hitResult.getHitPosition(), headshot));
				}
			}

			// calculate all damage reductions
			List<EntityHit> entityHits = new ArrayList<>(hitEntities.values());
			return new SniperRayTrace(hitEntities.isEmpty() ? null : entityHits, blockHit);
		}

		//Sniper Rifle Shooting
		// reflectShooter is the one who shot reflector, null if not a reflected bullet
		private void fireBullet(World world, Player player, int clientTick, TeamArenaTeam friendlyTeam, Location start, Vector velocity, Player reflectorShooter) {
			int now = TeamArena.getGameTick();
			var playerHitboxes = RewindablePlayerBoundingBoxManager.doRewind(2 + now - clientTick,
				victim -> victim.getGameMode() == GameMode.SURVIVAL && !friendlyTeam.hasMember(victim));
			var otherEntities = world.getLivingEntities().stream()
				.filter(entity -> !(entity instanceof Player) && !friendlyTeam.hasMember(entity)).toList();

			if (CommandDebug.sniperShowRewind) {
				player.sendMessage(Component.text("Rewound 2 + " + (now - clientTick) + " ticks"));
				RewindablePlayerBoundingBoxManager.showRewind(player, 2 + now - clientTick);
			}

			Vector startVector = start.toVector();
			var rayTraceResult = doRayTrace(player, playerHitboxes, otherEntities, start, velocity, 128);

			world.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 2.5f);

			Double shieldDistance = null;
			if (rayTraceResult.entityHits != null) {
				for (EntityHit entityHit : rayTraceResult.entityHits) {
					if (entityHit.isHeadshot)
						player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1, 2);
					else
						player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.5f, 1);

					if (entityHit.victim instanceof Player playerVictim) {
						PlayerInfo victimInfo = Main.getPlayerInfo(playerVictim);
						switch (Kit.getActiveKit(playerVictim)) {
							case KitPorcupine ignored -> {
								if (reflectorShooter == null) { // can't reflect twice
									Location hitLoc = entityHit.hitPosition.toLocation(world);
									KitPorcupine.PorcupineAbility.reflectEffect(playerVictim, hitLoc, true);
									// regulatory compliance
									// reflect the bullet by firing another bullet as if the porcupine is a sniper
									fireBullet(world, playerVictim, clientTick, victimInfo.team,
										hitLoc, velocity.clone().multiply(-1), player);
								}
								return; // the sniper would definitely die
							}
							case KitSniper ignored when !spawnProtectionExpired.contains(playerVictim) -> {
								// no damage
								continue;
							}
							case null, default -> {}
						}
					}
					DamageEvent damageEvent;
					if (reflectorShooter != null) {// is reflected bullet
						damageEvent = DamageEvent.newDamageEvent(entityHit.victim, 1000000, REFLECTED_SNIPER, player, true);
						damageEvent.setDamageTypeCause(reflectorShooter);
					}
					else if (entityHit.isHeadshot)
						damageEvent = DamageEvent.newDamageEvent(entityHit.victim, 150, DamageType.SNIPER_HEADSHOT, player, true);
					else
						damageEvent = DamageEvent.newDamageEvent(entityHit.victim, 15, DamageType.SNIPER_SHOT, player, false);
					Main.getGame().queueDamage(damageEvent);

					// shields block all further damage
					if (entityHit.victim.getPersistentDataContainer().has(ShieldInstance.SHIELD_ENTITY)) {
						shieldDistance = entityHit.hitPosition.distance(startVector);
						break;
					}
				}
			}
			double distance = shieldDistance != null ?
				shieldDistance :
				rayTraceResult.terminatingBlock != null ?
					rayTraceResult.terminatingBlock.getHitPosition().distance(startVector) :
					128;
			showTracers(start, distance, velocity, player);
		}

		@Override
		public void onPlayerDropItem(PlayerDropItemEvent event) {
			Player player = event.getPlayer();
			Item item = event.getItemDrop();
			World world = player.getWorld();
			int now = TeamArena.getGameTick();
			if (item.getItemStack().getType() == Material.SPYGLASS && !player.hasCooldown(Material.SPYGLASS)) {
				removeSpawnProtection(player, "because you fired a shot", SNIPER_COLOR);

				double inaccuracy = calcInaccuracy(player);
				Location start = player.getEyeLocation();
				Vector velocity = start.getDirection();
				if (inaccuracy != 0) {
					var random = new Vector(gunInaccuracy.nextGaussian(), gunInaccuracy.nextGaussian(), gunInaccuracy.nextGaussian());
					random.normalize().multiply(inaccuracy);
					velocity.add(random);
				}

				TeamArenaTeam friendlyTeam = Main.getPlayerInfo(player).team;
				int clientTick = RewindablePlayerBoundingBoxManager.getClientTickOrDefault(player, now);
				fireBullet(world, player, clientTick, friendlyTeam, start, velocity, null);

				//Sniper Cooldown + deleting the dropped sniper and returning a new one.
				if (!KitOptions.sniperAccuracy) {
					player.setCooldown(Material.SPYGLASS, 25);
				}
				event.setCancelled(true);
			}
		}

		private static final Vector UP = new Vector(0, 1, 0);
		private static void showTracers(Location start, double distance, Vector bulletDirection, Player player) {
			// for the shooter, move the line below their crosshair
			Location tracerLocation = start.clone();
			Vector direction = tracerLocation.getDirection();
			if (distance > 2) {
				tracerLocation.add(direction.clone().multiply(2));
			}
			Vector right = UP.getCrossProduct(direction).normalize();
			Vector down = right.getCrossProduct(direction).normalize();
			tracerLocation.add(down.multiply(0.15));

			var displays = DisplayUtils.createLine(tracerLocation, bulletDirection,
				(float) (distance > 2 ? distance - 2 : distance),
				blockDisplay -> {
					blockDisplay.setVisibleByDefault(false);
					player.showEntity(Main.getPlugin(), blockDisplay);
				}, null, Material.RED_CONCRETE.createBlockData());
			DisplayUtils.ensureCleanup(displays, 50);

			TeamArenaTeam team = Main.getPlayerInfo(player).team;
			BlockData glass = Objects.requireNonNull(Registry.MATERIAL.get(NamespacedKey.minecraft(team.getDyeColour().name().toLowerCase(Locale.ENGLISH) + "_stained_glass"))).createBlockData();
			var otherDisplays = DisplayUtils.createLine(start, bulletDirection, (float) distance, blockDisplay -> {
				player.hideEntity(Main.getPlugin(), blockDisplay);
			}, null, glass);
			DisplayUtils.ensureCleanup(otherDisplays);
		}

		@Override
		public void onPlayerTick(Player player) {
			PlayerInfo pinfo = Main.getPlayerInfo(player);
			var uuid = player.getUniqueId();
			float exp = player.getExp();
			World world = player.getWorld();
			var inventory = player.getInventory();

			// Update inaccuracy tracker
			EvictingQueue<Vector> inaccuracyTracker = this.inaccuracyTracker.get(player);
			Vector current = player.getLocation().toVector();
			Vector lastLocation = inaccuracyTracker.peek();
			if (lastLocation != null && current.distanceSquared(lastLocation) > 0.01) // remove spawn protection if moved
				removeSpawnProtection(player, "because you moved", TextColor.color(0x46bd49));
			inaccuracyTracker.add(current);

			// check spawn protection timer
			if (TeamArena.getGameTick() - spawnTime.get(player) >= SPAWN_PROTECTION_DURATION) {
				removeSpawnProtection(player, "over time", TextColor.color(0xfaf25e));
			}

			//Grenade Cooldown
			if (exp == 0.999f) {

			} else if (exp + 0.005f >= 1) {
				player.setExp(0.999f);
			} else {
				player.setExp(exp + 0.005f);
			}

			//Sniper Information message
			if (inventory.getItemInMainHand().getType() == Material.SPYGLASS) {
				if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(buildInaccuracyMessage(calcInaccuracy(player)));
				}
			}

			//Grenade Information message
			if (inventory.getItemInMainHand().getType() == Material.TURTLE_HELMET && player.getExp() == 0.999f) {

				Component actionBar = Component.text("Left/Right Click to Arm", GRENADE_MSG_COLOR);
				Component text = Component.text("Click to arm the grenade", GRENADE_MSG_COLOR);
				if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(actionBar);
				}
				//Chat Message is only sent once per life
				if (pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES) && RECEIVED_GRENADE_CHAT_MESSAGE.add(uuid)) {
					player.sendMessage(text);
				}
			}

			//Grenade Fail Check
			//Check if inventory has any grenades, maybe update later to allow for admin abuse grenade spam
			if (inventory.getHelmet() != null && inventory.getHelmet().getType() == Material.TURTLE_HELMET) {
				player.sendMessage(Component.text("Please do not wear the grenade on your head. Thank you.", GRENADE_MSG_COLOR));

				DamageEvent dEvent = DamageEvent.newDamageEvent(player, 999d, DamageType.EXPLOSION, null, false);
				Main.getGame().queueDamage(dEvent);

				world.createExplosion(player.getLocation(), 2.5f, false, false);
			} else if (player.getCooldown(Material.TURTLE_HELMET) == 1 && inventory.contains(Material.TURTLE_HELMET)) {
				DamageEvent dEvent = DamageEvent.newDamageEvent(player, 999d, DamageType.SNIPER_GRENADE_FAIL, null, false);
				Main.getGame().queueDamage(dEvent);

				world.createExplosion(player.getLocation(), 2.5f, false, false);
			}
			//Sniper Reload Sound
			if (player.getCooldown(Material.SPYGLASS) == 15) {
				player.playSound(player, Sound.ITEM_ARMOR_EQUIP_CHAIN, 2f, 0.8f);
			}
		}

		// spawn protection
		public void removeSpawnProtection(Player player, String reason, TextColor textColor) {
			if (spawnProtectionExpired.add(player)) {
				player.sendMessage(Component.text("Your protective aura has faded " + reason + ".", textColor));
			}
		}
	}

}

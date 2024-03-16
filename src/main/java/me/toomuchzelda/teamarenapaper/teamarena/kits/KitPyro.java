package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import me.toomuchzelda.teamarenapaper.utils.*;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

public class KitPyro extends Kit
{
	public static final ItemStack MOLOTOV_BOW;
	public static final Color MOLOTOV_ARROW_COLOR = Color.fromRGB(255, 84, 10);
	public static final TextColor MOLOTOV_TEXT_COLOR = TextColor.color(255, 84, 10);

	public static final Component MOLOTOV_READY = Component.text("Incendiary ready", NamedTextColor.LIGHT_PURPLE);
	public static final Component CANT_SHOOT_YET = Component.text("You can't fire another incendiary yet!", TextColors.ERROR_RED);

	static {
		MOLOTOV_BOW = new ItemStack(Material.BOW);
		ItemMeta meta = MOLOTOV_BOW.getItemMeta();
		meta.displayName(ItemUtils.noItalics(Component.text("Incendiary launcher", MOLOTOV_TEXT_COLOR)));

		Style style = Style.style(TextUtils.RIGHT_CLICK_TO).decoration(TextDecoration.ITALIC, false);
		List<Component> lore = TextUtils.wrapString(
				"Shoot a flaming arrow that lights the floor on fire and burns enemies that stand in it!\n" +
						"The arrow will bounce off walls and ceilings so you can land some epic trick shots.", style, 200);
		meta.lore(lore);
		MOLOTOV_BOW.setItemMeta(meta);
	}

	public KitPyro() {
		super("Pyro", "I fear no man...\n\nThis kit is equipped to burn everything that's not on its team.\n\n" +
				"With a flaming sword, it can burn up close. " +
				"With a flaming bow, it can burn from afar.\n\n" +
				(!KitOptions.pyroMolotov ?
					"And with an incendiary launcher, it can sear victims nicely, while they're still fresh (alive)."
					:
					"And with explosion-proof armour, it's safer than Obama in his limousine.")
				, Material.FLINT_AND_STEEL);

		ItemStack boots = ItemBuilder.of(Material.CHAINMAIL_BOOTS).enchant(Enchantment.PROTECTION_FIRE, 5).build();

		ItemStack leggings = ItemBuilder.of(Material.LEATHER_LEGGINGS)
			.enchant(Enchantment.PROTECTION_EXPLOSIONS, KitOptions.pyroMolotov ? 3 : 8).build();

		ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
		chestplate.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 3);

		ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
		ItemUtils.colourLeatherArmor(Color.RED, leggings);
		ItemUtils.colourLeatherArmor(Color.RED, chestplate);
		ItemUtils.colourLeatherArmor(Color.RED, helmet);

		this.setArmor(helmet, chestplate, leggings, boots);

		Material swordMat = KitOptions.pyroMolotov ? Material.WOODEN_SWORD : Material.IRON_SWORD;
		ItemStack sword = new ItemStack(swordMat);
		sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);

		ItemStack bow = new ItemStack(Material.BOW);
		ItemMeta bowMeta = bow.getItemMeta();
		bowMeta.addEnchant(Enchantment.ARROW_FIRE, 1, true);
		bowMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
		bow.setItemMeta(bowMeta);

		if (KitOptions.pyroMolotov)
			this.setItems(sword, bow, MOLOTOV_BOW, new ItemStack(Material.ARROW));
		else
			this.setItems(sword, bow, new ItemStack(Material.ARROW));

		this.setAbilities(new PyroAbility());

		setCategory(KitCategory.RANGED);
	}

	public static class PyroAbility extends Ability
	{
		public final Map<Player, Integer> MOLOTOV_RECHARGES = new LinkedHashMap<>();
		public final LinkedList<MolotovInfo> ACTIVE_MOLOTOVS = new LinkedList<>();
		public static final int MOLOTOV_RECHARGE_TIME = 10 * 20;
		public static final int MOLOTOV_ACTIVE_TIME = 5 * 20;
		public static final double BOX_RADIUS = 2.5;

		private final Map<Player, PacketEntity> PREVIEW_ENTITIES = new HashMap<>();

		@Override
		public void onShootBow(EntityShootBowEvent event) {
			Entity e = event.getProjectile();
			if(event.getProjectile() instanceof Arrow arrow) {
				ItemStack bow = event.getBow();
				if (bow != null) {
					Player shooter = (Player) event.getEntity();
					if(bow.isSimilar(MOLOTOV_BOW)) {
						event.setConsumeItem(false);
						//exp bar shows molotov recharge progress
						if (shooter.getExp() == 1f || shooter.getGameMode() == GameMode.CREATIVE) {
							shooter.setExp(0f);
							arrow.setColor(MOLOTOV_ARROW_COLOR);
							arrow.setPierceLevel(127); //don't want it directly colliding with players
							MOLOTOV_RECHARGES.put(shooter, TeamArena.getGameTick());
						} else {
							event.setCancelled(true);
							shooter.sendMessage(CANT_SHOOT_YET);
							shooter.playSound(shooter.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.AMBIENT,
									1f, 0.5f);
						}
						shooter.updateInventory();
					}
				}
			}
		}

		@Override
		public void onTick() {
			int currentTick = TeamArena.getGameTick();
			var itemIter = MOLOTOV_RECHARGES.entrySet().iterator();
			while(itemIter.hasNext()) {
				Map.Entry<Player, Integer> entry = itemIter.next();

				int diff = currentTick - entry.getValue();
				float percent = (float) diff / (float) MOLOTOV_RECHARGE_TIME;

				Player player = entry.getKey();
				if(percent >= 1f) {
					itemIter.remove();
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT,
							0.3f, 1f);
					PlayerUtils.sendKitMessage(player, MOLOTOV_READY, MOLOTOV_READY);
				}

				percent = MathUtils.clamp(0f, 1f, percent);
				player.setExp(percent);
			}

			var iter = ACTIVE_MOLOTOVS.iterator();
			while(iter.hasNext()) {
				MolotovInfo minfo = iter.next();
				if (currentTick - minfo.spawnTime >= MOLOTOV_ACTIVE_TIME) {
					iter.remove();
					continue;
				}
				World world = minfo.thrower.getWorld();
				for (int i = 0; i < 2; i++) {
					Location randomLoc = minfo.box.getCenter().toLocation(world);

					randomLoc.add(MathUtils.randomRange(-BOX_RADIUS, BOX_RADIUS),
							MathUtils.randomRange(-0.1, 0.5),
							MathUtils.randomRange(-BOX_RADIUS, BOX_RADIUS));

					randomLoc.getWorld().spawnParticle(Particle.FLAME, randomLoc, 1, 0, 0, 0, 0);

					ParticleUtils.colouredRedstone(randomLoc, minfo.color, 1, 2);
				}

				for (Entity entity : world.getNearbyEntities(minfo.box)) {
					if (!(entity instanceof LivingEntity living)) // only damage living entities
						continue;
					if (living instanceof Player p && Main.getGame().isSpectator(p))
						continue;
					if (living == minfo.thrower)
						continue;

					DamageEvent damageEvent = DamageEvent.newDamageEvent(living, 1.75d, DamageType.PYRO_MOLOTOV, minfo.thrower(), false);
					Main.getGame().queueDamage(damageEvent);
				}
			}
		}

		@Override
		public void onPlayerTick(Player player) {
			if (TeamArena.getGameTick() % 5 != 0)
				return;
			if (player.getInventory().getItemInMainHand().getType() != Material.FIRE_CHARGE) {
				var oldPreviewEntity = PREVIEW_ENTITIES.remove(player);
				if (oldPreviewEntity != null)
					oldPreviewEntity.remove();
				return;
			}

			World world = player.getWorld();
			Location location = player.getEyeLocation();
			Vector velocity = location.getDirection();
			location.add(velocity);
			velocity.add(player.getVelocity());
			var acceleration = new Vector(0, 0.03, 0); // constant for snowballs
			var dustOptions = new Particle.DustOptions(Color.BLACK, 1);
			for (int i = 0; i < 100; i++) { // ticks
				// check inside blocks
				var hitResult = world.rayTrace(location, velocity, velocity.length(), FluidCollisionMode.NEVER,
						true, 0, entity -> entity instanceof Player);
				if (hitResult != null) {
					var block = hitResult.getHitBlock();
					var blockFace = hitResult.getHitBlockFace();
					if (block != null && blockFace != null) {
						Vector dirVector = blockFace.getDirection();
						if (blockFace == BlockFace.UP) {
							var packetEntity = spawnPreviewBlock(location, block, player);

							var oldPreviewEntity = PREVIEW_ENTITIES.put(player, packetEntity);
							if (oldPreviewEntity != null)
								oldPreviewEntity.remove();
							packetEntity.respawn();

							return;
						} else if (blockFace != BlockFace.DOWN) {
							dirVector.setY(0).normalize();
						}

						velocity.subtract(dirVector.multiply(2 * velocity.dot(dirVector))).multiply(0.25);
					}
				}

				location.add(velocity);
				player.spawnParticle(Particle.REDSTONE, location, 1, 0, 0, 0, 0, dustOptions);
				velocity.multiply(0.99); // drag
				velocity.subtract(acceleration);
			}
			// nothing was spawned
			var oldPreviewEntity = PREVIEW_ENTITIES.remove(player);
			if (oldPreviewEntity != null)
				oldPreviewEntity.remove();
		}

		PacketEntity spawnPreviewBlock(Location location, Block block, Player player) {
			// MC-114286: falling blocks do not render at all if they have
			// the same block state as the block they are currently occupying
			var blockData = Material.FIRE.createBlockData(); // block.getBlockData();
			PacketEntity packetEntity = new PacketEntity(PacketEntity.NEW_ID, EntityType.FALLING_BLOCK,
					location.clone().set(location.getBlockX() + 0.5, location.getY() - 0.00001, location.getBlockZ()),
					List.of(player), null);

			packetEntity.setBlockType(blockData);

			// glowing
			packetEntity.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_GLOWING_MASK);
			// no gravity
			packetEntity.setMetadata(MetaIndex.NO_GRAVITY_OBJ, true);
			packetEntity.refreshViewerMetadata();
			return packetEntity;
		}

		void spawnMolotov(Player owner, Location location) {
			Location corner1 = location.clone().add(BOX_RADIUS, -0.1, BOX_RADIUS);
			Location corner2 = location.clone().add(-BOX_RADIUS, 0.5, -BOX_RADIUS);

			BoundingBox box = BoundingBox.of(corner1, corner2);
			//shooter will always be a player because this method will only be called if the projectile of a pyro hits smth
			ACTIVE_MOLOTOVS.add(new MolotovInfo(box, owner, Main.getPlayerInfo(owner).team.getColour(), TeamArena.getGameTick()));

			location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, 2, 2f);
			location.getWorld().playSound(location, Sound.BLOCK_FIRE_AMBIENT, 2, 0.5f);
		}

		//called manually in EventListeners.projectileHit
		// spawn the molotov effect
		@Override
		public void onProjectileHit(ProjectileHitEvent event) {
			Projectile projectile = event.getEntity();

			if (projectile instanceof Arrow || projectile instanceof Snowball) {
				// use the colour to know if it's a molotov arrow
				if (projectile instanceof Arrow arrow && !Objects.equals(arrow.getColor(), MOLOTOV_ARROW_COLOR))
					return;

				//if it's a molotov and it hit a mob, slow the arrow down on X and Z significantly
				// and don't register the hit
				if(event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity) {
					Vector vel = projectile.getVelocity();
					vel.setX(vel.getX() * 0.4);
					vel.setZ(vel.getZ() * 0.4);
					projectile.setVelocity(vel);
				}

				if (event.getHitBlockFace() == BlockFace.UP) {
					//shooter will always be a player because this method will only be called if the projectile of a pyro hits smth
					Player player = (Player) projectile.getShooter();
					Location loc = event.getEntity().getLocation();
					loc.setY(event.getHitBlock().getY() + 1); //set it to floor level of hit floor

					spawnMolotov(player, loc);
				} else if(event.getHitBlockFace() != null) { // it hit a wall, make it not stick in the wall
					event.setCancelled(true);

					//credit jacky8399 for bouncing arrow code
					BlockFace hitFace = event.getHitBlockFace();
					Vector dirVector = hitFace.getDirection();
					if (hitFace != BlockFace.DOWN) { // ignore Y component
						dirVector.setY(0).normalize();
					}
					Vector velocity = event.getEntity().getVelocity();

					// https://math.stackexchange.com/questions/13261/how-to-get-a-reflection-vector
					Vector newVelocity = velocity.subtract(dirVector.multiply(2 * velocity.dot(dirVector)));
					newVelocity.multiply(0.25);
					projectile.getWorld().spawn(projectile.getLocation(), projectile.getClass(), newProjectile -> {
						newProjectile.setShooter(projectile.getShooter());
						newProjectile.setVelocity(newVelocity);
						if (projectile instanceof Arrow arrow) {
							Arrow newArrow = (Arrow) newProjectile;
							newArrow.setColor(arrow.getColor());
							newArrow.setDamage(arrow.getDamage());
							newArrow.setKnockbackStrength(arrow.getKnockbackStrength());
							newArrow.setShotFromCrossbow(arrow.isShotFromCrossbow());
							newArrow.setPickupStatus(arrow.getPickupStatus());
						} else {
							Snowball snowball = (Snowball) projectile;
							Snowball newSnowball = (Snowball) newProjectile;
							newSnowball.setItem(snowball.getItem());
							newSnowball.setVisualFire(snowball.isVisualFire());
						}
					});
					projectile.remove();
				}
			}
		}

		//Cancel all damage from molotov arrow direct hits.
		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if(event.getDamageType().isProjectile() && event.getAttacker() instanceof Arrow arrow) {
				if(MOLOTOV_ARROW_COLOR.equals(arrow.getColor())) {
					event.setCancelled(true);
				}
			}
		}

		/*@Override
		public void onProjectileHitEntity(ProjectileCollideEvent event) {
			if (event.getEntity() instanceof Arrow arrow) {
				if (arrow.getColor() != null && arrow.getColor().equals(MOLOTOV_ARROW_COLOR)) {
					event.setCancelled(true);
					Vector vel = arrow.getVelocity();
					vel.setX(vel.getX() * 0.4);
					vel.setZ(vel.getZ() * 0.4);
					arrow.setVelocity(vel);
				}
			}
		}*/

		@Override
		public void onInteract(PlayerInteractEvent event) {
			var player = event.getPlayer();
			if (event.getMaterial() == Material.FIRE_CHARGE) {
				var location = player.getEyeLocation();
				var direction = location.getDirection();
				var playerVelocity = player.getVelocity();
				var velocity = event.getAction().isLeftClick() ? direction : direction.multiply(0.5);
				velocity.add(playerVelocity);
				player.getWorld().spawn(location.add(direction), Snowball.class, snowball -> {
					snowball.setShooter(player);
					snowball.setVelocity(velocity);
					snowball.setGravity(true);
					snowball.setInvulnerable(true);
					snowball.setItem(new ItemStack(Material.FIRE_CHARGE));
					snowball.setVisualFire(true);
				});
			}
		}

		@Override
		public void giveAbility(Player player) {
			player.setExp(1f);
		}

		@Override
		public void removeAbility(Player player) {
			player.setExp(0f);
			var packetEntity = PREVIEW_ENTITIES.remove(player);
			if (packetEntity != null)
				packetEntity.remove();
		}

		@Override
		public void unregisterAbility() {
			MOLOTOV_RECHARGES.clear();
			ACTIVE_MOLOTOVS.clear();
			PREVIEW_ENTITIES.values().forEach(PacketEntity::remove);
			PREVIEW_ENTITIES.clear();
		}

		@Override
		public void onDeath(DamageEvent event) {
			PREVIEW_ENTITIES.remove(event.getPlayerVictim());
			MOLOTOV_RECHARGES.remove(event.getPlayerVictim());
		}
	}

	public record MolotovInfo(BoundingBox box, Player thrower, Color color, int spawnTime) {}
}

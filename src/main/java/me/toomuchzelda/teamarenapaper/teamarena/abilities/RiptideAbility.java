package me.toomuchzelda.teamarenapaper.teamarena.abilities;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DetailedProjectileHitEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.ProjectileReflectEvent;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Range;

import java.util.*;

public class RiptideAbility extends Ability {
	public static final NamespacedKey TRIDENT_KEY = new NamespacedKey(Main.getPlugin(), "riptide_ability");
	public static final NamespacedKey TRIDENT_DAMAGE = new NamespacedKey(Main.getPlugin(), "riptide_damage");
	public static final ItemStack TRIDENT = ItemBuilder.of(Material.TRIDENT)
		.enchant(Enchantment.LOYALTY, 1)
		.enchantmentGlint(false)
		.meta(meta -> {
			var lore = new ArrayList<Component>(List.of(
				Component.text("Your trusty trident. Being near water allows you", NamedTextColor.YELLOW),
				Component.text("to increase the strength of the trident.", NamedTextColor.YELLOW),
				Component.empty(),
				Component.text("Legend:", NamedTextColor.GRAY)
			));
			for (RiptideFactor factor : RiptideFactor.values()) {
				lore.add(Component.textOfChildren(factor.display, Component.space(), factor.explanation.colorIfAbsent(NamedTextColor.GRAY)));
			}
			meta.lore(lore.stream().map(component -> component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)).toList());

			meta.getPersistentDataContainer().set(TRIDENT_KEY, PersistentDataType.BOOLEAN, true);
			meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
			meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
				new AttributeModifier(TRIDENT_DAMAGE, 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
		})
		.build();
	private static final AttributeModifier SNEAK_SPEED = new AttributeModifier(
		new NamespacedKey(Main.getPlugin(), "riptide_sneak_speed"), 1, AttributeModifier.Operation.ADD_NUMBER);

	public RiptideAbility() {

	}

	private static final int RIPTIDE_IMPACT_BOOST_DURATION = 20;
	static class RiptideInfo {
		BossBar bossBar;
		BlockCoords fakeWaterBlockCoords;
		long fakeWaterBlockKey = 0;
		BlockState serverFakeWater = null;
		boolean wasFakeWater = false;
		/** The last time player hit an enemy with a trident projectile */
		int lastImpact = -RIPTIDE_IMPACT_BOOST_DURATION;

		private boolean burdened;
		@Range(from = 0, to = 4)
		private float progress = 0;
		private float lastProgress = 0f;
		private int interpolationStart;
		private int interpolationDuration = 0;
		// stats
		double totalProgress = 0;
		int startTick;

		RiptideInfo(Player player) {
			bossBar = BossBar.bossBar(buildBarTitle(Set.of(), 0), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
			bossBar.addViewer(player);
			startTick = TeamArena.getGameTick();
		}

		Component buildBarTitle(Set<RiptideFactor> factors, float increment) {
			var builder = Component.text();
			if (!factors.isEmpty()) {
				for (RiptideFactor factor : factors) {
					builder.append(factor.display);
				}
				builder.appendSpace();
			}
			if (progress < 1) {
				builder.append(Component.text("Riptide", NamedTextColor.GRAY));
			} else if (progress <= 3.5f) {
				builder.append(Component.text("Riptide " + (int) progress));
			} else {
				// vibrate
				Component text = Component.text("Riptide 3", NamedTextColor.YELLOW);
				Component space = Component.text("  ");
				builder.append(TeamArena.getGameTick() % 20 < 10 ?
					Component.textOfChildren(text, space) :
					Component.textOfChildren(space, text));
			}
			if (increment > 0) {
				int nextMilestone = (int) Math.ceil(progress + 0.001f);
				float secondsUntilMilestone = (nextMilestone - progress) / increment / 20f;
				builder.append(Component.text(" - " + TextUtils.formatNumber(secondsUntilMilestone) + "s "));
				if (nextMilestone != 4) {
					builder.append(Component.text("to Riptide " + nextMilestone));
				} else {
					builder.append(Component.text("till OVERLOAD", TeamArena.getGameTick() % 20 < 10 ? NamedTextColor.GOLD : NamedTextColor.YELLOW));
				}
			}
			return builder.build();
		}

		void update(Set<RiptideFactor> factors, float increment) {
			if (burdened) {
				bossBar.color(BossBar.Color.RED);
				bossBar.name(Component.text("The weight of the flag bears down on you", NamedTextColor.RED));
			} else {
				bossBar.color(progress < 1 ? BossBar.Color.YELLOW : BossBar.Color.WHITE);
				bossBar.name(buildBarTitle(factors, increment));
			}
			if (interpolationDuration == 0 || (TeamArena.getGameTick() - interpolationStart > interpolationDuration)) {
				bossBar.progress(progress / 4);
			} else {
				float t = (float) (TeamArena.getGameTick() - interpolationStart) / interpolationDuration;
				bossBar.progress(MathUtils.lerp(lastProgress, progress, t) / 4);
			}
		}

		void setBurdened(boolean burdened) {
			this.burdened = burdened;
		}

		void setProgress(float progress) {
			this.progress = MathUtils.clamp(0, 4, progress);
		}

		void setProgress(float progress, int interpolationDuration) {
			this.lastProgress = this.progress;
			this.progress = MathUtils.clamp(0, 4, progress);
			this.interpolationStart = TeamArena.getGameTick();
			this.interpolationDuration = interpolationDuration;
		}

		float getProgress() {
			return progress;
		}

		void cleanUp(Player player) {
			bossBar.removeViewer(player);
			if (fakeWaterBlockKey != 0) {
				Main.getGame().getFakeBlockManager().removeFakeBlock(fakeWaterBlockCoords, fakeWaterBlockKey);
			}
		}
	}

	Map<Player, RiptideInfo> riptideInfoMap = new HashMap<>();

	/**
	 * Factors that influence the riptide progress.
	 * Each factor contribute to the player's progress differently.
	 * The contribution can be modeled as a piecewise function as follows:
	 * <pre>
	 * contribution = lerp(normal_start, normal_end, progress / 3) if progress <= 3
	 * contribution = lerp(overload_start, overload_end, progress - 3) if progress > 3
	 * </pre>
	 * Where progress is the player's current riptide progress. (0 <= progress <= 4)
	 */
	public enum RiptideFactor {
		// weather-based factors
		SUNNY(Component.text("☀", TextColor.color(0xFEE137)), Component.text("You can feel the warmth of the sun."),
			0.04f, 0.01f, 0),
		STORM(Component.text("☔", TextColor.color(0x937DF2)), Component.text("The raindrops moisturize your skin."),
			0.2f, 0.1f, 0.005f),
		THUNDERING(Component.text("⛈", TextColor.color(0xFFFECE)), Component.text("The thunderstorm makes your cells shiver in excitement."),
			5, 2f, 0.075f), // let's get crazy
		// environment factors
		WATER(Component.text("\uD83D\uDCA6", TextColor.color(0x6FCAFF)), Component.text("Water is your domain."),
			1, 0.8f, 0.2f),
		DEEP_WATER(Component.text("\uD83C\uDF0A", TextColor.color(0x6FCAFF)), Component.text("You are thriving below the water surface."),
			1.8f, 1.5f, 1),
		// troll factors
		BLUE_TEAM(Component.text("⚑", NamedTextColor.BLUE), Component.text("Blue Team members do look like water, but no."), 0),
		// impact-based factors
		IMPACT_BOOST(Component.text("🔱", TextColor.color(0x274036/*0x579B8C*/)), Component.text("You feel an adrenaline rush from masterfully impaling your enemy."),
			0.2f),
		;
		public final Component display, explanation;
		public final float normalStart, normalEnd, overloadStart, overloadEnd;
		RiptideFactor(Component display, Component explanation, float normalStart, float normalEnd, float overloadStart, float overloadEnd) {
			this.display = display;
			this.explanation = explanation;
			this.normalStart = normalStart;
			this.normalEnd = normalEnd;
			this.overloadStart = overloadStart;
			this.overloadEnd = overloadEnd;
		}

		RiptideFactor(Component display, Component explanation, float normalStart, float normalEnd, float overload) {
			this(display, explanation, normalStart, normalEnd, overload, overload);
		}

		RiptideFactor(Component display, Component explanation, float incrementPerSecond) {
			this(display, explanation, incrementPerSecond, incrementPerSecond, incrementPerSecond, incrementPerSecond);
		}
	}

	public Set<RiptideFactor> calcFactors(Player player) {
		EnumSet<RiptideFactor> factors = EnumSet.noneOf(RiptideFactor.class);
		World world = player.getWorld();
		Location location = player.getLocation(), eyeLocation = player.getEyeLocation();
		boolean canSeeSky = world.getHighestBlockYAt(eyeLocation, HeightMap.MOTION_BLOCKING_NO_LEAVES) <= eyeLocation.getY();
		if (canSeeSky) {
			if (world.isThundering())
				factors.add(RiptideFactor.THUNDERING);
			else if (world.hasStorm())
				factors.add(RiptideFactor.STORM);
			else
				factors.add(RiptideFactor.SUNNY);
		}

		if (player.isUnderWater())
			factors.add(RiptideFactor.DEEP_WATER);
		else if (player.isInWater())
			factors.add(RiptideFactor.WATER);

		RiptideInfo riptideInfo = riptideInfoMap.get(player);
		if (riptideInfo != null && TeamArena.getGameTick() - riptideInfo.lastImpact <= RIPTIDE_IMPACT_BOOST_DURATION) {
			factors.add(RiptideFactor.IMPACT_BOOST);
		}

		team_loop:
		for (TeamArenaTeam team : Main.getGame().getTeams()) {
			if (team.getDyeColour() == DyeColor.BLUE) {
				Location temp = location.clone();
				for (Player playerMember : team.getPlayerMembers()) {
					playerMember.getLocation(temp);
					if (temp.getWorld() == world && temp.distanceSquared(location) <= 6 * 6) {
						factors.add(RiptideFactor.BLUE_TEAM);
						break team_loop;
					}
				}
			}
		}
		return factors;
	}

	public float calcProgressIncrement(Set<RiptideFactor> factors, float progress) {
		float sum = 0;
		boolean overload = progress <= 3;
		float t = overload ? progress / 3 : progress - 3;
		for (RiptideFactor factor : factors) {
			sum += overload ?
				MathUtils.lerp(factor.normalStart, factor.normalEnd, t) :
				MathUtils.lerp(factor.overloadStart, factor.overloadEnd, t);
		}
		return sum / 20;
	}

	@Override
	protected void giveAbility(Player player) {
		riptideInfoMap.put(player, new RiptideInfo(player));
		player.getInventory().addItem(TRIDENT);
	}

	@Override
	protected void removeAbility(Player player) {
		RiptideInfo info = riptideInfoMap.remove(player);
		player.sendMessage(Component.textOfChildren(
			Component.text("Your average "),
			Component.text("Riptide", NamedTextColor.AQUA),
			Component.text(" level was "),
			Component.text("%.2f".formatted(info.totalProgress / (TeamArena.getGameTick() - info.startTick)), NamedTextColor.YELLOW),
			Component.text(".")
		));
		info.cleanUp(player);
		// only remove our trident in case of admin abuse
		for (var iter = player.getInventory().iterator(); iter.hasNext();) {
			ItemStack stack = iter.next();
			if (stack != null && stack.getType() == Material.TRIDENT && stack.getPersistentDataContainer().has(TRIDENT_KEY)) {
				iter.set(null);
			}
		}
		Objects.requireNonNull(player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY)).removeModifier(SNEAK_SPEED);
	}

	@Override
	public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
		if (event.getProjectile() instanceof Trident trident && trident.getShooter() instanceof Player player) {
			RiptideInfo riptideInfo = riptideInfoMap.get(player);
			int riptide = (int) riptideInfo.progress;
			trident.setDamage(4 + riptide);
			riptideInfo.setProgress(riptideInfo.getProgress() - riptide / 3f, 20);
			TeamArenaTeam team = Main.getPlayerInfo(player).team;
			GlowUtils.setGlowing(List.of((player)), List.of(trident), true, team != null ? NamedTextColor.nearestTo(team.getRGBTextColor()) : NamedTextColor.AQUA);
		}
	}

	public void updateItems(Player player, int level, boolean updateMeta) {
		boolean canRiptide = canRiptide(player);
		for (var iter = player.getInventory().iterator(); iter.hasNext();) {
			ItemStack stack = iter.next();
			if (stack != null && stack.getType() == Material.TRIDENT && stack.getPersistentDataContainer().has(TRIDENT_KEY)) {
				ItemMeta meta = stack.getItemMeta();
				boolean changed = updateMeta;
				if (level != 0 && canRiptide) {
					changed |= meta.addEnchant(Enchantment.RIPTIDE, level, true);
				} else {
					changed |= meta.removeEnchant(Enchantment.RIPTIDE);
				}
				if (updateMeta) {
					meta.itemName(switch (level) {
						case 0 -> Component.text("Trident");
						case 1 -> Component.text("Trident Plus", NamedTextColor.YELLOW);
						case 2 -> Component.text("Trident Pro", NamedTextColor.GOLD);
						default -> Component.text("Trident Pro Max", NamedTextColor.RED);
					});
					meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
					meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
						new AttributeModifier(TRIDENT_DAMAGE, 4 + level, AttributeModifier.Operation.ADD_NUMBER,
							EquipmentSlotGroup.MAINHAND));
				}
				meta.setEnchantmentGlintOverride(level != 0);
				if (changed) {
					stack.setItemMeta(meta);
					iter.set(stack);
				}
			}
		}
	}

	private static boolean canRiptide(Player player) {
		return !(Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player)) &&
			((player.isSneaking() && player.isOnGround()  /* TODO replace with more robust check */) || player.isInWaterOrRain());
	}

	private static final BlockData SHALLOW_WATER_DATA = Material.WATER.createBlockData(blockData -> ((Levelled) blockData).setLevel(7));
	@Override
	public void onPlayerTick(Player player) {
		RiptideInfo info = riptideInfoMap.get(player);
		boolean burdened = Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player);
		info.setBurdened(burdened);

		int oldProgress = (int) info.progress;
		Set<RiptideFactor> factors = calcFactors(player);
		float increment = calcProgressIncrement(factors, info.progress);
		if (!burdened)
			info.setProgress(info.progress + increment);
		else
			info.setProgress(Math.min(3, info.progress + increment));

		if (info.progress == 4) {
			// overload!
			player.setVelocity(new Vector(MathUtils.random.nextGaussian(), 2.5, MathUtils.random.nextGaussian()));
			player.startRiptideAttack(20, 0, null);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false, false));
			info.setProgress(0, 5);
		}
		info.totalProgress += info.progress;

		boolean changed = (int) info.progress != oldProgress;
		if (changed && (int) info.progress > oldProgress) {
			player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1f);
		}
		info.update(factors, increment);
		Component actionBar;

		var attribute = Objects.requireNonNull(player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY));
		if (info.progress >= 1 && !burdened) {
			// is sneaking
			if (canRiptide(player) && !player.isInWaterOrRain()) {
				if (attribute.getModifier(SNEAK_SPEED.getKey()) == null)
					attribute.addModifier(SNEAK_SPEED);
				BlockCoords targetCoords = new BlockCoords(player.getEyeLocation());
				if (!targetCoords.equals(info.fakeWaterBlockCoords)) {
					if (info.fakeWaterBlockKey != 0) {
						Main.getGame().getFakeBlockManager().removeFakeBlock(info.fakeWaterBlockCoords, info.fakeWaterBlockKey);
					}
					info.fakeWaterBlockCoords = targetCoords;
					info.fakeWaterBlockKey = Main.getGame().getFakeBlockManager().setFakeBlock(info.fakeWaterBlockCoords, SHALLOW_WATER_DATA, p -> p == player);
				}
				player.sendPotionEffectChange(player, new PotionEffect(PotionEffectType.CONDUIT_POWER, 1000000, 0, true));
			} else {
				player.sendPotionEffectChangeRemove(player, PotionEffectType.CONDUIT_POWER);
				attribute.removeModifier(SNEAK_SPEED);
				if (info.fakeWaterBlockKey != 0) {
					Main.getGame().getFakeBlockManager().removeFakeBlock(info.fakeWaterBlockCoords, info.fakeWaterBlockKey);
					info.fakeWaterBlockKey = 0;
					info.fakeWaterBlockCoords = null;
				}
			}

			if (canRiptide(player)) {
				actionBar = Component.text("Ready to Riptide", NamedTextColor.AQUA);
				if (player.getActiveItem().getType() == Material.TRIDENT) {
					List<Vector> vectors = estimateRiptideLocation(player, player.getActiveItem());
					if (!vectors.isEmpty()) {
						var cache = new PacketSender.Cached(1, vectors.size());
						Particle.DustOptions dustOptions = new Particle.DustOptions(Color.AQUA, 1);
						Location eye = player.getEyeLocation();
						Location temp = player.getLocation();
						for (Vector vector : vectors) {
							temp.set(vector.getX(), vector.getY(), vector.getZ());
							if (eye.distanceSquared(temp) <= 2 * 2) continue;
							ParticleUtils.batchParticles(
								player, cache, Particle.DUST, dustOptions, temp,
								64, 0, 0, 0, 0, 0, true
							);
						}
						cache.flush();
					}
				}
			} else if (!player.isOnGround()) {
				actionBar = Component.text("Stay on ground to Riptide", NamedTextColor.GRAY);
			} else {
				actionBar = Component.textOfChildren(
					Component.text("["),
					Component.keybind("key.sneak", NamedTextColor.GREEN),
					Component.text("] to Riptide")
				).color(NamedTextColor.GRAY);
			}
		} else {
			actionBar = Component.text("Can't riptide", NamedTextColor.DARK_GRAY);
		}
		player.sendActionBar(actionBar);

		updateItems(player, Math.min(3, (int) info.progress), changed);
	}

	private static final double ACCELERATION = 0.08;
	private static final double VERTICAL_DRAG = 1 - 0.02;
	private static final double HORIZONTAL_DRAG = 1 - 0.09;
	private static List<Vector> estimateRiptideLocation(Player player, ItemStack stack) {
		Location location = player.getLocation();
		if (player.isOnGround())
			location.add(0, 1.2, 0); // lol okay
		World world = player.getWorld();
		Vector velocity = ItemUtils.getRiptidePush(stack, player);
		if (velocity == null)
			return List.of();
		var list = new ArrayList<Vector>();
		for (int i = 0; i < 50; i++) {
			list.add(location.toVector());
			var hitResult = world.rayTraceBlocks(location, velocity, velocity.length(), FluidCollisionMode.ALWAYS, true);
			if (hitResult != null)
				break;
			location.add(velocity);
			velocity.setY((velocity.getY() - ACCELERATION) * VERTICAL_DRAG);
			velocity.setX(velocity.getX() * HORIZONTAL_DRAG);
			velocity.setZ(velocity.getZ() * HORIZONTAL_DRAG);
		}
		return list;
	}

	@Override
	public void onProjectileHit(DetailedProjectileHitEvent event) {
		ProjectileHitEvent projectileHitEvent = event.projectileHitEvent;
		Projectile projectile = projectileHitEvent.getEntity();
		if (projectile instanceof Trident trident &&
			projectileHitEvent.getHitEntity() != null &&
			trident.getShooter() instanceof Player player) {
			RiptideInfo info = riptideInfoMap.get(player);
			info.lastImpact = TeamArena.getGameTick();
		}
	}

	@Override
	public void onRiptide(PlayerRiptideEvent event) {
//		event.getPlayer().sendMessage("Riptide");
		RiptideInfo info = riptideInfoMap.get(event.getPlayer());
		info.setProgress(0, 10);
		updateItems(event.getPlayer(), 0, true);
		// restore old block
		if (info.wasFakeWater) {
			info.wasFakeWater = false;
			((CraftPlayer) event.getPlayer()).getHandle().wasTouchingWater = false;
		}

		if (info.serverFakeWater != null) {
			info.serverFakeWater.update(true, false);
			info.serverFakeWater = null;
		}
	}

	// set serverside water to enable riptide damage
	@Override
	public void onInteract(PlayerInteractEvent event) {
		ItemStack item = event.getItem();
		if (item == null || item.getType() != Material.TRIDENT || item.getEnchantmentLevel(Enchantment.RIPTIDE) == 0)
			return;
		Player player = event.getPlayer();
		if (player.isInWaterOrRain())
			return; // no need to set fake water
		((CraftPlayer) player).getHandle().wasTouchingWater = true;
		riptideInfoMap.get(player).wasFakeWater = true;
	}

	@Override
	public void onStopUsingItem(PlayerStopUsingItemEvent event) {
		ItemStack item = event.getItem();
		if (item.getType() != Material.TRIDENT || item.getEnchantmentLevel(Enchantment.RIPTIDE) == 0 || event.getTicksHeldFor() < 10)
			return;
		Player player = event.getPlayer();
		if (player.isInWaterOrRain())
			return; // no need to set fake water
		((CraftPlayer) player).getHandle().wasTouchingWater = true;
		riptideInfoMap.get(player).wasFakeWater = true;
	}

	@Override
	public void onAttemptedAttack(DamageEvent event) {
		if (event.getAttacker() instanceof Trident) {
			event.setDamageType(DamageType.TRIDENT_PROJECTILE);
		} else if (event.getDamageType().is(DamageType.MELEE) &&
			event.getMeleeWeapon().getType() == TRIDENT.getType()) {

			event.setDamageType(DamageType.TRIDENT_MELEE);
		}
	}

	// janky code to ensure loyalty works
	@Override
	public void onReflect(ProjectileReflectEvent event) {
		final Player shooter = (Player) event.projectile.getShooter();
		event.hitFunc = (porc, dEvent) -> {
			Trident trident = (Trident) dEvent.projectileHitEvent.getEntity();
			if (dEvent.projectileHitEvent.getHitBlock() != null) {
				trident.setLoyaltyLevel(1);
				trident.setShooter(shooter);
			} else {
				trident.setLoyaltyLevel(0);
			}
		};
		event.attackFunc = dEvent -> {
			dEvent.setDamageType(DamageType.TRIDENT_PROJECTILE);
			dEvent.setDamageTypeCause(shooter);
		};
	}
}

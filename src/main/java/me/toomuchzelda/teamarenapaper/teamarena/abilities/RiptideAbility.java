package me.toomuchzelda.teamarenapaper.teamarena.abilities;

import com.comphenix.protocol.events.PacketContainer;
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
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketDisplay;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
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
import org.bukkit.entity.Entity;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.*;

import static net.kyori.adventure.text.Component.*;

public class RiptideAbility extends Ability {
	/** The maximum Riptide level given to player tridents */
	public static final int MAX_RIPTIDE_LEVEL = 3;
	/** The maximum Riptide progress the player can store */
	public static final int MAX_RIPTIDE_PROGRESS = 4;
	/** Whether the player will fly into the sky upon reaching MAX_RIPTIDE_PROGRESS */
	public static final boolean CAN_OVERLOAD = false;
	/** Whether the trident can be thrown as a projectile */
	public static final boolean CAN_THROW = false;

	public static final NamespacedKey TRIDENT_KEY = new NamespacedKey(Main.getPlugin(), "riptide_ability");
	public static final NamespacedKey TRIDENT_DAMAGE = new NamespacedKey(Main.getPlugin(), "riptide_damage");
	public static final ItemStack TRIDENT = ItemBuilder.of(Material.TRIDENT)
		.enchantmentGlint(false)
		.meta(meta -> {
			var lore = new ArrayList<Component>(List.of(
				text("Your trusty trident. Being near water allows you", NamedTextColor.YELLOW),
				text("to increase its strength.", NamedTextColor.YELLOW),
				Component.empty(),
				text("Legend:", NamedTextColor.GRAY)
			));
			for (RiptideFactor factor : RiptideFactor.values()) {
				lore.add(textOfChildren(factor.display, Component.space(), factor.explanation.colorIfAbsent(NamedTextColor.GRAY)));
			}
			meta.lore(lore.stream().map(component -> component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)).toList());

			if (CAN_THROW)
				meta.addEnchant(Enchantment.LOYALTY, 1, true);
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
		// riptide strength
		private @Range(from = 0, to = 4) float progress = 0;
		private float lastProgress = 0f;
		private int interpolationStart;
		private int interpolationDuration = 0;
		// riptide path preview
		private List<PacketDisplay> pathDisplays = new ArrayList<>();
		// stats
		DoubleSummaryStatistics progressTracker = new DoubleSummaryStatistics();

		RiptideInfo(Player player) {
			bossBar = BossBar.bossBar(buildBarTitle(Set.of(), 0), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
			bossBar.addViewer(player);
		}

		Component buildBarTitle(Set<RiptideFactor> factors, float increment) {
			var builder = text();
			var incrementComponent = Component.text("+".repeat((int) Math.ceil(increment * 10 * 20)), NamedTextColor.GREEN);
			if (increment > 0) {
				builder.append(incrementComponent, space());
			}
			for (RiptideFactor factor : factors) {
				builder.append(factor.display);
			}
			if (!factors.isEmpty())
				builder.appendSpace();

			if (progress < 1) {
				builder.append(text("Riptide", NamedTextColor.GRAY));
			} else if (progress <= 3.5f || !CAN_OVERLOAD) {
				builder.append(text("Riptide " + Math.min(MAX_RIPTIDE_LEVEL, (int) progress)));
			} else { // vibrate
				Component text = text("Riptide 3", NamedTextColor.YELLOW);
				Component space = text("  ");
				builder.append(TeamArena.getGameTick() % 20 < 10 ?
					textOfChildren(text, space) :
					textOfChildren(space, text));
			}

			if (increment > 0) {
				int nextMilestone = (int) Math.ceil(progress + 0.001f);
				if (CAN_OVERLOAD || nextMilestone <= MAX_RIPTIDE_LEVEL) {
					float secondsUntilMilestone = (nextMilestone - progress) / increment / 20f;
					builder.append(text(" - " + TextUtils.formatNumber(secondsUntilMilestone) + "s "));
					if (nextMilestone != MAX_RIPTIDE_PROGRESS) {
						builder.append(text("to Riptide " + nextMilestone));
					} else {
						builder.append(text("till OVERLOAD", TeamArena.getGameTick() % 20 < 10 ? NamedTextColor.GOLD : NamedTextColor.YELLOW));
					}
				}
				builder.append(space(), incrementComponent);
			}
			return builder.build();
		}

		void updateBossBar(Set<RiptideFactor> factors, float increment) {
			if (burdened) {
				bossBar.color(BossBar.Color.RED);
				bossBar.name(text("The weight of the flag bears down on you", NamedTextColor.RED));
			} else {
				bossBar.color(progress < 1 ? BossBar.Color.YELLOW : BossBar.Color.WHITE);
				bossBar.name(buildBarTitle(factors, increment));
			}
			if (interpolationDuration == 0 || (TeamArena.getGameTick() - interpolationStart > interpolationDuration)) {
				bossBar.progress(progress / MAX_RIPTIDE_PROGRESS);
			} else {
				float t = (float) (TeamArena.getGameTick() - interpolationStart) / interpolationDuration;
				bossBar.progress(MathUtils.lerp(lastProgress, progress, t) / MAX_RIPTIDE_PROGRESS);
			}
		}

		private static final BlockData PATH_PREVIEW_STATE = Material.BLUE_STAINED_GLASS.createBlockData();
		void updatePathPreview(Player player, List<Vector> points) {
			World world = player.getWorld();
			int requestedSize = points.size() - 1;
			int size = pathDisplays.size();
			for (int i = 0; i < size; i++) {
				PacketDisplay entity = pathDisplays.get(i);
				if (i < requestedSize) {
					// update existing
					Vector pointStart = points.get(i);
					Vector direction = points.get(i + 1).clone().subtract(pointStart);
					entity.move(pointStart.toLocation(world));
					entity.setInterpolationDelay(0);
					entity.setScale(DisplayUtils.calcSegmentScale((float) direction.length()));
					entity.setLeftRotation(DisplayUtils.calcSegmentRotation(direction));
					entity.refreshViewerMetadata();
					entity.respawn();
				} else { // i >= requestedSize
					entity.despawn();
				}
			}
			for (int i = size; i < requestedSize; i++) {
				Vector pointStart = points.get(i);
				Vector direction = points.get(i + 1).clone().subtract(pointStart);
				PacketDisplay entity = DisplayUtils.createVirtualLineSegment(pointStart.toLocation(world),
					DisplayUtils.calcSegmentRotation(direction), (float) direction.length(), Color.AQUA, PATH_PREVIEW_STATE);
				entity.setTeleportDuration(1);
				entity.setInterpolationDuration(1);
				entity.updateMetadataPacket();
				entity.respawn();
				entity.setViewers(player);
				pathDisplays.add(entity);
			}
		}

		void setBurdened(boolean burdened) {
			this.burdened = burdened;
		}

		void setProgress(float progress) {
			this.progress = MathUtils.clamp(0, MAX_RIPTIDE_PROGRESS, progress);
		}

		void setProgress(float progress, int interpolationDuration) {
			this.lastProgress = this.progress;
			this.progress = MathUtils.clamp(0, MAX_RIPTIDE_PROGRESS, progress);
			this.interpolationStart = TeamArena.getGameTick();
			this.interpolationDuration = interpolationDuration;
		}

		float getProgress() {
			return progress;
		}

		void setFakeWater(Player player, @NotNull Location location) {
			BlockCoords targetCoords = new BlockCoords(location);
			if (!targetCoords.equals(fakeWaterBlockCoords)) {
				if (fakeWaterBlockKey != 0) {
					Main.getGame().getFakeBlockManager().removeFakeBlock(fakeWaterBlockCoords, fakeWaterBlockKey);
				}
				fakeWaterBlockCoords = targetCoords;
				fakeWaterBlockKey = Main.getGame().getFakeBlockManager().setFakeBlock(fakeWaterBlockCoords, SHALLOW_WATER_DATA, p -> p == player);
			}
		}

		void removeFakeWater() {
			if (fakeWaterBlockKey != 0) {
				Main.getGame().getFakeBlockManager().removeFakeBlock(fakeWaterBlockCoords, fakeWaterBlockKey);
				fakeWaterBlockKey = 0;
				fakeWaterBlockCoords = null;
			}
		}

		void cleanUp(Player player) {
			bossBar.removeViewer(player);
			removeFakeWater();
			pathDisplays.forEach(PacketEntity::remove);
			pathDisplays = null;
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
		SUNNY(text("☀", TextColor.color(0xFEE137)), text("It is not raining. Your trident struggles to charge up."),
			0.1f, 0.01f, 0),
		STORM(text("☔", TextColor.color(0x937DF2)), text("It is raining. Your trident charges from the raindrops."),
			0.2f, 0.1f, 0.005f),
		THUNDERING(text("⛈", TextColor.color(0xFFFECE)), text("It is thundering. Your trident is empowered."),
			5, 2f, 0.075f), // let's get crazy
		// environment factors
		WATER(text("\uD83D\uDCA6", TextColor.color(0x6FCAFF)), text("You are in water. Your trident charges from the water."),
			1, 0.8f, 0.2f),
		DEEP_WATER(text("\uD83C\uDF0A", TextColor.color(0x6FCAFF)), text("You are in deep water. Your trident charges from the water."),
			1.8f, 1.5f, 1),
		// troll factors
//		BLUE_TEAM(Component.text("⚑", NamedTextColor.BLUE), Component.text("You are near a Blue Team member. Your trident charges from the blue dye."), 0),
		// impact-based factors
		IMPACT_BOOST(text("🔱", TextColor.color(0x274036/*0x579B8C*/)), text("You impaled an enemy. Your trident charges from their blood."),
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

	private Set<RiptideFactor> calcFactors(Player player, RiptideInfo riptideInfo) {
		EnumSet<RiptideFactor> factors = EnumSet.noneOf(RiptideFactor.class);
		World world = player.getWorld();
		Location eyeLocation = player.getEyeLocation();
		boolean canSeeSky = world.getHighestBlockYAt(eyeLocation, HeightMap.MOTION_BLOCKING_NO_LEAVES) <= eyeLocation.getY();
		boolean isRealWater = riptideInfo.fakeWaterBlockKey == 0L;
		if (isRealWater && player.isUnderWater())
				factors.add(RiptideFactor.DEEP_WATER);
		else if (isRealWater && player.isInWater())
			factors.add(RiptideFactor.WATER);
		else if (canSeeSky) {
			if (world.isThundering())
				factors.add(RiptideFactor.THUNDERING);
			else if (world.hasStorm())
				factors.add(RiptideFactor.STORM);
			else
				factors.add(RiptideFactor.SUNNY);
		}
		if (CAN_THROW && TeamArena.getGameTick() - riptideInfo.lastImpact <= RIPTIDE_IMPACT_BOOST_DURATION) {
			factors.add(RiptideFactor.IMPACT_BOOST);
		}
		return factors;
	}

	public float calcProgressIncrement(Set<RiptideFactor> factors, float progress) {
		float sum = 0;
		boolean overload = progress <= MAX_RIPTIDE_LEVEL;
		float t = overload ? progress / MAX_RIPTIDE_LEVEL : progress - MAX_RIPTIDE_LEVEL;
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
		player.sendMessage(textOfChildren(
			text("Your average "), text("Riptide", NamedTextColor.AQUA), text(" level was "),
			text(TextUtils.formatNumber(info.progressTracker.getAverage()), NamedTextColor.YELLOW),
			text(".")
		));
		info.cleanUp(player);
		// only remove our trident in case of admin abuse
		for (var iter = player.getInventory().iterator(); iter.hasNext();) {
			ItemStack stack = iter.next();
			if (stack != null && stack.getType() == Material.TRIDENT && stack.getPersistentDataContainer().has(TRIDENT_KEY)) {
				iter.set(null);
			}
		}
		// remove all player tridents
		UUID uuid = player.getUniqueId();
		for (Entity entity : player.getWorld().getEntities()) {
			if (entity instanceof Trident trident && uuid.equals(trident.getOwnerUniqueId())) {
				entity.remove();
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
		for (var iter = player.getInventory().iterator(); iter.hasNext();) {
			ItemStack stack = iter.next();
			if (stack != null && stack.getType() == Material.TRIDENT && stack.getPersistentDataContainer().has(TRIDENT_KEY)) {
				ItemMeta meta = stack.getItemMeta();
				boolean changed = updateMeta;
				if (!CAN_THROW || (level != 0 && canRiptide(player))) { // force add riptide if trident can't be thrown
					changed |= meta.addEnchant(Enchantment.RIPTIDE, Math.max(1, level), true);
				} else {
					changed |= meta.removeEnchant(Enchantment.RIPTIDE);
				}
				if (updateMeta) {
					meta.itemName(switch (level) {
						case 0 -> text("Trident");
						case 1 -> text("Trident Plus", NamedTextColor.YELLOW);
						case 2 -> text("Trident Pro", NamedTextColor.GOLD);
						default -> text("Trident Pro Max", NamedTextColor.RED);
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
			((player.isSneaking() && player.isOnGround()) || player.isInWater() || player.isInRain());
	}

	private static final BlockData SHALLOW_WATER_DATA = Material.WATER.createBlockData(blockData -> ((Levelled) blockData).setLevel(7));
	@Override
	public void onPlayerTick(Player player) {
		RiptideInfo info = riptideInfoMap.get(player);
		boolean burdened = Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player);
		info.setBurdened(burdened);

		int oldProgress = (int) info.progress;
		Set<RiptideFactor> factors = calcFactors(player, info);
		float increment = calcProgressIncrement(factors, info.progress);
		info.setProgress(info.progress + increment);

		if (CAN_OVERLOAD && !burdened && info.progress == MAX_RIPTIDE_PROGRESS) {
			// overload!
			player.setVelocity(new Vector(MathUtils.random.nextGaussian(), 2.5, MathUtils.random.nextGaussian()));
			player.startRiptideAttack(20, 0, null);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false, false));
			info.setProgress(0, 5);
		}
		info.progressTracker.accept(info.progress);

		boolean changed = (int) info.progress != oldProgress;
		if (changed && (int) info.progress > oldProgress) {
			player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1f);
		}
		info.updateBossBar(factors, increment);
		Component actionBar;

		boolean canRiptide = canRiptide(player);
		var attribute = Objects.requireNonNull(player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY));
		if (info.progress >= 1 && !burdened) {
			// is sneaking
			if (canRiptide && !(player.isInWater() || player.isInRain())) {
				if (attribute.getModifier(SNEAK_SPEED.getKey()) == null)
					attribute.addModifier(SNEAK_SPEED);
				info.setFakeWater(player, player.getEyeLocation());
				player.sendPotionEffectChange(player, new PotionEffect(PotionEffectType.CONDUIT_POWER, 1000000, 0, true));
			} else {
				player.sendPotionEffectChangeRemove(player, PotionEffectType.CONDUIT_POWER);
				attribute.removeModifier(SNEAK_SPEED);
				info.removeFakeWater();
			}

			List<Vector> vectors = List.of();
			if (canRiptide) {
				actionBar = text("Ready to Riptide", NamedTextColor.AQUA);
				if (player.getActiveItem().getType() == Material.TRIDENT) {
					vectors = estimateRiptideLocation(player, player.getActiveItem());
					if (!vectors.isEmpty())
						vectors = vectors.subList(1, vectors.size()); // avoid blocking player view
				}
			} else if (!player.isOnGround()) {
				actionBar = text("Stay on ground to Riptide", NamedTextColor.GRAY);
			} else {
				actionBar = textOfChildren(
					text("["),
					Component.keybind("key.sneak", NamedTextColor.GREEN),
					text("] to Riptide")
				).color(NamedTextColor.GRAY);
			}
			info.updatePathPreview(player, vectors);
		} else {
			actionBar = text("Can't riptide", NamedTextColor.DARK_GRAY);
			info.removeFakeWater();
			info.updatePathPreview(player, List.of());
		}
		player.sendActionBar(actionBar);

		updateItems(player, Math.min(MAX_RIPTIDE_LEVEL, (int) info.progress), changed);

		displayParticles(player, info, (int) info.progress);
	}

	private static final int PARTICLE_BOB_INTERVAL = 5 * 20;
	private static final int PARTICLE_SPIN_INTERVAL = 10 * 20;
	private void displayParticles(Player player, RiptideInfo info, int progress) {
		if (progress <= 0 || TeamArena.getGameTick() % 2 != 0)
			return;

		boolean likelyToRiptide = player.getInventory().getItemInMainHand().getType() == Material.TRIDENT;
		boolean reallyGoingToRiptideISwear = player.getActiveItem().getType() == Material.TRIDENT;
		boolean showParticles = reallyGoingToRiptideISwear || !player.getCurrentInput().isSneak();
		if (!showParticles)
			return;

		Location loc = player.getLocation();
		List<PacketContainer> packets = new ArrayList<>(progress);
		List<PacketContainer> riptidePackets = new ArrayList<>(progress);

		// spawn spinning particles around the player
		Particle particle = switch (progress) {
			case 1 -> Particle.RAIN;
			case 2 -> Particle.DRIPPING_WATER;
			default -> Particle.NAUTILUS;
		};
		int now = TeamArena.getGameTick();
		double eyeHeight = player.getEyeHeight();
		double yaw = Math.toRadians(player.getBodyYaw()) + Math.TAU * (now % PARTICLE_SPIN_INTERVAL) / PARTICLE_SPIN_INTERVAL;
		double particleInterval = Math.TAU / progress;
		for (int i = 0; i < progress; i++) {
			double offset = particleInterval * i;
			double angle = yaw + offset;
			double sin = Math.sin(angle), cos = Math.cos(angle);
			double x = loc.x() + 0.5 * sin, z = loc.z() + 0.5 * cos;
			double y = loc.y() + eyeHeight * (0.25 + 0.125 * Math.sin(Math.TAU * (now % PARTICLE_BOB_INTERVAL) / PARTICLE_BOB_INTERVAL + offset));

			packets.add(ParticleUtils.batchParticles(particle, null, x, y, z, 0, 0, 0, 0, 0, false));

			if (likelyToRiptide && progress >= 3) { // crazy aura
				double nx = loc.x() + 2 * sin, nz = loc.z() + 2 * cos;
				riptidePackets.add(ParticleUtils.batchParticles(
					Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, null, nx, loc.y(), nz, 0,
					(float) (-2 * sin) /* x - nx */, -2, (float) (-2 * cos) /* z - nz */,
					0.05f, false
				));
			}
		}

		Location viewerLoc = loc.clone();
		List<? extends Player> viewers = Bukkit.getOnlinePlayers().stream()
			.filter(viewer -> viewer != player && viewer.getLocation(viewerLoc).distanceSquared(loc) <= 32 * 32)
			.toList();
		PlayerUtils.sendPacket(viewers, packets);
		PlayerUtils.sendPacket(viewers, riptidePackets); // doesn't send anything if empty
		PlayerUtils.sendPacket(player, packets);
		PlayerUtils.sendPacket(player, riptidePackets);
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
			var hitResult = world.rayTraceBlocks(location, velocity, velocity.length(), FluidCollisionMode.NEVER, true);
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
		RiptideInfo info = riptideInfoMap.get(event.getPlayer());
		int riptideLevel = event.getItem().getEnchantmentLevel(Enchantment.RIPTIDE);
		info.setProgress(info.getProgress() - Math.min(riptideLevel, MAX_RIPTIDE_LEVEL), 10);
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
		if (item == null || item.getType() != Material.TRIDENT ||
			!item.getPersistentDataContainer().has(TRIDENT_KEY))
			return;
		Player player = event.getPlayer();
		RiptideInfo info = riptideInfoMap.get(player);
		if (info.progress < 1) // ensure player can riptide with custom trident
			return;
		if (canRiptide(player)) {
			if (player.isInWater() || player.isInRain())
				return; // no need to set fake water
			((CraftPlayer) player).getHandle().wasTouchingWater = true;
			riptideInfoMap.get(player).wasFakeWater = true;
		}
	}

	@Override
	public void onStopUsingItem(PlayerStopUsingItemEvent event) {
		ItemStack item = event.getItem();
		if (item.getType() != Material.TRIDENT || item.getEnchantmentLevel(Enchantment.RIPTIDE) == 0 || event.getTicksHeldFor() < 10)
			return;
		Player player = event.getPlayer();
		if (canRiptide(player)) {
			if (player.isInWater() || player.isInRain())
				return; // no need to set fake water
			((CraftPlayer) player).getHandle().wasTouchingWater = true;
			riptideInfoMap.get(player).wasFakeWater = true;
		}
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

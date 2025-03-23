package me.toomuchzelda.teamarenapaper.teamarena.abilities.centurion;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketDisplay;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import net.minecraft.world.phys.AABB;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.DoubleConsumer;
import java.util.stream.IntStream;

/**
 * @author jacky
 */
public class ShieldInstance {
	public static final NamespacedKey SHIELD_ENTITY = new NamespacedKey(Main.getPlugin(), "shield");

	public static final boolean USE_TEXT_DISPLAY = false;

	public final UUID uuid = UUID.randomUUID();

	public final World world;
	public final Player player;
	@Nullable
	private TeamArenaTeam team;

	private boolean valid = true;

	static final long SHIELD_REGEN_COOLDOWN = 5 * 20;
	static final double SHIELD_REGEN_PER_TICK = 0.5f;

	double health;
	private double lastHealth;
	private final double maxHealth;
	long lastDamageTick = -SHIELD_REGEN_COOLDOWN;
	private final int duration;

	// if null, the shield follows the player
	@Nullable
	private final Location anchor;

	private final List<ShieldPart> parts;
	private final List<PacketDisplay> playerDisplays;
	private final List<PacketDisplay> otherDisplays;

	final LinkedHashSet<ArmorStand> boxes;

	private final long spawnedAt;

	private double mitigation;

	private Runnable breakListener;
	private Runnable expireListener;
	private DoubleConsumer mitigationListener;

	private static final int SHIELD_SIZE = 3;
	private static final float SHIELD_WIDTH = 5;
	private static final float SHIELD_HEIGHT = SHIELD_SIZE;
	private static final int SHIELD_BOXES_PER_METER = 3;
	private static final float SHIELD_BOX_WIDTH = 1f / SHIELD_BOXES_PER_METER;
	private static final int SHIELD_BOXES_COUNT = (int) SHIELD_WIDTH * SHIELD_BOXES_PER_METER;

	private static final int SHIELD_ALPHA_MAX = 127;
	private static final int SHIELD_ALPHA_MIN = 40;
	public static final int SHIELD_COLOR_HEX = 0x3AB3DA;
	private static final int SHIELD_COLOR_INTERP_INTERVAL = 100;

	public ShieldInstance(Player player, ShieldConfig shieldConfig) {
		this.world = player.getWorld();
		this.player = player;
		this.spawnedAt = world.getGameTime();
		team = Main.getPlayerInfo(player).team;

		if (shieldConfig.anchor() != null) {
			anchor = shieldConfig.anchor().clone();
			anchor.setPitch(0);
		} else {
			anchor = null;
		}
		this.health = shieldConfig.health();
		this.maxHealth = shieldConfig.maxHealth();
		this.duration = shieldConfig.duration();

		World world = player.getWorld();
		List<Location> boxLocations = getCurvedBoxLocations();
		parts = new ArrayList<>(boxLocations.size());
		int numElements = boxLocations.size() * 2;
		playerDisplays = new ArrayList<>(numElements);
		otherDisplays = new ArrayList<>(numElements);
		boxes = LinkedHashSet.newLinkedHashSet(numElements);

		DyeColor baseColor = DyeColor.LIGHT_BLUE;
		DyeColor otherColor = team != null ? team.getDyeColour() : DyeColor.BLUE;

		for (Location location : boxLocations) {
			ShieldPart shieldPart;
			if (USE_TEXT_DISPLAY)
				shieldPart = new TextShieldPart(location, getShieldBaseColor(), getShieldOtherColor(), playerDisplays, otherDisplays);
			else
				shieldPart = new BlockShieldPart(location, baseColor, otherColor, playerDisplays, otherDisplays);
			parts.add(shieldPart);
			boxes.add(world.spawn(location, ArmorStand.class, ShieldInstance::spawnBox));
			boxes.add(world.spawn(location.clone().add(0, 1.1f, 0), ArmorStand.class, ShieldInstance::spawnBox));
		}

		for (var playerDisplay : playerDisplays) {
			playerDisplay.setViewers(player);
			playerDisplay.respawn();
		}
		for (PacketDisplay otherDisplay : otherDisplays) {
			otherDisplay.respawn();
		}

		if (team != null) {
			team.addMembers(boxes.toArray(new Entity[0]));
		}

//		task = Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), this::tick, 0, 1);

		updateViewers();

		ShieldListener.registerShield(this);
	}

	public void setBreakListener(Runnable breakListener) {
		this.breakListener = breakListener;
	}

	public void setExpireListener(Runnable expireListener) {
		this.expireListener = expireListener;
	}

	public void setMitigationListener(DoubleConsumer mitigationListener) {
		this.mitigationListener = mitigationListener;
	}

	public boolean isValid() {
		return valid;
	}

	interface ShieldPart {
		void updateLocations(Location base);
	}

	public static class BlockShieldPart implements ShieldPart {
		private final PacketDisplay playerDisplay;
		private final PacketDisplay otherDisplay;
		BlockShieldPart(Location base, DyeColor baseColor, DyeColor otherColor,
						List<PacketDisplay> playerDisplays, List<PacketDisplay> otherDisplays) {
			playerDisplay = spawnDisplay(base, baseColor);
			otherDisplay = spawnOtherDisplay(base, otherColor);

			playerDisplays.add(playerDisplay);
			otherDisplays.add(otherDisplay);
		}

		@Override
		public void updateLocations(Location base) {
			playerDisplay.move(base);
			otherDisplay.move(base);

		}
		private static final float BLOCK_WIDTH = SHIELD_WIDTH / SHIELD_BOXES_COUNT;
		private PacketDisplay spawnDisplay(Location location, DyeColor color) {
			PacketDisplay blockDisplay = new PacketDisplay(PacketEntity.NEW_ID, EntityType.BLOCK_DISPLAY, location, null, null);
			blockDisplay.setBlockData(Objects.requireNonNull(Material.getMaterial(color.name() + "_STAINED_GLASS")).createBlockData());
			blockDisplay.setTeleportDuration(1);
			blockDisplay.setTranslation(new Vector3f(-BLOCK_WIDTH / 2, 0, 0));
			blockDisplay.setScale(new Vector3f(BLOCK_WIDTH, SHIELD_HEIGHT, 0.1f));
			blockDisplay.setInterpolationDuration(SHIELD_COLOR_INTERP_INTERVAL);
			blockDisplay.setBrightnessOverride(new Display.Brightness(15, 15));
			blockDisplay.updateMetadataPacket();
			return blockDisplay;
		}

		private PacketDisplay spawnOtherDisplay(Location location, DyeColor color) {
			PacketDisplay blockDisplay = spawnDisplay(location, color);
			blockDisplay.setTeleportDuration(3);
			blockDisplay.updateMetadataPacket();
			return blockDisplay;
		}
	}

	public static class TextShieldPart implements ShieldPart {
		final PacketDisplay playerFrontDisplay;
		final PacketDisplay playerBackDisplay;
		final PacketDisplay otherFrontDisplay;
		final PacketDisplay otherBackDisplay;

		TextShieldPart(Location base, Color baseColor, Color otherColor,
				   List<PacketDisplay> playerDisplays, List<PacketDisplay> otherDisplays) {
			playerFrontDisplay = spawnDisplay(base, baseColor);
			otherFrontDisplay = spawnOtherDisplay(base, otherColor);

			var back = base.clone();
			back.setYaw(back.getYaw() + 180);
			playerBackDisplay = spawnDisplay(back, baseColor);
			otherBackDisplay = spawnOtherDisplay(back, otherColor);

			playerDisplays.add(playerFrontDisplay);
			playerDisplays.add(playerBackDisplay);
			otherDisplays.add(otherFrontDisplay);
			otherDisplays.add(otherBackDisplay);
		}

		@Override
		public void updateLocations(Location base) {
			playerFrontDisplay.move(base);
			otherFrontDisplay.move(base);
			Location back = base.clone();
			back.setYaw(back.getYaw() + 180);
			playerBackDisplay.move(back);
			otherBackDisplay.move(back);
		}

		private static final float TEXT_WIDTH = SHIELD_WIDTH / SHIELD_BOXES_COUNT;
		private static final float TEXT_X_SCALE = 8f * TEXT_WIDTH; //SHIELD_WIDTH;
		private static final float TEXT_Y_SCALE = 4 * SHIELD_HEIGHT;
		private static final float TEXT_X_OFFSET = -3f / 256 * TEXT_X_SCALE;
		private static final float TEXT_Y_OFFSET = -1f / 16 * (16 + 3) / 16 * TEXT_Y_SCALE + SHIELD_HEIGHT / 4;
		private PacketDisplay spawnDisplay(Location location, Color color) {
			PacketDisplay textDisplay = new PacketDisplay(PacketEntity.NEW_ID, EntityType.TEXT_DISPLAY, location, null, null);
			textDisplay.text(Component.text(" "));
			textDisplay.setBackgroundColor(color);
			textDisplay.setTextOpacity((byte) 0);
			textDisplay.setTeleportDuration(1);
			textDisplay.setTranslation(new Vector3f(TEXT_X_OFFSET, TEXT_Y_OFFSET, 0.01f));
			textDisplay.setScale(new Vector3f(TEXT_X_SCALE, TEXT_Y_SCALE, 1));
			textDisplay.setSeeThrough(true);
			textDisplay.setInterpolationDuration(SHIELD_COLOR_INTERP_INTERVAL);
			textDisplay.updateMetadataPacket();
			return textDisplay;
		}

		private PacketDisplay spawnOtherDisplay(Location location, Color color) {
			PacketDisplay textDisplay = spawnDisplay(location, color);
			textDisplay.setTeleportDuration(3);
			textDisplay.updateMetadataPacket();
			return textDisplay;
		}
	}


	private static void spawnBox(ArmorStand interaction) {
		interaction.setVisibleByDefault(false);
		interaction.setAI(false);
		interaction.setSilent(true);
		interaction.setPersistent(false);
		interaction.setCanTick(false);
		interaction.setVisible(false);
		interaction.setDisabledSlots(EquipmentSlot.values());
		interaction.getPersistentDataContainer().set(SHIELD_ENTITY, PersistentDataType.BOOLEAN, true);
	}

	private int getShieldAlpha() {
		return (int) Math.round(MathUtils.lerp(SHIELD_ALPHA_MIN, SHIELD_ALPHA_MAX, Math.max(Math.min(health / maxHealth, 1), 0)));
	}

	private Color getShieldBaseColor() {
		return Color.fromARGB(SHIELD_COLOR_HEX | (getShieldAlpha() << 24));
	}

	Color getShieldOtherColor() {
		if (team != null) {
			int value = team.getRGBTextColor().value();
			return Color.fromARGB(value | (getShieldAlpha() << 24));
		}
		return getShieldBaseColor();
	}

	private static final float FACTOR = 0.7f;
	/** See {@link java.awt.Color#brighter()} */
	static Color brighter(Color base) {
		int a = base.getAlpha(), r = base.getRed(), g = base.getGreen(), b = base.getBlue();
		int i = (int)(1.0/(1.0-FACTOR));
		if ( r == 0 && g == 0 && b == 0) {
			return Color.fromARGB(a, i, i, i);
		}
		if ( r > 0 && r < i ) r = i;
		if ( g > 0 && g < i ) g = i;
		if ( b > 0 && b < i ) b = i;

		return Color.fromARGB(
			a,
			Math.min((int)(r/FACTOR), 255),
			Math.min((int)(g/FACTOR), 255),
			Math.min((int)(b/FACTOR), 255)
		);
	}

	public Location getShieldLocation() {
		if (anchor != null)
			return anchor.clone();
		Location eyeLocation = player.getLocation();
		eyeLocation.setY(eyeLocation.getY() + 1.5);
		eyeLocation.setPitch(0);
		Vector direction = eyeLocation.getDirection();
		direction.setY(0);
		eyeLocation.add(direction.multiply(1.5));
		return eyeLocation;
	}

	private static final double AABB_SIZE = SHIELD_BOX_WIDTH / 2 + 0.05;
	public List<AABB> buildVoxelShape() {
		List<Location> locations = getCurvedBoxLocations();
		List<AABB> list = new ArrayList<>(locations.size());
		for (Location location : locations) {
			list.add(new AABB(
				location.getX() - AABB_SIZE, location.getY() - 0.05, location.getZ() - AABB_SIZE,
				location.getX() + AABB_SIZE, location.getY() + SHIELD_HEIGHT + 0.05, location.getZ() + AABB_SIZE
			));
		}
		return list;
	}

	private List<Location> getCurvedBoxLocations() {
		if (true) {
			return getBoxLocations();
		}
		Location location = getShieldLocation();
		location.add(location.getDirection().multiply(-1.5))
			.add(0, -SHIELD_SIZE / 2d, 0);
		Location[] locations = new Location[SHIELD_BOXES_COUNT];
		int center = SHIELD_BOXES_COUNT / 2;
		double angle = Math.toDegrees(Math.acos((2 * 1.5 * 1.5 - (SHIELD_WIDTH / SHIELD_BOXES_COUNT)) / (2 * 1.5 * 1.5))) / 2;
//            double angle = 0;
		for (int i = 0; i < SHIELD_BOXES_COUNT; i++) {
			Location offset = location.clone();
			offset.setYaw((float) (offset.getYaw() + angle * (i - center)));
			offset.add(offset.getDirection().multiply(1.5));
			locations[i] = offset;
		}
		return List.of(locations);
	}

	private List<Location> getBoxLocations() {
		Location location = getShieldLocation();
		Vector front = location.getDirection();
		front.setY(0);
		Vector right = new Vector(0, 1, 0).crossProduct(front).normalize().multiply(-1 * SHIELD_BOX_WIDTH);
		double initialOffset = -(SHIELD_BOXES_COUNT / 2d + 0.5);
		location.add(right.getX() * initialOffset, -SHIELD_SIZE / 2d, right.getZ() * initialOffset);
		return IntStream.range(0, SHIELD_BOXES_COUNT)
			.mapToObj(i -> location.clone().add(right.getX() * (i + 1), 0, right.getZ() * (i + 1))).toList();
	}

	private static boolean playerCantSee(Player player, PacketDisplay display) {
		if (!USE_TEXT_DISPLAY) return false; // no need to hide for BlockShieldPart
		Location location = display.getLocation().add(0, SHIELD_HEIGHT / 2, 0);
		Location eyeLocation = player.getEyeLocation();
		double distance = location.distance(eyeLocation);
		Vector direction = location.subtract(eyeLocation).toVector().normalize();
		return player.getWorld().rayTraceBlocks(eyeLocation, direction, distance, FluidCollisionMode.NEVER, true) != null;
	}

	public void updateViewers() {
		Set<Player> trackedBy = new HashSet<>(player.getTrackedBy());
		for (PacketDisplay otherDisplay : otherDisplays) {
			var canSee = new ArrayList<>(trackedBy);
			canSee.removeIf(p -> playerCantSee(p, otherDisplay));
			otherDisplay.setViewers(canSee);
		}

		var plugin = Main.getPlugin();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if ((trackedBy.contains(player) && !isFriendly(player))) {
				for (var box : boxes) {
					player.showEntity(plugin, box);
				}
			} else {
				for (var box : boxes) {
					player.hideEntity(plugin, box);
				}
			}
		}
	}

	public void updateLocations() {
		var partIter = parts.iterator();
		var boxIter = boxes.iterator();
		for (Location location : getCurvedBoxLocations()) {
			partIter.next().updateLocations(location);

			boxIter.next().teleport(location);
			boxIter.next().teleport(location.clone().add(0, 1.1f, 0));
		}
	}

	public void playBreakEffect() {
		Location loc = getShieldLocation();
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.5f, 1f);
		// play block crack effect on the plane the shield occupies
		BlockData data = team != null ?
			Objects.requireNonNull(Material.getMaterial(team.getDyeColour().toString() + "_STAINED_GLASS")).createBlockData() :
			Material.LIGHT_BLUE_STAINED_GLASS.createBlockData();
		double x = loc.getX(), y = loc.getY(), z = loc.getZ();
		Vector front = loc.getDirection();
		front.setY(0);
		Vector right = new Vector(0, 1, 0).crossProduct(front).normalize().multiply(SHIELD_WIDTH / 10);
		double xStep = right.getX(), zStep = right.getZ();
		for (int j = -5; j <= 5; j++) {
			loc.setY(y + j * (SHIELD_HEIGHT / 5));
			for (int i = -10; i <= 10; i++) {
				loc.setX(x + i * xStep);
				loc.setZ(z + i * zStep);
				world.spawnParticle(Particle.BLOCK, loc, 0, data);
			}
		}
	}

	private static final int TITLE_PARTS = 20;
	public Title buildTitle() {
		int healthParts = Math.max(0, Math.min(TITLE_PARTS,
			(int) Math.floor((float) health / maxHealth * TITLE_PARTS)));
		int underlinedParts = duration <= 0 ? 0 : Math.max(0, Math.min(TITLE_PARTS,
			TITLE_PARTS - (int) Math.floor((float) (world.getGameTime() - spawnedAt) / duration * TITLE_PARTS)));
		var builder = Component.text();
		if (underlinedParts > healthParts) {
			builder.append(Component.text("|".repeat(healthParts), NamedTextColor.WHITE, TextDecoration.UNDERLINED));
			builder.append(Component.text("|".repeat(underlinedParts - healthParts), NamedTextColor.DARK_GRAY, TextDecoration.UNDERLINED));
		} else {
			builder.append(Component.text("|".repeat(underlinedParts), NamedTextColor.WHITE, TextDecoration.UNDERLINED));
			builder.append(Component.text("|".repeat(healthParts - underlinedParts), NamedTextColor.WHITE));
		}
		builder.append(Component.text("|".repeat(TITLE_PARTS - Math.max(healthParts, underlinedParts)), NamedTextColor.DARK_GRAY));
		return Title.title(Component.empty(), builder.build(), Title.Times.times(Ticks.duration(0), Ticks.duration(5), Ticks.duration(0)));
	}

	public void tick() {
		if (!player.isValid() || !Objects.equals(world, player.getWorld())) {
			cleanUp();
			return;
		}
		if (health == 0) {
			if (breakListener != null)
				breakListener.run();
			playBreakEffect();
			cleanUp();
			return;
		}
		if (duration != -1 && world.getGameTime() - spawnedAt > duration) {
			if (expireListener != null)
				expireListener.run();
			cleanUp();
			return;
		}

		updateLocations();
		updateViewers();

		long tickSinceSpawn = (player.getWorld().getGameTime() - spawnedAt) % (SHIELD_COLOR_INTERP_INTERVAL * 2);
		if (USE_TEXT_DISPLAY) {
			if (health != lastHealth || tickSinceSpawn == 0 || tickSinceSpawn == SHIELD_COLOR_INTERP_INTERVAL) {
				Color color = (tickSinceSpawn < SHIELD_COLOR_INTERP_INTERVAL) ? getShieldBaseColor() : brighter(getShieldBaseColor());
				Color otherColor = (tickSinceSpawn < SHIELD_COLOR_INTERP_INTERVAL) ? getShieldOtherColor() : brighter(getShieldOtherColor());
				for (PacketDisplay playerDisplay : playerDisplays) {
					playerDisplay.setBackgroundColor(color);
					playerDisplay.setInterpolationDelay(0);
					playerDisplay.updateMetadataPacket();
					playerDisplay.refreshViewerMetadata();
				}
				for (PacketDisplay otherDisplay : otherDisplays) {
					otherDisplay.setBackgroundColor(otherColor);
					otherDisplay.setInterpolationDelay(0);
					otherDisplay.updateMetadataPacket();
					otherDisplay.refreshViewerMetadata();
				}
			}
		}
		lastHealth = health;

		// regen
		if (TeamArena.getGameTick() - lastDamageTick >= SHIELD_REGEN_COOLDOWN) {
			health = Math.min(maxHealth, health + SHIELD_REGEN_PER_TICK);
		}

		player.showTitle(buildTitle());
	}

	public void cleanUp() {
		if (!valid)
			return;
		ShieldListener.unregisterShield(this);

//		task.cancel();

		for (PacketDisplay playerDisplay : playerDisplays) {
			playerDisplay.remove();
		}
		for (PacketDisplay otherDisplay : otherDisplays) {
			otherDisplay.remove();
		}
		Entity[] arr = boxes.toArray(new Entity[0]);
		if (team != null) {
			team.removeMembers(arr);
		}
		boxes.forEach(Entity::remove);
		boxes.clear();
		valid = false;
	}

	public boolean isFriendly(LivingEntity attacker) {
		if (attacker == player)
			return true;
		if (team == null)
			return false;
		return team.hasMember(attacker) && !(attacker instanceof ArmorStand armorStand && boxes.contains(armorStand));
	}

	@Nullable
	public TeamArenaTeam getTeam() {
		return team;
	}

	public void updateTeam(@Nullable TeamArenaTeam oldTeam, @Nullable TeamArenaTeam newTeam) {
		Entity[] arr = boxes.toArray(new Entity[0]);
		if (oldTeam != null) {
			oldTeam.removeMembers(arr);
		}
		if (newTeam != null) {
			newTeam.addMembers(arr);
		}
		team = newTeam;
	}

	public void damage(double damage) {
		lastDamageTick = TeamArena.getGameTick();
		health = Math.max(0, health - damage);
	}

	public void damage(double damage, @NotNull Vector hitPosition) {
		damage(damage);
		Location location = hitPosition.toLocation(world);
		world.playSound(location, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1, 0.8f);
		for (int i = 0; i < 10; i++)
			world.spawnParticle(Particle.DUST, hitPosition.getX(), hitPosition.getY(), hitPosition.getZ(), 0,
				Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5, 0,
				new Particle.DustOptions(brighter(getShieldOtherColor()), 1f));
	}

	public void addMitigation(double mitigation) {
		this.mitigation += mitigation;
		if (mitigationListener != null)
			mitigationListener.accept(mitigation);
	}

	public double getMitigation() {
		return mitigation;
	}

}

package me.toomuchzelda.teamarenapaper.teamarena.abilities.centurion;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketDisplay;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.IntStream;

/**
 * @author jacky
 */
public class ShieldInstance {
	public record ShieldConfig(double health, double maxHealth, int duration, @Nullable Location anchor) {
		public static final double DEFAULT_MAX_HEALTH = 100;
		public static final double DEFAULT_HEALTH = DEFAULT_MAX_HEALTH;

		private static final int SHIELD_PERMANENT = -1;
		public static ShieldConfig DEFAULT = new ShieldConfig(DEFAULT_HEALTH, DEFAULT_MAX_HEALTH, SHIELD_PERMANENT, null);

		public ShieldConfig {
			if (maxHealth < health) throw new IllegalArgumentException("maxHealth");
		}
	}


	public static final NamespacedKey SHIELD_ENTITY = new NamespacedKey(Main.getPlugin(), "shield");

	public final World world;
	public final Player player;

	private boolean valid = true;

	private static final long SHIELD_REGEN_COOLDOWN = 5 * 20;
	private static final double SHIELD_REGEN_PER_TICK = 0.5f;

	private double health;
	private double lastHealth;
	private final double maxHealth;
	private long lastDamageTick = -SHIELD_REGEN_COOLDOWN;
	private final int duration;

	// if null, the shield follows the player
	@Nullable
	private final Location anchor;

	private final List<ShieldPart> parts;
	private final List<PacketDisplay> playerDisplays;
	private final List<PacketDisplay> otherDisplays;

	private final LinkedHashSet<ArmorStand> boxes;

	private final BukkitTask task;

	private final long spawnedAt;

	private static final int SHIELD_SIZE = 3;
	private static final float SHIELD_WIDTH = 5;
	private static final float SHIELD_HEIGHT = SHIELD_SIZE;
	private static final int SHIELD_BOXES_PER_METER = 3;
	private static final float SHIELD_BOX_WIDTH = 1f / SHIELD_BOXES_PER_METER;
	private static final int SHIELD_BOXES_COUNT = (int) SHIELD_WIDTH * SHIELD_BOXES_PER_METER;

	private static final int SHIELD_ALPHA_MAX = 127;
	private static final int SHIELD_ALPHA_MIN = 40;
	private static final int SHIELD_COLOR_HEX = 0x3AB3DA;
	private static final int SHIELD_COLOR_INTERP_INTERVAL = 100;

	public ShieldInstance(Player player, ShieldConfig shieldConfig) {
		this.world = player.getWorld();
		this.player = player;
		this.spawnedAt = world.getGameTime();

		if (shieldConfig.anchor != null) {
			anchor = shieldConfig.anchor.clone();
			anchor.setPitch(0);
		} else {
			anchor = null;
		}
		this.health = shieldConfig.health;
		this.maxHealth = shieldConfig.maxHealth;
		this.duration = shieldConfig.duration;

		World world = player.getWorld();
		List<Location> boxLocations = getCurvedBoxLocations();
		parts = new ArrayList<>(boxLocations.size());
		int numElements = boxLocations.size() * 2;
		playerDisplays = new ArrayList<>(numElements);
		otherDisplays = new ArrayList<>(numElements);
		boxes = LinkedHashSet.newLinkedHashSet(numElements);
		for (Location location : boxLocations) {
			ShieldPart shieldPart = new ShieldPart(location);
			parts.add(shieldPart);
			playerDisplays.add(shieldPart.playerFrontDisplay);
			playerDisplays.add(shieldPart.playerBackDisplay);
			otherDisplays.add(shieldPart.otherFrontDisplay);
			otherDisplays.add(shieldPart.otherBackDisplay);
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

		TeamArenaTeam team = Main.getPlayerInfo(player).team;
		if (team != null) {
			team.addMembers(boxes.toArray(new Entity[0]));
		}

		task = Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), this::tick, 0, 1);

		updateViewers();
	}

	public boolean isValid() {
		return valid;
	}

	public class ShieldPart {
		final PacketDisplay playerFrontDisplay;
		final PacketDisplay playerBackDisplay;
		final PacketDisplay otherFrontDisplay;
		final PacketDisplay otherBackDisplay;

		ShieldPart(Location base) {
			playerFrontDisplay = spawnDisplay(base);
			otherFrontDisplay = spawnOtherDisplay(base);

			var back = base.clone();
			back.setYaw(back.getYaw() + 180);
			playerBackDisplay = spawnDisplay(back);
			otherBackDisplay = spawnOtherDisplay(back);
		}

		void updateLocations(Location base) {
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
		private PacketDisplay spawnDisplay(Location location) {
			PacketDisplay textDisplay = new PacketDisplay(PacketEntity.NEW_ID, EntityType.TEXT_DISPLAY, location, null, null);
			textDisplay.text(Component.text(" "));
			textDisplay.setBackgroundColor(getShieldBaseColor());
			textDisplay.setTextOpacity((byte) 0);
			textDisplay.setTeleportDuration(1);
			textDisplay.setTranslation(new Vector3f(TEXT_X_OFFSET, TEXT_Y_OFFSET, 0.01f));
			textDisplay.setScale(new Vector3f(TEXT_X_SCALE, TEXT_Y_SCALE, 1));
			textDisplay.setSeeThrough(true);
			textDisplay.setInterpolationDuration(SHIELD_COLOR_INTERP_INTERVAL);
			textDisplay.updateMetadataPacket();
			return textDisplay;
		}

		private PacketDisplay spawnOtherDisplay(Location location) {
			PacketDisplay textDisplay = spawnDisplay(location);
			textDisplay.setBackgroundColor(getShieldOtherColor());
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
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			interaction.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		interaction.getPersistentDataContainer().set(SHIELD_ENTITY, PersistentDataType.BOOLEAN, true);
	}

	private int getShieldAlpha() {
		return (int) Math.round(MathUtils.lerp(SHIELD_ALPHA_MIN, SHIELD_ALPHA_MAX, Math.max(Math.min(health / maxHealth, 1), 0)));
	}

	private Color getShieldBaseColor() {
		return Color.fromARGB(SHIELD_COLOR_HEX | (getShieldAlpha() << 24));
	}

	private Color getShieldOtherColor() {
		PlayerInfo playerInfo = Main.getPlayerInfo(player);
		if (playerInfo.team != null) {
			int value = playerInfo.team.getRGBTextColor().value();
			return Color.fromARGB(value | (getShieldAlpha() << 24));
		}
		return getShieldBaseColor();
	}

	private static final float FACTOR = 0.7f;
	/** See {@link java.awt.Color#brighter()} */
	private static Color brighter(Color base) {
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

	private Location getShieldLocation() {
		if (anchor != null)
			return anchor.clone();
		Location eyeLocation = player.getEyeLocation();
		eyeLocation.setPitch(0);
		Vector direction = eyeLocation.getDirection();
		direction.setY(0);
		eyeLocation.add(direction.multiply(1.5));
		return eyeLocation;
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

	private void playBreakEffect() {
		Location loc = getShieldLocation();
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.5f, 1f);
		// play block crack effect on the plane the shield occupies
		BlockData data = Main.getPlayerInfo(player).team instanceof TeamArenaTeam team ?
			Objects.requireNonNull(Material.getMaterial(team.getDyeColour().toString() + "_STAINED_GLASS")).createBlockData() :
			Material.LIGHT_BLUE_STAINED_GLASS.createBlockData();
		double x = loc.getX(), y = loc.getY(), z = loc.getZ();
		Vector front = loc.getDirection();
		front.setY(0);
		Vector right = new Vector(0, 1, 0).crossProduct(front).normalize().multiply(SHIELD_WIDTH / 5);
		double xStep = right.getX(), zStep = right.getZ();
		for (int j = -5; j <= 5; j++) {
			loc.setY(y + j * (SHIELD_HEIGHT / 5));
			for (int i = -5; i <= 5; i++) {
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
			playBreakEffect();
			cleanUp();
			return;
		}
		if (duration != -1 && world.getGameTime() - spawnedAt > duration) {
			cleanUp();
			return;
		}

		updateLocations();
		updateViewers();

		long tickSinceSpawn = (player.getWorld().getGameTime() - spawnedAt) % (SHIELD_COLOR_INTERP_INTERVAL * 2);
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
		lastHealth = health;

		// regen
		if (world.getGameTime() - lastDamageTick >= SHIELD_REGEN_COOLDOWN) {
			health = Math.min(maxHealth, health + SHIELD_REGEN_PER_TICK);
		}

		player.showTitle(buildTitle());
	}

	public void cleanUp() {
		if (!valid)
			return;
		task.cancel();

		for (PacketDisplay playerDisplay : playerDisplays) {
			playerDisplay.remove();
		}
		for (PacketDisplay otherDisplay : otherDisplays) {
			otherDisplay.remove();
		}
		boxes.forEach(Entity::remove);
		boxes.clear();
		valid = false;
	}

	public boolean isFriendly(LivingEntity attacker) {
		if (attacker == player)
			return true;
		PlayerInfo playerInfo = Main.getPlayerInfo(player);
		if (playerInfo.team == null)
			return false;
		return playerInfo.team.hasMember(attacker);
	}

}

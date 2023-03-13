package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingOutlineManager;
import me.toomuchzelda.teamarenapaper.teamarena.building.EntityBuilding;
import me.toomuchzelda.teamarenapaper.teamarena.building.PreviewableBuilding;
import me.toomuchzelda.teamarenapaper.utils.BlockUtils;
import me.toomuchzelda.teamarenapaper.utils.GlowUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.phys.AABB;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class DemoMine extends EntityBuilding implements PreviewableBuilding {
	public static final int TIME_TO_ARM = 30;

	public final TeamArenaTeam team;
	ArmorStand[] stands;
	PacketMineHitbox hitboxEntity;
	Player triggerer; //store the player that stepped on it for shaming OR the demo if remote detonate

	//for construction
	Location baseLoc;
	Color color;

	int damage = 0; //amount of damage it has
	int creationTime; //store for knowing when it gets 'armed' after placing
	int timeToDetonate;

	MineType type;

	private static Location blockToLocation(Block block) {
		Location blockLoc = block.getLocation();
		double topOfBlock = BlockUtils.getBlockHeight(block);
		return blockLoc.add(0.5d, topOfBlock, 0.5d);
	}

	/**
	 * Creates a new Demolition mine
	 * @param player The demolition player
	 * @param block The block the mine is sitting on
	 */
	public DemoMine(Player player, Block block) {
		super(player, blockToLocation(block));
		setName("Mine");
		this.team = Main.getPlayerInfo(player).team;
	}

	@Override
	public void onPlace() {
		Block block = getLocation().getBlock().getRelative(0, -1, 0);
		this.color = BlockUtils.getBlockBukkitColor(block);

		double topOfBlock = BlockUtils.getBlockHeight(block);
		this.baseLoc = block.getLocation().add(0.5, topOfBlock, 0.5);

		this.creationTime = TeamArena.getGameTick();
		this.hitboxEntity = new PacketMineHitbox(baseLoc.clone().add(0, -0.20d, 0));
		this.hitboxEntity.respawn();
	}

	@Override
	public void onTick() {
		if (isDone()) {
			markInvalid();
		}
		//if it hasn't been armed yet
		else if (!isArmed()) {
			//indicate its armed
			if (TeamArena.getGameTick() == creationTime + DemoMine.TIME_TO_ARM) {
				World world = hitboxEntity.getWorld();
				world.playSound(hitboxEntity.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1f, 1f);
				world.spawnParticle(Particle.CRIT, hitboxEntity.getLocation()
					.add(0, 0.4, 0), 2, 0, 0, 0, 0);

				Component message = Component.text("Your " + type.name + " is now armed", NamedTextColor.GREEN);
				PlayerUtils.sendKitMessage(owner, message, message);
			}
			// else do nothing and don't enter the control statement below that checks for collision
		}
		//if it hasn't been stepped on already check if anyone's standing on it
		else if (!isTriggered()) {
			BoundingBox box = hitboxEntity.getBoundingBox();
			for (Player stepper : Main.getGame().getPlayers()) {
				if (team.getPlayerMembers().contains(stepper))
					continue;

				if (stepper.getBoundingBox().overlaps(box)) {
					//they stepped on mine, trigger explosion
					trigger(stepper);
					break;
				}
			}

			tickViewers();
		}
	}

	private final Set<Player> outlineViewers = new HashSet<>();
	protected void tickViewers() {
		Set<Player> canSee = Bukkit.getOnlinePlayers().stream()
			.filter(player -> BuildingOutlineManager.shouldSeeOutline(this, player))
			.collect(Collectors.toSet());
		// remove invalid viewers
		for (var iter = outlineViewers.iterator(); iter.hasNext();) {
			Player player = iter.next();
			if (!player.isOnline() || !canSee.contains(player)) {
				hideOutline(player);
				iter.remove();
			}
		}
		// add new viewers
		for (Player player : canSee) {
			if (outlineViewers.add(player)) {
				showOutline(player);
			}
		}
	}

	protected void hideOutline(Player player) {
		GlowUtils.setGlowing(List.of(player), Arrays.asList(stands), false, null);
	}

	protected void showOutline(Player player) {
		GlowUtils.setGlowing(List.of(player), Arrays.asList(stands), true, NamedTextColor.nearestTo(getOutlineColor()));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		hitboxEntity.remove();

		outlineViewers.forEach(this::hideOutline);
		for (ArmorStand stand : stands) {
			stand.remove();
		}
		outlineViewers.clear();
		hitboxEntity.remove();
	}

	@Override
	public @NotNull Collection<? extends PacketEntity> getPacketEntities() {
		return List.of(); // hitbox entity shouldn't be visible
	}

	/**
	 * Returns true if mine extinguished/removed
	 */
	boolean hurt() {
		this.damage++;
		World world = this.hitboxEntity.getWorld();
		Location loc = hitboxEntity.getLocation();
		for(int i = 0; i < 3; i++) {
			world.playSound(loc, Sound.BLOCK_GRASS_HIT, 1f, 0.5f);
			world.spawnParticle(Particle.CLOUD, loc.clone().add(0d, 0.2d, 0d), 1,
					0.2d, 0.2d, 0.2d, 0.02d);
		}

		if(this.damage >= type.damageToKill) {
			// game command: /particle minecraft:cloud ~3 ~0.2 ~ 0.2 0.2 0.2 0.02 3 normal
			world.spawnParticle(Particle.CLOUD, hitboxEntity.getLocation().add(0d, 0.2d, 0d), 3,
					0.2d, 0.2d, 0.2d, 0.02d);
			world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 1f);
			world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 1.3f);
			world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1.5f, 1f);
			markInvalid();
			// reimburse mine items
			KitDemolitions.DemolitionsAbility.addRegeneratingMine(owner, type, TeamArena.getGameTick());
			return true;
		}
		return false;
	}

	public void trigger(Player triggerer) {
		this.triggerer = triggerer;

		Component message;
		if(this.owner == triggerer) {
			this.timeToDetonate = type.timeToDetonateRemote;
			message = Component.text("Remote triggered your " + this.type.name, NamedTextColor.AQUA);
		}
		else {
			this.timeToDetonate = type.timeToDetonate;
			message = Component.text("Your " + this.type.name + " was triggered!", NamedTextColor.AQUA);
		}

		PlayerUtils.sendKitMessage(this.owner, message, message);

		World world = hitboxEntity.getWorld();
		Location loc = hitboxEntity.getLocation();

		world.playSound(loc, Sound.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_CREEPER_HURT, 1f, 0f);
		world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0f);

		// reimburse mine items
		KitDemolitions.DemolitionsAbility.addRegeneratingMine(owner, type, TeamArena.getGameTick());
		//subclass here
	}

	abstract boolean isDone();

	boolean isTriggered() {
		return this.triggerer != null;
	}

	boolean isArmed() {
		return TeamArena.getGameTick() > this.creationTime + DemoMine.TIME_TO_ARM;
	}

	@Override
	public @Nullable PreviewResult doRayTrace() {
		Location eyeLocation = owner.getEyeLocation();
		World world = owner.getWorld();
		var result = world.rayTraceBlocks(eyeLocation, eyeLocation.getDirection(), 5, FluidCollisionMode.NEVER, true);
		if (result == null || result.getHitBlock() == null || result.getHitBlockFace() == null)
			return null;
		BlockFace face = result.getHitBlockFace();
		Block block = result.getHitBlock().getRelative(face);
		boolean canPlace = BuildingManager.canPlaceAt(block) &&
			KitDemolitions.isValidMineBlock(block.getRelative(BlockFace.DOWN));
		Location location = block.getLocation().add(0.5, BlockUtils.getBlockHeight(block), 0.5);
		return new PreviewResult(canPlace, location);
	}

	protected class PacketMineHitbox extends PacketEntity {
		private final BoundingBox hitbox;
		public int lastHurtTime;

		public PacketMineHitbox(Location location) {
			super(PacketEntity.NEW_ID, EntityType.AXOLOTL, location, null, PacketEntity.VISIBLE_TO_ALL);

			this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
			this.updateMetadataPacket();

			AABB bb = net.minecraft.world.entity.EntityType.AXOLOTL.getDimensions().makeBoundingBox(
					location.getX(), location.getY(), location.getZ());
			this.hitbox = new BoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
			lastHurtTime = 0;
		}

		//Axolotol hitbox shouldn't move, no need to adjust for that
		public BoundingBox getBoundingBox() {
			return hitbox;
		}

		@Override
		public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
			if (!attack)
				return;
			if (Main.getGame().isDead(player))
				return;

			DemoMine mine = DemoMine.this;
			//teammate punches it
			if (player != mine.owner && mine.team.getPlayerMembers().contains(player)) {
				player.sendMessage(Component.text("This is ", NamedTextColor.AQUA).append(
					mine.owner.playerListName()).append(Component.text("'s " + mine.type.name)));
			} else {
				int currentTick = TeamArena.getGameTick();
				int diff = currentTick - lastHurtTime;
				if (diff >= 10) {
					lastHurtTime = currentTick;
					if (mine.hurt()) {
						Component message;
						if (player != mine.owner) {
							message = Component.text("You've broken one of ", NamedTextColor.AQUA).append(
								mine.owner.playerListName()).append(Component.text("'s " + mine.type.name + "s!",
								NamedTextColor.AQUA));

							Component ownerMessage = Component.text("Someone broke one of your " + mine.type.name + "s!",
								NamedTextColor.AQUA);

							PlayerUtils.sendKitMessage(mine.owner, ownerMessage, ownerMessage);
						} else {
							message = Component.text("Broke your " + mine.type.name).color(NamedTextColor.AQUA);
						}
						player.sendMessage(message);
					}
				}
			}
		}
	}
}

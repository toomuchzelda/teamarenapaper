package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.building.BlockBuilding;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingOutlineManager;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.building.PreviewableBuilding;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class Teleporter extends BlockBuilding implements PreviewableBuilding {
	public static final int TELEPORT_COOLDOWN = 30;

	int lastUsedTick;
	@Nullable
	BlockCoords linkedTeleporter;
	BlockState originalBlockState;
	BoundingBox hitBox;
	TextColor teamColor;

	private static final ItemStack ICON = new ItemStack(Material.HONEYCOMB_BLOCK);

	public Teleporter(Player player, Location loc) {
		super(player, loc);
		setName("Teleporter");
		setIcon(ICON);
		setOutlineColor(NamedTextColor.AQUA);
		teamColor = Main.getPlayerInfo(player).team.getRGBTextColor();
		Block block = loc.getBlock();
		hitBox = BoundingBox.of(block.getRelative(-1, 1, -1), block.getRelative(1, 2, 1));
	}

	@Override
	protected Location getHologramLocation() {
		return getLocation().add(0, 1, 0);
	}

	public int getLastUsedTick() {
		return lastUsedTick;
	}

	public void setLastUsedTick(int newTick) {
		this.lastUsedTick = newTick;
	}

	public int getTimeElapsed() {
		return TeamArena.getGameTick() - lastUsedTick;
	}

	public boolean isReady() {
		return TeamArena.getGameTick() - lastUsedTick > TELEPORT_COOLDOWN;
	}

	@Nullable
	public BlockCoords getLinkedTeleporter() {
		return linkedTeleporter;
	}

	public void setLinkedTeleporter(BlockCoords block) {
		this.linkedTeleporter = block;
		setLastUsedTick(TeamArena.getGameTick());
	}

	@Override
	public void onPlace() {
		super.onPlace();

		Block block = getLocation().getBlock();
		this.originalBlockState = block.getState();
		block.setType(Material.HONEYCOMB_BLOCK, false);

		this.lastUsedTick = TeamArena.getGameTick();

		var otherTeleporters = BuildingManager.getPlayerBuildings(owner, Teleporter.class);
		if (otherTeleporters.size() > 0) {
			Teleporter toLink = otherTeleporters.get(otherTeleporters.size() - 1);
			setLinkedTeleporter(new BlockCoords(toLink.location));
			toLink.setLinkedTeleporter(new BlockCoords(location));

			owner.sendMessage(Component.text("A link to the teleporter at (%d, %d, %d) has been established."
							.formatted(linkedTeleporter.x(), linkedTeleporter.y(), linkedTeleporter.z()),
					NamedTextColor.YELLOW));
		}

		BuildingOutlineManager.registerBuilding(this);
	}

	@Override
	public boolean onBreak(BlockBreakEvent event) {
		if (event.isCancelled())
			return false;
		event.setCancelled(true);

		var player = event.getPlayer();
		var block = event.getBlock();
		if (owner == player) { // owner
			player.sendMessage(Component.text("You demolished a teleporter.", NamedTextColor.GREEN));
			BuildingManager.destroyBuilding(this);
		} else if (Main.getGame().canAttack(owner, player)) { // enemy
			player.sendMessage(Component.text("You destroyed an enemy teleporter.", NamedTextColor.RED));
			// block break sound
			owner.playSound(owner, block.getBlockSoundGroup().getBreakSound(), 0.1f, 1);
			owner.sendMessage(Component.textOfChildren(
					player.playerListName(), // rgb name
					Component.text(" destroyed your teleporter at ", NamedTextColor.RED),
					Component.text("(%d, %d, %d)".formatted(block.getX(), block.getY(), block.getZ()), NamedTextColor.YELLOW),
					Component.text(".", NamedTextColor.RED)
			));
			BuildingManager.destroyBuilding(this);
		} else { // teammate
			player.sendMessage(Component.textOfChildren(
					Component.text("This teleporter is owned by ", NamedTextColor.BLUE),
					owner.playerListName(),
					Component.text(".", NamedTextColor.BLUE)
			));
		}
		return true;
	}

	boolean checkCanTeleport(Entity entity) {
		if (!(entity instanceof Player player))
			return false;

		//If it is CTF, players with flag cannot use TP
		if (Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player))
			return false;

		//Allies and spies disguised as allies can use
		//User must be sneaking and be on top of the teleporter block
		return player.isSneaking() && player.getLocation().getY() - 1 == (int)location.getY() &&
			(!Main.getGame().canAttack(owner, player) || PlayerUtils.isDisguisedAsAlly(owner, player));
	}

	static final Component NOT_CONNECTED = Component.text("Not Connected", NamedTextColor.GRAY);
	@Override
	public void onTick() {
		if (linkedTeleporter == null) {
			setText(NOT_CONNECTED);
			return;
		}

		var linkedBuilding = BuildingManager.getBuildingAt(linkedTeleporter);

		Component hologramText;
		if (linkedBuilding instanceof Teleporter other) { // has link
			// update hologram
			if (isReady() && other.isReady()) {
				hologramText = Component.text("Teleport Ready", teamColor);
				// teleport eligible entities
				var nearbyEntities = location.getWorld().getNearbyEntities(hitBox, this::checkCanTeleport);
				if (nearbyEntities.size() != 0)
					teleport((Player) nearbyEntities.iterator().next(), other);
			} else {
				double progress = Math.min(1, (double) other.getTimeElapsed() / TELEPORT_COOLDOWN);
				int percentage = (int) Math.round(100d * progress);
				hologramText = TextUtils.getProgressText("Recharging... " + percentage + "%",
					NamedTextColor.GRAY, teamColor, teamColor, progress);
			}
		} else {
			hologramText = NOT_CONNECTED;
		}
		setText(hologramText);
	}

	public void teleport(Player player, Teleporter destination) {
		// offset the destination relative to player's location on the teleporter
		Location actualDestination = player.getLocation().subtract(location).add(destination.location);
		player.teleport(actualDestination);

		int currentTick = TeamArena.getGameTick();
		setLastUsedTick(currentTick);
		destination.setLastUsedTick(currentTick);

		var world = owner.getWorld();
		world.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
		world.playSound(destination.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// reset the block
		originalBlockState.update(true, false);
	}

	@Override
	public @Nullable PreviewResult doRayTrace() {
		Location eyeLocation = owner.getEyeLocation();
		var result = owner.getWorld().rayTraceBlocks(eyeLocation, eyeLocation.getDirection(), 4,
			FluidCollisionMode.NEVER, false);
		if (result != null) {
			// only accept blockFace = UP
			return new PreviewResult(result.getHitBlockFace() == BlockFace.UP,
				result.getHitBlock().getLocation().add(0.5, 0.01, 0.5));
		}
		return null;
	}

	private static List<PacketEntity> PREVIEW;
	@Override
	public @NotNull List<PacketEntity> getPreviewEntity(Location location) {
		if (PREVIEW == null) {
			var block = new PacketEntity(PacketEntity.NEW_ID, EntityType.FALLING_BLOCK, location, List.of(), null);
			block.setBlockType(Material.HONEYCOMB_BLOCK.createBlockData());
			block.setMetadata(MetaIndex.NO_GRAVITY_OBJ, true);
			block.remove();
			PREVIEW = List.of(block);
		}
		return PREVIEW;
	}
}

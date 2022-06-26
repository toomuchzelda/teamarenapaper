package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.building.Building;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.BoundingBox;

public class Teleporter extends Building {
	public static final int TELEPORT_COOLDOWN = 30;

	int lastUsedTick;
	Block linkedTeleporter;
	BlockState originalBlockState;
	BoundingBox hitBox;

	public Teleporter(Player player, Location loc) {
		super(player, loc);
		setName("Teleporter");
		Block block = loc.getBlock();
		hitBox = BoundingBox.of(block.getRelative(-1, 1, -1), block.getRelative(1, 2, 1));
	}

	@Override
	protected Location getHologramLocation() {
		return getLocation().add(0.5, 1.5, 0.5);
	}

	public BlockState getOriginalBlockState() {
		return this.originalBlockState;
	}

	public int getLastUsedTick() {
		return lastUsedTick;
	}

	public void setLastUsedTick(int newTick) {
		this.lastUsedTick = newTick;
	}

	public int getRemainingCD() {
		return TeamArena.getGameTick() - lastUsedTick;
	}

	public boolean isOnCooldown() {
		return TeamArena.getGameTick() - lastUsedTick <= TELEPORT_COOLDOWN;
	}

	public Block getLinkedTeleporter() {
		return linkedTeleporter;
	}

	public void setLinkedTeleporter(Block block) {
		this.linkedTeleporter = block;
		setLastUsedTick(TeamArena.getGameTick());
	}

	@Override
	public void onPlace() {
		super.onPlace();

		Block block = getLocation().getBlock();
		this.originalBlockState = block.getState();
		block.setType(Material.HONEYCOMB_BLOCK, false);

		// restore original block
		BuildingManager.registerBlockBreakCallback(block, this, this::onBlockBroken);

		this.lastUsedTick = TeamArena.getGameTick();

		var otherTeleporters = BuildingManager.getPlayerBuildings(owner, Teleporter.class);
		if (otherTeleporters.size() >= 2) {
			Teleporter toLink = otherTeleporters.get(otherTeleporters.size() - 2);
			setLinkedTeleporter(toLink.location.getBlock());
			toLink.setLinkedTeleporter(location.getBlock());

			owner.sendMessage(Component.text("A link to the teleporter at (%d, %d, %d) has been established."
							.formatted(linkedTeleporter.getX(), linkedTeleporter.getY(), linkedTeleporter.getZ()),
					NamedTextColor.YELLOW));
		}
	}

	public void onBlockBroken(BlockBreakEvent event) {
		if (event.isCancelled())
			return;
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
	}

	boolean checkCanTeleport(Entity entity) {
		if (!(entity instanceof Player player))
			return false;

		//If it is CTF, players with flag cannot use TP
		if (Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(player))
			return false;

		//Allies and spies disguised as allies can use
		//User must be sneaking and be on top of the teleporter block
		return (!Main.getGame().canAttack(player, player) ||
						PlayerUtils.isDisguisedAsAlly(player, player)) &&
				player.isSneaking() &&
				player.getLocation().getY() - 1 == location.getY();
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
			if (!other.isOnCooldown()) {
				hologramText = Component.text("Teleport Ready");
				// teleport eligible entities
				location.getWorld().getNearbyEntities(hitBox, this::checkCanTeleport)
						.forEach(entity -> teleport((Player) entity, other));
			} else {
				long percCD = Math.round(100d * (double) other.getRemainingCD() / TELEPORT_COOLDOWN);
				hologramText = Component.text("Recharging... " + percCD + "%");
			}
		} else {
			hologramText = NOT_CONNECTED;
		}
		setText(hologramText);
	}

	public void teleport(Player player, Teleporter destination) {
		// offset the destination relative to player's location on the teleporter
		Location actualDestination = player.getLocation().subtract(location).add(destination.getLocation());
		player.teleport(actualDestination);

		int currentTick = TeamArena.getGameTick();
		setLastUsedTick(currentTick);
		destination.setLastUsedTick(currentTick);

		owner.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// reset the block
		originalBlockState.update(true, false);
	}
}

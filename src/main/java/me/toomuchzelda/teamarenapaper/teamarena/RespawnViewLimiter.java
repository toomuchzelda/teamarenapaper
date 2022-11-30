package me.toomuchzelda.teamarenapaper.teamarena;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.mojang.datafixers.util.Pair;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.EquipmentSlot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that provides functions to limit what dead players in respawning games can see.
 * This is to prevent them from hanging around while dead to gain intel on enemies.
 *
 * @author toomuchzelda
 */
public class RespawnViewLimiter
{
	private static final Map<Player, RestrictInfo> RESTRICTED_PLAYERS = new LinkedHashMap<>();

	private record RestrictInfo(PacketAngel angel, Location baseLoc) {}

	static void limitView(Player spectator) {
		if(!RESTRICTED_PLAYERS.containsKey(spectator)) {
			final Location loc = spectator.getLocation();

			// Spawn indicator allay
			PacketAngel angel = new PacketAngel(loc, spectator);
			//TODO chance of dancing
			//TODO hold the dead player's head
			angel.respawn();

			// Make the player ride the allay to prevent them from moving anywhere else
			angel.mountOwner(true);

			RestrictInfo rinfo = new RestrictInfo(angel, loc);
			RESTRICTED_PLAYERS.put(spectator, rinfo);

			// Put a block above the spectator to prevent them from using third person mode to gain more view
			// The block is clientside only, it does not exist to any other players
			spectator.sendBlockChange(loc.clone().add(0, 2, 0), Material.BARRIER.createBlockData());
		}
	}

	static void releaseView(Player respawnedPlayer) {
		RestrictInfo rinfo = RESTRICTED_PLAYERS.remove(respawnedPlayer);
		if(rinfo != null) {
			rinfo.angel().mountOwner(false);
			rinfo.angel().remove();

			// Remove the clientside block with whatever the original block was.
			final Location blockLoc = rinfo.baseLoc().clone().add(0, 2, 0);
			respawnedPlayer.sendBlockChange(blockLoc, blockLoc.getBlock().getBlockData());
		}
	}

	/**
	 * Spam restricted players with "Look At" packets so they always looks towards the ground.
	 */
	public static void tick() {
		for(var entry : RESTRICTED_PLAYERS.entrySet()) {
			Location lookingLoc = entry.getKey().getLocation();

			lookingLoc.setPitch(Math.max(lookingLoc.getPitch() - 10, -90f));
			lookingLoc.setY(entry.getValue().baseLoc().getY());
			entry.getValue().angel().move(lookingLoc);
		}
	}

	static boolean isRestricted(Player player) {
		return RESTRICTED_PLAYERS.containsKey(player);
	}

	private static class PacketAngel extends PacketEntity {

		private static final int[] EMPTY_ARR = new int[0];

		private final Player owner;
		private final ClientboundSetEquipmentPacket equipmentPacket;

		/**
		 * Must call respawn manually after construction!
		 *
		 * @param location Location to spawn at.
		 * @param owner    Player the ghost/angel represents.
		 */
		public PacketAngel(Location location, Player owner) {
			super(PacketEntity.NEW_ID, EntityType.ALLAY, location, null, player -> true);
			this.owner = owner;

			this.setText(owner.playerListName().append(Component.text("'s soul")), false);
			if(MathUtils.randomMax(255) == 255) { // dancing easter egg
				this.setDancing(true);
			}
			this.updateMetadataPacket();

			// Construct the equipment packet and store it.
			ItemStack ownersHead = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta skullMeta = (SkullMeta) ownersHead.getItemMeta();
			skullMeta.setOwningPlayer(this.owner);
			ownersHead.setItemMeta(skullMeta);

			List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = List.of(
					Pair.of(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(ownersHead))
			);
			this.equipmentPacket = new ClientboundSetEquipmentPacket(this.getId(), list);
		}

		/**
		 * @param mount true to mount, false to dismount the owner from the allay.
		 */
		public void mountOwner(boolean mount) {
			PacketContainer setPassengersPacket = new PacketContainer(PacketType.Play.Server.MOUNT);
			setPassengersPacket.getIntegers().write(0, this.getId());
			int[] mountedEnts = mount ? new int[] {this.owner.getEntityId()} : EMPTY_ARR;
			setPassengersPacket.getIntegerArrays().write(0, mountedEnts);
			PlayerUtils.sendPacket(this.owner, setPassengersPacket);
		}

		public void setDancing(boolean dance) {
			this.setMetadata(MetaIndex.ALLAY_DANCING_OBJ, dance);
		}

		// Override to also send an equipment packet so the allay appears to be holding the dead
		// player's head.
		@Override
		protected void spawn(Player player) {
			super.spawn(player);

			PlayerUtils.sendPacket(player, this.equipmentPacket);
		}
	}
}

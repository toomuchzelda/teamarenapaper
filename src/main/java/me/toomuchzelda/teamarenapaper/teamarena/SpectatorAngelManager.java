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
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class that provides functions to limit what dead players in respawning games can see.
 * This is to prevent them from hanging around while dead to gain intel on enemies.
 * An allay (angel) is spawned so nearby players know there is someone watching.
 *
 * @author toomuchzelda
 */
public class SpectatorAngelManager
{
	private static final int ANGEL_LIVE_TIME = 40;
	private static final Map<Player, RestrictInfo> RESTRICTED_PLAYERS = new LinkedHashMap<>();

	// if baseLoc == null then the player is not restricted to the angel's position.
	// the angel will also self-destruct after some given time.
	private record RestrictInfo(PacketAngel angel, Location baseLoc) {}

	/**
	 * Spawn the angel at the player's position
	 * @param spectator The player.
	 * @param lock true if the player should be locked to the angel's position. false will have the angel follow
	 *             the player's position.
	 */
	public static void spawnAngel(Player spectator, boolean lock) {
		if(!RESTRICTED_PLAYERS.containsKey(spectator)) {
			Location loc = spectator.getLocation();

			// Spawn indicator allay
			PacketAngel angel = new PacketAngel(loc, spectator);
			angel.respawn();

			if(lock) {
				// Make the player ride the allay to prevent them from moving anywhere else
				angel.mountOwner(true);

				// Put a block above the spectator to prevent them from using third person mode to gain more view
				// The block is clientside only, it does not exist to any other players
				// Edit: Not do the block change anymore
				//spectator.sendBlockChange(loc.clone().add(0, 1, 0), Material.BARRIER.createBlockData());
			}
			else {
				loc = null; // Set to null, so we pass null into the RestrictInfo ctor.
			}

			RestrictInfo rinfo = new RestrictInfo(angel, loc);
			RESTRICTED_PLAYERS.put(spectator, rinfo);
		}
	}

	static void removeAngel(Player respawnedPlayer) {
		RestrictInfo rinfo = RESTRICTED_PLAYERS.remove(respawnedPlayer);
		if(rinfo != null) {
			if(rinfo.baseLoc() != null) {
				rinfo.angel().mountOwner(false);
				// Remove the clientside block with whatever the original block was.
				// Edit: Not do the block change anymore
				//final Location blockLoc = rinfo.baseLoc().clone().add(0, 1, 0);
				//respawnedPlayer.sendBlockChange(blockLoc, blockLoc.getBlock().getBlockData());
			}

			rinfo.angel().remove();
		}
	}

	/**
	 * Make the allay look wherever the player is looking.
	 * If it's not a restricted allay then remove it too if it has lived for long enough.
	 */
	public static void tick() {
		final int currentTick = TeamArena.getGameTick();
		final List<Player> toRemove = new LinkedList<>();

		for (Map.Entry<Player, RestrictInfo> entry : RESTRICTED_PLAYERS.entrySet()) {
			final Location lookingLoc = entry.getKey().getLocation();
			final RestrictInfo rinfo = entry.getValue();

			lookingLoc.setPitch(Math.max(lookingLoc.getPitch() - 10, -90f));
			if (rinfo.baseLoc() != null) {
				// prevent moving upwards when opening the kit menu
				lookingLoc.setY(rinfo.baseLoc().getY());
				rinfo.angel().move(lookingLoc);
			}
			else { // baseLoc is null - need to check if angel lived for long enough
				if (currentTick - rinfo.angel().spawnTime >= ANGEL_LIVE_TIME) {
					toRemove.add(entry.getKey());
				}
				else {
					rinfo.angel().move(lookingLoc);
				}
			}
		}

		toRemove.forEach(SpectatorAngelManager::removeAngel);
	}

	public static boolean isRestricted(Player player) {
		return RESTRICTED_PLAYERS.containsKey(player);
	}

	private static class PacketAngel extends PacketEntity {

		private static final int[] EMPTY_ARR = new int[0];

		private final Player owner;
		private final PacketContainer equipmentPacket;
		final int spawnTime;

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
			this.equipmentPacket = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT, new ClientboundSetEquipmentPacket(this.getId(), list));

			this.spawnTime = TeamArena.getGameTick();
		}

		/**
		 * @param mount true to mount, false to dismount the owner from the allay.
		 */
		public void mountOwner(boolean mount) {
			PacketContainer setPassengersPacket = new PacketContainer(PacketType.Play.Server.MOUNT);
			setPassengersPacket.getIntegers().write(0, this.getId());
			int[] mountedEnts = mount ? new int[] {this.owner.getEntityId()} : EMPTY_ARR;
			setPassengersPacket.getIntegerArrays().write(0, mountedEnts);
			this.sendPacket(this.owner, setPassengersPacket);
		}

		public void setDancing(boolean dance) {
			this.setMetadata(MetaIndex.ALLAY_DANCING_OBJ, dance);
		}

		// Override to also send an equipment packet so the allay appears to be holding the dead
		// player's head.
		@Override
		protected void spawn(Player player) {
			super.spawn(player);

			this.sendPacket(player, this.equipmentPacket);
		}

		@Override
		public void despawn() {
			super.despawn();

			final World world = this.owner.getWorld();
			final Location loc = this.owner.getLocation();
			for(int i = 0; i < 3; i++)
				world.spawnParticle(Particle.CLOUD, loc, 1, 0.2d, 0.2d, 0.2d, 0.02d);
		}
	}
}

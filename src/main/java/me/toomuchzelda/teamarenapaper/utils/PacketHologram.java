package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

//class for holograms made with marker armor stands
// originally made for player nametags using Components
// not using those anymore so adapted to be an independent packet hologram thing

/**
 * class for holograms made with marker armor stands
 * originally made for player nametags using Components
 * not using those anymore so adapted to be an independent packet hologram thing
 *
 * @author toomuchzelda
 */
public class PacketHologram extends PacketEntity
{

	public PacketHologram(int id, EntityType entityType, Location location, @Nullable LinkedHashSet<Player> viewers, @Nullable Predicate<Player> viewerRule) {
		super(id, entityType, location, viewers, viewerRule);
	}

	@Override
	protected void createMetadata() {
		metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		metadataPacket.getIntegers().write(0, id);

		//https://www.spigotmc.org/threads/simulating-potion-effect-glowing-with-protocollib.218828/#post-2246160
		// and https://www.spigotmc.org/threads/protocollib-entity-metadata-packet.219146/
		WrappedDataWatcher data = new WrappedDataWatcher();
		this.data = data;

		//metaObject: implies a field with index 0 and type of byte, i think
		WrappedDataWatcher.WrappedDataWatcherObject metaObject = new WrappedDataWatcher.WrappedDataWatcherObject(
				MetaIndex.BASE_ENTITY_META, WrappedDataWatcher.Registry.get(Byte.class));
		this.metadata = metaObject;

		//metaObject the field, byte the value
		// in this case the status bit of the metadata packet. 32 is the bit mask for invis
		data.setObject(metaObject, invisBitMask);

		//custom name field
		WrappedDataWatcher.WrappedDataWatcherObject customName =
				new WrappedDataWatcher.WrappedDataWatcherObject(customNameIndex,
						WrappedDataWatcher.Registry.getChatComponentSerializer(true));
		this.customNameMetadata = customName;

		Optional<?> nameComponent = Optional.of(AdventureComponentConverter.fromComponent(
				customNameComponent).getHandle());

		data.setObject(customName, nameComponent);

		//custom name visible
		WrappedDataWatcher.WrappedDataWatcherObject customNameVisible =
				new WrappedDataWatcher.WrappedDataWatcherObject(customNameVisibleIndex,
						WrappedDataWatcher.Registry.get(Boolean.class));
		data.setObject(customNameVisible, true);

		//marker armorstand (no client side hitbox)
		WrappedDataWatcher.WrappedDataWatcherObject armorStandMeta =
				new WrappedDataWatcher.WrappedDataWatcherObject(ARMOR_STAND_METADATA_INDEX,
						WrappedDataWatcher.Registry.get(Byte.class));
		data.setObject(armorStandMeta, ARMOR_STAND_MARKER_BIT_MASK);

		metadataPacket.getWatchableCollectionModifier().write(0, data.getWatchableObjects());
	}

	public void setText(Component component, boolean sendPacket) {
		Optional<?> nameComponent = Optional.of(AdventureComponentConverter.fromComponent(
				component).getHandle());

		//customNameWatcher.setValue(nameComponent, true);
		this.data.setObject(customNameMetadata, nameComponent);

		if(sendPacket) {
			for (Player p : viewers) {
				PlayerUtils.sendPacket(p, metadataPacket);
			}
		}
	}
}

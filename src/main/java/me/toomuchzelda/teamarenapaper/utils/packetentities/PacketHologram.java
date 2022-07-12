package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Optional;
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
	private Component text;

	public PacketHologram(Location location, @Nullable LinkedHashSet<Player> viewers, @Nullable Predicate<Player> rule,
						  Component text) {
		super(PacketEntity.NEW_ID, EntityType.ARMOR_STAND, location, viewers, rule);

		this.text = text;

		//setup the metadata
		this.data.setObject(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);

		Optional<?> nameComponent = Optional.of(AdventureComponentConverter.fromComponent(
				this.text).getHandle());

		this.data.setObject(MetaIndex.CUSTOM_NAME_OBJ, nameComponent);
		this.data.setObject(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, Boolean.TRUE);

		this.data.setObject(MetaIndex.ARMOR_STAND_BITFIELD_OBJ, MetaIndex.ARMOR_STAND_MARKER_MASK);

		this.updateMetadataPacket();

		this.respawn();
	}

	public void setText(Component component, boolean sendPacket) {
		Optional<?> nameComponent = Optional.of(AdventureComponentConverter.fromComponent(
				component).getHandle());

		this.data.setObject(MetaIndex.CUSTOM_NAME_OBJ, nameComponent);

		if(sendPacket) {
			this.refreshViewerMetadata();
		}
	}
}

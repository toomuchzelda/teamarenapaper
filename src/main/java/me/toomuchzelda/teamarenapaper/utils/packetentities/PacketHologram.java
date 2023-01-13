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

/**
 * class for holograms made with marker armor stands
 * originally made for player nametags using Components
 * not using those anymore so adapted to be an independent packet hologram
 *
 * @author toomuchzelda
 */
public class PacketHologram extends PacketEntity
{

	public PacketHologram(Location location, @Nullable LinkedHashSet<Player> viewers, @Nullable Predicate<Player> rule,
						  Component text) {
		super(PacketEntity.NEW_ID, EntityType.ARMOR_STAND, location, viewers, rule);

		//setup the metadata
		this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);

		this.setText(text, false);
		this.setMetadata(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, Boolean.TRUE);

		this.setMetadata(MetaIndex.ARMOR_STAND_BITFIELD_OBJ, MetaIndex.ARMOR_STAND_MARKER_MASK);

		this.updateMetadataPacket();

		this.respawn();
	}
}

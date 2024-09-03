package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.comphenix.protocol.wrappers.Vector3F;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

public class PacketDisplay extends PacketEntity {

	public PacketDisplay(int id, EntityType entityType, Location location, @Nullable Collection<? extends Player> viewers, @Nullable Predicate<Player> viewerRule) {
		super(id, entityType, location, viewers, viewerRule);
	}

	private static Vector3F bukkitToPLibVec(Vector vec) {
        return new Vector3F((float) vec.getX(), (float) vec.getY(), (float) vec.getZ());
	}

	// Not sure how this works with joml Vector3f serializer
	public void translate(Vector translation) {
		this.setMetadata(MetaIndex.DISPLAY_TRANSLATION_OBJ, bukkitToPLibVec(translation));
	}

	public void setScale(Vector scale) {
		this.setMetadata(MetaIndex.DISPLAY_SCALE_OBJ, bukkitToPLibVec(scale));
	}

	public void setBillboard(MetaIndex.DisplayBillboardOption option) {
		this.setMetadata(MetaIndex.DISPLAY_BILLBOARD_OBJ, option.get());
	}
}

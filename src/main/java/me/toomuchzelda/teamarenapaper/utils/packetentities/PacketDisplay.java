package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.comphenix.protocol.wrappers.Vector3F;
import io.papermc.paper.adventure.PaperAdventure;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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

	public void setInterpolationDelay(int delay) {
		this.setMetadata(MetaIndex.DISPLAY_INTERPOLATION_DELAY_OBJ, delay);
	}

	public void setInterpolationDuration(int duration) {
		this.setMetadata(MetaIndex.DISPLAY_TRANSFORMATION_INTERPOLATION_DURATION_OBJ, duration);
	}

	public void setTeleportDuration(int duration) {
		this.setMetadata(MetaIndex.DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ, duration);
	}

	public void setTranslation(Vector3f translation) {
		this.setMetadata(MetaIndex.DISPLAY_TRANSLATION_OBJ, translation);
	}

	public void setScale(Vector3f scale) {
		this.setMetadata(MetaIndex.DISPLAY_SCALE_OBJ, scale);
	}

	public void text(Component text) {
		this.setMetadata(MetaIndex.TEXT_DISPLAY_TEXT_OBJ, PaperAdventure.asVanilla(text));
	}

	public void setBackgroundColor(@Nullable Color color) {
		int argb = color == null ? 1073741824 : color.asARGB();
		this.setMetadata(MetaIndex.TEXT_DISPLAY_BACKGROUND_COLOR_OBJ, argb);
	}

	public void setTextOpacity(byte opacity) {
		this.setMetadata(MetaIndex.TEXT_DISPLAY_TEXT_OPACITY_OBJ, opacity);
	}

	public void setSeeThrough(boolean seeThrough) {
		Byte bits = (Byte) this.getMetadata(MetaIndex.TEXT_DISPLAY_BITMASK_OBJ);
		if (bits == null) bits = (byte) 0;
		int flag = 1 << MetaIndex.TextDisplayBitmask.IS_SEE_THROUGH.ordinal();
		if (seeThrough) {
			bits = (byte) (bits | flag);
		} else {
			bits = (byte) (bits & ~flag);
		}
		this.setMetadata(MetaIndex.TEXT_DISPLAY_BITMASK_OBJ, bits);
	}

	public void setWidth(float width) {
		this.setMetadata(MetaIndex.DISPLAY_WIDTH_OBJ, width);
	}

	public void setHeight(float height) {
		this.setMetadata(MetaIndex.DISPLAY_HEIGHT_OBJ, height);
	}
}

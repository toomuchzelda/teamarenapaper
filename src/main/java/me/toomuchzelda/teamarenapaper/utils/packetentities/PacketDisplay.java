package me.toomuchzelda.teamarenapaper.utils.packetentities;

import io.papermc.paper.adventure.PaperAdventure;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.PacketUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.util.Brightness;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.function.Predicate;

public class PacketDisplay extends PacketEntity {

	public PacketDisplay(int id, EntityType entityType, Location location, @Nullable Collection<? extends Player> viewers, @Nullable Predicate<Player> viewerRule) {
		super(id, entityType, location, viewers, viewerRule);
	}
	// bukkit
	public void setTranslation(Vector translation) {
		this.setMetadata(MetaIndex.DISPLAY_TRANSLATION_OBJ, PacketUtils.toNMS(translation));
	}
	// joml
	public void setTranslation(Vector3f translation) {
		this.setMetadata(MetaIndex.DISPLAY_TRANSLATION_OBJ, PacketUtils.toNMS(translation));
	}
 	// bukkit
	public void setScale(Vector scale) {
		this.setMetadata(MetaIndex.DISPLAY_SCALE_OBJ, PacketUtils.toNMS(scale));
	}
	// joml
	public void setScale(Vector3f scale) {
		this.setMetadata(MetaIndex.DISPLAY_SCALE_OBJ, PacketUtils.toNMS(scale));
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

	public void setLeftRotation(Quaternionfc leftRotation) {
		this.setMetadata(MetaIndex.DISPLAY_ROTATION_LEFT_OBJ, leftRotation);
	}

	public void setGlowColorOverride(@Nullable Color color) {
		this.setMetadata(MetaIndex.DISPLAY_GLOW_COLOR_OVERRIDE_OBJ, color == null ? -1 : color.asARGB());
	}

	public void setBrightnessOverride(@Nullable Display.Brightness brightnessOverride) {
		int packed = brightnessOverride == null ? -1 :
			new Brightness(brightnessOverride.getBlockLight(), brightnessOverride.getSkyLight()).pack();
		this.setMetadata(MetaIndex.DISPLAY_BRIGHTNESS_OVERRIDE_OBJ, packed);
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

	public void setBlockData(BlockData blockData) {
		this.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, ((CraftBlockData) blockData).getState());
	}
}

package me.toomuchzelda.teamarenapaper.utils.packetentities;

import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

public class AttachedPacketHologram extends AttachedPacketEntity
{
	public AttachedPacketHologram(int id, LivingEntity player, @Nullable Collection<? extends Player> viewers,
								  @Nullable Predicate<Player> viewerRule, Component text, boolean selfSee) {
		super(id, EntityType.ARMOR_STAND, player, viewers, viewerRule, selfSee, false);

		//setup the metadata
		//this.data.setObject(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
		this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);

		this.setText(text, false);
		this.setMetadata(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, Boolean.TRUE);

		this.setMetadata(MetaIndex.ARMOR_STAND_BITFIELD_OBJ, MetaIndex.ARMOR_STAND_MARKER_MASK);

		this.updateMetadataPacket();
	}

	public void setCustomNameVisible(boolean visible) {
		this.setMetadata(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, visible);
	}

	@Override
	public double getYOffset() {
		boolean sneaking;
		if(this.entity instanceof Player p)
			sneaking = p.isSneaking();
		else
			sneaking = this.entity.getPose() == Pose.SNEAKING;

		if(sneaking)
			return this.entity.getHeight();
		else
			return super.getYOffset();
	}
}

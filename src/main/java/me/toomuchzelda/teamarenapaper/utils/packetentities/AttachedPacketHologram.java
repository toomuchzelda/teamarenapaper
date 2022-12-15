package me.toomuchzelda.teamarenapaper.utils.packetentities;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

public class AttachedPacketHologram extends AttachedPacketEntity
{
	public AttachedPacketHologram(Player player, @Nullable Collection<? extends Player> viewers, @Nullable Predicate<Player> viewerRule, Component text) {
		super(Main.getPlayerInfo(player).statusIndicatorId, EntityType.ARMOR_STAND, player, viewers, viewerRule, false);

		//setup the metadata
		this.data.setObject(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);

		this.setText(text, false);
		this.data.setObject(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, Boolean.TRUE);

		this.data.setObject(MetaIndex.ARMOR_STAND_BITFIELD_OBJ, MetaIndex.ARMOR_STAND_MARKER_MASK);

		this.updateMetadataPacket();
	}

	@Override
	public double getYOffset() {
		if(this.player.getPose() == Pose.SNEAKING)
			return this.player.getHeight();
		else
			return super.getYOffset();
	}
}

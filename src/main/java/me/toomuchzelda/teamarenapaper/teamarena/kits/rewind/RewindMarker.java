package me.toomuchzelda.teamarenapaper.teamarena.kits.rewind;

import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.GlowUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketDisplay;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class RewindMarker {
	private final PacketDisplay indicator;

	// Construct once and cache
	private final List<Player> playerList;
	private final List<String> uuidList;
	boolean red;

	RewindMarker(Player rewind) {
		indicator = new PacketDisplay(Bukkit.getUnsafe().nextEntityId(), EntityType.ITEM_DISPLAY,
			rewind.getLocation().add(0d, 0.5d, 0d), null, player -> player == rewind);

		this.playerList = Collections.singletonList(rewind);
		this.uuidList = Collections.singletonList(indicator.getUuid().toString());

		GlowUtils.setPacketGlowing(this.playerList, this.uuidList, NamedTextColor.RED);
		this.red = true;

		indicator.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_GLOWING_MASK);

		indicator.setMetadata(MetaIndex.DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ, 1);
		indicator.setBillboard(MetaIndex.DisplayBillboardOption.CENTRE);

		ItemStack bukkitItem = new ItemStack(Material.CLOCK);
		indicator.setMetadata(MetaIndex.ITEM_DISPLAY_ITEM_OBJ, CraftItemStack.asNMSCopy(bukkitItem));
		indicator.updateMetadataPacket();

		indicator.respawn();
	}

	void remove() {
		this.indicator.remove();
		GlowUtils.setPacketGlowing(this.playerList, this.uuidList, null);
	}

	void setRed(boolean red) {
		if (!red && this.red) {
			GlowUtils.setPacketGlowing(this.playerList, this.uuidList, NamedTextColor.YELLOW);
			this.red = false;
		}
		else if (red && !this.red){
			GlowUtils.setPacketGlowing(this.playerList, this.uuidList, NamedTextColor.RED);
			this.red = true;
		}
	}

	void updatePos(Location location) {
		indicator.translate(location.toVector());
		indicator.move(location);
	}
}

package me.toomuchzelda.teamarenapaper.utils;

import io.netty.buffer.Unpooled;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.scoreboard.CraftScoreboardTranslations;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * @author jacky
 */
public class ScoreboardUtils {
	private static Constructor<ClientboundSetPlayerTeamPacket> TEAM_PACKET_CTOR;

	public static void sendPacket(Player player, Packet<?> packet) {
		((CraftPlayer) player).getHandle().connection.send(packet);
	}

	private static void initTeamPacketConstructor() {
		if (TEAM_PACKET_CTOR == null) {
			try {
				TEAM_PACKET_CTOR = ClientboundSetPlayerTeamPacket.class
						.getDeclaredConstructor(String.class, int.class, Optional.class, Collection.class);
				TEAM_PACKET_CTOR.setAccessible(true);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void sendTeamInfoPacket(Player player, String name, boolean update,
										  Component displayName, NamedTextColor color,
										  Component prefix, Component suffix, Collection<String> entries) {
		initTeamPacketConstructor();
		// thats crazy
		var parametersBuf = new FriendlyByteBuf(Unpooled.buffer());
		parametersBuf.writeComponent(displayName);
		parametersBuf.writeByte(3);
		parametersBuf.writeUtf("never");
		parametersBuf.writeUtf("never");
		parametersBuf.writeEnum(PaperAdventure.asVanilla(color));
		parametersBuf.writeComponent(prefix);
		parametersBuf.writeComponent(suffix);
		var parameters = new ClientboundSetPlayerTeamPacket.Parameters(parametersBuf);
		try {
			var packet = TEAM_PACKET_CTOR.newInstance(name, update ? 2 : 0, Optional.of(parameters), entries);
			sendPacket(player, packet);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void sendTeamPacket(Player player, String name, boolean leave, org.bukkit.entity.Entity... entities) {
		initTeamPacketConstructor();

		try {
			Collection<String> collection;
			if (entities.length == 1) {
				collection = Collections.singletonList(((CraftEntity) entities[0]).getHandle().getScoreboardName());
			} else {
				collection = Arrays.stream(entities)
						.map(entity -> ((CraftEntity) entity).getHandle().getScoreboardName())
						.toList();
			}
			var packet = TEAM_PACKET_CTOR.newInstance(name, leave ? 4 : 3, Optional.empty(), collection);
			sendPacket(player, packet);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void sendObjectivePacket(Player player, String objective, Component displayName, boolean update) {
		var fakeObjective = new Objective(null, objective, ObjectiveCriteria.DUMMY,
				PaperAdventure.asVanilla(displayName), ObjectiveCriteria.RenderType.INTEGER);
		var packet = new ClientboundSetObjectivePacket(fakeObjective, update ? 2 : 0);
		sendPacket(player, packet);
	}

	public static void sendDisplayObjectivePacket(Player player, @Nullable String objective, DisplaySlot slot) {
		var packetBuf = new FriendlyByteBuf(Unpooled.buffer());
		packetBuf.writeByte(CraftScoreboardTranslations.fromBukkitSlot(slot));
		packetBuf.writeUtf(objective == null ? "" : objective);
		var packet = new ClientboundSetDisplayObjectivePacket(packetBuf);
		sendPacket(player, packet);
	}

	public static void sendSetScorePacket(Player player, boolean remove, String objective, String entry, int score) {
		var packet = new ClientboundSetScorePacket(
				remove ? ServerScoreboard.Method.REMOVE : ServerScoreboard.Method.CHANGE,
				objective, entry, score);
		sendPacket(player, packet);
	}
}
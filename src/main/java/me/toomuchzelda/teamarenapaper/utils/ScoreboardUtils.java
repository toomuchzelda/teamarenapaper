package me.toomuchzelda.teamarenapaper.utils;

import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboardTranslations;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author jacky
 */
public class ScoreboardUtils {
	private static MethodHandle TEAM_PACKET_CTOR;

	public static void sendPacket(Player player, Packet<?> packet) {
		PlayerUtils.sendPacket(player, packet);
	}

	private static void initTeamPacketConstructor() {
		if (TEAM_PACKET_CTOR == null) {
			try {
				MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(ClientboundSetPlayerTeamPacket.class, MethodHandles.lookup());
				TEAM_PACKET_CTOR = lookup.findConstructor(
					ClientboundSetPlayerTeamPacket.class,
					MethodType.methodType(void.class, String.class, int.class, Optional.class, Collection.class)
				);
			} catch (IllegalAccessException | NoSuchMethodException ex) {
				throw new AssertionError("Constructor for SetPlayerTeamPacket not found", ex);
			}
		}
	}

	private static ClientboundSetPlayerTeamPacket.Parameters createParameter(Component displayName, NamedTextColor color,
																			 Component prefix, Component suffix) {
		// create a fake scoreboard team to return desired values
		return new ClientboundSetPlayerTeamPacket.Parameters(new PlayerTeam(null, "") {
			@Override
			public net.minecraft.network.chat.Component getDisplayName() {
				return PaperAdventure.asVanilla(displayName);
			}

			@Override
			public int packOptions() {
				return 3;
			}

			@Override
			public Visibility getNameTagVisibility() {
				return Visibility.NEVER;
			}

			@Override
			public CollisionRule getCollisionRule() {
				return CollisionRule.NEVER;
			}

			@Override
			public ChatFormatting getColor() {
				return ChatFormatting.valueOf(color.toString().toUpperCase(Locale.ENGLISH));
			}

			@Override
			public net.minecraft.network.chat.Component getPlayerPrefix() {
				return PaperAdventure.asVanilla(prefix);
			}

			@Override
			public net.minecraft.network.chat.Component getPlayerSuffix() {
				return PaperAdventure.asVanilla(suffix);
			}
		});
	}

	public static void sendTeamInfoPacket(Player player, String name, boolean update,
										  Component displayName, NamedTextColor color,
										  Component prefix, Component suffix, Collection<String> entries) {
		initTeamPacketConstructor();
		// that's crazy
		var parameters = createParameter(displayName, color, prefix, suffix);
		try {
			var packet = (ClientboundSetPlayerTeamPacket) TEAM_PACKET_CTOR.invoke(name, update ? 2 : 0, Optional.of(parameters), entries);
			sendPacket(player, packet);
		} catch (Throwable e) {
			throw new RuntimeException("Failed to construct Team Info packet", e);
		}
	}

	public static void sendTeamPacket(Player player, String name, boolean leave, org.bukkit.entity.Entity... entities) {
		String[] names = new String[entities.length];
		for (int i = 0; i < entities.length; i++) {
			names[i] = ((CraftEntity) entities[i]).getHandle().getScoreboardName();
		}
		sendTeamPacket(player, name, leave, names);
	}


	public static void sendTeamPacket(Player player, String name, boolean leave, String... names) {
		initTeamPacketConstructor();

		try {
			Collection<String> collection = List.of(names);
			var packet = (ClientboundSetPlayerTeamPacket) TEAM_PACKET_CTOR.invoke(name, leave ? 4 : 3, Optional.empty(), collection);
			sendPacket(player, packet);
		} catch (Throwable e) {
			throw new RuntimeException("Failed to construct Team packet", e);
		}
	}

	public static void sendObjectivePacket(Player player, String objective, Component displayName, boolean update) {
		var fakeObjective = new Objective(null, objective, ObjectiveCriteria.DUMMY,
				PaperAdventure.asVanilla(displayName), ObjectiveCriteria.RenderType.INTEGER, false ,null);
		var packet = new ClientboundSetObjectivePacket(fakeObjective, update ? 2 : 0);
		sendPacket(player, packet);
	}

	public static void sendDisplayObjectivePacket(Player player, @Nullable String objective, DisplaySlot slot) {
		net.minecraft.world.scores.DisplaySlot slotId = CraftScoreboardTranslations.fromBukkitSlot(slot);
		Objective fakeObjective;
		if (objective == null)
			fakeObjective = null;
		else
			fakeObjective = new Objective(null, objective, ObjectiveCriteria.DUMMY,
				net.minecraft.network.chat.Component.empty(), ObjectiveCriteria.RenderType.INTEGER, false, null);

		var packet = new ClientboundSetDisplayObjectivePacket(slotId, fakeObjective);
		sendPacket(player, packet);
	}

	public static void sendResetScorePacket(Player player, String objective, String entry) {
		sendPacket(player, new ClientboundResetScorePacket(entry, objective));
	}

	public static void sendSetScorePacket(Player player, String objective, String entry, int score,
										  @Nullable Component numberFormat) {
		sendPacket(player, new ClientboundSetScorePacket(entry, objective, score, null,
			numberFormat == null ? BlankFormat.INSTANCE : new FixedFormat(PaperAdventure.asVanilla(numberFormat))));
	}
}
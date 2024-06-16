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
import org.bukkit.craftbukkit.scoreboard.CraftScoreboardTranslations;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author jacky
 */
public class ScoreboardUtils {

	private static void sendPacket(Player player, Packet<?> packet) {
		PlayerUtils.sendPacket(player, packet);
	}

	public static void sendTeamInfoPacket(Player player, String name, boolean update,
										  Component displayName, NamedTextColor color,
										  Component prefix, Component suffix, Collection<String> entries) {
		// that's crazy
		try {
			var packet = ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(new PlayerTeam(null, name) {
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
					return PaperAdventure.asVanilla(color);
				}

				@Override
				public net.minecraft.network.chat.Component getPlayerPrefix() {
					return PaperAdventure.asVanilla(prefix);
				}

				@Override
				public net.minecraft.network.chat.Component getPlayerSuffix() {
					return PaperAdventure.asVanilla(suffix);
				}

				@Override
				public Collection<String> getPlayers() {
					return entries;
				}
			}, !update);
			sendPacket(player, packet);
		} catch (Throwable e) {
			throw new RuntimeException("Failed to construct Team Info packet", e);
		}
	}

	public static void sendTeamPacket(Player player, String name, boolean leave, org.bukkit.entity.Entity... entities) {
		String[] names = new String[entities.length];
		for (int i = 0; i < entities.length; i++) {
			names[i] = entities[i].getScoreboardEntryName();
		}
		sendTeamPacket(player, name, leave, names);
	}


	public static void sendTeamPacket(Player player, String name, boolean leave, String... names) {
		try {
			Collection<String> collection = List.of(names);
			var packet = ClientboundSetPlayerTeamPacket.createMultiplePlayerPacket(
				new PlayerTeam(null, name), // only team.getName() is used
				collection, leave ? ClientboundSetPlayerTeamPacket.Action.REMOVE : ClientboundSetPlayerTeamPacket.Action.ADD);
			sendPacket(player, packet);
		} catch (Throwable e) {
			throw new RuntimeException("Failed to construct Team packet", e);
		}
	}

	public static void sendObjectivePacket(Player player, String objective, Component displayName, boolean update) {
		var fakeObjective = new Objective(null, objective, ObjectiveCriteria.DUMMY,
				PaperAdventure.asVanilla(displayName), ObjectiveCriteria.RenderType.INTEGER, false ,null);
		var packet = new ClientboundSetObjectivePacket(fakeObjective, update ? ClientboundSetObjectivePacket.METHOD_CHANGE : ClientboundSetObjectivePacket.METHOD_ADD);
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
		sendPacket(player, new ClientboundSetScorePacket(entry, objective, score, Optional.empty(),
			numberFormat == null ? Optional.of(BlankFormat.INSTANCE) : Optional.of(new FixedFormat(PaperAdventure.asVanilla(numberFormat)))));
	}
}
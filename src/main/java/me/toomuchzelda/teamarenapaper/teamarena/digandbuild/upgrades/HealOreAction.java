package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades;

import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.TeamLifeOres;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.StatusOreType;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class HealOreAction implements StatusOreType.BuffAction
{
	private static final Component CANNOT_HEAL_DEAD_ORE = Component.text("Can't heal dead Life Ore!", TextColors.ERROR_RED);
	private static final Component CANNOT_HEAL_MAXED_HEALTH = Component.text("Your ore is too healthy!", TextColors.ERROR_RED);

	@Override
	public int buff(final Player redeemer, final int required, final int itemsUsed, final TeamLifeOres ore,
					final DigAndBuild gameInstance) {
		if (ore.isDead()) {
			Component t = CANNOT_HEAL_DEAD_ORE;
			if (ThreadLocalRandom.current().nextDouble() <= 0.01d)
				t = t.append(Component.text(" ☭"));
			redeemer.sendMessage(t);
			return 0;
		}
		else if (ore.getHealth() == TeamLifeOres.MAX_HEALTH) {
			redeemer.sendMessage(CANNOT_HEAL_MAXED_HEALTH);
			return 0;
		}
		else if (itemsUsed >= required) {
			ore.playHealEffect();
			ore.setHealth(ore.getHealth() + 1);

			return required;
		}
		else {
			redeemer.sendMessage(Component.text("You need " +
				(required - itemsUsed) + " more of these to heal your Life Ore", NamedTextColor.RED));

			return 0;
		}
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.statusorebuffactions;

import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.LifeOre;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.StatusOreType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class HealOreAction implements StatusOreType.BuffAction
{
	private static final Component CANNOT_HEAL_DEAD_ORE = Component.text("Can't heal dead Life Ore!");

	@Override
	public int buff(final Player redeemer, final int required, final int itemsUsed, final LifeOre ore,
					final DigAndBuild gameInstance) {
		if (ore.isDead()) {
			Component t = CANNOT_HEAL_DEAD_ORE;
			if (ThreadLocalRandom.current().nextDouble() <= 0.01d)
				t = t.append(Component.text(" ☭"));
			redeemer.sendMessage(t);
			return 0;
		}
		else if (itemsUsed >= required) {
			ore.playHealEffect();
			boolean b = ore.setHealth(ore.getHealth() + 1); assert b;

			return itemsUsed - required;
		}
		else {
			redeemer.sendMessage(Component.text("You need " +
				(required - itemsUsed) + " more of these to heal your Life Ore", NamedTextColor.RED));

			return 0;
		}
	}
}

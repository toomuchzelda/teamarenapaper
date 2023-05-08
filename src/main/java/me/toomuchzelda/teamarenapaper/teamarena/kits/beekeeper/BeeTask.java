package me.toomuchzelda.teamarenapaper.teamarena.kits.beekeeper;

import com.destroystokyo.paper.entity.ai.Goal;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Bee;

import java.util.Arrays;
import java.util.List;

/**
 * Class containing logic of the different BeeTasks.
 * @author toomuchzelda
 */
public abstract class BeeTask
{
	private final List<Goal<Bee>> mobGoals;

	public BeeTask(Bee beeEntity, Goal<Bee>... beeAi) {
		this.mobGoals = Arrays.asList(beeAi);
	}

	public List<Goal<Bee>> getMobGoals() {
		return this.mobGoals;
	}

	public boolean isDone() {
		boolean done = false;
		for (Goal<Bee> goal : this.mobGoals) {
			done = done || !goal.shouldStayActive();
		}

		return done;
	}

	abstract Component getActionBarPart();
}

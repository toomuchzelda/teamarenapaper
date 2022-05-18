package me.toomuchzelda.teamarenapaper.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;

public class GlobalObjective
{
	public final Map<String, Integer> entries;
	
	public final String name;
	public final String criteria;
	private Component displayName;
	private RenderType renderType;
	private DisplaySlot slot;
	
	public GlobalObjective(String name, String criteria, Component displayName, RenderType renderType) {
		this.name = name;
		this.criteria = criteria;
		this.displayName = displayName;
		this.renderType = renderType;
		
		this.entries = new HashMap<>();
	}
	
	public GlobalObjective(Objective obj) {
		this.name = obj.getName();
		this.criteria = obj.getCriteria();
		this.displayName = obj.displayName();
		this.renderType = obj.getRenderType();
		
		this.entries = new HashMap<>();
	}
	
	private void modifyProperty(Consumer<Objective> consumer) {
		PlayerScoreboard.getScoreboards().forEach(board -> board.modifyLocalObjective(this, consumer));
	}
	
	public String getName() {
		return name;
	}
	
	public Component getDisplayName() {
		return this.displayName;
	}
	
	public void setDisplayName(Component displayName) {
		this.displayName = displayName;
		modifyProperty(objective -> objective.displayName(displayName));
	}
	
	public RenderType getRenderType() {
		return this.renderType;
	}
	
	public void setRenderType(RenderType type) {
		this.renderType = type;
		modifyProperty(objective -> objective.setRenderType(type));
	}
	
	public DisplaySlot getDisplaySlot() {
		return this.slot;
	}
	
	public void setDisplaySlot(DisplaySlot slot) {
		this.slot = slot;
		modifyProperty(objective -> objective.setDisplaySlot(slot));
	}
	
	public void setScore(@NotNull String entryName, int score) {
		this.entries.put(entryName, score);
		modifyProperty(objective -> objective.getScore(entryName).setScore(score));
	}
	
	public void resetScore(@NotNull String entryName) {
		this.entries.remove(entryName);
		modifyProperty(objective -> objective.getScore(entryName).resetScore());
	}
	
	public void resetAllScores() {
		var iter = entries.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<String, Integer> entry = iter.next();
			String entryName = entry.getKey();
			iter.remove();
			modifyProperty(objective -> objective.getScore(entryName).resetScore());
		}
	}
	
	public Map<String, Integer> getScores() {
		return Collections.unmodifiableMap(entries);
	}
}

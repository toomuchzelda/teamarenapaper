package me.toomuchzelda.teamarenapaper.utils.packetentities;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class SpeechBubbleHologram extends PacketHologram {

	public enum ViewOptions {
		ALL((viewed, viewer) -> true),
		TEAM_ONLY((viewed, viewer) -> {
			return Main.getPlayerInfo(viewed).team.getPlayerMembers().contains(viewer);
		}),
		NONE((viewed, viewer) -> false);

		public final BiPredicate<Player, Player> rule;
		ViewOptions(BiPredicate<Player, Player> rule) {
			this.rule = rule;
		}
	}

	public interface MovementFunc {
		// Position is relative to where it was spawned
		Vector getNextPos(int age, int lifeTime);
	}

	public static class DamageIndicatorMovementFunc implements MovementFunc {
		private final Vector horizontalDirection;
		private final double speed;
		private final double height;
		public DamageIndicatorMovementFunc(double speed, double height) {
			this.horizontalDirection = new Vector(MathUtils.randomRange(-0.6d, 0.6d), 0, MathUtils.randomRange(-0.6d, 0.6d)).normalize();
			this.speed = speed;
			this.height = height;
		}
		@Override public Vector getNextPos(int age, int lifeTime) {
			double yPos = Math.sin((double) age / this.speed) * this.height;
			double horiPercent = ((double) age * 0.03);
			double x = horizontalDirection.getX() * horiPercent;
			double z = horizontalDirection.getZ() * horiPercent;
			return new Vector(x, yPos, z);
		}
	}

	private int age = 0;
	private int liveTime = 15;
	private final Location startLoc;
	private final MovementFunc func;

	private static Location spawnLoc(Player p) { // Because I can't put code before super()
		Location spawnLoc = p.getLocation();
		spawnLoc.add(0, MathUtils.randomRange(1.4, 1.85), 0);
		return spawnLoc;
	}

	public SpeechBubbleHologram(Player popper, Component text, MovementFunc func) {
		this(spawnLoc(popper), null, viewer -> {
				return Main.getPlayerInfo(viewer).getPreference(Preferences.SEE_SPEECH_POPUPS).rule.test(popper, viewer);
		}, text, func);
	}

	public SpeechBubbleHologram(Location spawnLoc, Set<Player> viewers, Component text) {
		this(spawnLoc, viewers, null, text, new DamageIndicatorMovementFunc(4d, 1d));
	}

	public SpeechBubbleHologram(Location spawnLoc, Set<Player> viewers, Predicate<Player> rule, Component text,
								MovementFunc func) {
		super(spawnLoc, viewers, rule , text);

		this.startLoc = location.clone();
		if (func == null) {
			func = new DamageIndicatorMovementFunc(4d, 1d); // Fallback
			Main.logger().warning("No MovementFunc provided");
			Thread.dumpStack();
		}
		this.func = func;
	}

	@Override
	public void tick() {
		Vector nextPos = this.func.getNextPos(this.age, this.liveTime);
		this.move(startLoc.clone().add(nextPos));

		if(age++ >= this.liveTime) {
			this.remove();
		}
	}

	public void setLiveTime(int ticks) {
		this.liveTime = ticks;
	}
}

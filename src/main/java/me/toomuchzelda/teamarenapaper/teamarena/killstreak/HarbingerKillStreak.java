package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.*;


public class HarbingerKillStreak extends KillStreak
{
	private static final TextColor color = TextColor.color(232, 210, 7);

	HarbingerKillStreak() {
		super("Harbinger", "Rain hell on your enemies", color, null, new HarbingerAbility());

		this.crateItemType = Material.TNT;
		this.crateBlockType = Material.TNT;
	}

	@Override
	boolean isDeliveredByCrate() {
		return true;
	}

	private static final Map<Player, Location> crateLocs = new HashMap<>();
	@Override
	public void onCrateLand(Player player, Location destination) {
		//crate.setDone();
		crateLocs.put(player, destination);
		this.giveStreak(player, Main.getPlayerInfo(player));
	}

	private static class HarbingerAbility extends Ability {

		private static final int BLUE_PARTICLE_FLY_TIME = 20;
		private static final int STRIKE_AMOUNT = 10;
		private static final int STRIKE_HEIGHT = 36;
		private static final int STRIKE_WIDTH = 20;
		private static final int STRIKE_DURATION = 10;
		private static final int LEAVE_AFTERMATH_DURATION = 40;
		private static final Material STRIKE_INNER_MATERIAL = Material.LAVA;
		private static final Material STRIKE_OUTER_MATERIAL = Material.YELLOW_STAINED_GLASS;

		private static final Vector ANGLE = new Vector(0.2d, 1d, 0.2d).multiply(STRIKE_HEIGHT);

		// Class to represent one Harbinger "strike"
		private static class HarbingerStrike {
			Vector originPoint; // Point it was summoned at
			int startTime;
			int strikeTime;
			Vector startPos;
			Vector destination;
			LinkedHashMap<BlockVector, BlockData> changedBlocks; // Record changed blocks so can change them back

			public HarbingerStrike(Vector originPoint, int startTime, Vector startPos, Vector destination) {
				this.originPoint = originPoint;
				this.startTime = startTime;
				this.strikeTime = MathUtils.randomMax(20) + BLUE_PARTICLE_FLY_TIME;
				this.startPos = startPos;
				this.destination = destination;
				this.changedBlocks = new LinkedHashMap<>();
			}

			public int getStrikeTime() {
				return this.startTime + this.strikeTime;
			}

			public int getStrikeFinishedTime() {
				return this.startTime + this.strikeTime + STRIKE_DURATION;
			}

			public int getFinishedTime() {
				return this.getStrikeFinishedTime() + LEAVE_AFTERMATH_DURATION;
			}
		}

		private static final Map<Player, Set<HarbingerStrike>> CURRENT_STRIKES = new LinkedHashMap<>();

		@Override
		public void giveAbility(Player player) {
			Location loc = crateLocs.remove(player);
			if(loc == null)
				loc = player.getLocation();

			Vector origin = loc.toVector();
			final int currentTick = TeamArena.getGameTick();
			Set<HarbingerStrike> strikes = CURRENT_STRIKES.computeIfAbsent(player, player1 -> new LinkedHashSet<>());
			// Decide on the harbinger locations
			int index = MathUtils.random.nextInt(100) + 1;
			for(int i = 0; i < STRIKE_AMOUNT; i++) {
				final double heightRad = (double) STRIKE_HEIGHT / 2d;

				double[] sequence = MathUtils.haltonSequence2d(index++);
				Location destination = loc.clone().add(
						(sequence[0] - 0.5d) * (double) STRIKE_WIDTH,
						-heightRad,
						(sequence[1] - 0.5d) * (double) STRIKE_WIDTH
				);

				Vector startPos = destination.toVector().add(new Vector(0, STRIKE_HEIGHT, 0));
				//Vector startPos = destination.toVector().add(ANGLE);

				HarbingerStrike strike = new HarbingerStrike(origin, currentTick, startPos, destination.toVector());
				strikes.add(strike);
			}
		}

		@Override
		public void removeAbility(Player player) {
			Set<HarbingerStrike> strikes = CURRENT_STRIKES.remove(player);
			World world = player.getWorld();
			// Reset all the blocks in the strikes
			for(HarbingerStrike strike : strikes) {
				for(var entry : strike.changedBlocks.entrySet()) {
					BlockVector vec = entry.getKey();
					world.setBlockData(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ(), entry.getValue());
				}
			}
		}

		@Override
		public void onTick() {
			List<Player> finished = new LinkedList<>();
			final int currentTick = TeamArena.getGameTick();
			var iter = CURRENT_STRIKES.entrySet().iterator();
			while(iter.hasNext()) {
				var entry = iter.next();
				final Player player = entry.getKey();
				final World world = player.getWorld();
				for(HarbingerStrike strike : entry.getValue()) {
					final int diff = currentTick - strike.startTime;
					if(diff < BLUE_PARTICLE_FLY_TIME) { // Still in particle fly stage
						blueParticle(diff, strike, world);
					}
					// During strike time
					else if(currentTick >= strike.getStrikeTime() && currentTick < strike.getStrikeFinishedTime()) {
						// Get the diff between now and the strike time starting, not the time the crate landed
						int strikeDiff = currentTick - strike.getStrikeTime();
						strike(strikeDiff, strike, world);
					}
					// == to run only once
					else if(currentTick == strike.getFinishedTime()) {
						Bukkit.broadcastMessage("run once");
						disappear(strike, world);
					}
					else {
						finished.add(player);
					}
				}
			}

			//finished.forEach(this::removeAbility);
		}

		private static void blueParticle(int diff, HarbingerStrike strike, World world) {
			if(diff % 2 == 0) return;

			float progress = (float) diff / (float) BLUE_PARTICLE_FLY_TIME;
			if(progress < 0f || progress > 1f) {
				Main.logger().warning("progress out of range " + progress);
			}

			Vector particlePos = strike.startPos.clone().subtract(strike.originPoint).multiply(progress);
			particlePos.add(strike.originPoint);

			ParticleUtils.colouredRedstone(particlePos.toLocation(world), Color.BLUE, 3f, 3f);
		}

		private static final BlockFace[] FACES_TO_CHANGE = new BlockFace[] {
				BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
				BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST,
				BlockFace.NORTH_EAST, BlockFace.NORTH_WEST
		};
		private static void strike(final int diff, HarbingerStrike strike, World world) {
			float start = (float) diff / (float) STRIKE_DURATION;
			if(start < 0f || start > 1f) {
				Thread.dumpStack();
			}
			float end = (float) (diff + 1) / (float) STRIKE_DURATION;
			if(end < 0f || end > 1f) {
				Thread.dumpStack();
			}

			final float amount = (end - start) * ((float) STRIKE_HEIGHT);

			Vector posDiff = strike.destination.clone().subtract(strike.startPos);
			Vector lineToCover = posDiff.clone().multiply(amount);
			// Now move it to the appropriate point along posDiff
			posDiff.multiply(start);
			Vector lineStart = strike.startPos.clone().add(posDiff);

			// lineStart: absolute position of the start of the line we are covering this tick
			// lineToCover: relative arrow of the line itself.

			// Loop over the line one block at a time and set blocks
			//Bukkit.broadcastMessage("amount " + amount);
			for(float i = 0; i < amount; i += 0.9f) {
				BlockVector currentPoint = lineStart.clone().add(lineToCover.clone().normalize().multiply(i)).toBlockVector();
				Block existingBlock = world.getBlockAt(currentPoint.getBlockX(), currentPoint.getBlockY(), currentPoint.getBlockZ());
				// don't override previously put data
				strike.changedBlocks.putIfAbsent(currentPoint, existingBlock.getBlockData());

				// Place the blocks
				//if(!existingBlock.getType().isAir()) { // Play block breaking particle effect
				//	world.spawnParticle(Particle.BLOCK_DUST, currentPoint.toLocation(world), 1, existingBlock.getBlockData());
				//}

				existingBlock.setType(STRIKE_INNER_MATERIAL);
				for(BlockFace face : FACES_TO_CHANGE) {
					Block relativeBlock = existingBlock.getRelative(face);
					BlockData relativeData = relativeBlock.getBlockData();
					strike.changedBlocks.putIfAbsent(relativeBlock.getLocation().toVector().toBlockVector(), relativeData);
					// Play block breaking particle effect
					// Mod 2 to reduce laggy particle effects
					if(!relativeData.getMaterial().isAir()) {
						world.spawnParticle(Particle.BLOCK_DUST, relativeBlock.getLocation(), 20, relativeData);
					}
					relativeBlock.setType(STRIKE_OUTER_MATERIAL);
				}

				// Harm enemies standing nearby
				// TODO
			}
		}

		private static final BlockData AIR_DATA = Material.AIR.createBlockData();
		private static void disappear(HarbingerStrike strike, World world) {
			for(var entry : strike.changedBlocks.entrySet()) {
				BlockVector vector = entry.getKey();
				BlockData data = entry.getValue().getMaterial().isAir() ? entry.getValue() : AIR_DATA;
				world.setBlockData(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ(), data);
			}
		}
	}
}

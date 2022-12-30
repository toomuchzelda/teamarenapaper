package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;


public class HarbingerKillStreak extends KillStreak
{
	private static final TextColor color = TextColor.color(232, 210, 7);

	HarbingerKillStreak() {
		super("Harbinger", "Rain hellish destruction on your enemies", color, null, new HarbingerAbility());

		this.crateItemType = Material.TNT;
		this.crateBlockType = Material.TNT;
	}

	@Override
	public boolean isDeliveredByCrate() {
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
		private static final double STRIKE_MAX_DAMAGE = 10d;
		private static final double STRIKE_MIN_DAMAGE = 6d;
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
			LinkedHashMap<BlockCoords, BlockData> changedBlocks; // Record changed blocks so can change them back

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

				//Vector startPos = destination.toVector().add(new Vector(0, STRIKE_HEIGHT, 0));
				Vector startPos = destination.toVector().add(ANGLE);

				HarbingerStrike strike = new HarbingerStrike(origin, currentTick, startPos, destination.toVector());
				strikes.add(strike);
			}
		}

		@Override
		public void removeAbility(Player player) {
			// Don't remove the strikes here, let the whole thing play out.
			// Cleaning up is handled in this.onTick()

			/*Set<HarbingerStrike> strikes = CURRENT_STRIKES.remove(player);
			World world = player.getWorld();
			// Reset all the blocks in the strikes
			for(HarbingerStrike strike : strikes) {
				for(var entry : strike.changedBlocks.entrySet()) {
					BlockCoords vec = entry.getKey();
					world.setBlockData(vec.x(), vec.y(), vec.z(), entry.getValue());
				}
			}*/
		}

		@Override
		public void unregisterAbility() {
			CURRENT_STRIKES.clear();
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
				for (Iterator<HarbingerStrike> strikeIter = entry.getValue().iterator(); strikeIter.hasNext(); ) {
					HarbingerStrike strike = strikeIter.next();
					final int diff = currentTick - strike.startTime;
					if (diff < BLUE_PARTICLE_FLY_TIME) { // Still in particle fly stage
						blueParticle(diff, strike, world);
					}
					// During strike time
					else if (currentTick >= strike.getStrikeTime() && currentTick < strike.getStrikeFinishedTime()) {
						// Get the diff between now and the strike time starting, not the time the crate landed
						int strikeDiff = currentTick - strike.getStrikeTime();
						strike(strikeDiff, strike, world, player);
					}
					// == to run only once
					else if (currentTick == strike.getFinishedTime()) {
						disappearAndRestore(strike, world);
						strikeIter.remove();
					}
				}
				if(entry.getValue().size() == 0) { // All harbinger strikes have finished, remove
					iter.remove();
					finished.add(player);
				}
			}

			finished.forEach(player -> {
				Ability.removeAbility(player, this, Main.getPlayerInfo(player));
				//Main.logger().info(Main.getPlayerInfo(player).abilities.toString());
			});

			//Main.logger().info(CURRENT_STRIKES.toString());
		}

		private static void blueParticle(int diff, HarbingerStrike strike, World world) {
			if(diff % 2 == 0) return;

			float progress = (float) diff / (float) BLUE_PARTICLE_FLY_TIME;
			if(progress < 0f || progress > 1f) {
				Main.logger().warning("progress out of range " + progress);
			}

			Vector particlePos = strike.startPos.clone().subtract(strike.originPoint).multiply(progress);
			particlePos.add(strike.originPoint);

			Location loc = particlePos.toLocation(world);
			ParticleUtils.colouredRedstone(loc, Color.BLUE, 3f, 3f);
			world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BIT, 0.5f, 0.5f);
		}

		private static final BlockFace[] FACES_TO_CHANGE = new BlockFace[] {
				BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
				BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST,
				BlockFace.NORTH_EAST, BlockFace.NORTH_WEST
		};
		private static void strike(final int diff, final HarbingerStrike strike, final World world, final Player user) {
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
			List<TeamArenaExplosion> explosions = new ArrayList<>(((int) amount) + 1);
			for(float i = 0; i < amount; i += 0.9f) {
				Vector currentPointVec = lineStart.clone().add(lineToCover.clone().normalize().multiply(i));
				BlockCoords currentPoint = new BlockCoords(currentPointVec);
				Block existingBlock = world.getBlockAt(currentPoint.x(), currentPoint.y(), currentPoint.z());

				// Place the blocks and play effects
				// Don't play block break particles for the one in the middle - noone can see it anyway
				// don't override previously put data
				// Don't do placements over other neighbouring strike blocks
				Material middleMat = existingBlock.getType();
				if(middleMat != STRIKE_INNER_MATERIAL && middleMat != STRIKE_OUTER_MATERIAL) {
					if (strike.changedBlocks.putIfAbsent(new BlockCoords(existingBlock), existingBlock.getBlockData()) == null) {
						existingBlock.setType(STRIKE_INNER_MATERIAL);
					}
				}

				for(BlockFace face : FACES_TO_CHANGE) {
					Block relativeBlock = existingBlock.getRelative(face);
					BlockData relativeData = relativeBlock.getBlockData();
					Location relativeBlockLoc = relativeBlock.getLocation();
					BlockCoords coords = new BlockCoords(relativeBlock);
					// Play block breaking particle effect and sound
					// Do it only if it's the first time this block is being overriden (blocks get overwritten multiple
					// times by the glass effect
					Material relMat = relativeData.getMaterial();
					if(relMat != STRIKE_INNER_MATERIAL && relMat != STRIKE_OUTER_MATERIAL) {
						if (strike.changedBlocks.putIfAbsent(coords, relativeData) == null) {
							if (!relativeData.getMaterial().isAir()) {
								world.spawnParticle(Particle.BLOCK_DUST, relativeBlock.getLocation(), 20, relativeData);
								world.playSound(relativeBlockLoc, relativeData.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 2f, 1f);
							}

							world.playSound(relativeBlockLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 2f);
							relativeBlock.setType(STRIKE_OUTER_MATERIAL);
						}
					}
				}

				// Harm enemies standing nearby
				// TODO
				Location centreLoc = currentPointVec.toLocation(world);
				TeamArenaExplosion explosion = new TeamArenaExplosion(centreLoc, 7d, 4.5d, STRIKE_MAX_DAMAGE,
						STRIKE_MIN_DAMAGE, 2.5d, DamageType.HARBINGER, user) {
					@Override
					public void playExplosionSound() {}

					@Override
					public void playExplosionEffect() {}
				};

				explosions.add(explosion);
			}

			for(TeamArenaExplosion explosion : explosions) {
				explosion.explode();
			}
		}

		//private static final BlockData AIR_DATA = Material.AIR.createBlockData();
		private static void disappearAndRestore(HarbingerStrike strike, World world) {
			for (Iterator<Map.Entry<BlockCoords, BlockData>> iter = strike.changedBlocks.entrySet()
					.iterator(); iter.hasNext(); ) {
				Map.Entry<BlockCoords, BlockData> entry = iter.next();
				BlockCoords vector = entry.getKey();
				//BlockData data = entry.getValue().getMaterial().isAir() ? entry.getValue() : AIR_DATA;
				//world.setBlockData(vector.x(), vector.y(), vector.z(), data);

				BlockData data = entry.getValue();
				world.setBlockData(vector.x(), vector.y(), vector.z(), data);

				iter.remove();

				Location loc = new Location(world, vector.x(), vector.y(), vector.z());
				if (MathUtils.random.nextBoolean()) { // poofy smokey particle effect
					loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 14);
				}
				else {
					loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 3);
				}
			}
		}
	}
}

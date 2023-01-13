package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate.CratePayload;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class HarbingerKillStreak extends CratedKillStreak
{
	private static final TextColor color = TextColor.color(232, 210, 7);

	private static final Component SELF_WARNING = Component.text("You're not safe near your own Harbinger! Move away!", TextColors.ERROR_RED);
	private static final Component TEAM_WARNING = Component.text(" has called a Harbinger strike! It's not safe for you to be here. Move away!");
	private static final Component ENEMY_WARNING = Component.text(" has called a Harbinger strike! You might want to run.", TextColors.ERROR_RED);
	private static final double WARNING_DIST_SQR = 22d * 22d;

	HarbingerKillStreak() {
		super("Harbinger", "Rain hellish destruction on your enemies", color, null, Material.TNT, new HarbingerAbility());
	}

	private static final Map<Player, Location> crateLocs = new HashMap<>();
	@Override
	public void onCrateLand(Player player, Location destination) {
		super.onCrateLand(player, destination);
		crateLocs.put(player, destination);
		this.giveStreak(player, Main.getPlayerInfo(player));
	}

	@Override
	public void onFireworkFinish(Player player, Location destination, Crate crate) {
		TeamArenaTeam team = Main.getPlayerInfo(player).team;
		Component component = player.playerListName().append(ENEMY_WARNING);
		for(Player p : Main.getGame().getPlayers()) {
			if(team.getPlayerMembers().contains(p)) continue;

			if(p.getLocation().distanceSquared(destination) <= WARNING_DIST_SQR) {
				p.sendMessage(component);
			}
		}
	}

	@Override
	public @NotNull CratePayload getPayload(Player player, Location destination) {
		return new CratePayload.SimpleBlock(Material.TNT.createBlockData());
	}

	@Override
	public boolean isPayloadFragile(Player player, Location destination) {
		return false; // an explosive ordnance cannot be fragile, right?
	}

	@Override
	public void onCratePlace(Player player, Location destination) {
		TeamArenaTeam team = Main.getPlayerInfo(player).team;
		Component component = player.playerListName().append(TEAM_WARNING);
		for(Player p : Main.getGame().getPlayers()) {
			if(p == player) continue;
			if(!team.getPlayerMembers().contains(p)) continue;

			if(p.getLocation().distanceSquared(destination) <= WARNING_DIST_SQR) {
				p.sendMessage(component);
			}
		}
		player.sendMessage(SELF_WARNING);
	}

	private static class HarbingerAbility extends Ability {

		private static final int BLUE_PARTICLE_FLY_TIME = 20;
		private static final int STRIKE_AMOUNT = 11;
		private static final int STRIKE_HEIGHT = 46;
		private static final int STRIKE_WIDTH = 20;
		private static final int STRIKE_DURATION = 12;
		private static final int LEAVE_STRIKE_DURATION = 40;
		private static final int LEAVE_HOLES_DURATION = 10 * 20;
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
			Set<BlockCoords> changedBlocks; // Record changed blocks so can change them back

			public HarbingerStrike(Vector originPoint, int startTime, Vector startPos, Vector destination) {
				this.originPoint = originPoint;
				this.startTime = startTime;
				this.strikeTime = MathUtils.randomMax(20) + BLUE_PARTICLE_FLY_TIME;
				this.startPos = startPos;
				this.destination = destination;
				this.changedBlocks = new LinkedHashSet<>();
			}

			public int getStrikeTime() {
				return this.startTime + this.strikeTime;
			}

			public int getStrikeFinishedTime() {
				return this.startTime + this.strikeTime + STRIKE_DURATION;
			}

			public int getFinishedTime() {
				return this.getStrikeFinishedTime() + LEAVE_STRIKE_DURATION;
			}

			public int getFillHolesTime() {
				return this.getFinishedTime() + LEAVE_HOLES_DURATION;
			}
		}

		private static final Map<Player, Set<HarbingerStrike>> CURRENT_STRIKES = new LinkedHashMap<>();
		private static final Map<BlockCoords, BlockData> CHANGED_BLOCKS = new HashMap<>();

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
						disappear(strike, world);
					}
					else if (currentTick >= strike.getFillHolesTime()) {
						restoreBlocks(strike, world);
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

			//Main.logger().info("CURRENT_STRIKES: " + CURRENT_STRIKES.size());
			//Main.logger().info("CHANGED_BLOCKS: " + CHANGED_BLOCKS.size());
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
				{
					BlockCoords coords = new BlockCoords(existingBlock);
					if (CHANGED_BLOCKS.putIfAbsent(coords, existingBlock.getBlockData()) == null) {
						strike.changedBlocks.add(coords);
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
					// times by the glass effect otherwise)
					{
						if (CHANGED_BLOCKS.putIfAbsent(coords, relativeData) == null) {
							strike.changedBlocks.add(coords);
							if (!relativeData.getMaterial().isAir()) {
								world.spawnParticle(Particle.BLOCK_DUST, relativeBlock.getLocation(), 20, relativeData);
								world.playSound(relativeBlockLoc, relativeData.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 2f, 1f);
							}

							world.playSound(relativeBlockLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 2f);
							relativeBlock.setType(STRIKE_OUTER_MATERIAL);
						}
					}
				}

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

		private static final BlockData AIR_DATA = Material.AIR.createBlockData();
		private static void disappear(HarbingerStrike strike, World world) {
			for (Iterator<BlockCoords> iter = strike.changedBlocks.iterator();
				 iter.hasNext(); ) {
				BlockCoords vector = iter.next();
				BlockData data = CHANGED_BLOCKS.get(vector);
				if(data == null || !data.getMaterial().isAir()) {
					if(data == null)
						Thread.dumpStack();
					data = AIR_DATA;
				}

				world.setBlockData(vector.x(), vector.y(), vector.z(), data);

				//BlockData data = entry.getValue();
				//world.setBlockData(vector.x(), vector.y(), vector.z(), data);
				//iter.remove();

				Location loc = new Location(world, vector.x(), vector.y(), vector.z());
				if (MathUtils.random.nextBoolean()) { // poofy smokey particle effect
					loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 14);
				}
				else {
					loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 3);
				}
			}
		}

		private static void restoreBlocks(HarbingerStrike strike, World world) {
			for (Iterator<BlockCoords> iter = strike.changedBlocks.iterator();
				 iter.hasNext(); ) {

				BlockCoords coords = iter.next();
				BlockData data = CHANGED_BLOCKS.remove(coords);
				if(data == null) {
					Main.logger().severe("data in restoreBlocks is null");
					Thread.dumpStack();
					return;
				}

				world.setBlockData(coords.x(), coords.y(), coords.z(), data);

				iter.remove();
			}
		}
	}
}

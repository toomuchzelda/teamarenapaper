package me.toomuchzelda.teamarenapaper.teamarena;

import com.destroystokyo.paper.entity.Pathfinder;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.FakeHuman;
import me.toomuchzelda.teamarenapaper.utils.packetentities.SpeechBubbleHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Herobrine extends FakeHuman {

	private static final int SLEEP_TIME = 3 * 20;
	private static final double MAX_HEALTH = 100d;

	private enum TargetType {
		CHASE,
		SMITE,
		BEAT
	}

	private static final Component[] SCREAM_MESSAGES = new Component[] {
		Component.text("AAAHH!!!", NamedTextColor.DARK_RED, TextDecoration.BOLD),
		Component.text("HELP", NamedTextColor.YELLOW, TextDecoration.BOLD),
		Component.text("SCREAMM", NamedTextColor.YELLOW, TextDecoration.BOLD)
	};

	private final TeamArena game;
	private final Block shrine;

	private LivingEntity target;
	private TargetType targetType = null;
	private int sleepTime;

	private int behaviourTick;

	private record BridgePosition(Location loc, Map<Block, Material> blocks) {}
	private final List<BridgePosition> scaffoldPositions = new ArrayList<>();

	private Entity beatMount;
	private static final NamespacedKey BEAT_MARKER_KEY = new NamespacedKey(Main.getPlugin(), "herobrinebeatmarker");

	public Herobrine(TeamArena game, Block shrineTop, Location location, String name) {
		super(location, name);

		this.game = game;
		this.shrine = shrineTop;
		this.sleepTime = SLEEP_TIME;

		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(MAX_HEALTH);
		this.setHealth((float) MAX_HEALTH);
	}

	private void selectTarget() {
		TargetType goal = MathUtils.randomElement(TargetType.values());

		LivingEntity candidate = null;
		double distSqr = -1d;

		// try the player furthest away
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (this.game.isDead(p)) continue;
			double distToCandidate = EntityUtils.distanceSqr(this.bukkitEntity, p);
			if (distToCandidate > distSqr) {
				candidate = p;
				distSqr = distToCandidate;
			}
		}

		this.target = candidate;

		if (this.target != null) {
			this.targetType = goal;
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (this.sleepTime-- > 0) {
			return;
		}

		if (this.target != null) {
			if (!this.game.isDead(this.target)) {
				if (this.targetType == TargetType.CHASE)
					this.chaseTick();
				else if (this.targetType == TargetType.SMITE)
					this.smiteTick();
				else if (this.targetType == TargetType.BEAT)
					this.beatTick();

				this.behaviourTick++;
			}
			else {
				this.sleepTime = SLEEP_TIME;
				this.target = null;
				this.behaviourTick = 0;
				this.setMainHand(Material.AIR);
				this.setGravity(true);
				if (this.beatMount != null) {
					this.beatMount.remove();
					this.beatMount = null;
				}
			}
		}
		else {
			this.scaffoldPositions.clear();
			if (this.beatMount != null) {
				this.beatMount.remove();
				this.beatMount = null;
			}
			this.selectTarget();
		}

		Location loc = this.mob.getLocation();
		if (this.target != null)
			loc.setDirection(target.getEyeLocation().toVector().subtract(this.bukkitEntity.getEyeLocation().toVector()));
		this.bukkitEntity.teleport(loc);
	}

	private void chaseTick() {
		if (!this.scaffoldPositions.isEmpty()) {
			if (TeamArena.getGameTick() % 2 == 0) {
				final BridgePosition bp = scaffoldPositions.removeFirst();
				final Location loc = bp.loc;

				for (var entry : bp.blocks.entrySet()) {
					final Material mat = entry.getValue();

					if (mat.isAir())
						this.setMainHand(Material.DIAMOND_PICKAXE);
					else
						this.setMainHand(mat);

					this.bukkitEntity.swingMainHand();

					final Block blockPos = entry.getKey();
					if (mat.isAir()) {
						if (!blockPos.getType().isAir())
							Bukkit.getOnlinePlayers().forEach(player -> ParticleUtils.blockBreakEffect(player, blockPos));
					}
					else {
						loc.getWorld().playSound(this.bukkitEntity, blockPos.getBlockSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1f, 1f);
					}
					blockPos.setType(mat);
				}

				this.mob.teleport(loc); // TODO interpolate?
			}
		}
		else {
			this.setGravity(true);

			Pathfinder pathfinder = this.mob.getPathfinder();
			Pathfinder.PathResult pf = pathfinder.findPath(this.target);
			if (pf == null || !pf.canReachFinalPoint()) {
				if (this.mob.isOnGround() || this.mob.isInWater()) {
					Location start = this.mob.getLocation().subtract(0, 0.5, 0);
					Vector startVec = start.toVector();
					Location end = this.target.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();

					List<Block> list = new ArrayList<>();
					for (double d = 0; d < start.distance(end); d++) {
						Vector v = end.toVector().subtract(startVec).normalize().multiply(d);
						list.add(start.clone().add(v).getBlock());
					}

					for (int i = 0; i < list.size() - 1; i++) {
						Block current = list.get(i);
						Block next = list.get(i + 1);

						BlockFace face = current.getFace(next);
						Map<Block, Material> blockPlacements = new HashMap<>();
						if (next.getY() > current.getY() &&
							(next.getX() != current.getX() || next.getZ() != current.getZ())) {
							blockPlacements.put(next, Material.SCAFFOLDING);
							blockPlacements.put(next.getRelative(BlockFace.DOWN), Material.NETHERRACK);
						} else if (face == BlockFace.UP || face == BlockFace.DOWN) {
							blockPlacements.put(next, Material.SCAFFOLDING);
						} else {
							blockPlacements.put(next, Material.NETHERRACK);
						}

						blockPlacements.put(next.getRelative(BlockFace.UP), Material.AIR);
						blockPlacements.put(next.getRelative(BlockFace.UP, 2), Material.AIR);

						scaffoldPositions.add(new BridgePosition(next.getLocation().add(0.5d, 1d, 0.5d), blockPlacements));
					}

					this.setGravity(false);
				}
			} else {
				pathfinder.moveTo(pf, 2d);
			}

			if (pf != null) {
				printPath(pf);
			}
		}

		double reachSqr = this.bukkitEntity.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).getValue();
		reachSqr *= reachSqr;
		if (EntityUtils.distanceSqr(this.bukkitEntity, this.target) <= reachSqr) {
			this.setMainHand(Material.DIAMOND_SWORD);
			this.bukkitEntity.swingMainHand();

			DamageEvent damageEvent = DamageEvent.newDamageEvent(this.target, 1d, DamageType.MELEE, this.bukkitEntity, false);
			Main.getGame().queueDamage(damageEvent);
		}
	}

	private void smiteTick() {
		if (this.behaviourTick == 0) {
			final Location top = this.shrine.getLocation().add(0.5d, 1d, 0.5d);
			this.mob.teleport(top);
			particleTeleport(this.bukkitEntity, top);
			particleTeleport(this.target, top);
		}
		else if (this.behaviourTick < 3 * 20) {
			// noop
		}
		else if (this.behaviourTick == 3 * 20) {
			this.setMainHand(Material.WOODEN_AXE);
		}
		else if (this.behaviourTick % 30 == 0) {
			this.swing(InteractionHand.MAIN_HAND);
			this.bukkitEntity.getWorld().strikeLightningEffect(this.target.getLocation());
			DamageEvent damage = DamageEvent.newDamageEvent(this.target, 10d, DamageType.END_GAME_LIGHTNING, this.bukkitEntity, false);
			this.game.queueDamage(damage);
		}
	}

	private void beatTick() {
		if (this.behaviourTick == 0) {
			final Location top = this.shrine.getLocation().add(0.5d, 1d, 0.5d);
			final Location hero = top.clone().add(3d, 0d, 0d);

			this.setGravity(false);
			this.mob.teleport(hero);
			particleTeleport(this.bukkitEntity, hero);
			this.setMainHand(Material.AIR);

			this.beatMount = top.getWorld().spawn(top, Interaction.class);
			this.beatMount.getPersistentDataContainer().set(
				BEAT_MARKER_KEY,
				PersistentDataType.BOOLEAN,
				true
			);

			particleTeleport(this.target, top);
			this.beatMount.addPassenger(this.target);
		}
		else if (this.behaviourTick <= 3 * 20) {

		}
		else { // ora ora ora
			this.swing(InteractionHand.MAIN_HAND);

			final Location bloodLoc = target.getLocation().add(0d, (this.behaviourTick % 2 == 0 ? 0d : 1d), 0d);
			EntityUtils.forEachTrackedPlayer(this.target,
				viewer -> ParticleUtils.blockBreakEffect(viewer, Material.REDSTONE_BLOCK, bloodLoc));

			if (this.target instanceof Player pTarget) {
				ParticleUtils.blockBreakEffect(pTarget, Material.REDSTONE_BLOCK, bloodLoc);
				if (this.behaviourTick % 4 == 0) {
					SpeechBubbleHologram s = new SpeechBubbleHologram(
						pTarget, MathUtils.randomElement(SCREAM_MESSAGES), new ScreamMovementFunc()
					);
					s.setLiveTime(30);
					s.respawn();
				}
			}

			DamageEvent damage = DamageEvent.newDamageEvent(this.target, 1d, DamageType.MELEE, this.bukkitEntity, false);
			damage.setIgnoreInvulnerability(true);
			this.game.queueDamage(damage);
		}
	}

	public static void onDismount(EntityDismountEvent event) {
		if (event.getDismounted().getPersistentDataContainer().has(BEAT_MARKER_KEY)) {
			event.setCancelled(true);
		}
	}

	private void hTeleport(Location loc) {
		this.mob.teleport(loc);
		this.bukkitEntity.teleport(loc);
	}

	private static void particleTeleport(Entity e, Location loc) {
		e.getWorld().playEffect(e.getLocation(), Effect.ENDER_SIGNAL, 0);
		e.getWorld().playSound(e, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1f, 1f);
		e.teleport(loc);
		e.getWorld().playEffect(loc, Effect.ENDER_SIGNAL, 0);
		e.getWorld().playSound(e, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1f, 1f);
	}

	private static void printPath(Pathfinder.PathResult path) {
		for (Location p : path.getPoints()) {
			ParticleUtils.colouredRedstone(p, Color.RED, 1d ,1f);
		}
	}

	public static void buildShrine(Location baseLocation) {
		final World world = baseLocation.getWorld();

		final int baseX = baseLocation.getBlockX();
		final int baseY = baseLocation.getBlockY();
		final int baseZ = baseLocation.getBlockZ();

		//clear out the surrounding space first
		for(int x = -1; x < 2; x++) {
			for(int y = 0; y < 4; y++) {
				for(int z = -1; z < 2; z++) {
					world.getBlockAt(baseX + x, baseY + y, baseZ + z).setType(Material.AIR);
				}
			}
		}

		for(int x = -1; x < 2; x++) {
			for(int z = -1; z < 2; z++) {
				world.getBlockAt(baseX + x, baseY, baseZ + z).setType(Material.GOLD_BLOCK);
			}
		}

		world.getBlockAt(baseX, baseY + 1, baseZ).setType(Material.NETHERRACK);
		world.getBlockAt(baseX, baseY + 2, baseZ).setType(Material.FIRE);

		for(int x = -1; x < 2; x += 2)
			world.getBlockAt(baseX + x, baseY + 1, baseZ).setType(Material.REDSTONE_TORCH);

		for(int z = -1; z < 2; z += 2)
			world.getBlockAt(baseX, baseY + 1, baseZ + z).setType(Material.REDSTONE_TORCH);
	}

	public static class ScreamMovementFunc implements SpeechBubbleHologram.MovementFunc {
		@Override
		public Vector getNextPos(int age, int lifeTime) {
			double x = MathUtils.randomMax(1d);
			double y = MathUtils.randomMax(1d);
			double z = MathUtils.randomMax(1d);

			return new Vector(x, y, z).normalize();
		}
	}
}

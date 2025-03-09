package me.toomuchzelda.teamarenapaper.teamarena;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageNumbers;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketPlayer;
import me.toomuchzelda.teamarenapaper.utils.packetentities.SpeechBubbleHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.*;

public class Herobrine extends PacketPlayer {

	private static final int SLEEP_TIME = 3 * 20;
	private static final double MAX_HEALTH = 1000d;

	private static final ItemStack PICKAXE = new ItemStack(Material.DIAMOND_PICKAXE);
	private static final ItemStack SWORD = new ItemStack(Material.DIAMOND_SWORD);
	private static final ItemStack WOODEN_AXE = new ItemStack(Material.WOODEN_AXE);

	private enum TargetType {
		CHASE,
		SMITE,
		BEAT
	}
	// Bias for CHASE
	private static final TargetType[] CHANCES = new TargetType[] {
		TargetType.CHASE, TargetType.CHASE, TargetType.SMITE, TargetType.BEAT
	};

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

	private final Map<UUID, Double> damagers = new HashMap<>(); // Player uuids only

	private static GameProfile buildGameProfile() {
		PlayerProfile pp = Bukkit.createProfile(UUID.randomUUID(), "Herobrine");
		pp.setProperty(
			new ProfileProperty("textures",
			"ewogICJ0aW1lc3RhbXAiIDogMTczNDE3Mzc1NjI4NCwKICAicHJvZmlsZUlkIiA6ICI5NTg2ZTVhYjE1N2E0NjU4YWQ4MGIwNzU1MmE5Y2E2MyIsCiAgInByb2ZpbGVOYW1lIiA6ICJNSEZfSGVyb2JyaW5lIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJjNjVlZDI4MjljODNlMTE5YTgwZGZiMjIyMTY0NDNlODc4ZWYxMDY0OWM0YTM1NGY3NGJmNDVhZDA2YmMxYTciCiAgICB9CiAgfQp9",
			"a6/5LRu4JVXslE1vAuPdAAeFC797LodGesjfq8q1icfQOvgJfkSicGYiFvZf86Fo2C/hcIz8sZmgUsa5vDRpOlHfuK34nEc7tIh09bQtufH6xQY4q/nSDHy7ODVhjkX27GwZhMYE31NFjfpjVhVrQXWVWaJEtyDok0KlrllO+cU7Hbu0WbxhEMsStiU4u9r2yTaE9Njjp0YOL2i/Bm/02x4yYzyR+frmnZvVcBBHf7i6aElyj+GojNah1u88oooERY70eAbriCTX/MlzfgHxy+DP/SJgFmst4GZcSwfKs2YkV3fWyOhTfoFtlhVBbv39kJh5mZykPjOWZ2KonL/uuTUdgro9kVMhQrKE22cM93Nm3aaqhWJ9Ny7vJ/TPz1syTYRF5akKtU8WLROhY0mqzHoTXFUWyYU0er2oZtpi4WkUZIOf3gEhkOUVE5m09etg+lzPzNVxCid8/ae1/J4QyFsHAd/xDIXKVhRbcyoGOZI5d0iyGKxH0gt7WcjLVGBeu45C0+2HDcRsqyhph+NnqaFg5FwhxisYiTe3yi8rXKqVwqZFYmqTkmZJpPiaWbK5O7rDM3vnGl6lTKhOkKcOp5WSRRX2FfTIs79VfBcToLMQva8kamX8r0p4SVwwOZ3f02ueUV9TEXdIHt4UB6CqL40/De5/ftzXZ59z5oyOksw="
			)
		);

		return ((CraftPlayerProfile) pp).buildGameProfile();
	}

	public Herobrine(TeamArena game, Block shrineTop, Location location) {
		super(location, null, viewer -> true, buildGameProfile());

		this.game = game;
		this.shrine = shrineTop;
		this.sleepTime = SLEEP_TIME;
	}

	@Override
	protected void spawnMob(Location location) {
		super.spawnMob(location);

		this.mob.customName(Component.text("Herobrine"));
	}

	@Override
	public void respawn() {
		super.respawn();
		this.mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(MAX_HEALTH);
		this.mob.setHealth(MAX_HEALTH);
	}

	@Override
	public void despawn() {
		super.despawn();
		if (this.beatMount != null)
			this.beatMount.remove();
		this.beatMount = null;
		this.target = null;
	}

	@Override
	public void onConfirmedHurt(DamageEvent event) {
		super.onConfirmedHurt(event);

		if (event.getFinalAttacker() instanceof Player pAttacker) {
			this.damagers.merge(pAttacker.getUniqueId(), event.getFinalDamage(), Double::sum);
		}
	}

	public Player getHighestDamager() {
		Player p = null;
		double max = 0d;
		for (var entry : this.damagers.entrySet()) {
			if (entry.getValue() > max) {
				max = entry.getValue();
				p = Bukkit.getPlayer(entry.getKey());
			}
		}

		return p;
	}

	private void selectTarget() {
		TargetType goal = TargetType.CHASE; // MathUtils.randomElement(CHANCES);

		LivingEntity candidate = null;

		ArrayList<Player> candidates = new ArrayList<>(Bukkit.getOnlinePlayers());
		candidates.removeIf(this.game::isDead);

		if (!candidates.isEmpty()) {
			Collections.shuffle(candidates);

			// Add a bias for players further away
			candidates.sort((o1, o2) -> {
				boolean o1Sneaking = o1.isSneaking();
				boolean o2Sneaking = o2.isSneaking();
				if (o1Sneaking != o2Sneaking) {
					if (o1Sneaking) return -1;
					else return 1;
				} else {
					double dist1 = EntityUtils.distanceSqr(o1, this.getLocationMut());
					double dist2 = EntityUtils.distanceSqr(o2, this.getLocationMut());
					dist1 /= 10d;
					dist2 /= 10d;
					dist1 = Math.floor(dist1);
					dist2 = Math.floor(dist2);

					return (int) (dist2 - dist1);
				}
			});

			if (candidates.size() == 1)
				candidate = candidates.getFirst();
			else {
				final int index = MathUtils.random.nextInt(candidates.size() / 2);
				candidate = candidates.get(index);
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

		if (this.sleepTime-- <= 0) {
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
					this.setEquipment(EquipmentSlot.HAND, new ItemStack(Material.AIR));
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
		}

		Location loc = this.mob.getLocation();
		if (this.target != null)
			loc.setDirection(target.getEyeLocation().toVector().subtract(this.getEyeLoc().toVector()));
		else
			loc.setDirection(this.getLocationMut().getDirection());
		this.move(loc, true);
	}

	private void chaseTick() {
		if (!this.scaffoldPositions.isEmpty()) {
			if (TeamArena.getGameTick() % 2 == 0) {
				final BridgePosition bp = scaffoldPositions.removeFirst();
				final Location loc = bp.loc;

				for (var entry : bp.blocks.entrySet()) {
					final Material mat = entry.getValue();

					this.swingMainHand();

					final Block blockPos = entry.getKey();
					if (mat.isAir()) {
						if (!blockPos.getType().isAir()) {
							this.setMainHand(PICKAXE);
							Bukkit.getOnlinePlayers().forEach(player -> ParticleUtils.blockBreakEffect(player, blockPos));
						}
					}
					else {
						this.setMainHand(new ItemStack(mat));
						loc.getWorld().playSound(this.getLocationMut(), blockPos.getBlockSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1f, 1f);
					}

					if (this.game.isVandalisableBlock(blockPos))
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
				//printPath(pf);
			}
		}

		double reachSqr = 3d; // this.mob.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).getValue();
		reachSqr *= reachSqr;
		if (EntityUtils.distanceSqr(this.target, this.getLocationMut()) <= reachSqr) {
			this.setMainHand(SWORD);
			this.swingMainHand();

			DamageEvent damageEvent = DamageEvent.newDamageEvent(this.target, 1d, DamageType.MELEE, this.mob, false);
			damageEvent.setRawDamage(DamageNumbers.getMaterialBaseDamage(SWORD.getType()));
			Main.getGame().queueDamage(damageEvent);
		}
	}

	private void smiteTick() {
		if (this.behaviourTick == 0) {
			final Location top = this.shrine.getLocation().add(0.5d, 1d, 0.5d);
			this.mob.teleport(top);
			particleMove(top);
			particleTeleport(this.target, top);
		}
		else if (this.behaviourTick < 2 * 20) {
			// noop
		}
		else if (this.behaviourTick == 3 * 20) {
			this.setMainHand(WOODEN_AXE);
		}
		else if (this.behaviourTick % 30 == 0) {
			this.swingMainHand();
			this.mob.getWorld().strikeLightningEffect(this.target.getLocation());
			DamageEvent damage = DamageEvent.newDamageEvent(this.target, 10d, DamageType.END_GAME_LIGHTNING, this.mob, false);
			this.game.queueDamage(damage);
		}
	}

	private void beatTick() {
		if (this.behaviourTick == 0) {
			final Location top = this.shrine.getLocation().add(0.5d, 1.5d, 0.5d);
			final Location hero = top.clone().add(3d, -0.5d, 0d);

			this.setGravity(false);
			this.mob.teleport(hero);
			particleMove(hero);
			this.setMainHand(new ItemStack(Material.AIR));

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
			this.swingMainHand();

			final Location bloodLoc = target.getLocation().add(0d, (this.behaviourTick % 2 == 0 ? 0d : 1d), 0d);
			EntityUtils.forEachTrackedPlayer(this.target,
				viewer -> ParticleUtils.blockBreakEffect(viewer, Material.REDSTONE_BLOCK, bloodLoc));

			if (this.target instanceof Player pTarget) {
				ParticleUtils.blockBreakEffect(pTarget, Material.REDSTONE_BLOCK, bloodLoc);
				if (this.behaviourTick % 4 == 0) {
					// Spawning a new packetent during tick() throws CME
					Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
						SpeechBubbleHologram s = new SpeechBubbleHologram(
							pTarget, MathUtils.randomElement(SCREAM_MESSAGES), new ScreamMovementFunc()
						);
						s.setLiveTime(30);
						s.respawn();
					});
				}
			}

			DamageEvent damage = DamageEvent.newDamageEvent(this.target, 1d, DamageType.MELEE, this.mob, false);
			damage.setIgnoreInvulnerability(true);
			this.game.queueDamage(damage);
		}
	}

	public static void onDismount(EntityDismountEvent event) {
		if (event.getDismounted().getPersistentDataContainer().has(BEAT_MARKER_KEY)) {
			event.setCancelled(true);
		}
	}

	private void particleMove(Location loc) {
		this.getWorld().playEffect(this.getLocationMut(), Effect.ENDER_SIGNAL, 0);
		this.getWorld().playSound(this.getLocationMut(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1f, 1f);
		this.move(loc);
		this.getWorld().playEffect(loc, Effect.ENDER_SIGNAL, 0);
		this.getWorld().playSound(this.getLocationMut(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1f, 1f);
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

			return new Vector(x, y, z).normalize().multiply(1.2d);
		}
	}
}

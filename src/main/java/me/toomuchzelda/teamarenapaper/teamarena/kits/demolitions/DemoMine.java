package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.building.EntityBuilding;
import me.toomuchzelda.teamarenapaper.utils.BlockUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.phys.AABB;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public abstract class DemoMine extends EntityBuilding
{
	public static final int TIME_TO_ARM = 30;
	public static final double REMOTE_ARMING_DISTANCE = 1000d;
	public static final double REMOTE_ARMING_DISTANCE_SQRD = REMOTE_ARMING_DISTANCE * REMOTE_ARMING_DISTANCE;

	public final TeamArenaTeam team;
	ArmorStand[] stands;
	final PacketMineHitbox hitboxEntity;
	Player triggerer; //store the player that stepped on it for shaming OR the demo if remote detonate

	//for construction
	final Location baseLoc;
	final Vector offset;
	final Color color;
	EquipmentSlot armorSlot;

	int damage = 0; //amount of damage it has
	//whether to remove on next tick
	// whether it needs to be removed from hashmaps is checked every tick, and we can't remove it on the same tick
	// as the damage events are processed after the ability tick, so we need to 'schedule' it for removal next tick
//	boolean removeNextTick = false;
	int creationTime; //store for knowing when it gets 'armed' after placing
	boolean glowing; //if it's glowing the targetted colour for the owner
	int timeToDetonate;

	MineType type;

	private static Location blockToLocation(Block block) {
		Location blockLoc = block.getLocation();
		double topOfBlock = BlockUtils.getBlockHeight(block);
		return blockLoc.add(0.5d, topOfBlock, 0.5d);
	}

	public DemoMine(Player demo, Block block) {
		super(demo, blockToLocation(block));
		setName("Mine");
		this.team = Main.getPlayerInfo(owner).team;

		Location blockLoc = block.getLocation();
		this.color = BlockUtils.getBlockBukkitColor(block);

		double topOfBlock = BlockUtils.getBlockHeight(block);
		this.offset = new Vector(0.5, topOfBlock, 0.5);
		this.baseLoc = blockLoc.add(offset);

		this.hitboxEntity = new PacketMineHitbox(baseLoc.clone().add(0, -0.20d, 0));
	}

	@Override
	public void onPlace() {
		this.creationTime = TeamArena.getGameTick();
		this.hitboxEntity.respawn();
	}

	@Override
	public void onTick() {
		if (isDone()) {
			markInvalid();
		}
		//if it hasn't been armed yet
		else if (!isArmed()) {
			//indicate its armed
			if (TeamArena.getGameTick() == creationTime + DemoMine.TIME_TO_ARM) {
				World world = hitboxEntity.getWorld();
				world.playSound(hitboxEntity.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1f, 1f);
				world.spawnParticle(Particle.CRIT, hitboxEntity.getLocation()
					.add(0, 0.4, 0), 2, 0, 0, 0, 0);

				Component message = Component.text("Your " + type.name + " is now armed", NamedTextColor.GREEN);
				PlayerUtils.sendKitMessage(owner, message, message);
			}
			// else do nothing and don't enter the control statement below that checks for collision
		}
		//if it hasn't been stepped on already check if anyone's standing on it
		else if (!isTriggered()) {
			for (Player stepper : Main.getGame().getPlayers()) {
				if (team.getPlayerMembers().contains(stepper))
					continue;

				PacketMineHitbox axolotl = hitboxEntity;
				if (stepper.getBoundingBox().overlaps(axolotl.getBoundingBox())) {
					//they stepped on mine, trigger explosion
					trigger(stepper);
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		hitboxEntity.remove();

		removeEntities();
	}

	@Override
	public @NotNull Collection<? extends PacketEntity> getPacketEntities() {
		return List.of(hitboxEntity);
	}

	public void onTeamSwitch(TeamArenaTeam oldTeam, TeamArenaTeam newTeam) {

	}

	void removeEntities() {
		for(ArmorStand stand : stands) {
			for(Player viewer : this.team.getPlayerMembers()) {
				Main.getPlayerInfo(viewer).getMetadataViewer().removeViewedValues(stand);
			}
			stand.remove();
		}
		hitboxEntity.remove();
	}

	/**
	 * @return return true if mine extinguised/removed
	 */
	boolean hurt() {
		this.damage++;
		World world = this.hitboxEntity.getWorld();
		Location loc = hitboxEntity.getLocation();
		for(int i = 0; i < 3; i++) {
			world.playSound(loc, Sound.BLOCK_GRASS_HIT, 1f, 0.5f);
			world.spawnParticle(Particle.CLOUD, loc.clone().add(0d, 0.2d, 0d), 1,
					0.2d, 0.2d, 0.2d, 0.02d);
		}

		if(this.damage >= type.damageToKill) {
			// game command: /particle minecraft:cloud ~3 ~0.2 ~ 0.2 0.2 0.2 0.02 3 normal
			world.spawnParticle(Particle.CLOUD, hitboxEntity.getLocation().add(0d, 0.2d, 0d), 3,
					0.2d, 0.2d, 0.2d, 0.02d);
			world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 1f);
			world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 1.3f);
			world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1.5f, 1f);
			markInvalid();
			// reimburse mine items
			KitDemolitions.DemolitionsAbility.addRegeneratingMine(owner, type, TeamArena.getGameTick());
			return true;
		}
		return false;
	}

	public void trigger(Player triggerer) {
		this.triggerer = triggerer;

		Component message;
		if(this.owner == triggerer) {
			this.timeToDetonate = type.timeToDetonateRemote;
			message = Component.text("Remote triggered your " + this.type.name, NamedTextColor.AQUA);
		}
		else {
			this.timeToDetonate = type.timeToDetonate;
			message = Component.text("Your " + this.type.name + " was triggered!", NamedTextColor.AQUA);
		}

		PlayerUtils.sendKitMessage(this.owner, message, message);

		World world = hitboxEntity.getWorld();
		Location loc = hitboxEntity.getLocation();

		world.playSound(loc, Sound.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_CREEPER_HURT, 1f, 0f);
		world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0f);

		// reimburse mine items
		KitDemolitions.DemolitionsAbility.addRegeneratingMine(owner, type, TeamArena.getGameTick());
		//subclass here
	}

	abstract boolean isDone();

	boolean isTriggered() {
		return this.triggerer != null;
	}

	boolean isArmed() {
		return TeamArena.getGameTick() > this.creationTime + DemoMine.TIME_TO_ARM;
	}

//	@Override
//	public Vector getOffset() {
//		return offset;
//	}

	protected class PacketMineHitbox extends PacketEntity {
		private final BoundingBox hitbox;
		public int lastHurtTime;

		public PacketMineHitbox(Location location) {
			super(PacketEntity.NEW_ID, EntityType.AXOLOTL, location, null, PacketEntity.VISIBLE_TO_ALL);

			this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
			this.updateMetadataPacket();

			AABB bb = net.minecraft.world.entity.EntityType.AXOLOTL.getDimensions().makeBoundingBox(
					location.getX(), location.getY(), location.getZ());
			this.hitbox = new BoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
			lastHurtTime = 0;
		}

		//Axolotol hitbox shouldn't move, no need to adjust for that
		public BoundingBox getBoundingBox() {
			return hitbox;
		}

		@Override
		public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
			if (!attack)
				return;
			if (Main.getGame().isDead(player))
				return;

			DemoMine mine = DemoMine.this;
			//teammate punches it
			if (player != mine.owner && mine.team.getPlayerMembers().contains(player)) {
				player.sendMessage(Component.text("This is ", NamedTextColor.AQUA).append(
					mine.owner.playerListName()).append(Component.text("'s " + mine.type.name)));
			} else {
				int currentTick = TeamArena.getGameTick();
				int diff = currentTick - lastHurtTime;
				if (diff >= 10) {
					lastHurtTime = currentTick;
					if (mine.hurt()) {
						Component message;
						if (player != mine.owner) {
							message = Component.text("You've broken one of ", NamedTextColor.AQUA).append(
								mine.owner.playerListName()).append(Component.text("'s " + mine.type.name + "s!",
								NamedTextColor.AQUA));

							Component ownerMessage = Component.text("Someone broke one of your " + mine.type.name + "s!",
								NamedTextColor.AQUA);

							PlayerUtils.sendKitMessage(mine.owner, ownerMessage, ownerMessage);
						} else {
							message = Component.text("Broke your " + mine.type.name).color(NamedTextColor.AQUA);
						}
						player.sendMessage(message);
					}
				}
			}
		}
	}
}

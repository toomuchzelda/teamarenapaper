package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.BlockUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BlockVector;
import org.bukkit.util.EulerAngle;

public abstract class DemoMine
{
	public static final EulerAngle LEG_ANGLE = new EulerAngle(1.5708d, 0 ,0); //angle for legs so boots r horizontal
	public static final int TNT_TIME_TO_DETONATE = 20;
	public static final int TIME_TO_ARM = 30;

	//used to set the colour of the glowing effect on the mine armor stand's armor
	// actual game teams don't matter, just need for the colour

	//DARK GREEN and RED - how other teammates see the mines of their team
	static final Team DARK_GREEN_GLOWING_TEAM; // push mine
	static final Team RED_GLOWING_TEAM; // tnt mine

	static final Team AQUA_GLOWING_TEAM; // demo's targetted mine

	//GREEN and GOLD - how the mine owner sees their mines
	static final Team GREEN_GLOWING_TEAM;
	static final Team GOLD_GLOWING_TEAM;


	static final Team[] COLOUR_TEAMS;

	private static final String MINE_TEAM_NAME = "DemoMine";

	static {
			COLOUR_TEAMS = new Team[5];

			NamedTextColor[] matchingColours = new NamedTextColor[] {NamedTextColor.DARK_GREEN, NamedTextColor.RED,
				NamedTextColor.AQUA, NamedTextColor.GREEN, NamedTextColor.GOLD};

			for(int i = 0; i < 5; i++) {
				COLOUR_TEAMS[i] = SidebarManager.SCOREBOARD.registerNewTeam(MINE_TEAM_NAME + matchingColours[i].value());
				COLOUR_TEAMS[i].color(matchingColours[i]);
				COLOUR_TEAMS[i].setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

				PlayerScoreboard.addGlobalTeam(COLOUR_TEAMS[i]);
			}

			DARK_GREEN_GLOWING_TEAM = COLOUR_TEAMS[0];
			RED_GLOWING_TEAM = COLOUR_TEAMS[1];
			AQUA_GLOWING_TEAM = COLOUR_TEAMS[2];
			GREEN_GLOWING_TEAM = COLOUR_TEAMS[3];
			GOLD_GLOWING_TEAM = COLOUR_TEAMS[4];
	}

	final Player owner;
	public final TeamArenaTeam team;
	Team glowingTeam;
	ArmorStand[] stands;
	final Axolotl hitboxEntity; //the mine's interactable hitbox
	Player triggerer; //store the player that stepped on it for shaming OR the demo if remote detonate

	//for construction
	final BlockVector blockVector;
	final Location baseLoc;
	final Color color;
	EquipmentSlot armorSlot;

	int damage = 0; //amount of damage it has
	//whether to remove on next tick
	// whether it needs to be removed from hashmaps is checked every tick, and we can't remove it on the same tick
	// as the damage events are processed after the ability tick, so we need to 'schedule' it for removal next tick
	boolean removeNextTick = false;
	int creationTime; //store for knowing when it gets 'armed' after placing
	boolean glowing; //if it's glowing the targetted colour for the owner

	MineType type;

	public DemoMine(Player demo, Block block) {
		owner = demo;
		this.team = Main.getPlayerInfo(owner).team;
		this.creationTime = TeamArena.getGameTick();

		Location blockLoc = block.getLocation();
		this.blockVector = blockLoc.toVector().toBlockVector();
		this.color = BlockUtils.getBlockBukkitColor(block);

		World world = block.getWorld();
		double topOfBlock = BlockUtils.getBlockHeight(block);
		//put downwards slightly so rotated legs lay flat on ground and boots partially in ground
		this.baseLoc = blockLoc.add(0.5d, topOfBlock, 0.5d);

		this.hitboxEntity = world.spawn(baseLoc.clone().add(0, -0.20d, 0), Axolotl.class, entity -> {
			entity.setAI(false);
			entity.setSilent(true);
			entity.setInvisible(true);
		});
	}

	void removeEntities() {
		glowingTeam.removeEntities(stands);
		PlayerScoreboard.removeMembersAll(glowingTeam, stands);
		for(ArmorStand stand : stands) {
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
		for(int i = 0; i < 3; i++) {
			world.playSound(hitboxEntity.getLocation(), Sound.BLOCK_GRASS_HIT, 999, 0.5f);
			world.spawnParticle(Particle.CLOUD, hitboxEntity.getLocation().add(0d, 0.2d, 0d), 1,
					0.2d, 0.2d, 0.2d, 0.02d);
		}

		if(this.damage >= type.damageToKill) {
			// game command: /particle minecraft:cloud ~3 ~0.2 ~ 0.2 0.2 0.2 0.02 3 normal
			world.spawnParticle(Particle.CLOUD, hitboxEntity.getLocation().add(0d, 0.2d, 0d), 3,
					0.2d, 0.2d, 0.2d, 0.02d);
			world.playSound(hitboxEntity.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 1f);
			world.playSound(hitboxEntity.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 1.3f);
			world.playSound(hitboxEntity.getLocation(), Sound.BLOCK_STONE_BREAK, 1.5f, 1f);
			this.removeNextTick = true;
			return true;
		}
		return false;
	}

	void trigger(Player triggerer) {
		this.triggerer = triggerer;
		World world = hitboxEntity.getWorld();
		Location loc = hitboxEntity.getLocation();

		world.playSound(loc, Sound.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_CREEPER_HURT, 1f, 0f);
		world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0f);

		//subclass here
	}

	abstract boolean isDone();

	boolean isTriggered() {
		return this.triggerer != null;
	}

	void tick() {};

	BlockVector getBlockVector() {
		return blockVector;
	}

	void glow() {
		this.glowing = true;
		Main.getPlayerInfo(owner).getScoreboard().addMembers(AQUA_GLOWING_TEAM, stands);
	}

	void unGlow() {
		this.glowing = false;
		Main.getPlayerInfo(owner).getScoreboard().addMembers(this.glowingTeam, stands);
	}

	public static boolean isMineStand(int id) {
		return KitDemolitions.DemolitionsAbility.ARMOR_STAND_ID_TO_DEMO_MINE.containsKey(id);
	}

	public static DemoMine getStandMine(int id) {
		return KitDemolitions.DemolitionsAbility.ARMOR_STAND_ID_TO_DEMO_MINE.get(id);
	}

	public static ArmorStand getMineStand(int id) {
		DemoMine mine = getStandMine(id);
		if(mine != null) {
			ArmorStand[] stands = mine.stands;
			for (ArmorStand stand : stands) {
				if (stand.getEntityId() == id)
					return stand;
			}
		}

		return null;
	}

	static void clearTeams() {
		for(Team team : COLOUR_TEAMS) {
			PlayerScoreboard.removeEntriesAll(team, team.getEntries());
			team.removeEntries(team.getEntries());
		}
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.announcer.AnnouncerManager;
import me.toomuchzelda.teamarenapaper.teamarena.announcer.AnnouncerSound;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

/**
 * Class to manage TeamArena's killstreaks.
 * Also handles the killstreak announcer voice lines.
 *
 * @author toomuchzelda
 */
public class KillStreakManager
{
	public enum KillStreakID {
		PAYLOAD_TEST,
		COMPASS,
		WOLVES,
		IRON_GOLEM,
		HARBINGER
	}

	private static final boolean ANNOUNCE_SUCCESSIVE_KILLS = false;
	// The amount of time since the last kill that each announcer line can play when getting a kill.
	// Index is the amount of kills - 2 (sounds start at 2 kills), value is the time in ticks and sound
	private static final int[] TIMES_SINCE_LAST_KILL = new int[] {20, 2 * 20, 3 * 20, 4 * 20};
	private static final AnnouncerSound[] SUCCESSIVE_KILL_SOUNDS = new AnnouncerSound[] {
		AnnouncerSound.GAME_DOUBLE_KILL,
		AnnouncerSound.GAME_TRIPLE_KILL,
		AnnouncerSound.GAME_QUAD_KILL,
		AnnouncerSound.GAME_UNSTOPPABLE
	};

	private final Map<String, KillStreak> allKillstreaks;
	private final Map<Integer, List<KillStreak>> killstreaksByKills;
	private final Map<ItemStack, CratedKillStreak> crateItemLookup;
	private final Set<KillStreakID> disabledKillstreaks; // Will still be processed but not awarded to players upon kills

	private final List<Crate> allCrates;

	// Crate calling fireworks here so can cancel the damage from their explosions.
	final Set<Firework> crateFireworks = Collections.newSetFromMap(new WeakHashMap<>());

	public KillStreakManager() {
		this.allKillstreaks = new LinkedHashMap<>();
		this.killstreaksByKills = new HashMap<>();
		this.crateItemLookup = new HashMap<>();
		this.disabledKillstreaks = Collections.newSetFromMap(new EnumMap<>(KillStreakID.class));

		// KillStreak map keys must not have spaces in them
		addKillStreak(-1, new PayloadTestKillstreak());
		addKillStreak(2, new CompassKillStreak());
		addKillStreak(4, new WolvesKillStreak());
		addKillStreak(8, new IronGolemKillStreak());
		addKillStreak(11, new HarbingerKillStreak());

		// Register all killstreaks
		this.allKillstreaks.values().forEach(killStreak ->
				killStreak.getAbilities().forEach(Ability::registerAbility));

		this.allCrates = new ArrayList<>();
	}

	public KillStreak getKillStreak(String name) {
		return allKillstreaks.get(name);
	}

	public void disableKillStreak(KillStreakID id) {
		this.disabledKillstreaks.add(id);
	}

	public boolean isDisabled(KillStreak streak) {
		return this.disabledKillstreaks.contains(streak.getIdentifier());
	}

	private void addKillStreak(int killCount, KillStreak killStreak) {
		allKillstreaks.put(killStreak.getName().replaceAll(" ", ""), killStreak);

		List<KillStreak> list = killstreaksByKills.computeIfAbsent(killCount, integer -> new ArrayList<>(1));
		list.add(killStreak);

		if (killStreak instanceof CratedKillStreak cratedKillStreak) {
			this.crateItemLookup.put(cratedKillStreak.getCrateItem(), cratedKillStreak);
		}
	}

	public void handleKill(Player killer, int newKills, PlayerInfo pinfo) {
		List<KillStreak> streaks = killstreaksByKills.get(newKills);
		if(streaks != null) {
			for (KillStreak streak : streaks) {
				if (this.isDisabled(streak)) continue;

				if(streak instanceof CratedKillStreak cratedStreak)
					killer.getInventory().addItem(cratedStreak.getCrateItem());
				else {
					streak.giveStreak(killer, pinfo);
				}

				killer.sendMessage(Component.text()
						.append(Component.text(newKills + " Kill streak! Got ", NamedTextColor.RED))
						.append(streak.getComponentName())
						.build()
				);
			}
		}

		if (ANNOUNCE_SUCCESSIVE_KILLS) {
			// Play the announcer sound if they got another kill in fast enough succession
			final int index = newKills - 2;
			if (index >= 0 && index < TIMES_SINCE_LAST_KILL.length) {
				if (TeamArena.getGameTick() <= pinfo.lastKillTime + TIMES_SINCE_LAST_KILL[index]) {
					AnnouncerManager.playSound(killer, SUCCESSIVE_KILL_SOUNDS[index]);
				}
			}
		}
	}

	/**
	 * When a player uses their crate call item, handle it and create the Crate instance that will fall to them.
	 */
	public void handleCrateItemUse(PlayerInteractEvent event) {
		if (event.getItem() == null) return;

		CratedKillStreak crateStreak = this.crateItemLookup.get(event.getItem().asOne());
		if(crateStreak == null) return;

		event.setUseItemInHand(Event.Result.DENY); // A crate item was used: cancel the event
		event.setUseInteractedBlock(Event.Result.DENY);

		// Validate it's a good position to drop a crate.
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if(event.getBlockFace() != BlockFace.UP) return;
		if(!event.getClickedBlock().getRelative(BlockFace.UP).getType().isAir()) return;

		// Decrement stack size by one
		PlayerInventory inventory = event.getPlayer().getInventory();
		EquipmentSlot slot = event.getHand();
		inventory.setItem(slot, inventory.getItem(slot).subtract());

		Crate crate = new Crate(event.getPlayer(),
				event.getClickedBlock().getLocation().add(0.5d, 1d, 0.5d), crateStreak);
		allCrates.add(crate);
	}

	public void removeKillStreaks(Player player, PlayerInfo pinfo) {
		// Need to copy as Ability.removeAbility will modify during iteration
		Set<Ability> playersAbilities = new HashSet<>(pinfo.abilities);
		for(KillStreak streak : allKillstreaks.values()) {
			for(Ability streakAbility : streak.getAbilities()) {
				if(playersAbilities.contains(streakAbility)) {
					Ability.removeAbility(player, streakAbility, pinfo);
				}
			}
		}

		this.allCrates.removeIf(crate -> {
			if (crate.owner == player) {
				crate.remove();
				return true;
			}
			return false;
		}); // remove currently falling crates
	}

	public void tick() {
		for(KillStreak killStreak : allKillstreaks.values()) {
			killStreak.getAbilities().forEach(Ability::onTick);
		}

		var crateIter = allCrates.iterator();
		while(crateIter.hasNext()) {
			Crate crate = crateIter.next();

			if(crate.isDone()) {
				crate.remove();
				crateIter.remove();
			}
			else {
				crate.tick();
			}
		}
	}

	public void unregister() {
		for(KillStreak killStreak : allKillstreaks.values()) {
			killStreak.getAbilities().forEach(Ability::unregisterAbility);
		}

		allKillstreaks.clear();
		killstreaksByKills.clear();
	}

	public boolean isCrateFirework(Entity entity) {
		return entity instanceof Firework firework && this.crateFireworks.contains(firework);
	}

	public Collection<String> getKillStreakNames() {
		return this.allKillstreaks.keySet();
	}
}

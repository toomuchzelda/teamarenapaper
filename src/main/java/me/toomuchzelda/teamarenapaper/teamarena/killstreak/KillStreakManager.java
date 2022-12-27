package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
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
import org.bukkit.inventory.Inventory;

import java.util.*;

/**
 * Class to manage TeamArena's killstreaks.
 *
 * @author toomuchzelda
 */
public class KillStreakManager
{
	private final Map<String, KillStreak> allKillstreaks;
	private final Map<Integer, List<KillStreak>> killstreaksByKills;

	private final List<Crate> allCrates;

	// Crate calling fireworks here so can cancel the damage from their explosions.
	final Set<Firework> crateFireworks = Collections.newSetFromMap(new WeakHashMap<>());

	public KillStreakManager() {
		this.allKillstreaks = new LinkedHashMap<>();
		this.killstreaksByKills = new HashMap<>();

		// TODO construst and put the killstreaks here
		// KillStreak map keys must not have spaces in them
		addKillStreak("Compass", 2, new CompassKillStreak());
		addKillStreak("Wolves", 4, new WolvesKillStreak());

		// Register all killstreaks
		this.allKillstreaks.values().forEach(killStreak ->
				killStreak.getAbilities().forEach(Ability::registerAbility));

		this.allCrates = new LinkedList<>();
	}

	public KillStreak getKillStreak(String name) {
		return allKillstreaks.get(name);
	}

	private void addKillStreak(String name, int killCount, KillStreak killStreak) {
		allKillstreaks.put(name, killStreak);
		List<KillStreak> list = killstreaksByKills.computeIfAbsent(killCount, integer -> new ArrayList<>(1));
		list.add(killStreak);
	}

	public void handleKill(Player killer, int newKills, PlayerInfo pinfo) {
		List<KillStreak> streaks = killstreaksByKills.get(newKills);
		if(streaks != null) {
			for (KillStreak streak : streaks) {
				if(!streak.isDeliveredByCrate())
					streak.giveStreak(killer, pinfo);
				else {
					killer.getInventory().addItem(Crate.createCrateItem(streak));
				}

				killer.sendMessage(Component.text()
						.append(Component.text(newKills + " Kill streak! Got ", NamedTextColor.RED))
						.append(streak.getComponentName())
						.build()
				);
			}
		}
	}

	/**
	 * When a player uses their crate call item, handle it and create the Crate instance that will fall to them.
	 */
	public void handleCrateItemUse(PlayerInteractEvent event) {
		// First validate it's a good position to drop a crate.
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if(event.getBlockFace() != BlockFace.UP) return;
		if(!event.getClickedBlock().getRelative(BlockFace.UP).getType().isAir()) return;

		KillStreak streak = Crate.crateItems.remove(event.getItem());
		if(streak == null) return;

		Crate crate = new Crate(event.getPlayer(), streak.getCrateBlockType(),
				event.getClickedBlock().getLocation().add(0.5d, 1d, 0.5d), streak);

		allCrates.add(crate);

		// Decrement stack size by one
		Inventory inventory = event.getPlayer().getInventory();
		int index = inventory.first(event.getItem());
		inventory.getItem(index).subtract();

		event.setUseItemInHand(Event.Result.DENY);
		event.setUseInteractedBlock(Event.Result.DENY);
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
	}

	public void tick() {
		for(KillStreak killStreak : allKillstreaks.values()) {
			killStreak.getAbilities().forEach(Ability::onTick);
		}

		var crateIter = allCrates.iterator();
		while(crateIter.hasNext()) {
			Crate crate = crateIter.next();

			if(crate.isDone()) {
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

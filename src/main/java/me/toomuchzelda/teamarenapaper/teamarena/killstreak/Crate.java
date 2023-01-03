package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Class to store data for a killstreak crate.
 * A crate that is summoned with a firework and falls from the sky to a destination and then can be opened by the owner.
 * In 30 seconds, allies can open it, and after 60 seconds anyone can open it.
 *
 * @author toomuchzelda
 */
public class Crate
{
	// Amount of time after spawning the firework the crate block entity spawns and starts falling
	private static final int CRATE_DELAY_TIME = 50;
	private static final double FALL_DELTA = -1d;
	private static final List<Component> USE_MSG = List.of(
			ItemUtils.noItalics(Component.text("Right Click on the ground to summon", TextUtils.RIGHT_CLICK_TO)),
			ItemUtils.noItalics(Component.text("this Killstreak to that location", TextUtils.RIGHT_CLICK_TO))
	);

	static final Map<ItemStack, KillStreak> crateItems = new HashMap<>();

	// Should not be called for KillStreaks that are not crate-delivered.
	public static ItemStack createCrateItem(KillStreak killStreak) {
		ItemStack item = new ItemStack(killStreak.getCrateItemType());
		ItemMeta meta = item.getItemMeta();

		meta.displayName(ItemUtils.noItalics(
				Component.text()
						.append(Component.text("Summon ", NamedTextColor.LIGHT_PURPLE))
						.append(killStreak.getComponentName())
						//.append(Component.text(" crate", NamedTextColor.LIGHT_PURPLE))
						.build()
		));
		List<Component> lore = new ArrayList<>(5);
		lore.add(ItemUtils.noItalics(killStreak.getComponentName()));
		lore.addAll(TextUtils.wrapString(killStreak.getDescription(), TextUtils.PLAIN_STYLE, TextUtils.DEFAULT_WIDTH));
		lore.addAll(USE_MSG);

		lore.add(Component.text(ItemUtils.getUniqueId())); // Add unique string to be able to track this in HashMap.

		meta.lore(lore);
		meta.addEnchant(Enchantment.DURABILITY, 1, true);

		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

		item.setItemMeta(meta);

		crateItems.put(item, killStreak);
		return item;
	}

	private final Player owner;
	private final Material blockType;
	private final Set<KillStreak> killStreaks; // Killstreaks given to the player that opens it.
	private final Location destination; // Destination it needs to fall to.

	private Firework firework; // The firework used to summon this crate
	private Parrot parrot; // Parrot riding the firework
	private FallingCrate fallingCrate;

	private final int spawnTime;

	private boolean done;

	public Crate(Player owner, Material blockType, Location destination, KillStreak... killStreaks) {
		this.owner = owner;
		this.blockType = blockType;
		this.destination = destination;

		this.killStreaks = Set.of(killStreaks);

		this.spawnTime = TeamArena.getGameTick();

		TeamArenaTeam team = Main.getPlayerInfo(owner).team;

		this.firework = owner.getWorld().spawn(this.destination, Firework.class, firework1 -> {
			FireworkMeta meta = firework1.getFireworkMeta();
			meta.clearEffects();
			meta.addEffect(FireworkEffect.builder().trail(true).withColor(team.getColour()).build());
			meta.setPower(127);
			firework1.setFireworkMeta(meta);
		});

		Location lookUp = destination.clone().setDirection(new Vector(0d, 1d, 0d));
		this.parrot = owner.getWorld().spawn(lookUp, Parrot.class, parrot -> {
			parrot.setInvulnerable(true);
			parrot.setAI(false);

			Parrot.Variant[] variants = Parrot.Variant.values();
			parrot.setVariant(variants[MathUtils.random.nextInt(variants.length)]);
		});

		this.firework.addPassenger(parrot);

		done = false;

		Main.getGame().getKillStreakManager().crateFireworks.add(this.firework);

		this.killStreaks.forEach(killStreak -> killStreak.onCratePlace(this.owner, this.destination));
	}

	void tick() {
		final int currentTick = TeamArena.getGameTick();

		final int diff = currentTick - this.spawnTime;
		if(diff >= CRATE_DELAY_TIME) {
			if(this.fallingCrate == null) {
				this.parrot.setHealth(0);
				this.firework.detonate();

				this.killStreaks.forEach(killStreak -> killStreak.onFireworkFinish(this.owner, this.destination, this));

				this.parrot = null;
				this.firework = null;

				if(!this.isDone()) { // May be marked done by the above event call

					this.fallingCrate = new FallingCrate(this.destination.clone().add(0, 64, 0),
						Main.getPlayerInfo(owner).team.getDyeColour(),
						blockType.createBlockData());

					this.fallingCrate.spawn();
				}
			}

			if(!this.isDone()) {
				if (fallingCrate.getY() < destination.getY()) {
					fallingCrate.despawn();
					fallingCrate = null;

					this.killStreaks.forEach(killStreak -> killStreak.onCrateLand(this.owner, this.destination));

					destination.getWorld().playSound(destination, Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);

					done = true;
				} else {
					Vector velocity;
					if (fallingCrate.getY() < destination.getY() + 16) {
						velocity = new Vector(0, FALL_DELTA * 0.25, 0);
						fallingCrate.spawnParachute();
					} else {
						velocity = new Vector(0, FALL_DELTA * 2, 0); // fall faster
					}

					fallingCrate.move(velocity, true);
				}
			}
		}
	}

	void setDone() {
		this.done = true;
	}

	boolean isDone() {
		return done;
	}
}

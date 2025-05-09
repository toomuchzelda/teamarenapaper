package me.toomuchzelda.teamarenapaper.teamarena.abilities.centurion;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.registry.keys.SoundEventKeys;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.kyori.adventure.text.Component.*;

public class CenturionAbility extends Ability {
	public static final NamespacedKey SHIELD_KEY = new NamespacedKey(Main.getPlugin(), "centurion_shield");
	public static final ItemStack SHIELD = ItemBuilder.of(Material.PORKCHOP)
		.name(text("Particle Shield", TextColor.color(ShieldInstance.SHIELD_COLOR_HEX)))
		.lore(List.of(
			text("An advanced particle shield that", NamedTextColor.WHITE),
			text("mitigates most attacks to your allies.", NamedTextColor.WHITE),
			textOfChildren(
				text("Hold ["), keybind("key.use", NamedTextColor.AQUA), text("] to make the shield "),
				text("follow you", NamedTextColor.BLUE)
			).color(NamedTextColor.GRAY),
			textOfChildren(
				text("Press ["), keybind("key.drop", NamedTextColor.GREEN), text("] to deploy the shield "),
				text("remotely", NamedTextColor.DARK_GREEN)
			).color(NamedTextColor.GRAY),
			text("Note that there is a cooldown after every action.", NamedTextColor.DARK_GRAY)
		))
		.meta(meta -> meta.getPersistentDataContainer().set(SHIELD_KEY, PersistentDataType.BOOLEAN, true))
		.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(10).cooldownGroup(SHIELD_KEY).build())
		// sus!
		.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("light_blue_stained_glass_pane"))
		.removeData(DataComponentTypes.FOOD)
		.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
			.sound(SoundEventKeys.ITEM_SHIELD_BLOCK)
			.animation(ItemUseAnimation.BLOCK)
			.hasConsumeParticles(false)
			.consumeSeconds(1000000)
			.build())
		.build();

	private final Map<Player, ShieldInstance> playerShields = new HashMap<>();
	record ShieldHistory(double health, long lastDamageTick, int stoppedAt) {
		double estimateHealthNow() {
			int now = TeamArena.getGameTick();
			long regenTicks = now - (lastDamageTick + ShieldInstance.SHIELD_REGEN_COOLDOWN);
			if (regenTicks > 0) {
				return Math.min(ShieldConfig.DEFAULT_MAX_HEALTH, health + regenTicks * ShieldInstance.SHIELD_REGEN_PER_TICK);
			}
			return health;
		}

		ShieldConfig buildConfig() {
			return new ShieldConfig(estimateHealthNow(), ShieldConfig.DEFAULT_MAX_HEALTH, -1, null);
		}
	}
	private final Map<Player, ShieldHistory> playerShieldHistories = new HashMap<>();
	private final Map<Player, Double> playerMitigation = new HashMap<>();

	@Override
	public void onTick() {
		for (ShieldInstance shield : playerShields.values()) {
			shield.tick();
		}
	}

	@Override
	protected void giveAbility(Player player) {
		player.getInventory().addItem(SHIELD);
	}

	private static boolean isShieldItem(ItemStack stack) {
		return stack != null && stack.getType() == Material.PORKCHOP && stack.getPersistentDataContainer().has(SHIELD_KEY);
	}

	@Override
	protected void removeAbility(Player player) {
		for (var iter = player.getInventory().iterator(); iter.hasNext();) {
			ItemStack stack = iter.next();
			if (isShieldItem(stack)) {
				iter.set(null);
				break;
			}
		}
		ShieldInstance shield = playerShields.remove(player);
		if (shield != null) {
			shield.playBreakEffect();
			shield.cleanUp();
		}
		playerShieldHistories.remove(player);
		Double mitigatedDamage = playerMitigation.remove(player);
		if (mitigatedDamage == null)
			mitigatedDamage = 0d;
		player.sendMessage(textOfChildren(
				text("You mitigated "),
				text(TextUtils.formatHealth(mitigatedDamage), TextColors.HEALTH),
				text(" with your "),
				text("Particle Shield", TextColor.color(ShieldInstance.SHIELD_COLOR_HEX)),
				text(".")
		).color(NamedTextColor.GRAY));
	}


	@Override
	public void onInteract(PlayerInteractEvent event) {
		if (!event.getAction().isRightClick()) return;
		Player player = event.getPlayer();
		ItemStack stack = event.getItem();
		if (!isShieldItem(stack))
			return;
		if (player.hasCooldown(SHIELD) || (playerShields.get(player) instanceof ShieldInstance shieldInstance && shieldInstance.isValid()))
			return;
		ShieldHistory history = playerShieldHistories.remove(player);
		ShieldConfig config = history != null ? history.buildConfig() : ShieldConfig.DEFAULT;
		ShieldInstance shield = new ShieldInstance(player, config);
		shield.setMitigationListener(mitigation -> playerMitigation.merge(player, mitigation, Double::sum));
		shield.setBreakListener(() -> {
			playerShields.remove(player);
			// punish the player for letting their shields break
			player.setCooldown(SHIELD, 10 * 20);
		});
		playerShields.put(player, shield);
	}

	@Override
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		ItemStack stack = event.getItemDrop().getItemStack();
		if (!isShieldItem(stack))
			return;
		if (player.hasCooldown(SHIELD) || (playerShields.get(player) instanceof ShieldInstance shieldInstance && shieldInstance.isValid()))
			return;


		RayTraceResult result = player.rayTraceBlocks(10);
		Location location = player.getLocation();
		if (result != null) {
			Vector hitPosition = result.getHitPosition();
			location.set(hitPosition.getX(), hitPosition.getY() + 1.5, hitPosition.getZ());
		} else {
			location.add(location.getDirection().multiply(10));
		}

		player.setCooldown(SHIELD, 16 * 20);
		ShieldHistory history = playerShieldHistories.remove(player);
		double health = history != null ? history.estimateHealthNow() : ShieldConfig.DEFAULT_MAX_HEALTH;
		ShieldInstance shield = new ShieldInstance(player,
			new ShieldConfig(health, ShieldConfig.DEFAULT_MAX_HEALTH, 10 * 20, location));
		shield.setMitigationListener(mitigation -> playerMitigation.merge(player, mitigation, Double::sum));
		shield.setExpireListener(() -> playerShields.remove(player));
		shield.setBreakListener(() -> playerShields.remove(player));
		playerShields.put(player, shield);
	}

	@Override
	public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
		stopUsingShield(event.getPlayer());
	}

	@Override
	public void onSwitchItemSlot(PlayerItemHeldEvent event) {
		stopUsingShield(event.getPlayer());
	}

	@Override
	public void onStopUsingItem(PlayerStopUsingItemEvent event) {
		stopUsingShield(event.getPlayer());
	}

	private void stopUsingShield(Player player) {
		if (!isShieldItem(player.getActiveItem()))
			return;
		ShieldInstance shield = playerShields.remove(player);
		if (shield != null) {
			ShieldHistory history = new ShieldHistory(shield.health, shield.lastDamageTick, TeamArena.getGameTick());
			playerShieldHistories.put(player, history);
			shield.cleanUp();
			player.setCooldown(SHIELD, 20);
		}
	}

	@Override
	public void onTeamSwitch(Player player, @Nullable TeamArenaTeam oldTeam, @Nullable TeamArenaTeam newTeam) {
		ShieldInstance shield = playerShields.get(player);
		if (shield != null) {
			shield.updateTeam(oldTeam, newTeam);
		}
	}
}

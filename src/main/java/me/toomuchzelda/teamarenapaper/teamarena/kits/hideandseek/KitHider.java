package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitHider extends Kit {

	public static final String NAME = "Hider";

	private static final int TRANSFORM_CD = 2 * 20;
	private static final Component TRANSFORM_WAIT_MSG = Component.text("You can't do this yet", TextColors.ERROR_RED);
	private static final ItemStack TRANSFORM_WAND = ItemBuilder.of(Material.WOODEN_SHOVEL)
		.displayName(Component.text("Transformer", TextColor.color(119, 168, 50)))
		.lore(List.of(
			Component.text("Use on a block or animal to take that form", TextUtils.RIGHT_CLICK_TO),
			Component.text("Cooldown: " + TRANSFORM_CD / 20 + " secs", NamedTextColor.LIGHT_PURPLE)
		))
		.build();

	public KitHider() {
		super(NAME, "Take the form of blocks and animals to hide from seekers", Material.HAY_BLOCK);

		this.setItems(TRANSFORM_WAND);
		this.setAbilities(new HiderAbility());
	}

	private static class HiderAbility extends Ability {

		private final Map<Player, HiderInfo> disguiseMap = new HashMap<>(Bukkit.getMaxPlayers());

		@Override
		public void giveAbility(Player player) {
			disguiseMap.put(player, new HiderInfo(player));
		}

		@Override
		public void removeAbility(Player player) {
			disguiseMap.remove(player).remove();
		}

		@Override
		public void unregisterAbility() {
			this.disguiseMap.forEach((player, hiderInfo) -> hiderInfo.remove());
			this.disguiseMap.clear();
		}

		@Override
		public void onTick() {
			this.disguiseMap.forEach((player, hiderInfo) -> hiderInfo.tick());
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			final Player clicker = event.getPlayer();
			if (event.getAction().isRightClick() && TRANSFORM_WAND.isSimilar(event.getItem())) {
				if (clicker.getCooldown(TRANSFORM_WAND.getType()) == 0) {
					final Block clickedBlock = event.getClickedBlock();
					if (clickedBlock != null) {
						this.disguiseMap.get(clicker).disguise(clickedBlock);
						clicker.setCooldown(TRANSFORM_WAND.getType(), TRANSFORM_CD);
					}
				}
				else if (Main.getPlayerInfo(clicker).messageHasCooldowned("kithiderinteracttransform", 20)){
					clicker.sendMessage(TRANSFORM_WAIT_MSG);
				}
			}
		}
	}
}

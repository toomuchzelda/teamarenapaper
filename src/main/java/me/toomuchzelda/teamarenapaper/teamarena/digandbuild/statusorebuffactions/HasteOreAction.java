package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.statusorebuffactions;

import me.toomuchzelda.teamarenapaper.Main;
//import me.toomuchzelda.teamarenapaper.potioneffects.PotionEffectManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.LifeOre;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.StatusOreType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HasteOreAction implements StatusOreType.BuffAction
{
	public static final String HASTE_EFFECT_KEY = "dnbhst";
	private static int compositeKeyCounter = 0; // Ensure all keys are unique to allow stacking of haste effect.
	private static final PotionEffect HASTE_EFFECT = new PotionEffect(PotionEffectType.HASTE, 30 * 20, 0, false, true);
	@Override
	public int buff(Player redeemer, int required, int itemsUsed, LifeOre ore, DigAndBuild gameInstance) {
		if (itemsUsed >= required) {
			Component message = redeemer.playerListName().append(Component.text(" got your team Haste!", StatusOreType.HASTE.getTextColor()));
			for (Player teammate : Main.getPlayerInfo(redeemer).team.getPlayerMembers()) {
				giveEffect(teammate);

				teammate.sendMessage(message);
				teammate.playSound(teammate, Sound.ENTITY_GHAST_WARN, SoundCategory.AMBIENT, 1f, 0.5f);
			}

			ore.playHasteEffect();
			return required;
		}
		else {
			redeemer.sendMessage(Component.text("You need " +
				(required - itemsUsed) + " more of these to activate Haste", NamedTextColor.RED));

			return 0;
		}
	}

	public static void giveEffect(LivingEntity living) {
		final String compositeKey = HASTE_EFFECT_KEY + compositeKeyCounter++;
		//PotionEffectManager.addEffect(living, compositeKey, HASTE_EFFECT);
	}
}

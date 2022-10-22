package me.toomuchzelda.teamarenapaper.teamarena.damage;

import org.bukkit.entity.AbstractArrow;

import java.util.WeakHashMap;

/**
 * Class to record info if arrows should or shouldn't leave an arrow in the body of their victim
 *
 * @author toomuchzelda
 */
public class ArrowImpaleStatus
{
	//no WeakHashSet class
	private static final WeakHashMap<AbstractArrow, Void> NON_IMPALING_ARROWS = new WeakHashMap<>();

	public static void setImpaling(AbstractArrow arrow, boolean impale) {
		if(impale) {
			NON_IMPALING_ARROWS.remove(arrow);
		}
		else {
			NON_IMPALING_ARROWS.put(arrow, null);
		}
	}

	public static boolean isImpaling(AbstractArrow arrow) {
		return !NON_IMPALING_ARROWS.containsKey(arrow);
	}
}

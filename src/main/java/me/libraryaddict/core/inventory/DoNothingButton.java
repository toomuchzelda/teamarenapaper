package me.libraryaddict.core.inventory;

import me.libraryaddict.core.inventory.utils.IButton;
import org.bukkit.event.inventory.ClickType;

/**
 * Inventory button that does nothing
 * used to avoid excessive declaration of useless classes implementing IButton
 */
public class DoNothingButton implements IButton
{
	public static final DoNothingButton button = new DoNothingButton();
	
	/**
	 * Return true if cancel
	 *
	 * @param clickType
	 */
	@Override
	public boolean onClick(ClickType clickType) {
		return true;
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;

public class CommandException extends RuntimeException {
	public final Component message;
	public CommandException(String message) {
		this(Component.text(message, TextColors.ERROR_RED));
	}

	public CommandException(String message, Throwable cause) {
		this(Component.text(message, TextColors.ERROR_RED), cause);
	}

	public CommandException(Component message) {
		this.message = message;
	}

	public CommandException(Component message, Throwable cause) {
		super(cause);
		this.message = message;
	}
}

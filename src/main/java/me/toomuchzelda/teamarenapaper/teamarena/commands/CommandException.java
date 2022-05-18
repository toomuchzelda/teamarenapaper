package me.toomuchzelda.teamarenapaper.teamarena.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CommandException extends RuntimeException {
	public final Component message;
	public CommandException(String message) {
		this(Component.text(message, NamedTextColor.DARK_RED));
	}

	public CommandException(String message, Throwable cause) {
		this(Component.text(message, NamedTextColor.DARK_RED), cause);
	}

	public CommandException(Component message) {
		this.message = message;
	}

	public CommandException(Component message, Throwable cause) {
		super(cause);
		this.message = message;
	}
}

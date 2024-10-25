package me.toomuchzelda.teamarenapaper.utils;

import net.kyori.adventure.text.Component;

import java.util.function.Function;

@FunctionalInterface
public interface CompileSafePlaceholder<C> extends Function<C, Component> {
}

package me.toomuchzelda.teamarenapaper.teamarena.kits.filter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public sealed interface FilterAction {
	boolean filter(String kit);

	Component toComponent();

	FilterAction withAllow(String kit);

	FilterAction withBlock(String kit);

	static FilterAction allow(Set<String> kits) {
		return new Allow(kits);
	}

	static FilterAction allow(String... kits) {
		return allow(Set.of(kits));
	}

	static FilterAction block(Set<String> kits) {
		return new Block(kits);
	}

	static FilterAction block(String... kits) {
		return block(Set.of(kits));
	}


	record Allow(Set<String> kits) implements FilterAction {
		public Allow {
			if (kits.isEmpty()) throw new IllegalArgumentException("Cannot block all kits");
			kits = Set.copyOf(kits);
		}

		@Override
		public FilterAction withAllow(String kit) {
			if (kits.contains(kit))
				return this;
			var newAllow = new HashSet<>(kits);
			newAllow.add(kit);
			return new Allow(newAllow);
		}

		@Override
		public FilterAction withBlock(String kit) {
			if (!kits.contains(kit))
				return this;
			var newAllow = new HashSet<>(kits);
			newAllow.remove(kit);
			return new Allow(newAllow);
		}

		@Override
		public boolean filter(String kit) {
			return kits.contains(kit);
		}

		@Override
		public Component toComponent() {
			return Component.textOfChildren(
				Component.text("Only allows ", NamedTextColor.GREEN),
				Component.text(
					kits.stream().sorted().collect(Collectors.joining(", ")),
					NamedTextColor.YELLOW
				)
			);
		}
	}

	record Block(Set<String> kits) implements FilterAction {
		public Block {
			kits = Set.copyOf(kits);
		}

		@Override
		public FilterAction withAllow(String kit) {
			if (!kits.contains(kit))
				return this;
			var newBlock = new HashSet<>(kits);
			newBlock.remove(kit);
			return new Allow(newBlock);
		}

		@Override
		public FilterAction withBlock(String kit) {
			if (kits.contains(kit))
				return this;
			var newBlock = new HashSet<>(kits);
			newBlock.add(kit);
			return new Allow(newBlock);
		}

		@Override
		public boolean filter(String kit) {
			return !kits.contains(kit);
		}

		@Override
		public Component toComponent() {
			return Component.textOfChildren(
				Component.text("Only disallows ", NamedTextColor.RED),
				Component.text(
					kits.stream().sorted().collect(Collectors.joining(", ")),
					NamedTextColor.YELLOW
				)
			);
		}
	}

}
